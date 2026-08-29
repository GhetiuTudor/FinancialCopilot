import json
import logging
from typing import Optional

# pyrefly: ignore [missing-import]
from langchain_core.tools import tool

from .config import MAX_ENCODER_LENGTH
from .predict import predict_company, predict_from_dataframe

logger = logging.getLogger(__name__)


def _build_scenario_summary(forecast: dict) -> str:
    q10 = forecast["q10"]
    q50 = forecast["q50"]
    q90 = forecast["q90"]
    spread = q90 - q10

    lines = [
        "Analiza scenariilor TVA (Decont D300):",
        "═" * 50,
        f"  📉 Scenariu pesimist (q10):   {q10:>12,.2f} RON",
        f"  📊 Scenariu median   (q50):   {q50:>12,.2f} RON",
        f"  📈 Scenariu optimist (q90):   {q90:>12,.2f} RON",
        "─" * 50,
        f"  Interval incertitudine:       {spread:>12,.2f} RON",
    ]

    if abs(q50) > 0:
        spread_pct = (spread / abs(q50)) * 100
        if spread_pct > 50:
            risk = "⚠️  Risc ridicat — incertitudine mare"
        elif spread_pct > 20:
            risk = "⚡ Risc moderat — variabilitate normală"
        else:
            risk = "✅ Risc scăzut — previziune stabilă"
        lines.append(f"  {risk} ({spread_pct:.0f}% variație)")
    else:
        lines.append("  ⚠️  TVA median = 0, verificați datele")

    lines.append("═" * 50)
    return "\n".join(lines)


def _format_monthly_output(raw_result: dict) -> dict:
    forecast = raw_result.get("monthly_vat_forecast", {})
    interp = raw_result.get("interpretation", {})

    if not forecast:
        return raw_result

    scenario_text = _build_scenario_summary(forecast)

    return {
        "company_id": raw_result.get("company_id"),
        "target": raw_result.get("target"),
        "data_source": raw_result.get("data_source", "unknown"),
        "forecast_period": raw_result.get("forecast_period", ""),
        "monthly_summary": {
            "total_vat_estimated_q50": forecast.get("q50", 0),
            "pessimistic_q10": forecast.get("q10", 0),
            "optimistic_q90": forecast.get("q90", 0),
            "uncertainty_range_ron": interp.get("uncertainty_range_ron", 0),
            "uncertainty_pct": interp.get("uncertainty_pct", 0),
            "currency": "RON",
        },
        "monthly_vat_forecast": forecast,
        "scenario_analysis": scenario_text,
        "note": raw_result.get("note", ""),
    }


@tool
def forecast_vat(company_id: str, checkpoint_path: Optional[str] = None) -> str:
    """
    Previzionează TVA lunar folosind modelul Temporal Fusion Transformer antrenat.

    Modelul prezice expunerea TVA lunară totală (suma pentru următoarele 30 de zile)
    cu trei scenarii probabilistice:
    - q10 (pesimist): estimare conservatoare
    - q50 (median): cea mai probabilă valoare
    - q90 (optimist): scenariu de risc ridicat

    Modelul folosește date financiare reale din baza de date (dacă sunt disponibile)
    sau date demonstrative ca fallback.

    Args:
        company_id: ID-ul companiei (UUID ca string).
        checkpoint_path: Opțional, calea către checkpoint.

    Returns:
        JSON cu TVA lunar estimat pe cele 3 cuantile, analiză de scenarii.
    """
    raw_result = None

    from .exceptions import InsufficientDataError, ModelLoadError
    import uuid

    is_real_company = False
    try:
        uuid.UUID(str(company_id))
        is_real_company = True
    except ValueError:
        logger.info("Company ID %s is not a valid UUID; treating as demo/synthetic ID", company_id)

    if is_real_company:
        try:
            from .real_data_adapter import fetch_and_build_dataframe
            from agent.config import JAVA_BACKEND_URL

            logger.info("Attempting real-data forecast for company %s", company_id)

            df = fetch_and_build_dataframe(
                company_id=company_id,
                backend_url=JAVA_BACKEND_URL,
                sector="Retail",
                days=400,
            )

            raw_result = predict_from_dataframe(
                real_df=df,
                real_company_id=company_id,
                checkpoint_path=checkpoint_path,
            )
            raw_result["data_source"] = "real"
            raw_result["data_points"] = len(df)
            raw_result["note"] = (
                "Predicție bazată pe datele financiare reale ale companiei, "
                "extrase din registrul contabil. Modelul TFT a fost antrenat "
                "pe date sintetice, deci acuratețea este orientativă."
            )
            logger.info(
                "Real-data forecast successful: %d data points",
                len(df),
            )

        except (InsufficientDataError, FileNotFoundError, ModelLoadError) as e:
            logger.warning(
                "Real-data forecast failed for company %s: %s. Falling back to demo.",
                company_id, e,
            )

    if raw_result is None:
        try:
            logger.info("Falling back to demo forecast for sector 'Retail'")

            raw_result = predict_company(
                company_id=company_id,
                checkpoint_path=checkpoint_path,
                sector="Retail",
            )
            raw_result["data_source"] = "demo"
            raw_result["note"] = (
                "Predicție demonstrativă (sector Retail, date sintetice). "
                "Nu s-au putut obține date reale din backend. "
                "Modelul TFT arată capacitatea de previziune, "
                "nu date reale ale firmei."
            )

        except FileNotFoundError as e:
            logger.error("Checkpoint not found: %s", e)
            return json.dumps({"error": f"Checkpoint negăsit: {e}"})
        except ValueError as e:
            logger.error("Prediction error: %s", e)
            return json.dumps({"error": f"Eroare la predicție: {e}"})
        except Exception as e:
            logger.exception("Unexpected error during forecast")
            return json.dumps({"error": f"Previziune eșuată: {e}"})

    monthly = _format_monthly_output(raw_result)
    return json.dumps(monthly, indent=2, ensure_ascii=False)
