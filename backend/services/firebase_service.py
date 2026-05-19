import os
import logging
import firebase_admin
from firebase_admin import credentials, firestore, auth
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger(__name__)

# ─────────────────────────────────────────────
# GLOBAL STATE
# ─────────────────────────────────────────────

db_client = None
FIREBASE_ENABLED = False

# ─────────────────────────────────────────────
# CONFIG
# ─────────────────────────────────────────────

cred_path = os.getenv("FIREBASE_CREDENTIALS_PATH")

# Optional fallback for cloud deployments
if cred_path:
    os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = cred_path

# ─────────────────────────────────────────────
# INIT FIREBASE (FIRESTORE ONLY)
# ─────────────────────────────────────────────

try:
    if cred_path and os.path.exists(cred_path):

        if not firebase_admin._apps:
            cred = credentials.Certificate(cred_path)
            firebase_admin.initialize_app(cred)

        db_client = firestore.client()
        FIREBASE_ENABLED = True

        logger.info("🔥 Firestore initialized successfully")

    else:
        logger.warning("⚠️ Firebase credentials missing. Running in MOCK mode.")

except Exception as e:
    logger.error(f"❌ Firestore init failed: {e}")
    FIREBASE_ENABLED = False

# ─────────────────────────────────────────────
# SAFE GET CLIENT
# ─────────────────────────────────────────────

def get_db():
    if not FIREBASE_ENABLED:
        raise RuntimeError("Firestore is not initialized")
    return db_client

# ─────────────────────────────────────────────
# REPORTS
# ─────────────────────────────────────────────

def save_report(report_data: dict) -> bool:
    try:
        if FIREBASE_ENABLED:
            get_db().collection("reports").document(
                report_data["report_id"]
            ).set(report_data)
        else:
            logger.warning(f"MOCK report saved: {report_data}")

        return True

    except Exception as e:
        logger.error(f"save_report failed: {e}")
        return False


def get_all_reports() -> list:
    try:
        if FIREBASE_ENABLED:
            return [
                doc.to_dict()
                for doc in get_db().collection("reports").stream()
            ]
    except Exception as e:
        logger.error(f"get_all_reports failed: {e}")

    return []

# ─────────────────────────────────────────────
# CRISIS
# ─────────────────────────────────────────────

def update_current_crisis(crisis_data: dict) -> bool:
    try:
        if FIREBASE_ENABLED:
            get_db().collection("crisis").document("current").set(crisis_data)
        else:
            logger.warning("MOCK crisis updated")

        return True

    except Exception as e:
        logger.error(f"update_current_crisis failed: {e}")
        return False


def get_current_crisis_firestore() -> dict:
    try:
        if FIREBASE_ENABLED:
            doc = get_db().collection("crisis").document("current").get()
            if doc.exists:
                return doc.to_dict()
            return {}

        return {
            "crisis_id": "mock",
            "type": "FLOOD",
            "severity": "HIGH"
        }

    except Exception as e:
        logger.error(f"get_current_crisis failed: {e}")
        return {}

# ─────────────────────────────────────────────
# AGENT TRACE
# ─────────────────────────────────────────────

def save_agent_trace(trace: dict) -> bool:
    try:
        if FIREBASE_ENABLED:
            get_db().collection("agent_traces").document(
                trace["trace_id"]
            ).set(trace)
        else:
            logger.warning(f"MOCK trace: {trace['trace_id']}")

        return True

    except Exception as e:
        logger.error(f"save_agent_trace failed: {e}")
        return False


def get_latest_trace():
    try:
        if FIREBASE_ENABLED:
            docs = (
                get_db()
                .collection("agent_traces")
                .order_by("timestamp", direction=firestore.Query.DESCENDING)
                .limit(1)
                .stream()
            )

            for doc in docs:
                return doc.to_dict()

    except Exception as e:
        logger.error(f"get_latest_trace failed: {e}")

    return None

# ─────────────────────────────────────────────
# AUTH (SAFE)
# ─────────────────────────────────────────────

def verify_token(id_token: str) -> dict:
    try:
        if FIREBASE_ENABLED:
            return auth.verify_id_token(id_token)

        logger.warning("MOCK auth used")
        return {
            "uid": "mock_uid",
            "email": "mock@example.com"
        }

    except Exception as e:
        logger.error(f"verify_token failed: {e}")
        raise

# ─────────────────────────────────────────────
# NOTIFICATIONS
# ─────────────────────────────────────────────

def save_notifications(notifications: list) -> bool:
    try:
        if FIREBASE_ENABLED:
            batch = get_db().batch()

            for notif in notifications:
                ref = get_db().collection("notifications").document()
                batch.set(ref, notif)

            batch.commit()
        else:
            logger.warning(f"MOCK notifications: {len(notifications)}")

        return True

    except Exception as e:
        logger.error(f"save_notifications failed: {e}")
        return False