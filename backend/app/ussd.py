"""
AfyaEarly Care USSD Gateway Service
Standard USSD protocol compatible with Africa's Talking, Safaricom, and Airtel Kenya.
Provides 2G/feature-phone access to child growth metrics, alerts, and emergency clinical triage.
"""

from fastapi import APIRouter, Form, HTTPException, Request
from pydantic import BaseModel
from typing import Optional, Dict

ussd_router = APIRouter(prefix="/ussd", tags=["USSD"])

# In-memory session state & registered database for ASAL beneficiaries
REGISTERED_CHILDREN_BY_PHONE: Dict[str, dict] = {
    "+254712345678": {
        "guardian_name": "Maria Akiru",
        "child_name": "Akiru Ewoi",
        "child_id": "TK-1042",
        "age_months": 18,
        "haz": -2.4,
        "muac": 12.5,
        "risk_tier": "MODERATE RISK",
        "stunting_status_sw": "Kudumaa kwa Wastani (-2.4 HAZ)",
        "stunting_status_en": "Moderate Stunting (-2.4 HAZ)",
        "assigned_chw": "Maria Akiru",
        "chw_phone": "+254712345678",
        "facility": "Kakuma West Sub-County Hospital",
        "next_clinic_date": "14 Nov 2026",
        "language": "sw"  # default swahili
    },
    "0712345678": {
        "guardian_name": "Maria Akiru",
        "child_name": "Akiru Ewoi",
        "child_id": "TK-1042",
        "age_months": 18,
        "haz": -2.4,
        "muac": 12.5,
        "risk_tier": "MODERATE RISK",
        "stunting_status_sw": "Kudumaa kwa Wastani (-2.4 HAZ)",
        "stunting_status_en": "Moderate Stunting (-2.4 HAZ)",
        "assigned_chw": "Maria Akiru",
        "chw_phone": "+254712345678",
        "facility": "Kakuma West Sub-County Hospital",
        "next_clinic_date": "14 Nov 2026",
        "language": "sw"
    },
    "+254722000000": {
        "guardian_name": "Nalimo Lomor",
        "child_name": "Ekai Lomor",
        "child_id": "TK-1043",
        "age_months": 22,
        "haz": -3.2,
        "muac": 11.2,
        "risk_tier": "HIGH RISK (SAM)",
        "stunting_status_sw": "Kudumaa Kukubwa na Utapiamlo Mkali (-3.2 HAZ)",
        "stunting_status_en": "Severe Stunting & SAM (-3.2 HAZ)",
        "assigned_chw": "Maria Akiru",
        "chw_phone": "+254712345678",
        "facility": "Kakuma West Sub-County Hospital",
        "next_clinic_date": "28 Oct 2026",
        "language": "sw"
    }
}

class USSDRequest(BaseModel):
    sessionId: str
    serviceCode: str
    phoneNumber: str
    text: str = ""

@ussd_router.post("")
async def handle_ussd(
    sessionId: Optional[str] = Form(None),
    serviceCode: Optional[str] = Form(None),
    phoneNumber: Optional[str] = Form(None),
    text: Optional[str] = Form("")
):
    """
    Standard form-encoded endpoint for Africa's Talking / Safaricom USSD gateway.
    """
    s_id = sessionId or "session-demo"
    s_code = serviceCode or "*384*46#"
    p_num = (phoneNumber or "+254712345678").strip()
    user_text = (text or "").strip()

    response = process_ussd_logic(s_id, s_code, p_num, user_text)
    return response

@ussd_router.post("/simulate")
async def simulate_ussd(payload: USSDRequest):
    """
    JSON endpoint for Android in-app USSD simulator.
    """
    response_text = process_ussd_logic(
        payload.sessionId,
        payload.serviceCode,
        payload.phoneNumber.strip(),
        payload.text.strip()
    )
    is_end = response_text.startswith("END")
    clean_text = response_text[4:] if response_text.startswith(("CON ", "END ")) else response_text
    return {
        "response": clean_text,
        "isEnd": is_end,
        "sessionId": payload.sessionId
    }

