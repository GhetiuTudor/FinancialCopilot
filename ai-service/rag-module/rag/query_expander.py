"""
QueryExpander — Domain-specific query expansion for Romanian fiscal/legal text.

Expands user queries with synonyms, acronym expansions, and normalized
article references to improve BM25 recall.

Design: The ORIGINAL query is used for embedding (semantic search),
        the EXPANDED query is used for BM25 (keyword search).
"""

import re


_FISCAL_SYNONYMS: dict[str, list[str]] = {
    "tva": ["taxa pe valoarea adăugată", "TVA", "valoare adăugată"],
    "impozit pe profit": ["impozit profit", "16%", "titlul II"],
    "microîntreprindere": [
        "micro", "microîntreprinderi", "impozit micro",
        "impozit pe veniturile microîntreprinderilor",
    ],
    "cas": ["contribuții asigurări sociale", "CAS", "contribuții sociale"],
    "cass": ["contribuții asigurări sociale de sănătate", "CASS"],
    "anaf": ["agenția națională de administrare fiscală", "ANAF", "fisc"],
    "declarație": ["declarații", "formular", "D300", "D100", "D112"],
    "scadență": ["scadent", "termen plată", "data limită", "scadentă"],
    "cod fiscal": ["codul fiscal", "legea 227/2015", "L227"],
    "contribuabil": ["contribuabili", "plătitor", "persoană impozabilă"],
    "venit": ["venituri", "câștig", "încasări"],
    "cheltuială": ["cheltuieli", "costuri", "plăți"],
    "impozit": ["impozite", "taxe", "taxă", "contribuție"],
    "deducere": ["deduceri", "deductibil", "deductibile", "cheltuieli deductibile"],
    "dividend": ["dividende", "distribuire profit"],
    "salariu": ["salarii", "venituri din salarii", "indemnizație"],
}

_ART_PATTERN = re.compile(
    r"\b(?:art\.?|articolul?)\s*\.?\s*(\d+)", re.IGNORECASE
)


def expand_query(query: str) -> str:
    """
    Expand query with domain-specific synonyms and normalized references.
    Returns the expanded query string (original + expansions appended).
    """
    expansions: set[str] = set()
    query_lower = query.lower()


    for term, synonyms in _FISCAL_SYNONYMS.items():
        if term in query_lower:
            expansions.update(synonyms)
        else:
            for syn in synonyms:
                if syn.lower() in query_lower:
                    expansions.add(term)
                    break

    # "art 7" → "ART. 7", "Articolul 7"
    for match in _ART_PATTERN.finditer(query):
        art_num = match.group(1)
        expansions.update([
            f"ART. {art_num}",
            f"Articolul {art_num}",
            f"Art. {art_num}",
        ])

    if expansions:
        return query + " " + " ".join(expansions)
    return query
