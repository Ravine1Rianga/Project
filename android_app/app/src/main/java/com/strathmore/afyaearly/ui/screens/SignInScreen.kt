package com.strathmore.afyaearly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strathmore.afyaearly.ui.theme.TealPrimary

@Composable
fun SignInScreen(
    onSignInSuccess: (isGuest: Boolean) -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val defaultPin = "1234"

    fun handleDigit(d: String) {
        if (enteredPin.length < 4) {
            enteredPin += d
            if (enteredPin.length == 4) {
                if (enteredPin == defaultPin) {
                    onSignInSuccess(false)
                } else {
                    errorMessage = "Incorrect PIN. Default demo PIN is 1234."
                    enteredPin = ""
                }
            }
        }
    }

    fun handleBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock",
                tint = TealPrimary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AfyaEarly",
                style = MaterialTheme.typography.headlineLarge,
                color = TealPrimary
            )
            Text(
                text = "Northern Kenya Paediatric Developmental Screening",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            // PIN Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(4) { index ->
                    val filled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (filled) TealPrimary else Color.LightGray)
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Numeric Keypad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val keypad = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("Guest", "0", "Del")
            )

            for (row in keypad) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    for (key in row) {
                        when (key) {
                            "Guest" -> {
                                TextButton(
                                    onClick = { onSignInSuccess(true) },
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Text("Guest", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            "Del" -> {
                                IconButton(
                                    onClick = { handleBackspace() },
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Icon(Icons.Default.Backspace, contentDescription = "Delete")
                                }
                            }
                            else -> {
                                FilledTonalButton(
                                    onClick = { handleDigit(key) },
                                    shape = CircleShape,
                                    modifier = Modifier.size(72.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(key, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Strathmore University Capstone Project • ICS 4C",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}
