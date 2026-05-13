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
// Module:      MainActivity.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.content.Intent
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
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.video.Recording
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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

private const val APP_NAME = "PhotoStudio"
private const val VERSION =  "Ver 0.3.5"
const val TAG = "PHOTO"

private lateinit var activeRecording: Recording
// --------------------------------------------------------------------------
// MainActivity
// --------------------------------------------------------------------------
class MainActivity : ComponentActivity() {

    var photo = Photo()

    // ----------------------------------------------------------------------
    // onCreate
    // ----------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PhotoStudioTheme {
                Scaffold { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // settings
    // ----------------------------------------------------------------------
    fun settings() {

        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
        startActivity(intent)
    }

    // ----------------------------------------------------------------------
    // info
    // ----------------------------------------------------------------------
    fun info() {

        val intent = Intent(this@MainActivity, InfoActivity::class.java)
        val bundle = Bundle()
        bundle.putString("APP", APP_NAME)
        bundle.putString("VERSION", VERSION)
        intent.putExtra("main_activity_data", bundle)
        startActivity(intent)
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
        var showDelayedPhoto by remember {mutableStateOf(false)}
        var showDelayedVideo by remember {mutableStateOf(false)}
        var showVideoProgress by remember {mutableStateOf(false)}

        if (showDelayedPhoto) {
            TakeDelayedPhoto(
                controller = controller,
                onFinished = {showDelayedPhoto = false})
        }

        if (showDelayedVideo) {
            TakeDelayedVideo(
                controller = controller,
                onDelayEnded = {showDelayedVideo = false},
                onRecInProgress = {
                    showVideoProgress = true
                    Log.v(TAG, "PROG")
                },
                onRecEnded = {
                    //showDelayedVideo = false
                    showVideoProgress = false
                }
            )
        }
        if (showVideoProgress) {
            ShowRecTime()
        }

        LaunchedEffect(Unit) {
            controller.bindToLifecycle(lifecycleOwner)
        }

        getCameraPermission()

        Scaffold(
            bottomBar = {
                BottomBar(
                    {showDelayedPhoto = true},
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
                    Log.v(TAG, timeVideo.toString())

                }
        }
        //Text(timeVideo.toString())
        Toast.makeText(LocalContext.current, timeVideo.toString(), Toast.LENGTH_SHORT).show()
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
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    this.controller = controller
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier.fillMaxSize()
        )
    }

    // ----------------------------------------------------------------------
    // rememberCameraController
    // ----------------------------------------------------------------------
    @Composable
    fun rememberCameraController(context: Context): LifecycleCameraController {
        return remember {
            LifecycleCameraController(context).apply {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                setEnabledUseCases(
                    CameraController.IMAGE_CAPTURE or
                    CameraController.VIDEO_CAPTURE
                )
            }
        }
    }

    // ----------------------------------------------------------------------
    // TakeDelayedPhoto
    // ----------------------------------------------------------------------
    @Composable
    fun TakeDelayedPhoto(
        controller: LifecycleCameraController,
        onFinished: () -> Unit
    ) {
        var delay by remember { mutableIntStateOf(Settings.photoBeepDelay) }
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

            photo.takePhoto(context, controller)
            onFinished()
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
    // TakeDelayedVideo
    // ----------------------------------------------------------------------
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Composable
    fun TakeDelayedVideo(
        controller: LifecycleCameraController,
        onDelayEnded:() -> Unit,
        onRecInProgress: () -> Unit,
        onRecEnded: () -> Unit) {

        var delay by remember { mutableIntStateOf(Settings.videoBeepDelay) }
        val context = LocalContext.current

        var showVideoProgress by remember { mutableStateOf(false) }
        var countVideoProgress by remember { mutableIntStateOf(0) }

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

            activeRecording = photo.startRecording(
                controller, context,
                onRecStarted = { Toast.makeText(context, "REC", Toast.LENGTH_SHORT).show() },
                /*onRecStarted = {
                                    CustomToast(
                                        "REC",
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(24.dp)
                                    )
                                },*/
                onRecFinished = {
                    Toast.makeText(context, "Saved Video", Toast.LENGTH_SHORT).show()
                    onRecEnded()
                }
            )
            onDelayEnded()
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
        onPhoto: () -> Unit,
        onVideo: () -> Unit){

        var isRecording by remember { mutableStateOf(false) }

        NavigationBar {
            NavigationBarItem(
                selected = true,
                onClick = onPhoto,
                icon = {
                    Icon(
                        painterResource(id = R.drawable.camera),
                        contentDescription = null
                    )
                },
                label = { Text("Photo") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Green,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
            NavigationBarItem(
                selected = true,
                onClick = {
                    if (!isRecording) {
                        onVideo()
                    }
                    else {
                        photo.stopRecording(activeRecording)
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
            NavigationBarItem(
                selected = true,
                onClick = { settings() },
                icon = {
                    Icon(
                        painterResource(id = R.drawable.settings),
                        contentDescription = null
                    )
                },
                label = { Text("Settings") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Green,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                selected = true,
                onClick = { info() },
                icon = {
                    Icon(
                        painterResource(id = R.drawable.info),
                        contentDescription = null
                    )
                },
                label = { Text("Info") },
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