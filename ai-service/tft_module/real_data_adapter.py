import logging
import xml.etree.ElementTree as ET
from datetime import date, timedelta
from typing import Optional

import pandas as pd
import requests

from .config import (
    TARGET,
    MAX_ENCODER_LENGTH,
    TIME_IDX,
    STATIC_CATEGORICALS,
    TIME_VARYING_KNOWN_REALS,
    TIME_VARYING_UNKNOWN_REALS,
    GROUP_IDS,
)
from .holidays import get_holidays

logger = logging.getLogger(__name__)

BNR_YEARLY_URL = "https://www.bnr.ro/files/xml/years/nbrfxrates{year}.xml"
BNR_CURRENT_URL = "https://www.bnr.ro/nbrfxrates.xml"
BNR_NS = {"bnr": "http://www.bnr.ro/xsd"}

DEFAULT_SECTOR = "Retail"

VALID_SECTORS = {"Retail", "Manufacturing", "Services", "IT", "Construction"}


def _fetch_bnr_rates_for_year(year: int) -> dict[date, float]:
    url = BNR_YEARLY_URL.format(year=year)
    try:
        resp = requests.get(url, timeout=10)
        resp.raise_for_status()
    except requests.exceptions.SSLError:
        logger.warning("SSL error for BNR, retrying without verification for year %d", year)
        try:
            resp = requests.get(url, timeout=10, verify=False)
            resp.raise_for_status()
        except Exception as e:
            logger.warning("Failed to fetch BNR rates for %d (no-verify): %s", year, e)
            return {}
    except Exception as e:
        logger.warning("Failed to fetch BNR rates for %d: %s", year, e)
        return {}

    rates = {}
    try:
        root = ET.fromstring(resp.content)
        for cube in root.findall(".//bnr:Cube", BNR_NS):
            date_str = cube.get("date")
            if not date_str:
                continue
            for rate_elem in cube.findall("bnr:Rate", BNR_NS):
                if rate_elem.get("currency") == "EUR":
                    try:
                        rates[date.fromisoformat(date_str)] = float(rate_elem.text)
                    except (ValueError, TypeError):
                        pass
    except ET.ParseError as e:
        logger.warning("Failed to parse BNR XML for %d: %s", year, e)

    return rates


def fetch_bnr_macro_index(start_date: date, end_date: date) -> dict[date, float]:
    years = set(range(start_date.year, end_date.year + 1))
    raw_rates = {}
    for year in years:
        raw_rates.update(_fetch_bnr_rates_for_year(year))

    if not raw_rates:
        logger.warning("No BNR rates available, using constant macro_index=1.0")
        result = {}
        current = start_date
        while current <= end_date:
            result[current] = 1.0
            current += timedelta(days=1)
        return result

    all_dates = sorted(raw_rates.keys())
    full_index = {}
    current = start_date
    last_rate = raw_rates.get(all_dates[0], 5.0)

    while current <= end_date:
        if current in raw_rates:
            last_rate = raw_rates[current]
        full_index[current] = last_rate
        current += timedelta(days=1)

    base_rate = full_index.get(start_date, list(full_index.values())[0])
    if base_rate == 0:
        base_rate = 1.0

    return {d: rate / base_rate for d, rate in full_index.items()}


def fetch_daily_financials(
    company_id: str,
    backend_url: str,
    days: int = 400,
) -> list[dict]:
    url = f"{backend_url}/internal/v1/forecast-data"
    logger.info("Fetching forecast data: GET %s (company_id=%s, days=%d)", url, company_id, days)

    response = requests.get(
        url,
        params={"days": days},
        headers={
            "Content-Type": "application/json",
            "X-Company-Id": company_id,
        },
        timeout=15,
    )
    response.raise_for_status()

    data = response.json()
    daily_data = data.get("dailyData", data.get("daily_data", []))

    logger.info(
        "Received %d daily rows for company %s (total_days=%s)",
        len(daily_data), company_id, data.get("totalDays", "?"),
    )
    return daily_data


