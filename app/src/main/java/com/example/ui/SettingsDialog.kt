package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.UpdateChecker
import com.example.UpdateType
import com.example.data.Channel
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.LiveBroadcastRed

@Composable
fun SettingsDialog(
    isPipEnabled: Boolean,
    onTogglePip: (Boolean) -> Unit,
    isLockScreenPlay: Boolean,
    onToggleLockScreenPlay: (Boolean) -> Unit,
    isBackgroundPlay: Boolean,
    onToggleBackgroundPlay: (Boolean) -> Unit,
    onEnterPipNow: () -> Unit,
    onDismissRequest: () -> Unit,
    isParentalControlEnabled: Boolean = false,
    hasParentalPin: Boolean = false,
    channels: List<Channel> = emptyList(),
    lockedChannelKeys: Set<String> = emptySet(),
    onToggleParentalControl: (Boolean) -> Unit = {},
    onSetParentalPin: (String) -> Unit = {},
    onVerifyParentalPin: (String) -> Boolean = { true },
    onToggleChannelLock: (Channel) -> Unit = {},
    onLockAllChannels: () -> Unit = {},
    onUnlockAllChannels: () -> Unit = {},
    getChannelKey: (Channel) -> String = { it.id.ifBlank { it.name.lowercase().trim() } }
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var showSetPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showManageChannelsDialog by remember { mutableStateOf(false) }
    var showVerifyPinToDisableDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 28.dp)
                .fillMaxWidth(0.95f)
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Settings",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Malayali AI TV",
                                fontSize = 11.sp,
                                color = AmberPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Playback & Features
                Text(
                    text = "PLAYBACK FEATURES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberPrimary,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Feature 1: Picture in Picture
                SettingToggleItem(
                    icon = Icons.Default.PictureInPictureAlt,
                    title = "Picture in Picture Mode",
                    description = "Mini-player floats over other apps when leaving or pressing home",
                    checked = isPipEnabled,
                    onCheckedChange = onTogglePip,
                    testTag = "setting_pip_toggle"
                )

                // Quick Launch PiP Button
                if (isPipEnabled) {
                    OutlinedButton(
                        onClick = {
                            onDismissRequest()
                            onEnterPipNow()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberPrimary),
                        border = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 42.dp, bottom = 12.dp)
                            .testTag("enter_pip_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPicture,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enter Picture in Picture Now",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Feature 2: Lock Screen Play
                SettingToggleItem(
                    icon = Icons.Default.Lock,
                    title = "Lock Screen Play",
                    description = "Keep display awake during playback to prevent screen timeouts",
                    checked = isLockScreenPlay,
                    onCheckedChange = onToggleLockScreenPlay,
                    testTag = "setting_lock_screen_toggle"
                )

                // Feature 3: Background Play
                SettingToggleItem(
                    icon = Icons.Default.PlayArrow,
                    title = "Background Play",
                    description = "Continue playing audio when app is minimized or screen turned off",
                    checked = isBackgroundPlay,
                    onCheckedChange = onToggleBackgroundPlay,
                    testTag = "setting_background_play_toggle"
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Parental Control
                Text(
                    text = "PARENTAL CONTROL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberPrimary,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingToggleItem(
                    icon = Icons.Default.Security,
                    title = "Parental Control PIN",
                    description = if (hasParentalPin) {
                        if (isParentalControlEnabled) "Protected channels require 4-digit PIN (${lockedChannelKeys.size} locked)"
                        else "PIN is set. Turn ON to protect locked channels"
                    } else {
                        "Set a 4-digit PIN to restrict selected channels"
                    },
                    checked = isParentalControlEnabled,
                    onCheckedChange = { enable ->
                        if (enable) {
                            if (!hasParentalPin) {
                                showSetPinDialog = true
                            } else {
                                onToggleParentalControl(true)
                            }
                        } else {
                            if (hasParentalPin) {
                                showVerifyPinToDisableDialog = true
                            } else {
                                onToggleParentalControl(false)
                            }
                        }
                    },
                    testTag = "setting_parental_control_toggle"
                )

                if (hasParentalPin) {
                    // Manage Locked Channels Button
                    OutlinedButton(
                        onClick = { showManageChannelsDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberPrimary),
                        border = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 38.dp, bottom = 8.dp)
                            .testTag("manage_locked_channels_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Manage Locked Channels (${lockedChannelKeys.size} locked)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Change PIN Button
                    OutlinedButton(
                        onClick = { showChangePinDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 38.dp, bottom = 12.dp)
                            .testTag("change_pin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Change 4-Digit PIN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { showSetPinDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberPrimary),
                        border = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 38.dp, bottom = 12.dp)
                            .testTag("set_pin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Set 4-Digit PIN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Updates & Developer Contact
                Text(
                    text = "UPDATES & SUPPORT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberPrimary,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Watch on Web Button
                Button(
                    onClick = {
                        openUrl(context, "https://malayali-ai-tv-web-redirect.vercel.app")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("watch_on_web_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Watch on Web",
                            tint = AmberPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Watch on Web",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Updates Button
                Button(
                    onClick = {
                        scope.launch {
                            val result = UpdateChecker.checkManually(context)
                            if (result.type != UpdateType.NONE) {
                                onDismissRequest()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("check_updates_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Updates",
                            tint = AmberPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Check for Updates",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mail to Developer Button
                Button(
                    onClick = {
                        openEmail(context, "anandhukrishnas@outlook.com")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mail_developer_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = AmberPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Contact Developer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Source Code Button
                Button(
                    onClick = {
                        openUrl(context, "https://github.com/instantcodes/malayali-ai-tv")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("source_code_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Source Code",
                            tint = AmberPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Source Code",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Legal, Terms & Creator
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Licensed and Copyrights reserved and applys terms & conditions",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Made With",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Love",
                                tint = LiveBroadcastRed,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(horizontal = 2.dp)
                            )
                            Text(
                                text = "From God's Own Country",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "by ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Instantcodes",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberPrimary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .clickable {
                                        openUrl(context, "https://github.com/instantcodes")
                                    }
                                    .testTag("instantcodes_link")
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSetPinDialog) {
        SetParentalPinDialog(
            isChangingPin = false,
            onSavePin = { newPin ->
                onSetParentalPin(newPin)
                onToggleParentalControl(true)
            },
            onDismissRequest = { showSetPinDialog = false }
        )
    }

    if (showChangePinDialog) {
        SetParentalPinDialog(
            isChangingPin = true,
            onVerifyCurrentPin = onVerifyParentalPin,
            onSavePin = { newPin ->
                onSetParentalPin(newPin)
            },
            onDismissRequest = { showChangePinDialog = false }
        )
    }

    if (showManageChannelsDialog) {
        ManageLockedChannelsDialog(
            channels = channels,
            lockedChannelKeys = lockedChannelKeys,
            onToggleLock = onToggleChannelLock,
            onLockAll = onLockAllChannels,
            onUnlockAll = onUnlockAllChannels,
            getChannelKey = getChannelKey,
            onDismissRequest = { showManageChannelsDialog = false }
        )
    }

    if (showVerifyPinToDisableDialog) {
        PinPromptDialog(
            channelName = "Parental Settings",
            onVerifyPin = { pin ->
                val ok = onVerifyParentalPin(pin)
                if (ok) {
                    onToggleParentalControl(false)
                    showVerifyPinToDisableDialog = false
                }
                ok
            },
            onDismissRequest = { showVerifyPinToDisableDialog = false }
        )
    }
}

@Composable
private fun SettingToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AmberPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = AmberPrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("SettingsDialog", "Failed to open url: ${e.message}")
    }
}

private fun openEmail(context: Context, email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, "Malayali AI TV Feedback / Support")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:$email")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        } catch (e2: Exception) {
            Log.e("SettingsDialog", "Failed to send email: ${e2.message}")
        }
    }
}
