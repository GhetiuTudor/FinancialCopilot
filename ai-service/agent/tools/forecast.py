"""
Forecast tool — re-exports the existing TFT forecast_vat tool.

The tft_module already provides a @tool-decorated forecast_vat function.
"""

from tft_module.forecast_tool import forecast_vat

__all__ = ["forecast_vat"]
