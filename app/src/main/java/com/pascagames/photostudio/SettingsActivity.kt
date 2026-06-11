// --------------------------------------------------------------------------
// Language:    Kotlin
//
// Framework:   Google Jetpack Compose
//
// Package:     com.pascagames.photostudio
//
// Author:      Antonio Pascarella
//
// Version:     Rel. 0.8.0
//
// Date:        June 2026
//
// Module:      SettingsActivity.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme

const val SETTINGS_PHOTO_INDEX = 0
const val SETTINGS_VIDEO_INDEX = 1
const val SETTINGS_STACKER_INDEX = 2

// --------------------------------------------------------------------------
// CLASS SettingsActivity
// --------------------------------------------------------------------------
class SettingsActivity : ComponentActivity() {

    private var backToCaller: (Unit) -> Unit = { back() }
    val cameraLib = CameraLib()

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
            SETTINGS_STACKER_INDEX -> barTitle = "Settings (Stacker)"
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

        var rawCanBeEnabled = false
        if (cameraLib.isRawSupported(LocalContext.current, "0"))
            rawCanBeEnabled = true

        var cameras by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            cameras = cameraLib.listAvailableCameras(context)
            Log.v(TAG, cameras.toString())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 0.dp, 150.dp)
        ) {
            when (idx) {
                SETTINGS_PHOTO_INDEX ->   PhotoSettings(rawCanBeEnabled)
                SETTINGS_VIDEO_INDEX ->   VideoSettings()
                SETTINGS_STACKER_INDEX ->   StackerSettings()
            }
        }
    }

    // ----------------------------------------------------------------------
    // PhotoSettings
    // ----------------------------------------------------------------------
    @Composable
    fun PhotoSettings(rawCanBeEnabled: Boolean) {

        // Handle Settings errors
        var settingError by remember { mutableStateOf(false) }

        if (settingError) {
            Toast.makeText(
                LocalContext.current,
                "RAW format not handled by this device",
                Toast.LENGTH_SHORT
            ).show()
            settingError = false
        }

        // Photo Settings
        // ------------------------------------------------------

        // Switch - Back or Front Camera
        var photoBackCamera by remember { mutableStateOf(Settings.photoBackCamera) }
        SettingSwitch(
            label = "Back camera",
            description = "(Use back camera)",
            value = photoBackCamera,
            onValueChange = {
                photoBackCamera = !photoBackCamera
                Settings.photoBackCamera = photoBackCamera
            }
        )

        // Switch - Beep after a photo has been taken
        var photoBeepEnabled by remember { mutableStateOf(Settings.photoBeepEnabled) }
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
        var photoDelayBeepEnabled by remember { mutableStateOf(Settings.photoDelayBeepEnabled) }
        SettingSwitch(
            label = "Photo beep during delay",
            description = "(Beep enabled during photo delay)",
            value = photoDelayBeepEnabled,
            onValueChange = {
                photoDelayBeepEnabled = !photoDelayBeepEnabled
                Settings.photoDelayBeepEnabled = photoDelayBeepEnabled
            }
        )

        // Switch - RAW Enable (if available in the device)
        var initValue = Settings.photoRawEnabled
        if (!rawCanBeEnabled)
            initValue = false

        var photoRawEnabled by remember { mutableStateOf(initValue) }
        SettingSwitch(
            label = "Enable DNG format (RAW)",
            description = "(DNG Format enabled)",
            value = photoRawEnabled,
            onValueChange = {
                if (rawCanBeEnabled) {
                    photoRawEnabled = !photoRawEnabled
                    Settings.photoRawEnabled = photoRawEnabled
                }
                else {
                    settingError = true
                }
            }
        )

        // Switch - Show histogram
        var photoShowHistogram by remember { mutableStateOf(Settings.photoShowHistogram) }
        SettingSwitch(
            label = "Histogram",
            description = "(Show or hide histogram)",
            value = photoShowHistogram,
            onValueChange = {
                photoShowHistogram = !photoShowHistogram
                Settings.photoShowHistogram = photoShowHistogram
            }
        )

        // Numeric Up-Down - Delay in seconds
        var photoBeepDelay by remember { mutableIntStateOf(Settings.photoBeepDelay) }
        NumericUpDown(
            value = photoBeepDelay,
            onValueChange = {
                photoBeepDelay = it
                Settings.photoBeepDelay = photoBeepDelay
            },
            min = 0,
            max = 10,
            description =  "Delay (in sec)"
        )

        // Numeric Up-Down - Num of multiple photos
        var photNumMultiple by remember { mutableIntStateOf(Settings.photoNumMultiple) }
        NumericUpDown(
            value = photNumMultiple,
            onValueChange = {
                photNumMultiple = it
                Settings.photoNumMultiple = photNumMultiple
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
            description =  "Delay between photos"
        )

        // Location to save photos
        var photoPath by remember { mutableStateOf(Settings.photoPath) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                .padding(16.dp)
        ) {
            Text("Photo path = $photoPath")
        }
    }

    // ----------------------------------------------------------------------
    // VideoSettings
    // ----------------------------------------------------------------------
    @Composable
    fun VideoSettings() {

        // Video Settings
        // ---------------------------------------------

        // Switch - Beep when a video starts
        var videoStartBeepEnabled by remember { mutableStateOf(Settings.videoStartBeepEnabled) }
        SettingSwitch(
            label = "Start video beep",
            description = "(Beep enabled when starting a video)",
            value = videoStartBeepEnabled,
            onValueChange = {
                videoStartBeepEnabled = !videoStartBeepEnabled
                Settings.videoStartBeepEnabled = videoStartBeepEnabled
            }
        )

        // Switch - Beep when a video stops
        var videoStopBeepEnabled by remember { mutableStateOf(Settings.videoStopBeepEnabled) }
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
        var videoDelayBeepEnabled by remember { mutableStateOf(Settings.videoDelayBeepEnabled) }
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
        var videoPath by remember { mutableStateOf(Settings.videoPath) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                .padding(16.dp)
        ) {
            Text("Video path = $videoPath")
        }
    }

    // ----------------------------------------------------------------------
    // StackerSettings
    // ----------------------------------------------------------------------
    @Composable
    fun StackerSettings() {

        // Stacker Settings
        // ---------------------------------------------

        // Switch - Beep when stacking starts or ends
        var stackerBeepEnabled by remember { mutableStateOf(Settings.stackerBeepEnabled) }
        SettingSwitch(
            label = "Stacking beep",
            description = "(Beep enabled when stacking ended)",
            value = stackerBeepEnabled,
            onValueChange = {
                stackerBeepEnabled = !stackerBeepEnabled
                Settings.stackerBeepEnabled = stackerBeepEnabled
            }
        )

        // NumericUpDown - Max allowed shift when re-aligning photos
        var stackerMaxShift by remember { mutableIntStateOf(Settings.stackerMaxShift) }
        NumericUpDown(
            value = stackerMaxShift,
            onValueChange = {
                stackerMaxShift = it
                Settings.stackerMaxShift = stackerMaxShift
            },
            min = 10,
            max = 100,
            description =  "Max shift in alignment",
        )

        // NumericUpDown - Portion of the image to use to compute the shift
        var stackerAreaPercentage by remember { mutableIntStateOf(Settings.stackerAreaPercentage ) }
        NumericUpDown(
            value = stackerAreaPercentage,
            onValueChange = {
                stackerAreaPercentage = it
                Settings.stackerAreaPercentage = stackerAreaPercentage
            },
            min = 10,
            max = 75,
            description =  "Portion of area in alignment",
        )

        // NumericUpDown - Portion of the image to use to compute the shift
        var stackerSharpening by remember { mutableIntStateOf(Settings.stackerSharpening ) }
        NumericUpDown(
            value = stackerSharpening,
            onValueChange = {
                stackerSharpening = it
                Settings.stackerSharpening = stackerSharpening
            },
            min = 25,
            max = 45,
            description =  "Sharpening value",
        )
    }
}

// --------------------------------------------------------------------------
// OBJECT SettingsActivity
// --------------------------------------------------------------------------
object Settings {

    var photoBackCamera = true
    var photoBeepEnabled = true
    var photoDelayBeepEnabled = false
    var photoRawEnabled = true
    var photoShowHistogram = true
    var photoBeepDelay = 5
    var photoNumMultiple = 3
    var delayBetweenPhotos = 400L    // (in ms)
    var photoPath = "Pictures/AstroPhoto"

    var videoStartBeepEnabled = true
    var videoStopBeepEnabled = true
    var videoDelayBeepEnabled = false
    var videoBeepDelay = 5
    var videoPath = Environment.DIRECTORY_MOVIES!!

    var stackerBeepEnabled = true
    var stackerMaxShift = 10
    var stackerAreaPercentage = 25

    // Values
    // 0.25 natural
    // 0.35 more aggressive (Moon)
    // 0.45 planets with good seeing
    var stackerSharpening = 25
}
