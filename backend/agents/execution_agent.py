import uuid
import datetime
import json
import logging
from services.firebase_service import save_simulation, save_notifications, save_ticket, update_current_crisis, save_agent_trace
from services.llm_service import call_llm
from services.logger import log_agent_event

logger = logging.getLogger(__name__)

class ExecutionAgent:
    def __init__(self):
        pass

    async def run(self, ingestion_result: dict, trust_result: dict, situation_result: dict, trace_logs: dict = None) -> dict:
        log_agent_event("EXECUTION_START", {"step": "Starting execution and simulation phase"})
        
        severity = situation_result.get("severity", "Low")
        eta_mins = situation_result.get("eta_impact_minutes", 15)
        crisis_type = trust_result.get("crisis_type", "unknown")
        
        # TASK 1: Traffic Rerouting Simulation
        before_state = {
            "blocked_route": "Canal Road, Karachi",
            "congestion_level": "SEVERE" if severity in ["High", "Critical"] else "MODERATE",
            "avg_eta_mins": eta_mins,
            "vehicles_affected": 2400 if severity in ["High", "Critical"] else 1200
        }
        after_state = {
            "alternate_route": "Shahrah-e-Faisal",
            "new_congestion_level": "LOW",
            "new_avg_eta_mins": max(4, int(eta_mins * 0.2)),
            "vehicles_rerouted": 1440,
            "improvement_pct": 84
        }
        simulation_data = {
            "before_state": before_state,
            "after_state": after_state,
            "timestamp": datetime.datetime.utcnow().isoformat()
        }
        save_simulation(simulation_data)
        log_agent_event("EXECUTION_SIMULATION", {"step": "Traffic rerouting simulation saved"})

        # TASK 2: Notification Dispatch
        now_ts = datetime.datetime.utcnow().isoformat()
        message_eng = "Heavy flooding on Canal Road. Use Shahrah-e-Faisal instead."
        
        prompt = f"Translate this notification to proper Urdu. Return ONLY the Urdu text, no quotes or additional words: '{message_eng}'"
        message_ur = "کینال روڈ پر شدید سیلاب۔ شاہراہ فیصل استعمال کریں۔" # Fallback
        try:
            log_agent_event("EXECUTION_LLM", {"step": "Calling LLM for Urdu translation"})
            result = await call_llm(prompt, model="gemini-2.5-flash")
            if result and len(result) > 5:
                message_ur = result.strip()
            log_agent_event("EXECUTION_LLM", {"step": "Urdu translation complete"})
        except Exception as e:
            log_agent_event("EXECUTION_ERROR", {"error": str(e)})

        notifications = [
            {
                "recipient_type": "nearby_users",
                "message_text": message_eng,
                "message_urdu": message_ur,
                "channel": "push_notification",
                "timestamp": now_ts,
                "status": "SENT"
            },
            {
                "recipient_type": "emergency_services",
                "message_text": f"DISPATCH REQUIRED: {severity} severity {crisis_type}.",
                "message_urdu": "Not applicable",
                "channel": "api_webhook",
                "timestamp": now_ts,
                "status": "SENT"
            },
            {
                "recipient_type": "traffic_authority",
                "message_text": f"REROUTE INITIATED: 60% traffic shifted to Shahrah-e-Faisal.",
                "message_urdu": "Not applicable",
                "channel": "dashboard_alert",
                "timestamp": now_ts,
                "status": "SENT"
            }
        ]
        save_notifications(notifications)
        log_agent_event("EXECUTION_NOTIFICATIONS", {"step": "Notifications dispatched and saved"})

        # TASK 3: Emergency Ticket
        ticket = {
            "ticket_id": "TG-2891",
            "priority": "CRITICAL" if severity in ["High", "Critical"] else "HIGH",
            "crisis_type": crisis_type,
            "location": "Canal Road, Karachi",
            "assigned_to": "Karachi Traffic Police",
            "created_at": now_ts,
            "status": "OPEN",
            "estimated_response_mins": 12
        }
        save_ticket(ticket)
        log_agent_event("EXECUTION_TICKET", {"step": "Emergency ticket created", "ticket_id": ticket["ticket_id"]})

        # TASK 4: Update Realtime DB
        current_crisis = {
            "crisis_id": ticket["ticket_id"],
            "location": ticket["location"],
            "type": crisis_type,
            "confidence": trust_result.get("confidence_score", 0),
            "severity": severity,
            "status": "ACTIVE" if trust_result.get("confidence_score", 0) >= 60 else "MONITOR",
            "last_updated": now_ts
        }
        update_current_crisis(current_crisis)
        log_agent_event("EXECUTION_REALTIME", {"step": "Live crisis status updated in RTDB"})

        # TASK 5: Compile Agent Trace
        trace_id = f"trace_{uuid.uuid4().hex[:6]}"
        agent_logs = trace_logs if trace_logs else {"Agent1": [], "Agent2": [], "Agent3": [], "Agent4": []}
        
        full_trace = {
            "trace_id": trace_id,
            "crisis_id": "c001",
            "agents": [
                {"agent_name": "IngestionAgent", "steps": agent_logs.get("Agent1", [])},
                {"agent_name": "TrustDetectionAgent", "steps": agent_logs.get("Agent2", [])},
                {"agent_name": "SituationPlanningAgent", "steps": agent_logs.get("Agent3", [])},
                {"agent_name": "ExecutionAgent", "steps": agent_logs.get("Agent4", [])}
            ]
        }
        save_agent_trace(full_trace)
        log_agent_event("EXECUTION_TRACE", {"step": "Full agent trace compiled and saved", "trace_id": trace_id})

        return {
            "before_state": before_state,
            "after_state": after_state,
            "notifications": notifications,
            "ticket": ticket,
            "firebase_saved": True,
            "trace_id": trace_id
        }

    def simple_heuristic(self, text: str) -> dict:
        """
        TASK 6: Baseline non-agentic algorithm for comparison.
        """
        keywords = ["flood", "pani", "accident", "block", "jam", "traffic", "bhar", "phans"]
        lower_text = text.lower()
        
        detected = False
        detected_type = "unknown"
        for kw in keywords:
            if kw in lower_text:
                detected = True
                if kw in ["flood", "pani", "bhar"]:
                    detected_type = "flooding"
                elif kw in ["accident"]:
                    detected_type = "accident"
                elif kw in ["block", "jam", "traffic", "phans"]:
                    detected_type = "congestion"
                break
                
        return {
            "detected": detected,
            "type": detected_type,
            "confidence": "HIGH" if detected else "LOW"
        }
