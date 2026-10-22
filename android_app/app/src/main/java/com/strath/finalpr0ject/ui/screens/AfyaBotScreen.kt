package com.strath.finalpr0ject.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strath.finalpr0ject.ai.AfyaBotEngine
import com.strath.finalpr0ject.ai.BotResponse
import com.strath.finalpr0ject.data.model.ChildProfile
import com.strath.finalpr0ject.data.model.UserRole
import com.strath.finalpr0ject.data.repository.FirebaseRepository
import com.strath.finalpr0ject.ui.theme.CalmGreenPrimary
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val referralData: BotResponse? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AfyaBotScreen(
    childId: String = "TK-1042",
    roleKey: String = "parent",
    repository: FirebaseRepository = remember { FirebaseRepository() },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var language by remember { mutableStateOf("sw") } // "en" or "sw"
    val role = if (roleKey.equals("chw", ignoreCase = true)) UserRole.CHW else UserRole.PARENT

    var childProfile by remember { mutableStateOf<ChildProfile?>(null) }
    var inputText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }

    // Fetch child from repository/Room
    LaunchedEffect(childId) {
        val result = repository.getChildProfile(childId)
        result.onSuccess { child ->
            childProfile = child ?: ChildProfile(
                childId = childId,
                name = "Akiru Ewoi",
                ageMonths = 18,
                currentHaz = -2.4f,
                currentWhz = -1.6f,
                currentMuac = 12.5f,
                latestRiskTier = "MODERATE RISK",
                assignedChwName = "Maria Akiru"
            )
        }
    }

    // Set initial welcome greeting when language changes or first loaded
    LaunchedEffect(language, childProfile) {
        if (messages.isEmpty() || messages.size == 1) {
            val initialResponse = AfyaBotEngine.generateReply(
                query = "hello",
                role = role,
                language = language,
                child = childProfile
            )
            messages = listOf(
                ChatMessage(
                    isUser = false,
                    text = initialResponse.replyText
                )
            )
        }
    }

    fun sendMessage(queryText: String) {
        val trimmed = queryText.trim()
        if (trimmed.isEmpty()) return

        val userMsg = ChatMessage(isUser = true, text = trimmed)
        val botResp = AfyaBotEngine.generateReply(
            query = trimmed,
            role = role,
            language = language,
            child = childProfile
        )
        val botMsg = ChatMessage(
            isUser = false,
            text = botResp.replyText,
            referralData = if (botResp.requiresReferral) botResp else null
        )

        messages = messages + userMsg + botMsg
        inputText = ""

        scope.launch {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CalmGreenPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = CalmGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AfyaBot Assistant",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (role == UserRole.CHW) "CHW Clinical Mode" else "Parent Health Guide",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Bilingual language switch toggle
                    FilterChip(
                        selected = language == "sw",
                        onClick = {
                            language = if (language == "sw") "en" else "sw"
                            Toast.makeText(
                                context,
                                if (language == "sw") "Lugha imebadilishwa kuwa Kiswahili" else "Switched to English",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        label = {
                            Text(
                                text = if (language == "sw") "🇰🇪 Swahili" else "🇬🇧 English",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CalmGreenPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = CalmGreenPrimary
                        ),
                        border = BorderStroke(1.dp, CalmGreenPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Chat Input Bar
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Quick suggestion chips
                    val suggestions = if (language == "sw") {
                        listOf(
                            "Mtoto wangu anaendeleaje?",
                            "Nimpatie chakula kipi leo?",
                            "Dalili za hatari",
                            "Matumizi ya Plumpy'Nut"
                        )
                    } else {
                        listOf(
                            "How is Akiru growing?",
                            "What should I feed him today?",
                            "Danger signs to watch",
                            "How does the ML model work?"
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestions.take(2).forEach { chipText ->
                            SuggestionChip(
                                onClick = { sendMessage(chipText) },
                                label = { Text(chipText, fontSize = 11.sp, maxLines = 1) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    if (language == "sw") "Uliza swali lolote la afya au lishe..." else "Ask any health or nutrition question...",
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 3
                        )

                        IconButton(
                            onClick = { sendMessage(inputText) },
                            modifier = Modifier
                                .size(44.dp)
                                .background(CalmGreenPrimary, shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Active Child Dashboard Live Tracker Header Card
            childProfile?.let { ch ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = CalmGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tracking Dashboard: ${ch.name} (${ch.ageMonths}m) • HAZ: ${ch.currentHaz} • MUAC: ${ch.currentMuac}cm",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Chat Messages Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg = msg, onEmergencyAction = { phone, action ->
                        if (action == "call") {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        } else {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
                                putExtra("sms_body", "EMERGENCY: Child ${childProfile?.name ?: "Akiru"} requires urgent triage.")
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    msg: ChatMessage,
    onEmergencyAction: (phone: String, action: String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (msg.isUser) 16.dp else 2.dp,
                bottomEnd = if (msg.isUser) 2.dp else 16.dp
            ),
            color = if (msg.isUser) CalmGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = msg.text,
                    color = if (msg.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp
                )

                // Render Emergency Health Worker Referral Card if danger signs triggered
                msg.referralData?.let { ref ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E8)),
                        border = BorderStroke(1.dp, Color(0xFFE02424)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MedicalServices,
                                    contentDescription = null,
                                    tint = Color(0xFFE02424),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Direct Health Worker Escalation",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF9B1C1C)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Assigned CHW: ${ref.referralChwName}\nFacility: ${ref.referralFacility}",
                                fontSize = 11.sp,
                                color = Color(0xFF771D1D)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { onEmergencyAction(ref.referralChwPhone, "call") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE02424)),
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call CHW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onEmergencyAction(ref.referralChwPhone, "sms") },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Send SMS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
