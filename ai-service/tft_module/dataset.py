import pandas as pd
# pyrefly: ignore [missing-import]
from pytorch_forecasting import TimeSeriesDataSet
# pyrefly: ignore [missing-import]
from pytorch_forecasting.data import GroupNormalizer

from .config import (
    DATA_PATH,
    TARGET,
    MAX_ENCODER_LENGTH,
    MAX_PREDICTION_LENGTH,
    STATIC_CATEGORICALS,
    TIME_VARYING_KNOWN_REALS,
    TIME_VARYING_UNKNOWN_REALS,
    GROUP_IDS,
    TIME_IDX,
)


def _create_monthly_vat_target(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()

    df["monthly_vat_target"] = (
        df.groupby("company_id")["vat_due"]
        .transform(lambda s: s.shift(-1).rolling(window=30, min_periods=30).sum().shift(-29))
    )

    before = len(df)
    df = df.dropna(subset=["monthly_vat_target"]).reset_index(drop=True)
    dropped = before - len(df)

    if dropped > 0:
        print(f"    [monthly_vat_target] Dropped {dropped} rows without full 30-day future window")

    return df


def load_dataframe(path=DATA_PATH) -> pd.DataFrame:
    df = pd.read_csv(path)
    df["date"] = pd.to_datetime(df["date"])

    df["sector"] = df["sector"].astype(str)
    df["company_id"] = df["company_id"].astype(str)

    df = df.sort_values(["company_id", TIME_IDX]).reset_index(drop=True)

    df[TIME_IDX] = df.groupby("company_id").cumcount()

    df = _create_monthly_vat_target(df)

    df = df.sort_values(["company_id", TIME_IDX]).reset_index(drop=True)
    df[TIME_IDX] = df.groupby("company_id").cumcount()

    return df


def build_datasets(df: pd.DataFrame):
    training_cutoff = df[TIME_IDX].max() - MAX_PREDICTION_LENGTH

    training = TimeSeriesDataSet(
        df[df[TIME_IDX] <= training_cutoff],
        time_idx=TIME_IDX,
        target=TARGET,
        group_ids=GROUP_IDS,

        min_encoder_length=MAX_ENCODER_LENGTH // 2,
        max_encoder_length=MAX_ENCODER_LENGTH,

        min_prediction_length=1,
        max_prediction_length=MAX_PREDICTION_LENGTH,

        static_categoricals=STATIC_CATEGORICALS,
        time_varying_known_reals=TIME_VARYING_KNOWN_REALS,
        time_varying_unknown_reals=TIME_VARYING_UNKNOWN_REALS,

        target_normalizer=GroupNormalizer(groups=GROUP_IDS),

        add_relative_time_idx=True,

        add_target_scales=True,

        add_encoder_length=True,
    )

    validation = TimeSeriesDataSet.from_dataset(
        training, df, predict=True, stop_randomization=True
    )

    return training, validation


def build_dataloaders(
    training: TimeSeriesDataSet,
    validation: TimeSeriesDataSet,
    batch_size: int,
):
    train_loader = training.to_dataloader(
        train=True, batch_size=batch_size, num_workers=0
    )
    val_loader = validation.to_dataloader(
        train=False, batch_size=batch_size, num_workers=0
    )
    return train_loader, val_loader