package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Channel
import com.example.data.ChannelRepository
import com.example.player.PlayerManager
import com.example.player.PlayerState
import com.example.player.ResizeMode
import com.example.player.VideoQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val selectedChannel: Channel? = null,
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isQualityDialogVisible: Boolean = false,
    val isCategoryDrawerOpen: Boolean = false,
    val isDarkMode: Boolean = true
)

class ChannelViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ChannelRepository(application.applicationContext)
    val playerManager = PlayerManager(application.applicationContext)

    val playerState: StateFlow<PlayerState> = playerManager.playerState
    val isLoadingChannels: StateFlow<Boolean> = repository.isLoading
    val loadError: StateFlow<String?> = repository.errorMessage

    private val _uiState = MutableStateFlow(
        UiState(isDarkMode = repository.isDarkModeEnabled())
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Dynamic list of categories from loaded channels
    val categories: StateFlow<List<String>> = repository.channels.combine(repository.favoriteIds) { channels, favs ->
        val groups = channels.map { it.displayCategory }.distinct().sorted()
        val list = mutableListOf("All")
        if (favs.isNotEmpty()) {
            list.add("⭐ Favorites")
        }
        list.addAll(groups)
        list.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    // Filtered channel list based on category and search query
    val filteredChannels: StateFlow<List<Channel>> = combine(
        repository.channels,
        _uiState
    ) { channels, state ->
        val filtered = channels.filter { channel ->
            // Category filter
            val matchesCategory = when {
                state.selectedCategory.equals("All", ignoreCase = true) -> true
                state.selectedCategory.contains("Favorites") -> channel.isFavorite
                else -> channel.displayCategory.equals(state.selectedCategory, ignoreCase = true)
            }

            // Search query filter
            val matchesSearch = if (state.searchQuery.isBlank()) {
                true
            } else {
                channel.name.contains(state.searchQuery, ignoreCase = true) ||
                channel.displayCategory.contains(state.searchQuery, ignoreCase = true)
            }

            matchesCategory && matchesSearch
        }

        // When "All" category is selected, favorite channels come First
        if (state.selectedCategory.equals("All", ignoreCase = true)) {
            filtered.sortedByDescending { it.isFavorite }
        } else {
            filtered
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadChannels()
    }

    fun loadChannels(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            repository.loadChannels(forceRefresh)
            // Auto play the first channel if none selected (preferring favorite first since favorites come first on All)
            if (_uiState.value.selectedChannel == null && repository.channels.value.isNotEmpty()) {
                val channels = repository.channels.value
                val firstChannel = channels.firstOrNull { it.isFavorite } ?: channels.first()
                selectChannel(firstChannel, keepDrawerOpen = false)
            }
        }
    }

    fun selectChannel(channel: Channel, keepDrawerOpen: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            selectedChannel = channel,
            isCategoryDrawerOpen = if (keepDrawerOpen) _uiState.value.isCategoryDrawerOpen else false
        )
        playerManager.playStream(channel.streamUrl, channel.name)
    }

    fun selectNextChannel() {
        val activeList = if (filteredChannels.value.isNotEmpty()) {
            filteredChannels.value
        } else {
            repository.channels.value.let { list ->
                if (_uiState.value.selectedCategory.equals("All", ignoreCase = true)) {
                    list.sortedByDescending { it.isFavorite }
                } else {
                    list
                }
            }
        }
        if (activeList.isEmpty()) return

        val current = _uiState.value.selectedChannel
        val currentIndex = activeList.indexOfFirst { it.id == current?.id || it.streamUrl == current?.streamUrl }
        val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % activeList.size
        selectChannel(activeList[nextIndex], keepDrawerOpen = _uiState.value.isCategoryDrawerOpen)
    }

    fun selectPreviousChannel() {
        val activeList = if (filteredChannels.value.isNotEmpty()) {
            filteredChannels.value
        } else {
            repository.channels.value.let { list ->
                if (_uiState.value.selectedCategory.equals("All", ignoreCase = true)) {
                    list.sortedByDescending { it.isFavorite }
                } else {
                    list
                }
            }
        }
        if (activeList.isEmpty()) return

        val current = _uiState.value.selectedChannel
        val currentIndex = activeList.indexOfFirst { it.id == current?.id || it.streamUrl == current?.streamUrl }
        val prevIndex = when {
            currentIndex == -1 -> 0
            currentIndex - 1 < 0 -> activeList.size - 1
            else -> currentIndex - 1
        }
        selectChannel(activeList[prevIndex], keepDrawerOpen = _uiState.value.isCategoryDrawerOpen)
    }

    fun toggleFavorite(channel: Channel) {
        repository.toggleFavorite(channel)
        if (_uiState.value.selectedChannel?.id == channel.id) {
            _uiState.value = _uiState.value.copy(
                selectedChannel = _uiState.value.selectedChannel?.copy(isFavorite = !channel.isFavorite)
            )
        }
    }

    fun setCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleQualityDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(isQualityDialogVisible = show)
    }

    fun toggleCategoryDrawer(open: Boolean? = null) {
        val next = open ?: !_uiState.value.isCategoryDrawerOpen
        _uiState.value = _uiState.value.copy(isCategoryDrawerOpen = next)
    }

    fun toggleDarkMode() {
        val newDark = !_uiState.value.isDarkMode
        repository.setDarkModeEnabled(newDark)
        _uiState.value = _uiState.value.copy(isDarkMode = newDark)
    }

    fun setVideoQuality(quality: VideoQuality) {
        playerManager.setVideoQuality(quality)
        toggleQualityDialog(false)
    }

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun toggleMute() = playerManager.toggleMute()
    fun setVolume(vol: Float) = playerManager.setVolume(vol)
    fun toggleResizeMode() = playerManager.toggleResizeMode()
    fun setFullscreen(fullscreen: Boolean) = playerManager.setFullscreen(fullscreen)
    fun toggleFullscreen() = playerManager.toggleFullscreen()
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
