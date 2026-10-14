package com.strath.finalpr0ject.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strath.finalpr0ject.data.model.UserRole
import com.strath.finalpr0ject.ui.theme.AdminIndigo
import com.strath.finalpr0ject.ui.theme.ChildCoral
import com.strath.finalpr0ject.ui.theme.TealPrimary

@Composable
fun AuthScreen(
    onRoleSelected: (UserRole, String) -> Unit
) {
    var selectedRole by remember { mutableStateOf(UserRole.CHW) }
    var email by remember { mutableStateOf("maria.akiru@health.go.ke") }
    var password by remember { mutableStateOf("••••••••") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF004D40),
                        Color(0xFF00796B),
                        Color(0xFFFBFDFA)
                    ),
                    startY = 0f,
                    endY = 900f
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "AfyaEarly Care Portal",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Pediatric Malnutrition & Neurodevelopmental Health",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }

            // Role Selector Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Select Portal Access Role",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.DarkGray
                    )

                    // 3 Role Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Admin
                        Surface(
                            onClick = {
                                selectedRole = UserRole.ADMIN
                                email = "admin@health.strath.ke"
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedRole == UserRole.ADMIN) AdminIndigo else Color(0xFFF5F5F5),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = if (selectedRole == UserRole.ADMIN) Color.White else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Admin",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (selectedRole == UserRole.ADMIN) Color.White else Color.DarkGray
                                )
                            }
                        }

                        // CHW
                        Surface(
                            onClick = {
                                selectedRole = UserRole.CHW
                                email = "maria.akiru@health.go.ke"
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedRole == UserRole.CHW) TealPrimary else Color(0xFFF5F5F5),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    tint = if (selectedRole == UserRole.CHW) Color.White else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "CHW",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (selectedRole == UserRole.CHW) Color.White else Color.DarkGray
                                )
                            }
                        }

                        // Child / Parent (3D)
                        Surface(
                            onClick = {
                                selectedRole = UserRole.CHILD
                                email = "parent.akiru@gmail.com"
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedRole == UserRole.CHILD) ChildCoral else Color(0xFFF5F5F5),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.ChildCare,
                                    contentDescription = null,
                                    tint = if (selectedRole == UserRole.CHILD) Color.White else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Child (3D)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (selectedRole == UserRole.CHILD) Color.White else Color.DarkGray
                                )
                            }
                        }
                    }

                    // Credentials input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Account Email / Ministry ID") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Sign-In Button
                    Button(
                        onClick = {
                            val targetId = when (selectedRole) {
                                UserRole.ADMIN -> "admin-01"
                                UserRole.CHW -> "CHW-1042"
                                UserRole.CHILD -> "TK-1042"
                            }
                            onRoleSelected(selectedRole, targetId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (selectedRole) {
                                UserRole.ADMIN -> AdminIndigo
                                UserRole.CHW -> TealPrimary
                                UserRole.CHILD -> ChildCoral
                            }
                        )
                    ) {
                        Text(
                            text = "Access ${selectedRole.name} Dashboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Footer
            Text(
                text = "Connected to Firebase Cloud (finalproject-eefdc)\nStrathmore University ICS Capstone",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
