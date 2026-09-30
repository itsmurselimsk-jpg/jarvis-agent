package com.example.jarvis.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.auth.AuthManager
import com.example.jarvis.auth.JarvisUser
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisBorder
import com.example.jarvis.ui.theme.JarvisBorderSubtle
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisSurface
import com.example.jarvis.ui.theme.JarvisSurfaceElevated
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary

@Composable
fun AccountProfileScreen(
    user: JarvisUser,
    authManager: AuthManager,
    onSignOut: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var actionStatusMessage by remember { mutableStateOf<String?>(null) }
    var isOperating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // User Holographic Ring
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(JarvisSurfaceElevated)
                .border(2.dp, JarvisCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "User Avatar",
                tint = JarvisCyan,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = user.displayName.uppercase(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = JarvisTextPrimary
        )

        Text(
            text = user.email,
            fontSize = 13.sp,
            color = JarvisTextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Identity Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Verification Badge
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(JarvisSurface)
                    .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (user.isEmailVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (user.isEmailVerified) JarvisGreen else JarvisAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "STATUS",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextDim
                        )
                        Text(
                            text = if (user.isEmailVerified) "Verified" else "Unverified",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (user.isEmailVerified) JarvisGreen else JarvisAmber
                        )
                    }
                }
            }

            // Session Mode Badge
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(JarvisSurface)
                    .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "SECURITY",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextDim
                        )
                        Text(
                            text = if (user.isAnonymous) "Guest Mode" else "Firebase Auth",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisCyan
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // UID & Security Matrix Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisSurface)
                .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "NEURAL IDENTITY METRICS",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = JarvisCyan
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Firebase UID", fontSize = 12.sp, color = JarvisTextSecondary)
                    Text(
                        text = user.uid.take(16) + if (user.uid.length > 16) "..." else "",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisTextPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Data Isolation", fontSize = 12.sp, color = JarvisTextSecondary)
                    Text(
                        text = "Isolated by UID",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisGreen
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Provider", fontSize = 12.sp, color = JarvisTextSecondary)
                    Text(
                        text = user.providerId,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisTextPrimary
                    )
                }

                if (!user.phoneNumber.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Linked Phone", fontSize = 12.sp, color = JarvisTextSecondary)
                        Text(
                            text = user.phoneNumber,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisCyan
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Resend Verification Email Button if not verified
        if (!user.isEmailVerified && !user.isAnonymous) {
            OutlinedButton(
                onClick = {
                    isOperating = true
                    authManager.sendEmailVerification(
                        onSuccess = { msg ->
                            isOperating = false
                            actionStatusMessage = msg
                        },
                        onError = { err ->
                            isOperating = false
                            actionStatusMessage = err
                        }
                    )
                },
                enabled = !isOperating,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("resend_verification_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.MarkEmailRead, contentDescription = null, tint = JarvisAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESEND VERIFICATION EMAIL", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = JarvisAmber)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Action Status Message Banner
        actionStatusMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(JarvisSurfaceElevated)
                    .border(0.5.dp, JarvisBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(text = msg, fontSize = 11.sp, color = JarvisCyan, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign Out Button
        Button(
            onClick = {
                authManager.signOut {
                    onSignOut()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = JarvisSurfaceElevated,
                contentColor = JarvisCyan
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, JarvisBorder, RoundedCornerShape(10.dp))
                .testTag("account_sign_out_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("DISCONNECT OPERATOR SESSION", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Delete Account Button
        OutlinedButton(
            onClick = { showDeleteDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisRed),
            border = BorderStroke(1.dp, JarvisRed.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("account_delete_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = JarvisRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("PURGE ACCOUNT IDENTITY", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = JarvisRed)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Official STRIX Emblem Brand Signature
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.strix_logo),
                contentDescription = "Official STRIX Logo",
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.dp, JarvisCyan.copy(alpha = 0.6f), CircleShape),
                contentScale = ContentScale.Fit
            )
            Column {
                Text(
                    text = "STRIX NEURAL CORE v2.4.0",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = JarvisCyan
                )
                Text(
                    text = "THINK • HELP • DO",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisTextDim
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Explicit Delete Account Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = JarvisSurfaceElevated,
            titleContentColor = JarvisRed,
            textContentColor = JarvisTextPrimary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = JarvisRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PERMANENT PURGE DIRECTIVE", fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete this account? All associated neural memory, task schedules, and local profiles linked to UID ${user.uid.take(10)}... will be permanently erased. This operation cannot be undone.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = JarvisTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        isOperating = true
                        authManager.deleteAccount(
                            onSuccess = {
                                isOperating = false
                                onSignOut()
                            },
                            onError = { err ->
                                isOperating = false
                                actionStatusMessage = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisRed)
                ) {
                    Text("PURGE FOREVER", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL", color = JarvisTextSecondary)
                }
            }
        )
    }
}
