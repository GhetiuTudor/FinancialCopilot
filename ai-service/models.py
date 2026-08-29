"""
Shared Pydantic models for the AI service.

Used by both the legacy main.py endpoint and the new LangGraph agent.
These models define the contract between the Java backend and the Python AI service.
"""

import json
import logging
import re
from datetime import date, datetime
from enum import Enum
from typing import Any, Optional
from uuid import UUID, uuid4

from pydantic import BaseModel, Field

logger = logging.getLogger("ai-service")

# ──────────────────────────────────────────────────────────────────────
# Enums
# ──────────────────────────────────────────────────────────────────────

class InvoiceDirection(str, Enum):
    AR = "AR"
    AP = "AP"


class InvoiceStatus(str, Enum):
    OPEN = "OPEN"
    PAID = "PAID"
    OVERDUE = "OVERDUE"
    CANCELED = "CANCELED"
    UNKNOWN = "UNKNOWN"


class SourceType(str, Enum):
    SAFT = "SAFT"
    SMARTBILL = "SMARTBILL"
    FGO = "FGO"
    TINK = "TINK"
    MANUAL = "MANUAL"


class CoverageStatus(str, Enum):
    OK = "OK"
    DEGRADED = "DEGRADED"
    ERROR = "ERROR"
    DISCONNECTED = "DISCONNECTED"


class EvidenceKind(str, Enum):
    INVOICE = "INVOICE"
    BANK_TX = "BANK_TX"
    LEDGER_ENTRY = "LEDGER_ENTRY"
    COUNTERPARTY = "COUNTERPARTY"
    DOCUMENT = "DOCUMENT"
    SAFT_IMPORT = "SAFT_IMPORT"
    OTHER = "OTHER"


class InsightType(str, Enum):
    CASHFLOW_RISK = "CASHFLOW_RISK"
    ANOMALY = "ANOMALY"
    RECOMMENDATION = "RECOMMENDATION"
    COMPLIANCE_NOTE = "COMPLIANCE_NOTE"
    COVERAGE_NOTICE = "COVERAGE_NOTICE"


class ActionType(str, Enum):
    FOLLOW_UP_CLIENT = "FOLLOW_UP_CLIENT"
    FOLLOW_UP_SUPPLIER = "FOLLOW_UP_SUPPLIER"
    REVIEW_CASHFLOW = "REVIEW_CASHFLOW"
    REVIEW_ANOMALY = "REVIEW_ANOMALY"
    REQUEST_SAFT_IMPORT = "REQUEST_SAFT_IMPORT"
    RECONNECT_BANK = "RECONNECT_BANK"
    SET_BUDGET_ALERT = "SET_BUDGET_ALERT"
    OTHER = "OTHER"


class ResponseStatus(str, Enum):
    OK = "OK"
    PARTIAL = "PARTIAL"
    ERROR = "ERROR"


# ──────────────────────────────────────────────────────────────────────
# Request models (Java → Python)
# ──────────────────────────────────────────────────────────────────────

class TimeRange(BaseModel):
    from_date: date = Field(alias="from")
    to_date: date = Field(alias="to")
    model_config = {"populate_by_name": True}


class CoverageWindow(BaseModel):
    source_type: SourceType
    coverage_start: Optional[date] = None
    coverage_end: Optional[date] = None
    last_successful_sync_at: Optional[datetime] = None
    status: CoverageStatus


class SystemContext(BaseModel):
    time_range: TimeRange
    coverage: list[CoverageWindow]


class AiConstraints(BaseModel):
    advisory_only: bool = True
    allow_external_calls: Optional[bool] = None


class LedgerWindow(BaseModel):
    from_date: Optional[str] = Field(None, alias="from")
    to_date: Optional[str] = Field(None, alias="to")
    model_config = {"populate_by_name": True}


class LedgerCoverage(BaseModel):
    as_of: Optional[str] = None
    window: Optional[LedgerWindow] = None
    sources: Optional[list[str]] = None


class LedgerEntities(BaseModel):
    organization_id: Optional[str] = None


class LedgerRawRef(BaseModel):
    pass


class FinancialContext(BaseModel):
    coverage: Optional[LedgerCoverage] = None
    entities: Optional[LedgerEntities] = None
    facts: Optional[dict[str, Any]] = None
    raw_refs: Optional[list[LedgerRawRef]] = None
    profile: Optional[dict[str, Any]] = None


class ChatTurn(BaseModel):
    role: str
    content: str


class AiAnalyzeRequest(BaseModel):
    request_id: UUID
    tenant_id: UUID
    user_id: UUID
    locale: str
    as_of: datetime
    question: str = Field(min_length=1, max_length=4000)
    system_context: SystemContext
    financial_context: Optional[FinancialContext] = None
    constraints: AiConstraints
    messages: Optional[list[ChatTurn]] = None
    thread_id: Optional[str] = None


