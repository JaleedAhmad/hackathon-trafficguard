import json
from models.schemas import RawSignal, SignalObject
from services.llm_service import call_llm
from services.logger import log_agent_event

class IngestionAgent:
    def __init__(self):
        pass

    async def run(self, raw_signals: list) -> list:
        """
        Accepts a list of raw signal objects (dicts or RawSignal models).
        Returns a list of normalized SignalObjects.
        """
        normalized_signals = []
        for signal in raw_signals:
            if isinstance(signal, dict):
                signal = RawSignal(**signal)
                
            log_agent_event("INGESTION_STEP", {
                "step": "Input received",
                "signal_id": signal.signal_id,
                "source": signal.source,
                "text": signal.text
            })
            
            prompt = f"""
            You are a helpful data processing assistant for an urban crisis intelligence system.
            Analyze the following report and return a JSON object with these exact keys:
            - "language_detected": one of "English", "Urdu", or "Roman Urdu" (or "Other")
            - "text_normalized": English translation of the text if it's not English, or a cleaned and normalized version of the English text.
            - "category_hint": one of "flooding", "accident", "congestion", "blockage", "checkpoint", or "infrastructure" based on the text.

            Text: {signal.text}
            """
            
            try:
                log_agent_event("INGESTION_STEP", {"step": "Calling LLM for detection and translation", "signal_id": signal.signal_id})
                
                # Ask Gemini to return JSON
                response_text = await call_llm(prompt, model="gemini-2.5-flash", require_json=True)
                if response_text.startswith("```json"):
                    response_text = response_text.split("```json")[1].split("```")[0].strip()
                elif response_text.startswith("```"):
                    response_text = response_text.split("```")[1].split("```")[0].strip()
                result = json.loads(response_text)
                
                language_detected = result.get("language_detected", "Unknown")
                text_normalized = result.get("text_normalized", signal.text)
                category_hint = result.get("category_hint", "infrastructure")
                
                log_agent_event("INGESTION_STEP", {
                    "step": "LLM processing complete",
                    "signal_id": signal.signal_id,
                    "language_detected": language_detected,
                    "text_normalized": text_normalized,
                    "category_hint": category_hint
                })
                
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
            except Exception as e:
                log_agent_event("INGESTION_ERROR", {
                    "signal_id": signal.signal_id,
                    "error": str(e)
                })
                
        return normalized_signals
