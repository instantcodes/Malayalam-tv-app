package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// Update JSON URL
const val VERSION_URL = "https://raw.githubusercontent.com/instantcodes/Malayalam-tv-app/main/version.json"

// Data class representing the version information
data class AppUpdateInfo(
    val latestVersionCode: Int,
    val minVersionCode: Int,
    val latestVersionName: String,
    val message: String,
    val downloadUrl: String
)

// Types of update prompt
enum class UpdateType {
    NONE,
    OPTIONAL,
    FORCED
}

// Result of update check
data class UpdateResult(
    val type: UpdateType,
    val info: AppUpdateInfo?
)

object UpdateChecker {
    private const val PREFS_NAME = "update_checker_prefs"
    private const val KEY_LAST_SNOOZE_TIME = "last_snooze_time"
    private const val SNOOZE_DURATION_MS = 3L * 24 * 60 * 60 * 1000 // 3 days in milliseconds

    // Observable active update for UpdateGate composable
    var activeUpdate by mutableStateOf<UpdateResult?>(null)
        private set

    // Dismiss active update dialog
    fun dismissDialog() {
        activeUpdate = null
    }

    // Read current app versionCode from PackageManager
    fun getCurrentVersionCode(context: Context): Long {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }

    // Save snooze time when user clicks "Later"
    fun snoozeOptionalUpdate(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_LAST_SNOOZE_TIME, System.currentTimeMillis()).apply()
        } catch (e: Exception) {
            // Fail silently
        }
    }

    // Check if optional update is currently snoozed (less than 3 days)
    fun isSnoozed(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastSnooze = prefs.getLong(KEY_LAST_SNOOZE_TIME, 0L)
            (System.currentTimeMillis() - lastSnooze) < SNOOZE_DURATION_MS
        } catch (e: Exception) {
            false
        }
    }

    // Fetch version JSON off main thread
    suspend fun fetchUpdateInfo(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val jsonString = URL(VERSION_URL).readText()
            val json = JSONObject(jsonString)
            AppUpdateInfo(
                latestVersionCode = json.optInt("latestVersionCode", 0),
                minVersionCode = json.optInt("minVersionCode", 0),
                latestVersionName = json.optString("latestVersionName", ""),
                message = json.optString("message", ""),
                downloadUrl = json.optString("downloadUrl", "")
            )
        } catch (e: Exception) {
            // Fail silently on network error or invalid JSON
            null
        }
    }

    // Compare versionCode and evaluate update requirement
    fun evaluateUpdate(
        info: AppUpdateInfo,
        currentVersionCode: Long,
        isSnoozed: Boolean,
        ignoreSnooze: Boolean
    ): UpdateResult {
        return when {
            // 1. Forced update if current < minVersionCode (ignores snooze)
            currentVersionCode < info.minVersionCode -> {
                UpdateResult(UpdateType.FORCED, info)
            }
            // 2. Optional update if current < latestVersionCode (respects snooze unless ignored)
            currentVersionCode < info.latestVersionCode -> {
                if (!ignoreSnooze && isSnoozed) {
                    UpdateResult(UpdateType.NONE, null)
                } else {
                    UpdateResult(UpdateType.OPTIONAL, info)
                }
            }
            // 3. Otherwise show nothing
            else -> {
                UpdateResult(UpdateType.NONE, null)
            }
        }
    }

    // Automatic check on app launch
    suspend fun checkOnLaunch(context: Context) {
        val info = fetchUpdateInfo() ?: return
        val currentCode = getCurrentVersionCode(context)
        val snoozed = isSnoozed(context)
        val result = evaluateUpdate(info, currentCode, snoozed, ignoreSnooze = false)
        if (result.type != UpdateType.NONE) {
            withContext(Dispatchers.Main) {
                activeUpdate = result
            }
        }
    }

    // Manual check triggered from Settings
    suspend fun checkManually(context: Context): UpdateResult {
        val info = fetchUpdateInfo()
        val currentCode = getCurrentVersionCode(context)
        val result = if (info == null) {
            UpdateResult(UpdateType.NONE, null)
        } else {
            evaluateUpdate(info, currentCode, isSnoozed = false, ignoreSnooze = true)
        }

        withContext(Dispatchers.Main) {
            if (result.type == UpdateType.NONE) {
                Toast.makeText(context, "You're on the latest version", Toast.LENGTH_SHORT).show()
            } else {
                activeUpdate = result
            }
        }
        return result
    }

    // Open APK download URL using ACTION_VIEW
    fun openDownloadUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fail silently
        }
    }
}