# ──────────────────────────────────────────────────────────────────────
# Response models (Python → Java)
# ──────────────────────────────────────────────────────────────────────

class EvidenceRef(BaseModel):
    kind: EvidenceKind
    id: UUID
    note: Optional[str] = Field(None, max_length=300)


class RecommendedAction(BaseModel):
    action: ActionType
    params: Optional[dict[str, Any]] = None


class AiInsight(BaseModel):
    type: InsightType
    title: str = Field(max_length=140)
    summary: str = Field(max_length=4000)
    risk_score: Optional[float] = Field(None, ge=0, le=1)
    confidence: float = Field(ge=0, le=1)
    evidence: list[EvidenceRef]
    actions: Optional[list[RecommendedAction]] = None


class TokenUsage(BaseModel):
    model: str
    tokens_in: Optional[int] = Field(None, ge=0)
    tokens_out: Optional[int] = Field(None, ge=0)


class AiAnalyzeResponse(BaseModel):
    request_id: UUID
    status: ResponseStatus
    insights: list[AiInsight]
    answer_text: Optional[str] = None
    tools_used: Optional[list[str]] = None
    usage: TokenUsage
    warnings: Optional[list[str]] = None


class HealthResponse(BaseModel):
    status: str = "ok"
    ai_backend: str = "unknown"


# ──────────────────────────────────────────────────────────────────────
# Prompt
#  TODO TAX KEYWORDS + bagate in graph 
# ──────────────────────────────────────────────────────────────────────

_TAX_KEYWORDS = re.compile(
    r"\b(tax[eăa]|tva|impozit|scadență|scadent[aă]|declarați[ie]|contribuți[ie]|"
    r"micro|profit|platitor|plătitor|norma|anaf|trimestrial|lunar)\b",
    re.IGNORECASE,
)


def is_tax_intent(question: str) -> bool:
    return bool(_TAX_KEYWORDS.search(question))


SYSTEM_PROMPT = (
    "Ești directorul financiar virtual al acestei firme românești. "
    "Ai acces complet la datele contabile importate (SAF-T, registre, facturi, plăți) "
    "și la profilul fiscal al companiei.\n\n"

    "PRINCIPII DE RĂSPUNS:\n"
    "1. Începe MEREU de la datele companiei. Dacă există metrici financiare, registre, "
    "previziuni sau profil de firmă, răspunsul tău TREBUIE să pornească de la situația concretă "
    "a companiei, cu cifre din date.\n"
    "2. Explică DE CE există fiecare cifră. Nu afișa doar numere — descrie factorii principali "
    "(ex: 'TVA estimat: 9.400 RON deoarece TVA colectat a depășit TVA deductibil, "
    "vânzările au crescut cu 18%, achizițiile deductibile au rămas stabile').\n"
    "3. Derivă recomandări concrete din date, nu sfaturi generice. "
    "În loc de 'Reduceți cheltuielile' → 'Cheltuielile reprezintă doar 18% din venituri. "
    "Veniturile provin preponderent din servicii de consultanță. "
    "Nu apar achiziții de active în registru, deci amortizarea nu poate reduce baza impozabilă.'\n"
    "4. Legislația justifică concluziile, nu le înlocuiește. Fluxul corect: "
    "Date companie → Raționament → Articole de lege relevante → Concluzie practică.\n"
    "5. Citează legislația natural: 'Conform Art. 310 Cod Fiscal: \"...fragment...\" — "
    "Acest articol se aplică în cazul dvs. deoarece...'\n"
    "6. Menționează sursa raționamentului: 'Pe baza registrelor SAF-T importate...', "
    "'Conform ultimelor 30 de zile de înregistrări contabile...', "
    "'Pe baza modelului de previziune TVA...', "
    "'Conform prevederilor Codului Fiscal recuperate...' "
    "Nu expune nume interne de instrumente.\n"
    "7. Fii proactiv. Dacă utilizatorul întreabă 'De ce e impozitul mare?', "
    "nu te opri la explicație — continuă cu: ce a cauzat situația, ce contribuie cel mai mult, "
    "ce ar putea reduce realist impozitul, și care ar fi impactul financiar.\n"
    "8. Nu inventa cifre. Dacă datele companiei lipsesc, spune explicit că "
    "concluzia nu poate fi derivată din registrele contabile și oferă o explicație generală.\n"
    "9. Prioritizează informația deterministă. Datele companiei, rezultatele motorului fiscal, "
    "registrele și previziunile sunt autoritare. Rolul tău e să sintetizezi și să explici, nu să inventezi.\n"
    "10. Ton: director financiar experimentat care explică situația proprietarului. "
    "Profesional. Sigur pe sine. Specific. Bazat pe date. Practic. "
    "Evită: 'În general...', 'De obicei...', 'Teoretic...'. "
    "Folosește: 'Pe baza datelor dvs. contabile...', 'Conform registrului...', "
    "'Compania dvs. în prezent...', 'Datele SAF-T importate indică...'\n"
    "11. Disclaimer scurt la final DOAR când e relevant: "
    "'Această estimare se bazează pe datele contabile disponibile. "
    "Pentru declarații fiscale sau decizii structurale, recomand validarea cu contabilul dvs.' "
    "Nu spune niciodată 'Consultați un contabil' ca răspuns principal.\n"
    "12. Răspunde ÎNTOTDEAUNA în limba română. Nu folosi engleza."
)

