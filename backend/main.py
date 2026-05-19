from dotenv import load_dotenv
load_dotenv()
from fastapi import FastAPI, Depends, File, UploadFile
from pydantic import BaseModel
import json
import os
import logging
import time
import math
from datetime import datetime, timezone, timedelta
from models.schemas import RawSignal, SignalObject, UserProfileResponse, UpdateProfileRequest
from agents.ingestion_agent import IngestionAgent
from agents.trust_detection_agent import TrustDetectionAgent
from agents.situation_planning_agent import SituationPlanningAgent
from agents.execution_agent import ExecutionAgent
from services.firebase_service import get_latest_trace, save_report, get_all_reports, get_current_crisis_firestore, increment_user_reputation
from fastapi.middleware.cors import CORSMiddleware
from auth import create_auth_router, get_current_user, FirebaseUser

app = FastAPI(title="TrafficGuard AI Backend")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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
    if signal.category:
        category = signal.category.upper()
    else:
        if "flood" in signal.text.lower():
            category = "FLOOD"
        elif "accident" in signal.text.lower() or "crash" in signal.text.lower():
            category = "ACCIDENT"
        elif "weather" in signal.text.lower():
            category = "WEATHER"
        elif "other" in signal.text.lower():
            category = "OTHER"
            
    severity = "High"
    if signal.severity:
        severity = signal.severity.capitalize()
        
    report_doc = {
        "report_id": report_id,
        "type": category,
        "message": signal.text,
        "severity": severity,
        "lat": signal.lat,
        "lng": signal.lng,
        "timestamp": signal.timestamp,
        "user_id": current_user.uid
    }
    save_report(report_doc)
    increment_user_reputation(current_user.uid, 10)
    
    # Broadcast active navigation reconsider route alerts
    try:
        await navigation_manager.broadcast_reconsider_route(
            new_report_lat=signal.lat,
            new_report_lng=signal.lng,
            report_msg=signal.text
        )
    except Exception as n_err:
        print(f"Failed to broadcast reconsider navigation alert: {n_err}")
    
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
def get_nearby_alerts(
    lat: float = 24.8607,
    lng: float = 67.0011,
    title: str = None,
    date: str = None
):
    alerts = []
    reports = get_all_reports()
    for rep in reports:
        dist = calculate_distance_m(lat, lng, rep.get("lat", 0.0), rep.get("lng", 0.0))
        if dist <= 5000000.0:  # Expanded to allow all reports in Feed
            from services.firebase_service import get_vote_stats
            stats = get_vote_stats(rep.get("report_id", ""))
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
                "lng": rep.get("lng", 0.0),
                "confirmations": stats.get("upvotes", 0)
            })
            
    # Fetch and append SOS alerts within expanded range
    try:
        from services.firebase_service import get_all_sos
        sos_alerts = get_all_sos()
        for sos in sos_alerts:
            dist = calculate_distance_m(lat, lng, sos.get("lat", 0.0), sos.get("lng", 0.0))
            if dist <= 5000000.0:  # Expanded to allow all SOS in Feed
                alerts.append({
                    "alert_id": sos.get("sos_id", ""),
                    "type": "sos",
                    "message": f"SOS EMERGENCY: {sos.get('problem', '')}",
                    "message_urdu": "",
                    "distance_km": dist / 1000.0,
                    "severity": "Critical",
                    "timestamp": sos.get("timestamp", ""),
                    "alternate_route": "",
                    "lat": sos.get("lat", 0.0),
                    "lng": sos.get("lng", 0.0),
                    "confirmations": 0
                })
    except Exception as e:
        print(f"Error fetching SOS alerts: {e}")

    # ─── SORTING AND FILTERING ───
    # 1. Apply filters
    filtered_alerts = []
    for alert in alerts:
        # Title filter (checks message or type)
        if title:
            t_lower = title.lower()
            msg = alert.get("message", "").lower()
            if t_lower not in msg:
                continue
        # Date filter (expects YYYY-MM-DD)
        if date:
            ts = alert.get("timestamp", "")
            if date not in ts:
                continue
        filtered_alerts.append(alert)

    # 2. Sort: Past 24 hours first (newest first), then older (newest first)
    now = datetime.now(timezone.utc)
    twenty_four_hours_ago = now - timedelta(hours=24)
    
    def parse_timestamp_to_datetime(ts) -> datetime:
        if not ts:
            return datetime.min.replace(tzinfo=timezone.utc)
        if isinstance(ts, (int, float)):
            if ts > 1e11:
                ts /= 1000.0
            return datetime.fromtimestamp(ts, tz=timezone.utc)
        if isinstance(ts, str):
            ts = ts.strip()
            try:
                val = float(ts)
                if val > 1e11:
                    val /= 1000.0
                return datetime.fromtimestamp(val, tz=timezone.utc)
            except ValueError:
                pass
            for fmt in ("%Y-%m-%dT%H:%M:%S.%fZ", "%Y-%m-%dT%H:%M:%SZ", "%Y-%m-%d %H:%M:%S", "%Y-%m-%d"):
                try:
                    dt = datetime.strptime(ts, fmt)
                    if dt.tzinfo is None:
                        dt = dt.replace(tzinfo=timezone.utc)
                    return dt
                except ValueError:
                    continue
        return datetime.min.replace(tzinfo=timezone.utc)

    def get_sort_key(a):
        dt = parse_timestamp_to_datetime(a["timestamp"])
        is_recent = 1 if dt >= twenty_four_hours_ago else 0
        return (is_recent, dt.timestamp())

    # Sort descending: recent group (1) first, and within groups, newest first
    filtered_alerts.sort(key=get_sort_key, reverse=True)
        
    return {"alerts": filtered_alerts}

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

