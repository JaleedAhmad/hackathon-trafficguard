import json
import logging
import math
import os
from models.schemas import SignalObject, CrisisDetectionResult
from services.llm_service import call_llm
from dotenv import load_dotenv

load_dotenv()
logger = logging.getLogger(__name__)

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
        logger.info("Trust Detection Agent: Starting trust scoring...")
        
        community_reports = [s for s in signals if s.source_type == "community_report"]
        weather_signals = [s for s in signals if s.source_type == "weather_signal"]
        maps_signals = [s for s in signals if s.source_type == "maps_signal"]
        
        score = 30
        logger.info(f"Trust Detection Agent: Base score starts at {score}")
        
        reasoning_steps = []
        contradictions = []
        
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
            logger.info("Trust Detection Agent: +20 for 3+ reports within 500m")
            reasoning_steps.append("3+ community reports found within 500m.")
            
        # 2. Weather confirmation: +15 if heavy_rain + flooding
        # In mock data: weather_signal has 'heavy_rain'
        has_heavy_rain = any("rain" in s.text_normalized.lower() or "rain" in s.text_original.lower() for s in weather_signals)
        has_flooding_report = any(s.category_hint == "flooding" for s in community_reports)
        if has_heavy_rain and has_flooding_report:
            score += 15
            logger.info("Trust Detection Agent: +15 for weather signal confirmation (heavy rain + flooding)")
            reasoning_steps.append("Weather signal confirmed flooding conditions.")
            
        # 3. Maps API confirms: +15 if severe_congestion
        has_congestion = any("severe_congestion" in s.text_normalized.lower() or "congestion" in s.text_original.lower() or "congestion" in s.text_normalized.lower() for s in maps_signals)
        if has_congestion:
            score += 15
            logger.info("Trust Detection Agent: +15 for Maps API congestion confirmation")
            reasoning_steps.append("Maps API confirmed severe congestion.")
            
        # 4. Additional corroborating reports: +10 per additional (max +20)
        # Also 5. Contradictions: -25 if report contradicts majority
        categories = [s.category_hint for s in community_reports]
        if categories:
            majority_category = max(set(categories), key=categories.count)
            majority_count = categories.count(majority_category)
            
            if majority_count > 1:
                additional = majority_count - 1
                bonus = min(additional * 10, 20)
                score += bonus
                logger.info(f"Trust Detection Agent: +{bonus} for {additional} additional corroborating reports")
                reasoning_steps.append(f"{additional} additional corroborating report(s) found.")
                
            for r in community_reports:
                if r.category_hint != majority_category:
                    score -= 25
                    contradiction_msg = f"Report '{r.signal_id}' ({r.category_hint}) contradicts majority ({majority_category})"
                    contradictions.append(contradiction_msg)
                    logger.info(f"Trust Detection Agent: -25 for contradiction: {contradiction_msg}")
                    reasoning_steps.append(contradiction_msg)
        
        # Cap score 0-100
        score = max(0, min(100, score))
        logger.info(f"Trust Detection Agent: Final confidence score calculated: {score}")
        
        status = "DISMISS"
        if score >= 60:
            status = "ACTIVE"
        elif 40 <= score <= 59:
            status = "MONITOR"
            
        logger.info(f"Trust Detection Agent: Status determined as {status}")
            
        # Use Gemini 2.5 Pro to classify confirmed crisis type
        prompt = f"""
        Based on these normalized signals and reasoning steps, classify the crisis type into one of:
        urban_flooding, road_blockage, accident, protest, infrastructure_failure, congestion.
        
        Signals: {[s.model_dump() if hasattr(s, 'model_dump') else s.dict() for s in signals]}
        Reasoning: {reasoning_steps}
        
        Return ONLY a JSON object with this exact key:
        - "crisis_type": the classified type
        """
        
        crisis_type = "unknown"
        try:
            logger.info("Trust Detection Agent: Asking Gemini 2.5 Pro for crisis type classification...")
            response_text = await call_llm(prompt, model="gemini-2.5-pro", require_json=True)
            if response_text.startswith("```json"):
                response_text = response_text.split("```json")[1].split("```")[0].strip()
            elif response_text.startswith("```"):
                response_text = response_text.split("```")[1].split("```")[0].strip()
            result = json.loads(response_text)
            crisis_type = result.get("crisis_type", "unknown")
            logger.info(f"Trust Detection Agent: Gemini classified crisis as: {crisis_type}")
        except Exception as e:
            logger.error(f"Trust Detection Agent: Error calling Gemini: {e}")
            
        final_reasoning = " | ".join(reasoning_steps)
        if not final_reasoning:
            final_reasoning = "Insufficient signals for strong correlation."
            
        output = CrisisDetectionResult(
            crisis_detected=(status == "ACTIVE"),
            status=status,
            crisis_type=crisis_type,
            confidence_score=score,
            reasoning=final_reasoning,
            contradictions=contradictions
        )
        
        return output.model_dump() if hasattr(output, 'model_dump') else output.dict()
