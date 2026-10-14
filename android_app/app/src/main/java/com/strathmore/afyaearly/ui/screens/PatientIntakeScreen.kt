package com.strathmore.afyaearly.ui.screens

import androidx.compose.foundation.background
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
import com.strathmore.afyaearly.AfyaEarlyApp
import com.strathmore.afyaearly.data.local.entity.PatientEntity
import com.strathmore.afyaearly.data.local.entity.ScreeningRecordEntity
import com.strathmore.afyaearly.ml.AnthroZScores
import com.strathmore.afyaearly.ml.InferenceOutput
import com.strathmore.afyaearly.ml.WHOAnthroCalculator
import com.strathmore.afyaearly.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientIntakeScreen(
    onNavigateBack: () -> Unit,
    onPredictionComplete: (PatientEntity, ScreeningRecordEntity, InferenceOutput) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Form State
    var childName by remember { mutableStateOf("") }
    var ageMonthsText by remember { mutableStateOf("18") }
    var isMale by remember { mutableStateOf(true) }
    var county by remember { mutableStateOf("Turkana") }
    var maternalEducation by remember { mutableStateOf("None") }
    var waterSource by remember { mutableStateOf("Unimproved") }

    // Anthropometrics
    var weightText by remember { mutableStateOf("8.5") }
    var heightText by remember { mutableStateOf("76.0") }
    var muacText by remember { mutableStateOf("12.0") }
    var hasEdema by remember { mutableStateOf(false) }

    // Dietary Recall (8 food groups)
    var dietGrains by remember { mutableStateOf(true) }
    var dietDairy by remember { mutableStateOf(true) }
    var dietLegumes by remember { mutableStateOf(false) }
    var dietFlesh by remember { mutableStateOf(false) }
    var dietEggs by remember { mutableStateOf(false) }
    var dietVitA by remember { mutableStateOf(false) }
    var dietOtherVeg by remember { mutableStateOf(false) }
    var dietBreastmilk by remember { mutableStateOf(true) }

    // Health History (past 2 weeks)
    var hadDiarrhoea by remember { mutableStateOf(true) }
    var hadFever by remember { mutableStateOf(false) }
    var hadCough by remember { mutableStateOf(true) }
    var isVaccinated by remember { mutableStateOf(false) }
    var isDewormed by remember { mutableStateOf(true) }
    var hadVitA by remember { mutableStateOf(false) }

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

    // DDI calculation
    val ddiScore = listOf(
        dietGrains, dietDairy, dietLegumes, dietFlesh,
        dietEggs, dietVitA, dietOtherVeg
    ).count { it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Paediatric Screening", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Demographics
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. Child Demographics", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealPrimary)

                    OutlinedTextField(
                        value = childName,
                        onValueChange = { childName = it },
                        label = { Text("Child's Full Name (or Caregiver ID)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = ageMonthsText,
                            onValueChange = { ageMonthsText = it },
                            label = { Text("Age (Months: 0-59)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sex", fontSize = 12.sp, color = Color.Gray)
                            Row {
                                FilterChip(
                                    selected = isMale,
                                    onClick = { isMale = true },
                                    label = { Text("Male") }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = !isMale,
                                    onClick = { isMale = false },
                                    label = { Text("Female") }
                                )
                            }
                        }
                    }

                    // County Selector
                    Text("ASAL County", fontSize = 12.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Turkana", "Marsabit", "Wajir", "Mandera").forEach { c ->
                            FilterChip(
                                selected = county == c,
                                onClick = { county = c },
                                label = { Text(c, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Section 2: Anthropometry & Live WHO Z-Scores (FR-03)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("2. Anthropometric Measurements", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealPrimary)

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
                            label = { Text("Length/Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = muacText,
                        onValueChange = { muacText = it },
                        label = { Text("MUAC (cm) — Mid-Upper Arm") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Live WHO Z-Scores Display
                    liveZScores?.let { zs ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Real-time WHO 2006 Standard Z-Scores:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("HAZ: ${zs.haz} ${if (zs.isStunted) "⚠️ Stunted" else "✓"}", fontSize = 13.sp, color = if (zs.isStunted) RiskHighRed else TealPrimary)
                                    Text("WHZ: ${zs.whz} ${if (zs.isWasted) "⚠️ Wasted" else "✓"}", fontSize = 13.sp, color = if (zs.isWasted) RiskHighRed else TealPrimary)
                                    Text("WAZ: ${zs.waz}", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: 24-hr Dietary Recall (DDI)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3. 24-Hour Dietary Recall", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealPrimary)
                        Text("DDI Score: $ddiScore/7", fontWeight = FontWeight.Bold, color = if (ddiScore >= 5) TealPrimary else RiskModerateAmber)
                    }
                    Text("Select all food groups consumed by the child in past 24 hours:", fontSize = 12.sp, color = Color.Gray)

                    val foodGroups = listOf(
                        "Grains, Roots & Tubers" to dietGrains,
                        "Dairy (Milk, Yogurt, Cheese)" to dietDairy,
                        "Legumes & Nuts" to dietLegumes,
                        "Flesh Foods (Meat, Poultry, Fish)" to dietFlesh,
                        "Eggs" to dietEggs,
                        "Vitamin A-rich Fruits & Veg" to dietVitA,
                        "Other Fruits & Vegetables" to dietOtherVeg,
                        "Continued Breastmilk" to dietBreastmilk
                    )

                    foodGroups.forEachIndexed { idx, (label, isChecked) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    when (idx) {
                                        0 -> dietGrains = checked
                                        1 -> dietDairy = checked
                                        2 -> dietLegumes = checked
                                        3 -> dietFlesh = checked
                                        4 -> dietEggs = checked
                                        5 -> dietVitA = checked
                                        6 -> dietOtherVeg = checked
                                        7 -> dietBreastmilk = checked
                                    }
                                }
                            )
                            Text(label, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Section 4: Recent Morbidity History
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("4. Health & Morbidity History", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealPrimary)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = hadDiarrhoea, onCheckedChange = { hadDiarrhoea = it })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Had diarrhoea in past 2 weeks")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = hadFever, onCheckedChange = { hadFever = it })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Had fever episode in past 2 weeks")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isVaccinated, onCheckedChange = { isVaccinated = it })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Vaccination complete for age")
                    }
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // RUN PREDICTION BUTTON
            Button(
                onClick = {
                    val age = ageMonthsText.toIntOrNull()
                    val w = weightText.toFloatOrNull()
                    val h = heightText.toFloatOrNull()
                    val m = muacText.toFloatOrNull()

                    if (age == null || age !in 0..59) {
                        validationError = "Please enter a valid age between 0 and 59 months."
                        return@Button
                    }
                    if (w == null || w <= 0f || h == null || h <= 0f || m == null || m <= 0f) {
                        validationError = "Please enter valid anthropometric numbers."
                        return@Button
                    }

                    validationError = null

                    // 1. Build patient entity
                    val randomId = "TK-${(1000..9999).random()}"
                    val patient = PatientEntity(
                        childId = randomId,
                        name = if (childName.isNotBlank()) childName else "Child $randomId",
                        ageMonths = age,
                        sex = if (isMale) "Male" else "Female",
                        county = county,
                        maternalEducation = maternalEducation,
                        waterSource = waterSource
                    )

                    // 2. Compute WHO Z-scores
                    val zScores = WHOAnthroCalculator.computeAllZScores(age, isMale, h, w)

                    // 3. Build screening entity
                    val screening = ScreeningRecordEntity(
                        childId = randomId,
                        weightKg = w,
                        heightCm = h,
                        muacCm = m,
                        haz = zScores.haz,
                        waz = zScores.waz,
                        whz = zScores.whz,
                        edema = if (hasEdema) 1 else 0,
                        ddiScore = ddiScore,
                        isMad = if (ddiScore >= 5) 1 else 0,
                        diarrhoeaRecent = if (hadDiarrhoea) 1 else 0,
                        feverRecent = if (hadFever) 1 else 0,
                        coughRecent = if (hadCough) 1 else 0,
                        dewormed = if (isDewormed) 1 else 0,
                        vitASupp = if (hadVitA) 1 else 0,
                        vaccinationComplete = if (isVaccinated) 1 else 0
                    )

                    // 4. Run Edge ONNX Model Inference
                    val mlEngine = AfyaEarlyApp.instance.mlEngine
                    val result = mlEngine.predict(patient, screening)

                    // 5. Navigate to Results Screen
                    onPredictionComplete(patient, screening, result)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Run AI Developmental Screening", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
