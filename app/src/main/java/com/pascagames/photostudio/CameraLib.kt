// --------------------------------------------------------------------------
// Language:    Kotlin
//
// Framework:   Google Jetpack Compose
//
// Package:     com.pascagames.photostudio
//
// Author:      Antonio Pascarella
//
// Version:     Rel. 0.6.0
//
// Date:        May 2026
//
// Module:      CameraLib.kt
// --------------------------------------------------------------------------
// Public Methods
//      fun bitmapToByteArray(bmp: Bitmap): ByteArray
//      fun CameraPreview(controller: LifecycleCameraController, modifier: Modifier)
//      fun fixBitmapRotation(path: String): Bitmap
//      fun floatArrayToBitmap(buffer: FloatArray, width: Int, height: Int): Bitmap
//      fun getAudioPermission(): Boolean
//      fun getCameraPermission(): Boolean
//      fun takePhoto(context: Context, controller: LifecycleCameraController)
//      fun rememberCameraController(context: Context): LifecycleCameraController
//      fun saveBitmapToGallery()
//      fun startRecording(controller: LifecycleCameraController, context: Context,
//              onRecStarted: () -> Unit,
//              onRecFinished: () -> Unit): Recording
//      fun stopRecording(recording: Recording?)
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
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
import android.hardware.camera2.CameraManager
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

// ----------------------------------------------------------------------
// CLASS CameraLib
// ----------------------------------------------------------------------
class CameraLib {

    @SuppressLint("RestrictedApi")
    fun initCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        onRawReady: (ImageCapture) -> Unit
    ) {

        Log.v(TAG, "Init camera")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // --- Preview ---
            val preview = Preview.Builder().build()

            // --- ImageCapture RAW ---
            val rawImageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setBufferFormat(ImageFormat.RAW_SENSOR)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                rawImageCapture
            )

            onRawReady(rawImageCapture)

        }, ContextCompat.getMainExecutor(context))
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
        bmp.recycle()       // 🔥 free memory
        return rotated
    }

    // ----------------------------------------------------------------------
    // floatArrayToBitmap
    // ----------------------------------------------------------------------
    // Create a bitmap from a FloatArray
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
    // isRawSupported
    // ----------------------------------------------------------------------
    // Input
    //      context: Context
    //      cameraID: String
    //                  "0" -> Back main camera
    //                  "1" -> Front camera
    //                  "2" -> Tele
    //                  "3" -> Ultra-Wide
    //  Note
    //      These values should be confirmed !!!!!!!!!!!!
    // ----------------------------------------------------------------------
    //@Composable
    fun isRawSupported(context: Context, cameraId: String): Boolean {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristics = manager.getCameraCharacteristics(cameraId)

        val capabilities = characteristics.get(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
        )

        return capabilities?.contains(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW
        ) == true
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

    @Composable
    fun rememberCameraProvider(context: Context): ProcessCameraProvider? {
        var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

        LaunchedEffect(Unit) {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            cameraProvider = providerFuture.get()   // blocca solo questo coroutine thread, sicuro
        }

        return cameraProvider
    }

    // ----------------------------------------------------------------------
    // takePhotoJpg
    // ----------------------------------------------------------------------
    fun takePhotoJpg(
        context: Context,
        controller: LifecycleCameraController
    ) {
        val nameBase = "IMG_${System.currentTimeMillis()}"

        // ---------- 1) JPEG con il controller (come fai già) ----------
        val jpegValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$nameBase.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Settings.photoPath)
        }

        val jpegOutput = ImageCapture.OutputFileOptions
            .Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                jpegValues
            )
            .build()

        controller.takePicture(
            jpegOutput,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraX", "Error saving JPEG", exc)
                    Toast.makeText(context, "Errore salvataggio JPEG", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // JPEG ok
                    if (Settings.photoBeepEnabled) {
                        beep(100, 80)
                    }

                    Toast.makeText(context, "JPEG salvato", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // ----------------------------------------------------------------------
    // takePhotoRaw
    // ----------------------------------------------------------------------
    @SuppressLint("RestrictedApi")
    fun takePhotoRaw(context: Context) {

        val nameBase = "RAW_${System.currentTimeMillis()}"

        // --- ImageCapture RAW ---
        val rawImageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setBufferFormat(ImageFormat.RAW_SENSOR)
            .build()

        val rawValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$nameBase.dng")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/x-adobe-dng")
            put(MediaStore.Images.Media.RELATIVE_PATH, Settings.photoPath)
        }

        val rawOutput = ImageCapture.OutputFileOptions
            .Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                rawValues
            )
            .build()

        Log.v(TAG, "Take RAW picture")
        rawImageCapture.takePicture(
            rawOutput,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraX", "Error saving RAW", exc)
                    Toast.makeText(context, "Errore salvataggio RAW", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(context, "RAW salvato", Toast.LENGTH_SHORT).show()
                }
            }
        )
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

    fun laplacianVariance(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Converti in luminanza (Y)
        val gray = DoubleArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            gray[i] = 0.299*r + 0.587*g + 0.114*b
        }

        // Kernel Laplaciano 3x3
        val kernel = arrayOf(
            intArrayOf(0,  1, 0),
            intArrayOf(1, -4, 1),
            intArrayOf(0,  1, 0)
        )

        val lap = DoubleArray(width * height)

        for (y in 1 until height-1) {
            for (x in 1 until width-1) {
                var sum = 0.0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val px = x + kx
                        val py = y + ky
                        val weight = kernel[ky+1][kx+1]
                        sum += gray[py * width + px] * weight
                    }
                }
                lap[y * width + x] = sum
            }
        }

        // Calcola la varianza
        val mean = lap.average()
        var variance = 0.0
        for (v in lap) variance += (v - mean) * (v - mean)
        return variance / lap.size
    }
}

/* Come usare la varianza per cancellare foto non buone
val scores = frames.map { frame ->
    laplacianVariance(frame)
}

// Ordina per nitidezza
val sorted = scores.zip(frames).sortedByDescending { it.first }

// Tieni il top 30%
val keepCount = (sorted.size * 0.30).toInt()
val bestFrames = sorted.take(keepCount).map { it.second }
*/