def process_ussd_logic(session_id: str, service_code: str, phone: str, text: str) -> str:
    # Normalize phone
    clean_phone = phone.replace(" ", "")
    child_data = REGISTERED_CHILDREN_BY_PHONE.get(clean_phone) or REGISTERED_CHILDREN_BY_PHONE.get("+254712345678")

    inputs = [t.strip() for t in text.split("*")] if text else []
    level = len(inputs)

    # Level 0: Main Menu
    if level == 0 or (level == 1 and inputs[0] == ""):
        return (
            f"CON Karibu AfyaEarly Care - Huduma ya Mtoto\n"
            f"Mtoto: {child_data['child_name']} ({child_data['age_months']}m)\n"
            f"1. Hali ya Mtoto & Tahadhari (Health Alerts)\n"
            f"2. Tuma Tahadhari ya Dharura (Report Emergency)\n"
            f"3. Ushauri wa Lishe & RUTF\n"
            f"4. Kliniki & CHW {child_data['assigned_chw']}\n"
            f"5. English / Kiswahili"
        )

    # 1. Baby Status & Alerts
    if inputs[0] == "1":
        if level == 1:
            return (
                f"CON TAHADHARI YA MTOTO: {child_data['child_name'].upper()}\n"
                f"• Urefu/Umri (HAZ): {child_data['haz']} ({child_data['stunting_status_sw']})\n"
                f"• Kipimo cha Mkono (MUAC): {child_data['muac']} cm\n"
                f"• Hatari: {child_data['risk_tier']}\n"
                f"1. Ufafanuzi wa Hatari (Consequences)\n"
                f"2. Tarehe ya Kliniki Inayofuata\n"
                f"0. Rudi Mwanzo"
            )
        elif level == 2 and inputs[1] == "1":
            return (
                f"END MATOKEO YA KIAFYA:\n"
                f"Mtoto ana upungufu wa urefu wa {abs(child_data['haz'])} SD chini ya kiwango cha WHO.\n"
                f"Hatari: Kudumaa kwa ukuaji wa ubongo, kuchelewa kutembea, na mwili kuwa dhaifu.\n"
                f"Hatua: Mpatie chakula cha ziada cha Plumpy'Nut na maziwa ya kutosha."
            )
        elif level == 2 and inputs[1] == "2":
            return (
                f"END TAREHE YA KLINIKI:\n"
                f"Tarehe inayofuata: {child_data['next_clinic_date']}\n"
                f"Kituo: {child_data['facility']}\n"
                f"Afisa: {child_data['assigned_chw']} ({child_data['chw_phone']})\n"
                f"Kumbuka kubeba kitabu cha afya cha mtoto."
            )
        elif level == 2 and inputs[1] == "0":
            return process_ussd_logic(session_id, service_code, phone, "")

    # 2. Emergency Report
    elif inputs[0] == "2":
        if level == 1:
            return (
                f"CON RIPOTI DALILI ZA HATARI:\n"
                f"Mtoto ana dalili gani sasa hivi?\n"
                f"1. Homa kali na kutokunywa maziwa\n"
                f"2. Kuhara sana au kutapika mfululizo\n"
                f"3. Kuvimba miguu na uso (Edema)\n"
                f"4. Udhaifu mkubwa (Mtoto hapumuagi vizuri)"
            )
        elif level == 2:
            symptom_map = {
                "1": "Homa kali na kukataa kula",
                "2": "Kuhara sana na kutapika",
                "3": "Kuvimba miguu na mikono (Edema)",
                "4": "Kupumua kwa shida na udhaifu mkubwa"
            }
            symptom = symptom_map.get(inputs[1], "Dalili za dharura")
            return (
                f"END TAHADHARI YA DHARURA IMETUMWA!\n"
                f"Afisa wa Afya {child_data['assigned_chw']} ({child_data['chw_phone']}) "
                f"ametumiwa ujumbe wa SMS kuwa mtoto {child_data['child_name']} ana '{symptom}'.\n"
                f"Tafadhali mpeleke mtoto mara moja kituo cha afya: {child_data['facility']}."
            )

    # 3. Nutrition & RUTF Guide
    elif inputs[0] == "3":
        if level == 1:
            return (
                f"CON MWONGOZO WA LISHE & RUTF:\n"
                f"1. Jinsi ya Kumpa Plumpy'Nut (RUTF)\n"
                f"2. Vyakula 4 Muhimu Kila Siku\n"
                f"3. Ratiba ya Maziwa ya Mama\n"
                f"0. Rudi Mwanzo"
            )
        elif level == 2 and inputs[1] == "1":
            return (
                f"END MATUMIZI YA RUTF (Plumpy'Nut):\n"
                f"• Mpe mtoto sachet 1 mara 2 kwa siku.\n"
                f"• Mpe maji safi ya kuchemsha baada ya kula.\n"
                f"• Usichanganye RUTF na chakula kingine.\n"
                f"• Usitupe mabaki; funika vizuri mahali pakavu."
            )
        elif level == 2 and inputs[1] == "2":
            return (
                f"END VYAKULA 4 MUHIMU (DDI):\n"
                f"1. Nafaka (Uji wa mtama au mahindi)\n"
                f"2. Protini (Maziwa safi, mayai au maharagwe)\n"
                f"3. Mboga za majani zenye Vit-A (Mchicha, karoti)\n"
                f"4. Matunda (Ndizi au embe)\n"
                f"Lisha mtoto mara 3 au zaidi kwa siku."
            )
        elif level == 2 and inputs[1] == "3":
            return (
                f"END MAZIWA YA MAMA:\n"
                f"Endelea kunyonyesha mtoto hadi anapotimiza miaka 2 au zaidi, "
                f"pamoja na kumpatia vyakula vizito vya lishe bora."
            )

    # 4. CHW & Clinic Details
    elif inputs[0] == "4":
        return (
            f"END KITUO CHA AFYA & AFISA:\n"
            f"• Afisa: {child_data['assigned_chw']}\n"
            f"• Simu: {child_data['chw_phone']}\n"
            f"• Kituo: {child_data['facility']}\n"
            f"• Kata: Kakuma West, Turkana\n"
            f"Unaweza kupiga simu bure wakati wa dharura."
        )

    # 5. English Language Mode
    elif inputs[0] == "5":
        if level == 1:
            return (
                f"CON AFYAEARLY CARE (ENGLISH):\n"
                f"Child: {child_data['child_name']} ({child_data['age_months']} mos)\n"
                f"Status: {child_data['stunting_status_en']}\n"
                f"MUAC: {child_data['muac']} cm ({child_data['risk_tier']})\n"
                f"1. View Growth Consequence Breakdown\n"
                f"2. Trigger Emergency CHW SMS Alert\n"
                f"3. Next Clinic Appointment\n"
                f"0. Back / Rudi"
            )
        elif level == 2 and inputs[1] == "1":
            return (
                f"END GROWTH CONSEQUENCE:\n"
                f"Height deficit: {abs(child_data['haz'])} SD below WHO reference.\n"
                f"Clinical Risk: Delayed synaptic brain development & motor milestones.\n"
                f"Immediate Action: High-energy therapeutic feeding (RUTF) & protein supplementation."
            )
        elif level == 2 and inputs[1] == "2":
            return (
                f"END EMERGENCY ALERT DISPATCHED!\n"
                f"SMS alert sent to Community Health Worker {child_data['assigned_chw']} ({child_data['chw_phone']}).\n"
                f"Please take {child_data['child_name']} immediately to {child_data['facility']}."
            )
        elif level == 2 and inputs[1] == "3":
            return (
                f"END NEXT CLINIC VISIT:\n"
                f"Date: {child_data['next_clinic_date']}\n"
                f"Facility: {child_data['facility']}\n"
                f"CHW: {child_data['assigned_chw']} ({child_data['chw_phone']})"
            )

    # Fallback
    return "END Chaguo si sahihi. Tafadhali piga tena *384*46#."
