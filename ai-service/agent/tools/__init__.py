"""single tool registry """

from agent.tools.rag import search_legislation
from agent.tools.forecast import forecast_vat
from agent.tools.financial_metrics import get_financial_metrics
from agent.tools.tax_engine import get_tax_summary
from agent.tools.ledger_records import get_ledger_records


ALL_TOOLS = [search_legislation, forecast_vat, get_financial_metrics, get_tax_summary, get_ledger_records]


__all__ = [
    "search_legislation",
    "forecast_vat",
    "get_financial_metrics",
    "get_tax_summary",
    "get_ledger_records",
    "ALL_TOOLS",
]
