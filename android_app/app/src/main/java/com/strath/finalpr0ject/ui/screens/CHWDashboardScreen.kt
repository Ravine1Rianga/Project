package com.strath.finalpr0ject.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strath.finalpr0ject.data.model.ChildProfile
import com.strath.finalpr0ject.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CHWDashboardScreen(
    chwId: String,
    onStartScreeningForChild: (ChildProfile) -> Unit,
    onSignOut: () -> Unit
) {
    // Only assigned children for this CHW!
    var assignedChildren by remember {
        mutableStateOf(
            listOf(
                ChildProfile("TK-1042", "Akiru Ewoi", 18, "Male", "Turkana", "CHW-1042", "Maria Akiru", "None", "Unimproved", 6, -2.4f, -2.1f, 11.2f, "HIGH RISK", 0.88f),
                ChildProfile("TK-1088", "Locham Nakor", 24, "Female", "Turkana", "CHW-1042", "Maria Akiru", "Primary", "Improved", 5, 0.2f, -0.4f, 14.5f, "LOW RISK", 0.12f),
                ChildProfile("TK-1150", "Napeyok Lokor", 12, "Female", "Turkana", "CHW-1042", "Maria Akiru", "None", "Unimproved", 5, -1.9f, -1.6f, 11.9f, "MODERATE RISK", 0.52f)
            )
        )
    }

    var showRegisterChildDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("CHW Field Portal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Logged in: Maria Akiru (ID: $chwId) • Turkana Unit", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showRegisterChildDialog = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Enroll Child") },
                containerColor = TealPrimary,
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Metric Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = TealPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Assigned to Me", style = MaterialTheme.typography.bodySmall)
                            Text("${assignedChildren.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = RiskHighContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RiskHighRed)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("High Risk Alerts", style = MaterialTheme.typography.bodySmall, color = RiskHighRed)
                            val highCount = assignedChildren.count { it.latestRiskTier == "HIGH RISK" }
                            Text("$highCount", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = RiskHighRed)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "My Assigned Children",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(assignedChildren) { child ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

                        // Clinical summary snippet
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("MUAC: ${child.currentMuac} cm", fontSize = 12.sp, color = Color.DarkGray)
                            Text("HAZ: ${child.currentHaz}", fontSize = 12.sp, color = Color.DarkGray)
                            Text("WHZ: ${child.currentWhz}", fontSize = 12.sp, color = Color.DarkGray)
                        }

                        // Primary screening action button
                        Button(
                            onClick = { onStartScreeningForChild(child) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch Point-of-Care AI Screening", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Dialog: CHW registers child in field
    if (showRegisterChildDialog) {
        var newName by remember { mutableStateOf("") }
        var newAge by remember { mutableStateOf("14") }
        var newSex by remember { mutableStateOf("Male") }

        AlertDialog(
            onDismissRequest = { showRegisterChildDialog = false },
            title = { Text("Enroll Child in Household") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Register a child directly during field screening. The child will be automatically assigned to you.", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Child's Full Name") })
                    OutlinedTextField(value = newAge, onValueChange = { newAge = it }, label = { Text("Age (Months: 0-59)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            val newChild = ChildProfile(
                                childId = "TK-${(1000..9999).random()}",
                                name = newName,
                                ageMonths = newAge.toIntOrNull() ?: 12,
                                sex = newSex,
                                assignedChwId = chwId,
                                assignedChwName = "Maria Akiru"
                            )
                            assignedChildren = assignedChildren + newChild
                            showRegisterChildDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Enroll Child")
                }
            },
            dismissButton = { TextButton(onClick = { showRegisterChildDialog = false }) { Text("Cancel") } }
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CHWDashboardScreenPreview() {
    CHWDashboardScreen(
        chwId = "CHW-1042",
        onStartScreeningForChild = {},
        onSignOut = {}
    )
}
