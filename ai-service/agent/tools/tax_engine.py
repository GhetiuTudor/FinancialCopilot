"""
Tax engine tool — calls the Java backend Dashboard API for tax estimates.

Retrieves simplified Romanian tax estimates (micro tax, profit tax, VAT payable)
computed by the Java TaxEngineService from SAF-T imported data.
"""

import json
import logging

import requests
from langchain_core.tools import tool

from agent.config import JAVA_BACKEND_URL

logger = logging.getLogger("ai-service.tools.tax_engine")


@tool
def get_tax_summary(company_id: str) -> str:
    """
    Obține estimările fiscale ale companiei din motorul fiscal al backend-ului.

    Returnează: impozit micro estimat, impozit profit estimat, TVA de plată,
    regimul fiscal selectat, scadențe estimative.

    Folosește acest instrument când utilizatorul întreabă despre:
    - Taxe, impozite, TVA
    - Impozit micro vs. impozit pe profit
    - Estimări fiscale, obligații de plată
    - Scadențe fiscale, declarații ANAF
    - Regim fiscal (micro / profit)

    Args:
        company_id: ID-ul companiei (UUID ca string).

    Returns:
        JSON cu estimările fiscale ale companiei.
    """
    try:
        url = f"{JAVA_BACKEND_URL}/internal/v1/dashboard/summary"
        logger.info("Calling Java backend for tax: GET %s (company_id=%s)", url, company_id)

        response = requests.get(
            url,
            headers={
                "Content-Type": "application/json",
                "X-Company-Id": company_id,
            },
            timeout=10,
        )
        response.raise_for_status()

        data = response.json()
        taxes = data.get("taxes", {})

        tax_summary = {
            "micro_tax_estimate": taxes.get("micro_tax_estimate"),
            "profit_tax_estimate": taxes.get("profit_tax_estimate"),
            "vat_payable_estimate": taxes.get("vat_payable_estimate"),
            "vat_status": taxes.get("vat_status"),
            "selected_regime": taxes.get("selected_regime"),
            "selected_tax_amount": taxes.get("selected_tax_amount"),
            "due_date": taxes.get("due_date"),
            "deadlines": taxes.get("deadlines", []),
            "disclaimer": taxes.get("disclaimer"),
            "currency": data.get("currency", "RON"),
        }

        return json.dumps(tax_summary, ensure_ascii=False, default=str)

    except requests.exceptions.ConnectionError:
        logger.error("Cannot connect to Java backend at %s", JAVA_BACKEND_URL)
        return json.dumps({
            "error": "Nu s-a putut conecta la backend-ul Java. Verificați dacă serverul este pornit.",
        })
    except requests.exceptions.HTTPError as e:
        logger.error("Java backend returned error: %s", e)
        return json.dumps({"error": f"Eroare backend: {e.response.status_code}"})
    except Exception as e:
        logger.exception("Failed to fetch tax summary")
        return json.dumps({"error": f"Eroare la obținerea estimărilor fiscale: {e}"})
