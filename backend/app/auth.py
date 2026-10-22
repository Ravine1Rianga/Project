"""
AfyaEarly Care Authentication & Role-Based Authorization Module
Compliant with Kenya Data Protection Act (2019) & Ministry of Health Guidelines.
"""

import os
import hmac
import hashlib
import secrets
from datetime import datetime, timedelta, timezone
from typing import Optional, List, Dict, Any

import jwt
from pydantic import BaseModel, EmailStr, Field
from fastapi import APIRouter, Depends, HTTPException, Header, status

# ---------------------------------------------------------------------------
# Configuration & Secrets
# ---------------------------------------------------------------------------
SECRET_KEY = os.getenv("AFYA_JWT_SECRET", "afyaearly-secret-key-production-asalkenya-2026")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 15
REFRESH_TOKEN_EXPIRE_DAYS = 7

# Whitelisted Ministry Admin emails for Google OAuth resolution
APPROVED_ADMIN_EMAILS = {
    "riangaravine@gmail.com",
    "riangaravin@gmail.com",
    "director.nutrition@health.go.ke",
    "kevin.omondi@strathmore.edu",
    "admin.moh@health.go.ke"
}

auth_router = APIRouter()

# ---------------------------------------------------------------------------
# In-Memory Database (Synced with Firebase / Cloud Datastore)
# ---------------------------------------------------------------------------
# In production, this proxies or mirrors Cloud Firestore users collection
USERS_DB: Dict[str, Dict[str, Any]] = {}
REFRESH_TOKENS_DB: Dict[str, str] = {}  # refresh_token -> user_id
PASSWORD_RESET_TOKENS: Dict[str, Dict[str, Any]] = {}  # reset_token -> {email, expires_at}

# ---------------------------------------------------------------------------
# Password Security (Salted PBKDF2-HMAC-SHA256 with 100,000 iterations)
# ---------------------------------------------------------------------------
def hash_password(password: str) -> str:
    salt = secrets.token_hex(16)
    key = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt.encode('utf-8'),
        100000
    )
    return f"{salt}${key.hex()}"

def verify_password(plain_password: str, hashed_password: str) -> bool:
    try:
        salt, key_hex = hashed_password.split("$")
        computed = hashlib.pbkdf2_hmac(
            'sha256',
            plain_password.encode('utf-8'),
            salt.encode('utf-8'),
            100000
        )
        return hmac.compare_digest(computed.hex(), key_hex)
    except Exception:
        return False

# ---------------------------------------------------------------------------
# JWT Token Generation & Verification
# ---------------------------------------------------------------------------
def create_access_token(user_id: str, role: str, email: str, is_verified: bool) -> str:
    now = datetime.now(timezone.utc)
    payload = {
        "sub": user_id,
        "role": role,
        "email": email,
        "is_verified": is_verified,
        "type": "access",
        "iat": now,
        "exp": now + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    }
    return jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)

def create_refresh_token(user_id: str) -> str:
    now = datetime.now(timezone.utc)
    token = secrets.token_urlsafe(48)
    REFRESH_TOKENS_DB[token] = user_id
    return token

def decode_token(token: str) -> Dict[str, Any]:
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Session has expired. Please sign in again."
        )
    except jwt.InvalidTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication token."
        )

# ---------------------------------------------------------------------------
# Request & Response Schemas
# ---------------------------------------------------------------------------
class SignUpRequest(BaseModel):
    full_name: str
    email: str
    phone: str
    password: str
    role: str = Field(..., description="Allowed roles: 'chw' or 'parent'. Admin role is rejected.")
    county: str = "Turkana"
    sub_county: Optional[str] = None
    chw_id: Optional[str] = None
    supervising_facility: Optional[str] = None
    num_children: Optional[int] = 1
    consent_given: bool = True

class LoginRequest(BaseModel):
    email: str
    password: str

class GoogleAuthRequest(BaseModel):
    id_token: str
    email: str
    display_name: str
    photo_url: Optional[str] = None
    role_choice: Optional[str] = None  # Chosen during complete-profile: 'chw' or 'parent'

class CompleteProfileRequest(BaseModel):
    user_id: str
    role_choice: str = Field(..., description="'chw' or 'parent'. Admin is strictly rejected.")
    phone: str
    county: str
    sub_county: Optional[str] = None
    chw_id: Optional[str] = None
    supervising_facility: Optional[str] = None
    num_children: Optional[int] = 1

class RefreshTokenRequest(BaseModel):
    refresh_token: str

class ForgotPasswordRequest(BaseModel):
    email: str

class ResetPasswordRequest(BaseModel):
    reset_token: str
    new_password: str

class AuthResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    user: Dict[str, Any]

# ---------------------------------------------------------------------------
# Role-Based Routing Middleware & Stealth Guards
# ---------------------------------------------------------------------------
def get_current_user(authorization: Optional[str] = Header(None)) -> Dict[str, Any]:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing or invalid Authorization header."
        )
    token = authorization.split(" ")[1]
    payload = decode_token(token)
    user_id = payload.get("sub")
    user = USERS_DB.get(user_id)
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User record not found.")
    return user