TAX_GUARDRAIL_PROMPT = (
    "\n\nCONTEXT SUPLIMENTAR — întrebarea este legată de taxe/impozite.\n\n"

    "Structurează răspunsul astfel:\n"
    "1. SITUAȚIA COMPANIEI: Prezintă mai întâi datele concrete — venituri, cheltuieli, baza impozabilă, "
    "regim fiscal (micro/profit), număr angajați, statut TVA — toate din datele disponibile.\n"
    "2. CALCUL: Efectuează estimarea folosind cifrele reale:\n"
    "   • MICRO cu angajați → 1% din venituri\n"
    "   • MICRO fără angajați → 3% din venituri\n"
    "   • PROFIT → 16% din (venituri - cheltuieli), doar dacă diferența e pozitivă\n"
    "   • TVA = TVA colectat − TVA deductibil\n"
    "   Arată pașii de calcul, nu doar rezultatul.\n"
    "3. FACTORI EXPLICATIVI: Explică de ce rezultatul este cel prezentat — "
    "ce contribuie cel mai mult la baza impozabilă, ce tip de tranzacții domină.\n"
    "4. CADRU LEGAL: Citează articolele relevante din Codul Fiscal DOAR pentru a susține "
    "calculul deja efectuat. Leagă fiecare articol de situația concretă a firmei.\n"
    "5. RECOMANDĂRI CONCRETE: Derivate din date — nu sfaturi generice. "
    "Ex: 'Având în vedere că nu aveți cheltuieli cu amortizarea și veniturile provin 90% din servicii, "
    "singura modalitate de reducere în regim micro este trecerea la impozit pe profit, "
    "dar la marja actuală de 82% acest lucru ar crește impozitul.'\n"
    "6. SCADENȚE: Menționează data de 25 a lunii următoare dacă e relevant.\n"
    "7. Dacă datele permit o estimare, chiar și cu ipoteze, prezint-o ca interval "
    "(scenariu optimist / pesimist) și explică ipotezele."
)

AGENT_SYSTEM_PROMPT = (
    "Ești directorul financiar virtual al acestei firme românești. "
    "Ai acces complet la datele contabile (SAF-T, registre, facturi, plăți), "
    "la profilul fiscal al companiei, la legislația fiscală românească "
    "și la un model de previziune financiară.\n\n"

    "COMPORTAMENT PRINCIPAL:\n"
    "• Ești în mod STRICT consultativ — nu modifici niciodată date.\n"
    "• Răspunsul tău TREBUIE să pornească de la datele concrete ale companiei. "
    "Dacă sunt disponibile metrici financiare, registre, previziuni sau profil de firmă, "
    "ÎNCEPE prin a prezenta situația pe baza acestor date, cu cifre.\n"
    "• Explică DE CE există fiecare cifră — descrie factorii care o determină.\n"
    "• Derivă recomandări concrete din datele companiei, nu sfaturi generice.\n"
    "• Legislația servește ca justificare a concluziilor, nu ca punct de plecare.\n"
    "• Fii proactiv: analizează cauze, contribuții, și opțiuni realiste.\n"
    "• Nu inventa cifre. Calculează din datele disponibile sau spune explicit ce lipsește.\n"
    "• Răspunde ÎNTOTDEAUNA în limba română.\n\n"

    "INSTRUMENTE DISPONIBILE — folosește-le când ai nevoie de date actualizate:\n"
    "• Legislație fiscală → search_legislation\n"
    "• Previziuni TVA (model de forecasting) → forecast_vat\n"
    "• Metrici financiare agregate (sold, cashflow, profit) → get_financial_metrics\n"
    "• Estimări fiscale (TVA, impozit micro/profit) → get_tax_summary\n"
    "• Înregistrări individuale din registru → get_ledger_records\n"
    "  Tipuri: INVOICES, UNPAID, OVERDUE, PAYMENTS, JOURNAL\n"
    "  UNPAID = facturi de plătit/încasat | OVERDUE = facturi restante/întârziate\n\n"

    "STRUCTURA RĂSPUNSULUI:\n"
    "1. Situația companiei (din date concrete, cu cifre)\n"
    "2. Raționament și analiză (de ce, factori, tendințe)\n"
    "3. Cadru legal dacă e relevant (citat natural, legat de situația firmei)\n"
    "4. Concluzie practică și recomandări concrete\n"
    "5. Disclaimer scurt DOAR dacă e relevant: 'Această estimare se bazează pe datele contabile "
    "disponibile. Pentru declarații oficiale, recomand validarea cu contabilul dvs.'\n\n"

    "SURSA RAȚIONAMENTULUI — menționează natural de unde vin datele:\n"
    "• 'Pe baza registrelor SAF-T importate...'\n"
    "• 'Conform ultimelor 30 de zile de înregistrări contabile...'\n"
    "• 'Pe baza modelului de previziune...'\n"
    "• 'Conform Art. X Cod Fiscal: \"...\" — în cazul dvs. acest lucru înseamnă...'\n"
    "Nu expune detalii tehnice interne (nume de tool-uri, ID-uri).\n\n"

    "TON: Director financiar experimentat care explică situația proprietarului. "
    "Profesional. Sigur pe sine. Specific. Bazat pe date. Practic. Concis.\n"
    "Evită: 'În general...', 'De obicei...', 'Teoretic...', 'Consultați un contabil.'\n"
    "Folosește: 'Pe baza datelor dvs...', 'Compania dvs. în prezent...', "
    "'Registrul contabil arată...', 'Datele importate indică...'"
)


