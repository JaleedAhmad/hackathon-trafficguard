import json
import re

from models.schemas import RawSignal, SignalObject
from services.llm_service import call_llm
from services.logger import log_agent_event


class IngestionAgent:

    def __init__(self):
        pass

    async def run(self, raw_signals: list) -> list:

        normalized_signals = []

        for signal in raw_signals:

            response_text = None

            try:

                if isinstance(signal, dict):
                    signal = RawSignal(**signal)

                log_agent_event(
                    "INGESTION_STEP",
                    {
                        "step": "Input received",
                        "signal_id": signal.signal_id,
                        "source": signal.source,
                        "text": signal.text
                    }
                )

                prompt = f"""
You are a JSON API for urban crisis intelligence.

Return ONLY valid JSON.
Do not include markdown.
Do not include explanations.
Do not include code fences.

Required schema:
{{
  "language_detected": "English",
  "text_normalized": "normalized text",
  "category_hint": "flooding"
}}

Allowed category_hint values:
- flooding
- accident
- congestion
- blockage
- checkpoint
- infrastructure

Text:
{signal.text}
"""

                log_agent_event(
                    "INGESTION_STEP",
                    {
                        "step": "Calling LLM",
                        "signal_id": signal.signal_id
                    }
                )

                response_text = await call_llm(
                    prompt,
                    model="gemma3:27b",
                    require_json=True
                )

                print("\n=== RAW LLM RESPONSE ===")
                print(response_text)
                print("========================\n")

                # Extract JSON robustly
                match = re.search(r'\{.*\}', response_text, re.DOTALL)

                if not match:
                    raise ValueError(
                        f"No JSON object found in response: {response_text}"
                    )

                json_text = match.group(0)

                result = json.loads(json_text)

                language_detected = result.get(
                    "language_detected",
                    "Unknown"
                )

                text_normalized = result.get(
                    "text_normalized",
                    signal.text
                )

                category_hint = result.get(
                    "category_hint",
                    "infrastructure"
                )

                normalized = SignalObject(
                    signal_id=signal.signal_id or "unknown",
                    text_normalized=text_normalized,
                    text_original=signal.text,
                    language_detected=language_detected,
                    source_type=signal.source,
                    lat=signal.lat,
                    lng=signal.lng,
                    timestamp=signal.timestamp,
                    category_hint=category_hint
                )

                normalized_signals.append(normalized)

                log_agent_event(
                    "INGESTION_STEP",
                    {
                        "step": "Normalization complete",
                        "signal_id": signal.signal_id,
                        "category_hint": category_hint
                    }
                )

            except Exception as e:

                print("\n=== INGESTION ERROR ===")
                print(e)

                if response_text:
                    print("\n=== RAW RESPONSE ===")
                    print(response_text)

                print("========================\n")

                log_agent_event(
                    "INGESTION_ERROR",
                    {
                        "signal_id": signal.signal_id,
                        "error": str(e)
                    }
                )

                # FAILSAFE OBJECT
                fallback = SignalObject(
                    signal_id=signal.signal_id or "unknown",
                    text_normalized=signal.text,
                    text_original=signal.text,
                    language_detected="Unknown",
                    source_type=signal.source,
                    lat=signal.lat,
                    lng=signal.lng,
                    timestamp=signal.timestamp,
                    category_hint="infrastructure"
                )

                normalized_signals.append(fallback)

        return normalized_signals