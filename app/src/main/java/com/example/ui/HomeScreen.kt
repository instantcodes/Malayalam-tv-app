package com.example.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.MalayalamTvTheme

@Composable
fun HomeScreen(
    viewModel: ChannelViewModel,
    onEnterPip: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val channels by viewModel.filteredChannels.collectAsStateWithLifecycle()
    val allChannels by viewModel.repository.channels.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingChannels.collectAsStateWithLifecycle()
    val isPipEnabled by viewModel.isPipEnabled.collectAsStateWithLifecycle()
    val isLockScreenPlay by viewModel.isLockScreenPlay.collectAsStateWithLifecycle()
    val isBackgroundPlay by viewModel.isBackgroundPlay.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? Activity

    // Handle Fullscreen orientation and immersive system bars
    LaunchedEffect(playerState.isFullscreen, uiState.isPipModeActive) {
        if (!uiState.isPipModeActive) {
            activity?.let { act ->
                val window = act.window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                if (playerState.isFullscreen) {
                    act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    // Keep screen awake while playing to prevent device lock/sleep if enabled
    DisposableEffect(playerState.isPlaying, isLockScreenPlay) {
        val window = activity?.window
        if (playerState.isPlaying && isLockScreenPlay) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.let { act ->
                act.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val insetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Hardware Back Button Handler
    BackHandler(enabled = playerState.isFullscreen || uiState.isCategoryDrawerOpen || uiState.searchQuery.isNotEmpty() || uiState.isSettingsDialogVisible) {
        when {
            uiState.isSettingsDialogVisible -> viewModel.toggleSettingsDialog(false)
            uiState.isCategoryDrawerOpen -> viewModel.toggleCategoryDrawer(false)
            playerState.isFullscreen -> viewModel.setFullscreen(false)
            uiState.searchQuery.isNotEmpty() -> viewModel.setSearchQuery("")
        }
    }

    MalayalamTvTheme(darkTheme = uiState.isDarkMode) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (uiState.isPipModeActive) {
                // PICTURE-IN-PICTURE MINIMAL SURFACE (NO UI CONTROLS OR OVERLAYS)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                player = viewModel.playerManager.exoPlayer
                                keepScreenOn = true
                            }
                        },
                        update = { playerView ->
                            playerView.player = viewModel.playerManager.exoPlayer
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (playerState.isFullscreen) {
                // FULLSCREEN MODE
                Box(modifier = Modifier.fillMaxSize()) {
                    VideoPlayerView(
                        playerManager = viewModel.playerManager,
                        playerState = playerState,
                        channel = uiState.selectedChannel,
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onPreviousChannel = { viewModel.selectPreviousChannel() },
                        onNextChannel = { viewModel.selectNextChannel() },
                        onOpenQualityPicker = { viewModel.toggleQualityDialog(true) },
                        onToggleSideBar = { viewModel.toggleCategoryDrawer() },
                        onOpenSettings = { viewModel.toggleSettingsDialog(true) },
                        onEnterPip = onEnterPip,
                        isSideBarOpen = uiState.isCategoryDrawerOpen,
                        isLockScreenPlay = isLockScreenPlay,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Side Bar Drawer in Fullscreen Mode
                    ChannelSideBar(
                        isOpen = uiState.isCategoryDrawerOpen,
                        channels = allChannels,
                        categories = categories,
                        selectedChannel = uiState.selectedChannel,
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = { viewModel.setCategory(it) },
                        onSelectChannel = { ch -> viewModel.selectChannel(ch, keepDrawerOpen = false) },
                        onToggleFavorite = { ch -> viewModel.toggleFavorite(ch) },
                        onClose = { viewModel.toggleCategoryDrawer(false) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // NORMAL PORTRAIT / DUAL-MODE
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Embedded Player (16:9 Aspect Ratio)
                            VideoPlayerView(
                                playerManager = viewModel.playerManager,
                                playerState = playerState,
                                channel = uiState.selectedChannel,
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onPreviousChannel = { viewModel.selectPreviousChannel() },
                                onNextChannel = { viewModel.selectNextChannel() },
                                onOpenQualityPicker = { viewModel.toggleQualityDialog(true) },
                                onToggleSideBar = {},
                                onOpenSettings = { viewModel.toggleSettingsDialog(true) },
                                onEnterPip = onEnterPip,
                                isSideBarOpen = false,
                                isLockScreenPlay = isLockScreenPlay,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            )

                            // Top Controls: Search, Refresh, Brightness/Theme, Settings
                            TopSearchBar(
                                query = uiState.searchQuery,
                                onQueryChanged = { viewModel.setSearchQuery(it) },
                                isDarkMode = uiState.isDarkMode,
                                onToggleDarkMode = { viewModel.toggleDarkMode() },
                                onRefresh = { viewModel.loadChannels(forceRefresh = true) },
                                onOpenSettings = { viewModel.toggleSettingsDialog(true) }
                            )

                            // Category Filter Pill Strip
                            CategoryFilterRow(
                                categories = categories,
                                selectedCategory = uiState.selectedCategory,
                                onCategorySelected = { viewModel.setCategory(it) }
                            )

                            // Channel Count & Status Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${channels.size} Channels Available",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (isLoading) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            color = AmberPrimary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier
                                                .width(14.dp)
                                                .height(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Syncing…",
                                            fontSize = 11.sp,
                                            color = AmberPrimary
                                        )
                                    }
                                }
                            }

                            // Channels List
                            if (channels.isEmpty() && !isLoading) {
                                EmptyChannelsView(
                                    onResetFilters = {
                                        viewModel.setCategory("All")
                                        viewModel.setSearchQuery("")
                                    }
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .testTag("channels_list"),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(channels, key = { it.id + it.streamUrl }) { channel ->
                                        ChannelCard(
                                            channel = channel,
                                            isSelected = channel.id == uiState.selectedChannel?.id,
                                            onSelect = { viewModel.selectChannel(channel, keepDrawerOpen = false) },
                                            onToggleFavorite = { viewModel.toggleFavorite(channel) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quality Dialog
            if (uiState.isQualityDialogVisible) {
                VideoQualityDialog(
                    currentQuality = playerState.selectedQuality,
                    availableHeights = playerState.availableHeights,
                    onQualitySelected = { quality ->
                        viewModel.setVideoQuality(quality)
                    },
                    onDismissRequest = {
                        viewModel.toggleQualityDialog(false)
                    }
                )
            }

            // Settings Dialog
            if (uiState.isSettingsDialogVisible) {
                SettingsDialog(
                    isPipEnabled = isPipEnabled,
                    onTogglePip = { viewModel.setPipEnabled(it) },
                    isLockScreenPlay = isLockScreenPlay,
                    onToggleLockScreenPlay = { viewModel.setLockScreenPlay(it) },
                    isBackgroundPlay = isBackgroundPlay,
                    onToggleBackgroundPlay = { viewModel.setBackgroundPlay(it) },
                    onEnterPipNow = onEnterPip,
                    onDismissRequest = {
                        viewModel.toggleSettingsDialog(false)
                    }
                )
            }
        }
    }
}