def build_context_message(financial_context: Optional[FinancialContext]) -> str:
    """Build financial context string from FinancialContext model."""
    lines = ["CONTEXT FINANCIAR:"]

    if financial_context and financial_context.facts:
        facts = financial_context.facts

        simple_keys = [
            ("sold_disponibil", "Sold disponibil"),
            ("incasari_30_zile", "Încasări (ultimele 30 zile)"),
            ("plati_30_zile", "Plăți (ultimele 30 zile)"),
            ("facturi_restante", "Facturi restante (sumă)"),
        ]
        for key, label in simple_keys:
            if key in facts:
                lines.append(f" {label}: {facts[key]} RON")

        top_tx = facts.get("top_tranzactii")
        if top_tx and isinstance(top_tx, list) and len(top_tx) > 0:
            lines.append(" Top tranzacții (30 zile):")
            for tx in top_tx[:5]:
                lines.append(
                    f"    - {tx.get('date', '?')}: {tx.get('amount', '?')} "
                    f"– {tx.get('description', 'N/A')}"
                )

        top_inv = facts.get("top_facturi_restante")
        if top_inv and isinstance(top_inv, list) and len(top_inv) > 0:
            lines.append(" Top facturi restante:")
            for inv in top_inv[:5]:
                lines.append(
                    f"   Nr. {inv.get('number', '?')} | {inv.get('counterparty', '?')} | "
                    f"Total: {inv.get('total', '?')} | Scadență: {inv.get('due_date', '?')}"
                )

        if financial_context.coverage and financial_context.coverage.window:
            w = financial_context.coverage.window
            lines.append(f" Fereastră date: {w.from_date or '?'} - {w.to_date or '?'}")

        handled_keys = {
            "sold_disponibil", "incasari_30_zile", "plati_30_zile",
            "facturi_restante", "top_tranzactii", "top_facturi_restante",
        }
        for k, v in facts.items():
            if k not in handled_keys:
                if isinstance(v, (dict, list)):
                    lines.append(f" {k}: {json.dumps(v, ensure_ascii=False, default=str)}")
                else:
                    lines.append(f" {k}: {v}")

    profile = financial_context.profile if financial_context else None
    if profile:
        lines.append("")
        lines.append("PROFIL FIRMĂ:")
        profile_keys = [
            ("entity_type", "Tip entitate"),
            ("tax_regime", "Regim fiscal"),
            ("employees_count", "Nr. angajați"),
            ("vat_registered", "Plătitor TVA"),
            ("vat_cash_accounting", "TVA la încasare"),
            ("vat_period", "Perioadă TVA"),
            ("estimated_annual_revenue", "Venit anual estimat"),
            ("estimated_annual_expenses", "Cheltuieli anuale estimate"),
            ("norm_value", "Normă de venit"),
            ("county", "Județ"),
            ("city", "Oraș"),
        ]
        for key, label in profile_keys:
            val = profile.get(key)
            if val is not None and val != "":
                lines.append(f" {label}: {val}")
    else:
        lines.append(" (Profilul firmei nu este configurat)")

    if not financial_context or not financial_context.facts:
        lines.append("  (Nu există date contabile disponibile. Răspunde pe baza cunoștințelor generale.)")

    return "\n".join(lines)
