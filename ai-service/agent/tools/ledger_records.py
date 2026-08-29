"""
Ledger records tool — calls the Java backend Internal Ledger API.

Retrieves individual financial records (invoices, payments, journal entries)
from the database, filtered by record type and date range.

This fills the gap left by get_financial_metrics (which only returns aggregates)
by giving the agent access to row-level data needed for questions like
"ce facturi am de plătit?" or "ce plăți am făcut luna asta?".
"""

import json
import logging
from typing import Optional

import requests
from langchain_core.tools import tool

from agent.config import JAVA_BACKEND_URL

logger = logging.getLogger("ai-service.tools.ledger_records")

VALID_FILTERS = {"ALL", "INVOICES", "UNPAID", "OVERDUE", "JOURNAL", "PAYMENTS"}


@tool
def get_ledger_records(
    company_id: str,
    record_types: list[str],
    days_back: int = 30,
) -> str:
    """
    Obține înregistrări financiare individuale din baza de date a companiei.

    Returnează liste de facturi, plăți sau înregistrări contabile cu detalii
    complete: număr document, partener, sumă, dată, status.

    Tipuri de înregistrări disponibile (record_types):
    - "INVOICES" — toate facturile (emise și primite)
    - "UNPAID" — doar facturile neplătite (de încasat sau de plătit)
    - "OVERDUE" — facturi restante (scadente și neplătite)
    - "PAYMENTS" — plăți efectuate
    - "JOURNAL" — înregistrări contabile (note contabile)

    Folosește acest instrument când utilizatorul întreabă despre:
    - Lista facturilor de plătit, facturile restante
    - Facturi specifice sau detalii despre facturi individuale
    - Furnizori/clienți cu facturi deschise
    - Plăți recente efectuate
    - Înregistrări contabile / note contabile
    - "Ce am de plătit?", "Ce facturi am?", "Cui datorez bani?"

    NU folosi acest instrument pentru metrici agregate (sold, cashflow, etc.)
    — folosește get_financial_metrics în schimb.

    Args:
        company_id: ID-ul companiei (UUID ca string).
        record_types: Lista tipurilor de înregistrări dorite.
                      Valori posibile: INVOICES, UNPAID, OVERDUE, PAYMENTS, JOURNAL.
        days_back: Câte zile în urmă să caute (implicit 30, maxim 365).

    Returns:
        JSON cu înregistrările financiare, grupate pe tip.
    """
    if not record_types:
        return json.dumps({"error": "record_types nu poate fi gol. Valori posibile: INVOICES, UNPAID, OVERDUE, PAYMENTS, JOURNAL."})

    
    normalized = []
    for rt in record_types:
        upper = rt.strip().upper()
        if upper in VALID_FILTERS and upper != "ALL":
            normalized.append(upper)
        else:
            logger.warning("Ignoring unknown record type: %s", rt)

    if not normalized:
        return json.dumps({"error": f"Niciun tip de înregistrare valid. Valori posibile: {', '.join(sorted(VALID_FILTERS - {'ALL'}))}"})

    days_back = max(1, min(days_back, 365))

    results = {}
    for filter_type in normalized:
        try:
            url = f"{JAVA_BACKEND_URL}/internal/v1/ledger"
            params = {
                "filter": filter_type,
                "days": days_back,
                "limit": 50,
            }
            logger.info(
                "Calling Java backend: GET %s (company_id=%s, filter=%s, days=%d)",
                url, company_id, filter_type, days_back,
            )

            response = requests.get(
                url,
                params=params,
                headers={
                    "Content-Type": "application/json",
                    "X-Company-Id": company_id,
                },
                timeout=10,
            )
            response.raise_for_status()

            data = response.json()
            items = data.get("items", [])
            total = data.get("total", len(items))

            results[filter_type.lower()] = {
                "total_count": total,
                "returned_count": len(items),
                "records": items,
            }

            logger.info(
                "Got %d/%d records for filter=%s (company=%s)",
                len(items), total, filter_type, company_id,
            )

        except requests.exceptions.ConnectionError:
            logger.error("Cannot connect to Java backend at %s", JAVA_BACKEND_URL)
            results[filter_type.lower()] = {
                "error": "Nu s-a putut conecta la backend-ul Java. Verificați dacă serverul este pornit.",
            }
        except requests.exceptions.HTTPError as e:
            logger.error("Java backend returned error for filter=%s: %s", filter_type, e)
            results[filter_type.lower()] = {
                "error": f"Eroare backend: {e.response.status_code}",
            }
        except Exception as e:
            logger.exception("Failed to fetch ledger records for filter=%s", filter_type)
            results[filter_type.lower()] = {
                "error": f"Eroare la obținerea înregistrărilor: {e}",
            }

    return json.dumps(results, ensure_ascii=False, default=str)
