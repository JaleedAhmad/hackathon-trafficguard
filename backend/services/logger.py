import logging
from datetime import datetime, timezone

logger = logging.getLogger("agent_events")
logger.setLevel(logging.INFO)

def log_agent_event(event_type: str, data: dict):
    logger.info(f"[{datetime.now(timezone.utc).isoformat()}] {event_type}: {data}")