from fastapi import WebSocket, WebSocketDisconnect
import time

class DiscussionManager:
    def __init__(self):
        self.active_connections: dict[str, list[WebSocket]] = {}

    async def connect(self, report_id: str, websocket: WebSocket):
        await websocket.accept()
        if report_id not in self.active_connections:
            self.active_connections[report_id] = []
        self.active_connections[report_id].append(websocket)

    def disconnect(self, report_id: str, websocket: WebSocket):
        if report_id in self.active_connections:
            try:
                self.active_connections[report_id].remove(websocket)
            except ValueError:
                pass
            if not self.active_connections[report_id]:
                del self.active_connections[report_id]

    async def broadcast(self, report_id: str, message: dict):
        if report_id in self.active_connections:
            for connection in self.active_connections[report_id]:
                try:
                    await connection.send_json(message)
                except Exception:
                    pass

discussion_manager = DiscussionManager()

@app.websocket("/ws/discussion/{report_id}")
async def websocket_discussion(websocket: WebSocket, report_id: str):
    await discussion_manager.connect(report_id, websocket)
    try:
        while True:
            data = await websocket.receive_json()
            text = data.get("text", "")
            token = data.get("token", "")
            
            display_name = "Anonymous Driver"
            uid = "anonymous"
            
            if token:
                try:
                    from firebase_admin import auth as firebase_auth
                    decoded = firebase_auth.verify_id_token(token)
                    uid = decoded.get("uid", "anonymous")
                    display_name = decoded.get("name") or decoded.get("email") or "Logged-in Driver"
                except Exception as e:
                    print(f"WS Auth failed: {e}")
            
            if text.strip():
                from services.firebase_service import add_comment, increment_user_reputation
                comment = add_comment(
                    report_id=report_id,
                    uid=uid,
                    display_name=display_name,
                    text=text
                )
                try:
                    increment_user_reputation(uid, 2)
                except Exception:
                    pass
                
                await discussion_manager.broadcast(report_id, {
                    "type": "new_comment",
                    "comment": {
                        "comment_id": comment.get("comment_id", ""),
                        "report_id": comment.get("report_id", ""),
                        "user_id": comment.get("uid", uid),
                        "display_name": comment.get("displayName") or comment.get("display_name") or display_name,
                        "text": comment.get("text", text),
                        "timestamp": comment.get("timestamp", int(time.time() * 1000))
                    }
                })
    except WebSocketDisconnect:
        discussion_manager.disconnect(report_id, websocket)
    except Exception as e:
        print(f"WS error: {e}")
        discussion_manager.disconnect(report_id, websocket)

