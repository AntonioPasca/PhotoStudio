// --------------------------------------------------------------------------
// Language:    Kotlin
//
// Framework:   Google Jetpack Compose
//
// Package:     com.pascagames.photostudio
//
// Author:      Antonio Pascarella
//
// Version:     Rel. 0.2.0
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme
import java.io.File
import android.os.Environment
import androidx.annotation.RequiresPermission
import androidx.camera.view.video.AudioConfig

private const val APP_NAME = "PhotoStudio"
private const val VERSION =  "Ver 0.2.1"

// ------------------------------------------------------------------------------------------------
// MainActivity
// ------------------------------------------------------------------------------------------------
class MainActivity : ComponentActivity() {

    var photo = Photo()

    // --------------------------------------------------------------------------------------------
    // onCreate
    // --------------------------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PhotoStudioTheme {
                Scaffold(
                ) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    // --------------------------------------------------------------------------------------------
    // settings
    // --------------------------------------------------------------------------------------------
    fun settings() {

    }

    // --------------------------------------------------------------------------------------------
    // info
    // --------------------------------------------------------------------------------------------
    fun info() {

        val intent = Intent(this@MainActivity, InfoActivity::class.java)
        val bundle = Bundle()
        bundle.putString("APP", APP_NAME)
        bundle.putString("VERSION", VERSION)
        intent.putExtra("main_activity_data", bundle)
        startActivity(intent)
    }

    // --------------------------------------------------------------------------------------------
    // MainScreen
    // --------------------------------------------------------------------------------------------
    @Composable
    fun MainScreen(modifier: Modifier = Modifier) {

        val context = LocalContext.current
        val controller = rememberCameraController(context)
        val lifecycleOwner = LocalLifecycleOwner.current

        LaunchedEffect(Unit) {
            controller.bindToLifecycle(lifecycleOwner)
        }

        getCameraPermission()

        Scaffold(
            bottomBar = {
                BottomBar(controller)
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

    // --------------------------------------------------------------------------------------------
    // getCameraPermission
    // --------------------------------------------------------------------------------------------
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

    // --------------------------------------------------------------------------------------------
    // startRecording
    // --------------------------------------------------------------------------------------------
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(
        controller: LifecycleCameraController,
        context: Context,
        onRecStarted: () -> Unit,
        onRecFinished: () -> Unit
    ): Recording {

        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "VID_${System.currentTimeMillis()}.mp4"
        )

        val outputOptions = FileOutputOptions.Builder(file).build()

        val audioConfig = AudioConfig.create(false)

        val recording = controller.startRecording(
            outputOptions,
            audioConfig,
            ContextCompat.getMainExecutor(context),
        ) { event: VideoRecordEvent ->
            when (event) {
                is VideoRecordEvent.Start -> onRecStarted()
                is VideoRecordEvent.Finalize -> onRecFinished()
            }
        }

        return recording
    }

    // --------------------------------------------------------------------------------------------
    // stopRecording
    // --------------------------------------------------------------------------------------------
    fun stopRecording(recording: Recording?) {
        recording?.stop()
    }

    // --------------------------------------------------------------------------------------------
    // CameraPreview
    // --------------------------------------------------------------------------------------------
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

    // --------------------------------------------------------------------------------------------
    // rememberCameraController
    // --------------------------------------------------------------------------------------------
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

    // --------------------------------------------------------------------------------------------
    // BottomBar
    // --------------------------------------------------------------------------------------------
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Composable
    fun BottomBar(controller: LifecycleCameraController) {

        var activeRecording by remember { mutableStateOf<Recording?>(null) }
        var isRecording by remember { mutableStateOf(false) }
        val audioConfig = AudioConfig.create(true)

        val context = LocalContext.current

        NavigationBar {
            NavigationBarItem(
                selected = true,
                onClick = { photo.takePhoto(context, controller) },
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
                        activeRecording = startRecording(
                            controller,
                            context,
                            onRecStarted = { Toast.makeText(context, "REC", Toast.LENGTH_SHORT).show() },
                            onRecFinished = { Toast.makeText(context, "Saved Video", Toast.LENGTH_SHORT).show() }
                        )
                    }
                    else {
                        stopRecording(activeRecording)
                    }
                    isRecording = !isRecording
                },
                icon = {
                    Icon(
                        painterResource(id = R.drawable.video),
                        contentDescription = null
                    )
                },
                label = { Text("Video") },
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