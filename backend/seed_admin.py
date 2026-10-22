"""
AfyaEarly Care Administrator Seed Script
Protected backend utility to initialize the first Ministry Administrator account.
Admins can NEVER be created via public signup endpoints.
"""

import argparse
import sys
import os

# Add project root to sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from backend.app.auth import USERS_DB, hash_password

def seed_admin_account(
    email: str = "riangaravine@gmail.com",
    password: str = "214129o3A@",
    full_name: str = "Ravine Riang'a (Admin)",
    county: str = "National MOH Headquarters"
) -> dict:
    admin_id = "admin_gov_super01"
    admin_record = {
        "uid": admin_id,
        "email": email.strip().lower(),
        "full_name": full_name.strip(),
        "phone": "+254 722 000 111",
        "role": "admin",
        "password_hash": hash_password(password),
        "county": county,
        "verification_status": "approved",
        "is_verified": True,
        "profile_completed": True,
        "created_at": "2026-01-01T00:00:00Z"
    }
    USERS_DB[admin_id] = admin_record
    print("=" * 60)
    print("[OK] AFYAEARLY CARE ADMIN ACCOUNT SEEDED SUCCESSFULLY")
    print(f"  UID:        {admin_id}")
    print(f"  Name:       {full_name}")
    print(f"  Email:      {email}")
    print(f"  Role:       admin (Access to Ministry Dashboard)")
    print(f"  Status:     Verified & Active")
    print("=" * 60)
    return admin_record

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Seed initial Ministry Admin account for AfyaEarly Care")
    parser.add_argument("--email", default="riangaravine@gmail.com", help="Admin email address")
    parser.add_argument("--password", default="214129o3A@", help="Admin secure password")
    parser.add_argument("--name", default="Ravine Riang'a (Admin)", help="Admin full name")
    parser.add_argument("--county", default="National MOH Headquarters", help="Admin administrative scope")

    args = parser.parse_args()
    seed_admin_account(
        email=args.email,
        password=args.password,
        full_name=args.name,
        county=args.county
    )