class NavigationConnectionManager:
    def __init__(self):
        self.active_connections = {}  # uid -> (websocket, start, dest, current)

    async def connect(self, uid: str, websocket: WebSocket, start_lat: float, start_lng: float, dest_lat: float, dest_lng: float):
        await websocket.accept()
        self.active_connections[uid] = (websocket, (start_lat, start_lng), (dest_lat, dest_lng), (start_lat, start_lng))

    def disconnect(self, uid: str):
        if uid in self.active_connections:
            del self.active_connections[uid]

    def update_current_location(self, uid: str, lat: float, lng: float):
        if uid in self.active_connections:
            ws, start, dest, _ = self.active_connections[uid]
            self.active_connections[uid] = (ws, start, dest, (lat, lng))

    async def broadcast_reconsider_route(self, new_report_lat: float, new_report_lng: float, report_msg: str):
        for uid, conn_data in list(self.active_connections.items()):
            ws = conn_data[0]
            try:
                await ws.send_json({
                    "type": "RECONSIDER_ROUTE",
                    "lat": new_report_lat,
                    "lng": new_report_lng,
                    "message": f"New hazard reported: '{report_msg}'. Tap to re-consider your route!"
                })
            except Exception as e:
                print(f"Failed to send alert to {uid}: {e}")
                self.disconnect(uid)

    async def broadcast_sos_alert(self, sos_lat: float, sos_lng: float, problem: str, user_name: str):
        for uid, (ws, start, dest, current) in list(self.active_connections.items()):
            dist_curr = calculate_distance_m(sos_lat, sos_lng, current[0], current[1])
            dist_start = calculate_distance_m(sos_lat, sos_lng, start[0], start[1])
            dist_dest = calculate_distance_m(sos_lat, sos_lng, dest[0], dest[1])
            if dist_curr <= 2000.0 or dist_start <= 2000.0 or dist_dest <= 2000.0:
                try:
                    await ws.send_json({
                        "type": "SOS_ALERT",
                        "lat": sos_lat,
                        "lng": sos_lng,
                        "message": f"SOS EMERGENCY from {user_name}: '{problem}'"
                    })
                except Exception as e:
                    print(f"Failed to send SOS alert to {uid}: {e}")
                    self.disconnect(uid)

navigation_manager = NavigationConnectionManager()

@app.websocket("/ws/navigation")
async def websocket_navigation(
    websocket: WebSocket,
    uid: str,
    start_lat: float = 33.6844,
    start_lng: float = 73.0479,
    dest_lat: float = 33.6844,
    dest_lng: float = 73.0479
):
    await navigation_manager.connect(uid, websocket, start_lat, start_lng, dest_lat, dest_lng)
    try:
        while True:
            # Keep socket alive and listen to location updates or heartbeats
            data = await websocket.receive_json()
            if isinstance(data, dict) and "lat" in data and "lng" in data:
                navigation_manager.update_current_location(uid, data["lat"], data["lng"])
    except WebSocketDisconnect:
        navigation_manager.disconnect(uid)
    except Exception as e:
        print(f"Navigation WS error: {e}")
        navigation_manager.disconnect(uid)

@app.post("/report/{report_id}/comment")
async def post_report_comment(
    report_id: str,
    req: CommentRequest,
    current_user: FirebaseUser = Depends(get_current_user)
):
    from services.firebase_service import add_comment, increment_user_reputation
    comment = add_comment(
        report_id=report_id,
        uid=current_user.uid,
        display_name=current_user.display_name or "Anonymous Driver",
        text=req.text
    )
    # Award commenting user 2 reputation points
    try:
        increment_user_reputation(current_user.uid, 2)
    except Exception:
        pass

    # Broadcast live!
    await discussion_manager.broadcast(report_id, {
        "type": "new_comment",
        "comment": {
            "comment_id": comment.get("comment_id", ""),
            "report_id": comment.get("report_id", ""),
            "user_id": comment.get("uid", current_user.uid),
            "display_name": comment.get("displayName") or comment.get("display_name") or current_user.display_name or "Anonymous Driver",
            "text": comment.get("text", req.text),
            "timestamp": comment.get("timestamp", int(time.time() * 1000))
        }
    })
    return {"status": "success", "comment": comment}

@app.get("/report/{report_id}/comments")
def get_report_comments(
    report_id: str,
    limit: int = 100,
    offset: int = 0
):
    from services.firebase_service import get_comments
    comments = get_comments(report_id)
    paginated = comments[offset : offset + limit]
    return {"comments": paginated}

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


