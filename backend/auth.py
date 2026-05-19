"""
FastAPI production-grade authentication module.

Responsibilities:
- Verify Firebase ID tokens via firebase_admin.auth
- Read / write Firestore user profiles
- Expose /auth/verify, /auth/me, /auth/logout endpoints
- Provide a reusable `get_current_user` FastAPI dependency
"""

from __future__ import annotations

from dotenv import load_dotenv
load_dotenv()

import logging
from datetime import datetime, timezone
from functools import lru_cache
from typing import Optional

import firebase_admin
from fastapi import Depends, FastAPI, HTTPException, Security, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from firebase_admin import auth as firebase_auth
from firebase_admin import credentials, firestore
from google.cloud.firestore_v1 import SERVER_TIMESTAMP
from pydantic import BaseModel

logger = logging.getLogger(__name__)

# ─── Firebase Admin bootstrap (idempotent) ─────────────────────────────────────

def _init_firebase() -> None:
    """Initialize Firebase Admin SDK if not already done."""
    if not firebase_admin._apps:
        import os
        cred_path = os.getenv("FIREBASE_CREDENTIALS_PATH", "./firebase_credentials.json")
        cred = credentials.Certificate(cred_path)
        bucket_name = os.getenv("FIREBASE_STORAGE_BUCKET") or "poetic-fact-496519-k2.firebasestorage.app"
        firebase_admin.initialize_app(cred, {
            "storageBucket": bucket_name
        })
        logger.info(f"Firebase Admin SDK initialized with Storage support: {bucket_name}")


@lru_cache(maxsize=1)
def get_firestore_client():
    _init_firebase()
    return firestore.client()


# ─── Pydantic schemas ──────────────────────────────────────────────────────────

class FirebaseUser(BaseModel):
    uid: str
    email: Optional[str] = None
    display_name: Optional[str] = None
    photo_url: Optional[str] = None
    auth_provider: str = "email"
    email_verified: bool = False


class VerifyTokenResponse(BaseModel):
    uid: str
    email: Optional[str]
    displayName: Optional[str]
    isActive: bool
    message: str


class MeResponse(BaseModel):
    uid: str
    email: Optional[str]
    displayName: Optional[str]
    photoUrl: Optional[str]
    authProvider: str
    createdAt: Optional[str]
    lastLoginAt: Optional[str]


class LogoutResponse(BaseModel):
    message: str


# ─── Token verification dependency ────────────────────────────────────────────

_bearer_scheme = HTTPBearer(auto_error=True)


async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Security(_bearer_scheme),
) -> FirebaseUser:
    """
    FastAPI dependency that verifies the Firebase ID token supplied in the
    `Authorization: Bearer <token>` header.  Raises HTTP 401 on failure.
    """
    _init_firebase()
    token = credentials.credentials
    try:
        decoded = firebase_auth.verify_id_token(token, check_revoked=True)
    except firebase_auth.RevokedIdTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token has been revoked. Please sign in again.",
        )
    except firebase_auth.ExpiredIdTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token has expired. Please refresh your session.",
        )
    except firebase_auth.InvalidIdTokenError as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=f"Invalid token: {exc}",
        )
    except Exception as exc:
        logger.error("Unexpected token verification error: %s", exc)
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not verify credentials.",
        )

    provider_data = decoded.get("firebase", {}).get("sign_in_provider", "email")
    return FirebaseUser(
        uid=decoded["uid"],
        email=decoded.get("email"),
        display_name=decoded.get("name"),
        photo_url=decoded.get("picture"),
        auth_provider=provider_data,
        email_verified=decoded.get("email_verified", False),
    )


# ─── Firestore profile helpers ─────────────────────────────────────────────────

def _upsert_profile(user: FirebaseUser, is_new: bool = False) -> None:
    """Create or update the Firestore /users/{uid} document."""
    db = get_firestore_client()
    ref = db.collection("users").document(user.uid)
    now = datetime.now(timezone.utc).isoformat()

    data: dict = {
        "uid": user.uid,
        "email": user.email,
        "displayName": user.display_name,
        "photoUrl": user.photo_url,
        "authProvider": user.auth_provider,
        "isActive": True,
        "lastLoginAt": SERVER_TIMESTAMP,
        "updatedAt": SERVER_TIMESTAMP,
    }
    if is_new:
        data["createdAt"] = SERVER_TIMESTAMP

    ref.set(data, merge=True)
    logger.info("Firestore profile upserted for uid=%s", user.uid)


def _get_profile(uid: str) -> Optional[dict]:
    db = get_firestore_client()
    doc = db.collection("users").document(uid).get()
    return doc.to_dict() if doc.exists else None


# ─── Auth router factory ───────────────────────────────────────────────────────

def create_auth_router() -> FastAPI:
    """
    Returns a FastAPI router (sub-application) with all auth endpoints.
    Mount it with app.include_router(create_auth_router(), prefix="/auth").
    """
    from fastapi import APIRouter

    router = APIRouter(tags=["auth"])

    @router.post("/verify", response_model=VerifyTokenResponse)
    async def verify_token(user: FirebaseUser = Depends(get_current_user)):
        """
        Verifies a Firebase ID token and syncs the user profile to Firestore.
        Returns basic profile info on success.
        """
        existing = _get_profile(user.uid)
        _upsert_profile(user, is_new=(existing is None))
        return VerifyTokenResponse(
            uid=user.uid,
            email=user.email,
            displayName=user.display_name,
            isActive=True,
            message="Token verified and profile synced.",
        )

    @router.get("/me", response_model=MeResponse)
    async def get_me(user: FirebaseUser = Depends(get_current_user)):
        """Returns the authenticated user's Firestore profile."""
        profile = _get_profile(user.uid)
        if profile is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User profile not found.",
            )

        def _ts(field) -> Optional[str]:
            v = profile.get(field)
            if v is None:
                return None
            if hasattr(v, "isoformat"):
                return v.isoformat()
            return str(v)

        return MeResponse(
            uid=profile.get("uid", user.uid),
            email=profile.get("email"),
            displayName=profile.get("displayName"),
            photoUrl=profile.get("photoUrl"),
            authProvider=profile.get("authProvider", "email"),
            createdAt=_ts("createdAt"),
            lastLoginAt=_ts("lastLoginAt"),
        )

    @router.post("/logout", response_model=LogoutResponse)
    async def logout(user: FirebaseUser = Depends(get_current_user)):
        """
        Revokes all Firebase refresh tokens for the authenticated user.
        The client must also call FirebaseAuth.signOut() locally.
        """
        _init_firebase()
        try:
            firebase_auth.revoke_refresh_tokens(user.uid)
            logger.info("Refresh tokens revoked for uid=%s", user.uid)
        except Exception as exc:
            logger.error("Token revocation failed for uid=%s: %s", user.uid, exc)
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Failed to revoke session. Please try again.",
            )
        return LogoutResponse(message="Session revoked. Please sign in again.")

    return router