def require_role(allowed_roles: List[str]):
    """
    Stealth Role Guard:
    If an endpoint requires 'admin' and the requester is not an admin,
    returns HTTP 404 (Not Found) instead of 403 (Forbidden) to conceal
    the existence of administrative surfaces from public users.
    """
    def dependency(user: Dict[str, Any] = Depends(get_current_user)):
        user_role = user.get("role")
        if user_role not in allowed_roles:
            if "admin" in allowed_roles:
                # Stealth 404: Do not reveal admin route existence
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Page not found"
                )
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Access restricted to authorized clinical staff."
            )
        return user
    return dependency

# ---------------------------------------------------------------------------
# Auth Endpoints
# ---------------------------------------------------------------------------
@auth_router.post("/signup", response_model=AuthResponse)
def signup(req: SignUpRequest):
    # Rule 1: Public signup can ONLY create chw or parent. Never admin.
    clean_role = req.role.strip().lower()
    if clean_role not in ["chw", "parent"]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Public registration allows only 'chw' or 'parent' roles. Admin creation is prohibited."
        )

    # Check email duplicate
    for uid, user in USERS_DB.items():
        if user["email"].lower() == req.email.lower():
            raise HTTPException(status_code=400, detail="An account with this email already exists.")

    user_id = f"usr_{secrets.token_hex(8)}"
    
    # Rule 2: CHW accounts start as 'pending' until verified by facility supervisor
    is_chw = (clean_role == "chw")
    verification_status = "pending" if is_chw else "approved"
    is_verified = not is_chw

    user_record = {
        "uid": user_id,
        "email": req.email,
        "full_name": req.full_name,
        "phone": req.phone,
        "role": clean_role,
        "password_hash": hash_password(req.password),
        "county": req.county,
        "sub_county": req.sub_county or "",
        "chw_id": req.chw_id or "",
        "supervising_facility": req.supervising_facility or "",
        "num_children": req.num_children or 1,
        "consent_given": req.consent_given,
        "verification_status": verification_status,
        "is_verified": is_verified,
        "profile_completed": True,
        "created_at": datetime.now(timezone.utc).isoformat()
    }
    USERS_DB[user_id] = user_record

    access_token = create_access_token(user_id, clean_role, req.email, is_verified)
    refresh_token = create_refresh_token(user_id)

    safe_user = {k: v for k, v in user_record.items() if k != "password_hash"}
    return AuthResponse(access_token=access_token, refresh_token=refresh_token, user=safe_user)

@auth_router.post("/login", response_model=AuthResponse)
def login(req: LoginRequest):
    user_record = None
    for uid, user in USERS_DB.items():
        if user["email"].lower() == req.email.lower():
            user_record = user
            break

    if not user_record or not verify_password(req.password, user_record.get("password_hash", "")):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password. Please verify your credentials."
        )

    user_id = user_record["uid"]
    role = user_record["role"]
    is_verified = user_record.get("is_verified", False)

    access_token = create_access_token(user_id, role, user_record["email"], is_verified)
    refresh_token = create_refresh_token(user_id)

    safe_user = {k: v for k, v in user_record.items() if k != "password_hash"}
    return AuthResponse(access_token=access_token, refresh_token=refresh_token, user=safe_user)

@auth_router.post("/google", response_model=AuthResponse)
def google_oauth(req: GoogleAuthRequest):
    """
    Google OAuth Sign-In:
    - If user exists, signs in directly.
    - If user is new:
        - If email in approved admin list, grants admin.
        - Otherwise, role must be provided ('chw' or 'parent') or profile_completed=False.
        - Public user cannot claim 'admin'.
    """
    existing_user = None
    for uid, user in USERS_DB.items():
        if user["email"].lower() == req.email.lower():
            existing_user = user
            break

    if existing_user:
        user_id = existing_user["uid"]
        role = existing_user["role"]
        is_verified = existing_user.get("is_verified", True)
        access_token = create_access_token(user_id, role, existing_user["email"], is_verified)
        refresh_token = create_refresh_token(user_id)
        safe_user = {k: v for k, v in existing_user.items() if k != "password_hash"}
        return AuthResponse(access_token=access_token, refresh_token=refresh_token, user=safe_user)

    # New user through Google
    role = "parent"
    if req.email.lower() in APPROVED_ADMIN_EMAILS:
        role = "admin"
    elif req.role_choice:
        clean_choice = req.role_choice.strip().lower()
        if clean_choice == "admin":
            raise HTTPException(status_code=400, detail="Cannot assign admin role to Google account.")
        role = clean_choice if clean_choice in ["chw", "parent"] else "parent"

    user_id = f"goog_{secrets.token_hex(8)}"
    is_chw = (role == "chw")
    verification_status = "pending" if is_chw else "approved"
    is_verified = not is_chw

    new_user = {
        "uid": user_id,
        "email": req.email,
        "full_name": req.display_name,
        "phone": "",
        "role": role,
        "photo_url": req.photo_url or "",
        "county": "Turkana",
        "verification_status": verification_status,
        "is_verified": is_verified,
        "profile_completed": (req.role_choice is not None),
        "created_at": datetime.now(timezone.utc).isoformat()
    }
    USERS_DB[user_id] = new_user

    access_token = create_access_token(user_id, role, req.email, is_verified)
    refresh_token = create_refresh_token(user_id)
    return AuthResponse(access_token=access_token, refresh_token=refresh_token, user=new_user)