@app.get("/user/profile", response_model=UserProfileResponse)
def get_user_profile_route(current_user: FirebaseUser = Depends(get_current_user)):
    from services.firebase_service import get_db, count_user_reports, FIREBASE_ENABLED
    
    uid = current_user.uid
    default_profile = {
        "uid": uid,
        "displayName": current_user.display_name or "Anonymous Driver",
        "age": 25,
        "gender": "Male",
        "photoUrl": current_user.photo_url or "",
        "totalReports": 0
    }
    
    if not FIREBASE_ENABLED:
        return default_profile
        
    try:
        doc_ref = get_db().collection("users").document(uid)
        doc = doc_ref.get()
        
        # Count reports
        total_reports = count_user_reports(uid)
        
        if doc.exists:
            d = doc.to_dict()
            return {
                "uid": uid,
                "displayName": d.get("displayName") or d.get("display_name") or current_user.display_name or "Anonymous Driver",
                "age": int(d.get("age", 25)),
                "gender": d.get("gender") or "Male",
                "photoUrl": d.get("photoUrl") or d.get("photo_url") or current_user.photo_url or "",
                "totalReports": total_reports
            }
        else:
            # Upsert default document
            doc_ref.set({
                "uid": uid,
                "displayName": default_profile["displayName"],
                "age": default_profile["age"],
                "gender": default_profile["gender"],
                "photoUrl": default_profile["photoUrl"],
                "reputationScore": 0,
                "createdAt": datetime.now(timezone.utc).isoformat()
            })
            return default_profile
            
    except Exception as e:
        print(f"Error in get_user_profile: {e}")
        return default_profile


@app.post("/user/profile")
def update_user_profile_route(req: UpdateProfileRequest, current_user: FirebaseUser = Depends(get_current_user)):
    from services.firebase_service import get_db, FIREBASE_ENABLED
    
    if not FIREBASE_ENABLED:
        return {"status": "success", "message": "Mock profile updated successfully"}
        
    try:
        doc_ref = get_db().collection("users").document(current_user.uid)
        doc_ref.set({
            "displayName": req.displayName,
            "age": req.age,
            "gender": req.gender,
            "photoUrl": req.photoUrl,
            "updatedAt": datetime.now(timezone.utc).isoformat()
        }, merge=True)
        return {"status": "success", "message": "Profile updated successfully"}
    except Exception as e:
        print(f"Error in update_user_profile: {e}")
        return {"status": "error", "message": str(e)}


@app.post("/user/profile/picture")
async def upload_profile_picture_route(file: UploadFile = File(...), current_user: FirebaseUser = Depends(get_current_user)):
    from services.firebase_service import upload_profile_picture
    
    try:
        contents = await file.read()
        photo_url = upload_profile_picture(current_user.uid, contents, file.content_type)
        if photo_url:
            return {"status": "success", "photoUrl": photo_url}
        else:
            return {"status": "error", "message": "Failed to upload picture"}
    except Exception as e:
        print(f"Error in upload_profile_picture_route: {e}")
        return {"status": "error", "message": str(e)}


@app.get("/user/reports")
def get_user_reports_route(current_user: FirebaseUser = Depends(get_current_user)):
    from services.firebase_service import get_all_reports
    
    reports = get_all_reports()
    user_alerts = []
    
    for rep in reports:
        if rep.get("user_id") == current_user.uid:
            from services.firebase_service import get_vote_stats
            stats = get_vote_stats(rep.get("report_id", ""))
            user_alerts.append({
                "alert_id": rep.get("report_id", ""),
                "type": rep.get("type", "TRAFFIC").lower(),
                "message": rep.get("message", ""),
                "message_urdu": "",
                "distance_km": 0.0,
                "severity": rep.get("severity", "High"),
                "timestamp": rep.get("timestamp", ""),
                "alternate_route": "",
                "lat": rep.get("lat", 0.0),
                "lng": rep.get("lng", 0.0),
                "confirmations": stats.get("upvotes", 0)
            })
            
    return {"alerts": user_alerts}


# ─────────────────────────────────────────────
# NEW ENDPOINTS FOR NAVIGATION AND SOS
# ─────────────────────────────────────────────

from typing import List

class RouteRequest(BaseModel):
    source_lat: float
    source_lng: float
    dest_lat: float
    dest_lng: float

