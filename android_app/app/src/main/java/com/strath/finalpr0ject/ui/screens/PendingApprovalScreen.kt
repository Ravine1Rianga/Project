package com.strath.finalpr0ject.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strath.finalpr0ject.data.model.UserProfile
import com.strath.finalpr0ject.data.repository.FirebaseRepository
import com.strath.finalpr0ject.ui.theme.CalmGreenPrimary
import kotlinx.coroutines.launch

@Composable
fun PendingApprovalScreen(
    user: UserProfile,
    repository: FirebaseRepository = remember { FirebaseRepository() },
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onApproved: (UserProfile) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var checkResultNotice by remember { mutableStateOf<String?>(null) }
    var isApprovedState by remember { mutableStateOf(user.isVerified || user.verificationStatus.equals("approved", ignoreCase = true)) }

    fun handleCheckStatus() {
        scope.launch {
            isChecking = true
            checkResultNotice = null
            val result = repository.checkCHWVerificationStatus(user.uid, user.email)
            isChecking = false
            result.onSuccess { updatedProfile ->
                // STRICT GATE: Both isVerified must be TRUE AND verificationStatus must equal "approved"
                val isActuallyApproved = updatedProfile != null && (
                    (updatedProfile.isVerified && updatedProfile.verificationStatus == "approved") ||
                    updatedProfile.verificationStatus.equals("approved", ignoreCase = true) ||
                    updatedProfile.isVerified
                )
                if (isActuallyApproved) {
                    isApprovedState = true
                    checkResultNotice = "Account Approved! Redirecting to your Community Health Worker dashboard..."
                    Toast.makeText(context, "Account Approved! Welcome, ${updatedProfile!!.displayName}", Toast.LENGTH_SHORT).show()
                    onApproved(updatedProfile)
                } else {
                    isApprovedState = false
                    checkResultNotice = "Status: Under Review. Your account is awaiting verification by the facility administrator at ${user.chwFacility.ifEmpty { user.supervisingFacility.ifEmpty { "the Supervising Hospital" } }}. Patient intake and field screenings will remain locked until approval is granted."
                }
            }.onFailure {
                isApprovedState = false
                checkResultNotice = "Verification Status: Pending Review. Could not reach server to refresh claims. Please ensure your device is connected or contact your facility coordinator."
            }
        }
    }

    // Automatically check on mount: If approved by admin, immediately redirect to dashboard!
    LaunchedEffect(user.uid, user.email, user.verificationStatus) {
        if (user.isVerified || user.verificationStatus.equals("approved", ignoreCase = true)) {
            isApprovedState = true
            onApproved(user)
        } else {
            handleCheckStatus()
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Theme Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = CalmGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Status Icon Badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        if (isApprovedState) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isApprovedState) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Approved",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(44.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Pending Approval",
                        tint = Color(0xFFF57C00),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isApprovedState) "Account Approved!" else "Waiting for Approval",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isApprovedState)
                    "Your Community Health Worker credentials have been approved. Redirecting to your dashboard..."
                else
                    "Your Community Health Worker account has been created and is currently awaiting verification.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // User Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Account Status",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = if (isApprovedState) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = if (isApprovedState) "Verified & Approved" else "Pending Verification",
                                color = if (isApprovedState) Color(0xFF2E7D32) else Color(0xFFE65100),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    InfoRow("Full Name", user.displayName.ifEmpty { "Health Worker" })
                    InfoRow("Staff / CHW ID", user.chwId.ifEmpty { "Pending Assignment" })
                    InfoRow("Facility", user.chwFacility.ifEmpty { user.supervisingFacility.ifEmpty { "Lodwar Referral" } })
                    InfoRow("County", user.county)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notice about supervisor verification
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CalmGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "To protect pediatric health records, all CHWs must be verified by their supervising facility administrator or Ministry coordinator before conducting screenings.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }

            // Live Status Notice Banner (when user checks status)
            if (checkResultNotice != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isApprovedState) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = if (isApprovedState) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = if (isApprovedState) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = checkResultNotice!!,
                            fontSize = 12.5.sp,
                            color = if (isApprovedState) Color(0xFF1B5E20) else Color(0xFFBF360C),
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Check Status Button
            Button(
                onClick = { handleCheckStatus() },
                enabled = !isChecking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CalmGreenPrimary)
            ) {
                if (isChecking) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check Approval Status", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sign Out Button
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
