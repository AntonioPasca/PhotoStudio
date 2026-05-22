// --------------------------------------------------------------------------
// Language:    Kotlin
//
// Framework:   Google Jetpack Compose
//
// Package:     com.pascagames.photostudio
//
// Author:      Antonio Pascarella
//
// Version:     Rel. 0.6.0
//
// Date:        May 2026
//
// Module:      SettingsActivity.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme

const val SETTINGS_PHOTO_INDEX = 0
const val SETTINGS_VIDEO_INDEX = 1

// --------------------------------------------------------------------------
// CLASS SettingsActivity
// --------------------------------------------------------------------------
class SettingsActivity : ComponentActivity() {

    private var backToCaller: (Unit) -> Unit = { back() }

    // ----------------------------------------------------------------------
    // onCreate
    // ----------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get vars from Main
        val bundle = intent.getBundleExtra("activity_data")
        val settingIdx = bundle!!.getInt("SETTINGS_INDEX")
        var barTitle = ""
        when (settingIdx) {
            SETTINGS_PHOTO_INDEX -> barTitle = "Settings (Photo)"
            SETTINGS_VIDEO_INDEX -> barTitle = "Settings (Video)"
        }

        setContent {
            PhotoStudioTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { TopBar(barTitle, backToCaller) }) { innerPadding ->
                        MainScreen(
                            modifier = Modifier.padding(innerPadding),
                            settingIdx)
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // back
    // ----------------------------------------------------------------------
    fun back() {
        finish()
    }

    // ----------------------------------------------------------------------
    // MainScreen
    // ----------------------------------------------------------------------
    @Composable
    fun MainScreen(modifier: Modifier = Modifier, idx: Int) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 0.dp, 150.dp)
        ) {
            when (idx) {
                SETTINGS_PHOTO_INDEX ->   PhotoSettings()
                SETTINGS_VIDEO_INDEX ->   VideoSettings()
            }
        }
    }

    // ----------------------------------------------------------------------
    // PhotoSettings
    // ----------------------------------------------------------------------
    @Composable
    fun PhotoSettings() {

        var photoBeepEnabled by remember { mutableStateOf(Settings.photoBeepEnabled) }
        var photoDelayBeepEnabled by remember { mutableStateOf(Settings.photoDelayBeepEnabled) }
        var photoStackingBeepEnabled by remember { mutableStateOf(Settings.photoStackingBeepEnabled) }
        var photoPath by remember { mutableStateOf(Settings.photoPath) }

        // Photo Settings
        // ------------------------------------------------------

        // Switch - Beep after a photo has been taken
        SettingSwitch(
            label = "Photo beep",
            description = "(Beep enabled when taking a photo)",
            value = photoBeepEnabled,
            onValueChange = {
                photoBeepEnabled = !photoBeepEnabled
                Settings.photoBeepEnabled = photoBeepEnabled
            }
        )

        // Switch - Beep during the delay
        SettingSwitch(
            label = "Photo beep during delay",
            description = "(Beep enabled during photo delay)",
            value = photoDelayBeepEnabled,
            onValueChange = {
                photoDelayBeepEnabled = !photoDelayBeepEnabled
                Settings.photoDelayBeepEnabled = photoDelayBeepEnabled
            }
        )

        // Switch - Beep when stacking is finished
        SettingSwitch(
            label = "Stacking beep",
            description = "(Beep enabled when stacking ended)",
            value = photoStackingBeepEnabled,
            onValueChange = {
                photoStackingBeepEnabled = !photoStackingBeepEnabled
                Settings.photoStackingBeepEnabled = photoStackingBeepEnabled
            }
        )

        // Numeric Up-Down - Delay in seconds
        var delayPhoto by remember { mutableIntStateOf(Settings.photoBeepDelay) }
        NumericUpDown(
            value = delayPhoto,
            onValueChange = {
                delayPhoto = it
                Settings.photoBeepDelay = delayPhoto
            },
            min = 0,
            max = 10,
            description =  "Delay (in sec)"
        )

        // Numeric Up-Down - Num of multiple photos
        var numMultiplePhotos by remember { mutableIntStateOf(Settings.photoNumMultiple) }
        NumericUpDown(
            value = numMultiplePhotos,
            onValueChange = {
                numMultiplePhotos = it
                Settings.photoNumMultiple = numMultiplePhotos
            },
            min = 1,
            max = 100,
            description =  "Num of multiple photos"
        )

        // Numeric Up-Down - Delay between successive photos
        var delayBetweenPhotos by remember { mutableLongStateOf(Settings.delayBetweenPhotos) }
        NumericUpDown(
            value = delayBetweenPhotos.toInt(),
            onValueChange = {
                delayBetweenPhotos = it.toLong()
                Settings.delayBetweenPhotos = delayBetweenPhotos
            },
            min = 100,
            max = 5000,
            description =  "Num of multiple photos"
        )

        // Location to save photos
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                .padding(16.dp)
        ) {
            Text("Photo path = $photoPath",)
        }
    }

    // ----------------------------------------------------------------------
    // VideoSettings
    // ----------------------------------------------------------------------
    @Composable
    fun VideoSettings() {

        var videoStartBeepEnabled by remember { mutableStateOf(Settings.videoStartBeepEnabled) }
        var videoStopBeepEnabled by remember { mutableStateOf(Settings.videoStopBeepEnabled) }
        var videoDelayBeepEnabled by remember { mutableStateOf(Settings.videoDelayBeepEnabled) }
        var videoPath by remember { mutableStateOf(Settings.videoPath) }

        // Video Settings
        // ---------------------------------------------
        SettingSwitch(
            label = "Start video beep",
            description = "(Beep enabled when starting a video)",
            value = videoStartBeepEnabled,
            onValueChange = {
                videoStartBeepEnabled = !videoStartBeepEnabled
                Settings.videoStartBeepEnabled = videoStartBeepEnabled
            }
        )
        SettingSwitch(
            label = "Stop video beep",
            description = "(Beep enabled when stopping a video)",
            value = videoStopBeepEnabled,
            onValueChange = {
                videoStopBeepEnabled = !videoStopBeepEnabled
                Settings.videoStopBeepEnabled = videoStopBeepEnabled
            }
        )

        // Switch - Beep during the delay
        SettingSwitch(
            label = "Video beep during delay",
            description = "(Beep enabled during video delay)",
            value = videoDelayBeepEnabled,
            onValueChange = {
                videoDelayBeepEnabled = !videoDelayBeepEnabled
                Settings.videoDelayBeepEnabled = videoDelayBeepEnabled
            }
        )

        // Numeric Up-Down - Delay in seconds
        var delayVideo by remember { mutableIntStateOf(Settings.videoBeepDelay) }
        NumericUpDown(
            value = delayVideo,
            onValueChange = {
                delayVideo = it
                Settings.videoBeepDelay = delayVideo
            },
            min = 0,
            max = 10,
            description =  "Delay (in sec)",
        )

        // Location to save photos
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                .padding(16.dp)
        ) {
            Text("Video path = $videoPath",)
        }
    }
}

// --------------------------------------------------------------------------
// OBJECT SettingsActivity
// --------------------------------------------------------------------------
object Settings {

    var photoBeepEnabled = true
    var photoDelayBeepEnabled = false
    var photoStackingBeepEnabled = true
    var photoBeepDelay = 5
    var photoNumMultiple = 3
    var delayBetweenPhotos = 500L    // (in ms)
    var photoPath = "Pictures/CameraX"

    var videoStartBeepEnabled = true
    var videoStopBeepEnabled = true
    var videoDelayBeepEnabled = false
    var videoBeepDelay = 5
    var videoPath = Environment.DIRECTORY_MOVIES!!
}
