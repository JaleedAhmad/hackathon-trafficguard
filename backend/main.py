from dotenv import load_dotenv
load_dotenv()
from fastapi import FastAPI, Depends
from pydantic import BaseModel
import json
import os
import logging
import time
import math
from datetime import datetime, timezone
from models.schemas import RawSignal, SignalObject
from agents.ingestion_agent import IngestionAgent
from agents.trust_detection_agent import TrustDetectionAgent
from agents.situation_planning_agent import SituationPlanningAgent
from agents.execution_agent import ExecutionAgent
from services.firebase_service import get_latest_trace, save_report, get_all_reports, get_current_crisis_firestore, increment_user_reputation
from auth import create_auth_router, get_current_user, FirebaseUser

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

class DuplicateCheckRequest(BaseModel):
    category: str
    lat: float
    lng: float

def calculate_distance_m(lat1: float, lng1: float, lat2: float, lng2: float) -> float:
    # Approximate distance in meters using Haversine
    R = 6371000.0 # Earth radius in meters
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lng2 - lng1)
    
    a = math.sin(delta_phi / 2) * math.sin(delta_phi / 2) + \
        math.cos(phi1) * math.cos(phi2) * \
        math.sin(delta_lambda / 2) * math.sin(delta_lambda / 2)
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return R * c

@app.post("/report/check-duplicate")
def check_duplicate(req: DuplicateCheckRequest):
    matches = []
    reports = get_all_reports()
    for rep in reports:
        dist = calculate_distance_m(req.lat, req.lng, rep.get("lat", 0.0), rep.get("lng", 0.0))
        # Match category (case-insensitive) and within 100 meters
        if rep.get("type", "").upper() == req.category.upper() and dist <= 100.0:
            matches.append({
                "alert_id": rep.get("report_id", ""),
                "type": rep.get("type", ""),
                "message": rep.get("message", ""),
                "distance_km": dist / 1000.0,
                "severity": rep.get("severity", "High"),
                "timestamp": rep.get("timestamp", "")
            })
    return {"duplicates": matches}

@app.post("/report")
async def receive_report(signal: RawSignal, current_user: FirebaseUser = Depends(get_current_user)):
    agent_log_handler.logs = {"Agent1": [], "Agent2": [], "Agent3": [], "Agent4": []}
    
    results = await ingestion_agent.run([signal])

    print(f"ingestion results: {results}")
    print(f"signal: {signal}")
    
    if not results:
        return {"report_id": signal.signal_id, "status": "failed", "message": "Ingestion failed"}
        
    ingestion_result = results[0]
    trust_result = await trust_agent.run(results)
    print(f"trust_result: {trust_result}")

    situation_result = await situation_agent.run(trust_result)
    print(f"situation_result: {situation_result}")
    
    ingestion_dict = ingestion_result.model_dump() if hasattr(ingestion_result, 'model_dump') else ingestion_result.dict()
    execution_result = await execution_agent.run(
        ingestion_dict,
        trust_result,
        situation_result,
        agent_log_handler.logs
    )
    
    print(f"execution_result: {execution_result}")

    report_id = f"rep_{int(time.time())}"
    
    category = "TRAFFIC"
    if "flood" in signal.text.lower():
        category = "FLOOD"
    elif "accident" in signal.text.lower() or "crash" in signal.text.lower():
        category = "ACCIDENT"
    elif "weather" in signal.text.lower():
        category = "WEATHER"
        
    report_doc = {
        "report_id": report_id,
        "type": category,
        "message": signal.text,
        "severity": "High",
        "lat": signal.lat,
        "lng": signal.lng,
        "timestamp": signal.timestamp,
        "user_id": current_user.uid
    }
    save_report(report_doc)
    increment_user_reputation(current_user.uid, 10)
    
    return {
        "report_id": report_id,
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
    firestore_crisis = get_current_crisis_firestore()
    if firestore_crisis:
        return firestore_crisis
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
    alerts = []
    # Dynamic search inside the live submitted reports in Firestore within 5 km
    reports = get_all_reports()
    for rep in reports:
        dist = calculate_distance_m(lat, lng, rep.get("lat", 0.0), rep.get("lng", 0.0))
        if dist <= 5000.0:
            alerts.append({
                "alert_id": rep.get("report_id", ""),
                "type": rep.get("type", "TRAFFIC").lower(),
                "message": rep.get("message", ""),
                "message_urdu": "",
                "distance_km": dist / 1000.0,
                "severity": rep.get("severity", "High"),
                "timestamp": rep.get("timestamp", ""),
                "alternate_route": "",
                "lat": rep.get("lat", 0.0),
                "lng": rep.get("lng", 0.0)
            })
    return {"alerts": alerts}

# ─────────────────────────────────────────────
# NEW ENDPOINTS FOR VOTES, COMMENTS, LEADERBOARD
# ─────────────────────────────────────────────

class VoteRequest(BaseModel):
    is_upvote: bool

class CommentRequest(BaseModel):
    text: str

@app.post("/report/{report_id}/vote")
def post_report_vote(
    report_id: str,
    req: VoteRequest,
    current_user: FirebaseUser = Depends(get_current_user)
):
    from services.firebase_service import save_vote, increment_user_reputation, get_all_reports
    # Save the vote
    success = save_vote(report_id, current_user.uid, req.is_upvote)
    if not success:
        return {"status": "failed", "message": "Failed to save vote"}
        
    # Increment voter's reputation by 2 points
    increment_user_reputation(current_user.uid, 2)
    
    # Increment reporter's reputation by 5 points if report is found
    reports = get_all_reports()
    reporter_id = None
    for rep in reports:
        if rep.get("report_id") == report_id:
            reporter_id = rep.get("user_id")
            break
    if reporter_id and reporter_id != current_user.uid:
        increment_user_reputation(reporter_id, 5)
        
    return {"status": "success", "message": "Vote recorded and reputation updated"}

@app.get("/report/{report_id}/votes")
def get_report_votes(report_id: str):
    from services.firebase_service import get_vote_stats
    stats = get_vote_stats(report_id)
    return stats

@app.post("/report/{report_id}/comment")
def post_report_comment(
    report_id: str,
    req: CommentRequest,
    current_user: FirebaseUser = Depends(get_current_user)
):
    from services.firebase_service import add_comment, increment_user_reputation
    comment = add_comment(
        report_id=report_id,
        uid=current_user.uid,
        display_name=current_user.display_name,
        text=req.text
    )
    # Award commenting user 2 reputation points
    increment_user_reputation(current_user.uid, 2)
    return {"status": "success", "comment": comment}

@app.get("/report/{report_id}/comments")
def get_report_comments(report_id: str):
    from services.firebase_service import get_comments
    comments = get_comments(report_id)
    return {"comments": comments}

@app.get("/leaderboard")
def get_leaderboard_route():
    from services.firebase_service import get_leaderboard_firestore
    list_users = get_leaderboard_firestore()
    return {"leaderboard": list_users}

@app.get("/user/rank")
def get_user_rank_route(current_user: FirebaseUser = Depends(get_current_user)):
    from services.firebase_service import get_user_rank_firestore
    user_rank = get_user_rank_firestore(current_user.uid)
    return user_rank
