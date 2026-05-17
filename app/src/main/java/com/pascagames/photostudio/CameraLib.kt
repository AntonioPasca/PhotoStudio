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
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
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
import androidx.core.graphics.get
import androidx.core.graphics.set
import androidx.core.graphics.createBitmap


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

    // ----------------------------------------------------------------------
    // executeStacking
    // ----------------------------------------------------------------------
    // Todo
    //      più veloce (con buffer preallocati)
    //      più preciso (stacking a colori, non solo grayscale)
    //      più “astro” (sigma‑clipping, dark frame, flat field)
    // ----------------------------------------------------------------------
    fun executeStacking(
        context: Context,
        folder: String
    ) {

        val bitmaps = loadBitmapsFromFolder(context, folder)
        val arrays = bitmaps.map { bitmapToFloatArray(it) }
        val stacked = stackMedian(arrays)   // oppure stackAverage
        val finalBitmap = floatArrayToBitmap(stacked)
        saveBitmapToGallery(context, finalBitmap, "stacked.jpg", folder)
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

    // ----------------------------------------------------------------------
    // loadBitmapsFromFolder
    // ----------------------------------------------------------------------
    // Loads all the bitmaps from a given folder
    //
    //  Input
    //      context: Context
    //      folder: String
    //
    //  Output
    //      bitmaps: List<Bitmap>
    // ----------------------------------------------------------------------
    fun loadBitmapsFromFolder(context: Context, folder: String): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )

        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$folder%")

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(collection, id)
                val bmp = BitmapFactory.decodeStream(
                    context.contentResolver.openInputStream(uri)
                )
                bitmaps.add(bmp)
            }
        }

        return bitmaps
    }

    // ----------------------------------------------------------------------
    // bitmapToFloatArray
    // ----------------------------------------------------------------------
    fun bitmapToFloatArray(bmp: Bitmap): Array<FloatArray> {
        val w = bmp.width
        val h = bmp.height
        val arr = Array(h) { FloatArray(w) }

        for (y in 0 until h) {
            for (x in 0 until w) {
                val pixel = bmp[x, y]
                val gray = (android.graphics.Color.red(pixel) +
                        android.graphics.Color.green(pixel) +
                        android.graphics.Color.blue(pixel)) / 3f
                arr[y][x] = gray
            }
        }
        return arr
    }

    // ----------------------------------------------------------------------
    // saveBitmapToGallery
    // ----------------------------------------------------------------------
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        filename: String = "stacked_${System.currentTimeMillis()}.jpg",
        folder: String
    ): Uri? {

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, folder)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return null

        context.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, contentValues, null, null)

        return uri
    }

    // ----------------------------------------------------------------------
    // stackAverage
    // ----------------------------------------------------------------------
    fun stackAverage(images: List<Array<FloatArray>>): Array<FloatArray> {
        val h = images[0].size
        val w = images[0][0].size
        val result = Array(h) { FloatArray(w) }

        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0f
                for (img in images) {
                    sum += img[y][x]
                }
                result[y][x] = sum / images.size
            }
        }
        return result
    }

    // ----------------------------------------------------------------------
    // stackMedian
    // ----------------------------------------------------------------------
    fun stackMedian(images: List<Array<FloatArray>>): Array<FloatArray> {
        val h = images[0].size
        val w = images[0][0].size
        val result = Array(h) { FloatArray(w) }

        for (y in 0 until h) {
            for (x in 0 until w) {
                val values = FloatArray(images.size)

                for (i in images.indices) {
                    values[i] = images[i][y][x]
                }

                values.sort()
                result[y][x] = values[values.size / 2]
            }
        }
        return result
    }

    // ----------------------------------------------------------------------
    // stackMedian
    // ----------------------------------------------------------------------
    fun floatArrayToBitmap(arr: Array<FloatArray>): Bitmap {
        val h = arr.size
        val w = arr[0].size
        val bmp = createBitmap(w, h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val v = arr[y][x].coerceIn(0f, 255f).toInt()
                bmp[x, y] = android.graphics.Color.rgb(v, v, v)
            }
        }
        return bmp
    }
}