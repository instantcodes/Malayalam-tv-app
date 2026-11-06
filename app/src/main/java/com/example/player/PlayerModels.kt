package com.example.player

enum class VideoQuality(val label: String, val maxHeight: Int) {
    AUTO("Auto (Adaptive)", Int.MAX_VALUE),
    FHD_1080P("1080p (Full HD)", 1080),
    HD_720P("720p (HD)", 720),
    SD_480P("480p (SD)", 480),
    LOW_360P("360p", 360)
}

enum class ResizeMode(val label: String) {
    FIT("Fit"),
    FILL("Crop / Fill"),
    STRETCH("Stretch")
}

data class PlayerState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isLive: Boolean = true,
    val isMuted: Boolean = false,
    val volume: Float = 1.0f,
    val selectedQuality: VideoQuality = VideoQuality.AUTO,
    val availableHeights: List<Int> = emptyList(),
    val currentResolutionLabel: String = "Auto",
    val resizeMode: ResizeMode = ResizeMode.FIT,
    val isFullscreen: Boolean = false,
    val errorMessage: String? = null,
    val switchingChannelName: String? = null
)