def build_tft_dataframe(
    daily_data: list[dict],
    company_id: str,
    sector: str = DEFAULT_SECTOR,
) -> pd.DataFrame:
    if not daily_data:
        raise ValueError("No daily data to build DataFrame from")

    if sector not in VALID_SECTORS:
        logger.warning("Sector '%s' not in training set, defaulting to '%s'", sector, DEFAULT_SECTOR)
        sector = DEFAULT_SECTOR

    rows = []
    for entry in daily_data:
        d = entry.get("date")
        if not d:
            continue
        rows.append({
            "date": pd.to_datetime(d),
            "revenue": float(entry.get("revenue", 0) or 0),
            "expenses": float(entry.get("expenses", 0) or 0),
            "vat_collected": float(entry.get("vatCollected", entry.get("vat_collected", 0)) or 0),
            "vat_deductible": float(entry.get("vatDeductible", entry.get("vat_deductible", 0)) or 0),
        })

    df = pd.DataFrame(rows)
    df = df.sort_values("date").reset_index(drop=True)

    date_range = pd.date_range(df["date"].min(), df["date"].max(), freq="D")
    df = df.set_index("date").reindex(date_range).fillna(0).reset_index()
    df = df.rename(columns={"index": "date"})

    df["profit"] = df["revenue"] - df["expenses"]
    df["profit_margin"] = df.apply(
        lambda r: r["profit"] / r["revenue"] if r["revenue"] > 0 else 0.0, axis=1
    )
    df["vat_due"] = df["vat_collected"] - df["vat_deductible"]
    df["tax_due"] = df["profit"].clip(lower=0) * 0.16
    df["total_tax_liability"] = df["vat_due"] + df["tax_due"]

    df["month"] = df["date"].dt.month
    df["quarter"] = df["date"].dt.quarter
    df["day_of_week"] = df["date"].dt.dayofweek
    df["is_weekend"] = (df["day_of_week"] >= 5).astype(int)

    years_in_data = df["date"].dt.year.unique()
    all_holidays = set()
    for year in years_in_data:
        all_holidays.update(get_holidays(int(year)))
    df["is_holiday"] = df["date"].dt.date.isin(all_holidays).astype(int)

    start_date = df["date"].min().date()
    end_date = df["date"].max().date()
    macro_index = fetch_bnr_macro_index(start_date, end_date)
    df["macro_index"] = df["date"].dt.date.map(macro_index).fillna(1.0)

    df["sector"] = sector
    df["company_id"] = str(company_id)

    df["time_idx"] = range(len(df))

    df["monthly_vat_target"] = (
        df["vat_due"]
        .shift(-1)
        .rolling(window=30, min_periods=30)
        .sum()
        .shift(-29)
    )
    df["monthly_vat_target"] = df["monthly_vat_target"].ffill().fillna(0)

    df["sector"] = df["sector"].astype(str)
    df["company_id"] = df["company_id"].astype(str)

    df = df.drop(columns=["vat_collected", "vat_deductible"], errors="ignore")

    expected_cols = [
        "company_id", "sector", "date", "time_idx",
        "revenue", "expenses", "profit", "profit_margin",
        "vat_due", "tax_due", "total_tax_liability",
        "month", "quarter", "day_of_week", "is_weekend", "is_holiday", "macro_index",
        "monthly_vat_target",
    ]
    df = df[expected_cols]

    logger.info(
        "Built TFT DataFrame: %d rows, %d columns, date range %s to %s",
        len(df), len(df.columns), df["date"].min(), df["date"].max(),
    )

    return df


def fetch_and_build_dataframe(
    company_id: str,
    backend_url: str,
    sector: str = DEFAULT_SECTOR,
    days: int = 400,
) -> pd.DataFrame:
    from .exceptions import InsufficientDataError

    daily_data = fetch_daily_financials(company_id, backend_url, days)

    if not daily_data:
        raise InsufficientDataError(f"Backend returned no daily data for company {company_id}")

    df = build_tft_dataframe(daily_data, company_id, sector)

    if len(df) < MAX_ENCODER_LENGTH:
        raise InsufficientDataError(
            f"Insufficient data for TFT: {len(df)} rows, need at least {MAX_ENCODER_LENGTH}. "
            f"Company needs more journal history."
        )

    return df
