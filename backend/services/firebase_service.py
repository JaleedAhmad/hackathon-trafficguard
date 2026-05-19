import os
import logging
import firebase_admin
from firebase_admin import credentials, firestore, auth, storage
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger(__name__)

# ─────────────────────────────────────────────
# GLOBAL STATE
# ─────────────────────────────────────────────

db_client = None
FIREBASE_ENABLED = False

_mock_reports = []
_mock_agent_traces = []
_mock_sos = []

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
            firebase_admin.initialize_app(cred, {
                "storageBucket": "poetic-fact-496519-k2.firebasestorage.app"
            })

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
            _mock_reports.append(report_data)

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
        else:
            return _mock_reports
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
            _mock_agent_traces.append(trace)

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
        else:
            if _mock_agent_traces:
                return _mock_agent_traces[-1]

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

# ─────────────────────────────────────────────
# REPUTATION & LEADERBOARD
# ─────────────────────────────────────────────

def increment_user_reputation(uid: str, amount: int) -> bool:
    try:
        if FIREBASE_ENABLED:
            ref = get_db().collection("users").document(uid)
            doc = ref.get()
            if doc.exists:
                current = doc.to_dict().get("reputationScore", 0)
                ref.update({"reputationScore": current + amount})
            else:
                ref.set({
                    "uid": uid,
                    "reputationScore": amount,
                    "displayName": "Anonymous Driver"
                }, merge=True)
        return True
    except Exception as e:
        logger.error(f"increment_user_reputation failed: {e}")
        return False


def get_leaderboard_firestore() -> list:
    try:
        if FIREBASE_ENABLED:
            docs = (
                get_db()
                .collection("users")
                .order_by("reputationScore", direction=firestore.Query.DESCENDING)
                .limit(10)
                .stream()
            )
            leaderboard = []
            for idx, doc in enumerate(docs):
                d = doc.to_dict()
                leaderboard.append({
                    "uid": d.get("uid", ""),
                    "displayName": d.get("displayName") or "Anonymous Driver",
                    "reputationScore": d.get("reputationScore", 0),
                    "rank": idx + 1
                })
            return leaderboard
    except Exception as e:
        logger.error(f"get_leaderboard_firestore failed: {e}")
    return [
        {"uid": "u1", "displayName": "Ahmad K.", "reputationScore": 1500, "rank": 1},
        {"uid": "u2", "displayName": "Zainab R.", "reputationScore": 1250, "rank": 2},
        {"uid": "u3", "displayName": "Usman B.", "reputationScore": 980, "rank": 3}
    ]


def get_user_rank_firestore(uid: str) -> dict:
    try:
        if FIREBASE_ENABLED:
            docs = (
                get_db()
                .collection("users")
                .order_by("reputationScore", direction=firestore.Query.DESCENDING)
                .stream()
            )
            count = 0
            for idx, doc in enumerate(docs):
                count += 1
                d = doc.to_dict()
                if d.get("uid") == uid:
                    return {
                        "uid": uid,
                        "displayName": d.get("displayName") or "You",
                        "reputationScore": d.get("reputationScore", 0),
                        "rank": idx + 1
                    }
            
            # If user has no reputationScore, they are excluded from order_by. Fetch their actual doc:
            user_doc = get_db().collection("users").document(uid).get()
            if user_doc.exists:
                d = user_doc.to_dict()
                return {
                    "uid": uid,
                    "displayName": d.get("displayName") or "You",
                    "reputationScore": d.get("reputationScore", 0),
                    "rank": count + 1
                }
    except Exception as e:
        logger.error(f"get_user_rank_firestore failed: {e}")
    return {
        "uid": uid,
        "displayName": "You",
        "reputationScore": 450,
        "rank": 45
    }

# ─────────────────────────────────────────────
# COMMENTS
# ─────────────────────────────────────────────

def add_comment(report_id: str, uid: str, display_name: str, text: str) -> dict:
    import time
    comment_id = f"comment_{int(time.time())}_{uid[:4]}"
    
    resolved_name = display_name or "Anonymous Driver"
    try:
        if FIREBASE_ENABLED:
            user_doc = get_db().collection("users").document(uid).get()
            if user_doc.exists:
                d = user_doc.to_dict()
                resolved_name = d.get("displayName") or d.get("display_name") or resolved_name
    except Exception as e:
        logger.warning(f"Failed to lookup display name for comment: {e}")

    comment_data = {
        "comment_id": comment_id,
        "report_id": report_id,
        "uid": uid,
        "displayName": resolved_name,
        "text": text,
        "timestamp": int(time.time() * 1000)
    }
    try:
        if FIREBASE_ENABLED:
            get_db().collection("comments").document(comment_id).set(comment_data)
        else:
            logger.warning(f"MOCK comment saved: {comment_data}")
    except Exception as e:
        logger.error(f"add_comment failed: {e}")
    return comment_data


