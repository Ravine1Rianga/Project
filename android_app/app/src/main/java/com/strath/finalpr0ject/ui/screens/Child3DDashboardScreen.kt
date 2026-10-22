package com.strath.finalpr0ject.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.strath.finalpr0ject.data.repository.FirebaseRepository
import com.strath.finalpr0ject.ml.WHOAnthroCalculator
import com.strath.finalpr0ject.ui.components.ChildBodyVisualizer
import com.strath.finalpr0ject.ui.theme.*
import kotlinx.coroutines.launch

data class LoggedMeal(
    val id: String,
    val name: String,
    val time: String,
    val category: String,
    val icon: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Child3DDashboardScreen(
    childId: String,
    userProfile: UserProfile? = null,
    onNavigateToAfyaBot: (role: String, childId: String) -> Unit = { _, _ -> },
    onNavigateToUSSD: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val repository = remember { FirebaseRepository() }

    // Language Toggle State: False = English, True = Kiswahili
    var isSwahili by remember { mutableStateOf(false) }

    // Dynamic Child Profile State
    var childName by remember { mutableStateOf(userProfile?.assignedChildName?.ifEmpty { "Akiru Ewoi" } ?: "Akiru Ewoi") }
    var ageMonths by remember { mutableStateOf(18) }
    var isMale by remember { mutableStateOf(true) }
    var heightCm by remember { mutableStateOf(74.0f) }
    var weightKg by remember { mutableStateOf(9.2f) }
    var muacCm by remember { mutableStateOf(11.2f) }
    var hazScore by remember { mutableStateOf(-2.4f) }
    var assignedChwName by remember { mutableStateOf("Maria Akiru") }
    var assignedChwPhone by remember { mutableStateOf("+254712345678") }
    var county by remember { mutableStateOf(userProfile?.county ?: "Turkana") }
    var facility by remember { mutableStateOf("Kakuma West Sub-County Hospital") }

    // Parent Profile State & Edit Dialog
    var parentName by remember { mutableStateOf(userProfile?.displayName?.ifEmpty { "Parent" } ?: "Parent") }
    var parentPhone by remember { mutableStateOf(userProfile?.phone ?: "") }
    var parentSubCounty by remember { mutableStateOf(userProfile?.subCounty ?: "Turkana West") }
    var showParentProfileDialog by remember { mutableStateOf(false) }

    // Compute expected height from WHO standard
    val expectedHeightCm = remember(ageMonths, isMale) {
        // Approximate WHO median for age
        when {
            ageMonths <= 6 -> 67.6f
            ageMonths <= 12 -> 75.7f
            ageMonths <= 18 -> 82.3f
            ageMonths <= 24 -> 87.8f
            ageMonths <= 36 -> 96.1f
            else -> 103.0f
        }
    }

    // Modal dialog to update measurements & growth
    var showUpdateMeasurementsDialog by remember { mutableStateOf(false) }

    // Load child dynamically from Room database or Firestore
    LaunchedEffect(childId) {
        try {
            val dbChild = AppDatabase.getDatabase(context).childDao().getChildById(childId)
            if (dbChild != null) {
                childName = dbChild.name
                ageMonths = dbChild.ageMonths
                isMale = dbChild.sex.equals("Male", ignoreCase = true)
                muacCm = dbChild.currentMuac
                hazScore = dbChild.currentHaz
                if (dbChild.assignedChwName.isNotBlank()) assignedChwName = dbChild.assignedChwName
                if (dbChild.county.isNotBlank()) county = dbChild.county
                // If height not explicitly stored, derive from HAZ
                heightCm = (expectedHeightCm + (dbChild.currentHaz * 3.2f)).coerceIn(45f, 120f)
            } else {
                val remoteResult = repository.getChildProfile(childId)
                remoteResult.onSuccess { child ->
                    if (child != null) {
                        childName = child.name
                        ageMonths = child.ageMonths
                        isMale = child.sex.equals("Male", ignoreCase = true)
                        muacCm = child.currentMuac
                        hazScore = child.currentHaz
                        if (child.assignedChwName.isNotBlank()) assignedChwName = child.assignedChwName
                        heightCm = (expectedHeightCm + (child.currentHaz * 3.2f)).coerceIn(45f, 120f)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // Meal Logging State (Simplified for Mothers)
    var customMealText by remember { mutableStateOf("") }
    var loggedMeals by remember {
        mutableStateOf(
            listOf(
                LoggedMeal("1", "Maziwa Safi (Fresh Milk)", "Asubuhi", "Dairy", "🥛"),
                LoggedMeal("2", "Uji wa Mtama (Sorghum Porridge)", "Mchana", "Grains", "🥣"),
                LoggedMeal("3", "Yai na Karoti (Egg & Carrots)", "Jioni", "Eggs", "🥚")
            )
        )
    }

    val quickFoodOptions = if (isSwahili) {
        listOf(
            Triple("Maziwa Safi", "Dairy", "🥛"),
            Triple("Uji Bora", "Grains", "🥣"),
            Triple("Yai", "Eggs", "🥚"),
            Triple("Mboga za Kijani", "Veggies", "🥬"),
            Triple("Matunda", "Fruits", "🍌"),
            Triple("Plumpy'Nut", "Therapeutic", "🥜")
        )
    } else {
        listOf(
            Triple("Fresh Milk", "Dairy", "🥛"),
            Triple("Enriched Porridge", "Grains", "🥣"),
            Triple("Boiled Egg", "Eggs", "🥚"),
            Triple("Local Greens", "Veggies", "🥬"),
            Triple("Fruits", "Fruits", "🍌"),
            Triple("Plumpy'Nut (RUTF)", "Therapeutic", "🥜")
        )
    }

    // Compute Dietary Diversity count (target 4)
    val distinctCategoriesCount = remember(loggedMeals) {
        loggedMeals.map { it.category }.distinct().size.coerceIn(1, 6)
    }

    // Simple Traffic-Light Maternal Status
    val isSevere = hazScore < -3.0f || muacCm < 11.5f
    val isModerate = (hazScore in -3.0f..-2.0f) || (muacCm in 11.5f..12.5f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ParentCoral.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ChildCare, contentDescription = null, tint = ParentCoral)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isSwahili) "Ukuaji wa $childName" else "$childName's Growth",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (isSwahili) "Miezi $ageMonths • Kaunti ya $county" else "$ageMonths months • $county County",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                actions = {
                    // Language Switcher Segmented Pill [ EN | SW ]
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (!isSwahili) CalmGreenPrimary else Color.Transparent,
                                modifier = Modifier.clickable { isSwahili = false }
                            ) {
                                Text(
                                    "EN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (!isSwahili) Color.White else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSwahili) CalmGreenPrimary else Color.Transparent,
                                modifier = Modifier.clickable { isSwahili = true }
                            ) {
                                Text(
                                    "SW",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isSwahili) Color.White else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Parent Profile View & Edit
                    IconButton(onClick = { showParentProfileDialog = true }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Parent Profile", tint = ParentCoral)
                    }

                    IconButton(onClick = onNavigateToUSSD) {
                        Icon(Icons.Default.PhoneIphone, contentDescription = "2G USSD Alerts", tint = CalmGreenPrimary)
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
                onClick = { onNavigateToAfyaBot("parent", childId) },
                icon = { Icon(Icons.Default.SmartToy, contentDescription = null) },
                text = { Text(if (isSwahili) "Uliza AfyaBot (Msaada)" else "Ask AfyaBot (Help)", fontWeight = FontWeight.Bold) },
                containerColor = CalmGreenPrimary,
                contentColor = Color.White
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
            // 1. CLEAR, MATERNAL-FRIENDLY GROWTH STATUS CARD (TRAFFIC LIGHT)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isSevere -> Color(0xFFFFEBEE)
                        isModerate -> Color(0xFFFFF8E1)
                        else -> Color(0xFFE8F5E9)
                    }
                ),
                border = BorderStroke(
                    1.5.dp,
                    when {
                        isSevere -> RiskHighRed
                        isModerate -> RiskModerateAmber
                        else -> RiskLowGreen
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when {
                            isSevere -> RiskHighRed
                            isModerate -> RiskModerateAmber
                            else -> RiskLowGreen
                        },
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    isSevere -> Icons.Default.Warning
                                    isModerate -> Icons.Default.Info
                                    else -> Icons.Default.Check
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isSevere -> if (isSwahili) "🔴 Anahitaji Huduma ya Hospitali" else "🔴 Needs Urgent Clinic Care"
                                isModerate -> if (isSwahili) "🟡 Anahitaji Lishe Zaidi na Matunzo" else "🟡 Needs Extra Good Food & Care"
                                else -> if (isSwahili) "🟢 Mtoto Anakua Vizuri na Yuko Salama" else "🟢 Growing Well & Healthy"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = when {
                                isSevere -> RiskHighRed
                                isModerate -> Color(0xFFE65100)
                                else -> Color(0xFF1B5E20)
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                isSevere -> if (isSwahili) "Urefu na mkono wa mtoto uko chini sana. Mpe RUTF na mpeleke hospitali mara moja." else "Height and arm measurements are severely low. Provide RUTF and visit the clinic immediately."
                                isModerate -> if (isSwahili) "Mtoto ana upungufu wa urefu wa ${(expectedHeightCm - heightCm).toInt().coerceAtLeast(1)} cm. Mpe vyakula vya nguvu, mayai, na maziwa." else "Child has a height deficit of ${(expectedHeightCm - heightCm).toInt().coerceAtLeast(1)} cm. Provide energy-dense foods, eggs, and milk."
                                else -> if (isSwahili) "Urefu na uzito wa mtoto unafuata viwango bora vya WHO. Endelea kumpa lishe bora!" else "Height and weight match healthy WHO standards. Continue providing balanced local foods!"
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF37474F),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 2. VISUAL MEASUREMENT STRIP WITH UPDATE ACTION
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSwahili) "Vipimo vya Mtoto (Ukuaji)" else "Child Measurements (Growth)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        TextButton(onClick = { showUpdateMeasurementsDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isSwahili) "Sasisha Ukuaji" else "Update Growth", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Height
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isSwahili) "📏 Urefu Sasa" else "📏 Current Height", fontSize = 11.sp, color = Color.Gray)
                            Text("${heightCm.toInt()} cm", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF263238))
                            Text(if (isSwahili) "Unaotarajiwa: ${expectedHeightCm.toInt()}cm" else "Expected: ${expectedHeightCm.toInt()}cm", fontSize = 10.sp, color = Color.Gray)
                        }

                        // MUAC
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isSwahili) "💪 Mkono (MUAC)" else "💪 Arm (MUAC)", fontSize = 11.sp, color = Color.Gray)
                            Text("${"%.1f".format(muacCm)} cm", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                                color = if (muacCm < 11.5f) RiskHighRed else if (muacCm < 12.5f) RiskModerateAmber else RiskLowGreen)
                            Text(if (muacCm < 11.5f) (if (isSwahili) "Mkali (SAM)" else "Severe (SAM)") else if (muacCm < 12.5f) (if (isSwahili) "Wastani (MAM)" else "Moderate (MAM)") else (if (isSwahili) "Salama" else "Healthy"), fontSize = 10.sp, color = Color.Gray)
                        }

                        // Age
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isSwahili) "🎂 Umri wa Mtoto" else "🎂 Child Age", fontSize = 11.sp, color = Color.Gray)
                            Text("$ageMonths m", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF263238))
                            Text(if (isSwahili) "Miezi" else "Months", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // 3. FULL-BODY VISUALIZER
            ChildBodyVisualizer(
                childName = childName,
                ageMonths = ageMonths,
                heightCm = heightCm,
                expectedHeightCm = expectedHeightCm,
                muacCm = muacCm,
                hazScore = hazScore
            )

            // 4. FIRST 1,000 DAYS GOLDEN WINDOW (SIMPLIFIED FOR MOTHERS)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF213129)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE57373).copy(alpha = 0.25f),
                        border = BorderStroke(1.5.dp, RiskHighRed),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${(24 - ageMonths).coerceAtLeast(0)}m",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = if (isSwahili) "Dirisha Muhimu: Siku 1,000 za Kwanza" else "Critical Window: First 1,000 Days",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSwahili)
                                "Kumpa mtoto lishe bora kabla hajatimiza miaka 2 humsaidia kujenga ubongo wenye akili na kuzuia kudumaa maishani."
                            else
                                "Providing quality nutrition before age 2 builds a sharp brain and prevents lifelong stunting.",
                            fontSize = 11.5.sp,
                            color = Color(0xFFECEFF1),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 5. SIMPLIFIED MATERNAL MEAL LOGGER (1-TAP QUICK FOODS)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSwahili) "Chakula cha Leo" else "Today's Meals",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (distinctCategoriesCount >= 4) RiskLowContainer else RiskModerateContainer
                        ) {
                            Text(
                                text = if (isSwahili) "$distinctCategoriesCount / 4 Makundi" else "$distinctCategoriesCount / 4 Food Groups",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (distinctCategoriesCount >= 4) RiskLowGreen else RiskModerateAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isSwahili) "Gusa chakula ulichompa $childName leo:" else "Tap food given to $childName today:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    // 1-Tap Quick Food Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickFoodOptions) { (label, category, icon) ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val newMeal = LoggedMeal(
                                        id = System.currentTimeMillis().toString(),
                                        name = label,
                                        time = if (isSwahili) "Sasa hivi" else "Just now",
                                        category = category,
                                        icon = icon
                                    )
                                    loggedMeals = loggedMeals + newMeal
                                    Toast.makeText(
                                        context,
                                        if (isSwahili) "Umerekodi $icon $label!" else "Recorded $icon $label!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                label = { Text("$icon $label", fontSize = 11.sp) }
                            )
                        }
                    }

                    // Optional Text Input for Local Meal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customMealText,
                            onValueChange = { customMealText = it },
                            placeholder = { Text(if (isSwahili) "Andika chakula kingine..." else "Type another food item...", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                if (customMealText.isNotBlank()) {
                                    val newMeal = LoggedMeal(
                                        id = System.currentTimeMillis().toString(),
                                        name = customMealText.trim(),
                                        time = if (isSwahili) "Leo" else "Today",
                                        category = "Family Food",
                                        icon = "🥣"
                                    )
                                    loggedMeals = loggedMeals + newMeal
                                    customMealText = ""
                                    Toast.makeText(
                                        context,
                                        if (isSwahili) "Chakula kimerekodiwa!" else "Meal recorded!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CalmGreenPrimary)
                        ) {
                            Text(if (isSwahili) "Weka" else "Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Meal List Display
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        loggedMeals.takeLast(4).forEach { meal ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF5F5F5),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${meal.icon} ${meal.name}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    IconButton(
                                        onClick = { loggedMeals = loggedMeals.filter { it.id != meal.id } },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Simple Feedback Box
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CalmGreenContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CalmGreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSwahili) {
                                    if (distinctCategoriesCount >= 4) {
                                        "🌟 Hongera mama! Mtoto amekula makundi 4 ya vyakula bora leo."
                                    } else {
                                        "Ongeza yai au maziwa ya ngamia ili kufikisha makundi 4 ya lishe."
                                    }
                                } else {
                                    if (distinctCategoriesCount >= 4) {
                                        "🌟 Great job mama! Your child has eaten 4 nutrient-rich food groups today."
                                    } else {
                                        "Add an egg, milk, or local greens to reach 4 food groups."
                                    }
                                },
                                fontSize = 11.5.sp,
                                color = CalmGreenOnContainer
                            )
                        }
                    }
                }
            }

            // 6. HEALTH WORKER & CLINIC ASSISTANCE CARD
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isSwahili) "Afisa wa Afya Kijijini (CHW)" else "Community Health Worker (CHW)", fontSize = 11.sp, color = Color.Gray)
                        Text(assignedChwName, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        Text(if (isSwahili) "Kituo: $facility" else "Facility: $facility", fontSize = 11.sp, color = CalmGreenPrimary)
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$assignedChwPhone")
                            }
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CalmGreenPrimary)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSwahili) "Piga Simu" else "Call CHW", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Modal 1: Update Child Growth & Measurements (Continuous Growth Over Weeks/Months)
    if (showUpdateMeasurementsDialog) {
        var editName by remember { mutableStateOf(childName) }
        var editAge by remember { mutableStateOf(ageMonths.toString()) }
        var editIsMale by remember { mutableStateOf(isMale) }
        var editHeight by remember { mutableStateOf(heightCm.toString()) }
        var editWeight by remember { mutableStateOf(weightKg.toString()) }
        var editMuac by remember { mutableStateOf(muacCm.toString()) }

        AlertDialog(
            onDismissRequest = { showUpdateMeasurementsDialog = false },
            title = {
                Text(
                    text = if (isSwahili) "Sasisha Vipimo na Ukuaji wa Mtoto" else "Update Child Growth & Measurements",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (isSwahili)
                            "Weka jina la mtoto na vipimo vipya kadiri anavyokua kwa wiki na miezi:"
                        else
                            "Update the child's name, age, and new measurements as they grow over time:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(if (isSwahili) "Jina la Mtoto" else "Child's Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editAge,
                            onValueChange = { editAge = it },
                            label = { Text(if (isSwahili) "Umri (Miezi)" else "Age (Months)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        // Boy / Girl Selector
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSwahili) "Jinsia" else "Gender",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = editIsMale,
                                    onClick = { editIsMale = true },
                                    label = { Text(if (isSwahili) "Mvulana" else "Boy", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = !editIsMale,
                                    onClick = { editIsMale = false },
                                    label = { Text(if (isSwahili) "Msichana" else "Girl", fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editHeight,
                        onValueChange = { editHeight = it },
                        label = { Text(if (isSwahili) "Urefu mpya (cm)" else "Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editWeight,
                        onValueChange = { editWeight = it },
                        label = { Text(if (isSwahili) "Uzito mpya (kg)" else "Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editMuac,
                        onValueChange = { editMuac = it },
                        label = { Text(if (isSwahili) "Kipimo cha Mkono - MUAC (cm)" else "Arm Circumference - MUAC (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedName = editName.trim().ifEmpty { childName }
                        val parsedAge = editAge.toIntOrNull() ?: ageMonths
                        val h = editHeight.toFloatOrNull() ?: heightCm
                        val w = editWeight.toFloatOrNull() ?: weightKg
                        val m = editMuac.toFloatOrNull() ?: muacCm
                        val zs = WHOAnthroCalculator.computeAllZScores(parsedAge, editIsMale, h, w)

                        childName = parsedName
                        ageMonths = parsedAge
                        isMale = editIsMale
                        heightCm = h
                        weightKg = w
                        muacCm = m
                        hazScore = zs.haz

                        // Persist to Room & Firestore
                        scope.launch {
                            val updatedChild = ChildProfile(
                                childId = childId,
                                name = parsedName,
                                ageMonths = parsedAge,
                                sex = if (editIsMale) "Male" else "Female",
                                county = county,
                                assignedChwId = "CHW-1042",
                                assignedChwName = assignedChwName,
                                currentHaz = zs.haz,
                                currentWhz = zs.whz,
                                currentMuac = m,
                                latestRiskTier = if (zs.isSeverelyStunted) "HIGH RISK" else if (zs.isStunted) "MODERATE RISK" else "LOW RISK",
                                lastScreeningDate = System.currentTimeMillis()
                            )
                            try {
                                AppDatabase.getDatabase(context).childDao().insertOrUpdate(ChildEntity.fromDomainModel(updatedChild))
                                repository.updateChildProfile(updatedChild)
                            } catch (_: Exception) {}
                        }

                        Toast.makeText(
                            context,
                            if (isSwahili) "Vipimo na ukuaji wa $parsedName vimesasishwa!" else "Growth & measurements for $parsedName updated!",
                            Toast.LENGTH_SHORT
                        ).show()
                        showUpdateMeasurementsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CalmGreenPrimary)
                ) {
                    Text(if (isSwahili) "Hifadhi (Save)" else "Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateMeasurementsDialog = false }) {
                    Text(if (isSwahili) "Funga" else "Close")
                }
            }
        )
    }

    // Modal 2: View and Edit Parent / Guardian Profile
    if (showParentProfileDialog) {
        var editParentName by remember { mutableStateOf(parentName) }
        var editParentPhone by remember { mutableStateOf(parentPhone) }
        var editParentSubCounty by remember { mutableStateOf(parentSubCounty) }
        var editChildNameLink by remember { mutableStateOf(childName) }

        AlertDialog(
            onDismissRequest = { showParentProfileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = ParentCoral)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSwahili) "Wasifu wa Mzazi / Mlezi" else "Parent / Guardian Profile",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (isSwahili)
                            "Angalia na usasishe maelezo yako ya mawasiliano na jina la mtoto:"
                        else
                            "View and edit your personal contact information and child's name:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = editParentName,
                        onValueChange = { editParentName = it },
                        label = { Text(if (isSwahili) "Jina Lako Kamili" else "Your Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = userProfile?.email?.ifEmpty { "turkana.family@gmail.com" } ?: "turkana.family@gmail.com",
                        onValueChange = {},
                        enabled = false,
                        label = { Text(if (isSwahili) "Barua Pepe (Household Email)" else "Household Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editParentPhone,
                        onValueChange = { editParentPhone = it },
                        label = { Text(if (isSwahili) "Nambari ya Simu (Kwa Alerts za USSD)" else "Phone Number (For USSD Alerts)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editParentSubCounty,
                        onValueChange = { editParentSubCounty = it },
                        label = { Text(if (isSwahili) "Kaunti Ndogo (Sub-County)" else "Sub-County") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editChildNameLink,
                        onValueChange = { editChildNameLink = it },
                        label = { Text(if (isSwahili) "Jina la Mtoto Uliyemsajili" else "Registered Child's Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        parentName = editParentName.trim()
                        parentPhone = editParentPhone.trim()
                        parentSubCounty = editParentSubCounty.trim()
                        if (editChildNameLink.isNotBlank()) {
                            childName = editChildNameLink.trim()
                        }

                        scope.launch {
                            try {
                                val currentUid = userProfile?.uid ?: ""
                                if (currentUid.isNotBlank()) {
                                    val updatedProf = (userProfile ?: UserProfile()).copy(
                                        displayName = parentName,
                                        phone = parentPhone,
                                        subCounty = parentSubCounty,
                                        assignedChildName = childName
                                    )
                                    repository.updateUserProfile(updatedProf)
                                }
                                // Also update child if childName changed
                                val dbChild = AppDatabase.getDatabase(context).childDao().getChildById(childId)
                                if (dbChild != null) {
                                    val updatedChild = dbChild.toDomainModel().copy(name = childName)
                                    AppDatabase.getDatabase(context).childDao().insertOrUpdate(ChildEntity.fromDomainModel(updatedChild))
                                    repository.updateChildProfile(updatedChild)
                                }
                            } catch (_: Exception) {}
                        }

                        Toast.makeText(
                            context,
                            if (isSwahili) "Wasifu umesasishwa kikamilifu!" else "Parent profile saved successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        showParentProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CalmGreenPrimary)
                ) {
                    Text(if (isSwahili) "Hifadhi Wasifu" else "Save Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showParentProfileDialog = false }) {
                    Text(if (isSwahili) "Funga" else "Close")
                }
            }
        )
    }
}
