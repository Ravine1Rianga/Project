package com.strath.finalpr0ject.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strath.finalpr0ject.data.model.UserProfile
import com.strath.finalpr0ject.data.model.UserRole
import com.strath.finalpr0ject.data.repository.FirebaseRepository
import com.strath.finalpr0ject.ui.theme.CalmGreenPrimary
import com.strath.finalpr0ject.ui.theme.ParentCoral
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    initialRoleKey: String = "chw",
    repository: FirebaseRepository = remember { FirebaseRepository() },
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onNavigateToPendingApproval: (user: UserProfile) -> Unit,
    onParentRegistered: (user: UserProfile) -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Active role selection: Strictly either CHW or PARENT
    var selectedRole by remember {
        mutableStateOf(if (initialRoleKey.equals("parent", ignoreCase = true)) UserRole.PARENT else UserRole.CHW)
    }

    // Common fields
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneDigits by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // CHW Extras
    var county by remember { mutableStateOf("Turkana") }
    var subCounty by remember { mutableStateOf("") }
    var chwId by remember { mutableStateOf("") }
    var supervisingFacility by remember { mutableStateOf("") }

    // Parent Extras
    var numChildren by remember { mutableStateOf(1) }

    // Consent
    var consentAccepted by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // State management
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var countyDropdownExpanded by remember { mutableStateOf(false) }

    val asalCounties = listOf(
        "Turkana", "Garissa", "Mandera", "Wajir", "Marsabit", "Samburu",
        "West Pokot", "Isiolo", "Tana River", "Baringo", "Nairobi"
    )

    // Password strength evaluation
    val passwordStrength = remember(password) {
        when {
            password.isEmpty() -> ""
            password.length < 6 -> "Weak (min 6 characters)"
            password.length < 10 || !password.any { it.isDigit() } -> "Moderate"
            else -> "Strong"
        }
    }
    val strengthColor = remember(passwordStrength) {
        when {
            passwordStrength.startsWith("Weak") -> Color(0xFFD32F2F)
            passwordStrength == "Moderate" -> Color(0xFFF57C00)
            passwordStrength == "Strong" -> CalmGreenPrimary
            else -> Color.Transparent
        }
    }

    val activeColor = if (selectedRole == UserRole.CHW) CalmGreenPrimary else ParentCoral

    fun validate(): Boolean {
        errorMessage = null
        if (fullName.trim().isEmpty()) {
            errorMessage = "Please enter your full name"
            return false
        }
        if (email.trim().isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            errorMessage = "Please enter a valid email address"
            return false
        }
        if (phoneDigits.trim().length < 8) {
            errorMessage = "Please enter a valid phone number (+254)"
            return false
        }
        if (password.length < 6) {
            errorMessage = "Password must be at least 6 characters"
            return false
        }
        if (password != confirmPassword) {
            errorMessage = "Passwords do not match"
            return false
        }
        if (selectedRole == UserRole.CHW) {
            if (subCounty.trim().isEmpty()) {
                errorMessage = "Please enter your sub-county or ward"
                return false
            }
            if (chwId.trim().isEmpty()) {
                errorMessage = "Please enter your CHW ID or Community Health Unit"
                return false
            }
            if (supervisingFacility.trim().isEmpty()) {
                errorMessage = "Please enter your supervising health facility"
                return false
            }
        }
        if (!consentAccepted) {
            errorMessage = "Please accept the health data privacy consent to proceed"
            return false
        }
        return true
    }

    fun handleSignUp() {
        if (!validate()) return
        scope.launch {
            isLoading = true
            val fullPhone = "+254${phoneDigits.trim().removePrefix("0").removePrefix("+254")}"

            val result = repository.registerWithEmail(
                email = email.trim(),
                pass = password,
                displayName = fullName.trim(),
                role = selectedRole,
                phone = fullPhone,
                county = county,
                subCounty = subCounty.trim(),
                ward = subCounty.trim(),
                chwId = chwId.trim(),
                supervisingFacility = supervisingFacility.trim(),
                numChildren = numChildren,
                consentGiven = true
            )
            isLoading = false

            result.onSuccess { profile ->
                if (selectedRole == UserRole.CHW) {
                    onNavigateToPendingApproval(profile)
                } else {
                    onParentRegistered(profile)
                }
            }.onFailure { ex ->
                errorMessage = when {
                    ex.message?.contains("Configuration not found", ignoreCase = true) == true ||
                    ex.message?.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) == true ->
                        "Firebase Auth is not enabled in your Firebase Console! Please open the Firebase Console for 'finalproject-eefdc', go to Build > Authentication > Sign-in method, and Enable 'Email/Password'."
                    ex.message?.contains("email-already-in-use", ignoreCase = true) == true ->
                        "This email is associated with a family account. You can register your profile or sign in with your family password."
                    else -> ex.localizedMessage ?: "Account registration failed. Please try again."
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = activeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = "Create Account",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Join AfyaEarly Care in tracking child health",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // ROLE SELECTOR TAB (CHW vs PARENT ONLY)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                // Health Worker Tab
                Surface(
                    onClick = { selectedRole = UserRole.CHW },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedRole == UserRole.CHW) CalmGreenPrimary else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = if (selectedRole == UserRole.CHW) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Health Worker",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedRole == UserRole.CHW) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Parent Tab
                Surface(
                    onClick = { selectedRole = UserRole.PARENT },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedRole == UserRole.PARENT) ParentCoral else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = if (selectedRole == UserRole.PARENT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Parent/Guardian",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedRole == UserRole.PARENT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // COMMON FIELDS
            // ==========================================
            Text("Full Name", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = { Text("e.g. Maria Akiru") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("Email Address", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("e.g. name@example.com") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("Phone Number (+254)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = phoneDigits,
                onValueChange = { phoneDigits = it.filter { char -> char.isDigit() } },
                prefix = { Text("+254 ", fontWeight = FontWeight.Bold) },
                placeholder = { Text("712 345 678") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // County Selection (Both roles)
            Text("County", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = countyDropdownExpanded,
                onExpandedChange = { countyDropdownExpanded = !countyDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = county,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countyDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = countyDropdownExpanded,
                    onDismissRequest = { countyDropdownExpanded = false }
                ) {
                    asalCounties.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                county = item
                                countyDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // ROLE EXTRAS
            // ==========================================
            if (selectedRole == UserRole.CHW) {
                // CHW Extras
                Text("Sub-County / Ward", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = subCounty,
                    onValueChange = { subCounty = it },
                    placeholder = { Text("e.g. Turkana West / Kakuma") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("CHW ID or Community Health Unit (CHU)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = chwId,
                    onValueChange = { chwId = it },
                    placeholder = { Text("e.g. CHU-TK-104 or Staff ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Supervising Health Facility", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = supervisingFacility,
                    onValueChange = { supervisingFacility = it },
                    placeholder = { Text("e.g. Lodwar County Referral Hospital") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            } else {
                // Parent Extras
                Text("Number of Children to Track", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3, 4).forEach { count ->
                        Surface(
                            onClick = { numChildren = count },
                            shape = RoundedCornerShape(10.dp),
                            color = if (numChildren == count) ParentCoral else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (count == 4) "4+" else count.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (numChildren == count) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Specific child health measurements & 3D milestones can be registered after sign-up.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            Text("Create Password", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("••••••••") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Password strength hint
            if (passwordStrength.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Strength: $passwordStrength",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = strengthColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Confirm Password Field
            Text("Confirm Password", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = { Text("••••••••") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // MANDATORY CONSENT CHECKBOX (Kenya DPA 2019)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = consentAccepted,
                    onCheckedChange = { consentAccepted = it },
                    colors = CheckboxDefaults.colors(checkedColor = activeColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "I consent to the collection and secure processing of pediatric health information in accordance with the Kenya Data Protection Act (2019).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "View Privacy Policy & Clinical Data Protocol",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeColor,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { showPrivacyDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = { handleSignUp() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = activeColor)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                } else {
                    Text(
                        text = if (selectedRole == UserRole.CHW) "Register Health Worker Account" else "Register Guardian Account",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Already have account
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sign in",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeColor,
                    modifier = Modifier.clickable { onNavigateToSignIn() }
                )
            }

            // Error display
            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("AfyaEarly Care Privacy Policy") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Data Protection & Clinical Privacy Guarantee\n\n" +
                                "1. Compliance: AfyaEarly Care adheres strictly to the Kenya Data Protection Act (2019) and Ministry of Health digital health guidelines.\n\n" +
                                "2. Purpose: Health indicators (MUAC, age, anthropometry) are used exclusively for early malnutrition detection, developmental milestone tracking, and clinical triage.\n\n" +
                                "3. Security: All health data is encrypted in transit and at rest in Cloud Firestore. CHWs access records scoped only to their assigned health catchment unit.\n\n" +
                                "4. Guardianship: Parents and guardians retain full rights to review and request deletion of their child's health profile.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        consentAccepted = true
                        showPrivacyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                ) {
                    Text("I Understand & Accept")
                }
            }
        )
    }
}