def get_comments(report_id: str) -> list:
    import time
    try:
        if FIREBASE_ENABLED:
            docs = (
                get_db()
                .collection("comments")
                .where("report_id", "==", report_id)
                .stream()
            )
            comments = []
            for doc in docs:
                d = doc.to_dict()
                disp = d.get("displayName")
                uid = d.get("uid", "unknown")
                if not disp or disp == "Anonymous Driver":
                    disp = f"anon_{uid}"
                comments.append({
                    **d,
                    "displayName": disp
                })
            comments.sort(key=lambda x: x.get("timestamp", 0), reverse=True)
            return comments
    except Exception as e:
        logger.error(f"get_comments failed: {e}")
    return []

# ─────────────────────────────────────────────
# VOTES
# ─────────────────────────────────────────────

def save_vote(report_id: str, uid: str, is_upvote: bool) -> bool:
    import time
    vote_id = f"{uid}_{report_id}"
    vote_data = {
        "vote_id": vote_id,
        "report_id": report_id,
        "uid": uid,
        "is_upvote": is_upvote,
        "timestamp": int(time.time() * 1000)
    }
    try:
        if FIREBASE_ENABLED:
            ref = get_db().collection("votes").document(vote_id)
            ref.set(vote_data)
            return True
    except Exception as e:
        logger.error(f"save_vote failed: {e}")
    return False


def get_vote_stats(report_id: str) -> dict:
    try:
        if FIREBASE_ENABLED:
            docs = (
                get_db()
                .collection("votes")
                .where("report_id", "==", report_id)
                .stream()
            )
            votes = [doc.to_dict() for doc in docs]
            upvotes = sum(1 for v in votes if v.get("is_upvote", True))
            downvotes = len(votes) - upvotes
            return {"upvotes": upvotes, "downvotes": downvotes}
    except Exception as e:
        logger.error(f"get_vote_stats failed: {e}")
    return {"upvotes": 12, "downvotes": 2}


# ─────────────────────────────────────────────
# SOS ALERTS
# ─────────────────────────────────────────────

def save_sos(sos_data: dict) -> bool:
    try:
        if FIREBASE_ENABLED:
            get_db().collection("sos_alerts").document(
                sos_data["sos_id"]
            ).set(sos_data)
        else:
            logger.warning(f"MOCK SOS saved: {sos_data}")
            _mock_sos.append(sos_data)
        return True
    except Exception as e:
        logger.error(f"save_sos failed: {e}")
        return False


def get_all_sos() -> list:
    try:
        if FIREBASE_ENABLED:
            return [
                doc.to_dict()
                for doc in get_db().collection("sos_alerts").stream()
            ]
        else:
            return _mock_sos
    except Exception as e:
        logger.error(f"get_all_sos failed: {e}")
    return []


# ─────────────────────────────────────────────
# USER PROFILE & PICTURES
# ─────────────────────────────────────────────

def upload_profile_picture(uid: str, file_bytes: bytes, content_type: str) -> str:
    try:
        if FIREBASE_ENABLED:
            bucket = storage.bucket("poetic-fact-496519-k2.firebasestorage.app")
            blob = bucket.blob(f"profiles/{uid}.jpg")
            blob.upload_from_string(file_bytes, content_type=content_type)
            try:
                blob.make_public()
                return blob.public_url
            except Exception as pe:
                logger.warning(f"make_public failed: {pe}. Returning direct URL.")
                return f"https://storage.googleapis.com/poetic-fact-496519-k2.firebasestorage.app/profiles/{uid}.jpg"
    except Exception as e:
        logger.error(f"upload_profile_picture failed: {e}")
    return ""


def count_user_reports(uid: str) -> int:
    try:
        if FIREBASE_ENABLED:
            reports_ref = get_db().collection("reports")
            query = reports_ref.where("user_id", "==", uid)
            docs = query.stream()
            return sum(1 for _ in docs)
    except Exception as e:
        logger.error(f"count_user_reports failed: {e}")
    return 0