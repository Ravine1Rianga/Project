package com.strath.finalpr0ject.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strath.finalpr0ject.AfyaEarlyApp
import com.strath.finalpr0ject.data.model.ChildProfile
import com.strath.finalpr0ject.ml.AnthroZScores
import com.strath.finalpr0ject.ml.InferenceOutput
import com.strath.finalpr0ject.ml.ScreeningInput
import com.strath.finalpr0ject.ml.WHOAnthroCalculator
import com.strath.finalpr0ject.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientIntakeScreen(
    initialChild: ChildProfile,
    onNavigateBack: () -> Unit,
    onPredictionComplete: (ChildProfile, ScreeningInput, InferenceOutput) -> Unit
) {
    val scrollState = rememberScrollState()

    // Form State prefilled from initial child
    var childName by remember { mutableStateOf(initialChild.name) }
    var ageMonthsText by remember { mutableStateOf(initialChild.ageMonths.toString()) }
    var isMale by remember { mutableStateOf(initialChild.sex.equals("Male", ignoreCase = true)) }
    var county by remember { mutableStateOf(initialChild.county) }

    // Anthropometrics
    var weightText by remember { mutableStateOf("7.8") }
    var heightText by remember { mutableStateOf("74.0") }
    var muacText by remember { mutableStateOf("11.2") }
    var hasEdema by remember { mutableStateOf(false) }

    // Dietary Recall (8 groups)
    var dietGrains by remember { mutableStateOf(true) }
    var dietDairy by remember { mutableStateOf(true) }
    var dietLegumes by remember { mutableStateOf(false) }
    var dietFlesh by remember { mutableStateOf(false) }
    var dietEggs by remember { mutableStateOf(false) }
    var dietVitA by remember { mutableStateOf(false) }
    var dietOtherVeg by remember { mutableStateOf(false) }
    var dietBreastmilk by remember { mutableStateOf(true) }

    // Morbidity History
    var hadDiarrhoea by remember { mutableStateOf(true) }
    var hadFever by remember { mutableStateOf(false) }
    var hadCough by remember { mutableStateOf(true) }
    var isVaccinated by remember { mutableStateOf(false) }

    var validationError by remember { mutableStateOf<String?>(null) }

    // Live WHO Z-Scores calculation
    val liveZScores: AnthroZScores? = remember(ageMonthsText, isMale, heightText, weightText) {
        val age = ageMonthsText.toIntOrNull() ?: 12
        val h = heightText.toFloatOrNull() ?: 0f
        val w = weightText.toFloatOrNull() ?: 0f
        if (h > 40f && w > 2f) {
            WHOAnthroCalculator.computeAllZScores(age, isMale, h, w)
        } else null
    }

    val ddiScore = listOf(dietGrains, dietDairy, dietLegumes, dietFlesh, dietEggs, dietVitA, dietOtherVeg).count { it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Screening: ${initialChild.name}", fontWeight = FontWeight.Bold) },
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
            // Section 1: Anthropometry & Live WHO Z-Scores
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Anthropometry & Live WHO Z-Scores", fontWeight = FontWeight.Bold, color = TealPrimary)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it },
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = muacText,
                        onValueChange = { muacText = it },
                        label = { Text("MUAC (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    liveZScores?.let { zs ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Real-time WHO 2006 LMS Calculation:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("HAZ: ${zs.haz} ${if (zs.isStunted) "⚠️ Stunted" else "✓"}", fontSize = 12.sp, color = if (zs.isStunted) RiskHighRed else TealPrimary)
                                    Text("WHZ: ${zs.whz} ${if (zs.isWasted) "⚠️ Wasted" else "✓"}", fontSize = 12.sp, color = if (zs.isWasted) RiskHighRed else TealPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: 24-hr Dietary Recall
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("2. 24-Hour Dietary Recall", fontWeight = FontWeight.Bold, color = TealPrimary)
                        Text("DDI Score: $ddiScore/7", fontWeight = FontWeight.Bold, color = if (ddiScore >= 5) TealPrimary else RiskModerateAmber)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = dietGrains, onCheckedChange = { dietGrains = it })
                        Text("Grains, Roots & Tubers")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = dietDairy, onCheckedChange = { dietDairy = it })
                        Text("Dairy (Camel/Cow Milk)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = dietLegumes, onCheckedChange = { dietLegumes = it })
                        Text("Legumes & Beans")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = dietFlesh, onCheckedChange = { dietFlesh = it })
                        Text("Flesh Foods (Meat/Fish)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = dietEggs, onCheckedChange = { dietEggs = it })
                        Text("Eggs")
                    }
                }
            }

            // RUN PREDICTION BUTTON
            Button(
                onClick = {
                    val age = ageMonthsText.toIntOrNull() ?: initialChild.ageMonths
                    val w = weightText.toFloatOrNull() ?: 8.0f
                    val h = heightText.toFloatOrNull() ?: 75.0f
                    val m = muacText.toFloatOrNull() ?: 12.0f
                    val zs = WHOAnthroCalculator.computeAllZScores(age, isMale, h, w)

                    val input = ScreeningInput(
                        child = initialChild.copy(ageMonths = age),
                        weightKg = w,
                        heightCm = h,
                        muacCm = m,
                        haz = zs.haz,
                        waz = zs.waz,
                        whz = zs.whz,
                        edema = if (hasEdema) 1 else 0,
                        ddiScore = ddiScore,
                        isMad = if (ddiScore >= 5) 1 else 0,
                        diarrhoeaRecent = if (hadDiarrhoea) 1 else 0,
                        feverRecent = if (hadFever) 1 else 0,
                        coughRecent = if (hadCough) 1 else 0,
                        dewormed = 1,
                        vitASupp = 1,
                        vaccinationComplete = if (isVaccinated) 1 else 0
                    )

                    val result = AfyaEarlyApp.instance.mlEngine.predict(input)
                    onPredictionComplete(initialChild, input, result)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Run Edge AI Screening & SHAP", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
