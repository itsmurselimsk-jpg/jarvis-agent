package com.example.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.jarvis.model.RiskLevel
import com.example.jarvis.model.SafetyRequest
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBorder
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisSurfaceElevated
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary

@Composable
fun SafetyConfirmationDialog(
    request: SafetyRequest,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF090E1A))
                .border(
                    width = 1.5.dp,
                    color = if (request.riskLevel == RiskLevel.RESTRICTED) JarvisRed else JarvisAmber,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            (if (request.riskLevel == RiskLevel.RESTRICTED) JarvisRed else JarvisAmber).copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Security Alert",
                        tint = if (request.riskLevel == RiskLevel.RESTRICTED) JarvisRed else JarvisAmber,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SECURITY CLEARANCE REQUIRED",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (request.riskLevel == RiskLevel.RESTRICTED) JarvisRed else JarvisAmber,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "JARVIS wants to:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = JarvisTextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .border(0.5.dp, JarvisBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = request.actionDescription,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisCyan,
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Reason:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = JarvisTextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B132B))
                        .padding(10.dp)
                ) {
                    Text(
                        text = request.reason,
                        fontSize = 12.sp,
                        color = JarvisTextPrimary,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tool: ${request.toolName}",
                        fontSize = 12.sp,
                        color = JarvisTextDim,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Risk: ${when (request.riskLevel) {
                            RiskLevel.SAFE -> "LOW"
                            RiskLevel.CONFIRMATION -> "MEDIUM"
                            RiskLevel.RESTRICTED -> "HIGH"
                        }}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (request.riskLevel == RiskLevel.RESTRICTED) JarvisRed else JarvisAmber
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            request.onCancel()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("safety_cancel_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = JarvisTextSecondary
                        )
                    ) {
                        Text("CANCEL", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            request.onConfirm()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("safety_confirm_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (request.riskLevel == RiskLevel.RESTRICTED) JarvisRed else JarvisAmber,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("CONFIRM", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
