// --------------------------------------------------------------------------
// Language:    Kotlin
//
// Framework:   Google Jetpack Compose
//
// Package:     com.pascagames.photostudio
//
// Author:      Antonio Pascarella
//
// Version:     Rel. 0.7.0
//
// Date:        June 2026
//
// Module:      MainActivity.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
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
import androidx.camera.view.LifecycleCameraController
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


// --------------------------------------------------------------------------
// PhotoActivity
// --------------------------------------------------------------------------
class PhotoActivity : ComponentActivity() {

    private var backToCaller: (Unit) -> Unit = { back() }
    val cameraLib = CameraLib()
    private lateinit var cameraExecutor: ExecutorService

    // ----------------------------------------------------------------------
    // onCreate
    // ----------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        cameraExecutor = Executors.newSingleThreadExecutor()

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
    // onDestroy
    // --------------------------------------------------------------------------
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
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
    @SuppressLint("UnrememberedMutableState")
    @Composable
    fun MainScreen(modifier: Modifier = Modifier) {

        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        // CameraX controller
        val controller = cameraLib.rememberCameraController(context)

        // UI States
        var doFocus by remember {mutableStateOf(false)}
        var doSinglePhoto by remember {mutableStateOf(false)}
        var doMultiplePhotos by remember {mutableStateOf(false)}

        // Bitmap for overlay focus peaking
        val focusPeakingBitmap = mutableStateOf<Bitmap?>(null)

        // Bind controller to lifecycle
        LaunchedEffect(Unit) {
            controller.bindToLifecycle(lifecycleOwner)
        }

        // Focus Peaking management
        LaunchedEffect(doFocus) {
            if (doFocus) {
                Log.v(TAG, "Focus peaking ON")
                controller.setImageAnalysisAnalyzer(
                    cameraExecutor,
                    FocusPeakingAnalyzer { edges ->
                        focusPeakingBitmap.value = edges
                    }
                )
            } else {
                Log.v(TAG, "Focus peaking OFF")
                controller.clearImageAnalysisAnalyzer()
                focusPeakingBitmap.value = null
            }
        }

        // Single Photo
        if (doSinglePhoto) {
            val newFolder =  createSessionDirectory()
            TakeSinglePhoto(
                controller = controller,
                newFolder,
                onFinished = {doSinglePhoto = false})
        }

        // MultiplePhotos
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
                    {doFocus = !doFocus},
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
                    controller = controller,
                    modifier = Modifier.fillMaxSize(),
                    focusPeakingBitmap = focusPeakingBitmap.value
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
        folder: File,
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

        // Countdown
        LaunchedEffect(Unit) {
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
                if (Settings.photoDelayBeepEnabled) {
                    beep(100,20)
                }
            }

            // Take RAW or JPG photo
            if (Settings.photoRawEnabled) {
                cameraLib.takePhoto(
                    context,
                    controller,
                    PHOTO_RAW,
                    folder,
                    1,
                    onSaved = { message = "RAW saved" },
                    onError = { message = "RAW error" })
            }
                else {
                cameraLib.takePhoto(
                    context,
                    controller,
                    PHOTO_JPEG,
                    folder,
                    1,
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
        var createNewDir by remember { mutableStateOf(true) }
        var newFolder = File("")

        if (createNewDir) {
            newFolder = createSessionDirectory()
            createNewDir = false
        }

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
                    cameraLib.takePhoto(
                        context,
                        controller,
                        PHOTO_RAW,
                        newFolder,
                        showShotIdx+1,
                        onSaved = { message = "RAW shot " + (showShotIdx +1).toString()},
                        onError = { message = "RAW error on shot " + (showShotIdx +1).toString()})
                }
                else {
                    cameraLib.takePhoto(
                        context,
                        controller,
                        PHOTO_JPEG,
                        newFolder,
                        showShotIdx+1,
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
        onFocus: () -> Unit,
        onPhoto: () -> Unit,
        onMultiplePhotos: () -> Unit){

        NavigationBar {
            NavigationBarItem(
                selected = true,
                onClick = onFocus,
                icon = {
                    Icon(
                        painterResource(id = R.drawable.focus),
                        contentDescription = null
                    )
                },
                label = { Text("Focus peaking") },
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

class FocusPeakingView(context: Context) : View(context) {
    var edges: Bitmap? = null

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        edges?.let {
            canvas.drawBitmap(it, null, Rect(0, 0, width, height), null)
        }
    }
}