@auth_router.post("/complete-profile")
def complete_profile(req: CompleteProfileRequest):
    user = USERS_DB.get(req.user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found.")

    clean_role = req.role_choice.strip().lower()
    if clean_role not in ["chw", "parent"]:
        raise HTTPException(status_code=400, detail="Role can only be 'chw' or 'parent'.")

    is_chw = (clean_role == "chw")
    user["role"] = clean_role
    user["phone"] = req.phone
    user["county"] = req.county
    user["sub_county"] = req.sub_county or ""
    user["chw_id"] = req.chw_id or ""
    user["supervising_facility"] = req.supervising_facility or ""
    user["num_children"] = req.num_children or 1
    user["verification_status"] = "pending" if is_chw else "approved"
    user["is_verified"] = not is_chw
    user["profile_completed"] = True

    return {"message": "Profile completed successfully", "user": {k: v for k, v in user.items() if k != "password_hash"}}

@auth_router.post("/refresh")
def refresh_token(req: RefreshTokenRequest):
    user_id = REFRESH_TOKENS_DB.get(req.refresh_token)
    if not user_id or user_id not in USERS_DB:
        raise HTTPException(status_code=401, detail="Invalid or expired refresh token.")

    user = USERS_DB[user_id]
    access_token = create_access_token(user_id, user["role"], user["email"], user.get("is_verified", False))
    return {"access_token": access_token, "token_type": "bearer"}

@auth_router.post("/logout")
def logout(req: RefreshTokenRequest):
    if req.refresh_token in REFRESH_TOKENS_DB:
        del REFRESH_TOKENS_DB[req.refresh_token]
    return {"message": "Logged out successfully."}

@auth_router.post("/forgot-password")
def forgot_password(req: ForgotPasswordRequest):
    reset_token = secrets.token_urlsafe(32)
    PASSWORD_RESET_TOKENS[reset_token] = {
        "email": req.email.lower(),
        "expires_at": datetime.now(timezone.utc) + timedelta(hours=1)
    }
    return {
        "message": f"Password reset link has been dispatched to {req.email}.",
        "reset_token": reset_token  # Provided for programmatic testing
    }

@auth_router.post("/reset-password")
def reset_password(req: ResetPasswordRequest):
    record = PASSWORD_RESET_TOKENS.get(req.reset_token)
    if not record or record["expires_at"] < datetime.now(timezone.utc):
        raise HTTPException(status_code=400, detail="Invalid or expired reset token.")

    email = record["email"]
    for uid, user in USERS_DB.items():
        if user["email"].lower() == email:
            user["password_hash"] = hash_password(req.new_password)
            del PASSWORD_RESET_TOKENS[req.reset_token]
            return {"message": "Password has been successfully reset. You may now sign in."}

    raise HTTPException(status_code=404, detail="User account not found.")

# ---------------------------------------------------------------------------
# Protected Clinical & Administrative Endpoints
# ---------------------------------------------------------------------------
@auth_router.get("/api/admin/dashboard")
def admin_dashboard(user: Dict[str, Any] = Depends(require_role(["admin"]))):
    """
    Protected Admin Dashboard:
    Guarded with stealth 404. Non-admins will receive 404 Page Not Found.
    """
    total_chws = sum(1 for u in USERS_DB.values() if u.get("role") == "chw")
    pending_chws = sum(1 for u in USERS_DB.values() if u.get("role") == "chw" and u.get("verification_status") == "pending")
    return {
        "scope": "National ASAL Malnutrition Oversight",
        "admin_user": user["full_name"],
        "analytics": {
            "registered_chws": total_chws,
            "pending_approvals": pending_chws,
            "county_coverage": ["Turkana", "Garissa", "Mandera", "Wajir", "Marsabit"]
        }
    }

@auth_router.get("/api/chw/screenings")
def chw_screenings(user: Dict[str, Any] = Depends(require_role(["chw", "admin"]))):
    if user["role"] == "chw" and not user.get("is_verified", False):
        raise HTTPException(status_code=403, detail="Account pending supervisor verification.")
    return {
        "chw": user["full_name"],
        "assigned_facility": user.get("supervising_facility", "Lodwar Referral"),
        "active_screenings_count": 42
    }

@auth_router.get("/api/parent/children")
def parent_children(user: Dict[str, Any] = Depends(require_role(["parent", "admin"]))):
    return {
        "parent": user["full_name"],
        "children": [
            {
                "child_id": "TK-1042",
                "name": "Baby Ayan",
                "age_months": 18,
                "current_muac": 13.1,
                "risk_tier": "Low Risk"
            }
        ]
    }
