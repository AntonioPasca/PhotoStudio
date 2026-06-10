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
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size

// --------------------------------------------------------------------------
// CLASS PhotoActivity
// --------------------------------------------------------------------------
class PhotoActivity : ComponentActivity() {

    private var backToCaller: (Unit) -> Unit = { back() }
    private lateinit var cameraLib: CameraLib
    private lateinit var cameraExecutor: ExecutorService
    private var controller: LifecycleCameraController? = null

    // ----------------------------------------------------------------------
    // onCreate
    // ----------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        cameraLib = CameraLib()
        cameraExecutor = Executors.newSingleThreadExecutor()

        controller = LifecycleCameraController(this).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                CameraController.VIDEO_CAPTURE or
                CameraController.IMAGE_ANALYSIS
            )
        }
    }

    // ----------------------------------------------------------------------
    // onResume
    // ----------------------------------------------------------------------
    override fun onResume() {
        super.onResume()

        var title = "Photo (Jpg Format)"
        if (Settings.photoRawEnabled)
            title = "Photo (Raw Format)"

        val actions = listOf(
            TopBarAction.Settings { settings() },
        )

        // Update camera
        controller?.cameraSelector =
            if (Settings.photoBackCamera)
                CameraSelector.DEFAULT_BACK_CAMERA
            else
                CameraSelector.DEFAULT_FRONT_CAMERA

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
    @Composable
    fun MainScreen(
        modifier: Modifier = Modifier,
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current

        // UI States
        var doFocus by remember { mutableStateOf(false) }
        var doSinglePhoto by remember { mutableStateOf(false) }
        var doMultiplePhotos by remember { mutableStateOf(false) }

        // Overlay states
        val focusPeakingBitmap = remember { mutableStateOf<Bitmap?>(null) }
        val histogram = remember { mutableStateOf(FloatArray(256)) }

        // Bind controller to lifecycle
        LaunchedEffect(Unit) {
            controller?.bindToLifecycle(lifecycleOwner)
        }

        // Focus Peaking + Histogram Analyzer
        LaunchedEffect(doFocus) {
            if (doFocus) {
                controller?.setImageAnalysisAnalyzer(
                    cameraExecutor,
                    CombinedAnalyzer(
                        onFocusPeaking = { bmp -> focusPeakingBitmap.value = bmp },
                        onHistogram = { hist -> histogram.value = hist }
                    )
                )
            } else {
                controller?.clearImageAnalysisAnalyzer()
                focusPeakingBitmap.value = null
            }
        }

        // Single Photo
        if (doSinglePhoto) {
            val newFolder = createSessionDirectory()
            TakeSinglePhoto(
                controller = controller!!,
                newFolder,
                onFinished = { doSinglePhoto = false }
            )
        }

        // Multiple Photos
        if (doMultiplePhotos) {
            TakeMultiplePhotos(
                controller = controller!!,
                onFinishedSingle = {},
                onEnd = {
                    doMultiplePhotos = false
                    if (Settings.photoBeepEnabled)
                        beep(100, 50)
                }
            )
        }

        Scaffold(
            bottomBar = {
                BottomBar(
                    { doFocus = !doFocus },
                    { doSinglePhoto = true },
                    onMultiplePhotos = { doMultiplePhotos = true }
                )
            }
        ) { innerPadding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {

                // Camera preview
                cameraLib.CameraPreview(
                    controller = controller!!,
                    modifier = Modifier.fillMaxSize(),
                    focusPeakingBitmap = focusPeakingBitmap.value
                )

                // Histogram overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(width = 160.dp, height = 100.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    HistogramView(histogram.value)
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // HistogramView
    // ----------------------------------------------------------------------
    @Composable
    fun HistogramView(values: FloatArray) {
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val barWidth = size.width / 256f
            values.forEachIndexed { i, v ->
                drawRect(
                    color = Color.Green,
                    topLeft = Offset(i * barWidth, size.height * (1f - v)),
                    size = Size(barWidth, size.height * v)
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
