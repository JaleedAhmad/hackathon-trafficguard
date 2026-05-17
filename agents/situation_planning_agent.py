import json
import logging
import os
from datetime import datetime, timezone
from models.schemas import ActionItem, SituationPlanResult
from services.llm_service import call_llm
from dotenv import load_dotenv

load_dotenv()
logger = logging.getLogger(__name__)

class SituationPlanningAgent:
    def __init__(self):
        pass
        
    def _log_with_ts(self, msg: str):
        ts = datetime.now(timezone.utc).isoformat()
        logger.info(f"[{ts}] Situation Planning Agent: {msg}")

    async def run(self, crisis_event: dict) -> dict:
        """
        Receives crisis event dict from Agent 2.
        Returns a dict matching SituationPlanResult schema.
        """
        self._log_with_ts("Starting situation analysis...")
        
        confidence = crisis_event.get("confidence_score", 0)
        crisis_type = crisis_event.get("crisis_type", "unknown")
        reasoning_str = crisis_event.get("reasoning", "")
        
        # Determine Severity
        severity = "Low"
        severity_multiplier = 1.0
        if confidence > 85:
            severity = "Critical"
            severity_multiplier = 2.5
        elif confidence >= 70:
            severity = "High"
            severity_multiplier = 1.8
        elif confidence >= 50:
            severity = "Medium"
            severity_multiplier = 1.3
            
        self._log_with_ts(f"Severity assessed as {severity} based on confidence {confidence}")
        
        # Determine Radius
        radius_map = {
            "flooding": "2-4km",
            "urban_flooding": "2-4km",
            "accident": "0.5-1km",
            "congestion": "1-2km",
            "blockage": "0.3-0.8km",
            "road_blockage": "0.3-0.8km"
        }
        radius = radius_map.get(crisis_type, "1-2km")
        self._log_with_ts(f"Affected radius estimated at {radius} for type {crisis_type}")
        
        # Determine ETA impact
        base_delay_map = {
            "flooding": 45,
            "urban_flooding": 45,
            "accident": 30,
            "congestion": 20,
            "blockage": 40,
            "road_blockage": 40
        }
        base_delay = base_delay_map.get(crisis_type, 15)
        eta_impact = int(severity_multiplier * base_delay)
        self._log_with_ts(f"ETA impact estimated at {eta_impact} minutes")
        
        # Escalation probability
        escalation = "LOW"
        if confidence > 80 and ("weather" in reasoning_str.lower() or "rain" in reasoning_str.lower() or "flooding" in reasoning_str.lower()):
            escalation = "HIGH"
        self._log_with_ts(f"Escalation probability determined as {escalation}")
        
        # Use Gemini 2.5 Pro for response planning and reasoning
        self._log_with_ts("Asking Gemini 2.5 Pro to generate response actions and reasoning...")
        
        prompt = f"""
        Given the following urban crisis situation, generate a human-readable reasoning explaining the situation and create exactly 3 response actions ranked by priority.
        
        Crisis Event: {json.dumps(crisis_event)}
        Severity: {severity}
        Radius: {radius}
        ETA Impact: {eta_impact} mins
        Escalation: {escalation}
        
        Action 1 (Immediate): Must be rerouting or evacuation alert
        Action 2 (Coordination): Must involve emergency services or traffic authority
        Action 3 (Communication): Must involve user push notifications or social alert
        
        Return exactly a JSON object with this structure:
        {{
            "ai_reasoning": "Human-readable reasoning explaining why these actions are being taken based on the severity, crisis type, and escalation probability.",
            "actions": [
                {{
                    "action_id": "act_001",
                    "priority": 1,
                    "type": "rerouting_alert",
                    "description": "...",
                    "estimated_impact": "...",
                    "requires_api": true
                }},
                ... exactly 3 actions matching the constraints ...
            ]
        }}
        """
        
        ai_reasoning = "Standard automated protocol."
        actions = []
        
        try:
            response_text = await call_llm(prompt, model="gemini-2.5-pro", require_json=True)
            if response_text.startswith("```json"):
                response_text = response_text.split("```json")[1].split("```")[0].strip()
            elif response_text.startswith("```"):
                response_text = response_text.split("```")[1].split("```")[0].strip()
            result = json.loads(response_text)
            ai_reasoning = result.get("ai_reasoning", ai_reasoning)
            actions_data = result.get("actions", [])
            
            for ad in actions_data:
                actions.append(ActionItem(
                    action_id=ad.get("action_id", "act_00x"),
                    priority=ad.get("priority", 3),
                    type=ad.get("type", "unknown"),
                    description=ad.get("description", ""),
                    estimated_impact=ad.get("estimated_impact", ""),
                    requires_api=ad.get("requires_api", False)
                ))
            
            self._log_with_ts("Successfully generated actions and reasoning from Gemini.")
        except Exception as e:
            self._log_with_ts(f"Error calling Gemini: {e}")
            actions = [
                ActionItem(action_id="act_001", priority=1, type="rerouting_alert", description="Immediate Rerouting Alert", estimated_impact="High", requires_api=True),
                ActionItem(action_id="act_002", priority=2, type="coordination", description="Notify Emergency Services", estimated_impact="Medium", requires_api=True),
                ActionItem(action_id="act_003", priority=3, type="communication", description="Push Notification to Users", estimated_impact="Low", requires_api=False)
            ]
            
        result_obj = SituationPlanResult(
            severity=severity,
            affected_radius_km=radius,
            eta_impact_minutes=eta_impact,
            escalation_probability=escalation,
            actions=actions,
            ai_reasoning=ai_reasoning
        )
        
        return result_obj.model_dump() if hasattr(result_obj, 'model_dump') else result_obj.dict()
