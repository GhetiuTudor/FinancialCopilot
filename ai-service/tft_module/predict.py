import argparse
import logging
from pathlib import Path

# pyrefly: ignore [missing-import]
import numpy as np
import pandas as pd
# pyrefly: ignore [missing-import]
import torch
# pyrefly: ignore [missing-import]
from pytorch_forecasting import TemporalFusionTransformer

logger = logging.getLogger(__name__)

from .config import (
    DATA_PATH,
    BATCH_SIZE,
    CHECKPOINT_DIR,
    TARGET,
    MAX_PREDICTION_LENGTH,
    MAX_ENCODER_LENGTH,
    QUANTILES,
)
from .dataset import load_dataframe, build_datasets


Q10_IDX = 0
Q50_IDX = 1
Q90_IDX = 2


def load_model(checkpoint_path: str | None = None) -> TemporalFusionTransformer:
    if checkpoint_path is None:
        new_ckpt = CHECKPOINT_DIR / "tft_monthly_vat_quantile.ckpt"
        legacy_ckpt = CHECKPOINT_DIR / "tft_vat_due.ckpt"
        if new_ckpt.exists():
            checkpoint_path = str(new_ckpt)
        elif legacy_ckpt.exists():
            checkpoint_path = str(legacy_ckpt)
        else:
            raise FileNotFoundError(
                f"Checkpoint not found at {new_ckpt} or {legacy_ckpt}\n"
                f"Run 'python -m tft_module.train' first."
            )

    if not Path(checkpoint_path).exists():
        raise FileNotFoundError(
            f"Checkpoint not found: {checkpoint_path}\n"
            f"Run 'python -m tft_module.train' first."
        )

    _orig_zeros = torch.zeros

    def _safe_zeros(*args, **kwargs):
        if "device" in kwargs:
            dev = kwargs["device"]
            if isinstance(dev, torch.device) and dev.type == "mps":
                kwargs["device"] = torch.device("cpu")
            elif isinstance(dev, str) and "mps" in dev:
                kwargs["device"] = "cpu"
        return _orig_zeros(*args, **kwargs)

    from .exceptions import ModelLoadError

    torch.zeros = _safe_zeros
    try:
        model = TemporalFusionTransformer.load_from_checkpoint(
            checkpoint_path, map_location=torch.device("cpu")
        )
    except FileNotFoundError:
        raise
    except Exception as e:
        raise ModelLoadError(f"Failed to load model: {e}") from e
    finally:
        torch.zeros = _orig_zeros

    model.eval()
    return model


def _extract_quantile_predictions(
    model: TemporalFusionTransformer,
    dataloader,
) -> dict:
    with torch.no_grad():
        raw_output = model.predict(dataloader, mode="raw", return_x=False)

    predictions = raw_output["prediction"]

    predictions = predictions.squeeze(1)

    last_pred = predictions[-1].detach().cpu().numpy()

    if last_pred.ndim == 0 or last_pred.shape[0] < 3:
        import logging
        logging.getLogger(__name__).warning(
            "Checkpoint outputs %s values instead of 3 quantiles. "
            "Using point estimate as q50 with ±15%% spread. "
            "Retrain the model for proper quantile forecasts: "
            "python -m tft_module.train",
            last_pred.shape[0] if last_pred.ndim > 0 else 1,
        )
        point = float(last_pred.flat[0])
        return {
            "q10": round(point * 0.85, 2),
            "q50": round(point, 2),
            "q90": round(point * 1.15, 2),
        }

    return {
        "q10": float(last_pred[Q10_IDX]),
        "q50": float(last_pred[Q50_IDX]),
        "q90": float(last_pred[Q90_IDX]),
    }


def _format_output(
    quantiles: dict,
    company_id: str,
    last_date: pd.Timestamp,
    data_source: str = "unknown",
    note: str = "",
) -> dict:
    period_start = last_date + pd.Timedelta(days=1)
    period_end = last_date + pd.Timedelta(days=30)

    q10 = quantiles["q10"]
    q50 = quantiles["q50"]
    q90 = quantiles["q90"]

    uncertainty_range = q90 - q10
    if q50 != 0:
        uncertainty_pct = (uncertainty_range / abs(q50)) * 100
    else:
        uncertainty_pct = 0.0

    return {
        "company_id": company_id,
        "target": TARGET,
        "data_source": data_source,
        "forecast_period": f"{period_start.date()} → {period_end.date()}",
        "monthly_vat_forecast": {
            "q10": round(q10, 2),
            "q50": round(q50, 2),
            "q90": round(q90, 2),
        },
        "interpretation": {
            "pessimistic_estimate": round(q10, 2),
            "median_estimate": round(q50, 2),
            "optimistic_estimate": round(q90, 2),
            "uncertainty_range_ron": round(uncertainty_range, 2),
            "uncertainty_pct": round(uncertainty_pct, 1),
            "currency": "RON",
        },
        "note": note,
    }


