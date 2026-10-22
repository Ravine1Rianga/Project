package com.strath.finalpr0ject.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strath.finalpr0ject.ui.theme.*

/**
 * Anatomical Consequence Hotspot representing a key body system affected by pediatric stunting.
 */
data class ConsequenceHotspot(
    val id: String,
    val title: String,
    val iconName: String,
    val normalizedY: Float, // 0.0 top of head to 1.0 bottom of feet
    val normalizedX: Float,
    val organ: String,
    val currentStatus: String,
    val consequenceDescription: String,
    val developmentalImpact: String,
    val recommendedIntervention: String,
    val severityColor: Color
)

enum class GrowthVisualMode {
    CURRENT_STUNTED,
    HEALTHY_BENCHMARK,
    CATCH_UP_PROJECTION
}

@Composable
fun ChildBodyVisualizer(
    childName: String = "Akiru",
    ageMonths: Int = 18,
    heightCm: Float = 74.0f,
    expectedHeightCm: Float = 82.0f,
    muacCm: Float = 11.2f,
    hazScore: Float = -2.4f,
    modifier: Modifier = Modifier
) {
    var visualMode by remember { mutableStateOf(GrowthVisualMode.CURRENT_STUNTED) }
    var selectedHotspot by remember { mutableStateOf<ConsequenceHotspot?>(null) }

    val hotspots = remember(hazScore, muacCm) {
        listOf(
            ConsequenceHotspot(
                id = "brain",
                title = "Cognitive & Neural Development",
                iconName = "Psychology",
                normalizedY = 0.16f,
                normalizedX = 0.50f,
                organ = "Brain & Central Nervous System",
                currentStatus = "High Risk of Synaptic & Cognitive Delay (HAZ $hazScore)",
                consequenceDescription = "Prolonged stunting reduces neural synapse density by up to 25% during the First 1,000 Days. Without dietary zinc and iron, neural connectivity between motor and language centers slows.",
                developmentalImpact = "Delayed speech milestones, shortened attention span, and difficulty with exploratory play and problem-solving.",
                recommendedIntervention = "Add zinc-rich foods (camel milk, mashed boiled egg, fortified porridge) + daily interactive storytelling and responsive play.",
                severityColor = RiskHighRed
            ),
            ConsequenceHotspot(
                id = "muac",
                title = "Upper Arm & Muscular Tone (MUAC)",
                iconName = "FitnessCenter",
                normalizedY = 0.38f,
                normalizedX = 0.28f,
                organ = "Musculoskeletal Mass & Arm Circumference",
                currentStatus = "MUAC: $muacCm cm (Severe Acute Malnutrition Alert)",
                consequenceDescription = "The red band (<11.5 cm) indicates rapid depletion of skeletal muscle and fat stores. The body is breaking down muscle tissue to supply essential amino acids to vital organs.",
                developmentalImpact = "Generalized weakness, weak grip strength, delayed walking, and quick physical exhaustion during active play.",
                recommendedIntervention = "Immediate therapeutic intake: RUTF (Plumpy'Nut) ration + high-protein mashed pulses/dairy under CHW guidance.",
                severityColor = RiskHighRed
            ),
            ConsequenceHotspot(
                id = "immune",
                title = "Vitality & Immune System",
                iconName = "Shield",
                normalizedY = 0.44f,
                normalizedX = 0.50f,
                organ = "Thymus, Lymphatic & Gut Immune Barrier",
                currentStatus = "Vulnerable to Recurrent Infections",
                consequenceDescription = "Malnutrition causes mucosal thinning in the gut and low immunoglobulin levels. Infections like diarrhoea and acute respiratory tract illnesses occur 3x more frequently.",
                developmentalImpact = "Frequent sick days trigger secondary weight loss, further deepening linear stunting into a vicious cycle.",
                recommendedIntervention = "Vitamin A supplementation, clean treated drinking water, and continued zinc oral rehydration therapy during sickness.",
                severityColor = RiskModerateAmber
            ),
            ConsequenceHotspot(
                id = "stature",
                title = "Skeletal Frame & Linear Height",
                iconName = "Straighten",
                normalizedY = 0.75f,
                normalizedX = 0.50f,
                organ = "Long Bones & Epiphyseal Growth Plates",
                currentStatus = "Height Deficit: ${(expectedHeightCm - heightCm).toInt()} cm Below WHO Standard",
                consequenceDescription = "Linear stunting occurs when bone elongation stops due to chronic protein-energy deficit. If not corrected before age 2, stature reduction becomes biologically irreversible.",
                developmentalImpact = "Permanent reduction in adult height, reduced physical work capacity, and increased lifetime metabolic vulnerability.",
                recommendedIntervention = "Catch-up growth window active: 6 months left in First 1000 Days. Catch-up rate of +1.8 cm/month is achievable with diverse feeding.",
                severityColor = RiskHighRed
            )
        )
    }

    val animatedHeightScale by animateFloatAsState(
        targetValue = when (visualMode) {
            GrowthVisualMode.CURRENT_STUNTED -> 0.82f
            GrowthVisualMode.HEALTHY_BENCHMARK -> 1.0f
            GrowthVisualMode.CATCH_UP_PROJECTION -> 0.94f
        },
        animationSpec = tween(durationMillis = 600),
        label = "heightScale"
    )

    val modeColor by animateColorAsState(
        targetValue = when (visualMode) {
            GrowthVisualMode.CURRENT_STUNTED -> RiskHighRed
            GrowthVisualMode.HEALTHY_BENCHMARK -> RiskLowGreen
            GrowthVisualMode.CATCH_UP_PROJECTION -> Color(0xFF0288D1)
        },
        animationSpec = tween(durationMillis = 400),
        label = "modeColor"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1412)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Child Identity & Consequence Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = modeColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (visualMode == GrowthVisualMode.HEALTHY_BENCHMARK) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = modeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$childName's Growth Consequence Visualizer",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Age: $ageMonths months • Actual: ${heightCm}cm (WHO Target: ${expectedHeightCm}cm)",
                        fontSize = 11.sp,
                        color = Color.LightGray.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 36.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = modeColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, modeColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = when (visualMode) {
                            GrowthVisualMode.CURRENT_STUNTED -> "HAZ $hazScore"
                            GrowthVisualMode.HEALTHY_BENCHMARK -> "HAZ 0.0"
                            GrowthVisualMode.CATCH_UP_PROJECTION -> "HAZ -0.8"
                        },
                        color = modeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Visual Mode Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF162320), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GrowthVisualMode.values().forEach { mode ->
                    val isSelected = visualMode == mode
                    val tabColor = when (mode) {
                        GrowthVisualMode.CURRENT_STUNTED -> RiskHighRed
                        GrowthVisualMode.HEALTHY_BENCHMARK -> RiskLowGreen
                        GrowthVisualMode.CATCH_UP_PROJECTION -> Color(0xFF0288D1)
                    }
                    Surface(
                        onClick = { visualMode = mode },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) tabColor.copy(alpha = 0.25f) else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, tabColor) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = when (mode) {
                                GrowthVisualMode.CURRENT_STUNTED -> "Stunted (Now)"
                                GrowthVisualMode.HEALTHY_BENCHMARK -> "Normal Target"
                                GrowthVisualMode.CATCH_UP_PROJECTION -> "Catch-Up"
                            },
                            color = if (isSelected) Color.White else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Anatomical Visualizer Stage with Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF13221E), Color(0xFF0B1210))
                        ),
                        RoundedCornerShape(18.dp)
                    )
                    .border(1.dp, Color(0xFF1F352E), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Background Height Benchmark Measurement Grid
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // Draw expected height benchmark line (top)
                    val expectedY = canvasHeight * 0.10f
                    drawLine(
                        color = Color(0xFF2E7D32).copy(alpha = 0.5f),
                        start = Offset(24.dp.toPx(), expectedY),
                        end = Offset(canvasWidth - 24.dp.toPx(), expectedY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )

                    // Draw actual stunted height line
                    val actualY = canvasHeight * 0.10f + (canvasHeight * 0.75f * (1f - animatedHeightScale))
                    if (visualMode != GrowthVisualMode.HEALTHY_BENCHMARK) {
                        drawLine(
                            color = RiskHighRed.copy(alpha = 0.7f),
                            start = Offset(24.dp.toPx(), actualY),
                            end = Offset(canvasWidth - 24.dp.toPx(), actualY),
                            strokeWidth = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                    }
                }

                // Measurement Scale Labels Overlay
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp, top = 22.dp, bottom = 22.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = RiskLowGreen.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = "Target: ${expectedHeightCm.toInt()}cm",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    if (visualMode != GrowthVisualMode.HEALTHY_BENCHMARK) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = RiskHighRed.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = "Actual: ${heightCm.toInt()}cm (-${(expectedHeightCm - heightCm).toInt()}cm)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Floor 0cm",
                            fontSize = 9.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Child Body Silhouette Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val groundY = h * 0.88f
                    val totalHeight = (groundY - (h * 0.10f)) * animatedHeightScale
                    val topY = groundY - totalHeight

                    // Anatomical Proportions
                    val headRadius = totalHeight * 0.13f
                    val headCenterY = topY + headRadius
                    val neckHeight = totalHeight * 0.04f
                    val torsoTop = headCenterY + headRadius + neckHeight
                    val torsoHeight = totalHeight * 0.34f
                    val pelvisY = torsoTop + torsoHeight
                    val legHeight = groundY - pelvisY

                    // Body Widths based on nutrition mode (stunting causes wasting & thinner limbs)
                    val widthFactor = when (visualMode) {
                        GrowthVisualMode.CURRENT_STUNTED -> 0.75f
                        GrowthVisualMode.HEALTHY_BENCHMARK -> 1.05f
                        GrowthVisualMode.CATCH_UP_PROJECTION -> 0.92f
                    }

                    val torsoWidth = (totalHeight * 0.22f) * widthFactor
                    val armThickness = (totalHeight * 0.065f) * widthFactor
                    val legThickness = (totalHeight * 0.085f) * widthFactor

                    val bodyColor = when (visualMode) {
                        GrowthVisualMode.CURRENT_STUNTED -> Color(0xFFC7786B)
                        GrowthVisualMode.HEALTHY_BENCHMARK -> Color(0xFF5B9E83)
                        GrowthVisualMode.CATCH_UP_PROJECTION -> Color(0xFF5694B8)
                    }

                    val glowColor = bodyColor.copy(alpha = 0.25f)

                    // 1. Soft Silhouette Glow
                    drawCircle(
                        color = glowColor,
                        radius = headRadius * 1.35f,
                        center = Offset(cx, headCenterY)
                    )

                    // 2. Head
                    drawCircle(
                        color = bodyColor,
                        radius = headRadius,
                        center = Offset(cx, headCenterY)
                    )

                    // Face features: eyes & mouth contour
                    val eyeY = headCenterY - (headRadius * 0.1f)
                    val eyeSpacing = headRadius * 0.35f
                    drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 3.5f, center = Offset(cx - eyeSpacing, eyeY))
                    drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 3.5f, center = Offset(cx + eyeSpacing, eyeY))

                    // 3. Neck
                    drawRect(
                        color = bodyColor,
                        topLeft = Offset(cx - (headRadius * 0.25f), headCenterY + headRadius),
                        size = Size(headRadius * 0.5f, neckHeight)
                    )

                    // 4. Torso (Rounded Chest & Abdomen)
                    val torsoPath = Path().apply {
                        moveTo(cx - (torsoWidth / 2f), torsoTop)
                        lineTo(cx + (torsoWidth / 2f), torsoTop)
                        // In severe stunting/kwashiorkor, abdomen can appear distended or hollow
                        val bellyBulge = if (visualMode == GrowthVisualMode.CURRENT_STUNTED) 4f else 8f
                        cubicTo(
                            cx + (torsoWidth / 2f) + bellyBulge, torsoTop + (torsoHeight * 0.6f),
                            cx + (torsoWidth / 2f) - 2f, pelvisY,
                            cx, pelvisY
                        )
                        cubicTo(
                            cx - (torsoWidth / 2f) + 2f, pelvisY,
                            cx - (torsoWidth / 2f) - bellyBulge, torsoTop + (torsoHeight * 0.6f),
                            cx - (torsoWidth / 2f), torsoTop
                        )
                        close()
                    }
                    drawPath(torsoPath, bodyColor)

                    // 5. Arms
                    // Left Arm (Viewer's left)
                    val armStartX = cx - (torsoWidth / 2f)
                    val armStartY = torsoTop + 6.dp.toPx()
                    val armLength = torsoHeight * 0.95f
                    val armEndX = armStartX - (totalHeight * 0.11f)
                    val armEndY = armStartY + armLength

                    drawLine(
                        color = bodyColor,
                        start = Offset(armStartX, armStartY),
                        end = Offset(armEndX, armEndY),
                        strokeWidth = armThickness,
                        cap = StrokeCap.Round
                    )

                    // MUAC Band on Left Upper Arm (Real Clinical Metric)
                    val muacFraction = 0.35f
                    val muacBandX = armStartX + (armEndX - armStartX) * muacFraction
                    val muacBandY = armStartY + (armEndY - armStartY) * muacFraction
                    val muacBandColor = when {
                        muacCm < 11.5f -> RiskHighRed
                        muacCm < 12.5f -> RiskModerateAmber
                        else -> RiskLowGreen
                    }
                    drawCircle(
                        color = muacBandColor,
                        radius = armThickness * 0.85f,
                        center = Offset(muacBandX, muacBandY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = armThickness * 0.35f,
                        center = Offset(muacBandX, muacBandY)
                    )

                    // Right Arm (Viewer's right)
                    val rArmStartX = cx + (torsoWidth / 2f)
                    val rArmStartY = torsoTop + 6.dp.toPx()
                    val rArmEndX = rArmStartX + (totalHeight * 0.11f)
                    val rArmEndY = rArmStartY + armLength
                    drawLine(
                        color = bodyColor,
                        start = Offset(rArmStartX, rArmStartY),
                        end = Offset(rArmEndX, rArmEndY),
                        strokeWidth = armThickness,
                        cap = StrokeCap.Round
                    )

                    // 6. Legs
                    val hipSpacing = torsoWidth * 0.28f
                    // Left Leg
                    drawLine(
                        color = bodyColor,
                        start = Offset(cx - hipSpacing, pelvisY),
                        end = Offset(cx - hipSpacing - 6.dp.toPx(), groundY),
                        strokeWidth = legThickness,
                        cap = StrokeCap.Round
                    )
                    // Right Leg
                    drawLine(
                        color = bodyColor,
                        start = Offset(cx + hipSpacing, pelvisY),
                        end = Offset(cx + hipSpacing + 6.dp.toPx(), groundY),
                        strokeWidth = legThickness,
                        cap = StrokeCap.Round
                    )

                    // Feet
                    drawOval(
                        color = bodyColor,
                        topLeft = Offset(cx - hipSpacing - 18.dp.toPx(), groundY - 4.dp.toPx()),
                        size = Size(18.dp.toPx(), 9.dp.toPx())
                    )
                    drawOval(
                        color = bodyColor,
                        topLeft = Offset(cx + hipSpacing, groundY - 4.dp.toPx()),
                        size = Size(18.dp.toPx(), 9.dp.toPx())
                    )
                }

                // Hotspot Interactive Badges Overlay (Brain, MUAC, Immune, Stature)
                hotspots.forEach { hotspot ->
                    val yFraction = hotspot.normalizedY
                    val xFraction = hotspot.normalizedX

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Surface(
                            onClick = { selectedHotspot = hotspot },
                            shape = CircleShape,
                            color = hotspot.severityColor.copy(alpha = 0.9f),
                            border = BorderStroke(2.dp, Color.White),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .align(
                                    when (hotspot.id) {
                                        "brain" -> Alignment.TopCenter
                                        "muac" -> Alignment.CenterStart
                                        "immune" -> Alignment.Center
                                        "stature" -> Alignment.BottomCenter
                                        else -> Alignment.Center
                                    }
                                )
                                .padding(
                                    start = if (hotspot.id == "muac") 32.dp else 0.dp,
                                    bottom = if (hotspot.id == "stature") 44.dp else 0.dp,
                                    top = if (hotspot.id == "brain") 28.dp else 0.dp
                                )
                                .size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (hotspot.id) {
                                        "brain" -> Icons.Default.Psychology
                                        "muac" -> Icons.Default.FitnessCenter
                                        "immune" -> Icons.Default.Shield
                                        else -> Icons.Default.Straighten
                                    },
                                    contentDescription = hotspot.title,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Help hint badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color(0xFF73F8E7), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tap body badges for consequences", fontSize = 10.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Golden Window & Growth Consequence Alert Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = when (visualMode) {
                    GrowthVisualMode.CURRENT_STUNTED -> RiskHighContainer
                    GrowthVisualMode.HEALTHY_BENCHMARK -> RiskLowContainer
                    GrowthVisualMode.CATCH_UP_PROJECTION -> Color(0xFFE1F5FE)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (visualMode) {
                                GrowthVisualMode.CURRENT_STUNTED -> Icons.Default.ReportProblem
                                GrowthVisualMode.HEALTHY_BENCHMARK -> Icons.Default.Verified
                                GrowthVisualMode.CATCH_UP_PROJECTION -> Icons.Default.TrendingUp
                            },
                            contentDescription = null,
                            tint = modeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (visualMode) {
                                GrowthVisualMode.CURRENT_STUNTED -> "Growth Consequence: Severe Stunting (HAZ $hazScore)"
                                GrowthVisualMode.HEALTHY_BENCHMARK -> "Optimal Growth Consequence: Thriving Potential"
                                GrowthVisualMode.CATCH_UP_PROJECTION -> "Catch-Up Potential: First 1,000 Days Window"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = modeColor
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (visualMode) {
                            GrowthVisualMode.CURRENT_STUNTED ->
                                "Without immediate zinc, protein, and micro-nutrient intervention, the ${(expectedHeightCm - heightCm).toInt()}cm deficit will permanently reduce Akiru's adult height and cognitive school readiness. Only 6 months remain in the critical 2-year window!"
                            GrowthVisualMode.HEALTHY_BENCHMARK ->
                                "At 82cm and normal MUAC (>12.5cm), child exhibits full neural connectivity, robust immune resilience, and is expected to walk, run, and learn age-appropriate vocabulary on schedule."
                            GrowthVisualMode.CATCH_UP_PROJECTION ->
                                "With 4 daily food groups (including camel milk & eggs) plus CHW-administered zinc supplementation, Akiru can recover up to +1.8cm in height and regain +1.3cm on arm circumference within 90 days."
                        },
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    // Detail Dialog for Tapped Consequence Hotspot
    selectedHotspot?.let { spot ->
        AlertDialog(
            onDismissRequest = { selectedHotspot = null },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = spot.severityColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (spot.id) {
                                "brain" -> Icons.Default.Psychology
                                "muac" -> Icons.Default.FitnessCenter
                                "immune" -> Icons.Default.Shield
                                else -> Icons.Default.Straighten
                            },
                            contentDescription = null,
                            tint = spot.severityColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = spot.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = spot.severityColor.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, spot.severityColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = spot.currentStatus,
                            fontWeight = FontWeight.Bold,
                            color = spot.severityColor,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Text(
                        text = "Clinical Consequence:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = spot.consequenceDescription,
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "Developmental Milestone Impact:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = spot.developmentalImpact,
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        lineHeight = 18.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TealPrimaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Healing, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("What You Can Do (Parent Action):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TealOnPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = spot.recommendedIntervention,
                                fontSize = 12.sp,
                                color = TealOnPrimaryContainer,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedHotspot = null },
                    colors = ButtonDefaults.buttonColors(containerColor = spot.severityColor)
                ) {
                    Text("Got It")
                }
            }
        )
    }
}
