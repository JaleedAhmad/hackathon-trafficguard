import json
import math
import os
from datetime import datetime, timezone
from models.schemas import SignalObject, CrisisDetectionResult
from services.llm_service import call_llm
from services.logger import log_agent_event
from dotenv import load_dotenv

load_dotenv()

def haversine(lat1, lon1, lat2, lon2):
    R = 6371.0 # Radius of earth in km
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = (math.sin(dlat / 2) * math.sin(dlat / 2) +
         math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) *
         math.sin(dlon / 2) * math.sin(dlon / 2))
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    distance = R * c
    return distance

class TrustDetectionAgent:
    def __init__(self):
        pass
        
    async def run(self, signals: list) -> dict:
        """
        Receives a list of normalized SignalObjects.
        Returns a dict matching the CrisisDetectionResult schema.
        """
        log_agent_event("TRUST_DETECTION_START", {"step": "Starting trust scoring", "initial_signals_count": len(signals)})
        
        # Hardcoded mocks for testing simulation
        mock_signals = [
            SignalObject(
                signal_id="mock_cr1", text_normalized="heavy flooding", text_original="heavy flooding",
                language_detected="English", source_type="community_report",
                lat=24.8607, lng=67.0011, timestamp=datetime.now(timezone.utc).isoformat(), category_hint="flooding"
            ),
            SignalObject(
                signal_id="mock_cr2", text_normalized="water on road", text_original="water on road",
                language_detected="English", source_type="community_report",
                lat=24.8608, lng=67.0012, timestamp=datetime.now(timezone.utc).isoformat(), category_hint="flooding"
            ),
            SignalObject(
                signal_id="mock_cr3", text_normalized="canal road flooded", text_original="canal road flooded",
                language_detected="English", source_type="community_report",
                lat=24.8606, lng=67.0010, timestamp=datetime.now(timezone.utc).isoformat(), category_hint="flooding"
            ),
            SignalObject(
                signal_id="mock_ws1", text_normalized="heavy_rain", text_original="heavy_rain",
                language_detected="English", source_type="weather_signal",
                lat=24.8607, lng=67.0011, timestamp=datetime.now(timezone.utc).isoformat(), category_hint="flooding"
            ),
            SignalObject(
                signal_id="mock_ms1", text_normalized="congestion_spike_340", text_original="congestion_spike_340",
                language_detected="English", source_type="maps_signal",
                lat=24.8607, lng=67.0011, timestamp=datetime.now(timezone.utc).isoformat(), category_hint="congestion"
            )
        ]
        signals.extend(mock_signals)
        
        community_reports = [s for s in signals if s.source_type == "community_report"]
        weather_signals = [s for s in signals if s.source_type == "weather_signal"]
        maps_signals = [s for s in signals if s.source_type == "maps_signal"]
        
        score = 30
        reasoning_steps = [f"Base score starts at 30."]
        contradictions = []
        
        log_agent_event("TRUST_SCORING", {"step": "Base score", "score": score})
        
        # 1. Geospatial clustering: +20 if 3+ reports within 500m
        cluster_found = False
        if len(community_reports) >= 3:
            for i, r1 in enumerate(community_reports):
                close_count = 1
                for j, r2 in enumerate(community_reports):
                    if i != j and haversine(r1.lat, r1.lng, r2.lat, r2.lng) <= 0.5:
                        close_count += 1
                if close_count >= 3:
                    cluster_found = True
                    break
        if cluster_found:
            score += 20
            log_agent_event("TRUST_SCORING", {"step": "Geospatial clustering", "score_change": 20, "new_score": score})
            reasoning_steps.append("3+ community reports found within 500m (+20).")
            
        # 2. Weather confirmation: +15 if heavy_rain + flooding
        has_heavy_rain = any("rain" in s.text_normalized.lower() or "rain" in s.text_original.lower() for s in weather_signals)
        has_flooding_report = any(s.category_hint == "flooding" for s in community_reports)
        if has_heavy_rain and has_flooding_report:
            score += 15
            log_agent_event("TRUST_SCORING", {"step": "Weather confirmation", "score_change": 15, "new_score": score})
            reasoning_steps.append("Weather signal confirmed heavy rain (+15).")
            
        # 3. Maps API confirms: +15 if congestion
        has_congestion = any("congestion" in s.text_normalized.lower() or "congestion" in s.text_original.lower() for s in maps_signals)
        if has_congestion:
            score += 15
            log_agent_event("TRUST_SCORING", {"step": "Maps API confirmation", "score_change": 15, "new_score": score})
            reasoning_steps.append("Maps API confirmed congestion spike (+15).")
            
        # 4. Additional corroborating reports: +10 per additional (max +20)
        # 5. Contradictions: -25 if report contradicts majority
        categories = [s.category_hint for s in community_reports]
        if categories:
            majority_category = max(set(categories), key=categories.count)
            majority_count = categories.count(majority_category)
            
            if majority_count > 1:
                additional = majority_count - 1
                bonus = min(additional * 10, 20)
                score += bonus
                log_agent_event("TRUST_SCORING", {"step": f"{additional} corroborating reports", "score_change": bonus, "new_score": score})
                reasoning_steps.append(f"{additional} additional corroborating report(s) found (+{bonus}).")
                
            for r in community_reports:
                if r.category_hint != majority_category and r.category_hint != "unknown":
                    score -= 25
                    contradiction_msg = f"Report '{r.signal_id}' ({r.category_hint}) contradicts majority ({majority_category})"
                    contradictions.append(contradiction_msg)
                    log_agent_event("TRUST_SCORING", {"step": "Contradiction found", "score_change": -25, "new_score": score, "message": contradiction_msg})
                    reasoning_steps.append(f"Contradiction detected: {contradiction_msg} (-25).")
        
        if not contradictions:
            reasoning_steps.append("No contradictions detected.")
            
        # Cap score 0-100
        score = max(0, min(100, score))
        log_agent_event("TRUST_SCORING", {"step": "Final score calculation", "final_score": score})
        
        status = "DISMISS"
        if score >= 60:
            status = "ACTIVE"
        elif 40 <= score <= 59:
            status = "MONITOR"
            
        log_agent_event("TRUST_SCORING", {"step": "Status determined", "status": status})
            
        # Use Gemini 2.5 Pro to classify confirmed crisis type
        prompt = f"""
        Based on these normalized signals and reasoning steps, classify the crisis type into one of exactly these options:
        urban_flooding, road_blockage, accident, protest, infrastructure_failure, congestion.
        
        Signals: {[s.model_dump() if hasattr(s, 'model_dump') else s.dict() for s in signals]}
        Reasoning: {reasoning_steps}
        
        Return ONLY a JSON object with this exact key:
        - "crisis_type": the classified type
        """
        
        crisis_type = "unknown"
        try:
            log_agent_event("LLM_CLASSIFICATION", {"step": "Asking Gemini 2.5 Pro for crisis type classification"})
            response_text = await call_llm(prompt, require_json=True)
            if response_text.startswith("```json"):
                response_text = response_text.split("```json")[1].split("```")[0].strip()
            elif response_text.startswith("```"):
                response_text = response_text.split("```")[1].split("```")[0].strip()
            result = json.loads(response_text)
            crisis_type = result.get("crisis_type", "unknown")
            log_agent_event("LLM_CLASSIFICATION", {"step": "Classification result", "crisis_type": crisis_type})
        except Exception as e:
            log_agent_event("LLM_ERROR", {"error": str(e)})
            
        final_reasoning = " ".join(reasoning_steps) + f" Total: {score}."
            
        output = CrisisDetectionResult(
            crisis_detected=(status == "ACTIVE"),
            status=status,
            crisis_type=crisis_type,
            confidence_score=score,
            reasoning=final_reasoning,
            contradictions=contradictions
        )
        
        return output.model_dump() if hasattr(output, 'model_dump') else output.dict()
