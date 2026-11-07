package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class ChannelRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("malayalam_tv_prefs", Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(loadFavorites())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    companion object {
        private const val TAG = "ChannelRepository"
        const val PLAYLIST_URL = "https://iptv-org.github.io/iptv/languages/mal.m3u"
        private const val PREF_FAVORITES_KEY = "key_favorite_channel_ids"
        private const val PREF_CACHED_M3U = "key_cached_m3u_content"
        private const val PREF_DARK_MODE = "key_dark_mode_enabled"
    }

    suspend fun loadChannels(forceRefresh: Boolean = false) {
        _isLoading.value = true
        _errorMessage.value = null

        withContext(Dispatchers.IO) {
            var rawM3u: String? = null

            // 1. Try remote network fetch
            try {
                val request = Request.Builder()
                    .url(PLAYLIST_URL)
                    .header("User-Agent", "MalayalamLiveTV/1.0 (Android; ExoPlayer)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank() && body.contains("#EXTM3U")) {
                            rawM3u = body
                            // Save to cache for offline availability
                            prefs.edit().putString(PREF_CACHED_M3U, body).apply()
                            Log.d(TAG, "Successfully loaded fresh playlist from network (${body.length} bytes)")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Remote playlist fetch failed: ${e.message}")
            }

            // 2. Fallback to cached M3U if network failed or empty
            if (rawM3u == null) {
                rawM3u = prefs.getString(PREF_CACHED_M3U, null)
                if (rawM3u != null) {
                    Log.d(TAG, "Using cached playlist from SharedPreferences")
                }
            }

            // 3. Fallback to bundled asset if still null
            if (rawM3u == null) {
                try {
                    context.assets.open("channels_mal.m3u").use { input ->
                        rawM3u = InputStreamReader(input).readText()
                        Log.d(TAG, "Using bundled asset playlist")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load bundled asset: ${e.message}")
                }
            }

            if (rawM3u != null) {
                val parsed = M3uParser.parse(rawM3u!!)
                val favs = _favoriteIds.value
                val mapped = parsed.map { channel ->
                    channel.copy(isFavorite = favs.contains(channel.id) || favs.contains(channel.name.lowercase()))
                }.toMutableList()

                // Guarantee that the requested Surya TV stream is always included
                if (mapped.none { it.streamUrl.contains("SuryaTv/SuryaTV-HDR10-IN-index.m3u8", ignoreCase = true) }) {
                    mapped.add(
                        2.coerceAtMost(mapped.size),
                        Channel(
                            id = "suryatv",
                            name = "Surya TV",
                            streamUrl = "https://d3dt6rg724ecr3.cloudfront.net/SuryaTv/SuryaTV-HDR10-IN-index.m3u8",
                            logoUrl = "https://jiotvimages.cdn.jio.com/dare_images/images/Surya_HD.png",
                            groupTitle = "Entertainment",
                            isFavorite = favs.contains("suryatv") || favs.contains("surya tv")
                        )
                    )
                }

                _channels.value = mapped
            } else {
                _errorMessage.value = "Unable to load channels. Please check network connection."
            }

            _isLoading.value = false
        }
    }

    fun toggleFavorite(channel: Channel) {
        val currentFavs = _favoriteIds.value.toMutableSet()
        val key = channel.id.ifBlank { channel.name.lowercase() }
        val isNowFav = if (currentFavs.contains(key)) {
            currentFavs.remove(key)
            false
        } else {
            currentFavs.add(key)
            true
        }
        _favoriteIds.value = currentFavs
        prefs.edit().putStringSet(PREF_FAVORITES_KEY, currentFavs).apply()

        // Update in-memory list
        _channels.value = _channels.value.map {
            if (it.id == channel.id && it.streamUrl == channel.streamUrl) {
                it.copy(isFavorite = isNowFav)
            } else {
                it
            }
        }
    }

    fun isDarkModeEnabled(): Boolean {
        return prefs.getBoolean(PREF_DARK_MODE, true)
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_DARK_MODE, enabled).apply()
    }

    private fun loadFavorites(): Set<String> {
        return prefs.getStringSet(PREF_FAVORITES_KEY, emptySet()) ?: emptySet()
    }
}
