package com.strath.finalpr0ject.ai

import com.strath.finalpr0ject.data.model.ChildProfile
import com.strath.finalpr0ject.data.model.UserRole

data class BotResponse(
    val replyText: String,
    val requiresReferral: Boolean = false,
    val referralChwName: String = "",
    val referralChwPhone: String = "",
    val referralFacility: String = "",
    val detectedEmergency: String = ""
)

object AfyaBotEngine {

    fun generateReply(
        query: String,
        role: UserRole,
        language: String, // "en" or "sw"
        child: ChildProfile?
    ): BotResponse {
        val q = query.trim().lowercase()
        val isSwahili = language.equals("sw", ignoreCase = true)
        val isChw = (role == UserRole.CHW || role == UserRole.ADMIN)

        // -------------------------------------------------------------
        // 1. EMERGENCY & DANGER SIGNS DETECTION (CLINICAL ESCALATION)
        // -------------------------------------------------------------
        val emergencyKeywordsEn = listOf(
            "fever", "high fever", "hot", "vomit", "vomiting", "diarrhea", "diarrhoea",
            "edema", "swollen", "swelling", "unconscious", "lethargic", "not drinking",
            "refusing milk", "refuses to eat", "convulsion", "seizure", "difficulty breathing",
            "gasping", "red muac", "pale"
        )
        val emergencyKeywordsSw = listOf(
            "homa", "homa kali", "joto kali", "kutapika", "anatapika", "kuhara", "anahara",
            "kuvimba", "miguu kuvimba", "uso kuvimba", "amelegea", "hawezi kunyonya",
            "kukataa kula", "kukataa maziwa", "degedege", "kupumua kwa shida", "hapumui vizuri",
            "udhaifu", "anazimia"
        )

        val isEmergency = if (isSwahili) {
            emergencyKeywordsSw.any { q.contains(it) }
        } else {
            emergencyKeywordsEn.any { q.contains(it) }
        }

        if (isEmergency) {
            val chwName = child?.assignedChwName?.ifEmpty { "Maria Akiru" } ?: "Maria Akiru"
            val chwPhone = "+254 712 345 678"
            val facility = "Kakuma West Sub-County Hospital"

            val reply = if (isSwahili) {
                "🚨 HATUA YA DHARURA INAHITAJIKA MARA MOJA!\n\n" +
                "Dalili ulizotaja (homa, kuhara, kukataa kula, au kuvimba) ni dalili za hatari kubwa kiafya kwa mtoto chini ya miaka mitano.\n\n" +
                "1. Tafadhali usimpatie dawa za watu wazima.\n" +
                "2. Mpatie maji ya ORS au maji safi ya kuchemsha kijiko kimoja kimoja.\n" +
                "3. Wasiliana mara moja na Afisa wa Afya wa Eneo Lako ($chwName) au nenda $facility.\n\n" +
                "Nimekuwekea kitufe hapa chini kupiga simu au kutuma SMS ya dharura."
            } else {
                "🚨 IMMEDIATE MEDICAL ATTENTION REQUIRED!\n\n" +
                "The signs you reported (fever, diarrhea, refusal to feed, or swelling/edema) are clinical danger signs under WHO pediatric triage guidelines.\n\n" +
                "1. Do not give adult medications or aspirin.\n" +
                "2. Offer sips of Oral Rehydration Salts (ORS) or breastmilk continuously.\n" +
                "3. Contact your assigned Community Health Worker ($chwName) immediately or visit $facility.\n\n" +
                "Use the emergency call/SMS button below to connect with your health worker right away."
            }

            return BotResponse(
                replyText = reply,
                requiresReferral = true,
                referralChwName = chwName,
                referralChwPhone = chwPhone,
                referralFacility = facility,
                detectedEmergency = if (isSwahili) "Dalili za hatari za kiafya" else "Acute pediatric danger signs"
            )
        }

        // -------------------------------------------------------------
        // 2. LIVE CHILD DASHBOARD METRICS TRACKING
        // -------------------------------------------------------------
        val trackingKeywords = listOf(
            "my baby", "my child", "akiru", "growth", "dashboard", "how is he", "how is she",
            "muac", "haz", "status", "anaendeleaje", "hali ya mtoto", "kipimo", "ukuaji",
            "maendeleo", "urefu", "uzito"
        )
        if (trackingKeywords.any { q.contains(it) } && child != null) {
            val childName = child.name.ifEmpty { "Akiru Ewoi" }
            val age = child.ageMonths
            val haz = child.currentHaz
            val muac = child.currentMuac
            val tier = child.latestRiskTier
            val motor = child.motorMilestoneScore

            return if (isSwahili) {
                val statusSw = if (haz < -3.0f) "Kudumaa Kukubwa (Severe)" else if (haz < -2.0f) "Kudumaa kwa Wastani (Moderate)" else "Urefu wa Kawaida (Normal)"
                val muacAdvice = if (muac < 11.5f) "Mzunguko wa mkono (MUAC $muac cm) upo katika kiwango cha hatari (Nyekundu)." else "Mzunguko wa mkono (MUAC $muac cm) upo katika kiwango cha wastani."
                BotResponse(
                    replyText = "📊 TATHMINI YA DASHIBODI YA MTOTO $childName (Miezi $age):\n\n" +
                            "• Hali ya Urefu (HAZ): $haz ($statusSw)\n" +
                            "• Kipimo cha Mkono (MUAC): $muac cm ($tier)\n" +
                            "• Alama ya Mwendo & Misuli: $motor/100 (ASQ-3)\n" +
                            "• Dirisha la Siku 1,000: Mtoto ana miezi $age. Dirisha muhimu la kurekebisha urefu linakamilika anapofikisha miezi 24.\n\n" +
                            "$muacAdvice\n\n" +
                            "💡 Ushauri: Endelea kumpatia milo minne yenye mchanganyiko wa vyakula (DDI) na pakiti ya Plumpy'Nut kama alivyoelekezwa na CHW ${child.assignedChwName}."
                )
            } else {
                val statusEn = if (haz < -3.0f) "Severe Chronic Stunting (HAZ < -3)" else if (haz < -2.0f) "Moderate Stunting (HAZ < -2)" else "Normal Stature Range"
                val muacAdvice = if (muac < 11.5f) "MUAC ($muac cm) is in the RED zone, indicating Severe Acute Malnutrition." else "MUAC ($muac cm) is within the moderate/yellow monitoring band."
                BotResponse(
                    replyText = "📊 DASHBOARD GROWTH TRACKER FOR $childName ($age months):\n\n" +
                            "• Height-for-Age (HAZ): $haz ($statusEn)\n" +
                            "• Upper Arm MUAC: $muac cm ($tier)\n" +
                            "• Gross Motor Milestone: $motor% age-calibrated progress\n" +
                            "• First 1,000 Days Window: ${24 - age} months remaining before epigenetic catch-up closure.\n\n" +
                            "$muacAdvice\n\n" +
                            "💡 Recommended Action: Prioritize energy-dense complementary feeding (DDI target >= 4 food groups) and attend the upcoming screening with CHW ${child.assignedChwName}."
                )
            }
        }

        // -------------------------------------------------------------
        // 3. ROLE-BASED INTELLIGENCE: CHW CLINICAL PROTOCOLS
        // -------------------------------------------------------------
        if (isChw) {
            if (q.contains("model") || q.contains("ml") || q.contains("onnx") || q.contains("algorithm") || q.contains("technical") || q.contains("ai")) {
                return if (isSwahili) {
                    BotResponse(
                        replyText = "🔬 MAELEZO YA KITEKNOLOJIA (ONNX Edge ML):\n\n" +
                                "• Muundo: LightGBM Gradient Boosted Decision Ensemble iliyogeuzwa kuwa muundo wa ONNX Mobile.\n" +
                                "• Vipengele (Features): Umri (miezi), Jinsia, Urefu (cm), Uzito (kg), MUAC (cm), Elimu ya Mama, Chanzo cha Maji, Ukubwa wa Kaya.\n" +
                                "• Usahihi: 92.4% ROC-AUC iliyojaribiwa kwenye data za KDHS na ASAL.\n" +
                                "• Uwezo wa Nje ya Mtandao: Muundo unafanya kazi 100% ndani ya simu bila mtandao wa data (Edge Inference chini ya 12ms)."
                    )
                } else {
                    BotResponse(
                        replyText = "🔬 TECHNICAL ARCHITECTURE (ONNX Edge ML Engine):\n\n" +
                                "• Model Pipeline: LightGBM Gradient Boosted Ensemble quantized for edge runtime via ONNX Mobile (12.4 MB).\n" +
                                "• Core Predictors: Child Age, Sex, HAZ, WHZ, MUAC, Maternal Education, Water Source Infrastructure, Household Size.\n" +
                                "• Diagnostic Performance: 92.4% ROC-AUC calibrated against KDHS ASAL cohort benchmarks.\n" +
                                "• Zero-Latency Edge Execution: 100% offline inference (<12ms latency on low-end Arm64 SoC) preserving field battery life."
                    )
                }
            }

            if (q.contains("rutf") || q.contains("plumpy") || q.contains("dose") || q.contains("dosage") || q.contains("sachet")) {
                return if (isSwahili) {
                    BotResponse(
                        replyText = "📋 PROTOKALI YA KUTOA RUTF / PLUMPY'NUT (CHW):\n\n" +
                                "• Vigezo vya Kutoa: Watoto wenye MUAC < 11.5 cm au Kudumaa Kukubwa (HAZ < -3) bila matatizo mengine ya kiafya.\n" +
                                "• Dozi kwa Uzito:\n" +
                                "  - Kilo 5.0 - 6.9: Pakiti 2 kwa siku (14 kwa wiki)\n" +
                                "  - Kilo 7.0 - 9.9: Pakiti 3 kwa siku (21 kwa wiki)\n" +
                                "  - Kilo 10.0+: Pakiti 4 kwa siku (28 kwa wiki)\n" +
                                "• Ufuatiliaji: Pima mtoto kila baada ya siku 14 na rekodi kwenye mfumo wa MOH-711."
                    )
                } else {
                    BotResponse(
                        replyText = "📋 CLINICAL RUTF DISPENSING PROTOCOL (CHW Guidelines):\n\n" +
                                "• Admission Criteria: MUAC < 115mm or bilateral edema (+) without medical complications.\n" +
                                "• Caloric Dosing Matrix (200 kcal/kg/day):\n" +
                                "  - 5.0 - 6.9 kg: 2 sachets/day (14/week)\n" +
                                "  - 7.0 - 9.9 kg: 3 sachets/day (21/week)\n" +
                                "  - 10.0+ kg: 4 sachets/day (28/week)\n" +
                                "• Follow-up: Schedule mandatory 14-day home visit and enter progress in MOH-711 registry."
                    )
                }
            }

            if (q.contains("z-score") || q.contains("haz") || q.contains("whz") || q.contains("who") || q.contains("anthro")) {
                return if (isSwahili) {
                    BotResponse(
                        replyText = "📏 VIWANGO VYA ANTHROPOMETRY VYA WHO:\n\n" +
                                "• HAZ (Height-for-Age / Urefu kwa Umri):\n" +
                                "  - > -2 SD: Ukuaji wa Kawaida\n" +
                                "  - -2 hadi -3 SD: Kudumaa kwa Wastani (MAM Stunting)\n" +
                                "  - < -3 SD: Kudumaa Kukubwa (Severe Stunting)\n" +
                                "• MUAC:\n" +
                                "  - Kijani (> 12.5 cm): Kawaida\n" +
                                "  - Njano (11.5 - 12.4 cm): Hatari ya Wastani (RUSF)\n" +
                                "  - Nyekundu (< 11.5 cm): Utapiamlo Mkali (RUTF OTP Admission)"
                    )
                } else {
                    BotResponse(
                        replyText = "📏 WHO ANTHROPOMETRIC CLASSIFICATION MATRIX:\n\n" +
                                "• Height-for-Age Z-score (HAZ):\n" +
                                "  - > -2 SD: Normal Growth Range\n" +
                                "  - -2.0 to -2.99 SD: Moderate Stunting (MAM)\n" +
                                "  - < -3.0 SD: Severe Stunting (SAM)\n" +
                                "• Mid-Upper Arm Circumference (MUAC):\n" +
                                "  - Green (>= 12.5 cm): Well-nourished\n" +
                                "  - Yellow (11.5 - 12.4 cm): Moderate Acute Malnutrition (MAM - RUSF)\n" +
                                "  - Red (< 11.5 cm): Severe Acute Malnutrition (SAM - RUTF OTP)"
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 4. ROLE-BASED INTELLIGENCE: MOTHER / PARENT GUIDANCE
        // -------------------------------------------------------------
        if (q.contains("food") || q.contains("meal") || q.contains("feed") || q.contains("diet") || q.contains("chakula") || q.contains("lishe") || q.contains("uji") || q.contains("maziwa")) {
            return if (isSwahili) {
                BotResponse(
                    replyText = "🍲 MWONGOZO WA CHAKULA NA LISHE BORA YA MTOTO:\n\n" +
                            "Ili kuzuia na kurekebisha kudumaa, mpatie mtoto makundi haya 4 kila siku:\n" +
                            "1. Nafaka & Mizizi: Uji mzito wa mtama, mahindi au viazi vitamu.\n" +
                            "2. Protini: Maziwa safi ya ngamia au mbuzi, mayai, au maharagwe yaliyopondwa vizuri.\n" +
                            "3. Mboga za Vitamini A: Mchicha, karoti au maboga.\n" +
                            "4. Matunda: Embe, papai au ndizi mbivu.\n\n" +
                            "🍼 Kunyonyesha: Endelea kunyonyesha mtoto hadi anapofikisha miaka 2 au zaidi."
                )
            } else {
                BotResponse(
                    replyText = "🍲 DAILY NUTRITIONAL CATCH-UP GUIDE (Dietary Diversity Index):\n\n" +
                            "To reverse growth faltering, provide foods from at least 4 key groups daily:\n" +
                            "1. Grains & Roots: Thick sorghum/millet porridge, sweet potatoes, or fortified maize.\n" +
                            "2. High-Quality Protein: Fresh camel/goat milk, well-cooked eggs, or mashed legumes.\n" +
                            "3. Vitamin-A Rich Vegetables: Spinach, pumpkin, or orange carrots.\n" +
                            "4. Local Fruits: Ripe papaya, bananas, or mangoes.\n\n" +
                            "🍼 Breastfeeding: Continue on-demand breastfeeding alongside solids until at least 24 months."
                )
            }
        }

        if (q.contains("stunting") || q.contains("kudumaa") || q.contains("1000 days") || q.contains("siku 1000") || q.contains("urefu")) {
            return if (isSwahili) {
                BotResponse(
                    replyText = "🌱 KUDUMAA NI NINI NA JE KUNAWEZA KUREKEBISHWA?\n\n" +
                            "• Kudumaa kunatokea pale mtoto anapokosa virutubisho vya kutosha wakati wa ukuaji wake, hasa katika Siku 1,000 za Kwanza (tangu mimba hadi miaka 2).\n" +
                            "• Athari zake: Kuchelewa kwa maendeleo ya ubongo, udhaifu wa kinga ya mwili, na kupoteza urefu wa kudumu.\n" +
                            "• Je, kunaweza kurekebishwa? NDIO! Kabla mtoto hajatimiza miaka 2, mwili na ubongo wake una uwezo mkubwa wa kurejea (catch-up growth) ukimpatia lishe bora na matunzo mazuri."
                )
            } else {
                BotResponse(
                    replyText = "🌱 WHAT IS STUNTING & CAN IT BE REVERSED?\n\n" +
                            "• Stunting (low height-for-age) reflects chronic undernutrition, frequent infections, and inadequate psychosocial stimulation during the critical First 1,000 Days.\n" +
                            "• Long-Term Impact: Irreversible loss of neural synapses, delayed motor/language milestones, and reduced adult economic productivity.\n" +
                            "• Reversibility: YES! Within the first 24 months, neuroplasticity and skeletal epiphyseal plates remain highly responsive to high-energy dietary catch-up."
                )
            }
        }

        // Default General Greeting & Guidance
        return if (isSwahili) {
            BotResponse(
                replyText = "Habari! Mimi ni AfyaBot, msaidizi wako wa afya ya mtoto na lishe.\n\n" +
                        "Unaweza kuniuliza kuhusu:\n" +
                        "1. Maendeleo ya mtoto wako kwenye dashibodi ('Mtoto wangu anaendeleaje?')\n" +
                        "2. Ushauri wa chakula na lishe ('Nimpatie chakula kipi leo?')\n" +
                        "3. Matumizi ya Plumpy'Nut (RUTF)\n" +
                        "4. Dalili za hatari za kiafya\n\n" +
                        "Una swali gani kuhusu mtoto wako leo?"
            )
        } else {
            BotResponse(
                replyText = "Hello! I am AfyaBot, your intelligent pediatric health and nutrition companion.\n\n" +
                        "You can ask me about:\n" +
                        "1. Live dashboard tracking ('How is my baby Akiru growing?')\n" +
                        "2. Daily meal plans and dietary diversity (DDI)\n" +
                        "3. RUTF / Plumpy'Nut dosage and administration\n" +
                        "4. Clinical questions & danger signs\n\n" +
                        "How can I help you support your child's growth today?"
            )
        }
    }
}
