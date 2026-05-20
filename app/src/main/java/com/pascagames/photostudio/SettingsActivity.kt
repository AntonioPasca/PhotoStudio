// --------------------------------------------------------------------------
// Language:    Kotlin
//
// Framework:   Google Jetpack Compose
//
// Package:     com.pascagames.photostudio
//
// Author:      Antonio Pascarella
//
// Version:     Rel. 0.5.0
//
// Date:        May 2026
//
// Module:      SettingsActivity.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

    private var gSetIndex: Int = 0
    private var backToCaller: (Unit) -> Unit = { back() }

    // ----------------------------------------------------------------------
    // onCreate
    // ----------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        // Get vars from Main
        val bundle = intent.getBundleExtra("activity_data")
        gSetIndex = bundle!!.getInt("SETTINGS_INDEX")

        setContent {
            PhotoStudioTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { TopBar("Settings", backToCaller) }) { innerPadding ->
                    MainScreen()
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
    fun MainScreen() {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 0.dp, 150.dp)
        ) {

            Log.v(TAG, "SA")
            Log.v(TAG, gSetIndex.toString())

            if (gSetIndex == SETTINGS_PHOTO_INDEX) {
                PhotoSettings()
            }

            if (gSetIndex == SETTINGS_VIDEO_INDEX) {
                VideoSettings()
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
        Text(
            "Photo",
            style = TextStyle(fontSize = 32.sp)
        )

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
            description = "(Beep enabled before taking a photo)",
            value = photoDelayBeepEnabled,
            onValueChange = {
                photoDelayBeepEnabled = !photoDelayBeepEnabled
                Settings.photoDelayBeepEnabled = photoDelayBeepEnabled
            }
        )

        // Switch - Beep when stacking is finished
        SettingSwitch(
            label = "Stacking beep",
            description = "(Beep enabled when staking ended)",
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
            modifier = Modifier.padding(start = 16.dp),
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
            modifier = Modifier.padding(start = 16.dp),
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
            modifier = Modifier.padding(start = 16.dp),
            min = 100,
            max = 5000,
            description =  "Num of multiple photos"
        )

        // Location to save photos
        Text(
            "Photo path = $photoPath",
            Modifier.padding(start = 16.dp, top=20.dp),
        )
    }

    // ----------------------------------------------------------------------
    // VideoSettings
    // ----------------------------------------------------------------------
    @Composable
    fun VideoSettings() {

        var videoStartBeepEnabled by remember { mutableStateOf(Settings.videoStartBeepEnabled) }
        var videoStopBeepEnabled by remember { mutableStateOf(Settings.videoStopBeepEnabled) }
        var videoPath by remember { mutableStateOf(Settings.videoPath) }

        // Video Settings
        // ---------------------------------------------
        Text(
            "Video",
            Modifier.padding(top=30.dp),
            style = TextStyle(fontSize = 32.sp)
        )
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

        // Numeric Up-Down - Delay in seconds
        var delayVideo by remember { mutableIntStateOf(Settings.videoBeepDelay) }
        NumericUpDown(
            value = delayVideo,
            onValueChange = {
                delayVideo = it
                Settings.videoBeepDelay = delayVideo
            },
            modifier = Modifier.padding(start = 16.dp),
            min = 0,
            max = 10,
            description =  "Delay (in sec)",
        )

        // Location to save videos
        Text(
            "Video path = $videoPath",
            Modifier.padding(start = 16.dp, top=20.dp)
        )
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
    var videoBeepDelay = 5
    var videoPath = Environment.DIRECTORY_MOVIES!!
}
