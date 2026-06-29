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
// Module:      CameraLib.kt
// --------------------------------------------------------------------------
//  CAMERA HANDLING
//      fun startCam()
//      fun shutdown
//      fun getAudioPermission()
//      fun getAudioPermission()
//      fun isRawSupported(context: Context, cameraId: String): Boolean
//      suspend fun listAvailableCameras(context: Context): List<Pair<String, String>>
//      fun rememberCameraController(context: Context): LifecycleCameraController
//
// PHOTO FUNCTIONS
//      fun CameraPreview(controller: LifecycleCameraController, modifier: Modifier)
//      fun takePhoto(context: Context, controller: LifecycleCameraController)
//
// VIDEO FUNCTIONS
//      fun startRecording(controller: LifecycleCameraController, context: Context,
//              onRecStarted: () -> Unit,
//              onRecFinished: () -> Unit): Recording
//      fun stopRecording(recording: Recording?)
//
// IMAGE HANDLING
//      fun bitmapToByteArray(bmp: Bitmap): ByteArray
//      fun fixBitmapRotation(path: String): Bitmap
//      fun floatArrayToBitmap(buffer: FloatArray, width: Int, height: Int): Bitmap
//      fun saveBitmapToGallery()
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
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
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.lifecycle.ProcessCameraProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val PHOTO_JPEG = 0
const val PHOTO_RAW  = 1

// ----------------------------------------------------------------------
// CLASS CameraLib
// ----------------------------------------------------------------------
class CameraLib {

    lateinit var cameraExecutor: ExecutorService

    // ----------------------------------------------------------------------
    //                      CAMERA HANDLING
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // startCam
    // ----------------------------------------------------------------------
    fun startCam() {

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    // ----------------------------------------------------------------------
    // shutdown
    // ----------------------------------------------------------------------
    fun shutdown() {

        cameraExecutor.shutdown()
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
    // listAvailableCameras
    // ----------------------------------------------------------------------
    @OptIn(ExperimentalCamera2Interop::class)
    suspend fun listAvailableCameras(context: Context): List<Pair<String, String>> {
        val provider = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(context).get()
        }
        val cameraInfos = provider.availableCameraInfos

        return cameraInfos.map { cameraInfo ->
            val camera2Info = Camera2CameraInfo.from(cameraInfo)
            val id = camera2Info.cameraId

            val facing = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.LENS_FACING
            )

            val facingName = when (facing) {
                CameraCharacteristics.LENS_FACING_BACK -> "Back"
                CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                else -> "Unknown"
            }

            id to facingName
        }
    }

    // ----------------------------------------------------------------------
    // rememberCameraController
    // ----------------------------------------------------------------------
    @Composable
    fun rememberCameraController(context: Context): LifecycleCameraController {
        val controller = remember {
            LifecycleCameraController(context).apply {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                if (!Settings.photoBackCamera)
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                setEnabledUseCases(
                    CameraController.IMAGE_CAPTURE or
                            CameraController.VIDEO_CAPTURE or
                            CameraController.IMAGE_ANALYSIS
                )
            }
        }
        controller.isPinchToZoomEnabled = true
        return controller
    }

    // ----------------------------------------------------------------------
    // selectCamera
    // ----------------------------------------------------------------------
    fun selectCamera(controller: LifecycleCameraController) {

        controller.cameraSelector =
            if (Settings.photoBackCamera)
                CameraSelector.DEFAULT_BACK_CAMERA
            else
                CameraSelector.DEFAULT_FRONT_CAMERA
    }

    // ----------------------------------------------------------------------
    //                      PHOTO FUNCTIONS
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // CameraPreview
    // ----------------------------------------------------------------------
    @Composable
    fun CameraPreview(
        controller: LifecycleCameraController,
        modifier: Modifier,
        focusPeakingBitmap: Bitmap? // <- to the analyzer
    ) {
        val zoomState: ZoomState? by controller.zoomState.observeAsState()
        val context = LocalContext.current

        // PreviewView stabile
        val previewView = remember {
            PreviewView(context).apply {
                this.controller = controller
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {

            // PREVIEW CAMERA
            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        this.controller = controller
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = modifier.fillMaxSize()
            )

            // Overlay Focus peaking
            if (focusPeakingBitmap != null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        FocusPeakingView(context)
                    },
                    update = { view ->
                        view.edges = focusPeakingBitmap
                        view.invalidate()
                    }
                )
            }

            // Slider Zoom
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
    // takePhoto
    // ----------------------------------------------------------------------
    // Input
    //      photoType: 0 = JPEG
    //                 1 = RAW
    //      folder:    path where to save photo
    //                 /Pictures/AstroPhoto + <mmm><dd>_<hhmmss>
    //      photoIdx:  Photo sequence number
    // ----------------------------------------------------------------------
    fun takePhoto(
        context: Context,
        controller: CameraController,
        photoType: Int,
        folder: File,
        photoIdx: Int,
        photoPrefix: String,
        onSaved: () -> Unit,
        onError: () -> Unit
    ) {
        var mimeType = "image/jpeg"
        if (photoType == PHOTO_RAW)
            mimeType = "image/x-adobe-dng"

        val name = photoPrefix + "_$photoIdx"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, folder.toString())
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
                    exc.printStackTrace()
                    onError()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onSaved()
                }
            }
        )
    }

    // ----------------------------------------------------------------------
    //                      VIDEO FUNCTIONS
    // ----------------------------------------------------------------------

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
    //                      IMAGE HANDLING
    // ----------------------------------------------------------------------

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
}
