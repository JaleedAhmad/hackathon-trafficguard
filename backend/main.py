from dotenv import load_dotenv
load_dotenv()
from fastapi import FastAPI
import json
import os
import logging
import time
from datetime import datetime, timezone
from models.schemas import RawSignal, SignalObject
from agents.ingestion_agent import IngestionAgent
from agents.trust_detection_agent import TrustDetectionAgent
from agents.situation_planning_agent import SituationPlanningAgent
from agents.execution_agent import ExecutionAgent
from services.firebase_service import get_latest_trace
from auth import create_auth_router

app = FastAPI(title="TrafficGuard AI Backend")

# Mount production auth routes at /auth
app.include_router(create_auth_router(), prefix="/auth")

ingestion_agent = IngestionAgent()
trust_agent = TrustDetectionAgent()
situation_agent = SituationPlanningAgent()
execution_agent = ExecutionAgent()


class AgentLogCaptureHandler(logging.Handler):
    """Intercepts python logs during a request to compile the Agent Trace export."""
    def __init__(self):
        super().__init__()
        self.logs = {"Agent1": [], "Agent2": [], "Agent3": [], "Agent4": []}
    def emit(self, record):
        msg = self.format(record)
        step = {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "type": "DECISION" if "reasoning" in msg.lower() or "score" in msg.lower() or "assessed" in msg.lower() else "OUTPUT",
            "message": msg,
            "data": {}
        }
        if "INGESTION_" in msg: self.logs["Agent1"].append(step)
        elif "TRUST_" in msg or "LLM_CLASSIFICATION" in msg: self.logs["Agent2"].append(step)
        elif "SITUATION_" in msg or "LLM_PLANNING" in msg: self.logs["Agent3"].append(step)
        elif "EXECUTION_" in msg: self.logs["Agent4"].append(step)

agent_log_handler = AgentLogCaptureHandler()
logging.getLogger("agent_events").addHandler(agent_log_handler)

@app.post("/report")
async def receive_report(signal: RawSignal):
    agent_log_handler.logs = {"Agent1": [], "Agent2": [], "Agent3": [], "Agent4": []}
    
    results = await ingestion_agent.run([signal])
    
    if not results:
        return {"report_id": signal.signal_id, "status": "failed", "message": "Ingestion failed"}
        
    ingestion_result = results[0]
    trust_result = await trust_agent.run(results)
    situation_result = await situation_agent.run(trust_result)
    
    ingestion_dict = ingestion_result.model_dump() if hasattr(ingestion_result, 'model_dump') else ingestion_result.dict()
    execution_result = await execution_agent.run(
        ingestion_dict,
        trust_result,
        situation_result,
        agent_log_handler.logs
    )
    
    return {
        "report_id": f"rep_{int(time.time())}",
        "status": "processed",
        "message": "Report received and processed by full pipeline",
        "estimated_processing_ms": 4500,
        "ingestion_result": ingestion_dict,
        "trust_result": trust_result,
        "situation_result": situation_result,
        "execution_result": execution_result
    }

