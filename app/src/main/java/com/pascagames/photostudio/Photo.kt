// --------------------------------------------------------------------------
// Language:    Kotlin
//
// Framework:   Google Jetpack Compose
//
// Package:     com.pascagames.photostudio
//
// Author:      Antonio Pascarella
//
// Version:     Rel. 0.4.0
//
// Date:        May 2026
//
// Module:      Photo.kt
// --------------------------------------------------------------------------
// Public Methods
//      fun takePhoto(context: Context, controller: LifecycleCameraController)
//      fun startRecording(controller: LifecycleCameraController, context: Context,
//        onRecStarted: () -> Unit, onRecFinished: () -> Unit): Recording
//      fun stopRecording(recording: Recording?)
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.core.content.ContextCompat

class Photo {

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