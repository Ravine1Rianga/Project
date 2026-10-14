package com.strath.finalpr0ject.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strath.finalpr0ject.data.model.ChildProfile
import com.strath.finalpr0ject.ml.InferenceOutput
import com.strath.finalpr0ject.ml.ScreeningInput
import com.strath.finalpr0ject.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    child: ChildProfile,
    screening: ScreeningInput,
    result: InferenceOutput,
    onNavigateBack: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val (badgeColor, containerColor) = when (result.riskTier) {
        "HIGH RISK" -> RiskHighRed to RiskHighContainer
        "MODERATE RISK" -> RiskModerateAmber to RiskModerateContainer
        else -> RiskLowGreen to RiskLowContainer
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Risk Assessment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Patient Identification Banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(child.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("ID: ${child.childId} • Age: ${child.ageMonths}m (${child.sex})", fontSize = 13.sp, color = Color.Gray)
                    Text("Assigned CHW: ${child.assignedChwName} • ${child.county}", fontSize = 13.sp, color = Color.Gray)
                }
            }

            // Prominent Risk Badge
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = result.riskTier,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = badgeColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Calibrated Delay Risk: ${(result.calibratedProbability * 100).toInt()}%",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = badgeColor
                    )
                }
            }

            // Clinical Triage Action Box
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = badgeColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recommended Clinical Action:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = result.recommendedAction,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }

            // Plain-Language SHAP Explainability Section
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Key Factors Driving Score (SHAP)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TealPrimary
                        )
                    }

                    result.topContributingFactors.forEach { factor ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "#${factor.rank}",
                                fontWeight = FontWeight.Bold,
                                color = if (factor.isRiskFactor) RiskHighRed else TealPrimary,
                                modifier = Modifier.width(28.dp)
                            )
                            Column {
                                Text(
                                    text = factor.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = factor.statement,
                                    fontSize = 13.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Referral summary note generated for ${child.name}", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Print / Export Referral Note")
            }

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Sync to Firebase & Return", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
