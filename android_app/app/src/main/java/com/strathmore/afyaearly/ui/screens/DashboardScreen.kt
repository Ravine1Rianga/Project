package com.strathmore.afyaearly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strathmore.afyaearly.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onStartNewScreening: () -> Unit,
    onSignOut: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AfyaEarly Screening Portal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Northern Kenya • Turkana Unit", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                actions = {
                    TextButton(onClick = onSignOut) {
                        Text("Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartNewScreening,
                icon = { Icon(Icons.Default.Add, contentDescription = "New") },
                text = { Text("New Screening") },
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
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search child ID or name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Summary Metric Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.ChildCare, contentDescription = null, tint = TealPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Total Screened", style = MaterialTheme.typography.bodySmall)
                            Text("142", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = RiskHighContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RiskHighRed)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("High Risk Triage", style = MaterialTheme.typography.bodySmall, color = RiskHighRed)
                            Text("38", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = RiskHighRed)
                        }
                    }
                }
            }

            // Recent Screenings Section
            item {
                Text(
                    text = "Recent Community Screenings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(3) { index ->
                val sampleIds = listOf("TK-1042", "TK-1088", "TK-1102")
                val sampleNames = listOf("Akiru Ewoi", "Locham Nakor", "Erupe Ekidor")
                val sampleRisks = listOf("HIGH RISK", "LOW RISK", "MODERATE RISK")
                val sampleColors = listOf(RiskHighRed to RiskHighContainer, RiskLowGreen to RiskLowContainer, RiskModerateAmber to RiskModerateContainer)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(sampleNames[index], fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("ID: ${sampleIds[index]} • Age: ${18 + index * 4}m", fontSize = 13.sp, color = Color.Gray)
                        }

                        val (textColor, bgColor) = sampleColors[index]
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = bgColor
                        ) {
                            Text(
                                text = sampleRisks[index],
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
