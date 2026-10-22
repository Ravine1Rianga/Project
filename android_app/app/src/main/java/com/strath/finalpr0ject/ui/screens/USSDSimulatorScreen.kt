package com.strath.finalpr0ject.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strath.finalpr0ject.ui.theme.CalmGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun USSDSimulatorScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Dialing pad state
    var dialCode by remember { mutableStateOf("*384*46#") }
    var selectedPhoneNumber by remember { mutableStateOf("+254 712 345 678") }

    // Active session modal state
    var isSessionActive by remember { mutableStateOf(false) }
    var sessionScreenText by remember { mutableStateOf("") }
    var sessionInput by remember { mutableStateOf("") }
    var sessionIsEnd by remember { mutableStateOf(false) }
    var accumulatedSessionText by remember { mutableStateOf("") }

    // Pre-configured simulation logic for offline demonstration
    fun getLocalUSSDResponse(textAccum: String): Pair<String, Boolean> {
        val inputs = textAccum.split("*").filter { it.isNotEmpty() }
        val level = inputs.size

        if (level == 0) {
            return Pair(
                "Karibu AfyaEarly Care - Huduma ya Mtoto\n" +
                "Mtoto: Akiru Ewoi (Miezi 18)\n\n" +
                "1. Hali ya Mtoto & Tahadhari\n" +
                "2. Tuma Tahadhari ya Dharura\n" +
                "3. Ushauri wa Lishe & RUTF\n" +
                "4. Wasiliana na CHW Maria Akiru\n" +
                "5. English Language",
                false
            )
        }

        return when (inputs[0]) {
            "1" -> {
                if (level == 1) {
                    Pair(
                        "TAHADHARI YA MTOTO: AKIRU EWOI\n\n" +
                        "• Urefu/Umri (HAZ): -2.4 (Kudumaa kwa Wastani)\n" +
                        "• Kipimo cha Mkono (MUAC): 12.5 cm (Tahadhari ya Wastani)\n" +
                        "• Hatari: Watoto wenye kipimo hiki wanahitaji chakula chenye virutubisho mara 3 kwa siku.\n\n" +
                        "1. Matokeo ya Kiafya (Consequences)\n" +
                        "2. Tarehe ya Kliniki Inayofuata\n" +
                        "0. Rudi Mwanzo",
                        false
                    )
                } else if (inputs.getOrNull(1) == "1") {
                    Pair(
                        "MATOKEO YA KIAFYA:\n\n" +
                        "Mtoto ana upungufu wa urefu wa 2.4 SD chini ya kiwango cha WHO.\n" +
                        "Hatari: Kudumaa kwa ukuaji wa ubongo, kuchelewa kutembea, na mwili kuwa dhaifu.\n\n" +
                        "Hatua: Mpatie chakula cha ziada cha Plumpy'Nut na maziwa ya kutosha.",
                        true
                    )
                } else if (inputs.getOrNull(1) == "2") {
                    Pair(
                        "TAREHE YA KLINIKI:\n\n" +
                        "Tarehe inayofuata: 14 Nov 2026\n" +
                        "Kituo: Kakuma West Sub-County Hospital\n" +
                        "Afisa: Maria Akiru (+254 712 345 678)\n\n" +
                        "Kumbuka kubeba kitabu cha afya cha mtoto.",
                        true
                    )
                } else {
                    getLocalUSSDResponse("")
                }
            }
            "2" -> {
                if (level == 1) {
                    Pair(
                        "RIPOTI DALILI ZA HATARI:\n" +
                        "Mtoto ana dalili gani sasa hivi?\n\n" +
                        "1. Homa kali na kukataa kula\n" +
                        "2. Kuhara sana au kutapika mfululizo\n" +
                        "3. Kuvimba miguu na uso (Edema)\n" +
                        "4. Udhaifu mkubwa wa mwili",
                        false
                    )
                } else {
                    val symptomMap = mapOf(
                        "1" to "Homa kali na kukataa kula",
                        "2" to "Kuhara sana na kutapika",
                        "3" to "Kuvimba miguu na mikono (Edema)",
                        "4" to "Kupumua kwa shida na udhaifu mkubwa"
                    )
                    val symptom = symptomMap[inputs.getOrNull(1)] ?: "Dalili za hatari"
                    Pair(
                        "TAHADHARI YA DHARURA IMETUMWA!\n\n" +
                        "Afisa wa Afya Maria Akiru (+254 712 345 678) ametumiwa ujumbe wa SMS kuwa mtoto Akiru ana '$symptom'.\n\n" +
                        "Tafadhali mpeleke mtoto mara moja kituo cha afya: Kakuma West Hospital.",
                        true
                    )
                }
            }
            "3" -> {
                if (level == 1) {
                    Pair(
                        "MWONGOZO WA LISHE & RUTF:\n\n" +
                        "1. Jinsi ya Kumpa Plumpy'Nut (RUTF)\n" +
                        "2. Vyakula 4 Muhimu Kila Siku (DDI)\n" +
                        "3. Ratiba ya Maziwa ya Mama\n" +
                        "0. Rudi Mwanzo",
                        false
                    )
                } else if (inputs.getOrNull(1) == "1") {
                    Pair(
                        "MATUMIZI YA RUTF (Plumpy'Nut):\n\n" +
                        "• Mpe mtoto sachet 1 mara 2 kwa siku.\n" +
                        "• Mpe maji safi ya kuchemsha baada ya kula.\n" +
                        "• Usichanganye RUTF na chakula kingine.\n" +
                        "• Usitupe mabaki; funika vizuri mahali pakavu.",
                        true
                    )
                } else {
                    Pair(
                        "VYAKULA 4 MUHIMU (DDI):\n\n" +
                        "1. Nafaka (Uji wa mtama au mahindi)\n" +
                        "2. Protini (Maziwa safi, mayai au maharagwe)\n" +
                        "3. Mboga za majani zenye Vit-A\n" +
                        "4. Matunda (Ndizi au embe)",
                        true
                    )
                }
            }
            "4" -> {
                Pair(
                    "KITUO CHA AFYA & AFISA WA ENEO:\n\n" +
                    "• Afisa: Maria Akiru\n" +
                    "• Simu: +254 712 345 678\n" +
                    "• Kituo: Kakuma West Sub-County Hospital\n" +
                    "• Kata: Kakuma West, Turkana\n\n" +
                    "Huduma ni bure kwa watoto chini ya miaka 5.",
                    true
                )
            }
            "5" -> {
                Pair(
                    "AFYAEARLY CARE (ENGLISH):\n" +
                    "Child: Akiru Ewoi (18 mos)\n" +
                    "Status: Moderate Stunting (-2.4 HAZ)\n" +
                    "MUAC: 12.5 cm (Moderate Risk)\n\n" +
                    "Alert: Synaptic brain growth requires urgent dietary diversity.\n" +
                    "Next Visit: 14 Nov 2026 at Kakuma West Hospital.",
                    true
                )
            }
            else -> Pair("Chaguo si sahihi. Tafadhali piga tena *384*46#.", true)
        }
    }

    fun startUSSDSession() {
        accumulatedSessionText = ""
        sessionInput = ""
        val (resp, isEnd) = getLocalUSSDResponse("")
        sessionScreenText = resp
        sessionIsEnd = isEnd
        isSessionActive = true
    }

    fun sendUSSDInput() {
        val nextInput = sessionInput.trim()
        if (nextInput.isEmpty()) return

        val newAccum = if (accumulatedSessionText.isEmpty()) nextInput else "$accumulatedSessionText*$nextInput"
        accumulatedSessionText = newAccum
        sessionInput = ""

        val (resp, isEnd) = getLocalUSSDResponse(newAccum)
        sessionScreenText = resp
        sessionIsEnd = isEnd
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("2G USSD Gateway Simulator", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Offline Feature Phone Alerts (*384*46#)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Explanation Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CalmGreenPrimary.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, CalmGreenPrimary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CalmGreenPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = CalmGreenPrimary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ASAL Offline 2G USSD Service", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CalmGreenPrimary)
                        Text(
                            "Allows parents on basic non-smartphone feature phones (Safaricom/Airtel) to receive growth alerts, check MUAC status, and report emergencies.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Phone Number Selector
            OutlinedTextField(
                value = selectedPhoneNumber,
                onValueChange = { selectedPhoneNumber = it },
                label = { Text("Registered Parent Phone Number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = CalmGreenPrimary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // USSD Shortcode Dial Input
            OutlinedTextField(
                value = dialCode,
                onValueChange = { dialCode = it },
                label = { Text("USSD Service Code") },
                leadingIcon = { Icon(Icons.Default.Dialpad, contentDescription = null, tint = CalmGreenPrimary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { startUSSDSession() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CalmGreenPrimary)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulate Dial", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val encodedHash = Uri.encode("#")
                        val codeUri = "tel:*384*46$encodedHash"
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse(codeUri))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot launch native dialer", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhoneForwarded, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Device Dialer", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Quick USSD Features Card
            Text(
                text = "Key USSD Capabilities for ASAL Counties",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureItem(
                        icon = Icons.Default.NotificationsActive,
                        title = "1. Active Baby Health Alerts",
                        desc = "Instantly reads child HAZ stunting status and MUAC measurement directly from cloud/offline registry."
                    )
                    FeatureItem(
                        icon = Icons.Default.Warning,
                        title = "2. 1-Tap Emergency SMS Dispatch",
                        desc = "When severe fever or edema is reported, sends automated SMS alerts to assigned CHW Maria Akiru."
                    )
                    FeatureItem(
                        icon = Icons.Default.Restaurant,
                        title = "3. RUTF & Nutrition Guidance",
                        desc = "Clear instructions on Plumpy'Nut dosage and the 4 critical food groups in local Swahili."
                    )
                    FeatureItem(
                        icon = Icons.Default.Translate,
                        title = "4. Kiswahili & English Toggle",
                        desc = "Fully localized for rural mothers and community elders."
                    )
                }
            }
        }
    }

    // ================================================================
    // REALISTIC USSD MODAL DIALOG (Feature Phone UI)
    // ================================================================
    if (isSessionActive) {
        AlertDialog(
            onDismissRequest = { isSessionActive = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (sessionIsEnd) Color.Gray else Color(0xFF4CAF50))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (sessionIsEnd) "USSD Session Ended" else "USSD: *384*46#",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B) // Dark telecom LCD display
                    ) {
                        Text(
                            text = sessionScreenText,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    if (!sessionIsEnd) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = sessionInput,
                            onValueChange = { sessionInput = it },
                            placeholder = { Text("Enter number (e.g. 1)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            },
            confirmButton = {
                if (!sessionIsEnd) {
                    Button(
                        onClick = { sendUSSDInput() },
                        colors = ButtonDefaults.buttonColors(containerColor = CalmGreenPrimary)
                    ) {
                        Text("Send", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { isSessionActive = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CalmGreenPrimary)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!sessionIsEnd) {
                    TextButton(onClick = { isSessionActive = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = CalmGreenPrimary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
        }
    }
}
