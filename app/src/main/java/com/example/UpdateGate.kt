package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MalayalamTvTheme
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

/**
 * UpdateGate composable displaying Forced or Optional update dialogs
 * styled with the app's dark theme and orange/amber accent.
 */
@Composable
fun UpdateGate() {
    val context = LocalContext.current

    // Trigger update check on app launch
    LaunchedEffect(Unit) {
        UpdateChecker.checkOnLaunch(context)
    }

    val updateResult = UpdateChecker.activeUpdate ?: return
    val info = updateResult.info ?: return
    val isForced = updateResult.type == UpdateType.FORCED

    MalayalamTvTheme {
        AlertDialog(
            onDismissRequest = {
                // Back press and outside tap do nothing for forced updates
                if (!isForced) {
                    UpdateChecker.snoozeOptionalUpdate(context)
                    UpdateChecker.dismissDialog()
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = !isForced,
                dismissOnClickOutside = !isForced
            ),
            icon = {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = "Update Available",
                    tint = AmberPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (isForced) "Update Required" else "New version ${info.latestVersionName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimaryDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = info.message,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        UpdateChecker.openDownloadUrl(context, info.downloadUrl)
                        if (!isForced) {
                            UpdateChecker.dismissDialog()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("update_confirm_button")
                ) {
                    Text(
                        text = "Update",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            dismissButton = if (!isForced) {
                {
                    TextButton(
                        onClick = {
                            UpdateChecker.snoozeOptionalUpdate(context)
                            UpdateChecker.dismissDialog()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = TextSecondaryDark
                        ),
                        modifier = Modifier.testTag("update_later_button")
                    ) {
                        Text(
                            text = "Later",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            } else null,
            shape = RoundedCornerShape(20.dp),
            containerColor = DarkSurface,
            tonalElevation = 8.dp,
            modifier = Modifier.testTag("update_gate_dialog")
        )
    }
}
