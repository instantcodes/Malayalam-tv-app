package com.example.data

import java.io.BufferedReader
import java.io.StringReader

object M3uParser {

    private val TVG_ID_REGEX = Regex("""tvg-id="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val TVG_NAME_REGEX = Regex("""tvg-name="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val TVG_LOGO_REGEX = Regex("""tvg-logo="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val GROUP_TITLE_REGEX = Regex("""group-title="([^"]*)"""", RegexOption.IGNORE_CASE)

    fun cleanChannelName(rawName: String): String {
        var name = rawName

        // 1. Remove bracketed or parenthesized quality, fps, codec, or status tags
        name = name.replace(
            Regex(
                """[\(\[]\s*(1080[pi]|720p|576[pi]|480p|360p|240p|fhd|uhd|4k|hd|sd|50fps|60fps|hevc|h\.?26[45]|not 24/7|geo-blocked)\s*[\)\]]""",
                RegexOption.IGNORE_CASE
            ),
            ""
        )

        // 2. Remove standalone quality/resolution/codec tokens at word boundaries
        name = name.replace(
            Regex(
                """\b(1080[pi]|720p|576[pi]|480p|360p|240p|fhd|uhd|4k|hd|sd|50fps|60fps|hevc|h\.?264|h\.?265)\b""",
                RegexOption.IGNORE_CASE
            ),
            ""
        )

        // 3. Clean any remaining empty brackets/parentheses like () or []
        name = name.replace(Regex("""\s*[\(\[]\s*[\)\]]"""), "")

        // 4. Strip trailing and leading punctuation symbols like -, |, :, /, .
        name = name.replace(Regex("""[\s\-|:/\.,]+$"""), "")
        name = name.replace(Regex("""^[\s\-|:/\.,]+"""), "")

        // 5. Collapse multiple spaces into a single space
        name = name.replace(Regex("""\s{2,}"""), " ").trim()

        return if (name.isNotBlank()) name else rawName.trim()
    }

    fun parse(m3uContent: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val reader = BufferedReader(StringReader(m3uContent))
        var line: String?
        var currentExtInf: String? = null

        while (reader.readLine().also { line = it } != null) {
            val trimmed = line!!.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                currentExtInf = trimmed
            } else if (!trimmed.startsWith("#") && currentExtInf != null) {
                val streamUrl = trimmed
                parseChannel(currentExtInf, streamUrl)?.let { channel ->
                    // Exclude any channel matching blacklisted names
                    if (!BlacklistConfig.isBlacklisted(channel.name)) {
                        channels.add(channel)
                    }
                }
                currentExtInf = null
            }
        }

        // Deduplicate channels by streamUrl or cleaned name
        return channels.distinctBy { it.name.trim().lowercase() }
    }

    private fun parseChannel(extInf: String, streamUrl: String): Channel? {
        if (streamUrl.isBlank() || !streamUrl.startsWith("http", ignoreCase = true)) {
            return null
        }

        val tvgId = TVG_ID_REGEX.find(extInf)?.groupValues?.getOrNull(1)?.trim()
        val tvgName = TVG_NAME_REGEX.find(extInf)?.groupValues?.getOrNull(1)?.trim()
        val tvgLogo = TVG_LOGO_REGEX.find(extInf)?.groupValues?.getOrNull(1)?.trim()
        val rawGroup = GROUP_TITLE_REGEX.find(extInf)?.groupValues?.getOrNull(1)?.trim() ?: "General"
        val groupTitle = when {
            rawGroup.equals("Animation;Kids", ignoreCase = true) ||
            rawGroup.equals("Kids;Animation", ignoreCase = true) ||
            (rawGroup.contains("Animation", ignoreCase = true) && rawGroup.contains("Kids", ignoreCase = true)) ||
            rawGroup.equals("Animation", ignoreCase = true) -> "Kids"
            rawGroup.isBlank() -> "General"
            else -> rawGroup
        }

        // Channel display name is either after the last comma or tvg-name
        val commaIndex = extInf.lastIndexOf(',')
        val rawDisplayName = if (commaIndex != -1 && commaIndex < extInf.length - 1) {
            extInf.substring(commaIndex + 1).trim()
        } else {
            tvgName ?: tvgId ?: "Malayalam TV"
        }

        // Clean out picture quality indicators (HD, FHD, 1080p, 720p, etc.)
        val cleanedName = cleanChannelName(rawDisplayName)

        // Check if raw name or cleaned name is blacklisted
        if (BlacklistConfig.isBlacklisted(rawDisplayName) || BlacklistConfig.isBlacklisted(cleanedName)) {
            return null
        }

        val finalName = when {
            cleanedName.equals("Sun Surya", ignoreCase = true) || cleanedName.equals("Surya TV", ignoreCase = true) -> "Surya TV"
            cleanedName.isNotBlank() -> cleanedName
            else -> tvgName ?: "Channel"
        }

        val id = tvgId?.takeIf { it.isNotBlank() } ?: finalName.replace(" ", "_").lowercase()

        return Channel(
            id = id,
            name = finalName,
            streamUrl = streamUrl,
            logoUrl = tvgLogo?.takeIf { it.isNotBlank() },
            groupTitle = if (groupTitle.isNotBlank()) groupTitle else "General"
        )
    }
}
