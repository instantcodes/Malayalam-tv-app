package com.example.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlayerManager(private val context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var progressPollJob: Job? = null

    companion object {
        private const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }

    private var activeStreamUrl: String? = null
    private var activeChannelName: String? = null

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(
        context,
        DefaultRenderersFactory(context).setEnableDecoderFallback(true)
    )
        .setSeekBackIncrementMs(10000)
        .setSeekForwardIncrementMs(10000)
        .build().apply {
            playWhenReady = true
        }

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private fun createDataSourceFactory(): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setUserAgent(DEFAULT_USER_AGENT)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*"
                )
            )
    }

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == Player.STATE_BUFFERING
                val isEnded = playbackState == Player.STATE_ENDED
                val isReady = playbackState == Player.STATE_READY
                val isLive = exoPlayer.isCurrentMediaItemLive

                _playerState.update {
                    it.copy(
                        isBuffering = isBuffering,
                        durationMs = if (isLive) 0L else exoPlayer.duration.coerceAtLeast(0L),
                        isLive = isLive,
                        errorMessage = null,
                        switchingChannelName = if (isReady) null else it.switchingChannelName
                    )
                }

                if (isReady) {
                    startProgressPolling()
                } else if (isEnded) {
                    stopProgressPolling()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update {
                    it.copy(
                        isPlaying = isPlaying,
                        switchingChannelName = if (isPlaying) null else it.switchingChannelName
                    )
                }
                if (isPlaying) {
                    startProgressPolling()
                } else {
                    stopProgressPolling()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("PlayerManager", "Player error: ${error.message}", error)
                _playerState.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        switchingChannelName = null,
                        errorMessage = "Stream temporarily unavailable. Tap to retry or select another channel."
                    )
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                extractVideoResolutions(tracks)
            }
        })
    }

    fun playStream(url: String, channelName: String? = null) {
        activeStreamUrl = url
        activeChannelName = channelName
        playStreamInternal(url, channelName)
    }

    fun retryCurrentStream() {
        activeStreamUrl?.let { url ->
            playStreamInternal(url, activeChannelName)
        }
    }

    private fun playStreamInternal(url: String, channelName: String?) {
        try {
            // Stop previous stream cleanly to eliminate ghost frames, stale decoders, or audio overlaps
            exoPlayer.stop()
            exoPlayer.clearMediaItems()

            _playerState.update {
                it.copy(
                    isBuffering = true,
                    isPlaying = false,
                    errorMessage = null,
                    switchingChannelName = channelName,
                    currentPositionMs = 0L,
                    durationMs = 0L,
                    availableHeights = emptyList()
                )
            }

            val uri = Uri.parse(url)
            val isHls = url.contains(".m3u8", ignoreCase = true) || url.contains("hls", ignoreCase = true)
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMimeType(if (isHls) MimeTypes.APPLICATION_M3U8 else null)
                .build()

            val dataSourceFactory = createDataSourceFactory()

            val mediaSource: MediaSource = if (isHls) {
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(false) // Safe against missing CODECS attribute in manifests
                    .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(4))
                    .createMediaSource(mediaItem)
            } else {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }

            // resetPosition = true clears out prior timeline buffers so no overlap happens
            exoPlayer.setMediaSource(mediaSource, true)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            Log.e("PlayerManager", "Failed to start stream: ${e.message}", e)
            _playerState.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    switchingChannelName = null,
                    errorMessage = "Cannot open stream: ${e.message}"
                )
            }
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                exoPlayer.prepare()
            }
            exoPlayer.play()
        }
    }

    fun toggleMute() {
        val currentlyMuted = _playerState.value.isMuted
        val newMuted = !currentlyMuted
        exoPlayer.volume = if (newMuted) 0f else 1.0f
        _playerState.update { it.copy(isMuted = newMuted, volume = if (newMuted) 0f else 1.0f) }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        exoPlayer.volume = clamped
        _playerState.update { it.copy(volume = clamped, isMuted = clamped == 0f) }
    }

    fun setVideoQuality(quality: VideoQuality) {
        val builder = exoPlayer.trackSelectionParameters.buildUpon()
        if (quality == VideoQuality.AUTO) {
            builder.clearVideoSizeConstraints()
        } else {
            // Constrain video height
            builder.setMaxVideoSize(quality.maxHeight * 16 / 9, quality.maxHeight)
        }
        exoPlayer.trackSelectionParameters = builder.build()

        val label = when (quality) {
            VideoQuality.AUTO -> "Auto"
            VideoQuality.FHD_1080P -> "1080p"
            VideoQuality.HD_720P -> "720p"
            VideoQuality.SD_480P -> "480p"
            VideoQuality.LOW_360P -> "360p"
        }

        _playerState.update {
            it.copy(
                selectedQuality = quality,
                currentResolutionLabel = label
            )
        }
    }

    fun toggleResizeMode() {
        val current = _playerState.value.resizeMode
        val next = when (current) {
            ResizeMode.FIT -> ResizeMode.FILL
            ResizeMode.FILL -> ResizeMode.STRETCH
            ResizeMode.STRETCH -> ResizeMode.FIT
        }
        _playerState.update { it.copy(resizeMode = next) }
    }

    fun setFullscreen(fullscreen: Boolean) {
        _playerState.update { it.copy(isFullscreen = fullscreen) }
    }

    fun toggleFullscreen() {
        _playerState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    fun seekToLive() {
        if (exoPlayer.isCurrentMediaItemLive) {
            exoPlayer.seekToDefaultPosition()
        }
    }

    private fun startProgressPolling() {
        if (progressPollJob?.isActive == true) return
        progressPollJob = coroutineScope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = if (exoPlayer.isCurrentMediaItemLive) 0L else exoPlayer.duration.coerceAtLeast(0L)
                    _playerState.update {
                        it.copy(
                            currentPositionMs = pos,
                            durationMs = dur
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressPolling() {
        progressPollJob?.cancel()
        progressPollJob = null
    }

    private fun extractVideoResolutions(tracks: Tracks) {
        val heights = mutableSetOf<Int>()
        var activeResLabel: String? = null

        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    if (format.height > 0) {
                        heights.add(format.height)
                        if (group.isTrackSelected(i)) {
                            activeResLabel = "${format.height}p"
                        }
                    }
                }
            }
        }

        val sortedHeights = heights.sortedDescending()
        _playerState.update {
            it.copy(
                availableHeights = sortedHeights,
                currentResolutionLabel = if (it.selectedQuality != VideoQuality.AUTO) {
                    it.selectedQuality.label.split(" ").first()
                } else {
                    activeResLabel ?: "Auto"
                }
            )
        }
    }

    fun release() {
        stopProgressPolling()
        exoPlayer.release()
    }
}
