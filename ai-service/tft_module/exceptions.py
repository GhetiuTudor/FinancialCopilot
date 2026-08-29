class ModelLoadError(Exception):
    """Raised when the TFT model cannot be loaded from the checkpoint."""
    pass

class InsufficientDataError(ValueError):
    """Raised when there is insufficient historical data to run the forecast."""
    pass
