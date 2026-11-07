package com.example

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ChannelViewModel
import com.example.ui.HomeScreen

class MainActivity : ComponentActivity() {

    private var channelViewModel: ChannelViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: ChannelViewModel = viewModel()
            channelViewModel = vm
            HomeScreen(
                viewModel = vm,
                onEnterPip = { enterPipMode() }
            )
            UpdateGate()
        }
    }

    fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(16, 9)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()
            try {
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                // If PiP is not supported or failed on device, continue gracefully
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val vm = channelViewModel ?: return
        if (vm.isPipEnabled.value && vm.playerState.value.isPlaying) {
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        channelViewModel?.setPipModeActive(isInPictureInPictureMode)
    }

    override fun onStop() {
        super.onStop()
        val vm = channelViewModel ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) {
            return
        }
        if (!vm.isBackgroundPlay.value) {
            vm.playerManager.exoPlayer.pause()
        }
    }
}