class RoutePoint(BaseModel):
    latitude: float
    longitude: float

class RoutePathResponse(BaseModel):
    points: List[RoutePoint]
    distance_meters: int
    duration_seconds: int
    is_hazard_segment: bool
    summary: str
    pros: str
    cons: str

class NavigationRouteResponse(BaseModel):
    active_route: RoutePathResponse
    alternate_routes: List[RoutePathResponse]

class SosBroadcastRequest(BaseModel):
    lat: float
    lng: float
    problem: str

def generate_points_between(lat1: float, lng1: float, lat2: float, lng2: float, offset: float = 0.0) -> List[RoutePoint]:
    steps = 15
    points = []
    for i in range(steps + 1):
        fraction = i / steps
        lat = lat1 + (lat2 - lat1) * fraction
        lng = lng1 + (lng2 - lng1) * fraction
        
        # simple wave offset
        wave = math.sin(fraction * math.pi) * offset
        points.append(RoutePoint(latitude=lat + wave, longitude=lng + wave))
    return points

def fetch_osrm_routes(lat1: float, lng1: float, lat2: float, lng2: float) -> tuple[List[RoutePoint], List[RoutePoint], List[RoutePoint], int, int, int, int, int, int]:
    import urllib.request
    import json
    url = f"http://router.project-osrm.org/route/v1/driving/{lng1},{lat1};{lng2},{lat2}?overview=full&geometries=geojson&alternatives=true"
    try:
        req = urllib.request.Request(
            url, 
            headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
        )
        with urllib.request.urlopen(req, timeout=4) as response:
            data = json.loads(response.read().decode())
            if data.get("code") == "Ok" and data.get("routes"):
                routes = data["routes"]
                
                def parse_route(r):
                    coords = r["geometry"]["coordinates"]
                    pts = [RoutePoint(latitude=c[1], longitude=c[0]) for c in coords]
                    dist = int(r.get("distance", 0.0))
                    dur = int(r.get("duration", 0.0))
                    return pts, dist, dur
                
                pts1, dist1, dur1 = parse_route(routes[0])
                
                if len(routes) > 1:
                    pts2, dist2, dur2 = parse_route(routes[1])
                else:
                    pts2 = [RoutePoint(latitude=p.latitude + 0.001, longitude=p.longitude + 0.001) for p in pts1]
                    dist2 = int(dist1 * 1.08)
                    dur2 = int(dur1 * 1.1)
                    
                if len(routes) > 2:
                    pts3, dist3, dur3 = parse_route(routes[2])
                else:
                    pts3 = [RoutePoint(latitude=p.latitude - 0.001, longitude=p.longitude - 0.001) for p in pts1]
                    dist3 = int(dist1 * 1.05)
                    dur3 = int(dur1 * 1.07)
                    
                return pts1, pts2, pts3, dist1, dur1, dist2, dur2, dist3, dur3
    except Exception as e:
        print(f"OSRM routing failed: {e}")
    return [], [], [], 0, 0, 0, 0, 0, 0

def check_route_hazards(points: List[RoutePoint], reports: List[dict]) -> tuple[bool, List[str]]:
    has_hazard = False
    hazard_types = []
    for pt in points:
        for rep in reports:
            dist = calculate_distance_m(pt.latitude, pt.longitude, rep.get("lat", 0.0), rep.get("lng", 0.0))
            if dist <= 500.0:  # 500 meters
                has_hazard = True
                hazard_type = rep.get("type", "TRAFFIC").upper()
                if hazard_type not in hazard_types:
                    hazard_types.append(hazard_type)
    return has_hazard, hazard_types

