import uuid
import datetime
import logging
from services.firebase_service import save_simulation, save_notifications, save_ticket, update_current_crisis, save_agent_trace

logger = logging.getLogger(__name__)

class ExecutionAgent:
    def __init__(self):
        pass

    def run(self, situation_plan: dict, trust_analysis: dict) -> dict:
        severity = situation_plan.get("severity", "Low")
        eta_mins = situation_plan.get("eta_impact_minutes", 15)
        crisis_type = trust_analysis.get("crisis_type", "unknown")
        confidence = trust_analysis.get("confidence_score", 0)

        logger.info(f"Execution Agent: Starting simulations for {severity} {crisis_type}")

        # Task 1: Traffic Rerouting Simulation
        before_state = {
            "blocked_route": "Canal Road",
            "congestion_level": "SEVERE" if severity in ["High", "Critical"] else "MODERATE",
            "avg_eta_mins": eta_mins,
            "vehicles_affected": 450 if severity in ["High", "Critical"] else 120
        }
        after_state = {
            "alternate_route": "M.A. Jinnah Road",
            "new_congestion_level": "MODERATE",
            "new_avg_eta_mins": int(eta_mins * 0.4),
            "improvement_pct": 60
        }
        simulation_data = {
            "before_state": before_state,
            "after_state": after_state,
            "timestamp": datetime.datetime.utcnow().isoformat()
        }
        save_simulation(simulation_data)
        logger.info("Execution Agent: Completed Traffic Rerouting Simulation.")

        # Task 2: Notification Dispatch Simulation
        now_ts = datetime.datetime.utcnow().isoformat()
        message_eng = f"ALERT: {crisis_type.replace('_', ' ').title()} reported. Alternate routes advised."
        message_ur = "الرٹ: متبادل راستے اختیار کریں۔"
        
        notifications = [
            {
                "recipient_type": "nearby_users",
                "message_text": f"{message_eng} | {message_ur}",
                "channel": "PUSH",
                "timestamp": now_ts,
                "status": "SENT"
            },
            {
                "recipient_type": "emergency_services",
                "message_text": f"DISPATCH REQUIRED: {severity} severity {crisis_type}.",
                "channel": "API",
                "timestamp": now_ts,
                "status": "SENT"
            },
            {
                "recipient_type": "traffic_authority",
                "message_text": f"REROUTE INITIATED: 60% traffic shifted from {before_state['blocked_route']}.",
                "channel": "DASHBOARD",
                "timestamp": now_ts,
                "status": "SENT"
            }
        ]
        save_notifications(notifications)
        logger.info("Execution Agent: Completed Notification Dispatch Simulation.")

        # Task 3: Emergency Ticket
        ticket = {
            "ticket_id": f"TKT-{uuid.uuid4().hex[:6].upper()}",
            "priority": "P1" if severity in ["High", "Critical"] else "P2",
            "crisis_type": crisis_type,
            "location": "Canal Road, Karachi", 
            "assigned_to": "Emergency Response Team",
            "created_at": now_ts,
            "status": "OPEN",
            "estimated_response_mins": 10 if severity in ["High", "Critical"] else 25
        }
        save_ticket(ticket)
        logger.info(f"Execution Agent: Created Emergency Ticket {ticket['ticket_id']}.")

        # Update Live Crisis
        current_crisis = {
            "crisis_id": ticket["ticket_id"],
            "location": ticket["location"],
            "type": crisis_type,
            "confidence": confidence,
            "severity": severity,
            "status": "ACTIVE" if confidence >= 60 else "MONITOR",
            "last_updated": now_ts
        }
        update_current_crisis(current_crisis)

        return {
            "simulation": simulation_data,
            "notifications": notifications,
            "ticket": ticket
        }

    def compile_full_trace(self, agent1_logs: list, agent2_logs: list, agent3_logs: list, agent4_logs: list, crisis_id: str) -> dict:
        now_ts = datetime.datetime.utcnow().isoformat()
        trace = {
            "trace_id": f"TRC-{uuid.uuid4().hex[:8].upper()}",
            "crisis_id": crisis_id,
            "timestamp": now_ts,
            "agents": [
                {"agent_name": "Ingestion Agent", "steps": agent1_logs},
                {"agent_name": "Trust + Detection Agent", "steps": agent2_logs},
                {"agent_name": "Situation + Planning Agent", "steps": agent3_logs},
                {"agent_name": "Execution + Simulation Agent", "steps": agent4_logs}
            ]
        }
        save_agent_trace(trace)
        return trace

    def simple_heuristic(self, raw_text: str) -> dict:
        """
        Baseline non-agentic algorithm for comparison.
        """
        keywords = ["flood", "pani", "accident", "block", "jam", "traffic"]
        lower_text = raw_text.lower()
        
        detected = False
        detected_type = "unknown"
        for kw in keywords:
            if kw in lower_text:
                detected = True
                if kw in ["flood", "pani"]:
                    detected_type = "flooding"
                elif kw in ["accident"]:
                    detected_type = "accident"
                elif kw in ["block", "jam", "traffic"]:
                    detected_type = "congestion"
                break
                
        return {
            "detected": detected,
            "type": detected_type,
            "confidence": "HIGH" if detected else "LOW"
        }
