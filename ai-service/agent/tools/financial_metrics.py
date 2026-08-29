"""
Financial metrics tool — calls the Java backend Dashboard API.

Retrieves real-time financial metrics (balance, cashflow, revenue, expenses,
outstanding invoices, VAT, coverage ratio, runway) computed by the Java
FinancialMetricsService from SAF-T imported data.
"""

import json
import logging

import requests
from langchain_core.tools import tool

from agent.config import JAVA_BACKEND_URL

logger = logging.getLogger("ai-service.tools.financial_metrics")


@tool
def get_financial_metrics(company_id: str) -> str:
    """
    Obține metricile financiare actuale ale companiei din sistemul backend.

    Returnează: sold disponibil, încasări/plăți 30 zile, venituri, cheltuieli,
    profit, facturi restante, TVA colectat/deductibil/de plată, coverage ratio,
    runway (luni de operare).

    Folosește acest instrument când utilizatorul întreabă despre:
    - Situația financiară curentă
    - Sold, cash-flow, lichidități
    - Facturi restante sau neplătite
    - Venituri, cheltuieli, profit
    - Indicatori financiari (coverage ratio, runway)

    Args:
        company_id: ID-ul companiei (UUID ca string).

    Returns:
        JSON cu metricile financiare ale companiei.
    """
    try:
        url = f"{JAVA_BACKEND_URL}/internal/v1/dashboard/summary"
        logger.info("Calling Java backend: GET %s (company_id=%s)", url, company_id)

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

        
        metrics = {
            "sold_disponibil": data.get("soldDisponibil"),
            "incasari_30_zile": data.get("incasari30Zile"),
            "plati_30_zile": data.get("plati30Zile"),
            "net_cashflow_30d": data.get("netCashflow30d"),
            "revenue_30d": data.get("revenue30d"),
            "expenses_30d": data.get("expenses30d"),
            "profit_30d": data.get("profit30d"),
            "facturi_restante": data.get("facturiRestante"),
            "unpaid_invoice_count": data.get("unpaidInvoiceCount"),
            "coverage_ratio": data.get("coverageRatio"),
            "runway_months": data.get("runwayMonths"),
            "currency": data.get("currency", "RON"),
        }

        coverage = data.get("acoperire")
        if coverage:
            metrics["coverage_from"] = coverage.get("from")
            metrics["coverage_to"] = coverage.get("to")
            metrics["coverage_sources"] = coverage.get("sources")

        return json.dumps(metrics, ensure_ascii=False, default=str)

    except requests.exceptions.ConnectionError:
        logger.error("Cannot connect to Java backend at %s", JAVA_BACKEND_URL)
        return json.dumps({
            "error": "Nu s-a putut conecta la backend-ul Java. Verificați dacă serverul este pornit.",
        })
    except requests.exceptions.HTTPError as e:
        logger.error("Java backend returned error: %s", e)
        return json.dumps({"error": f"Eroare backend: {e.response.status_code}"})
    except Exception as e:
        logger.exception("Failed to fetch financial metrics")
        return json.dumps({"error": f"Eroare la obținerea metricilor: {e}"})
