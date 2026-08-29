"""
TFT model builder for probabilistic monthly VAT forecasting.

Key changes from previous version:
- Loss function: MAE() → QuantileLoss([0.1, 0.5, 0.9])
- Output: deterministic daily values → quantile forecasts (q10, q50, q90)
- Checkpoint naming reflects the new architecture
"""

# pyrefly: ignore [missing-import]
import lightning.pytorch as pl
# pyrefly: ignore [missing-import]
from lightning.pytorch.callbacks import ModelCheckpoint
# pyrefly: ignore [missing-import]
from pytorch_forecasting import TemporalFusionTransformer
# pyrefly: ignore [missing-import]
from pytorch_forecasting.metrics import QuantileLoss

from .config import (
    LEARNING_RATE,
    HIDDEN_SIZE,
    ATTENTION_HEAD_SIZE,
    DROPOUT,
    HIDDEN_CONTINUOUS_SIZE,
    CHECKPOINT_DIR,
    QUANTILES,
)


def build_model(training_dataset):
    """
    Build a TFT configured for probabilistic sequence-to-one forecasting.

    Uses QuantileLoss to produce three forecast scenarios:
    - q10: pessimistic (10th percentile) — conservative low estimate
    - q50: median — most likely value
    - q90: optimistic (90th percentile) — high-risk scenario

    This replaces the previous MAE loss which produced only point forecasts.
    """
   
    model = TemporalFusionTransformer.from_dataset(
        training_dataset,
        learning_rate=LEARNING_RATE,         
        hidden_size=HIDDEN_SIZE,              
        attention_head_size=ATTENTION_HEAD_SIZE,  
        dropout=DROPOUT,                      
        hidden_continuous_size=HIDDEN_CONTINUOUS_SIZE,  
        loss=QuantileLoss(QUANTILES),

        log_interval=10,

        reduce_on_plateau_patience=3,
    )
    return model


def build_trainer(max_epochs: int):
    """Build a PyTorch Lightning trainer with checkpointing."""
    checkpoint_callback = ModelCheckpoint(
        dirpath=str(CHECKPOINT_DIR),
        filename="tft_monthly_vat_quantile",
        monitor="val_loss",  
        mode="min",        
        save_top_k=1,        
        verbose=True,        
    )
    trainer = pl.Trainer(
        max_epochs=max_epochs, 

        accelerator="auto",
        devices=1,  

        gradient_clip_val=0.1,
        enable_checkpointing=True,
        callbacks=[checkpoint_callback],
        logger=False,
    )
    return trainer