package com.strath.finalpr0ject.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strath.finalpr0ject.data.model.CHWProfile
import com.strath.finalpr0ject.data.model.ChildProfile
import com.strath.finalpr0ject.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: CHWs, 1: Children, 2: Analytics

    // Mock initial state for preview & demo sync
    var chwList by remember {
        mutableStateOf(
            listOf(
                CHWProfile("CHW-1042", "Maria Akiru", "maria.akiru@health.go.ke", "+254712345678", "Turkana", "Kakuma West", 5, 2, "Active"),
                CHWProfile("CHW-1088", "Peter Locham", "p.locham@health.go.ke", "+254723456789", "Turkana", "Lodwar North", 3, 0, "Active"),
                CHWProfile("CHW-1105", "Fatuma Wario", "f.wario@health.go.ke", "+254734567890", "Marsabit", "Moyale South", 4, 1, "Pending Password Reset")
            )
        )
    }

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

    // Dialog States
    var showAddCHWDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var selectedChildForAssign by remember { mutableStateOf<ChildProfile?>(null) }
    var showEditChildDialog by remember { mutableStateOf(false) }
    var selectedChildForEdit by remember { mutableStateOf<ChildProfile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AfyaEarly Admin Control Center", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("County Health Directorate • Northern Kenya ASAL", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out")
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
                    text = { Text("CHWs (${chwList.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Children (${childrenList.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.ChildCare, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Analytics", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // TAB 0: CHW Workforce Management
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

                        items(chwList) { chw ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(chw.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (chw.status == "Active") Color(0xFFE8F5E9) else Color(0xFFFFF9C4)
                                        ) {
                                            Text(
                                                chw.status,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (chw.status == "Active") Color(0xFF2E7D32) else Color(0xFFF57F17),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text("ID: ${chw.chwId} • ${chw.email}", fontSize = 12.sp, color = Color.Gray)
                                    Text("Location: ${chw.subCounty}, ${chw.county} County", fontSize = 12.sp, color = Color.DarkGray)
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 4.dp)) {
                                        Text("Assigned Children: ${chw.assignedChildrenCount}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = AdminIndigo)
                                        Text("High Risk Cases: ${chw.activeHighRiskCount}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = RiskHighRed)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: Children Registry (View & Edit & Assign)
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
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(child.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("ID: ${child.childId} • Age: ${child.ageMonths}m (${child.sex})", fontSize = 12.sp, color = Color.Gray)
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
                                                child.latestRiskTier,
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

                                    // Assigned CHW badge
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Assigned CHW: ${child.assignedChwName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            TextButton(
                                                onClick = {
                                                    selectedChildForAssign = child
                                                    showAssignDialog = true
                                                }
                                            ) {
                                                Text("Reassign", fontSize = 12.sp, color = AdminIndigo)
                                            }
                                        }
                                    }

                                    // Action buttons (Edit Profile)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedChildForEdit = child
                                                showEditChildDialog = true
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Edit Profile", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: County Analytics
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = AdminIndigoContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Northern Kenya Surveillance", fontWeight = FontWeight.Bold, color = AdminIndigo)
                                Text("Total Monitored Children: ${childrenList.size}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Active CHWs in Field: ${chwList.size}", fontSize = 14.sp, color = Color.DarkGray)
                            }
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
                                status = "Pending Password Reset"
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
                    chwList.forEach { chw ->
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
                        val ageInt = editAge.toIntOrNull() ?: selectedChildForEdit!!.ageMonths
                        childrenList = childrenList.map { c ->
                            if (c.childId == selectedChildForEdit!!.childId) {
                                c.copy(name = editName, ageMonths = ageInt, maternalEducation = editEdu)
                            } else c
                        }
                        Toast.makeText(context, "Child profile updated!", Toast.LENGTH_SHORT).show()
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
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AdminDashboardScreenPreview() {
    AdminDashboardScreen(onSignOut = {})
}
