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
// Module:      Photo.kt
// --------------------------------------------------------------------------
// Public Methods
//      fun CameraPreview(controller: LifecycleCameraController, modifier: Modifier)
//      fun getCameraPermission(): Boolean
//      fun takePhoto(context: Context, controller: LifecycleCameraController)
//      fun rememberCameraController(context: Context): LifecycleCameraController
//      fun startRecording(controller: LifecycleCameraController, context: Context,
//              onRecStarted: () -> Unit,
//              onRecFinished: () -> Unit): Recording
//      fun stopRecording(recording: Recording?)
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.ZoomState
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CameraLib {

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
    // takePhoto
    // ----------------------------------------------------------------------
    fun takePhoto(
        context: Context,
        controller: LifecycleCameraController
    ) {
        val name = "IMG_${System.currentTimeMillis()}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX")
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        controller.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Error", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Photo saved in: ${output.savedUri}")
                    Toast.makeText(context, "Photo saved", Toast.LENGTH_SHORT).show()

                    if (Settings.photoBeepEnabled)
                        playPhotoBeep()

                }
            }
        )
    }

    // ----------------------------------------------------------------------
    // takeMultiplePhotos
    // ----------------------------------------------------------------------
    // Input
    //      context: Context
    //      imageCapture: ImageCapture
    //      count: Int                          Num of photos to capture
    //      delayMS: Long = 500L                Delay between successive photos
    //
    //  Output
    //      results: List<Bitmaps>
    // ----------------------------------------------------------------------
    suspend fun takeMultiplePhotos(
        context: Context,
        imageCapture: ImageCapture,
        count: Int,
        delayMs: Long = 500L
    ): List<Bitmap> {
        val results = mutableListOf<Bitmap>()

        repeat(count) {
            val bitmap = captureBitmap(imageCapture, context)
            results.add(bitmap)
            delay(delayMs)
        }

        return results
    }

    // ----------------------------------------------------------------------
    // startRecording
    // ----------------------------------------------------------------------
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(
        controller: LifecycleCameraController,
        context: Context,
        onRecStarted:  () -> Unit,
        onRecFinished: () -> Unit
    ): Recording {

        if (Settings.videoStartBeepEnabled) {
            playStartVideoBeep()
        }

        val name = "VID_${System.currentTimeMillis()}.mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies")
        }

        val outputOptions = MediaStoreOutputOptions
            .Builder(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
            .setContentValues(contentValues)
            .build()

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

    // ----------------------------------------------------------------------
    // stopRecording
    // ----------------------------------------------------------------------
    fun stopRecording(recording: Recording?) {

        if (Settings.videoStopBeepEnabled)
            playStopVideoBeep()

        recording?.stop()
    }

    // Private Methods

    // ----------------------------------------------------------------------
    // captureBitmap
    // ----------------------------------------------------------------------
    suspend fun captureBitmap(
                                imageCapture: ImageCapture,
                                context:Context
        ): Bitmap = suspendCoroutine { cont ->
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bitmap = image.toBitmap()
                        image.close()
                        cont.resume(bitmap)
                    }

                    override fun onError(exc: ImageCaptureException) {
                        cont.resumeWithException(exc)
                    }
                }
            )
        }

    // ----------------------------------------------------------------------
    // ImageProxy.toBitmap
    // ----------------------------------------------------------------------
    fun ImageProxy.toBitmap(): Bitmap {
        val plane = planes[0]
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    // ----------------------------------------------------------------------
    // playPhotoBeep
    // ----------------------------------------------------------------------
    private fun playPhotoBeep() {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 80)
    }

    // ----------------------------------------------------------------------
    // playStartVideoBeep
    // ----------------------------------------------------------------------
    fun playStartVideoBeep() {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
    }

    // ----------------------------------------------------------------------
    // playStopVideoBeep
    // ----------------------------------------------------------------------
    fun playStopVideoBeep() {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 50)
    }
}