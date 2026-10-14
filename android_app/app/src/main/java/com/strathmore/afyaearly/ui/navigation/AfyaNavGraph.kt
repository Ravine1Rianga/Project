package com.strathmore.afyaearly.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.strathmore.afyaearly.data.local.entity.PatientEntity
import com.strathmore.afyaearly.data.local.entity.ScreeningRecordEntity
import com.strathmore.afyaearly.ml.InferenceOutput
import com.strathmore.afyaearly.ui.screens.DashboardScreen
import com.strathmore.afyaearly.ui.screens.PatientIntakeScreen
import com.strathmore.afyaearly.ui.screens.ResultsScreen
import com.strathmore.afyaearly.ui.screens.SignInScreen

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object Dashboard : Screen("dashboard")
    object Intake : Screen("intake")
    object Results : Screen("results")
}

@Composable
fun AfyaNavGraph() {
    val navController = rememberNavController()

    var currentPatient by remember { mutableStateOf<PatientEntity?>(null) }
    var currentScreening by remember { mutableStateOf<ScreeningRecordEntity?>(null) }
    var currentResult by remember { mutableStateOf<InferenceOutput?>(null) }

    NavHost(
        navController = navController,
        startDestination = Screen.SignIn.route
    ) {
        composable(Screen.SignIn.route) {
            SignInScreen(
                onSignInSuccess = { isGuest ->
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onStartNewScreening = {
                    navController.navigate(Screen.Intake.route)
                },
                onSignOut = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Intake.route) {
            PatientIntakeScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPredictionComplete = { patient, screening, result ->
                    currentPatient = patient
                    currentScreening = screening
                    currentResult = result
                    navController.navigate(Screen.Results.route)
                }
            )
        }

        composable(Screen.Results.route) {
            if (currentPatient != null && currentScreening != null && currentResult != null) {
                ResultsScreen(
                    patient = currentPatient!!,
                    screening = currentScreening!!,
                    result = currentResult!!,
                    onNavigateBack = { navController.popBackStack() },
                    onDone = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
