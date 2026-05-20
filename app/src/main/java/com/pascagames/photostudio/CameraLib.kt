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
//      fun getAudioPermission(): Boolean
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
import android.graphics.Matrix
import android.media.AudioManager
import androidx.exifinterface.media.ExifInterface
import android.media.ToneGenerator
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder


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
    // getAudioPermission
    // ----------------------------------------------------------------------
    @Composable
    fun getAudioPermission(): Boolean {

        var result = false
        val context = LocalContext.current
        val permission = Manifest.permission.RECORD_AUDIO

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
        val controller = remember {
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
    fun startRecording(
        controller: LifecycleCameraController,
        context: Context,
        audioConfig: AudioConfig,
        onRecStarted: () -> Unit,
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
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val path = File(picturesDir, "CameraX").absolutePath

        //val dir = File(Settings.photoPath)        // CHECK !!!!!!!!
        val dir = File(path)

        val files = dir.listFiles { f ->
            f.isFile && (f.extension.lowercase() in listOf("jpg", "jpeg", "png"))
        } ?: emptyArray()

        val bmp = BitmapFactory.decodeFile(files[0].toString())
        val bmpRotated = fixBitmapRotation(files[0].toString())

        val width = bmpRotated.width
        val height = bmpRotated.height
        val stacker = Stacker(width, height)

        for ((index, file) in files.withIndex()) {

            val bmpRotated = fixBitmapRotation(file.absolutePath)

            if (bmp != null) {
                stacker.addFrame(bmpRotated)
            }

            // 3️⃣ Free memory every 3 images
            if (index % 3 == 0) {
                System.gc()
            }
        }

        // Final step
        val resultArray = stacker.buildFinal()
        val finalBitmap = floatArrayToBitmap(resultArray, width, height)

        // Finally save the stacked image
        val name = "Stacked_${System.currentTimeMillis()}.jpg"
        saveBitmapToGallery(context, finalBitmap, name, Settings.photoPath)
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
    // saveBitmapToGallery
    // ----------------------------------------------------------------------
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        filename: String = "stacked_${System.currentTimeMillis()}.jpg",
        folder: String
    ): Uri? {

        val folder1 = "DCIM/Camera"
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
    // floatArrayToBitmap
    // ----------------------------------------------------------------------
    fun floatArrayToBitmap(buffer: FloatArray, width: Int, height: Int): Bitmap {

        val bmp = createBitmap(width, height)

        var i = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val v = buffer[i++].coerceIn(0f, 255f).toInt()
                val color = android.graphics.Color.rgb(v, v, v)
                bmp[x, y] = color
            }
        }

        return bmp
    }

    // ----------------------------------------------------------------------
    // fixBitmapRotation
    // ----------------------------------------------------------------------
    fun fixBitmapRotation(path: String): Bitmap {
        val bmp = BitmapFactory.decodeFile(path)

        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bmp   // nessuna rotazione necessaria
        }

        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        bmp.recycle() // 🔥 libera memoria
        return rotated
    }
}

// ----------------------------------------------------------------------
// Bitmap.rotate
// ----------------------------------------------------------------------
// Bitmap extension to create a rotation
// ----------------------------------------------------------------------
fun Bitmap.rotate(degrees: Float): Bitmap {

    val matrix = Matrix().apply { postRotate(degrees) }
    val bmp = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    return bmp
}

// ----------------------------------------------------------------------
// CLASS Stacker
// ----------------------------------------------------------------------
class Stacker(val width: Int, val height: Int) {

    private val buffers = mutableListOf<ByteBuffer>()
    private val totalPixels = width * height

    // ----------------------------------------------------------------------
    // addFrame
    // ----------------------------------------------------------------------
    fun addFrame(bmp: Bitmap) {
        val buf = bitmapToGrayByteBuffer(bmp)
        buffers.add(buf)

        // Call GC every three images
        if (buffers.size % 3 == 0) {
            System.gc()
        }
    }

    // ----------------------------------------------------------------------
    // buildFinal
    // ----------------------------------------------------------------------
    fun buildFinal(): FloatArray {
        val result = FloatArray(totalPixels)
        val temp = IntArray(buffers.size)

        for (i in 0 until totalPixels) {

            // Get values from the same pixel from all buffers
            for (b in buffers.indices) {
                temp[b] = buffers[b].get(i).toInt() and 0xFF
            }

            // sort
            temp.sort()

            // median
            result[i] = temp[temp.size / 2].toFloat()
        }

        return result
    }

    fun bitmapToGrayByteBuffer(bmp: Bitmap): ByteBuffer {
        val w = bmp.width
        val h = bmp.height

        val buffer = ByteBuffer.allocateDirect(w * h)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val p = pixels[i]
            val gray = (
                    android.graphics.Color.red(p) +
                    android.graphics.Color.green(p) +
                    android.graphics.Color.blue(p)
            ) / 3
            buffer.put(gray.toByte())
        }

        // 🔥 LIBERA SUBITO LA MEMORIA DEL BITMAP
        bmp.recycle()
        buffer.rewind()
        return buffer
    }
}