@app.get("/crisis/current")
async def get_current_crisis():
    return {
      "crisis_id": "c001",
      "location": "Canal Road, Karachi",
      "lat": 24.8607,
      "lng": 67.0011,
      "type": "urban_flooding",
      "confidence": 91,
      "severity": "High",
      "status": "ACTIVE",
      "affected_radius_km": 2.4,
      "eta_increase_mins": 21,
      "signals": [
        {"source": "community_report", "count": 4, "cluster_radius_m": 300},
        {"source": "weather_api", "status": "heavy_rain", "confirmed": True},
        {"source": "maps_api", "congestion_spike_pct": 340, "confirmed": True}
      ],
      "trust_analysis": {
        "crisis_detected": True,
        "status": "ACTIVE",
        "crisis_type": "urban_flooding",
        "confidence_score": 91,
        "reasoning": "4 reports clustered within 300m (+40), weather API confirms heavy rain (+15), Maps API shows 340% congestion spike (+15), 2 additional corroborating reports (+20). No contradictions detected. Total: 91.",
        "contradictions": []
      },
      "situation_plan": {
        "severity": "High",
        "affected_radius_km": 2.4,
        "eta_impact_minutes": 21,
        "escalation_probability": "HIGH",
        "actions": [
          {
            "action_id": "act_001",
            "priority": 1,
            "type": "rerouting_alert",
            "description": "Reroute traffic via Shahrah-e-Faisal — estimated 60% congestion reduction",
            "estimated_impact": "High",
            "requires_api": True
          },
          {
            "action_id": "act_002",
            "priority": 2,
            "type": "coordination",
            "description": "Dispatch emergency services to Canal Road and Kalma Chowk",
            "estimated_impact": "High",
            "requires_api": True
          },
          {
            "action_id": "act_003",
            "priority": 3,
            "type": "communication",
            "description": "Push alert to 1,200 nearby users: heavy flooding ahead, use alternate route",
            "estimated_impact": "Medium",
            "requires_api": False
          }
        ],
        "ai_reasoning": "Severity classified as HIGH (confidence 91 > 85 threshold). Flooding crisis type sets affected radius at 2.4km. ETA impact calculated at 21 minutes based on severity multiplier. Escalation probability HIGH due to confidence > 80 and confirmed weather signal."
      }
    }

@app.post("/simulate")
async def simulate_action():
    return {
        "status": "Endpoint retired. Simulation is now handled dynamically by Agent 4 in POST /report."
    }

@app.get("/agents/trace")
async def get_agent_trace():
    trace = get_latest_trace()
    if trace:
        return trace
    return {
        "status": "no_trace_found_or_firebase_mocked",
        "message": "Run POST /report first to generate a full trace. Ensure FIREBASE_CREDENTIALS_PATH is set if you want persistent fetch."
    }

@app.get("/baseline/compare")
async def get_baseline_compare():
    # Uses mock data to show heuristic vs agentic pipeline side by side
    mock_file = os.path.join(os.path.dirname(__file__), "data", "mock_signals.json")
    with open(mock_file, "r") as f:
        data = json.load(f)
        
    s = data["signals"][0]
    raw_text = s.get("description", "")
    
    # Run Baseline Heuristic
    heuristic_result = execution_agent.simple_heuristic(raw_text)
    
    # Run Agentic Pipeline (Agent 1 + Agent 2 for comparison)
    signal_obj = RawSignal(
        signal_id=s.get("id", "unknown"),
        text=raw_text,
        source=s.get("type", "unknown"),
        lat=24.8607,
        lng=67.0011,
        timestamp=s.get("timestamp", ""),
        language="Unknown"
    )
    
    results = await ingestion_agent.run([signal_obj])
    agentic_result = {"detected": False, "type": "unknown", "confidence": "LOW"}
    if results:
        trust_result = await trust_agent.run(results)
        agentic_result = {
            "detected": trust_result.get("crisis_detected", False),
            "type": trust_result.get("crisis_type", "unknown"),
            "confidence": f"{trust_result.get('confidence_score', 0)}%"
        }
        
    return {
        "heuristic_result": heuristic_result,
        "agentic_result": agentic_result,
        "raw_input_text": raw_text
    }

@app.get("/alerts/nearby")
def get_nearby_alerts(lat: float = 24.8607, lng: float = 67.0011):
    return {
        "alerts": [
            {
                "alert_id": "alt_001",
                "type": "urban_flooding",
                "message": "Heavy flooding detected on Canal Road. Avoid the area.",
                "message_urdu": "کینال روڈ پر شدید سیلاب۔ اس علاقے سے بچیں۔",
                "distance_km": 1.3,
                "severity": "High",
                "timestamp": "2026-05-16T09:30:00Z",
                "alternate_route": "Shahrah-e-Faisal"
            }
        ]
    }
