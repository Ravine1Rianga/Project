package com.strath.finalpr0ject.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.strath.finalpr0ject.data.model.ChildProfile
import com.strath.finalpr0ject.data.model.UserRole
import com.strath.finalpr0ject.ml.InferenceOutput
import com.strath.finalpr0ject.ml.ScreeningInput
import com.strath.finalpr0ject.ui.screens.*

@Composable
fun AfyaNavGraph() {
    val navController = rememberNavController()

    var activeChildForScreening by remember { mutableStateOf<ChildProfile?>(null) }
    var lastScreeningInput by remember { mutableStateOf<ScreeningInput?>(null) }
    var lastInferenceResult by remember { mutableStateOf<InferenceOutput?>(null) }

    NavHost(
        navController = navController,
        startDestination = "auth"
    ) {
        // 1. Role Selection & Authentication
        composable("auth") {
            AuthScreen(
                onRoleSelected = { role, targetId ->
                    when (role) {
                        UserRole.ADMIN -> navController.navigate("admin_dashboard")
                        UserRole.CHW -> navController.navigate("chw_dashboard/$targetId")
                        UserRole.CHILD -> navController.navigate("child_dashboard/$targetId")
                    }
                }
            )
        }

        // 2. Admin Dashboard
        composable("admin_dashboard") {
            AdminDashboardScreen(
                onSignOut = {
                    navController.navigate("auth") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                }
            )
        }

        // 3. CHW Dashboard
        composable("chw_dashboard/{chwId}") { backStackEntry ->
            val chwId = backStackEntry.arguments?.getString("chwId") ?: "CHW-1042"
            CHWDashboardScreen(
                chwId = chwId,
                onStartScreeningForChild = { child ->
                    activeChildForScreening = child
                    navController.navigate("intake")
                },
                onSignOut = {
                    navController.navigate("auth") {
                        popUpTo("chw_dashboard/$chwId") { inclusive = true }
                    }
                }
            )
        }

        // 4. Child / Caregiver Dashboard (with 3D)
        composable("child_dashboard/{childId}") { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: "TK-1042"
            Child3DDashboardScreen(
                childId = childId,
                onSignOut = {
                    navController.navigate("auth") {
                        popUpTo("child_dashboard/$childId") { inclusive = true }
                    }
                }
            )
        }

        // 5. Intake Screen (prefilled with assigned child)
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

        // 6. Results Screen
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
    }
}