def predict_company(
    company_id: str,
    checkpoint_path: str | None = None,
    sector: str = "Retail",
) -> dict:
    df = load_dataframe(DATA_PATH)

    available_companies = sorted(df["company_id"].unique().tolist())
    sector_companies = df[df["sector"] == sector]["company_id"].unique()
    if len(sector_companies) == 0:
        available_sectors = sorted(df["sector"].unique().tolist())
        raise ValueError(
            f"No companies found in sector '{sector}'. "
            f"Available sectors: {available_sectors}"
        )

    demo_company = sector_companies[0]

    training, _ = build_datasets(df)

    company_df = df[df["company_id"] == demo_company].copy()

    logger.info("Loaded %d rows", len(company_df))
    logger.info("Missing values before preprocessing: %d", company_df.isna().sum().sum())

    company_df = company_df.sort_values("time_idx").reset_index(drop=True)

    logger.info("Missing values after preprocessing: %d", company_df.isna().sum().sum())
    logger.info("Encoder window size: %d", MAX_ENCODER_LENGTH)
    logger.info("Decoder window size: %d", MAX_PREDICTION_LENGTH)

    if len(company_df) < MAX_ENCODER_LENGTH:
        raise ValueError(
            f"Company '{demo_company}' has only {len(company_df)} rows, "
            f"but {MAX_ENCODER_LENGTH} are needed for encoding."
        )

    predict_ds = training.from_dataset(
        training,
        company_df,
        predict=True,
        stop_randomization=True,
    )
    predict_loader = predict_ds.to_dataloader(
        train=False, batch_size=BATCH_SIZE, num_workers=0
    )

    model = load_model(checkpoint_path)
    logger.info("Calling TFT.predict()")
    quantiles = _extract_quantile_predictions(model, predict_loader)
    logger.info("Forecast completed successfully")

    last_date = company_df["date"].max()

    return _format_output(
        quantiles=quantiles,
        company_id=company_id,
        last_date=last_date,
        data_source="demo",
        note=(
            "Predicție demonstrativă (date sintetice). "
            "Modelul TFT arată capacitatea de previziune, nu date reale ale firmei."
        ),
    )


def predict_from_dataframe(
    real_df: pd.DataFrame,
    real_company_id: str = "",
    checkpoint_path: str | None = None,
) -> dict:
    import logging
    logger = logging.getLogger(__name__)

    if len(real_df) < MAX_ENCODER_LENGTH:
        raise ValueError(
            f"Need at least {MAX_ENCODER_LENGTH} rows, got {len(real_df)}"
        )

    required = {"company_id", "sector", "date", "time_idx", "vat_due"}
    missing = required - set(real_df.columns)
    if missing:
        raise ValueError(f"DataFrame missing required columns: {missing}")

    logger.info("Loaded %d rows", len(real_df))
    logger.info("Missing values before preprocessing: %d", real_df.isna().sum().sum())

    company_df = real_df.copy()
    if "monthly_vat_target" not in company_df.columns:
        logger.info("Computing monthly_vat_target from vat_due...")
        company_df["monthly_vat_target"] = (
            company_df.groupby("company_id")["vat_due"]
            .transform(
                lambda s: s.shift(-1).rolling(window=30, min_periods=30).sum().shift(-29)
            )
        )
        company_df["monthly_vat_target"] = (
            company_df["monthly_vat_target"].ffill().fillna(0)
        )

    logger.info("Missing values after preprocessing: %d", company_df.isna().sum().sum())
    logger.info("Encoder window size: %d", MAX_ENCODER_LENGTH)
    logger.info("Decoder window size: %d", MAX_PREDICTION_LENGTH)

    logger.info("Loading synthetic dataset for normalizer state...")
    synthetic_df = load_dataframe(DATA_PATH)
    training, _ = build_datasets(synthetic_df)

    company_df = company_df.sort_values("time_idx").reset_index(drop=True)

    company_df["company_id"] = "0"
    predict_ds = training.from_dataset(
        training,
        company_df,
        predict=True,
        stop_randomization=True,
    )
    predict_loader = predict_ds.to_dataloader(
        train=False, batch_size=BATCH_SIZE, num_workers=0
    )

    model = load_model(checkpoint_path)
    logger.info("Calling TFT.predict()")
    quantiles = _extract_quantile_predictions(model, predict_loader)
    logger.info("Forecast completed successfully")

    last_date = company_df["date"].max()

    return _format_output(
        quantiles=quantiles,
        company_id=real_company_id or "0",
        last_date=last_date,
        data_source="real",
        note=(
            "Predicție bazată pe datele financiare reale ale companiei, "
            "extrase din registrul contabil. Modelul TFT a fost antrenat "
            "pe date sintetice, deci acuratețea este orientativă."
        ),
    )


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="TFT Monthly VAT Prediction")
    parser.add_argument(
        "--company_id", type=str, default="0",
        help="Company ID to predict for"
    )
    args = parser.parse_args()

    result = predict_company(args.company_id)
    forecast = result["monthly_vat_forecast"]
    interp = result["interpretation"]

    print(f"\n📊 Monthly VAT Forecast for company {result['company_id']}:")
    print(f"   Period: {result['forecast_period']}")
    print(f"   ─────────────────────────────────")
    print(f"   q10 (pessimistic):  {forecast['q10']:>12,.2f} RON")
    print(f"   q50 (median):       {forecast['q50']:>12,.2f} RON")
    print(f"   q90 (optimistic):   {forecast['q90']:>12,.2f} RON")
    print(f"   ─────────────────────────────────")
    print(f"   Uncertainty range:  {interp['uncertainty_range_ron']:>12,.2f} RON ({interp['uncertainty_pct']:.1f}%)")