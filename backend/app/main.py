"""
AfyaEarly Care REST API Backend
Provides authentication, AI pediatric malnutrition inference, and clinical data endpoints.
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from backend.app.auth import auth_router
from backend.app.ussd import ussd_router

app = FastAPI(
    title="AfyaEarly Care API",
    description="ASAL Pediatric Malnutrition & Neurodevelopmental Health Platform",
    version="2.0.0"
)

# CORS Configuration for Android App & Web Simulator
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_router)
app.include_router(ussd_router)

@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "service": "AfyaEarly Care Platform",
        "version": "2.0.0",
        "compliance": "Kenya Data Protection Act (2019)"
    }
