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
// Module:      MainActivity.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.ZoomState
import androidx.camera.video.Recording
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import androidx.compose.runtime.livedata.observeAsState

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

        setContent {
            PhotoStudioTheme {
                Scaffold(topBar = { TopBar("Video", backToCaller) }) {
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
    // MainScreen
    // ----------------------------------------------------------------------
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Composable
    fun MainScreen(modifier: Modifier = Modifier) {

        val context = LocalContext.current
        val controller = rememberCameraController(context)
        val lifecycleOwner = LocalLifecycleOwner.current
        var showDelayedVideo by remember {mutableStateOf(false)}
        var showVideoProgress by remember {mutableStateOf(false)}
        var showStartVideoMsg by remember {mutableStateOf(false)}

        if (showDelayedVideo) {
            TakeDelayedVideo(
                controller = controller,
                onRecStarted = {showStartVideoMsg = true},
                onRecInProgress = {
                    showDelayedVideo = false
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

        getCameraPermission()

        Scaffold(
            bottomBar = {
                BottomBar(
                    onVideo = {showDelayedVideo = true}
               )
            }
        ) { innerPadding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                CameraPreview(
                    controller,
                    modifier = Modifier.fillMaxSize()
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
    // getCameraPermission
    // ----------------------------------------------------------------------
    @Composable
    fun getCameraPermission(): Boolean {

        var result = false
        val context = LocalContext.current
        val permission = Manifest.permission.CAMERA

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                result = true
            }
        }

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(permission)
            }
        }
        return result
    }

    // ----------------------------------------------------------------------
    // getAudioPermission
    // ----------------------------------------------------------------------
    @Composable
    fun getAudioPermission(): Boolean {

        var result = false
        val context = LocalContext.current
        val permission = Manifest.permission.RECORD_AUDIO

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                result = true
            }
        }

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(permission)
            }
        }
        return result
    }

    // ----------------------------------------------------------------------
    // CameraPreview
    // ----------------------------------------------------------------------
    @Composable
    fun CameraPreview(
        controller: LifecycleCameraController,
        modifier: Modifier
    ) {
        val zoomState: ZoomState? by controller.zoomState.observeAsState()

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        this.controller = controller
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier.fillMaxSize()
            )

            // Slider di zoom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            ) {
                Slider(
                    value = zoomState?.linearZoom ?: 0f,
                    onValueChange = { controller.setLinearZoom(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Zoom: ${"%.1f".format(zoomState?.zoomRatio ?: 1f)}x",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    // ----------------------------------------------------------------------
    // rememberCameraController
    // ----------------------------------------------------------------------
    @Composable
    fun rememberCameraController(context: Context): LifecycleCameraController {
        val controller =  remember {
            LifecycleCameraController(context).apply {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                setEnabledUseCases(
                    CameraController.IMAGE_CAPTURE or
                            CameraController.VIDEO_CAPTURE
                )
            }
        }

        controller.isPinchToZoomEnabled = true

        return controller
    }

    // ----------------------------------------------------------------------
    // TakeDelayedVideo
    // ----------------------------------------------------------------------
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Composable
    fun TakeDelayedVideo(
        controller: LifecycleCameraController,
        onRecStarted:    () -> Unit,
        onRecInProgress: () -> Unit,
        onRecEnded: () -> Unit) {

        var delay by remember { mutableIntStateOf(Settings.videoBeepDelay) }
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            while (delay > 0) {
                delay(1000)
                delay--
                if (Settings.photoDelayBeepEnabled) {
                    val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 20)
                }
            }

            onRecInProgress()

            activeRecording = cameraLib.startRecording(
                controller, context,
                onRecStarted =  {onRecStarted()},
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
        onVideo: () -> Unit){

        var isRecording by remember { mutableStateOf(false) }

        NavigationBar {

            NavigationBarItem(
                selected = true,
                onClick = {
                    if (!isRecording) {
                        onVideo()
                    }
                    else {
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
                label = {  if (!isRecording)
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

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        PhotoStudioTheme {
            MainScreen()
        }
    }
}