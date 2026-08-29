from .config import BATCH_SIZE, MAX_EPOCHS
from .dataset import load_dataframe, build_datasets, build_dataloaders
from .model import build_model, build_trainer


def main():
    print("[1/4] Loading dataset...")
    df = load_dataframe()
    print(f"       Loaded {len(df)} rows, {df['company_id'].nunique()} companies")
    print(f"       Target: monthly_vat_target (30-day forward VAT sum)")
    print(f"       Target range: {df['monthly_vat_target'].min():.2f} — {df['monthly_vat_target'].max():.2f}")

    print("[2/4] Building TimeSeriesDataSets...")
    training, validation = build_datasets(df)
    train_loader, val_loader = build_dataloaders(training, validation, BATCH_SIZE)
    print(f"       Training samples: {len(training)}, Validation samples: {len(validation)}")

    print("[3/4] Building model (QuantileLoss → q10, q50, q90)...")
    model = build_model(training)
    total_params = sum(p.numel() for p in model.parameters())
    print(f"       Model parameters: {total_params:,}")

    print("[4/4] Training...")
    trainer = build_trainer(MAX_EPOCHS)
    trainer.fit(model, train_dataloaders=train_loader, val_dataloaders=val_loader)

    best_path = trainer.checkpoint_callback.best_model_path
    print(f"\nTraining complete")
    print(f"   Best checkpoint: {best_path}")
    print(f"   Best val_loss:   {trainer.checkpoint_callback.best_model_score:.4f}")


if __name__ == "__main__":
    main()
