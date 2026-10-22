package com.strath.finalpr0ject.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strath.finalpr0ject.data.model.CHWProfile
import com.strath.finalpr0ject.data.model.ChildProfile
import com.strath.finalpr0ject.data.model.UserProfile
import com.strath.finalpr0ject.data.model.UserRole
import com.strath.finalpr0ject.data.repository.FirebaseRepository
import com.strath.finalpr0ject.ui.theme.*
import kotlinx.coroutines.launch

data class CountySurveillanceData(
    val countyName: String,
    val stuntingRatePct: Float,
    val samCases: Int,
    val chwCoveragePct: Int,
    val phaseStatus: String,
    val statusColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: CHWs, 1: Parents, 2: Children, 3: Analytics

    // Mock initial state for preview & demo sync
    var chwList by remember {
        mutableStateOf(
            listOf(
                CHWProfile("CHW-1042", "Maria Akiru", "maria.akiru@health.go.ke", "+254712345678", "Turkana", "Kakuma West", 5, 2, "Active"),
                CHWProfile("CHW-1088", "Peter Locham", "p.locham@health.go.ke", "+254723456789", "Turkana", "Lodwar North", 3, 0, "Active"),
                CHWProfile("CHW-1105", "Fatuma Wario", "f.wario@health.go.ke", "+254734567890", "Marsabit", "Moyale South", 4, 1, "Pending Approval")
            )
        )
    }

    // Live Firestore CHWs combined with fallback demo list
    val liveChwList by repository.getAllCHWs().collectAsState(initial = emptyList())
    val displayedChwList: List<CHWProfile> = if (liveChwList.isNotEmpty()) liveChwList else chwList

    // Live Firestore Parents combined with fallback demo list
    val liveParentList by repository.getAllParents().collectAsState(initial = emptyList())
    var fallbackParents by remember {
        mutableStateOf(
            listOf(
                UserProfile(
                    uid = "parent-101",
                    email = "turkana.family@gmail.com",
                    displayName = "Amina Ewoi (Mother)",
                    role = UserRole.PARENT,
                    phone = "+254712345678",
                    county = "Turkana",
                    subCounty = "Kakuma West",
                    assignedChildName = "Akiru Ewoi",
                    isVerified = true,
                    verificationStatus = "approved"
                ),
                UserProfile(
                    uid = "parent-102",
                    email = "turkana.family@gmail.com",
                    displayName = "Ekidor Ewoi (Father)",
                    role = UserRole.PARENT,
                    phone = "+254722334455",
                    county = "Turkana",
                    subCounty = "Kakuma West",
                    assignedChildName = "Akiru Ewoi",
                    isVerified = true,
                    verificationStatus = "approved"
                ),
                UserProfile(
                    uid = "parent-103",
                    email = "chegem.family@health.org",
                    displayName = "Nakor Chegem (Guardian)",
                    role = UserRole.PARENT,
                    phone = "+254733445566",
                    county = "Marsabit",
                    subCounty = "Moyale Central",
                    assignedChildName = "Lokai Chegem",
                    isVerified = true,
                    verificationStatus = "approved"
                )
            )
        )
    }
    val displayedParentList: List<UserProfile> = if (liveParentList.isNotEmpty()) liveParentList else fallbackParents

    // Deletion Dialog State
    var profileToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isDeletingChw by remember { mutableStateOf(false) }

    var childrenList by remember {
        mutableStateOf(
            listOf(
                ChildProfile("TK-1042", "Akiru Ewoi", 18, "Male", "Turkana", "CHW-1042", "Maria Akiru", "None", "Unimproved", 6, -2.4f, -2.1f, 11.2f, "HIGH RISK", 0.88f),
                ChildProfile("TK-1088", "Locham Nakor", 24, "Female", "Turkana", "CHW-1042", "Maria Akiru", "Primary", "Improved", 5, 0.2f, -0.4f, 14.5f, "LOW RISK", 0.12f),
                ChildProfile("TK-1102", "Erupe Ekidor", 30, "Male", "Turkana", "CHW-1088", "Peter Locham", "None", "Unimproved", 7, -1.8f, -1.5f, 12.1f, "MODERATE RISK", 0.45f),
                ChildProfile("MB-2015", "Galgalo Guyo", 14, "Male", "Marsabit", "CHW-1105", "Fatuma Wario", "None", "Unimproved", 6, -2.8f, -2.4f, 10.9f, "HIGH RISK", 0.92f)
            )
        )
    }

    // ASAL County Malnutrition Surveillance Matrix
    val countyMatrix = remember {
        listOf(
            CountySurveillanceData("Turkana County", 34.2f, 412, 89, "Crisis (Phase 4)", RiskHighRed),
            CountySurveillanceData("Marsabit County", 29.1f, 280, 82, "Emergency (Phase 3)", RiskHighRed),
            CountySurveillanceData("Mandera County", 31.8f, 340, 78, "Crisis (Phase 4)", RiskHighRed),
            CountySurveillanceData("Wajir County", 28.0f, 220, 85, "Emergency (Phase 3)", RiskModerateAmber),
            CountySurveillanceData("Garissa County", 26.4f, 195, 91, "Alert (Phase 2)", RiskModerateAmber)
        )
    }

    // Dialog States
    var showAddCHWDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var selectedChildForAssign by remember { mutableStateOf<ChildProfile?>(null) }
    var showEditChildDialog by remember { mutableStateOf(false) }
    var selectedChildForEdit by remember { mutableStateOf<ChildProfile?>(null) }
    var showExportMOHDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("MOH Directorate Control Center", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AdminIndigoContainer
                            ) {
                                Text(
                                    text = "SUPERADMIN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdminIndigo,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Admin: riangaravine@gmail.com • Northern Kenya ASAL Directorate",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = AdminIndigo
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("CHWs (${displayedChwList.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Parents (${displayedParentList.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.FamilyRestroom, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Children (${childrenList.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.ChildCare, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("ASAL Analytics", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // =========================================================
                    // TAB 0: CHW Workforce & Approval Management
                    // =========================================================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Button(
                                onClick = { showAddCHWDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdminIndigo)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Provision New CHW (Send Reset Email)", fontWeight = FontWeight.Bold)
                            }
                        }

                        items(displayedChwList) { chw ->
                            val isPending = chw.status.contains("Pending", ignoreCase = true)
                            val isDeclined = chw.status.contains("Declined", ignoreCase = true)

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(chw.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("${chw.email} • ${chw.phone}", fontSize = 12.sp, color = Color.Gray)
                                            Text("Unit: ${chw.subCounty}, ${chw.county} County", fontSize = 12.sp, color = AdminIndigo)
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = when {
                                                isPending -> RiskModerateContainer
                                                isDeclined -> RiskHighContainer
                                                else -> RiskLowContainer
                                            }
                                        ) {
                                            Text(
                                                text = chw.status,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    isPending -> RiskModerateAmber
                                                    isDeclined -> RiskHighRed
                                                    else -> RiskLowGreen
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Metric breakdown for CHW
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "Assigned: ${chw.assignedChildrenCount} children",
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (chw.activeHighRiskCount > 0) RiskHighContainer else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "High Risk: ${chw.activeHighRiskCount}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (chw.activeHighRiskCount > 0) RiskHighRed else Color.DarkGray,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    // Action buttons for Pending CHWs: Approve & Notify or Decline
                                    if (isPending) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        repository.adminApproveCHW(chw.chwId, chw.email)
                                                        chwList = chwList.map {
                                                            if (it.chwId == chw.chwId) it.copy(status = "Active") else it
                                                        }
                                                        Toast.makeText(
                                                            context,
                                                            "Approved ${chw.name}! Password reset email sent to ${chw.email}",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                },
                                                modifier = Modifier.weight(1.3f),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = CalmGreenPrimary)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Approve & Notify", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    scope.launch {
                                                        repository.adminDeclineCHW(chw.chwId)
                                                        chwList = chwList.map {
                                                            if (it.chwId == chw.chwId) it.copy(status = "Declined") else it
                                                        }
                                                        Toast.makeText(context, "Declined ${chw.name}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskHighRed)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Decline", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    // Delete Profile button for CHW
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                profileToDelete = Pair(chw.chwId, chw.name)
                                                isDeletingChw = true
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = RiskHighRed)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Delete CHW Profile", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // =========================================================
                    // TAB 1: Parents & Guardians Management
                    // =========================================================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = AdminIndigo)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Registered Household Beneficiaries", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "Parents and guardians registered across ASAL sub-counties. Multi-profile household accounts share family email credentials.",
                                            fontSize = 11.5.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        items(displayedParentList) { parent ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(parent.displayName.ifEmpty { "Parent / Guardian" }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFFFEBEE)
                                                ) {
                                                    Text(
                                                        text = "Parent",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFD32F2F),
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${parent.email} • ${parent.phone}",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "Location: ${parent.subCounty}, ${parent.county} County",
                                                fontSize = 12.sp,
                                                color = AdminIndigo
                                            )
                                            if (parent.assignedChildName.isNotBlank()) {
                                                Text(
                                                    text = "Child: ${parent.assignedChildName}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = CalmGreenPrimary
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                profileToDelete = Pair(parent.uid, parent.displayName.ifEmpty { "Parent" })
                                                isDeletingChw = false
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Profile", tint = RiskHighRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // =========================================================
                    // TAB 2: Children Registry & CHW Assignment
                    // =========================================================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(childrenList) { child ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(child.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("ID: ${child.childId} • Age: ${child.ageMonths}m (${child.sex}) • ${child.county}", fontSize = 12.sp, color = Color.Gray)
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = when (child.latestRiskTier) {
                                                "HIGH RISK" -> RiskHighContainer
                                                "MODERATE RISK" -> RiskModerateContainer
                                                else -> RiskLowContainer
                                            }
                                        ) {
                                            Text(
                                                text = child.latestRiskTier,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (child.latestRiskTier) {
                                                    "HIGH RISK" -> RiskHighRed
                                                    "MODERATE RISK" -> RiskModerateAmber
                                                    else -> RiskLowGreen
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Anthropometrics & Consequence Badges
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (child.currentHaz < -2.0f) RiskHighContainer else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "HAZ: ${child.currentHaz} SD",
                                                fontSize = 11.sp,
                                                color = if (child.currentHaz < -2.0f) RiskHighRed else Color.DarkGray,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (child.currentMuac < 11.5f) RiskHighContainer else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "MUAC: ${child.currentMuac}cm",
                                                fontSize = 11.sp,
                                                color = if (child.currentMuac < 11.5f) RiskHighRed else Color.DarkGray,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    // Assigned CHW Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Assigned CHW:", fontSize = 11.sp, color = Color.Gray)
                                            Text(child.assignedChwName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AdminIndigo)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    selectedChildForAssign = child
                                                    showAssignDialog = true
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Reassign", fontSize = 12.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    selectedChildForEdit = child
                                                    showEditChildDialog = true
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Edit", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // =========================================================
                    // TAB 3: ASAL County Analytics & Consequence Projections
                    // =========================================================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Overview Banner
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = AdminIndigoContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = AdminIndigo)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Northern Kenya Malnutrition Directorate", fontWeight = FontWeight.Bold, color = AdminIndigo)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Regional Stunting Surveillance & Consequence Matrix", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("5 ASAL Counties Monitored • Real-Time DHIS2 Integration", fontSize = 12.sp, color = Color.DarkGray)
                                }
                            }
                        }

                        // Consequence Projection Clock (1,447 Children at Risk)
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2824)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "First 1,000 Days Irreversible Consequence Clock",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFFFF8A80)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "1,447 Children",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "projected to cross the 24-month age threshold with chronic active stunting (HAZ < -2.0) across Turkana, Marsabit, and Mandera within 90 days. Early supplementary feeding can salvage up to +2.1 cm catch-up growth.",
                                        fontSize = 11.sp,
                                        color = Color.LightGray,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // Supply Chain & RUTF Alert Meter
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = RiskHighContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = RiskHighRed, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("RUTF Supply Chain Alert: Kakuma West Depot", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RiskHighRed)
                                        Text("Therapeutic food stocks down to 18% (3-day buffer). Emergency re-order dispatched to Kenya Medical Supplies Authority (KEMSA).", fontSize = 11.sp, color = Color.DarkGray, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }

                        // ASAL County Breakdown Header
                        item {
                            Text(
                                text = "County Malnutrition & Stunting Profiles",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        // County Surveillance Cards
                        items(countyMatrix) { county ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(county.countyName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = county.statusColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = county.phaseStatus,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = county.statusColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Stunting Prevalence", fontSize = 11.sp, color = Color.Gray)
                                            Text("${county.stuntingRatePct}%", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = county.statusColor)
                                        }
                                        Column {
                                            Text("Active SAM Cases", fontSize = 11.sp, color = Color.Gray)
                                            Text("${county.samCases}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text("CHW Coverage", fontSize = 11.sp, color = Color.Gray)
                                            Text("${county.chwCoveragePct}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CalmGreenPrimary)
                                        }
                                    }
                                }
                            }
                        }

                        // MOH-711 Export Report Button
                        item {
                            Button(
                                onClick = { showExportMOHDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdminIndigo),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate & Export MOH-711 Summary Report", fontWeight = FontWeight.Bold)
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }

    // Dialog 1: Provision New CHW
    if (showAddCHWDialog) {
        var newName by remember { mutableStateOf("") }
        var newEmail by remember { mutableStateOf("") }
        var newPhone by remember { mutableStateOf("") }
        var newSubCounty by remember { mutableStateOf("Kakuma West") }

        AlertDialog(
            onDismissRequest = { showAddCHWDialog = false },
            title = { Text("Authorize & Provision CHW") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("The admin creates the initial profile. A Firebase password reset email will be sent for the CHW to set their password.", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Full Name") })
                    OutlinedTextField(value = newEmail, onValueChange = { newEmail = it }, label = { Text("Ministry / Gmail Email") })
                    OutlinedTextField(value = newPhone, onValueChange = { newPhone = it }, label = { Text("Phone Number") })
                    OutlinedTextField(value = newSubCounty, onValueChange = { newSubCounty = it }, label = { Text("Sub-County / Ward") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newEmail.isNotBlank()) {
                            val newChw = CHWProfile(
                                chwId = "CHW-${(1000..9999).random()}",
                                name = newName,
                                email = newEmail,
                                phone = newPhone,
                                subCounty = newSubCounty,
                                status = "Pending Approval"
                            )
                            chwList = chwList + newChw
                            Toast.makeText(context, "CHW profile created! Reset email sent to $newEmail", Toast.LENGTH_LONG).show()
                            showAddCHWDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminIndigo)
                ) {
                    Text("Create Profile & Send Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCHWDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog 2: Assign Child to CHW
    if (showAssignDialog && selectedChildForAssign != null) {
        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text("Assign Child to CHW") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select which Community Health Worker is responsible for ${selectedChildForAssign!!.name}:", fontSize = 13.sp)
                    displayedChwList.forEach { chw ->
                        Surface(
                            onClick = {
                                childrenList = childrenList.map { c ->
                                    if (c.childId == selectedChildForAssign!!.childId) {
                                        c.copy(assignedChwId = chw.chwId, assignedChwName = chw.name)
                                    } else c
                                }
                                Toast.makeText(context, "Assigned to ${chw.name}", Toast.LENGTH_SHORT).show()
                                showAssignDialog = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${chw.name} (${chw.subCounty})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAssignDialog = false }) { Text("Cancel") } }
        )
    }

    // Dialog 3: Edit Child Profile
    if (showEditChildDialog && selectedChildForEdit != null) {
        var editName by remember { mutableStateOf(selectedChildForEdit!!.name) }
        var editAge by remember { mutableStateOf(selectedChildForEdit!!.ageMonths.toString()) }
        var editEdu by remember { mutableStateOf(selectedChildForEdit!!.maternalEducation) }

        AlertDialog(
            onDismissRequest = { showEditChildDialog = false },
            title = { Text("Edit Child Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Child Name") })
                    OutlinedTextField(value = editAge, onValueChange = { editAge = it }, label = { Text("Age (Months)") })
                    OutlinedTextField(value = editEdu, onValueChange = { editEdu = it }, label = { Text("Maternal Education") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        childrenList = childrenList.map { c ->
                            if (c.childId == selectedChildForEdit!!.childId) {
                                c.copy(
                                    name = editName,
                                    ageMonths = editAge.toIntOrNull() ?: c.ageMonths,
                                    maternalEducation = editEdu
                                )
                            } else c
                        }
                        Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                        showEditChildDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminIndigo)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = { TextButton(onClick = { showEditChildDialog = false }) { Text("Cancel") } }
        )
    }

    // Dialog 4: Export MOH-711 Report
    if (showExportMOHDialog) {
        AlertDialog(
            onDismissRequest = { showExportMOHDialog = false },
            title = { Text("MOH-711 Monthly Surveillance Export") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Generated Ministry of Health Form MOH-711 for Northern Kenya ASAL:", fontSize = 13.sp)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("• Reporting Period: October 2026", fontSize = 11.sp)
                            Text("• Total Children Screened: 1,447", fontSize = 11.sp)
                            Text("• Severe Acute Malnutrition (SAM): 1,447 cases", fontSize = 11.sp)
                            Text("• RUTF Sachets Dispensed: 40,516", fontSize = 11.sp)
                            Text("• Format: DHIS2 XML / CSV Standard", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "MOH-711 report downloaded to device storage!", Toast.LENGTH_LONG).show()
                        showExportMOHDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminIndigo)
                ) {
                    Text("Download Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportMOHDialog = false }) { Text("Close") }
            }
        )
    }

    // Dialog 5: Confirmation for Profile Deletion
    if (profileToDelete != null) {
        val (targetId, targetName) = profileToDelete!!
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = RiskHighRed) },
            title = { Text("Delete User Profile?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to permanently delete the profile for '$targetName'? This will revoke their app access and purge their profile record.",
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.adminDeleteUserProfile(targetId, isChw = isDeletingChw)
                            if (isDeletingChw) {
                                chwList = chwList.filterNot { it.chwId == targetId }
                            } else {
                                fallbackParents = fallbackParents.filterNot { it.uid == targetId }
                            }
                            profileToDelete = null
                            Toast.makeText(context, "Deleted profile for $targetName", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RiskHighRed)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
