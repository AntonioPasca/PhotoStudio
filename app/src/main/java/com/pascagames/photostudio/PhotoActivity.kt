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
// Module:      MainActivity.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.ImageFormat
import android.os.Bundle
import android.util.Log
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
import androidx.camera.core.ImageCapture
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay


// --------------------------------------------------------------------------
// PhotoActivity
// --------------------------------------------------------------------------
class PhotoActivity : ComponentActivity() {

    private var backToCaller: (Unit) -> Unit = { back() }
    val cameraLib = CameraLib()

    @SuppressLint("RestrictedApi")
    val rawImageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        .setBufferFormat(ImageFormat.RAW_SENSOR)
        .build()

    // ----------------------------------------------------------------------
    // onCreate
    // ----------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

       /* var title = "Photo (Jpg Format)"
        if (Settings.photoRawEnabled)
            title = "Photo (Raw Format)"*/

        val title = "Photo"

        val actions = listOf(
            TopBarAction.Settings { settings() },
        )
        setContent {
            PhotoStudioTheme {
                Scaffold(topBar = { TopBarEx(title, actions, backToCaller) })  {
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

        val intent = Intent(this@PhotoActivity, SettingsActivity::class.java)
        val bundle = Bundle()
        bundle.putInt("SETTINGS_INDEX", SETTINGS_PHOTO_INDEX)
        intent.putExtra("activity_data", bundle)
        startActivity(intent)
    }

    // ----------------------------------------------------------------------
    // MainScreen
    // ----------------------------------------------------------------------
    @Composable
    fun MainScreen(modifier: Modifier = Modifier) {

        val context = LocalContext.current
        val controller = cameraLib.rememberCameraController(context)
        val lifecycleOwner = LocalLifecycleOwner.current
        var doSinglePhoto by remember {mutableStateOf(false)}
        var doMultiplePhotos by remember {mutableStateOf(false)}


        if (doSinglePhoto) {
            TakeSinglePhoto(
                controller = controller,
                onFinished = {doSinglePhoto = false})
        }

        if (doMultiplePhotos) {
            TakeMultiplePhotos(
                controller = controller,
                onFinishedSingle = {},
                onEnd = {
                    doMultiplePhotos = false
                    if (Settings.photoBeepEnabled)
                        beep(100, 50)
                })
        }

        LaunchedEffect(Unit) {
            controller.bindToLifecycle(lifecycleOwner)
        }

        Scaffold(
            bottomBar = {
                BottomBar(
                    {doSinglePhoto = true},
                    onMultiplePhotos = {doMultiplePhotos = true}
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
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // ----------------------------------------------------------------------
    // TakeSinglePhoto
    // ----------------------------------------------------------------------
    @Composable
    fun TakeSinglePhoto(
        controller: LifecycleCameraController,
        onFinished: () -> Unit
    ) {
        val context = LocalContext.current
        var secondsLeft by remember { mutableIntStateOf(Settings.photoBeepDelay) }
        var showCountDown by remember { mutableStateOf(true) }
        var message by remember { mutableStateOf<String?>(null) }

        // Auto-hide message
        LaunchedEffect(message) {
            if (message != null) {
                delay(5000)
                message = null
            }
        }

        LaunchedEffect(Unit) {
            // 1) Countdown
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
                if (Settings.photoDelayBeepEnabled) {
                    beep(100,20)
                }
            }

            // 2) Take RAW or JPG photo
            if (Settings.photoRawEnabled) {
                cameraLib.takePhotoRaw(
                    context,
                    controller,
                    onSaved = { message = "RAW saved" },
                    onError = { message = "RAW error" })
            }
                else {
                cameraLib.takePhotoJpg(
                    context,
                    controller,
                    onSaved = { message = "JPEG saved" },
                    onError = { message = "JPEG error" })
            }

            delay(1000)
            showCountDown = false

            if (Settings.photoBeepEnabled)
                beep(100, 50)

            onFinished()
        }

        if (showCountDown)
            //ShowCountDown(secondsLeft)
            ShowMessage(secondsLeft.toString(), 100.sp)

        if (message != null)
            ShowMessage(message!!, 22.sp)
    }

    // ----------------------------------------------------------------------
    // TakeMultiplePhotos
    // ----------------------------------------------------------------------
    @Composable
    fun TakeMultiplePhotos(
        controller: LifecycleCameraController,
        onFinishedSingle: () -> Unit,
        onEnd: () -> Unit
    ) {
        val context = LocalContext.current
        var secondsLeft by remember { mutableIntStateOf(Settings.photoBeepDelay) }
        var showCountDown by remember { mutableStateOf(true) }
        var message by remember { mutableStateOf<String?>(null) }

        // Auto-hide message
        LaunchedEffect(message) {
            if (message != null) {
                delay(5000)
                message = null
            }
        }

        LaunchedEffect(Unit) {

            // Countdown
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
                if (Settings.photoDelayBeepEnabled) {
                    beep(100,20)
                }
            }

            showCountDown = false

            // Multiple photo loop
            for (showShotIdx in 0 until Settings.photoNumMultiple) {

                if (Settings.photoRawEnabled) {
                    cameraLib.takePhotoRaw(
                        context,
                        controller,
                        onSaved = { message = "RAW shot " + (showShotIdx +1).toString()},
                        onError = { message = "RAW error on shot " + (showShotIdx +1).toString()})
                }
                else {
                    cameraLib.takePhotoJpg(
                        context,
                        controller,
                        onSaved = { message = "JPG shot " + (showShotIdx +1).toString() },
                        onError = { message = "JPG error on shot " + (showShotIdx +1).toString()})
                        onFinishedSingle()
                }

                delay(Settings.delayBetweenPhotos)

            }
            onEnd()
        }

        // Show count down - OK
        if (showCountDown)
            ShowMessage(secondsLeft.toString(), 100.sp)

        // Show message
        if (message != null)
            ShowMessage(message!!, 22.sp)
    }

    // ----------------------------------------------------------------------
    // BottomBar
    // ----------------------------------------------------------------------
    @Composable
    fun BottomBar(
        onPhoto: () -> Unit,
        onMultiplePhotos: () -> Unit){

        NavigationBar {
            NavigationBarItem(
                selected = true,
                onClick = onPhoto,
                icon = {
                    Icon(
                        painterResource(id = R.drawable.photo),
                        contentDescription = null
                    )
                },
                label = { Text("Single Photo") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Green,
                    unselectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                selected = true,
                onClick = onMultiplePhotos,
                icon = {
                    Icon(
                        painterResource(id = R.drawable.photos),
                        contentDescription = null
                    )
                },
                label = { Text("Multiple Photos") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Green,
                    unselectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
