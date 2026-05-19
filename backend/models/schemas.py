from pydantic import BaseModel
from typing import Optional, List

class RawSignal(BaseModel):
    signal_id: str
    text: str
    source: str
    lat: float
    lng: float
    timestamp: str
    language: Optional[str] = None
    category: Optional[str] = None
    severity: Optional[str] = None

class SignalObject(BaseModel):
    signal_id: str
    text_normalized: str
    text_original: str
    language_detected: str
    source_type: str
    lat: float
    lng: float
    timestamp: str
    category_hint: str

class CrisisDetectionResult(BaseModel):
    crisis_detected: bool
    status: str
    crisis_type: str
    confidence_score: int
    reasoning: str
    contradictions: List[str]

class ActionItem(BaseModel):
    action_id: str
    priority: int
    type: str
    description: str
    estimated_impact: str
    requires_api: bool

class SituationPlanResult(BaseModel):
    severity: str
    affected_radius_km: str
    eta_impact_minutes: int
    escalation_probability: str
    actions: List[ActionItem]
    ai_reasoning: str


class UserProfileResponse(BaseModel):
    uid: str
    displayName: str
    age: int
    gender: str
    photoUrl: str
    totalReports: int


class UpdateProfileRequest(BaseModel):
    displayName: str
    age: int
    gender: str
    photoUrl: str
