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
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
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
import androidx.compose.foundation.Canvas
import kotlin.math.max

// --------------------------------------------------------------------------
// CLASS PhotoActivity
// --------------------------------------------------------------------------
class PhotoActivity : ComponentActivity() {

    private var backToCaller: (Unit) -> Unit = { back() }
    private lateinit var cameraLib: CameraLib
    private var controller: LifecycleCameraController? = null

    // ----------------------------------------------------------------------
    // onCreate
    // ----------------------------------------------------------------------
    @OptIn(ExperimentalCamera2Interop::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        cameraLib = CameraLib()
        cameraLib.startCam()

        // QUI dentro aggiungi Camera2Interop
        // Non sembrano avere effetto
        /*val imageCaptureBuilder = ImageCapture.Builder()
        val extender = Camera2Interop.Extender(imageCaptureBuilder)

        // ISO
        extender.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, 1600)

        // Exposure time 1/10 sec = 100.000.000 ns
        extender.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, 100_000_000L)

        // Noise Reduction OFF (Really important for Astro)
        extender.setCaptureRequestOption(CaptureRequest.NOISE_REDUCTION_MODE, NOISE_REDUCTION_MODE_OFF)*/
    }

    // ----------------------------------------------------------------------
    // onResume
    // ----------------------------------------------------------------------
    override fun onResume() {
        super.onResume()

        var title = "Photo (Jpg Format)"
        if (Settings.photoRawEnabled)
            title = "Photo (Raw Format)"

        // Update camera
        if (controller != null)
            cameraLib.selectCamera(controller!!)

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
        cameraLib.shutdown()
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
        val context = LocalContext.current
        controller = cameraLib.rememberCameraController(context)

        // UI States
        var doSinglePhoto by remember { mutableStateOf(false) }
        var doMultiplePhotos by remember { mutableStateOf(false) }
        var doDarks by remember { mutableStateOf(false) }
        var doFocus by remember { mutableStateOf(false) }
        var doHistogram by remember {mutableStateOf(Settings.photoShowHistogram)}

        // Overlay states
        val focusPeakingBitmap = remember { mutableStateOf<Bitmap?>(null) }
        val histogram = remember { mutableStateOf(FloatArray(256)) }

        // Bind controller to lifecycle
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(Unit) {
            controller?.bindToLifecycle(lifecycleOwner)
        }

        // Combined Analyzer (Focus + Histogram)
        LaunchedEffect(Unit) {
            controller?.setImageAnalysisAnalyzer(
                cameraLib.cameraExecutor,
                CombinedAnalyzer(
                    onFocusPeaking = { bmp ->
                        if (doFocus) focusPeakingBitmap.value = bmp
                        else focusPeakingBitmap.value = null
                    },
                    /*onHistogram = { hist ->
                        histogram.value = hist
                    }*/

                    onHistogram = { hist ->
                        if (doHistogram) histogram.value = hist
                        //else histogram.value = 0.0
                    }
                )
            )
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
                "PHOTO",
                onFinishedSingle = {},
                onEnd = {
                    doMultiplePhotos = false
                    if (Settings.photoBeepEnabled)
                        beep(100, 50)
                }
            )
        }

        // Take multiple dark photo
        if (doDarks) {
            TakeMultiplePhotos(
                controller = controller!!,
                "DARK",
                onFinishedSingle = {},
                onEnd = {
                    doDarks = false
                    if (Settings.photoBeepEnabled)
                        beep(100, 50)
                }
            )
        }

        Scaffold(
            bottomBar = {
                BottomBar(
                    { doSinglePhoto = true },
                    onMultiplePhotos = { doMultiplePhotos = true },
                    onDark = {doDarks = true},
                    { doFocus = !doFocus },
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
                Log.v(TAG, "Histo")
                if (Settings.photoShowHistogram) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 60.dp)
                            .size(width = 160.dp, height = 120.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    ) {
                        HistogramView(histogram.value)
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // HistogramView
    // ----------------------------------------------------------------------
    @Composable
    fun HistogramView(values: FloatArray) {
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
        ) {
            val barWidth = size.width / 256f
            values.forEachIndexed { i, v ->
                val x = i * barWidth
                val y = size.height * (1f - v)
                drawLine(
                    color = Color.Green,
                    start = Offset(x, size.height),
                    end = Offset(x, y),
                    strokeWidth = max(1f, barWidth)   // always visible
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
            val photoType = if (Settings.photoRawEnabled)  PHOTO_RAW else PHOTO_JPEG
            val msgPrefix = if (Settings.photoRawEnabled) "RAW" else "JPEG"
            cameraLib.takePhoto(
                    context,
                    controller,
                    photoType,
                    folder,
                    1,
                    "PHOTO",
                    onSaved = { message = "$msgPrefix saved" },
                    onError = { message = "$msgPrefix error" })

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
        photoPrefix: String,
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

                val photoType = if (Settings.photoRawEnabled)  PHOTO_RAW else PHOTO_JPEG
                val msgPrefix = if (Settings.photoRawEnabled) "RAW" else "JPEG"

                cameraLib.takePhoto(
                    context,
                    controller,
                    photoType,
                    newFolder,
                    showShotIdx+1,
                    photoPrefix = photoPrefix,
                    onSaved = { message = msgPrefix + " shot " + (showShotIdx +1).toString()},
                    onError = { message = msgPrefix + " error on shot " + (showShotIdx +1).toString()})

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
        onMultiplePhotos: () -> Unit,
        onDark: () -> Unit,
        onFocus: () -> Unit,
    ){

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

            NavigationBarItem(
                selected = true,
                onClick = onDark,
                icon = {
                    Icon(
                        painterResource(id = R.drawable.dark),
                        contentDescription = null
                    )
                },
                label = { Text("Dark") },
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