@app.post("/navigation/route", response_model=NavigationRouteResponse)
def get_navigation_route(req: RouteRequest):
    reports = get_all_reports()
    
    # Calculate base distance (always available as a reference)
    base_dist = calculate_distance_m(req.source_lat, req.source_lng, req.dest_lat, req.dest_lng)
    
    # Try fetching real OSRM road routes (follows actual roads/lanes)
    pts1, pts2, pts3, dist1, dur1, dist2, dur2, dist3, dur3 = fetch_osrm_routes(req.source_lat, req.source_lng, req.dest_lat, req.dest_lng)
    
    # Check hazards on OSRM routes if succeeded
    if pts1 and pts2 and pts3:
        has_haz1, haz_types1 = check_route_hazards(pts1, reports)
        has_haz2, haz_types2 = check_route_hazards(pts2, reports)
        has_haz3, haz_types3 = check_route_hazards(pts3, reports)
    else:
        # Fallback: Generate 3 alternative paths using math curves
        # 1. Primary path (direct Srinagar Highway)
        pts1 = generate_points_between(req.source_lat, req.source_lng, req.dest_lat, req.dest_lng, offset=0.0)
        has_haz1, haz_types1 = check_route_hazards(pts1, reports)
        
        # 2. Safer path (offset positive - left curve Jinnah Ave)
        pts2 = generate_points_between(req.source_lat, req.source_lng, req.dest_lat, req.dest_lng, offset=0.005)
        has_haz2, haz_types2 = check_route_hazards(pts2, reports)
        
        # 3. Quick Expressway path (offset negative - right curve Expressway)
        pts3 = generate_points_between(req.source_lat, req.source_lng, req.dest_lat, req.dest_lng, offset=-0.004)
        has_haz3, haz_types3 = check_route_hazards(pts3, reports)
        
        # Route 1 metadata
        dist1 = int(base_dist)
        dur1 = int(dist1 / 13.8) # Approx 50 km/h
        
        # Route 2 metadata
        dist2 = int(base_dist * 1.08) # 8% longer for the curve
        dur2 = int(dist2 / 13.8)
        
        # Route 3 metadata
        dist3 = int(base_dist * 1.05) # 5% longer
        dur3 = int(dist3 / 13.8)

    # Route 1 metadata
    summary1 = "Primary Direct Route"
    if has_haz1:
        pros1 = "Fastest distance."
        cons1 = f"Warning: {', '.join(haz_types1)} hazard detected ahead!"
    else:
        pros1 = "Fastest route, completely clear of incidents."
        cons1 = "Heavier construction zones."

    # Route 2 metadata
    summary2 = "Safer Alternative (Left Curve)"
    if has_haz2:
        pros2 = "Usually lower congestion."
        cons2 = f"Active {', '.join(haz_types2)} warning."
    else:
        pros2 = "100% safe, bypassing all known hazards."
        cons2 = "Slightly longer (+3 mins)."

    # Route 3 metadata
    summary3 = "Express Alternative (Right Curve)"
    if has_haz3:
        pros3 = "Shortest detour."
        cons3 = f"Active critical hazard: {', '.join(haz_types3)} blocking lane."
    else:
        pros3 = "Minimal delays."
        cons3 = "High probability of rush-hour backup."
        
    route1 = RoutePathResponse(
        points=pts1,
        distance_meters=dist1,
        duration_seconds=dur1,
        is_hazard_segment=has_haz1,
        summary=summary1,
        pros=pros1,
        cons=cons1
    )
    
    route2 = RoutePathResponse(
        points=pts2,
        distance_meters=dist2,
        duration_seconds=dur2,
        is_hazard_segment=has_haz2,
        summary=summary2,
        pros=pros2,
        cons=cons2
    )
    
    route3 = RoutePathResponse(
        points=pts3,
        distance_meters=dist3,
        duration_seconds=dur3,
        is_hazard_segment=has_haz3,
        summary=summary3,
        pros=pros3,
        cons=cons3
    )
    
    return NavigationRouteResponse(
        active_route=route1,
        alternate_routes=[route2, route3]
    )

@app.post("/sos/broadcast")
async def post_sos_broadcast(req: SosBroadcastRequest, current_user: FirebaseUser = Depends(get_current_user)):
    from services.firebase_service import save_sos
    import time
    sos_id = f"sos_{int(time.time())}_{current_user.uid[:4]}"
    displayName = current_user.display_name or "Anonymous Driver"
    sos_doc = {
        "sos_id": sos_id,
        "user_id": current_user.uid,
        "displayName": displayName,
        "lat": req.lat,
        "lng": req.lng,
        "problem": req.problem,
        "timestamp": int(time.time() * 1000)
    }
    success = save_sos(sos_doc)
    if not success:
        return {"status": "failed", "message": "Failed to broadcast SOS"}
    
    # Broadcast to active navigation clients within 2km via websocket
    await navigation_manager.broadcast_sos_alert(req.lat, req.lng, req.problem, displayName)
    
    return {"status": "success", "sos_id": sos_id, "message": "SOS broadcasted successfully to all users within 100km!"}


