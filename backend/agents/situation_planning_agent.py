import json
import os
from datetime import datetime, timezone
from models.schemas import ActionItem, SituationPlanResult
from services.llm_service import call_llm
from services.logger import log_agent_event
from dotenv import load_dotenv

load_dotenv()

class SituationPlanningAgent:
    def __init__(self):
        pass
        
    async def run(self, crisis_event: dict) -> dict:
        """
        Receives crisis event dict from Agent 2.
        Returns a dict matching SituationPlanResult schema.
        """
        log_agent_event("SITUATION_PLANNING_START", {"step": "Starting situation analysis"})
        
        confidence = crisis_event.get("confidence_score", 0)
        crisis_type = crisis_event.get("crisis_type", "unknown")
        
        # Determine Severity: Low (<50) / Medium (50-70) / High (70-85) / Critical (>85)
        severity = "Low"
        if confidence > 85:
            severity = "Critical"
        elif confidence >= 70:
            severity = "High"
        elif confidence >= 50:
            severity = "Medium"
            
        log_agent_event("SITUATION_ANALYSIS", {"step": "Severity assessment", "confidence": confidence, "severity": severity})
        
        # Determine Radius
        radius_map = {
            "flooding": 2.4,
            "urban_flooding": 2.4,
            "accident": 0.8,
            "congestion": 1.5,
            "blockage": 0.6,
            "road_blockage": 0.6,
            "protest": 1.0,
            "infrastructure_failure": 2.0
        }
        radius = radius_map.get(crisis_type, 1.0)
        log_agent_event("SITUATION_ANALYSIS", {"step": "Radius estimation", "crisis_type": crisis_type, "radius_km": radius})
        
        # Determine ETA impact: Critical=25, High=21, Medium=12, Low=5
        eta_map = {
            "Critical": 25,
            "High": 21,
            "Medium": 12,
            "Low": 5
        }
        eta_impact = eta_map.get(severity, 5)
        log_agent_event("SITUATION_ANALYSIS", {"step": "ETA impact estimation", "severity": severity, "eta_impact_minutes": eta_impact})
        
        # Escalation probability: HIGH if confidence > 80 AND weather confirmed
        escalation = "LOW"
        reasoning_str = crisis_event.get("reasoning", "")
        weather_confirmed = "weather" in reasoning_str.lower() or "rain" in reasoning_str.lower()
        if confidence > 80 and weather_confirmed:
            escalation = "HIGH"
        log_agent_event("SITUATION_ANALYSIS", {"step": "Escalation probability assessment", "confidence": confidence, "weather_confirmed": weather_confirmed, "escalation": escalation})
        
        # Use Gemini 2.5 Pro for response planning
        prompt = f"""
        Given the following urban crisis situation, generate a human-readable reasoning explaining the situation and create exactly 3 response actions ranked by priority.
        
        Crisis Event: {json.dumps(crisis_event)}
        Severity: {severity}
        Radius: {radius} km
        ETA Impact: {eta_impact} mins
        Escalation: {escalation}
        
        Action 1 (priority 1, Immediate): rerouting or evacuation
        Action 2 (priority 2, Coordination): emergency services or traffic authority
        Action 3 (priority 3, Communication): user push notifications
        
        Each action must include specific descriptions with Karachi road names.
        
        Return exactly a JSON object with this structure:
        {{
            "ai_reasoning": "full Gemini-generated explanation of the situation and why these actions were chosen",
            "actions": [
                {{
                    "action_id": "act_001",
                    "priority": 1,
                    "type": "rerouting_alert",
                    "description": "specific description with Karachi road names",
                    "estimated_impact": "High",
                    "requires_api": true
                }},
                ... exactly 3 actions matching the constraints ...
            ]
        }}
        """
        
        ai_reasoning = "Standard automated protocol."
        actions = []
        
        try:
            log_agent_event("LLM_PLANNING", {"step": "Asking Gemini 2.5 Pro for actions and reasoning"})
            response_text = await call_llm(prompt, require_json=True)
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
            
            log_agent_event("LLM_PLANNING", {"step": "Successfully generated actions and reasoning", "actions_count": len(actions)})
        except Exception as e:
            log_agent_event("LLM_ERROR", {"error": str(e)})
            actions = [
                ActionItem(action_id="act_001", priority=1, type="rerouting_alert", description="Reroute traffic via Shahrah-e-Faisal", estimated_impact="High", requires_api=True),
                ActionItem(action_id="act_002", priority=2, type="coordination", description="Dispatch emergency services to Canal Road", estimated_impact="High", requires_api=True),
                ActionItem(action_id="act_003", priority=3, type="communication", description="Push alert to nearby users: flooding ahead", estimated_impact="Medium", requires_api=False)
            ]
            
        result_obj = SituationPlanResult(
            severity=severity,
            affected_radius_km=str(radius),
            eta_impact_minutes=eta_impact,
            escalation_probability=escalation,
            actions=actions,
            ai_reasoning=ai_reasoning
        )
        
        output = result_obj.model_dump() if hasattr(result_obj, 'model_dump') else result_obj.dict()
        output["affected_radius_km"] = radius
        return output
