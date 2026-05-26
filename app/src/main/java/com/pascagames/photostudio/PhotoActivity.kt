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
                lifecycleOwner,
                onFinished = {doSinglePhoto = false})
        }

        if (doMultiplePhotos) {
            TakeMultiplePhotos(
                controller = controller,
                onFinishedSingle = {},
                onFinishedAll = {doMultiplePhotos = false})
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
        lifecycleOwner: LifecycleOwner,
        onFinished: () -> Unit
    ) {
        val context = LocalContext.current
        var secondsLeft by remember { mutableIntStateOf(Settings.photoBeepDelay) }
        var showCountDown by remember { mutableStateOf(true) }
        var message by remember { mutableStateOf<String?>(null) }

        // Auto-hide message
        LaunchedEffect(message) {
            if (message != null) {
                delay(1500)
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

            // 2) Take JPG
            if (Settings.photoRawEnabled) {
                cameraLib.takePhotoRaw(
                    context,
                    controller,
                    onSaved = { message = "JPEG saved" },
                    onError = { message = "JPEG error" })
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
            onFinished()
        }

        if (showCountDown)
            ShowCountDown(secondsLeft)

        if (message != null) {
            ShowMessage(message!!)
        }
    }

    // ----------------------------------------------------------------------
    // ShowMessage
    // ----------------------------------------------------------------------
    @Composable
    fun ShowMessage(text: String) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }

    // ----------------------------------------------------------------------
    // TakeMultiplePhotos
    // ----------------------------------------------------------------------
    @Composable
    fun TakeMultiplePhotos(
        controller: LifecycleCameraController,
        onFinishedSingle: () -> Unit,
        onFinishedAll: () -> Unit
    ) {
        var delay by remember { mutableIntStateOf(Settings.photoBeepDelay) }
        var message by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current

        // Auto-hide message
        LaunchedEffect(message) {
            if (message != null) {
                delay(1500)
                message = null
            }
        }

        LaunchedEffect(Unit) {
            while (delay > 0) {
                delay(1000)
                delay--
                if (Settings.photoDelayBeepEnabled) {
                    beep(100,20)
                }
            }

            for (i in 0 until Settings.photoNumMultiple) {
                cameraLib.takePhotoJpg(
                    context,
                    controller,
                    onSaved = { message = "JPEG saved" },
                    onError = { message = "JPEG error" })
                delay(Settings.delayBetweenPhotos)
                onFinishedSingle()
            }
            onFinishedAll()
        }
        ShowCountDown(delay)
    }

    // ----------------------------------------------------------------------
    // ShowCountDown
    // ----------------------------------------------------------------------
    @Composable
    fun ShowCountDown(delay: Int) {

        // UI countdown
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
