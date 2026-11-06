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
            "Mazhavil Manorama HD",
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
            "Star Sports 3"
        )

        for (channel in blacklisted) {
            assertTrue("Expected $channel to be blacklisted", BlacklistConfig.isBlacklisted(channel))
        }

        // Channels that must remain available:
        val allowed = listOf(
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
    fun `m3u parser excludes blacklisted channels and keeps allowed channels`() {
        val testM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="AsianetNews.in" group-title="News",Asianet News
            https://example.com/asianet.m3u8
            #EXTINF:-1 tvg-id="Pogo.in" group-title="Kids",Pogo
            https://example.com/pogo.m3u8
            #EXTINF:-1 tvg-id="SonyTen5.in" group-title="Sports",Sony Sports Ten 5
            https://example.com/sony5.m3u8
            #EXTINF:-1 tvg-id="StarSelect.in" group-title="Sports",Star Sports Select 1
            https://example.com/star1.m3u8
            #EXTINF:-1 tvg-id="MediaOne.in" group-title="News",MediaOne TV
            https://example.com/mediaone.m3u8
        """.trimIndent()

        val parsed = M3uParser.parse(testM3u)
        assertEquals(2, parsed.size)
        assertEquals("Asianet News", parsed[0].name)
        assertEquals("MediaOne TV", parsed[1].name)
    }

    @Test
    fun `cleanChannelName removes picture quality tags while preserving channel name`() {
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
}
