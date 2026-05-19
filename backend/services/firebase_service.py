import os
import logging
import firebase_admin
from firebase_admin import credentials, firestore, db, auth
from dotenv import load_dotenv

load_dotenv()


logger = logging.getLogger(__name__)

cred_path = os.getenv("FIREBASE_CREDENTIALS_PATH")
if cred_path and os.path.exists(cred_path) and cred_path != "path/to/your/firebase/credentials.json":
    os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = cred_path
    try:
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred, {
            'databaseURL': 'https://hackathon-trafficguard-default-rtdb.asia-southeast1.firebasedatabase.app'
        })
        db_client = firestore.client()
        rtdb_client = db.reference()
        FIREBASE_ENABLED = True
        logger.info("Firebase initialized successfully.")
    except Exception as e:
        logger.warning(f"Error initializing Firebase: {e}. Writes will be mocked.")
        FIREBASE_ENABLED = False
else:
    FIREBASE_ENABLED = False
    logger.warning("Firebase not configured properly (missing or invalid FIREBASE_CREDENTIALS_PATH). Writes will be mocked.")

def save_simulation(data: dict):
    try:
        if FIREBASE_ENABLED:
            db_client.collection("simulations").add(data)
            return True
        else:
            logger.warning(f"MOCK FIREBASE: Saved simulation {data}")
            return True
    except Exception as e:
        logger.warning(f"Firebase write failed, continuing without save: {e}")
        return False

def save_notifications(notifications: list):
    try:
        if FIREBASE_ENABLED:
            batch = db_client.batch()
            for notif in notifications:
                doc_ref = db_client.collection("notifications").document()
                batch.set(doc_ref, notif)
            batch.commit()
            return True
        else:
            logger.warning(f"MOCK FIREBASE: Saved {len(notifications)} notifications")
            return True
    except Exception as e:
        logger.warning(f"Firebase write failed, continuing without save: {e}")
        return False

def save_ticket(ticket: dict):
    try:
        if FIREBASE_ENABLED:
            db_client.collection("tickets").document(ticket['ticket_id']).set(ticket)
            return True
        else:
            logger.warning(f"MOCK FIREBASE: Saved ticket {ticket['ticket_id']}")
            return True
    except Exception as e:
        logger.warning(f"Firebase write failed, continuing without save: {e}")
        return False

def update_current_crisis(crisis_data: dict):
    try:
        if FIREBASE_ENABLED:
            rtdb_client.child("crisis/current").set(crisis_data)
            return True
        else:
            logger.warning(f"MOCK FIREBASE: Updated RTDB /crisis/current {crisis_data}")
            return True
    except Exception as e:
        logger.warning(f"Firebase write failed, continuing without save: {e}")
        return False

def save_agent_trace(trace: dict):
    try:
        if FIREBASE_ENABLED:
            db_client.collection("agent_traces").document(trace['trace_id']).set(trace)
            return True
        else:
            logger.warning(f"MOCK FIREBASE: Saved agent trace {trace['trace_id']}")
            return True
    except Exception as e:
        logger.warning(f"Firebase write failed, continuing without save: {e}")
        return False

def get_latest_trace():
    try:
        if FIREBASE_ENABLED:
            traces = db_client.collection("agent_traces").order_by("timestamp", direction=firestore.Query.DESCENDING).limit(1).get()
            if traces:
                return traces[0].to_dict()
    except Exception as e:
        logger.warning(f"Firebase read failed: {e}")
    return None

def verify_token(id_token: str) -> dict:
    """Verifies a Firebase ID token. Returns decoded claims or raises an exception."""
    try:
        if FIREBASE_ENABLED:
            decoded_token = auth.verify_id_token(id_token)
            return decoded_token
        else:
            logger.warning(f"MOCK FIREBASE AUTH: Decoding token {id_token[:15]}...")
            return {
                "uid": "mock_firebase_uid",
                "email": "mockuser@example.com",
                "name": "Mock Firebase User"
            }
    except Exception as e:
        logger.error(f"Failed to verify ID token: {e}")
        raise e

def save_report(report_data: dict) -> bool:
    try:
        if FIREBASE_ENABLED:
            db_client.collection("reports").document(report_data["report_id"]).set(report_data)
            return True
        else:
            logger.warning(f"MOCK FIREBASE: Saved report {report_data}")
            return True
    except Exception as e:
        logger.warning(f"Firebase write failed, continuing without save: {e}")
        return False

def get_all_reports() -> list:
    try:
        if FIREBASE_ENABLED:
            docs = db_client.collection("reports").stream()
            reports = []
            for doc in docs:
                reports.append(doc.to_dict())
            return reports
        else:
            logger.warning("MOCK FIREBASE: Reading all reports (returning empty)")
            return []
    except Exception as e:
        logger.warning(f"Firebase read failed: {e}")
    return []

def get_current_crisis_rtdb() -> dict:
    try:
        if FIREBASE_ENABLED:
            return rtdb_client.child("crisis/current").get()
        else:
            logger.warning("MOCK FIREBASE: Reading RTDB crisis/current (returning mock data)")
            return {
                "crisis_id": "c001",
                "location": "Canal Road, Karachi",
                "lat": 24.8607,
                "lng": 67.0011,
                "type": "FLOODING",
                "status": "active",
                "severity": "HIGH",
                "timestamp": "2026-05-19T00:00:00Z",
                "description": "Severe urban flooding reported near Canal Road. Roads submerged, traffic halted.",
                "affected_areas": ["Canal Road", "Shahrah-e-Faisal", "Gulshan-e-Iqbal"],
                "alternate_route": "Use Karsaz Road or University Road instead."
            }
    except Exception as e:
        logger.warning(f"Firebase read failed: {e}")
    return {}

