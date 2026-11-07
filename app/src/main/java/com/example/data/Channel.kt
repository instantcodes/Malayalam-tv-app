package com.example.data

data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val groupTitle: String,
    val isFavorite: Boolean = false
) {
    val displayCategory: String
        get() {
            val raw = groupTitle.trim()
            return when {
                raw.isBlank() || raw.equals("Undefined", ignoreCase = true) -> "General"
                raw.equals("Animation;Kids", ignoreCase = true) ||
                raw.equals("Kids;Animation", ignoreCase = true) ||
                (raw.contains("Animation", ignoreCase = true) && raw.contains("Kids", ignoreCase = true)) ||
                raw.equals("Animation", ignoreCase = true) -> "Kids"
                else -> raw
            }
        }
}

object BlacklistConfig {
    // Whitelist for channels explicitly requested back by the user
    private val allowedWhitelist = listOf(
        "asianethd",
        "asianet",
        "mazhavilmanorama576p",
        "mazhavilmanorama"
    )

    private val blacklistedPatterns = listOf(
        "anandtv",
        "asianetmiddleeast",
        "cmalayalamtv",
        "didiktvkpm",
        "garshomtv",
        "goodnesstv",
        "harvesttv",
        "kairaliarabia",
        "kalikatv",
        "mewatchlive1",
        "mta7asia",
        "nickelodeonindia",
        "nickelodeon",
        "ntv7",
        "rtmparlimendewannegara",
        "rtmparlimendewanrakyat",
        "pogo",
        "pottadivine",
        "selangortv",
        "shalomamerica",
        "shalomamerica720p",
        "tuningshalomamerica",
        "shekinahtv",
        "sonic",
        "sonysportsten1",
        "sonysportsten2",
        "sonysportsten3",
        "sonysportsten5",
        "visionindia",
        "starsports1",
        "starsports3",
        "sttarsports3",
        "starsportsselect1",
        "starsportsselect",
        "suryatv",
        "suryatvhd",
        "sunsurya",
        "sunsuryahd",
        "surya"
    )

    fun isBlacklisted(channelName: String): Boolean {
        val normalized = normalize(channelName)
        if (normalized.isBlank()) return false

        // Asianet Middle East must remain blacklisted
        if (normalized.contains("middleeast") || normalized.contains("asianetmiddleeast")) {
            return true
        }

        // Shalom America must be removed/blacklisted
        if (normalized.contains("shalomamerica")) {
            return true
        }

        // Surya TV / Sun Surya must be removed/blacklisted
        if (normalized.contains("surya")) {
            return true
        }

        // Whitelist override: Allow Asianet HD and Mazhavil Manorama back
        if (allowedWhitelist.any { normalized == it || normalized.startsWith(it) }) {
            return false
        }

        return blacklistedPatterns.any { pattern ->
            normalized == pattern || normalized.contains(pattern)
        }
    }

    private fun normalize(input: String): String {
        return input.lowercase().replace(Regex("[^a-z0-9]"), "")
    }
}
