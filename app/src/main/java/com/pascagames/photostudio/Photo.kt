package com.pascagames.photostudio

import android.content.ContentValues
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat

class Photo {

    // --------------------------------------------------------------------------------------------
    // takePhoto
    // --------------------------------------------------------------------------------------------
    fun takePhoto(
        context: Context,
        controller: LifecycleCameraController,
        beepEnabled: Boolean = true
    ) {
         val name = "IMG_${System.currentTimeMillis()}.jpg"

         Log.v(TAG, "takePhoto")
         Log.v(TAG, name)

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

                     if (beepEnabled)
                         playBeep()
                 }
             }
         )
    }

    // --------------------------------------------------------------------------------------------
    // playBeep
    // --------------------------------------------------------------------------------------------
    fun playBeep() {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 80)
    }

}