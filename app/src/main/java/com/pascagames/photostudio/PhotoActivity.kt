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
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.camera.view.LifecycleCameraController
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


// --------------------------------------------------------------------------
// PhotoActivity
// --------------------------------------------------------------------------
class PhotoActivity : ComponentActivity() {

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
                Scaffold(topBar = { TopBar("Photo", backToCaller) })  {
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
    @Composable
    fun MainScreen(modifier: Modifier = Modifier) {

        val context = LocalContext.current
        val controller = cameraLib.rememberCameraController(context)
        val lifecycleOwner = LocalLifecycleOwner.current
        var showSinglePhoto by remember {mutableStateOf(false)}
        var showMultiplePhotos by remember {mutableStateOf(false)}
        var showStacking by remember {mutableStateOf(false)}

        if (showSinglePhoto) {
            TakeSinglePhoto(
                controller = controller,
                onFinished = {showSinglePhoto = false})
        }

        if (showMultiplePhotos) {
            TakeMultiplePhotos(
                controller = controller,
                onFinished = {showMultiplePhotos = false})
        }

        if (showStacking) {
            cameraLib.executeStacking(context, Settings.photoPath)
        }

        LaunchedEffect(Unit) {
            controller.bindToLifecycle(lifecycleOwner)
        }

        cameraLib.getCameraPermission()

        Scaffold(
            bottomBar = {
                BottomBar(
                    {showSinglePhoto = true},
                    onMultiplePhotos = {showMultiplePhotos = true},
                    onStacking = {showStacking = true}
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

            cameraLib.takePhoto(context, controller)
            onFinished()
        }

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

    @Composable
    fun TakeMultiplePhotos(
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

            for (i in 0 until 3) {
                cameraLib.takePhoto(context, controller)
                delay(200)
            }
            onFinished()
        }

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
        onMultiplePhotos: () -> Unit,
        onStacking: () ->Unit){

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
                label = { Text("Photo") },
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

            NavigationBarItem(
                selected = true,
                onClick = onStacking,
                icon = {
                    Icon(
                        painterResource(id = R.drawable.photos),
                        contentDescription = null
                    )
                },
                label = { Text("Stacking") },
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

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        PhotoStudioTheme {
            MainScreen()
        }
    }
}