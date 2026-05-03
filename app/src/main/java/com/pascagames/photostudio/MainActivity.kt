// --------------------------------------------------------------------------
// Language:    Kotlin
//
// Framework:   Google Jetpack Compose
//
// Package:     com.pascagames.photostudio
//
// Author:      Antonio Pascarella
//
// Version:     Rel. 0.1.0
//
// Date:        May 2026
//
// Module:      MainActivity.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.*
//import androidx.compose.material3.Icon
//import androidx.compose.foundation.Image
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme

const val TAG = "PHOTO"
private const val VERSION =  "Ver 0.0.1"

// ------------------------------------------------------------------------------------------------
// MainActivity
// ------------------------------------------------------------------------------------------------
class MainActivity : ComponentActivity() {

    // --------------------------------------------------------------------------------------------
    // onCreate
    // --------------------------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        /*ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            0
        )*/

        setContent {
            PhotoStudioTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------------------
// MainScreen
// --------------------------------------------------------------------------------------------
@Composable
fun MainScreen(modifier: Modifier = Modifier) {

    getCameraPermission()
    CameraScreen()
}

// --------------------------------------------------------------------------------------------
// getCameraPermission
// --------------------------------------------------------------------------------------------
@Composable
fun getCameraPermission(): Boolean {

    var result = false
    val context = LocalContext.current
    val permission = Manifest.permission.CAMERA

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
            granted -> if (granted) {
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


// --------------------------------------------------------------------------------------------
// CameraPreview
// --------------------------------------------------------------------------------------------
@Composable
fun CameraPreview(_controller: LifecycleCameraController) {
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                controller = _controller
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// --------------------------------------------------------------------------------------------
// CameraScreen
// --------------------------------------------------------------------------------------------
@Composable
//fun CameraScreen(takePhoto: (Context, LifecycleCameraController) -> Unit) {
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Controller CameraX
    val controller = remember {
        LifecycleCameraController(context).apply {
            bindToLifecycle(lifecycleOwner)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Preview
        CameraPreview(controller)

        // Pulsante di scatto
        FloatingActionButton(
            onClick = { takePhoto(context, controller) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Icon(
                painterResource(R.drawable.phone),
                contentDescription = "Scatta foto"
            )
        }
    }
}

// --------------------------------------------------------------------------------------------
// takePhoto
// --------------------------------------------------------------------------------------------
fun takePhoto(
    context: Context,
    controller: LifecycleCameraController
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
                Log.e(TAG, "Errore scatto", exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Log.d(TAG, "Foto salvata: ${output.savedUri}")
                Toast.makeText(context, "Foto salvata", Toast.LENGTH_SHORT).show()
            }
        }
    )
}




@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PhotoStudioTheme {
        MainScreen()
    }
}