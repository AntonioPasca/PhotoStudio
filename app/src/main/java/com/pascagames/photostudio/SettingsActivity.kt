// --------------------------------------------------------------------------
// Language:    Kotlin
//
// Framework:   Google Jetpack Compose
//
// Package:     com.pascagames.photostudio
//
// Author:      Antonio Pascarella
//
// Version:     Rel. 0.3.0
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme

// --------------------------------------------------------------------------------------------
// SettingsActivity
// --------------------------------------------------------------------------------------------
class SettingsActivity : ComponentActivity() {

    private var backToCaller: (Unit) -> Unit = { back() }

    // --------------------------------------------------------------------------------------------
    // onCreate
    // --------------------------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PhotoStudioTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { TopBar("Settings", backToCaller) }) { innerPadding ->
                    MainScreen(
                        //modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    // --------------------------------------------------------------------------
    // back
    // --------------------------------------------------------------------------
    fun back() {
        finish()
    }

    // --------------------------------------------------------------------------
    // MainScreen
    // --------------------------------------------------------------------------
    @Composable
    fun MainScreen() {

        var photoBeepEnabled by remember { mutableStateOf(Settings.photoBeepEnabled) }
        var photoDelayBeepEnabled by remember { mutableStateOf(Settings.photoDelayBeepEnabled) }
        var photoPath by remember { mutableStateOf(Settings.photoPath) }

        var videoStartBeepEnabled by remember { mutableStateOf(Settings.videoStartBeepEnabled) }
        var videoStopBeepEnabled by remember { mutableStateOf(Settings.videoStopBeepEnabled) }
        var videoPath by remember { mutableStateOf(Settings.videoPath) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 0.dp, 150.dp)
        ) {
            // Photo Settings
            Text(
                "Photo",
                style = TextStyle(fontSize = 22.sp)
            )

            SettingSwitch(
                label = "Photo beep",
                description = "(Beep enabled when taking a photo)",
                value = photoBeepEnabled,
                onValueChange = {
                                    photoBeepEnabled = !photoBeepEnabled
                                    Settings.photoBeepEnabled = photoBeepEnabled
                }
            )
            SettingSwitch(
                label = "Photo beep during delay",
                description = "(Beep enabled before taking a photo)",
                value = photoDelayBeepEnabled,
                onValueChange = {
                    photoDelayBeepEnabled = !photoDelayBeepEnabled
                    Settings.photoDelayBeepEnabled = photoDelayBeepEnabled
                }
            )
            Text(
                "Photo path = $photoPath",
                Modifier.padding(start = 10.dp, top=20.dp),
            )

            // Video Settings
            Text(
                "Video",
                Modifier.padding(top=30.dp),
                style = TextStyle(fontSize = 22.sp)
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
            Text(
                "Video path = $videoPath",
                Modifier.padding(start = 20.dp, top=20.dp)
            )
        }
    }
}

object Settings {

    var photoBeepEnabled = true
    var photoDelayBeepEnabled = false
    var photoPath = "Pictures/CameraX"

    var videoStartBeepEnabled = true
    var videoStopBeepEnabled = true
    var videoPath = Environment.DIRECTORY_MOVIES
}
