"""
AfyaEarly Care USSD Gateway Tests
Tests Africa's Talking / Safaricom form-encoded endpoint and Android JSON simulator.
"""

import sys
import os
import pytest
from fastapi.testclient import TestClient

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "../..")))

from backend.app.main import app

client = TestClient(app)

def test_ussd_main_menu_form():
    response = client.post(
        "/ussd",
        data={
            "sessionId": "test-sess-1",
            "serviceCode": "*384*46#",
            "phoneNumber": "+254712345678",
            "text": ""
        }
    )
    assert response.status_code == 200
    text = response.text
    assert "CON Karibu AfyaEarly Care" in text
    assert "Akiru Ewoi" in text
    assert "1. Hali ya Mtoto" in text
    assert "2. Tuma Tahadhari ya Dharura" in text

def test_ussd_status_menu_form():
    response = client.post(
        "/ussd",
        data={
            "sessionId": "test-sess-1",
            "serviceCode": "*384*46#",
            "phoneNumber": "+254712345678",
            "text": "1"
        }
    )
    assert response.status_code == 200
    assert "CON TAHADHARI YA MTOTO: AKIRU EWOI" in response.text
    assert "HAZ" in response.text
    assert "MUAC" in response.text

def test_ussd_emergency_report_form():
    # Step 1: select 2
    res1 = client.post(
        "/ussd",
        data={
            "sessionId": "test-sess-2",
            "serviceCode": "*384*46#",
            "phoneNumber": "+254712345678",
            "text": "2"
        }
    )
    assert res1.status_code == 200
    assert "CON RIPOTI DALILI ZA HATARI" in res1.text

    # Step 2: select 1 (Homa kali)
    res2 = client.post(
        "/ussd",
        data={
            "sessionId": "test-sess-2",
            "serviceCode": "*384*46#",
            "phoneNumber": "+254712345678",
            "text": "2*1"
        }
    )
    assert res2.status_code == 200
    assert "END TAHADHARI YA DHARURA IMETUMWA!" in res2.text
    assert "Maria Akiru" in res2.text

def test_ussd_simulate_json():
    response = client.post(
        "/ussd/simulate",
        json={
            "sessionId": "sim-session-123",
            "serviceCode": "*384*46#",
            "phoneNumber": "+254712345678",
            "text": ""
        }
    )
    assert response.status_code == 200
    data = response.json()
    assert "Karibu AfyaEarly Care" in data["response"]
    assert data["isEnd"] is False

def test_ussd_simulate_english_flow():
    response = client.post(
        "/ussd/simulate",
        json={
            "sessionId": "sim-session-123",
            "serviceCode": "*384*46#",
            "phoneNumber": "+254712345678",
            "text": "5"
        }
    )
    assert response.status_code == 200
    data = response.json()
    assert "AFYAEARLY CARE (ENGLISH)" in data["response"]
    assert "Akiru Ewoi" in data["response"]
