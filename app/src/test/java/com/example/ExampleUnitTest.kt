package com.example

import com.example.data.BlacklistConfig
import com.example.data.Channel
import com.example.data.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `blacklist filter correctly detects user specified blacklisted channels`() {
        // Channels explicitly requested to be removed:
        val blacklisted = listOf(
            "Anand TV",
            "Asianet Middle East",
            "C Malayalam TV",
            "DidikTV KPM",
            "Garshom TV",
            "Goodness TV",
            "Harvest TV",
            "Kairali Arabia",
            "Kalika TV",
            "meWATCH LIVE 1",
            "MTA7 Asia",
            "Nickelodeon (India)",
            "NTV7",
            "RTM Parlimen Dewan Negara",
            "RTM Parlimen Dewan Rakyat",
            "Pogo",
            "Potta-Divine",
            "SelangorTV",
            "Tuning Shalom America",
            "Shekinah TV",
            "Sonic",
            "SONY SPORTS Ten 1",
            "SONY SPORTS Ten 2",
            "SONY SPORTS Ten 3",
            "SONYSPORTS Ten 3",
            "Sony Sports Ten 5",
            "Star Sports Select 1",
            "Star Sports Select 1 HD",
            "Vision(india)",
            "Star Sports1",
            "Sttar Sports 3",
            "Star Sports 3",
            "Shalom America (720p)",
            "Shalom America (720p) [Not 24/7]",
            "Shalom America"
        )

        for (channel in blacklisted) {
            assertTrue("Expected $channel to be blacklisted", BlacklistConfig.isBlacklisted(channel))
        }

        // Channels that must remain available, including Asianet HD, Mazhavil Manorama, and Surya TV:
        val allowed = listOf(
            "Asianet HD",
            "Mazhavil Manorama (576p)",
            "Mazhavil Manorama",
            "Surya TV",
            "Surya TV HD",
            "Sun Surya",
            "Sun Surya HD",
            "Asianet News",
            "MediaOne TV",
            "Reporter TV",
            "24 News",
            "Kairali News",
            "Manorama News",
            "Mathrubhumi News",
            "DD Malayalam",
            "Amrita TV",
            "Flowers TV",
            "Jaihind TV",
            "Janam TV",
            "Kaumudy TV",
            "Safari TV",
            "Kappa TV"
        )

        for (channel in allowed) {
            assertFalse("Expected $channel to NOT be blacklisted", BlacklistConfig.isBlacklisted(channel))
        }
    }

    @Test
    fun `m3u parser excludes blacklisted channels and keeps allowed channels including Mazhavil Manorama and Asianet HD`() {
        val testM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="AsianetHD.in" group-title="Entertainment",Asianet HD
            https://example.com/asianethd.m3u8
            #EXTINF:-1 tvg-id="Mazhavil.in" group-title="Entertainment",Mazhavil Manorama (576p)
            https://example.com/mazhavil.m3u8
            #EXTINF:-1 tvg-id="Pogo.in" group-title="Kids",Pogo
            https://example.com/pogo.m3u8
            #EXTINF:-1 tvg-id="SonyTen5.in" group-title="Sports",Sony Sports Ten 5
            https://example.com/sony5.m3u8
            #EXTINF:-1 tvg-id="AsianetME.in" group-title="Entertainment",Asianet Middle East
            https://example.com/asianetme.m3u8
            #EXTINF:-1 tvg-id="SuryaTV.in" group-title="Entertainment",Surya TV HD
            https://d3dt6rg724ecr3.cloudfront.net/SuryaTv/SuryaTV-HDR10-IN-index.m3u8
            #EXTINF:-1 tvg-id="MediaOne.in" group-title="News",MediaOne TV
            https://example.com/mediaone.m3u8
        """.trimIndent()

        val parsed = M3uParser.parse(testM3u)
        assertEquals(4, parsed.size)
        assertEquals("Asianet", parsed[0].name)
        assertEquals("Mazhavil Manorama", parsed[1].name)
        assertEquals("Surya TV", parsed[2].name)
        assertEquals("MediaOne TV", parsed[3].name)
    }

    @Test
    fun `cleanChannelName removes picture quality tags while preserving channel name`() {
        assertEquals("Asianet", M3uParser.cleanChannelName("Asianet HD"))
        assertEquals("Mazhavil Manorama", M3uParser.cleanChannelName("Mazhavil Manorama (576p)"))
        assertEquals("Surya TV", M3uParser.cleanChannelName("Surya TV HD"))
        assertEquals("Sun Surya", M3uParser.cleanChannelName("Sun Surya HD (1080p)"))
        assertEquals("Asianet News", M3uParser.cleanChannelName("Asianet News (720p)"))
        assertEquals("Reporter TV", M3uParser.cleanChannelName("Reporter TV HD [1080p]"))
        assertEquals("24 News", M3uParser.cleanChannelName("24 News 1080p"))
        assertEquals("Flowers TV", M3uParser.cleanChannelName("Flowers TV HD"))
        assertEquals("Amrita TV", M3uParser.cleanChannelName("Amrita TV (720p) [50fps]"))
        assertEquals("DD Malayalam", M3uParser.cleanChannelName("DD Malayalam (576p)"))
        assertEquals("Safari TV", M3uParser.cleanChannelName("Safari TV SD"))
        assertEquals("MediaOne TV", M3uParser.cleanChannelName("MediaOne TV (FHD)"))
        assertEquals("Kairali News", M3uParser.cleanChannelName("Kairali News 4K"))
        assertEquals("Kaumudy TV", M3uParser.cleanChannelName("Kaumudy TV [480p]"))
        assertEquals("Jaihind TV", M3uParser.cleanChannelName("Jaihind TV (1080p) [HEVC]"))
    }

    @Test
    fun `animation kids category correctly maps to Kids`() {
        val ch1 = Channel("1", "Kochu TV", "https://example.com/stream.m3u8", null, "Animation;Kids")
        assertEquals("Kids", ch1.displayCategory)

        val ch2 = Channel("2", "Cartoon", "https://example.com/stream.m3u8", null, "Kids;Animation")
        assertEquals("Kids", ch2.displayCategory)

        val ch3 = Channel("3", "Anime", "https://example.com/stream.m3u8", null, "Animation")
        assertEquals("Kids", ch3.displayCategory)
    }

    @Test
    fun `favorite channels come first when sorting on All category`() {
        val list = listOf(
            Channel("1", "Asianet", "https://example.com/1.m3u8", null, "Entertainment", isFavorite = false),
            Channel("2", "Mazhavil Manorama", "https://example.com/2.m3u8", null, "Entertainment", isFavorite = true),
            Channel("3", "MediaOne", "https://example.com/3.m3u8", null, "News", isFavorite = false),
            Channel("4", "24 News", "https://example.com/4.m3u8", null, "News", isFavorite = true)
        )

        val sorted = list.sortedByDescending { it.isFavorite }
        assertEquals("Mazhavil Manorama", sorted[0].name)
        assertEquals("24 News", sorted[1].name)
        assertEquals("Asianet", sorted[2].name)
        assertEquals("MediaOne", sorted[3].name)
        assertTrue(sorted[0].isFavorite)
        assertTrue(sorted[1].isFavorite)
        assertFalse(sorted[2].isFavorite)
        assertFalse(sorted[3].isFavorite)
    }

    @Test
    fun `evaluateUpdate returns FORCED when current versionCode is less than minVersionCode`() {
        val info = AppUpdateInfo(
            latestVersionCode = 5,
            minVersionCode = 3,
            latestVersionName = "1.5",
            message = "Critical security update",
            downloadUrl = "https://example.com/app.apk"
        )

        val result = UpdateChecker.evaluateUpdate(info, currentVersionCode = 2, isSnoozed = false, ignoreSnooze = false)
        assertEquals(UpdateType.FORCED, result.type)
        assertEquals(info, result.info)

        // Forced update also ignores snooze
        val snoozedResult = UpdateChecker.evaluateUpdate(info, currentVersionCode = 2, isSnoozed = true, ignoreSnooze = false)
        assertEquals(UpdateType.FORCED, snoozedResult.type)
    }

    @Test
    fun `evaluateUpdate returns OPTIONAL when current is between min and latest and not snoozed`() {
        val info = AppUpdateInfo(
            latestVersionCode = 2,
            minVersionCode = 1,
            latestVersionName = "1.1",
            message = "New features available",
            downloadUrl = "https://example.com/app.apk"
        )

        // Current is 1 (matches minVersionCode 1, less than latestVersionCode 2)
        val result = UpdateChecker.evaluateUpdate(info, currentVersionCode = 1, isSnoozed = false, ignoreSnooze = false)
        assertEquals(UpdateType.OPTIONAL, result.type)
        assertEquals("New features available", result.info?.message)
    }

    @Test
    fun `evaluateUpdate respects snooze for optional updates unless ignoreSnooze is true`() {
        val info = AppUpdateInfo(
            latestVersionCode = 2,
            minVersionCode = 1,
            latestVersionName = "1.1",
            message = "New features available",
            downloadUrl = "https://example.com/app.apk"
        )

        // Snoozed and ignoreSnooze is false -> returns NONE
        val snoozedResult = UpdateChecker.evaluateUpdate(info, currentVersionCode = 1, isSnoozed = true, ignoreSnooze = false)
        assertEquals(UpdateType.NONE, snoozedResult.type)

        // Snoozed but ignoreSnooze is true (manual check from Settings) -> returns OPTIONAL
        val manualResult = UpdateChecker.evaluateUpdate(info, currentVersionCode = 1, isSnoozed = true, ignoreSnooze = true)
        assertEquals(UpdateType.OPTIONAL, manualResult.type)
    }

    @Test
    fun `evaluateUpdate returns NONE when app is on or above latest version`() {
        val info = AppUpdateInfo(
            latestVersionCode = 2,
            minVersionCode = 1,
            latestVersionName = "1.1",
            message = "New version",
            downloadUrl = "https://example.com/app.apk"
        )

        val resultEqual = UpdateChecker.evaluateUpdate(info, currentVersionCode = 2, isSnoozed = false, ignoreSnooze = true)
        assertEquals(UpdateType.NONE, resultEqual.type)

        val resultHigher = UpdateChecker.evaluateUpdate(info, currentVersionCode = 3, isSnoozed = false, ignoreSnooze = true)
        assertEquals(UpdateType.NONE, resultHigher.type)
    }
}

