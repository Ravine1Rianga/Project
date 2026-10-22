"""
AfyaEarly Care Authentication & Security Test Checklist
Covers the 6 core scenarios required by the architecture specification:
1. Parent Signup (approved status, parent role)
2. CHW Signup (pending verification status, chw role)
3. Google First-Time Login (new user requires profile completion; cannot claim admin)
4. Admin Login Redirect (receives admin role and accesses dashboard)
5. Non-Admin Accessing Admin URL (returns stealth 404, not 403)
6. Tampered Role Claim in JWT (rejected signature / 401 Unauthorized)
"""

import sys
import os
import pytest
from fastapi.testclient import TestClient

# Add project root to sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "../..")))

from backend.app.main import app
from backend.app.auth import USERS_DB, SECRET_KEY, ALGORITHM
from backend.seed_admin import seed_admin_account
import jwt

client = TestClient(app)

@pytest.fixture(autouse=True)
def setup_test_environment():
    """Clear memory database and seed standard admin before each test."""
    USERS_DB.clear()
    seed_admin_account(
        email="director.nutrition@health.go.ke",
        password="KenyaMOH@2026Admin",
        full_name="Dr. Kevin Ochieng Omondi"
    )

# ---------------------------------------------------------------------------
# Test 1: Parent Signup
# ---------------------------------------------------------------------------
def test_1_parent_signup():
    """Verify Parent signup sets role='parent' and status='approved'."""
    payload = {
        "full_name": "Amina Hassan",
        "email": "amina.hassan@example.com",
        "phone": "+254712345678",
        "password": "ParentPassword123!",
        "role": "parent",
        "county": "Turkana",
        "num_children": 2,
        "consent_given": True
    }
    response = client.post("/signup", json=payload)
    assert response.status_code == 200, response.text
    data = response.json()

    assert data["user"]["role"] == "parent"
    assert data["user"]["verification_status"] == "approved"
    assert data["user"]["is_verified"] is True
    assert "access_token" in data
    assert "refresh_token" in data

    # Verify public cannot set admin role
    tampered_signup = payload.copy()
    tampered_signup["email"] = "fake.admin@strath.edu"
    tampered_signup["role"] = "admin"
    bad_res = client.post("/signup", json=tampered_signup)
    assert bad_res.status_code == 400
    assert "Admin creation is prohibited" in bad_res.json()["detail"]

# ---------------------------------------------------------------------------
# Test 2: CHW Signup
# ---------------------------------------------------------------------------
def test_2_chw_signup():
    """Verify CHW signup sets role='chw' and status='pending' until supervisor verification."""
    payload = {
        "full_name": "Maria Akiru",
        "email": "maria.akiru@health.go.ke",
        "phone": "+254720987654",
        "password": "ChwSecurePassword2026!",
        "role": "chw",
        "county": "Turkana",
        "sub_county": "Turkana West",
        "chw_id": "CHU-TK-104",
        "supervising_facility": "Kakuma Sub-County Hospital",
        "consent_given": True
    }
    response = client.post("/signup", json=payload)
    assert response.status_code == 200, response.text
    data = response.json()

    assert data["user"]["role"] == "chw"
    assert data["user"]["verification_status"] == "pending"
    assert data["user"]["is_verified"] is False
    assert data["user"]["chw_id"] == "CHU-TK-104"

# ---------------------------------------------------------------------------
# Test 3: Google First-Time Login & Complete Profile
# ---------------------------------------------------------------------------
def test_3_google_first_time_login():
    """Verify new Google user starts with profile_completed=False and chooses only CHW or Parent."""
    payload = {
        "id_token": "mock-google-id-token-123",
        "email": "john.newuser@gmail.com",
        "display_name": "John Doe",
        "photo_url": "https://example.com/avatar.jpg"
    }
    response = client.post("/google", json=payload)
    assert response.status_code == 200
    data = response.json()
    user = data["user"]

    assert user["profile_completed"] is False
    assert user["role"] == "parent"  # Default initial role

    # Now complete profile as CHW
    complete_payload = {
        "user_id": user["uid"],
        "role_choice": "chw",
        "phone": "+254711223344",
        "county": "Garissa",
        "sub_county": "Dadaab",
        "chw_id": "CHU-GAR-55",
        "supervising_facility": "Garissa County Referral"
    }
    comp_res = client.post("/complete-profile", json=complete_payload)
    assert comp_res.status_code == 200
    updated_user = comp_res.json()["user"]
    assert updated_user["role"] == "chw"
    assert updated_user["verification_status"] == "pending"
    assert updated_user["profile_completed"] is True

    # Confirm user cannot complete profile as admin
    admin_attempt = complete_payload.copy()
    admin_attempt["role_choice"] = "admin"
    bad_comp = client.post("/complete-profile", json=admin_attempt)
    assert bad_comp.status_code == 400

# ---------------------------------------------------------------------------
# Test 4: Admin Login & Role Routing
# ---------------------------------------------------------------------------
def test_4_admin_login_redirect():
    """Verify seeded Admin logs in using the exact same login form and receives role='admin'."""
    login_payload = {
        "email": "director.nutrition@health.go.ke",
        "password": "KenyaMOH@2026Admin"
    }
    response = client.post("/login", json=login_payload)
    assert response.status_code == 200
    data = response.json()

    assert data["user"]["role"] == "admin"
    assert data["user"]["is_verified"] is True

    # Use token to access Admin Dashboard
    token = data["access_token"]
    headers = {"Authorization": f"Bearer {token}"}
    admin_res = client.get("/api/admin/dashboard", headers=headers)
    assert admin_res.status_code == 200
    assert "National ASAL Malnutrition Oversight" in admin_res.json()["scope"]

# ---------------------------------------------------------------------------
# Test 5: Stealth 404 Guard: Non-Admin Accessing Admin URL
# ---------------------------------------------------------------------------
def test_5_non_admin_accessing_admin_url():
    """Verify non-admin (CHW or Parent) receives HTTP 404 (Not Found) rather than 403."""
    # 1. Register and login as parent
    parent_payload = {
        "full_name": "Fatuma Abdi",
        "email": "fatuma@example.com",
        "phone": "+254700000000",
        "password": "Password123!",
        "role": "parent"
    }
    client.post("/signup", json=parent_payload)
    login_res = client.post("/login", json={"email": "fatuma@example.com", "password": "Password123!"})
    parent_token = login_res.json()["access_token"]

    # 2. Try to hit admin endpoint with parent token
    headers = {"Authorization": f"Bearer {parent_token}"}
    admin_res = client.get("/api/admin/dashboard", headers=headers)

    # Must be 404 Not Found to conceal admin portal existence!
    assert admin_res.status_code == 404
    assert admin_res.json()["detail"] == "Page not found"

# ---------------------------------------------------------------------------
# Test 6: Tampered Role Claim in JWT
# ---------------------------------------------------------------------------
def test_6_tampered_role_claim():
    """Verify that tampering with role claim in token results in cryptographic rejection."""
    # Create fake token signed with invalid secret
    fake_secret = "attacker-forged-secret-key"
    fake_payload = {
        "sub": "attacker-01",
        "role": "admin",  # Attacker attempts privilege escalation
        "email": "attacker@evil.com",
        "is_verified": True
    }
    forged_token = jwt.encode(fake_payload, fake_secret, algorithm=ALGORITHM)

    headers = {"Authorization": f"Bearer {forged_token}"}
    response = client.get("/api/admin/dashboard", headers=headers)

    # Must be rejected as 401 Unauthorized
    assert response.status_code == 401
    assert "Invalid authentication token" in response.json()["detail"]
