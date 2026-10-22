package com.strath.finalpr0ject.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.strath.finalpr0ject.data.model.ChildProfile
import com.strath.finalpr0ject.data.model.UserProfile
import com.strath.finalpr0ject.data.model.UserRole
import com.strath.finalpr0ject.data.repository.FirebaseRepository
import com.strath.finalpr0ject.ml.InferenceOutput
import com.strath.finalpr0ject.ml.ScreeningInput
import com.strath.finalpr0ject.ui.screens.*

@Composable
fun AfyaNavGraph(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    val navController = rememberNavController()
    val repository = remember { FirebaseRepository() }

    var currentUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var pendingProfileForCompletion by remember { mutableStateOf<UserProfile?>(null) }
    var activeChildForScreening by remember { mutableStateOf<ChildProfile?>(null) }
    var lastScreeningInput by remember { mutableStateOf<ScreeningInput?>(null) }
    var lastInferenceResult by remember { mutableStateOf<InferenceOutput?>(null) }

    // Check existing auth state on app launch
    LaunchedEffect(Unit) {
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            val profileRes = repository.getCurrentUserProfile()
            profileRes.onSuccess { profile ->
                if (profile != null) {
                    currentUserProfile = profile
                    if (!profile.profileCompleted && profile.role != UserRole.ADMIN) {
                        pendingProfileForCompletion = profile
                        navController.navigate("complete_profile") {
                            popUpTo("welcome_auth") { inclusive = true }
                        }
                    } else {
                        when (profile.role) {
                            UserRole.ADMIN -> {
                                navController.navigate("admin_dashboard") {
                                    popUpTo("welcome_auth") { inclusive = true }
                                }
                            }
                            UserRole.CHW -> {
                                if (profile.isVerified && profile.verificationStatus != "pending") {
                                    val target = profile.chwCode.ifEmpty { "CHW-1042" }
                                    navController.navigate("chw_dashboard/$target") {
                                        popUpTo("welcome_auth") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("pending_approval") {
                                        popUpTo("welcome_auth") { inclusive = true }
                                    }
                                }
                            }
                            UserRole.PARENT, UserRole.GUARDIAN, UserRole.CHILD -> {
                                val target = profile.assignedChildId.ifEmpty { "TK-1042" }
                                navController.navigate("child_dashboard/$target") {
                                    popUpTo("welcome_auth") { inclusive = true }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "welcome_auth"
    ) {
        // ==================== 1. REDESIGNED WELCOME SCREEN ====================
        composable("welcome_auth") {
            WelcomeAuthScreen(
                repository = repository,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onNavigateToSignIn = {
                    navController.navigate("sign_in")
                },
                onNavigateToSignUp = { roleKey ->
                    navController.navigate("sign_up/$roleKey")
                },
                onNavigateToCompleteProfile = { profile ->
                    currentUserProfile = profile
                    pendingProfileForCompletion = profile
                    navController.navigate("complete_profile")
                },
                onAuthenticated = { profile ->
                    currentUserProfile = profile
                    when (profile.role) {
                        UserRole.ADMIN -> {
                            navController.navigate("admin_dashboard") {
                                popUpTo("welcome_auth") { inclusive = true }
                            }
                        }
                        UserRole.CHW -> {
                            if (profile.isVerified && profile.verificationStatus != "pending") {
                                navController.navigate("chw_dashboard/${profile.chwCode.ifEmpty { "CHW-1042" }}") {
                                    popUpTo("welcome_auth") { inclusive = true }
                                }
                            } else {
                                navController.navigate("pending_approval") {
                                    popUpTo("welcome_auth") { inclusive = true }
                                }
                            }
                        }
                        UserRole.PARENT, UserRole.GUARDIAN, UserRole.CHILD -> {
                            navController.navigate("child_dashboard/${profile.assignedChildId.ifEmpty { "TK-1042" }}") {
                                popUpTo("welcome_auth") { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        // ==================== 2. SHARED SIGN-IN SCREEN ====================
        composable("sign_in") {
            SignInScreen(
                repository = repository,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onBack = { navController.popBackStack() },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") },
                onNavigateToSignUp = { navController.navigate("sign_up/parent") },
                onNavigateToCompleteProfile = { profile ->
                    currentUserProfile = profile
                    pendingProfileForCompletion = profile
                    navController.navigate("complete_profile")
                },
                onNavigateToPendingApproval = { profile ->
                    currentUserProfile = profile
                    navController.navigate("pending_approval") {
                        popUpTo("sign_in") { inclusive = true }
                    }
                },
                onRoleResolved = { role, profile ->
                    currentUserProfile = profile
                    when (role) {
                        UserRole.ADMIN -> {
                            navController.navigate("admin_dashboard") {
                                popUpTo("sign_in") { inclusive = true }
                            }
                        }
                        UserRole.CHW -> {
                            val target = profile.chwCode.ifEmpty { "CHW-1042" }
                            navController.navigate("chw_dashboard/$target") {
                                popUpTo("sign_in") { inclusive = true }
                            }
                        }
                        UserRole.PARENT, UserRole.GUARDIAN, UserRole.CHILD -> {
                            val target = profile.assignedChildId.ifEmpty { "TK-1042" }
                            navController.navigate("child_dashboard/$target") {
                                popUpTo("sign_in") { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        // ==================== 3. ROLE-SPECIFIC SIGN-UP SCREEN ====================
        composable(
            route = "sign_up/{roleKey}",
            arguments = listOf(navArgument("roleKey") { type = NavType.StringType; defaultValue = "parent" })
        ) { backStackEntry ->
            val roleKey = backStackEntry.arguments?.getString("roleKey") ?: "parent"
            SignUpScreen(
                initialRoleKey = roleKey,
                repository = repository,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onBack = { navController.popBackStack() },
                onNavigateToSignIn = {
                    navController.navigate("sign_in") {
                        popUpTo("sign_up/$roleKey") { inclusive = true }
                    }
                },
                onNavigateToPendingApproval = { profile ->
                    currentUserProfile = profile
                    navController.navigate("pending_approval") {
                        popUpTo("welcome_auth") { inclusive = true }
                    }
                },
                onParentRegistered = { profile ->
                    currentUserProfile = profile
                    val target = profile.assignedChildId.ifEmpty { "TK-1042" }
                    navController.navigate("child_dashboard/$target") {
                        popUpTo("welcome_auth") { inclusive = true }
                    }
                }
            )
        }

        // ==================== 4. FORGOT PASSWORD SCREEN ====================
        composable("forgot_password") {
            ForgotPasswordScreen(
                repository = repository,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onBack = { navController.popBackStack() }
            )
        }

        // ==================== 5. PENDING APPROVAL SCREEN ====================
        composable("pending_approval") {
            val currentAuth = repository.getCurrentUser()
            val user = currentUserProfile ?: UserProfile(
                uid = currentAuth?.uid.orEmpty(),
                email = currentAuth?.email.orEmpty(),
                displayName = currentAuth?.displayName?.ifEmpty { "Health Worker" } ?: "Health Worker",
                role = UserRole.CHW,
                county = "Turkana",
                isVerified = false,
                verificationStatus = "pending"
            )
            PendingApprovalScreen(
                user = user,
                repository = repository,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onApproved = { approvedUser ->
                    currentUserProfile = approvedUser
                    val chwId = approvedUser.chwCode.ifEmpty { "CHW-1042" }
                    navController.navigate("chw_dashboard/$chwId") {
                        popUpTo("pending_approval") { inclusive = true }
                    }
                },
                onSignOut = {
                    repository.signOut()
                    currentUserProfile = null
                    navController.navigate("welcome_auth") {
                        popUpTo("pending_approval") { inclusive = true }
                    }
                }
            )
        }

        // ==================== 6. COMPLETE PROFILE SCREEN ====================
        composable("complete_profile") {
            val initial = pendingProfileForCompletion ?: UserProfile(
                uid = repository.getCurrentUser()?.uid.orEmpty(),
                email = repository.getCurrentUser()?.email.orEmpty(),
                displayName = repository.getCurrentUser()?.displayName.orEmpty(),
                role = UserRole.PARENT
            )
            CompleteProfileScreen(
                initialProfile = initial,
                onProfileCompleted = { role, targetId ->
                    when (role) {
                        UserRole.CHW -> {
                            val user = repository.getCurrentUser()
                            val chwUser = initial.copy(
                                uid = user?.uid.orEmpty(),
                                role = UserRole.CHW,
                                chwCode = targetId,
                                isVerified = false,
                                verificationStatus = "pending"
                            )
                            currentUserProfile = chwUser
                            navController.navigate("pending_approval") {
                                popUpTo("welcome_auth") { inclusive = true }
                            }
                        }
                        UserRole.PARENT, UserRole.GUARDIAN, UserRole.CHILD -> {
                            navController.navigate("child_dashboard/$targetId") {
                                popUpTo("welcome_auth") { inclusive = true }
                            }
                        }
                        UserRole.ADMIN -> {
                            navController.navigate("admin_dashboard") {
                                popUpTo("welcome_auth") { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        // ==================== 7. ADMIN DASHBOARD (STEALTH ROUTE GUARD) ====================
        composable("admin_dashboard") {
            // Guard: If not an authenticated Admin, display 404 (do not leak existence of admin)
            val isAdmin = currentUserProfile?.role == UserRole.ADMIN
            if (!isAdmin) {
                NotFoundScreen(
                    onNavigateHome = {
                        navController.navigate("welcome_auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            } else {
                AdminDashboardScreen(
                    onSignOut = {
                        repository.signOut()
                        currentUserProfile = null
                        navController.navigate("welcome_auth") {
                            popUpTo("admin_dashboard") { inclusive = true }
                        }
                    }
                )
            }
        }

        // ==================== 8. GENERIC 404 NOT FOUND ====================
        composable("not_found_404") {
            NotFoundScreen(
                onNavigateHome = {
                    navController.navigate("welcome_auth") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ==================== 9. CHW DASHBOARD ====================
        composable("chw_dashboard/{chwId}") { backStackEntry ->
            val chwId = backStackEntry.arguments?.getString("chwId") ?: "CHW-1042"
            CHWDashboardScreen(
                chwId = chwId,
                userProfile = currentUserProfile,
                onStartScreeningForChild = { child ->
                    activeChildForScreening = child
                    navController.navigate("intake")
                },
                onNavigateToAfyaBot = { role, cId ->
                    navController.navigate("afya_bot/$role/$cId")
                },
                onNavigateToUSSD = {
                    navController.navigate("ussd_simulator")
                },
                onSignOut = {
                    repository.signOut()
                    currentUserProfile = null
                    navController.navigate("welcome_auth") {
                        popUpTo("chw_dashboard/$chwId") { inclusive = true }
                    }
                }
            )
        }

        // ==================== 10. CHILD / GUARDIAN 3D DASHBOARD ====================
        composable("child_dashboard/{childId}") { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: "TK-1042"
            Child3DDashboardScreen(
                childId = childId,
                userProfile = currentUserProfile,
                onNavigateToAfyaBot = { role, cId ->
                    navController.navigate("afya_bot/$role/$cId")
                },
                onNavigateToUSSD = {
                    navController.navigate("ussd_simulator")
                },
                onSignOut = {
                    repository.signOut()
                    currentUserProfile = null
                    navController.navigate("welcome_auth") {
                        popUpTo("child_dashboard/$childId") { inclusive = true }
                    }
                }
            )
        }

        // ==================== 11. INTAKE SCREEN ====================
        composable("intake") {
            val child = activeChildForScreening ?: ChildProfile("TK-1042", "Akiru Ewoi", 18, "Male")
            PatientIntakeScreen(
                initialChild = child,
                onNavigateBack = { navController.popBackStack() },
                onPredictionComplete = { childProfile, input, output ->
                    activeChildForScreening = childProfile
                    lastScreeningInput = input
                    lastInferenceResult = output
                    navController.navigate("results")
                }
            )
        }

        // ==================== 12. RESULTS SCREEN ====================
        composable("results") {
            if (activeChildForScreening != null && lastScreeningInput != null && lastInferenceResult != null) {
                ResultsScreen(
                    child = activeChildForScreening!!,
                    screening = lastScreeningInput!!,
                    result = lastInferenceResult!!,
                    onNavigateBack = { navController.popBackStack() },
                    onDone = {
                        navController.popBackStack("chw_dashboard/CHW-1042", false)
                    }
                )
            }
        }

        // ==================== 13. AFYABOT INTELLIGENT ASSISTANT ====================
        composable(
            route = "afya_bot/{roleKey}/{childId}",
            arguments = listOf(
                navArgument("roleKey") { type = NavType.StringType; defaultValue = "parent" },
                navArgument("childId") { type = NavType.StringType; defaultValue = "TK-1042" }
            )
        ) { backStackEntry ->
            val roleKey = backStackEntry.arguments?.getString("roleKey") ?: "parent"
            val childId = backStackEntry.arguments?.getString("childId") ?: "TK-1042"
            AfyaBotScreen(
                childId = childId,
                roleKey = roleKey,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }

        // ==================== 14. 2G USSD GATEWAY SIMULATOR ====================
        composable("ussd_simulator") {
            USSDSimulatorScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
