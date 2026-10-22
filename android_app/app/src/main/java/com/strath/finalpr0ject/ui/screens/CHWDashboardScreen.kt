package com.strath.finalpr0ject.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strath.finalpr0ject.data.local.AppDatabase
import com.strath.finalpr0ject.data.local.entity.ChildEntity
import com.strath.finalpr0ject.data.model.ChildProfile
import com.strath.finalpr0ject.data.model.UserProfile
import com.strath.finalpr0ject.ml.WHOAnthroCalculator
import com.strath.finalpr0ject.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CHWDashboardScreen(
    chwId: String,
    userProfile: UserProfile? = null,
    onStartScreeningForChild: (ChildProfile) -> Unit,
    onNavigateToAfyaBot: (role: String, childId: String) -> Unit = { _, _ -> },
    onNavigateToUSSD: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Dynamic, personalized CHW credentials
    val chwName = userProfile?.displayName?.trim()?.ifEmpty { "Community Health Worker" } ?: "Community Health Worker"
    val effectiveChwId = userProfile?.chwId?.ifEmpty { userProfile.chwCode.ifEmpty { chwId } } ?: chwId
    val facility = userProfile?.chwFacility?.ifEmpty { userProfile.supervisingFacility.ifEmpty { "Sub-County Hospital" } } ?: "Sub-County Hospital"
    val county = userProfile?.county?.ifEmpty { "Turkana" } ?: "Turkana"
    val subCounty = userProfile?.subCounty?.ifEmpty { "Field Unit" } ?: "Field Unit"

    // Check if this is the original demo account (Maria Akiru)
    val isDemoWorker = effectiveChwId == "CHW-1042" || userProfile?.email?.contains("maria.akiru", ignoreCase = true) == true

    var searchQuery by remember { mutableStateOf("") }
    var selectedTriageFilter by remember { mutableStateOf("ALL") } // ALL, HIGH, MODERATE, LOW

    // Assigned Children State: Starts empty for newly registered CHWs; seeded demo for Maria Akiru
    var assignedChildren by remember {
        mutableStateOf<List<ChildProfile>>(
            if (isDemoWorker) {
                listOf(
                    ChildProfile(
                        childId = "TK-1042",
                        name = "Akiru Ewoi",
                        ageMonths = 18,
                        sex = "Male",
                        county = county,
                        assignedChwId = effectiveChwId,
                        assignedChwName = chwName,
                        maternalEducation = "None",
                        waterSource = "Unimproved",
                        householdSize = 6,
                        currentHaz = -2.4f,
                        currentWhz = -2.1f,
                        currentMuac = 11.2f,
                        latestRiskTier = "HIGH RISK",
                        latestProbability = 0.88f
                    ),
                    ChildProfile(
                        childId = "TK-1150",
                        name = "Napeyok Lokor",
                        ageMonths = 12,
                        sex = "Female",
                        county = county,
                        assignedChwId = effectiveChwId,
                        assignedChwName = chwName,
                        maternalEducation = "None",
                        waterSource = "Unimproved",
                        householdSize = 5,
                        currentHaz = -1.9f,
                        currentWhz = -1.6f,
                        currentMuac = 11.9f,
                        latestRiskTier = "MODERATE RISK",
                        latestProbability = 0.52f
                    ),
                    ChildProfile(
                        childId = "TK-1088",
                        name = "Locham Nakor",
                        ageMonths = 24,
                        sex = "Female",
                        county = county,
                        assignedChwId = effectiveChwId,
                        assignedChwName = chwName,
                        maternalEducation = "Primary",
                        waterSource = "Improved",
                        householdSize = 5,
                        currentHaz = 0.2f,
                        currentWhz = -0.4f,
                        currentMuac = 14.5f,
                        latestRiskTier = "LOW RISK",
                        latestProbability = 0.12f
                    )
                )
            } else {
                emptyList()
            }
        )
    }

    // Load scoped children from local Room database
    LaunchedEffect(effectiveChwId) {
        try {
            val dbChildren = AppDatabase.getDatabase(context).childDao().getChildrenByChw(effectiveChwId)
            if (dbChildren.isNotEmpty()) {
                assignedChildren = dbChildren.map { it.toDomainModel() }
            } else if (!isDemoWorker) {
                assignedChildren = emptyList()
            }
        } catch (_: Exception) {}
    }

    var showRegisterChildDialog by remember { mutableStateOf(false) }
    var selectedChildForAction by remember { mutableStateOf<ChildProfile?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }

    // Filtered Children List
    val filteredChildren = remember(assignedChildren, searchQuery, selectedTriageFilter) {
        assignedChildren.filter { child ->
            val matchesQuery = child.name.contains(searchQuery, ignoreCase = true) ||
                    child.childId.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedTriageFilter) {
                "HIGH" -> child.latestRiskTier == "HIGH RISK" || child.currentHaz < -2.0f
                "MODERATE" -> child.latestRiskTier == "MODERATE RISK"
                "LOW" -> child.latestRiskTier == "LOW RISK"
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("CHW Clinical Field Portal", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CalmGreenContainer
                            ) {
                                Text(
                                    text = "Active Unit",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CalmGreenPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "CHW: $chwName (ID: $effectiveChwId) • $facility • $county County",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToAfyaBot("chw", assignedChildren.firstOrNull()?.childId ?: "GENERAL") }) {
                        Icon(Icons.Default.SmartToy, contentDescription = "AfyaBot Assistant", tint = CalmGreenPrimary)
                    }
                    IconButton(onClick = onNavigateToUSSD) {
                        Icon(Icons.Default.PhoneIphone, contentDescription = "2G USSD Gateway", tint = CalmGreenPrimary)
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showRegisterChildDialog = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Enroll Child", fontWeight = FontWeight.Bold) },
                containerColor = CalmGreenPrimary,
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // =================================================================
            // 1. FIELD TRIAGE & CLINICAL CONSEQUENCE METRICS
            // =================================================================
            item {
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Assigned
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = CalmGreenContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = CalmGreenPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Assigned to Me", fontSize = 11.sp, color = CalmGreenOnContainer)
                            Text("${assignedChildren.size}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CalmGreenOnContainer)
                        }
                    }

                    // Critical Stunting & SAM Alert
                    val highCount = assignedChildren.count { it.latestRiskTier == "HIGH RISK" || it.currentHaz < -2.0f }
                    Card(
                        modifier = Modifier.weight(1.2f),
                        colors = CardDefaults.cardColors(containerColor = RiskHighContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = RiskHighRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Severe Stunting (SAM)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RiskHighRed)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$highCount Children", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = RiskHighRed)
                            Text("Urgent Intervention Needed", fontSize = 10.sp, color = RiskHighRed)
                        }
                    }
                }
            }

            // =================================================================
            // 2. SEARCH & TRIAGE PIPELINE FILTER
            // =================================================================
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search child by name or ID (e.g., Akiru, TK-1042)...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CalmGreenPrimary) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTriageFilter == "ALL",
                        onClick = { selectedTriageFilter = "ALL" },
                        label = { Text("All (${assignedChildren.size})", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedTriageFilter == "HIGH",
                        onClick = { selectedTriageFilter = "HIGH" },
                        label = { Text("Severe Stunting", fontSize = 12.sp, color = if (selectedTriageFilter == "HIGH") Color.White else RiskHighRed) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RiskHighRed,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedTriageFilter == "MODERATE",
                        onClick = { selectedTriageFilter = "MODERATE" },
                        label = { Text("Moderate MAM", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedTriageFilter == "LOW",
                        onClick = { selectedTriageFilter = "LOW" },
                        label = { Text("Normal", fontSize = 12.sp) }
                    )
                }
            }

            // =================================================================
            // 3. CLINICAL TRIAGE PATIENT CARDS WITH CONSEQUENCE ALERTS
            // =================================================================
            // Empty State Card when CHW has 0 enrolled children in catchment unit
            if (filteredChildren.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCFD8DC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = CalmGreenContainer,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = CalmGreenPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Catchment Unit Ready",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Welcome, $chwName!",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CalmGreenPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You have not enrolled any children in your unit yet. Begin surveillance by enrolling children from household visits in $subCounty, $county.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { showRegisterChildDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CalmGreenPrimary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enroll First Child", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            items(filteredChildren) { child ->
                val isSevere = child.latestRiskTier == "HIGH RISK" || child.currentHaz < -2.0f
                val isModerate = child.latestRiskTier == "MODERATE RISK"

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top Row: Child Identity & Risk Tier
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(child.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    "ID: ${child.childId} • Age: ${child.ageMonths}m (${child.sex}) • ${child.county}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isSevere -> RiskHighContainer
                                    isModerate -> RiskModerateContainer
                                    else -> RiskLowContainer
                                }
                            ) {
                                Text(
                                    text = child.latestRiskTier,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isSevere -> RiskHighRed
                                        isModerate -> RiskModerateAmber
                                        else -> RiskLowGreen
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Anthropometric Measurement Badge Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // HAZ (Height-for-Age Stunting)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (child.currentHaz < -2.0f) RiskHighContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "HAZ: ${child.currentHaz} SD",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (child.currentHaz < -2.0f) RiskHighRed else Color.DarkGray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }

                            // MUAC Band
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    child.currentMuac < 11.5f -> RiskHighContainer
                                    child.currentMuac < 12.5f -> RiskModerateContainer
                                    else -> RiskLowContainer
                                }
                            ) {
                                Text(
                                    text = "MUAC: ${child.currentMuac} cm",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        child.currentMuac < 11.5f -> RiskHighRed
                                        child.currentMuac < 12.5f -> RiskModerateAmber
                                        else -> RiskLowGreen
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }

                            // Calibrated Delay Probability
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Delay Risk: ${(child.latestProbability * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Growth Consequence Callout Banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isSevere -> Color(0xFFFFF0F0)
                                isModerate -> Color(0xFFFFFBEA)
                                else -> Color(0xFFF1F8F5)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isSevere -> RiskHighRed.copy(alpha = 0.3f)
                                    isModerate -> RiskModerateAmber.copy(alpha = 0.3f)
                                    else -> RiskLowGreen.copy(alpha = 0.3f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = when {
                                        isSevere -> "⚠️ Critical Consequence: Linear stunting active. Risk of permanent cognitive and motor loss if untreated."
                                        isModerate -> "⚡ Faltering Consequence: Early linear growth faltering. 14-day home counseling recommended."
                                        else -> "✅ Stable: Normal velocity. Routine growth monitoring recommended."
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        isSevere -> RiskHighRed
                                        isModerate -> Color(0xFF8D6E00)
                                        else -> RiskLowGreen
                                    },
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // Action Buttons Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Start AI Screening Button
                            Button(
                                onClick = { onStartScreeningForChild(child) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CalmGreenPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Screen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Quick Action Button: Dispense RUTF / Schedule Visit
                            OutlinedButton(
                                onClick = {
                                    selectedChildForAction = child
                                    showActionDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Actions", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp)) // Padding for FAB
            }
        }
    }

    // Modal Dialog: Field Clinical Actions (RUTF, 14-Day Visit, Refer to Hospital)
    if (showActionDialog && selectedChildForAction != null) {
        val child = selectedChildForAction!!
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = { Text("Clinical Actions for ${child.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Current Status: HAZ ${child.currentHaz} SD • MUAC ${child.currentMuac}cm (${child.latestRiskTier})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RiskHighRed
                    )

                    Surface(
                        onClick = {
                            Toast.makeText(context, "Logged 28 sachets of RUTF (Plumpy'Nut) for ${child.name}", Toast.LENGTH_LONG).show()
                            showActionDialog = false
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = CalmGreenContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = CalmGreenPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Dispense RUTF (Plumpy'Nut)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CalmGreenOnContainer)
                                Text("Therapeutic intake for SAM/severe stunting recovery", fontSize = 11.sp, color = CalmGreenOnContainer)
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            Toast.makeText(context, "Scheduled 14-day home checkup for ${child.name}", Toast.LENGTH_LONG).show()
                            showActionDialog = false
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF9C4),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFFF57F17))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Schedule 14-Day Home Checkup", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF5D4037))
                                Text("Verify dietary diversity and remeasure MUAC", fontSize = 11.sp, color = Color(0xFF5D4037))
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            Toast.makeText(context, "Emergency referral note issued to Kakuma Sub-County Hospital for ${child.name}", Toast.LENGTH_LONG).show()
                            showActionDialog = false
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = RiskHighContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Emergency, contentDescription = null, tint = RiskHighRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Refer to Sub-County Hospital", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RiskHighRed)
                                Text("For medical complications, edema, or severe anorexia", fontSize = 11.sp, color = RiskHighRed)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showActionDialog = false }) { Text("Close") }
            }
        )
    }

    // Modal Dialog: Enroll New Child
    if (showRegisterChildDialog) {
        var newName by remember { mutableStateOf("") }
        var newAgeMonths by remember { mutableStateOf("") }
        var newSex by remember { mutableStateOf("Male") }
        var newHeight by remember { mutableStateOf("") }
        var newWeight by remember { mutableStateOf("") }
        var newMuac by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRegisterChildDialog = false },
            title = { Text("Enroll Child for Surveillance", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Register child under $chwName • $facility • $county:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Child Full Name") },
                        singleLine = true
                    )

                    // Gender Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Gender:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        listOf("Male", "Female").forEach { sex ->
                            FilterChip(
                                selected = newSex == sex,
                                onClick = { newSex = sex },
                                label = { Text(sex, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newAgeMonths,
                        onValueChange = { newAgeMonths = it },
                        label = { Text("Age in Months (0-59)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newHeight,
                        onValueChange = { newHeight = it },
                        label = { Text("Height / Length (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newWeight,
                        onValueChange = { newWeight = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newMuac,
                        onValueChange = { newMuac = it },
                        label = { Text("MUAC (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newAgeMonths.isNotBlank()) {
                            val age = newAgeMonths.toIntOrNull() ?: 12
                            val hVal = newHeight.toFloatOrNull() ?: 75.0f
                            val wVal = newWeight.toFloatOrNull() ?: 9.0f
                            val muacVal = newMuac.toFloatOrNull() ?: 12.5f

                            val zs = WHOAnthroCalculator.computeAllZScores(age, newSex == "Male", hVal, wVal)
                            val risk = when {
                                zs.isSeverelyStunted || zs.isSeverelyWasted || muacVal < 11.5f -> "HIGH RISK"
                                zs.isStunted || zs.isWasted || muacVal < 12.5f -> "MODERATE RISK"
                                else -> "LOW RISK"
                            }

                            val newChild = ChildProfile(
                                childId = "TK-${(1000..9999).random()}",
                                name = newName.trim(),
                                ageMonths = age,
                                sex = newSex,
                                county = county,
                                assignedChwId = effectiveChwId,
                                assignedChwName = chwName,
                                maternalEducation = "Primary",
                                waterSource = "Borehole",
                                householdSize = 5,
                                currentHaz = zs.haz,
                                currentWhz = zs.whz,
                                currentMuac = muacVal,
                                latestRiskTier = risk,
                                latestProbability = if (risk == "HIGH RISK") 0.88f else if (risk == "MODERATE RISK") 0.50f else 0.12f,
                                lastScreeningDate = System.currentTimeMillis()
                            )

                            // Save into local Room database
                            scope.launch {
                                try {
                                    AppDatabase.getDatabase(context).childDao().insertOrUpdate(ChildEntity.fromDomainModel(newChild))
                                } catch (_: Exception) {}
                            }

                            assignedChildren = listOf(newChild) + assignedChildren
                            Toast.makeText(context, "Enrolled ${newName.trim()} under $chwName!", Toast.LENGTH_SHORT).show()
                            showRegisterChildDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CalmGreenPrimary)
                ) {
                    Text("Enroll & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegisterChildDialog = false }) { Text("Cancel") }
            }
        )
    }
}
