import os
import logging
import firebase_admin
from firebase_admin import credentials, firestore, db, auth
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger(__name__)

# Global clients
db_client = None
FIREBASE_ENABLED = False

# ─────────────────────────────────────────────
# CONFIG
# ─────────────────────────────────────────────

cred_path = os.getenv("FIREBASE_CREDENTIALS_PATH")

# ─────────────────────────────────────────────
# INIT FIREBASE
# ─────────────────────────────────────────────

if cred_path and os.path.exists(cred_path) and cred_path != "./firebase_credentials.json":
    try:

        if not firebase_admin._apps:
            cred = credentials.Certificate(cred_path)
            firebase_admin.initialize_app(cred)

        db_client = firestore.client()

        FIREBASE_ENABLED = True
        logger.info("Firebase initialized successfully.")

    except Exception as e:
        FIREBASE_ENABLED = False
        logger.warning(f"Firebase init failed: {e} (mock mode enabled)")
else:
    FIREBASE_ENABLED = False
    logger.warning("Firebase not configured properly. Running in MOCK mode.")

# ─────────────────────────────────────────────
# HELPERS
# ─────────────────────────────────────────────

def save_simulation(data: dict):
    try:
        if FIREBASE_ENABLED:
            db_client.collection("simulations").add(data)
        else:
            logger.warning(f"MOCK FIREBASE: simulation {data}")
        return True
    except Exception as e:
        logger.warning(f"Firebase write failed: {e}")
        return False


def save_notifications(notifications: list):
    try:
        if FIREBASE_ENABLED:
            batch = db_client.batch()

            for notif in notifications:
                ref = db_client.collection("notifications").document()
                batch.set(ref, notif)

            batch.commit()
        else:
            logger.warning(f"MOCK FIREBASE: {len(notifications)} notifications")

        return True
    except Exception as e:
        logger.warning(f"Firebase write failed: {e}")
        return False


def save_ticket(ticket: dict):
    try:
        if FIREBASE_ENABLED:
            db_client.collection("tickets").document(
                ticket["ticket_id"]
            ).set(ticket)
        else:
            logger.warning(f"MOCK FIREBASE: ticket {ticket['ticket_id']}")

        return True
    except Exception as e:
        logger.warning(f"Firebase write failed: {e}")
        return False


def update_current_crisis(crisis_data: dict):
    try:
        if FIREBASE_ENABLED:
            db_client.collection("crisis").document("current").set(crisis_data)
        else:
            logger.warning("MOCK FIREBASE: crisis updated")

        return True
    except Exception as e:
        logger.warning(f"Firebase write failed: {e}")
        return False


def save_agent_trace(trace: dict):
    try:
        if FIREBASE_ENABLED:
            db_client.collection("agent_traces").document(
                trace["trace_id"]
            ).set(trace)
        else:
            logger.warning(f"MOCK FIREBASE: trace {trace['trace_id']}")

        return True
    except Exception as e:
        logger.warning(f"Firebase write failed: {e}")
        return False


def get_latest_trace():
    try:
        if FIREBASE_ENABLED:
            docs = (
                db_client.collection("agent_traces")
                .order_by("timestamp", direction=firestore.Query.DESCENDING)
                .limit(1)
                .get()
            )
            if docs:
                return docs[0].to_dict()
    except Exception as e:
        logger.warning(f"Firebase read failed: {e}")

    return None


def verify_token(id_token: str) -> dict:
    try:
        if FIREBASE_ENABLED:
            return auth.verify_id_token(id_token)

        logger.warning("MOCK AUTH used")
        return {
            "uid": "mock_uid",
            "email": "mock@example.com"
        }

    except Exception as e:
        logger.error(f"Auth error: {e}")
        raise


def save_report(report_data: dict) -> bool:
    try:
        if FIREBASE_ENABLED:
            db_client.collection("reports").document(
                report_data["report_id"]
            ).set(report_data)
        else:
            logger.warning(f"MOCK report: {report_data}")

        return True
    except Exception as e:
        logger.warning(f"Firebase write failed: {e}")
        return False


def get_all_reports() -> list:
    try:
        if FIREBASE_ENABLED:
            return [doc.to_dict() for doc in db_client.collection("reports").stream()]
    except Exception as e:
        logger.warning(f"Firebase read failed: {e}")

    return []


def get_current_crisis_firestore() -> dict:
    try:
        if FIREBASE_ENABLED:
            doc = db_client.collection("crisis").document("current").get()
            if doc.exists:
                return doc.to_dict()

        return {
            "crisis_id": "mock",
            "type": "FLOOD",
            "severity": "HIGH"
        }

    except Exception as e:
        logger.warning(f"Firebase read failed: {e}")
        return {}