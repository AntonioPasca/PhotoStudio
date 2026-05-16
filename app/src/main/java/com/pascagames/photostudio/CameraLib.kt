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
//      fun takePhoto(context: Context, controller: LifecycleCameraController)
//      fun startRecording(controller: LifecycleCameraController, context: Context,
//              onRecStarted: () -> Unit,
//              onRecFinished: () -> Unit): Recording
//      fun stopRecording(recording: Recording?)
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CameraLib {

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