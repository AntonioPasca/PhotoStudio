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
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
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
            put(MediaStore.Images.Media.RELATIVE_PATH, Settings.photoPath)
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
                        beep(100, 80)

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
            beep(100, 80)
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
            beep(100, 200)

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
        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val path = File(picturesDir, "CameraX").absolutePath

        val dir = File(path)

        val files = dir.listFiles { f ->
            f.isFile && (f.extension.lowercase() in listOf("jpg", "jpeg", "png"))
        } ?: emptyArray()

        if (files.isEmpty()) return

        // Save the first image to get dimensions
        val refIndex = files.count()/2
        val firstBmp = fixBitmapRotation(files[refIndex].absolutePath)
        val width = firstBmp.width
        val height = firstBmp.height

        Log.v(TAG,"EX_1")
        Log.v(TAG,refIndex.toString() )
        val stacker = Stacker(width, height)

        // Add all images
        for ((index, file) in files.withIndex()) {

            val bmpRotated = fixBitmapRotation(file.absolutePath)
            stacker.addFrame(bmpRotated)

            // Free memory every three images
            if (index % 3 == 0) {
                System.gc()
            }
        }

        Log.v(TAG,"EX_2")
        // Compute shifts
        val shifts = computeShifts(buffers = stacker.buffers, w = width, h = height, maxShift = 50)
        Log.v(TAG, shifts.toString())

        // Do the final stack
        val resultArray = stacker.stack(stacker.buffers, shifts, width, height)

        // Convert to bmp
        val finalBitmap = floatArrayToBitmap(resultArray, width, height)

        // Save
        val name = "Stacked_${System.currentTimeMillis()}.jpg"
        saveBitmapToGallery(context, finalBitmap, name, Settings.photoPath)
    }

    // Private Methods

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
            else -> return bmp   // Rotation not needed
        }

        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        bmp.recycle() // 🔥 libera memoria
        return rotated
    }

    // ----------------------------------------------------------------------
    // estimateShift
    // ----------------------------------------------------------------------
    // Estimates the shift (dxm dy) between two images (represented as
    // liner FloatArray) in grayscale
    //
    //  imageRef    reference image
    //  img         imagine da align
    //  w           width
    //  h           height
    //  maxShift    max shift to consider
    // ----------------------------------------------------------------------
    fun estimateShift(
        imageRef: ByteArray,
        img: ByteArray,
        w: Int,
        h: Int,
        maxShift: Int = 50
    ): Pair<Int, Int> {

        var bestDx = 0
        var bestDy = 0
        var bestScore = Float.NEGATIVE_INFINITY

        // Central window to eliminate borders
        val startX = w / 4
        val endX = w * 3 / 4
        val startY = h / 4
        val endY = h * 3 / 4

        for (dy in -maxShift..maxShift) {
            for (dx in -maxShift..maxShift) {

                var score = 0f

                for (y in startY until endY) {
                    val yy = y + dy
                    if (yy !in 0 until h) continue

                    val baseRef = y * w
                    val baseImg = yy * w

                    for (x in startX until endX) {
                        val xx = x + dx
                        if (xx !in 0 until w) continue

                        val r = imageRef[baseRef + x].toInt() and 0xFF
                        val v = img[baseImg + xx].toInt() and 0xFF

                        score += r * v
                    }
                }

                if (score > bestScore) {
                    bestScore = score
                    bestDx = dx
                    bestDy = dy
                }
            }
        }

        return Pair(bestDx, bestDy)
    }

    // ----------------------------------------------------------------------
    // computeShifts
    // ----------------------------------------------------------------------
    fun computeShifts(
        buffers: List<ByteArray>,
        w: Int,
        h: Int,
        maxShift: Int = 50
    ): List<Pair<Int, Int>> {

        Log.v(TAG,"cS_1")
        val shifts = MutableList(buffers.size) { Pair(0, 0) }

        //val refImage = buffers[0]
        val refImage = buffers[5]

        Log.v(TAG,"cS_2")

        for (i in 1 until buffers.size) {
            val img = buffers[i]

            val (dx, dy) = estimateShift(
                imageRef = refImage,
                img = img,
                w = w,
                h = h,
                maxShift = maxShift
            )

            Log.v(TAG,"cS_3")
            shifts[i] = Pair(dx, dy)

            Log.v(TAG, dx.toString())
            Log.v(TAG, dy.toString())
        }

        return shifts
    }
}

// ----------------------------------------------------------------------
// CLASS Stacker
// ----------------------------------------------------------------------
class Stacker(val width: Int, val height: Int) {

    val buffers = mutableListOf<ByteArray>()

    // ----------------------------------------------------------------------
    // addFrame
    // ----------------------------------------------------------------------
    fun addFrame(bmp: Bitmap) {

        buffers += bitmapToByteArray(bmp)
        bmp.recycle()
    }

    // ----------------------------------------------------------------------
    // stack
    // ----------------------------------------------------------------------
    fun stack(
        buffers: List<ByteArray>,
        shifts: List<Pair<Int, Int>>,
        w: Int,
        h: Int
    ): FloatArray {

        val totalPixels = w * h
        val result = FloatArray(totalPixels)
        val temp = IntArray(buffers.size)

        for (i in 0 until totalPixels) {

            val x = i % w
            val y = i / w

            for (b in buffers.indices) {
                val (dx, dy) = shifts[b]

                val xx = x + dx
                val yy = y + dy

                temp[b] =
                    if (xx !in 0 until w || yy !in 0 until h) {
                        0
                    } else {
                        val idx = yy * w + xx
                        buffers[b][idx].toInt() and 0xFF
                    }
            }

            temp.sort()
            result[i] = temp[temp.size / 2].toFloat()
        }

        return result
    }

    // ----------------------------------------------------------------------
    // bitmapToByteArray
    // ----------------------------------------------------------------------
    fun bitmapToByteArray(bmp: Bitmap): ByteArray {
        val w = bmp.width
        val h = bmp.height
        val total = w * h

        val result = ByteArray(total)
        val rowPixels = IntArray(w)

        var index = 0

        for (y in 0 until h) {
            bmp.getPixels(rowPixels, 0, w, 0, y, w, 1)

            for (x in 0 until w) {
                val p = rowPixels[x]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val gray = ((r + g + b) / 3)
                result[index++] = gray.toByte()
            }
        }

        return result
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
