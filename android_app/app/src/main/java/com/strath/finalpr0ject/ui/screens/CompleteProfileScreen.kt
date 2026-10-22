package com.strath.finalpr0ject.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.strath.finalpr0ject.data.local.AppDatabase
import com.strath.finalpr0ject.data.local.entity.ChildEntity
import com.strath.finalpr0ject.data.model.ChildProfile
import com.strath.finalpr0ject.data.model.UserProfile
import com.strath.finalpr0ject.data.model.UserRole
import com.strath.finalpr0ject.data.repository.FirebaseRepository
import com.strath.finalpr0ject.ml.WHOAnthroCalculator
import com.strath.finalpr0ject.ui.theme.ChildCoral
import com.strath.finalpr0ject.ui.theme.TealPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    initialProfile: UserProfile,
    onProfileCompleted: (UserRole, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { FirebaseRepository() }

    // Multi-step state: 1 = Role, 2 = Photo & Contact, 3 = Role Specific (Child / Facility)
    var currentStep by remember { mutableStateOf(1) }

    // Step 1: Role
    var selectedRole by remember { mutableStateOf(initialProfile.role) }

    // Step 2: Photo & Contact
    val maleAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300"
    val femaleAvatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300"

    var selectedPhotoUrl by remember {
        mutableStateOf(
            if (initialProfile.photoUrl.isNotBlank()) initialProfile.photoUrl else femaleAvatarUrl
        )
    }
    var capturedPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedAvatarType by remember { mutableStateOf(if (initialProfile.photoUrl.contains("150700")) "male" else "female") }

    // Camera Launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedPhotoBitmap = bitmap
            selectedAvatarType = "camera"
            selectedPhotoUrl = "camera_captured"
            Toast.makeText(context, "Profile photo captured successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, "Camera permission is required to take your profile photo.", Toast.LENGTH_LONG).show()
        }
    }

    var displayName by remember { mutableStateOf(initialProfile.displayName.ifEmpty { "" }) }
    var phone by remember { mutableStateOf(initialProfile.phone.ifEmpty { "+254 " }) }
    var county by remember { mutableStateOf(initialProfile.county.ifEmpty { "Turkana" }) }
    var subCounty by remember { mutableStateOf(initialProfile.subCounty.ifEmpty { "Kakuma West" }) }

    // Step 3: Role Specific
    // Guardian Child Details
    var childName by remember { mutableStateOf("") }
    var childAgeMonths by remember { mutableStateOf(18) }
    var childSex by remember { mutableStateOf("Male") }
    var childHeight by remember { mutableStateOf("75.0") }
    var childWeight by remember { mutableStateOf("9.2") }
    var childMuac by remember { mutableStateOf("12.5") }

    // CHW Details
    var chwFacility by remember { mutableStateOf(initialProfile.chwFacility.ifEmpty { "Kakuma Sub-County Hospital" }) }
    var chwCode by remember { mutableStateOf(initialProfile.chwCode.ifEmpty { "CHW-${(1000..9999).random()}" }) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Complete Your Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (selectedRole == UserRole.CHW) TealPrimary else ChildCoral
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAF9))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step Progress Indicator
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepItem(stepNumber = 1, title = "Role", isActive = currentStep == 1, isDone = currentStep > 1)
                    HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = Color(0xFFE0E0E0))
                    StepItem(stepNumber = 2, title = "Profile", isActive = currentStep == 2, isDone = currentStep > 2)
                    HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = Color(0xFFE0E0E0))
                    StepItem(
                        stepNumber = 3,
                        title = if (selectedRole == UserRole.CHW) "Facility" else "Child Details",
                        isActive = currentStep == 3,
                        isDone = false
                    )
                }
            }

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFC62828),
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Step Content
            when (currentStep) {
                1 -> {
                    // ==================== STEP 1: ROLE SELECTION ====================
                    Text(
                        text = "Step 1: Choose Your Account Role",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF263238)
                    )
                    Text(
                        text = "Please specify how you will be using AfyaEarly in Northern Kenya.",
                        fontSize = 12.5.sp,
                        color = Color.Gray
                    )

                    // Option A: Parent / Guardian
                    RoleSelectionOptionCard(
                        title = "I am a Parent / Guardian",
                        description = "Track growth milestones, nutrition indices & interact with the personalized 3D Child body visualizer.",
                        icon = Icons.Default.ChildCare,
                        accentColor = ChildCoral,
                        isSelected = selectedRole == UserRole.GUARDIAN || selectedRole == UserRole.CHILD,
                        onClick = { selectedRole = UserRole.GUARDIAN }
                    )

                    // Option B: CHW
                    RoleSelectionOptionCard(
                        title = "I am a Community Health Worker",
                        description = "Conduct household visits, record MUAC & run on-device AI predictions for pediatric delay screening.",
                        icon = Icons.Default.HealthAndSafety,
                        accentColor = TealPrimary,
                        isSelected = selectedRole == UserRole.CHW,
                        onClick = { selectedRole = UserRole.CHW }
                    )

                    Button(
                        onClick = { currentStep = 2 },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRole == UserRole.CHW) TealPrimary else ChildCoral
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Continue to Profile Details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                2 -> {
                    // ==================== STEP 2: PROFILE PICTURE & CONTACT ====================
                    Text(
                        text = "Step 2: Add Profile Picture & Personal Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF263238)
                    )

                    // Profile Picture Selector
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Profile Picture", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            // Selected Avatar / Captured Photo Preview
                            Box(contentAlignment = Alignment.BottomEnd) {
                                if (selectedAvatarType == "camera" && capturedPhotoBitmap != null) {
                                    Image(
                                        bitmap = capturedPhotoBitmap!!.asImageBitmap(),
                                        contentDescription = "Captured Profile Photo",
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(CircleShape)
                                            .border(
                                                2.5.dp,
                                                if (selectedRole == UserRole.CHW) TealPrimary else ChildCoral,
                                                CircleShape
                                            )
                                    )
                                } else {
                                    AsyncImage(
                                        model = selectedPhotoUrl,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(CircleShape)
                                            .border(
                                                2.5.dp,
                                                if (selectedRole == UserRole.CHW) TealPrimary else ChildCoral,
                                                CircleShape
                                            )
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedRole == UserRole.CHW) TealPrimary else ChildCoral,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = Color.White,
                                        modifier = Modifier.padding(5.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Choose an avatar or capture your photo:",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )

                            // 3 Options: Male Avatar, Female Avatar, Camera Capture
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Male Avatar Option
                                val isMaleSelected = selectedAvatarType == "male"
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedAvatarType = "male"
                                            selectedPhotoUrl = maleAvatarUrl
                                        }
                                        .padding(6.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        AsyncImage(
                                            model = maleAvatarUrl,
                                            contentDescription = "Male Avatar",
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = if (isMaleSelected) 3.dp else 1.dp,
                                                    color = if (isMaleSelected) (if (selectedRole == UserRole.CHW) TealPrimary else ChildCoral) else Color(0xFFCFD8DC),
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Male",
                                        fontSize = 12.sp,
                                        fontWeight = if (isMaleSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isMaleSelected) (if (selectedRole == UserRole.CHW) TealPrimary else ChildCoral) else Color.DarkGray
                                    )
                                }

                                // 2. Female Avatar Option
                                val isFemaleSelected = selectedAvatarType == "female"
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedAvatarType = "female"
                                            selectedPhotoUrl = femaleAvatarUrl
                                        }
                                        .padding(6.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        AsyncImage(
                                            model = femaleAvatarUrl,
                                            contentDescription = "Female Avatar",
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = if (isFemaleSelected) 3.dp else 1.dp,
                                                    color = if (isFemaleSelected) (if (selectedRole == UserRole.CHW) TealPrimary else ChildCoral) else Color(0xFFCFD8DC),
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Female",
                                        fontSize = 12.sp,
                                        fontWeight = if (isFemaleSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isFemaleSelected) (if (selectedRole == UserRole.CHW) TealPrimary else ChildCoral) else Color.DarkGray
                                    )
                                }

                                // 3. Access Camera / Take Photo Option
                                val isCameraSelected = selectedAvatarType == "camera"
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                cameraLauncher.launch()
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        }
                                        .padding(6.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isCameraSelected) (if (selectedRole == UserRole.CHW) TealPrimary else ChildCoral) else Color(0xFFECEFF1),
                                        border = BorderStroke(
                                            width = if (isCameraSelected) 3.dp else 1.dp,
                                            color = if (isCameraSelected) (if (selectedRole == UserRole.CHW) TealPrimary else ChildCoral) else Color(0xFFCFD8DC)
                                        ),
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = "Access Camera",
                                                tint = if (isCameraSelected) Color.White else Color(0xFF455A64),
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Take Photo",
                                        fontSize = 12.sp,
                                        fontWeight = if (isCameraSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCameraSelected) (if (selectedRole == UserRole.CHW) TealPrimary else ChildCoral) else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    // Contact Details Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Your Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Phone Number (for SMS Alerts)") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = county,
                                onValueChange = { county = it },
                                label = { Text("County (e.g. Turkana, Samburu, Wajir)") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = subCounty,
                                onValueChange = { subCounty = it },
                                label = { Text("Sub-County (e.g. Kakuma West, Lodwar)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { currentStep = 1 },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("Back")
                        }

                        Button(
                            onClick = {
                                if (displayName.isBlank() || phone.isBlank()) {
                                    errorMessage = "Please enter your Name and Phone Number."
                                    return@Button
                                }
                                currentStep = 3
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedRole == UserRole.CHW) TealPrimary else ChildCoral
                            ),
                            modifier = Modifier.weight(2f).height(50.dp)
                        ) {
                            Text("Next Step", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                3 -> {
                    // ==================== STEP 3: ROLE SPECIFIC DETAILS ====================
                    if (selectedRole == UserRole.CHW) {
                        Text(
                            text = "Step 3: CHW Health Center Credentials",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF263238)
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                OutlinedTextField(
                                    value = chwFacility,
                                    onValueChange = { chwFacility = it },
                                    label = { Text("Health Facility / Dispensary") },
                                    leadingIcon = { Icon(Icons.Default.LocalHospital, contentDescription = null, tint = TealPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = chwCode,
                                    onValueChange = { chwCode = it },
                                    label = { Text("CHW Staff Code") },
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = TealPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        // GUARDIAN: REGISTER CHILD
                        Text(
                            text = "Step 3: Register Your Child",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF263238)
                        )
                        Text(
                            text = "Connect your child's data to power the interactive 3D growth model.",
                            fontSize = 12.5.sp,
                            color = Color.Gray
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = childName,
                                    onValueChange = { childName = it },
                                    label = { Text("Child's Full Name") },
                                    leadingIcon = { Icon(Icons.Default.Face, contentDescription = null, tint = ChildCoral) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Gender Selection
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Gender:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    listOf("Male", "Female").forEach { sex ->
                                        val isSelected = childSex == sex
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { childSex = sex },
                                            label = { Text(sex) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ChildCoral,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                // Age in Months
                                Column {
                                    Text(
                                        text = "Child Age: $childAgeMonths months",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Slider(
                                        value = childAgeMonths.toFloat(),
                                        onValueChange = { childAgeMonths = it.toInt() },
                                        valueRange = 0f..59f,
                                        steps = 58,
                                        colors = SliderDefaults.colors(thumbColor = ChildCoral, activeTrackColor = ChildCoral)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = childHeight,
                                        onValueChange = { childHeight = it },
                                        label = { Text("Height (cm)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = childWeight,
                                        onValueChange = { childWeight = it },
                                        label = { Text("Weight (kg)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = childMuac,
                                        onValueChange = { childMuac = it },
                                        label = { Text("MUAC (cm)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Live WHO Assessment Preview Card
                                val hFloat = childHeight.toFloatOrNull() ?: 75.0f
                                val wFloat = childWeight.toFloatOrNull() ?: 9.2f
                                val muacFloat = childMuac.toFloatOrNull() ?: 12.5f
                                val liveAnthro = remember(childAgeMonths, childSex, hFloat, wFloat) {
                                    WHOAnthroCalculator.computeAllZScores(childAgeMonths, childSex == "Male", hFloat, wFloat)
                                }

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            liveAnthro.haz < -3.0f || muacFloat < 11.5f -> Color(0xFFFFEBEE)
                                            liveAnthro.haz < -2.0f || muacFloat < 12.5f -> Color(0xFFFFF8E1)
                                            else -> Color(0xFFE8F5E9)
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                liveAnthro.haz < -2.0f -> Icons.Default.Warning
                                                else -> Icons.Default.CheckCircle
                                            },
                                            contentDescription = null,
                                            tint = when {
                                                liveAnthro.haz < -3.0f -> Color(0xFFD32F2F)
                                                liveAnthro.haz < -2.0f -> Color(0xFFF57C00)
                                                else -> Color(0xFF2E7D32)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = when {
                                                    liveAnthro.haz < -3.0f -> "Severe Stunting (HAZ %.1f)".format(liveAnthro.haz)
                                                    liveAnthro.haz < -2.0f -> "Moderate Stunting (HAZ %.1f)".format(liveAnthro.haz)
                                                    else -> "Healthy Growth (HAZ %.1f)".format(liveAnthro.haz)
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = when {
                                                    liveAnthro.haz < -3.0f -> Color(0xFFD32F2F)
                                                    liveAnthro.haz < -2.0f -> Color(0xFFE65100)
                                                    else -> Color(0xFF1B5E20)
                                                }
                                            )
                                            Text(
                                                text = "Based on official WHO growth standards for ${childAgeMonths}m ${childSex.lowercase()}.",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { currentStep = 2 },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("Back")
                        }

                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    val finalUid = initialProfile.uid.ifEmpty { "user-${System.currentTimeMillis() % 10000}" }

                                    if (selectedRole == UserRole.CHW) {
                                        val updatedProfile = initialProfile.copy(
                                            uid = finalUid,
                                            displayName = displayName.ifEmpty { "Community Health Worker" },
                                            phone = phone,
                                            photoUrl = selectedPhotoUrl,
                                            county = county,
                                            subCounty = subCounty,
                                            role = UserRole.CHW,
                                            chwFacility = chwFacility,
                                            isVerified = initialProfile.isVerified,
                                            verificationStatus = initialProfile.verificationStatus,
                                            profileCompleted = true
                                        )
                                        repository.saveUserProfile(updatedProfile)
                                        isLoading = false
                                        onProfileCompleted(UserRole.CHW, chwCode.ifEmpty { "CHW-${(1000..9999).random()}" })
                                    } else {
                                        // Guardian + Child with real computed WHO statistics
                                        val hVal = childHeight.toFloatOrNull() ?: 75.0f
                                        val wVal = childWeight.toFloatOrNull() ?: 9.2f
                                        val mVal = childMuac.toFloatOrNull() ?: 12.5f
                                        val zs = WHOAnthroCalculator.computeAllZScores(childAgeMonths, childSex == "Male", hVal, wVal)
                                        val riskTier = when {
                                            zs.isSeverelyStunted || zs.isSeverelyWasted || mVal < 11.5f -> "HIGH RISK"
                                            zs.isStunted || zs.isWasted || mVal < 12.5f -> "MODERATE RISK"
                                            else -> "LOW RISK"
                                        }

                                        val generatedChildId = "TK-${(1000..9999).random()}"
                                        val childRecord = ChildProfile(
                                            childId = generatedChildId,
                                            name = childName.trim().ifEmpty { "Child of $displayName" },
                                            ageMonths = childAgeMonths,
                                            sex = childSex,
                                            county = county,
                                            assignedChwId = "CHW-1042",
                                            assignedChwName = "Kakuma Community Unit",
                                            maternalEducation = "Primary",
                                            waterSource = "Protected Borehole",
                                            householdSize = 5,
                                            currentHaz = zs.haz,
                                            currentWhz = zs.whz,
                                            currentMuac = mVal,
                                            latestRiskTier = riskTier,
                                            latestProbability = if (riskTier.contains("HIGH")) 0.88f else if (riskTier.contains("MODERATE")) 0.50f else 0.12f,
                                            motorMilestoneScore = if (zs.haz < -2.0f) 72 else 88,
                                            cognitiveMilestoneScore = if (zs.haz < -2.0f) 70 else 85,
                                            languageMilestoneScore = if (zs.haz < -2.0f) 68 else 82,
                                            lastScreeningDate = System.currentTimeMillis()
                                        )

                                        // 1. Save child to local Room database for 100% offline access
                                        try {
                                            val childEntity = ChildEntity.fromDomainModel(childRecord)
                                            AppDatabase.getDatabase(context).childDao().insertOrUpdate(childEntity)
                                        } catch (_: Exception) {}

                                        // 2. Update Guardian profile with the new child ID
                                        val updatedProfile = initialProfile.copy(
                                            uid = finalUid,
                                            displayName = displayName.ifEmpty { "Parent / Guardian" },
                                            phone = phone,
                                            photoUrl = selectedPhotoUrl,
                                            county = county,
                                            subCounty = subCounty,
                                            role = UserRole.GUARDIAN,
                                            isVerified = true,
                                            verificationStatus = "approved",
                                            assignedChildId = generatedChildId,
                                            assignedChildName = childRecord.name,
                                            profileCompleted = true
                                        )

                                        repository.saveUserProfile(updatedProfile)
                                        repository.linkGuardianChild(finalUid, childRecord)
                                        isLoading = false
                                        onProfileCompleted(UserRole.GUARDIAN, generatedChildId)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedRole == UserRole.CHW) TealPrimary else ChildCoral
                            ),
                            modifier = Modifier.weight(2f).height(50.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("Complete & Launch Dashboard", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepItem(stepNumber: Int, title: String, isActive: Boolean, isDone: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = when {
                isDone -> Color(0xFF2E7D32)
                isActive -> Color(0xFF00675B)
                else -> Color(0xFFB0BEC5)
            },
            modifier = Modifier.size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isDone) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                } else {
                    Text(stepNumber.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) Color(0xFF00675B) else Color.DarkGray
        )
    }
}

@Composable
fun RoleSelectionOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) accentColor else Color(0xFFE0E0E0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) accentColor else accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                Text(description, fontSize = 11.5.sp, color = Color.Gray, lineHeight = 15.sp)
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
            )
        }
    }
}
