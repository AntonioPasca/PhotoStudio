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
// Module:      MainActivity.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.camera.video.Recording
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.background
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

private lateinit var activeRecording: Recording

// --------------------------------------------------------------------------
// VideoActivity
// --------------------------------------------------------------------------
class VideoActivity : ComponentActivity() {

    private var backToCaller: (Unit) -> Unit = { back() }
    var cameraLib = CameraLib()

    // ----------------------------------------------------------------------
    // onCreate
    // ----------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val actions = listOf(
            TopBarAction.Settings { settings() }
        )

        setContent {
            PhotoStudioTheme {
                Scaffold(topBar = { TopBarEx("Video", actions,backToCaller) }) {
                    innerPadding ->
                        MainScreen(modifier = Modifier.padding(innerPadding))
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

    // ----------------------------------------------------------------------
    // settings
    // ----------------------------------------------------------------------
    fun settings() {

        val intent = Intent(this@VideoActivity, SettingsActivity::class.java)
        val bundle = Bundle()
        bundle.putInt("SETTINGS_INDEX", SETTINGS_VIDEO_INDEX)
        intent.putExtra("activity_data", bundle)
        startActivity(intent)
    }

    // ----------------------------------------------------------------------
    // MainScreen
    // ----------------------------------------------------------------------
    @SuppressLint("UnrememberedMutableState")
    @Composable
    fun MainScreen(modifier: Modifier = Modifier) {

        val context = LocalContext.current
        val controller = cameraLib.rememberCameraController(context)
        val lifecycleOwner = LocalLifecycleOwner.current
        var startDelayedVideo by remember {mutableStateOf(false)}
        var showVideoProgress by remember {mutableStateOf(false)}
        var showStartVideoMsg by remember {mutableStateOf(false)}
        val focusPeakingBitmap = mutableStateOf<Bitmap?>(null)

        val permissionsGranted = cameraLib.getAudioPermission() && cameraLib.getCameraPermission()
        val granted = cameraLib.getAudioPermission()

        if (startDelayedVideo) {

            StartDelayedVideo(
                controller = controller,
                permissionsGranted,
                onRecStarted = { showStartVideoMsg = true },
                onRecInProgress = {
                    startDelayedVideo = false
                    showVideoProgress = true
                },
                onRecEnded = {
                    showVideoProgress = false
                }
            )
        }

        LaunchedEffect(Unit) {
            controller.bindToLifecycle(lifecycleOwner)
        }

        Scaffold(
            bottomBar = {
                BottomBar(
                    onVideo = {startDelayedVideo = true}
               )
            }
        ) { innerPadding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                cameraLib.CameraPreview(
                    controller,
                    modifier = Modifier.fillMaxSize(),
                    focusPeakingBitmap = focusPeakingBitmap.value
                )

                if (showStartVideoMsg) {
                    CustomToast(
                        "REC Started",
                    )
                }

                if (showVideoProgress) {
                    ShowRecTime()
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // ShowRecTime
    // ----------------------------------------------------------------------
    @Composable
    fun ShowRecTime()
    {
        var timeVideo by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
                while(true) {
                    delay(1000)
                    timeVideo++
                }
        }

        // UI del countdown
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .zIndex(10f),                                   // important!
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatTime(timeVideo),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
        }
    }

    // ----------------------------------------------------------------------
    // StartDelayedVideo
    // ----------------------------------------------------------------------
    @Composable
    fun StartDelayedVideo(
        controller: LifecycleCameraController,
        permissionsGranted: Boolean,
        onRecStarted:    () -> Unit,
        onRecInProgress: () -> Unit,
        onRecEnded: () -> Unit) {

        var delay by remember { mutableIntStateOf(Settings.videoBeepDelay) }
        val context = LocalContext.current
        val audioConfig = remember { createSilentAudioConfig() }

        LaunchedEffect(Unit) {
            while (delay > 0) {
                delay(1000)
                delay--
                if (Settings.videoDelayBeepEnabled) {
                    beep(100,20)
                }
            }

            onRecInProgress()

            activeRecording = cameraLib.startRecording(
                controller, context,
                audioConfig,
                onRecStarted = { onRecStarted() },
                onRecFinished = {
                    Toast.makeText(context, "Saved Video", Toast.LENGTH_SHORT).show()
                    onRecEnded()
                }
            )
        }

        // UI del countdown
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .zIndex(10f),                                   // important!
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = delay.toString(),
                fontSize = 100.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    // ----------------------------------------------------------------------
    // BottomBar
    // ----------------------------------------------------------------------
    @Composable
    fun BottomBar(
        onVideo: () -> Unit) {

        var isRecording by remember { mutableStateOf(false) }

        NavigationBar {

            NavigationBarItem(
                selected = true,
                onClick = {
                    if (!isRecording) {
                        onVideo()
                    } else {
                        cameraLib.stopRecording(activeRecording)
                    }
                    isRecording = !isRecording
                },
                icon = {
                    var iconId = R.drawable.videoon
                    if (isRecording)
                        iconId = R.drawable.videooff
                    Icon(
                        painterResource(id = iconId),
                        contentDescription = null
                    )
                },
                label = {
                    if (!isRecording)
                        Text("Video Start")
                    else
                        Text("Video Stop")
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Green,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }

    // ----------------------------------------------------------------------
    // createSilentAudioConfig
    // ----------------------------------------------------------------------
    // It seems CameraX has a bug.
    // Any call to "AudioConfig.create(false)" in any context
    // generates a warning
    // ----------------------------------------------------------------------
    @SuppressLint("MissingPermission")
    fun createSilentAudioConfig(): AudioConfig {
        return AudioConfig.create(false)
    }
}
