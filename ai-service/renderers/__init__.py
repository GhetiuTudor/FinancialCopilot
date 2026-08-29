"""
Response renderer abstraction layer.

Provides an OPTIONAL post-processing step for canonical AI responses.
The renderer NEVER modifies the underlying reasoning — it only
improves readability, fluency, and formatting of the final text.

Default mode: GPT-only (pass-through, identical to current behavior).
"""