class PlaceSuggestion(BaseModel):
    description: str
    latitude: float
    longitude: float

class PlacesResponse(BaseModel):
    predictions: List[PlaceSuggestion]

LOCAL_PLACES = [
    {"description": "F-7 Markaz, Islamabad", "latitude": 33.7220, "longitude": 73.0580},
    {"description": "Centaurus Mall, Sector F-8, Islamabad", "latitude": 33.7077, "longitude": 73.0503},
    {"description": "Srinagar Highway, G-9, Islamabad", "latitude": 33.6844, "longitude": 73.0479},
    {"description": "Jinnah Avenue, Blue Area, Islamabad", "latitude": 33.7120, "longitude": 73.0570},
    {"description": "Faisal Mosque, Islamabad", "latitude": 33.7299, "longitude": 73.0373},
    {"description": "Jinnah Super Market (F-7), Islamabad", "latitude": 33.7206, "longitude": 73.0578},
    {"description": "Zero Point Interchange, Islamabad", "latitude": 33.6888, "longitude": 73.0645},
    {"description": "Shahrah-e-Faisal, Karachi", "latitude": 24.8607, "longitude": 67.0011},
    {"description": "Jinnah International Airport, Karachi", "latitude": 24.9066, "longitude": 67.1608},
    {"description": "Clifton Beach, Karachi", "latitude": 24.8138, "longitude": 67.0311},
    {"description": "Karachi Cantonment Station, Karachi", "latitude": 24.8465, "longitude": 67.0336},
    {"description": "National Stadium, Stadium Road, Karachi", "latitude": 24.8944, "longitude": 67.0788},
    {"description": "Dolmen Mall Clifton, Karachi", "latitude": 24.8021, "longitude": 67.0315},
    {"description": "University of Karachi, Main University Rd, Karachi", "latitude": 24.9351, "longitude": 67.1238}
]

@app.get("/navigation/places", response_model=PlacesResponse)
def get_places_autocomplete(query: str):
    import os
    import requests
    
    api_key = os.getenv("GOOGLE_MAPS_API_KEY")
    if api_key:
        try:
            # 1. Fetch autocomplete predictions from real Google Places API
            url = f"https://maps.googleapis.com/maps/api/place/autocomplete/json?input={query}&key={api_key}"
            response = requests.get(url, timeout=5)
            if response.status_code == 200:
                data = response.json()
                predictions = data.get("predictions", [])
                
                results = []
                # Resolve coordinates for the top predictions
                for pred in predictions[:5]:
                    desc = pred.get("description")
                    place_id = pred.get("place_id")
                    
                    # Call details API to get lat/lng
                    details_url = f"https://maps.googleapis.com/maps/api/place/details/json?place_id={place_id}&fields=geometry&key={api_key}"
                    det_resp = requests.get(details_url, timeout=5)
                    if det_resp.status_code == 200:
                        det_data = det_resp.json()
                        location = det_data.get("result", {}).get("geometry", {}).get("location", {})
                        lat = location.get("lat")
                        lng = location.get("lng")
                        if lat is not None and lng is not None:
                            results.append(PlaceSuggestion(description=desc, latitude=lat, longitude=lng))
                
                if results:
                    return PlacesResponse(predictions=results)
        except Exception as e:
            print(f"Error calling real Google Places API: {e}")

    # Fallback to local hardcoded mock places
    q = query.lower()
    matches = []
    for place in LOCAL_PLACES:
        if q in place["description"].lower():
            matches.append(PlaceSuggestion(**place))
            
    if not matches:
        import hashlib
        h = int(hashlib.md5(query.encode('utf-8')).hexdigest(), 16)
        lat_offset = (h % 100) / 1000.0 - 0.05
        lng_offset = ((h // 100) % 100) / 1000.0 - 0.05
        
        if "karachi" in q or "clifton" in q or "dha" in q:
            lat = 24.8607 + lat_offset
            lng = 67.0011 + lng_offset
        else:
            lat = 33.6888 + lat_offset
            lng = 73.0645 + lng_offset
            
        matches.append(PlaceSuggestion(
            description=f"{query} (Local Fallback)",
            latitude=lat,
            longitude=lng
        ))
        
    return PlacesResponse(predictions=matches)
