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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.strath.finalpr0ject.ui.theme.ParentCoral
import kotlinx.coroutines.launch

@Composable
fun WelcomeAuthScreen(
    repository: FirebaseRepository = remember { FirebaseRepository() },
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onNavigateToSignIn: () -> Unit,
    onNavigateToSignUp: (roleKey: String) -> Unit,
    onNavigateToCompleteProfile: (user: UserProfile) -> Unit,
    onAuthenticated: (user: UserProfile) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Real Native Google Sign-In configuration
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    var showGoogleConfigDialog by remember { mutableStateOf(false) }

    fun processGoogleAccount(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        val userEmail = account.email.orEmpty().trim()
        val userName = account.displayName.orEmpty().ifEmpty { "User" }
        val photo = account.photoUrl?.toString().orEmpty()
        val idToken = account.idToken

        scope.launch {
            isLoading = true
            errorMessage = null

            // Check if this Google account is the Admin
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
                onAuthenticated(adminProfile)
                return@launch
            }

            // Authenticate via Firebase Google Credential if ID token available
            if (!idToken.isNullOrEmpty()) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authRes = repository.signInWithGoogleCredential(credential)
                isLoading = false
                authRes.onSuccess { profile ->
                    if (!profile.profileCompleted) {
                        onNavigateToCompleteProfile(profile)
                    } else {
                        onAuthenticated(profile)
                    }
                }.onFailure { ex ->
                    errorMessage = ex.localizedMessage ?: "Google Sign-In failed"
                }
            } else {
                // Standard Google account selected on device
                val existingProfile = repository.getCurrentUserProfile().getOrNull()
                if (existingProfile != null && existingProfile.profileCompleted) {
                    isLoading = false
                    onAuthenticated(existingProfile)
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
                onAuthenticated(adminProfile)
                return@launch
            }

            val existingProfile = repository.getCurrentUserProfile().getOrNull()
            if (existingProfile != null && existingProfile.email.equals(targetEmail, ignoreCase = true) && existingProfile.profileCompleted) {
                isLoading = false
                Toast.makeText(context, "Welcome back, ${existingProfile.displayName}!", Toast.LENGTH_SHORT).show()
                onAuthenticated(existingProfile)
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
                onNavigateToCompleteProfile(newProfile)
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Theme Toggle Button in top corner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // HEADER BANNER (Exact mockup design)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CalmGreenPrimary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Shield Icon with Soft Background
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = "AfyaEarly Shield",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "AfyaEarly Care",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Child health, tracked early",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ==========================================
            // GET STARTED SECTION
            // ==========================================
            Text(
                text = "Get started",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // Continue with Google Button
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
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = CalmGreenPrimary
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Google "G" Badge
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
                            text = "Continue with Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider: "or choose how you use the app"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Text(
                    text = "  or choose how you use the app  ",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // EXACT TWO ROLE CARDS ONLY (NO ADMIN)
            // ==========================================

            // 1. Health Worker Card
            RoleCard(
                icon = Icons.Default.MedicalServices,
                iconTint = CalmGreenPrimary,
                iconBackground = CalmGreenPrimary.copy(alpha = 0.12f),
                title = "Health worker",
                description = "Screen and triage",
                onClick = { onNavigateToSignUp("chw") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Parent or Guardian Card
            RoleCard(
                icon = Icons.Default.Favorite,
                iconTint = ParentCoral,
                iconBackground = ParentCoral.copy(alpha = 0.15f),
                title = "Parent or guardian",
                description = "Follow your child",
                onClick = { onNavigateToSignUp("parent") }
            )

            Spacer(modifier = Modifier.height(30.dp))

            // ==========================================
            // FOOTER: "Have an account? Sign in"
            // ==========================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Have an account? ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sign in",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CalmGreenPrimary,
                    modifier = Modifier.clickable { onNavigateToSignIn() }
                )
            }

            // Inline Error Banner
            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
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
        var googleEmailInput by remember { mutableStateOf("") }

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
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
