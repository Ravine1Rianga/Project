package com.strath.finalpr0ject.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strath.finalpr0ject.data.local.entity.ScreeningEntity
import com.strath.finalpr0ject.data.model.ChildProfile
import com.strath.finalpr0ject.ml.WHOAnthroCalculator
import java.text.SimpleDateFormat
import java.util.*

/**
 * DigitizedRoadToHealthChart
 * ==========================
 * Digital reproduction of the Kenyan Ministry of Health / WHO "Road to Health"
 * growth monitoring book.
 * Plots:
 * 1. WHO Standard Reference Curves (+3 SD, +2 SD, 0 Median, -2 SD, -3 SD)
 * 2. Danger zone / Referral shaded regions
 * 3. Child's real recorded monthly visit points & trajectory line (Good vs Danger trend)
 * 4. Month-by-month visits table right below with Weight, Height, Gains/Losses, and Feeding Category
 * 5. Quick inline monthly measurements logger
 */
@Composable
fun DigitizedRoadToHealthChart(
    child: ChildProfile,
    screeningVisits: List<ScreeningEntity>,
    onAddMonthlyVisit: ((ScreeningEntity) -> Unit)? = null,
    showInlineLogger: Boolean = true,
    modifier: Modifier = Modifier
) {
    var selectedMetric by remember { mutableStateOf("WEIGHT") } // "WEIGHT" or "HEIGHT"
    val isMale = child.sex.equals("Male", ignoreCase = true)

    // Inline Logger State
    var newVisitAge by remember(child.ageMonths) { mutableStateOf((child.ageMonths + 1).coerceAtMost(59).toString()) }
    var newVisitWeight by remember { mutableStateOf("") }
    var newVisitHeight by remember { mutableStateOf("") }
    var newVisitMuac by remember(child.currentMuac) { mutableStateOf(if (child.currentMuac > 0f) child.currentMuac.toString() else "12.5") }
    var showLoggerExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Chart Title & Metric Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (selectedMetric == "WEIGHT") "Weight-for-Age (Road to Health)" else "Length/Height-for-Age",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${if (isMale) "Boys" else "Girls"} (0 - 60 Months) • Digital Mother & Child Booklet",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Row(
                    modifier = Modifier
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
                        .padding(3.dp)
                ) {
                    MetricTabButton(
                        text = "Weight (kg)",
                        selected = selectedMetric == "WEIGHT",
                        onClick = { selectedMetric = "WEIGHT" }
                    )
                    MetricTabButton(
                        text = "Height (cm)",
                        selected = selectedMetric == "HEIGHT",
                        onClick = { selectedMetric = "HEIGHT" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Milestone Feeding & Stage Indicators (Kenya MOH Booklet Header)
            FeedingStageBar(ageMonths = child.ageMonths)

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Color(0xFFFCFDFA), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    .padding(top = 10.dp, bottom = 22.dp, start = 34.dp, end = 16.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Coordinate system boundaries
                    val minAge = 0f
                    val maxAge = 60f
                    val minVal = if (selectedMetric == "WEIGHT") 1f else 40f
                    val maxVal = if (selectedMetric == "WEIGHT") 28f else 125f

                    fun toX(age: Float): Float = ((age - minAge) / (maxAge - minAge)) * width
                    fun toY(v: Float): Float = height - (((v - minVal) / (maxVal - minVal)) * height)

                    // Draw Background Grid (Years: 1yr=12m, 2yr=24m, 3yr=36m, 4yr=48m, 5yr=60m)
                    val yearAges = listOf(12f, 24f, 36f, 48f, 60f)
                    for (yAge in yearAges) {
                        val x = toX(yAge)
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1.2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                    }

                    // Value horizontal grids
                    val gridSteps = if (selectedMetric == "WEIGHT") (5..25 step 5).toList() else (50..120 step 10).toList()
                    for (step in gridSteps) {
                        val y = toY(step.toFloat())
                        drawLine(
                            color = Color(0xFFF1F5F9),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    // WHO Curves Data Points (0, 3, 6, 9, 12, 18, 24, 36, 48, 60 months)
                    val sampleAges = listOf(0f, 3f, 6f, 9f, 12f, 18f, 24f, 36f, 48f, 60f)

                    fun getWhoSD(sd: Float, age: Float): Float {
                        return if (selectedMetric == "WEIGHT") {
                            val baseMedian = if (isMale) {
                                3.3f + (age / 60f) * 15.0f + if (age < 12f) (age / 12f) * 4.0f else 4.0f
                            } else {
                                3.2f + (age / 60f) * 15.0f + if (age < 12f) (age / 12f) * 3.5f else 3.5f
                            }
                            val sdStep = 1.0f + (age / 60f) * 2.2f
                            (baseMedian + sd * sdStep).coerceAtLeast(minVal)
                        } else {
                            val baseMedian = if (isMale) {
                                49.9f + if (age <= 12f) (age / 12f) * 25.8f else 25.8f + ((age - 12f) / 48f) * 34.3f
                            } else {
                                49.1f + if (age <= 12f) (age / 12f) * 24.9f else 24.9f + ((age - 12f) / 48f) * 35.4f
                            }
                            val sdStep = 2.0f + (age / 60f) * 3.5f
                            (baseMedian + sd * sdStep).coerceAtLeast(minVal)
                        }
                    }

                    // Shaded Danger / Referral Zone below -2 SD (Pink/Red Area like in Kenya booklet)
                    val dangerPath = Path()
                    dangerPath.moveTo(toX(0f), toY(minVal))
                    for (a in sampleAges) {
                        dangerPath.lineTo(toX(a), toY(getWhoSD(-2f, a)))
                    }
                    dangerPath.lineTo(toX(60f), toY(minVal))
                    dangerPath.close()
                    drawPath(dangerPath, color = Color(0xFFFDE8E8)) // Light red danger band

                    // Draw Standard Reference Lines
                    fun drawCurve(sd: Float, color: Color, strokeWidth: Float, isDashed: Boolean = false) {
                        val path = Path()
                        for ((idx, a) in sampleAges.withIndex()) {
                            val px = toX(a)
                            val py = toY(getWhoSD(sd, a))
                            if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(
                                width = strokeWidth,
                                pathEffect = if (isDashed) PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f) else null
                            )
                        )
                    }

                    // +3 SD (Red dashed)
                    drawCurve(3f, Color(0xFFE53935), 1.8f, isDashed = true)
                    // +2 SD (Yellow solid)
                    drawCurve(2f, Color(0xFFFDD835), 2.2f)
                    // 0 SD Median (Green solid - target)
                    drawCurve(0f, Color(0xFF2E7D32), 2.8f)
                    // -2 SD (Yellow solid)
                    drawCurve(-2f, Color(0xFFFDD835), 2.2f)
                    // -3 SD (Red dashed - severe undernutrition)
                    drawCurve(-3f, Color(0xFFD32F2F), 1.8f, isDashed = true)

                    // Draw Child's Actual Plotted Data Points
                    val sortedVisits = screeningVisits.filter {
                        (if (selectedMetric == "WEIGHT") it.weightKg else it.heightCm) > 0f
                    }.sortedBy { it.ageMonths }

                    if (sortedVisits.isNotEmpty()) {
                        val childLinePath = Path()
                        for ((idx, visit) in sortedVisits.withIndex()) {
                            val vVal = if (selectedMetric == "WEIGHT") visit.weightKg else visit.heightCm
                            val px = toX(visit.ageMonths.toFloat())
                            val py = toY(vVal)

                            if (idx == 0) childLinePath.moveTo(px, py) else childLinePath.lineTo(px, py)

                            // Point circle
                            drawCircle(
                                color = Color(0xFF1E293B),
                                radius = 5.5f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color(0xFF38BDF8),
                                radius = 3.5f,
                                center = Offset(px, py)
                            )
                        }

                        // Connect child's monthly points
                        drawPath(
                            path = childLinePath,
                            color = Color(0xFF0F172A),
                            style = Stroke(width = 3.2f)
                        )
                    }

                    // Axis labels
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = 24f
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText("0m", toX(0f), height + 24f, paint)
                    drawContext.canvas.nativeCanvas.drawText("1yr", toX(12f) - 15f, height + 24f, paint)
                    drawContext.canvas.nativeCanvas.drawText("2yr", toX(24f) - 15f, height + 24f, paint)
                    drawContext.canvas.nativeCanvas.drawText("3yr", toX(36f) - 15f, height + 24f, paint)
                    drawContext.canvas.nativeCanvas.drawText("5yr", toX(60f) - 25f, height + 24f, paint)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chart Legend
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = Color(0xFF2E7D32), label = "Median (Target)")
                    LegendItem(color = Color(0xFFFDD835), label = "±2 SD (Counsel)")
                    LegendItem(color = Color(0xFFE53935), label = "±3 SD (Refer)")
                    LegendItem(color = Color(0xFF0F172A), label = "${child.name} Trend")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Growth Velocity Assessment
            GrowthVelocityAssessment(screeningVisits = screeningVisits, selectedMetric = selectedMetric)

            // =========================================================================
            // MONTH-BY-MONTH VISITS TABLE (Traditional Book Table Digitized)
            // =========================================================================
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Month-by-Month Clinical Visits History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = Color(0xFF0F172A)
                )
                if (showInlineLogger && onAddMonthlyVisit != null) {
                    TextButton(onClick = { showLoggerExpanded = !showLoggerExpanded }) {
                        Icon(
                            if (showLoggerExpanded) Icons.Default.ExpandLess else Icons.Default.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showLoggerExpanded) "Hide Logger" else "+ Add Visit", fontSize = 12.sp)
                    }
                }
            }

            // Quick Inline Monthly Measurements Logger Form
            if (showLoggerExpanded && onAddMonthlyVisit != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Record Monthly Follow-Up for ${child.name}:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color(0xFF166534)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newVisitAge,
                                onValueChange = { newVisitAge = it },
                                label = { Text("Age (m)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newVisitWeight,
                                onValueChange = { newVisitWeight = it },
                                label = { Text("Weight (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newVisitHeight,
                                onValueChange = { newVisitHeight = it },
                                label = { Text("Height (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newVisitMuac,
                                onValueChange = { newVisitMuac = it },
                                label = { Text("MUAC (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = {
                                val age = newVisitAge.toIntOrNull() ?: (child.ageMonths + 1)
                                val w = newVisitWeight.toFloatOrNull() ?: 9.0f
                                val h = newVisitHeight.toFloatOrNull() ?: 75.0f
                                val m = newVisitMuac.toFloatOrNull() ?: 12.5f

                                val zs = WHOAnthroCalculator.computeAllZScores(age, isMale, h, w)
                                val risk = when {
                                    zs.isSeverelyStunted || zs.isSeverelyWasted || m < 11.5f -> "HIGH RISK"
                                    zs.isStunted || zs.isWasted || m < 12.5f -> "MODERATE RISK"
                                    else -> "LOW RISK"
                                }
                                val feedingCat = when {
                                    age < 6 -> "Exclusive Breastfeeding (0-6m)"
                                    age in 6..8 -> "Complementary Weaning (6-9m)"
                                    age in 9..11 -> "Finger Foods (9-11m)"
                                    else -> "Family Foods (1-2+ yrs)"
                                }

                                val visitEntity = ScreeningEntity(
                                    childId = child.childId,
                                    timestamp = System.currentTimeMillis(),
                                    ageMonths = age,
                                    weightKg = w,
                                    heightCm = h,
                                    haz = zs.haz,
                                    whz = zs.whz,
                                    muac = m,
                                    stuntingRiskTier = risk,
                                    predictedProbability = if (risk == "HIGH RISK") 0.85f else if (risk == "MODERATE RISK") 0.50f else 0.12f,
                                    clinicalNotes = "Monthly follow-up visit",
                                    feedingCategory = feedingCat,
                                    isSynced = false
                                )
                                onAddMonthlyVisit(visitEntity)
                                newVisitWeight = ""
                                newVisitHeight = ""
                                showLoggerExpanded = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF166534)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Monthly Measurement & Plot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Table Header & Rows
            val sortedVisits = screeningVisits.sortedBy { it.ageMonths }
            val dateFormat = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }

            if (sortedVisits.isEmpty()) {
                Text(
                    text = "No recorded visit measurements yet. Use '+ Add Visit' to plot the first point.",
                    fontSize = 11.5.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    Column(
                        modifier = Modifier
                            .width(520.dp)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        // Table Header
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9)).padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Visit Date", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(65.dp))
                            Text("Age", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(40.dp))
                            Text("Weight (Δ)", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(75.dp))
                            Text("Height (Δ)", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(75.dp))
                            Text("MUAC", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(50.dp))
                            Text("Feeding Stage", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(130.dp))
                            Text("Curve", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(55.dp))
                        }

                        // Table Rows
                        sortedVisits.forEachIndexed { index, visit ->
                            val prevVisit = if (index > 0) sortedVisits[index - 1] else null
                            val weightDelta = if (prevVisit != null && prevVisit.weightKg > 0f && visit.weightKg > 0f) {
                                visit.weightKg - prevVisit.weightKg
                            } else null
                            val heightDelta = if (prevVisit != null && prevVisit.heightCm > 0f && visit.heightCm > 0f) {
                                visit.heightCm - prevVisit.heightCm
                            } else null

                            val curveTrend = when {
                                weightDelta == null -> "Base"
                                weightDelta > 0.05f -> "▲ Good"
                                kotlin.math.abs(weightDelta) <= 0.05f -> "▬ Flat"
                                else -> "▼ Danger"
                            }
                            val trendColor = when {
                                curveTrend.contains("Good") -> Color(0xFF166534)
                                curveTrend.contains("Flat") -> Color(0xFFB45309)
                                curveTrend.contains("Danger") -> Color(0xFFB91C1C)
                                else -> Color.DarkGray
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (index % 2 == 0) Color.White else Color(0xFFFAFAFA))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(dateFormat.format(Date(visit.timestamp)), fontSize = 11.sp, modifier = Modifier.width(65.dp))
                                Text("${visit.ageMonths}m", fontSize = 11.sp, modifier = Modifier.width(40.dp))
                                Text(
                                    "${"%.1f".format(visit.weightKg)}k" + if (weightDelta != null) " (${if (weightDelta >= 0) "+" else ""}${"%.1f".format(weightDelta)})" else "",
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(75.dp)
                                )
                                Text(
                                    "${"%.1f".format(visit.heightCm)}c" + if (heightDelta != null) " (${if (heightDelta >= 0) "+" else ""}${"%.1f".format(heightDelta)})" else "",
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(75.dp)
                                )
                                Text("${"%.1f".format(visit.muac)}", fontSize = 11.sp, modifier = Modifier.width(50.dp))
                                Text(
                                    visit.feedingCategory.ifBlank { if (visit.ageMonths < 6) "Breastfeeding" else "Complementary" },
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    modifier = Modifier.width(130.dp)
                                )
                                Text(
                                    curveTrend,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    color = trendColor,
                                    modifier = Modifier.width(55.dp)
                                )
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (selected) Color.White else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .border(
                if (selected) 1.dp else 0.dp,
                if (selected) Color(0xFFCBD5E1) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color(0xFF0F172A) else Color(0xFF64748B)
        )
    }
}

@Composable
private fun FeedingStageBar(ageMonths: Int) {
    val stageText = when {
        ageMonths < 6 -> "Stage: 0 - 6 Months • Exclusive Breastfeeding ONLY"
        ageMonths in 6..8 -> "Stage: 6 - 8 Months • Weaning: Thick porridge + frequent breastfeeds (2 meals/day)"
        ageMonths in 9..11 -> "Stage: 9 - 11 Months • Complementary Finger Foods + 3 meals & 1 snack/day"
        ageMonths in 12..23 -> "Stage: 1 - 2 Years • Family foods cut small + 5 feeds (3 meals, 2 snacks)"
        else -> "Stage: 2 - 5 Years • Full Family Diet + 2 cups of milk"
    }

    val bannerColor = when {
        ageMonths < 6 -> Color(0xFFEFF6FF)
        ageMonths in 6..8 -> Color(0xFFFFFBEB)
        ageMonths in 9..11 -> Color(0xFFFEF3C7)
        else -> Color(0xFFF0FDF4)
    }
    val contentColor = when {
        ageMonths < 6 -> Color(0xFF1E40AF)
        ageMonths in 6..8 -> Color(0xFF92400E)
        ageMonths in 9..11 -> Color(0xFFB45309)
        else -> Color(0xFF166534)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bannerColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(contentColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stageText,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.5.sp, color = Color(0xFF475569))
    }
}

@Composable
private fun GrowthVelocityAssessment(screeningVisits: List<ScreeningEntity>, selectedMetric: String) {
    val sorted = screeningVisits.sortedBy { it.timestamp }
    if (sorted.size < 2) {
        Text(
            text = "ℹ️ At least 2 monthly visits required to compute growth slope direction (GOOD vs DANGER curve).",
            fontSize = 11.sp,
            color = Color(0xFF64748B)
        )
        return
    }

    val latest = sorted.last()
    val prev = sorted[sorted.size - 2]
    val delta = if (selectedMetric == "WEIGHT") latest.weightKg - prev.weightKg else latest.heightCm - prev.heightCm
    val metricUnit = if (selectedMetric == "WEIGHT") "kg" else "cm"

    val isGood = delta > 0.05f
    val isFlat = kotlin.math.abs(delta) <= 0.05f

    val alertTitle = when {
        isGood -> "GOOD CURVE: Child is gaining steadily (+${"%.2f".format(delta)} $metricUnit)"
        isFlat -> "DANGER: Flat growth curve (0 change). Counsel caregiver immediately on feeding."
        else -> "VERY DANGEROUS: Falling curve (${"%.2f".format(delta)} $metricUnit). Suspect acute illness/infection."
    }

    val containerColor = when {
        isGood -> Color(0xFFDCFCE7)
        isFlat -> Color(0xFFFEF9C3)
        else -> Color(0xFFFEE2E2)
    }
    val textColor = when {
        isGood -> Color(0xFF166534)
        isFlat -> Color(0xFF854D0E)
        else -> Color(0xFF991B1B)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(text = alertTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
            Text(
                text = "Previous: ${if (selectedMetric == "WEIGHT") prev.weightKg else prev.heightCm} $metricUnit (Month ${prev.ageMonths}) ➔ Latest: ${if (selectedMetric == "WEIGHT") latest.weightKg else latest.heightCm} $metricUnit (Month ${latest.ageMonths})",
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.9f)
            )
        }
    }
}
