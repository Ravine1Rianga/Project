package com.strath.finalpr0ject.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.strath.finalpr0ject.data.model.UserProfile
import com.strath.finalpr0ject.data.model.UserRole
import com.strath.finalpr0ject.data.repository.FirebaseRepository
import com.strath.finalpr0ject.ui.theme.AdminIndigo
import com.strath.finalpr0ject.ui.theme.CalmGreenPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    repository: FirebaseRepository = remember { FirebaseRepository() },
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToCompleteProfile: (user: UserProfile) -> Unit,
    onNavigateToPendingApproval: (user: UserProfile) -> Unit,
    onRoleResolved: (role: UserRole, user: UserProfile) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Native Google Sign In setup
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    var showGoogleConfigDialog by remember { mutableStateOf(false) }
    var multiProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var showProfileChooser by remember { mutableStateOf(false) }

    fun processGoogleAccount(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        val userEmail = account.email.orEmpty().trim()
        val userName = account.displayName.orEmpty().ifEmpty { "User" }
        val photo = account.photoUrl?.toString().orEmpty()
        val idToken = account.idToken

        scope.launch {
            isLoading = true
            errorMessage = null

            // If admin signs in with Google
            if (userEmail.equals("riangaravine@gmail.com", ignoreCase = true) || userEmail.equals(FirebaseRepository.ADMIN_EMAIL, ignoreCase = true)) {
                val adminProfile = UserProfile(
                    uid = "admin-${account.id ?: "rianga"}",
                    email = userEmail,
                    displayName = "Ravine Riang'a (Admin)",
                    photoUrl = photo,
                    role = UserRole.ADMIN,
                    isVerified = true,
                    verificationStatus = "approved",
                    profileCompleted = true
                )
                repository.saveUserProfile(adminProfile)
                isLoading = false
                onRoleResolved(UserRole.ADMIN, adminProfile)
                return@launch
            }

            if (!idToken.isNullOrEmpty()) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = repository.signInWithGoogleCredential(credential)
                isLoading = false
                authResult.onSuccess { profile ->
                    routeUser(profile, onRoleResolved, onNavigateToPendingApproval, onNavigateToCompleteProfile)
                }.onFailure { ex ->
                    errorMessage = ex.localizedMessage ?: "Google sign in failed"
                }
            } else {
                val existingProfile = repository.getCurrentUserProfile().getOrNull()
                if (existingProfile != null && existingProfile.profileCompleted) {
                    isLoading = false
                    routeUser(existingProfile, onRoleResolved, onNavigateToPendingApproval, onNavigateToCompleteProfile)
                } else {
                    val newProfile = UserProfile(
                        uid = "google-${account.id ?: System.currentTimeMillis()}",
                        email = userEmail,
                        displayName = userName,
                        photoUrl = photo,
                        role = UserRole.PARENT,
                        profileCompleted = false
                    )
                    repository.saveUserProfile(newProfile)
                    isLoading = false
                    onNavigateToCompleteProfile(newProfile)
                }
            }
        }
    }

    fun handleDirectGoogleSignIn(targetEmail: String, targetRole: UserRole) {
        scope.launch {
            isLoading = true
            errorMessage = null

            if (targetEmail.equals("riangaravine@gmail.com", ignoreCase = true) || targetRole == UserRole.ADMIN) {
                val adminProfile = UserProfile(
                    uid = "admin-${targetEmail.hashCode()}",
                    email = targetEmail,
                    displayName = "Ravine Riang'a (Admin)",
                    role = UserRole.ADMIN,
                    isVerified = true,
                    verificationStatus = "approved",
                    profileCompleted = true
                )
                repository.saveUserProfile(adminProfile)
                isLoading = false
                Toast.makeText(context, "Signed in as Admin", Toast.LENGTH_SHORT).show()
                onRoleResolved(UserRole.ADMIN, adminProfile)
                return@launch
            }

            val existingProfile = repository.getCurrentUserProfile().getOrNull()
            if (existingProfile != null && existingProfile.email.equals(targetEmail, ignoreCase = true) && existingProfile.profileCompleted) {
                isLoading = false
                Toast.makeText(context, "Welcome back, ${existingProfile.displayName}!", Toast.LENGTH_SHORT).show()
                routeUser(existingProfile, onRoleResolved, onNavigateToPendingApproval, onNavigateToCompleteProfile)
            } else {
                val newProfile = UserProfile(
                    uid = "google-${System.currentTimeMillis()}",
                    email = targetEmail,
                    displayName = targetEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() },
                    role = targetRole,
                    isVerified = (targetRole != UserRole.CHW),
                    verificationStatus = if (targetRole == UserRole.CHW) "pending" else "approved",
                    profileCompleted = false
                )
                repository.saveUserProfile(newProfile)
                isLoading = false
                if (targetRole == UserRole.CHW) {
                    onNavigateToPendingApproval(newProfile)
                } else {
                    onNavigateToCompleteProfile(newProfile)
                }
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    processGoogleAccount(account)
                    return@rememberLauncherForActivityResult
                }
            } catch (e: ApiException) {
                if (e.statusCode == 12501 && result.resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(context, "Google sign-in cancelled", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }
                showGoogleConfigDialog = true
                return@rememberLauncherForActivityResult
            } catch (e: Exception) {
                showGoogleConfigDialog = true
                return@rememberLauncherForActivityResult
            }
        }

        if (result.resultCode != Activity.RESULT_OK) {
            showGoogleConfigDialog = true
        }
    }

    fun validateInputs(): Boolean {
        var isValid = true
        emailError = null
        passwordError = null
        errorMessage = null

        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            emailError = "Please enter your email address"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            emailError = "Please enter a valid email format (e.g. name@email.com)"
            isValid = false
        }

        if (password.isEmpty()) {
            passwordError = "Please enter your password"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }

    fun handleSignIn(customEmail: String? = null, customPass: String? = null) {
        val targetEmail = customEmail ?: email
        val targetPass = customPass ?: password

        if (customEmail == null && !validateInputs()) return
        scope.launch {
            isLoading = true
            errorMessage = null
            val result = repository.signInWithEmail(targetEmail, targetPass)
            isLoading = false
            result.onSuccess { profile ->
                val familyProfiles = repository.getProfilesForEmail(targetEmail).getOrDefault(emptyList())
                if (familyProfiles.size > 1) {
                    multiProfiles = familyProfiles
                    showProfileChooser = true
                } else {
                    routeUser(profile, onRoleResolved, onNavigateToPendingApproval, onNavigateToCompleteProfile)
                }
            }.onFailure { ex ->
                errorMessage = when {
                    ex.message?.contains("Configuration not found", ignoreCase = true) == true ||
                    ex.message?.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) == true ->
                        "Firebase Auth is not enabled in your Firebase Console! Please open the Firebase Console for 'finalproject-eefdc', go to Build > Authentication > Sign-in method, and Enable 'Email/Password'."
                    ex.message?.contains("user-not-found", ignoreCase = true) == true ->
                        "No account found with this email. Please check or register."
                    ex.message?.contains("wrong-password", ignoreCase = true) == true ||
                    ex.message?.contains("invalid-credential", ignoreCase = true) == true ||
                    ex.message?.contains("incorrect", ignoreCase = true) == true ||
                    ex.message?.contains("malformed", ignoreCase = true) == true ->
                        "Incorrect email or password. Please check your credentials or tap 'Forgot password'."
                    else -> ex.localizedMessage ?: "Sign-in failed. Please check your network and credentials."
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
            // Top Navigation & Theme Toggle Bar
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title & Subtitle
            Text(
                text = "Welcome back",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Sign in to continue",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email Field
            Text(
                text = "Email",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null
                },
                placeholder = { Text("name@email.com", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                isError = emailError != null,
                supportingText = {
                    emailError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password Field
            Text(
                text = "Password",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                },
                placeholder = { Text("••••••••", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                isError = passwordError != null,
                supportingText = {
                    passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        handleSignIn()
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Forgot Password Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Forgot password",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = CalmGreenPrimary,
                    modifier = Modifier.clickable { onNavigateToForgotPassword() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary "Sign in" Button
            Button(
                onClick = { handleSignIn() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalmGreenPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Sign in",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // "or" Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Text(
                    text = "  or  ",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Real Google Button
            OutlinedButton(
                onClick = {
                    showGoogleConfigDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = CalmGreenPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Link: "New here? Create account"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New here? ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Create account",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CalmGreenPrimary,
                    modifier = Modifier.clickable { onNavigateToSignUp() }
                )
            }

            // General Error Message
            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
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

    // ================================================================
    // GOOGLE SIGN-IN SETUP & DIRECT ACCESS DIALOG
    // ================================================================
    if (showGoogleConfigDialog) {
        val clipboardManager = LocalClipboardManager.current
        var selectedGoogleRole by remember { mutableStateOf(UserRole.PARENT) }
        var googleEmailInput by remember { mutableStateOf(if (email.isNotEmpty()) email else "") }

        AlertDialog(
            onDismissRequest = { showGoogleConfigDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CalmGreenPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Google Sign-In",
                        tint = CalmGreenPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Google Account Sign-In",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Enter your Google account email to sign in directly:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = googleEmailInput,
                        onValueChange = { googleEmailInput = it },
                        label = { Text("Google Email") },
                        placeholder = { Text("e.g. riangaravine@gmail.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = CalmGreenPrimary)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val trimmed = googleEmailInput.trim()
                    val isAdminDetected = trimmed.equals("riangaravine@gmail.com", ignoreCase = true) ||
                                          trimmed.equals(FirebaseRepository.ADMIN_EMAIL, ignoreCase = true)

                    if (isAdminDetected) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AdminIndigo.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, AdminIndigo.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = AdminIndigo,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Admin account detected. Will sign in with administrative privileges.",
                                    fontSize = 12.sp,
                                    color = AdminIndigo,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Select account role:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedGoogleRole == UserRole.CHW,
                                onClick = { selectedGoogleRole = UserRole.CHW },
                                label = { Text("Health Worker", fontSize = 12.sp) },
                                leadingIcon = if (selectedGoogleRole == UserRole.CHW) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedGoogleRole == UserRole.PARENT,
                                onClick = { selectedGoogleRole = UserRole.PARENT },
                                label = { Text("Parent / Guardian", fontSize = 12.sp) },
                                leadingIcon = if (selectedGoogleRole == UserRole.PARENT) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val inputEmail = googleEmailInput.trim()
                        if (inputEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(inputEmail).matches()) {
                            Toast.makeText(context, "Please enter a valid Google email", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showGoogleConfigDialog = false
                        handleDirectGoogleSignIn(
                            inputEmail,
                            if (inputEmail.equals("riangaravine@gmail.com", ignoreCase = true)) UserRole.ADMIN else selectedGoogleRole
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CalmGreenPrimary)
                ) {
                    Text("Continue with Google", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showProfileChooser) {
        AlertDialog(
            onDismissRequest = { showProfileChooser = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = CalmGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Account Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Multiple family profiles are registered under $email. Who is logging in?",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    multiProfiles.forEach { p ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showProfileChooser = false
                                    routeUser(p, onRoleResolved, onNavigateToPendingApproval, onNavigateToCompleteProfile)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (p.role == UserRole.CHW) CalmGreenPrimary else Color(0xFFE57373),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            p.displayName.ifEmpty { "User" }.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(p.displayName.ifEmpty { "Family Member" }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        "${p.role} • ${if (p.assignedChildName.isNotBlank()) "Child: ${p.assignedChildName}" else p.county}",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileChooser = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Server-decided role-based routing
 */
private fun routeUser(
    profile: UserProfile,
    onRoleResolved: (UserRole, UserProfile) -> Unit,
    onNavigateToPendingApproval: (UserProfile) -> Unit,
    onNavigateToCompleteProfile: (UserProfile) -> Unit
) {
    // If CHW is approved by admin, route directly to CHW Dashboard! Do NOT redirect to complete profile or pending approval
    if (profile.role == UserRole.CHW) {
        val isApproved = profile.isVerified || profile.verificationStatus.equals("approved", ignoreCase = true)
        if (isApproved) {
            onRoleResolved(UserRole.CHW, profile)
            return
        }
    }

    if (!profile.profileCompleted && profile.role != UserRole.ADMIN) {
        onNavigateToCompleteProfile(profile)
        return
    }

    when (profile.role) {
        UserRole.ADMIN -> {
            onRoleResolved(UserRole.ADMIN, profile)
        }
        UserRole.CHW -> {
            val isApproved = profile.isVerified || profile.verificationStatus.equals("approved", ignoreCase = true)
            if (isApproved) {
                onRoleResolved(UserRole.CHW, profile)
            } else {
                onNavigateToPendingApproval(profile)
            }
        }
        UserRole.PARENT, UserRole.GUARDIAN, UserRole.CHILD -> {
            onRoleResolved(UserRole.PARENT, profile)
        }
    }
}
