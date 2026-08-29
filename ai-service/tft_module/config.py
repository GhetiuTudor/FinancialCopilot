from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent
DATA_PATH = BASE_DIR / "tft_romanian_financial_dataset.csv"
CHECKPOINT_DIR = BASE_DIR / "checkpoints"
CHECKPOINT_DIR.mkdir(parents=True, exist_ok=True)


TARGET = "monthly_vat_target"

MAX_ENCODER_LENGTH = 60

MAX_PREDICTION_LENGTH = 1


BATCH_SIZE = 64


MAX_EPOCHS = 10

LEARNING_RATE = 1e-2

HIDDEN_SIZE = 16

ATTENTION_HEAD_SIZE = 4


DROPOUT = 0.1

HIDDEN_CONTINUOUS_SIZE = 8

QUANTILES = [0.1, 0.5, 0.9]

STATIC_CATEGORICALS = ["sector"]

TIME_VARYING_KNOWN_REALS = [
    "month",
    "quarter",
    "day_of_week",
    "is_weekend",
    "is_holiday",
    "macro_index",
]

TIME_VARYING_UNKNOWN_REALS = [
    "revenue",
    "expenses",
    "profit",
    "profit_margin",
    "vat_due",
    "tax_due",
    "total_tax_liability",
]

GROUP_IDS = ["company_id"]


TIME_IDX = "time_idx"
