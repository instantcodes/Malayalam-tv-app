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
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Update JSON URL
const val VERSION_URL = "https://raw.githubusercontent.com/instantcodes/Malayalam-tv-app/main/version.json"

// Data class representing an optional announcement
data class AnnouncementInfo(
    val id: String,
    val title: String,
    val text: String,
    val startDate: String,
    val endDate: String
)

// Data class representing the version information
data class AppUpdateInfo(
    val latestVersionCode: Int,
    val minVersionCode: Int,
    val latestVersionName: String,
    val message: String,
    val downloadUrl: String,
    val updateDeadline: String = "",
    val announcements: List<AnnouncementInfo> = emptyList()
) {
    // Backward compatibility helper for single announcement
    val announcement: AnnouncementInfo?
        get() = announcements.firstOrNull()
}

typealias UpdateInfo = AppUpdateInfo

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

    private const val KEY_SHOWN_ANNOUNCEMENT_IDS = "shown_announcement_ids"

    // Observable active update for UpdateGate composable
    var activeUpdate by mutableStateOf<UpdateResult?>(null)
        private set

    // Queue of pending announcements to show one after another
    private val pendingAnnouncements = mutableListOf<AnnouncementInfo>()

    // Observable active announcement currently being displayed
    var activeAnnouncement by mutableStateOf<AnnouncementInfo?>(null)
        private set

    // Dismiss active update dialog
    fun dismissDialog() {
        activeUpdate = null
    }

    // Dismiss active announcement, mark it as shown in SharedPreferences, and show next if available
    fun dismissAnnouncement(context: Context) {
        val current = activeAnnouncement
        if (current != null) {
            markAnnouncementShown(context, current.id)
        }
        if (pendingAnnouncements.isNotEmpty()) {
            activeAnnouncement = pendingAnnouncements.removeAt(0)
        } else {
            activeAnnouncement = null
        }
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

    // Check if an announcement has already been shown
    fun isAnnouncementShown(context: Context, id: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val shown = prefs.getStringSet(KEY_SHOWN_ANNOUNCEMENT_IDS, emptySet()) ?: emptySet()
            shown.contains(id)
        } catch (e: Exception) {
            false
        }
    }

    // Mark an announcement as shown in SharedPreferences
    fun markAnnouncementShown(context: Context, id: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = prefs.getStringSet(KEY_SHOWN_ANNOUNCEMENT_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
            current.add(id)
            prefs.edit().putStringSet(KEY_SHOWN_ANNOUNCEMENT_IDS, current).apply()
        } catch (e: Exception) {
            // Fail silently
        }
    }

    // Check if announcement should be shown today (not already shown and within date range)
    fun shouldShowAnnouncement(
        announcement: AnnouncementInfo,
        context: Context,
        today: LocalDate = LocalDate.now()
    ): Boolean {
        if (isAnnouncementShown(context, announcement.id)) {
            return false
        }
        return try {
            val start = LocalDate.parse(announcement.startDate.trim())
            val end = LocalDate.parse(announcement.endDate.trim())
            !today.isBefore(start) && !today.isAfter(end)
        } catch (e: Exception) {
            // Fail silently on invalid date
            false
        }
    }

    // Helper to parse AnnouncementInfo from a JSONObject safely
    private fun parseAnnouncement(obj: JSONObject?): AnnouncementInfo? {
        if (obj == null) return null
        val id = obj.optString("id", "").trim()
        val title = obj.optString("title", "").trim()
        val text = obj.optString("text", "").trim()
        val startDate = obj.optString("startDate", "").trim()
        val endDate = obj.optString("endDate", "").trim()
        return if (id.isNotEmpty() && title.isNotEmpty() && text.isNotEmpty() && startDate.isNotEmpty() && endDate.isNotEmpty()) {
            AnnouncementInfo(id, title, text, startDate, endDate)
        } else null
    }

    // Fetch version JSON off main thread
    suspend fun fetchUpdateInfo(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val jsonString = URL(VERSION_URL).readText()
            val json = JSONObject(jsonString)

            val announcementsList = mutableListOf<AnnouncementInfo>()

            // 1. Support "announcements" array
            val announcementsArray = json.optJSONArray("announcements")
            if (announcementsArray != null) {
                for (i in 0 until announcementsArray.length()) {
                    val obj = announcementsArray.optJSONObject(i)
                    parseAnnouncement(obj)?.let { announcementsList.add(it) }
                }
            }

            // 2. Keep supporting the old single "announcement" object if present and not already added
            val singleObj = json.optJSONObject("announcement")
            if (singleObj != null) {
                parseAnnouncement(singleObj)?.let { singleAnn ->
                    if (announcementsList.none { it.id == singleAnn.id }) {
                        announcementsList.add(singleAnn)
                    }
                }
            }

            AppUpdateInfo(
                latestVersionCode = json.optInt("latestVersionCode", 0),
                minVersionCode = json.optInt("minVersionCode", 0),
                latestVersionName = json.optString("latestVersionName", ""),
                message = json.optString("message", ""),
                downloadUrl = json.optString("downloadUrl", ""),
                updateDeadline = json.optString("updateDeadline", ""),
                announcements = announcementsList
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
        ignoreSnooze: Boolean,
        today: LocalDate = LocalDate.now()
    ): UpdateResult {
        // App is on or above latest version: show nothing
        if (currentVersionCode >= info.latestVersionCode) {
            return UpdateResult(UpdateType.NONE, null)
        }

        // Parse optional updateDeadline if present
        val deadline = if (info.updateDeadline.isNotBlank()) {
            try {
                LocalDate.parse(info.updateDeadline.trim())
            } catch (e: Exception) {
                // Rule 3: If date is invalid, ignore deadline and fail silently
                null
            }
        } else {
            null
        }

        // Rule 1: versionCode is lower than latestVersionCode and updateDeadline is set
        if (deadline != null) {
            return if (!today.isBefore(deadline)) {
                // Today is ON or AFTER the deadline: show FORCED dialog
                UpdateResult(UpdateType.FORCED, info)
            } else {
                // Today is BEFORE deadline: show optional dialog and add deadline notice to the message
                if (!ignoreSnooze && isSnoozed) {
                    UpdateResult(UpdateType.NONE, null)
                } else {
                    val daysLeft = ChronoUnit.DAYS.between(today, deadline)
                    val deadlineText = "Update required by ${info.updateDeadline.trim()} ($daysLeft days left)"
                    val newMessage = if (info.message.isBlank()) {
                        deadlineText
                    } else if (info.message.contains("Update required by")) {
                        info.message
                    } else {
                        "${info.message}\n\n$deadlineText"
                    }
                    UpdateResult(UpdateType.OPTIONAL, info.copy(message = newMessage))
                }
            }
        }

        // Rule 2: If updateDeadline is empty (or invalid), keep current behavior
        return when {
            // minVersionCode forces (ignores snooze)
            currentVersionCode < info.minVersionCode -> {
                UpdateResult(UpdateType.FORCED, info)
            }
            // latestVersionCode is optional (respects snooze unless ignored)
            currentVersionCode < info.latestVersionCode -> {
                if (!ignoreSnooze && isSnoozed) {
                    UpdateResult(UpdateType.NONE, null)
                } else {
                    UpdateResult(UpdateType.OPTIONAL, info)
                }
            }
            else -> {
                UpdateResult(UpdateType.NONE, null)
            }
        }
    }

    // Automatic check on app launch
    suspend fun checkOnLaunch(context: Context, today: LocalDate = LocalDate.now()) {
        val info = fetchUpdateInfo() ?: return
        val currentCode = getCurrentVersionCode(context)
        val snoozed = isSnoozed(context)
        val result = evaluateUpdate(info, currentCode, snoozed, ignoreSnooze = false, today = today)
        withContext(Dispatchers.Main) {
            if (result.type != UpdateType.NONE) {
                activeUpdate = result
            }
            // Rule 4: Do not show announcements while a forced update dialog is showing
            val isForced = result.type == UpdateType.FORCED
            if (!isForced && info.announcements.isNotEmpty()) {
                val qualifying = info.announcements.filter { shouldShowAnnouncement(it, context, today) }
                if (qualifying.isNotEmpty()) {
                    pendingAnnouncements.clear()
                    pendingAnnouncements.addAll(qualifying)
                    // Show first qualifying announcement
                    activeAnnouncement = pendingAnnouncements.removeAt(0)
                }
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
