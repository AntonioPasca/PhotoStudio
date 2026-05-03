// --------------------------------------------------------------------------
// Language:    Kotlin
//
// Framework:   Google Jetpack Compose
//
// Package:     com.pascagames.photostudio
//
// Author:      Antonio Pascarella
//
// Version:     Rel. 0.2.0
//
// Date:        May 2026
//
// Module:      MainActivity.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme

private const val APP_NAME = "PhotoStudio"
private const val VERSION =  "Ver 0.2.0"

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

        setContent {
            PhotoStudioTheme {
                Scaffold(
                ) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }


    // --------------------------------------------------------------------------------------------
// MainScreen
// --------------------------------------------------------------------------------------------
    @Composable
    fun MainScreen(modifier: Modifier = Modifier) {

        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        // CameraX Controller
        val controller = remember {
            LifecycleCameraController(context).apply {
                bindToLifecycle(lifecycleOwner)
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                setEnabledUseCases(
                    CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE
                )
            }
        }

        getCameraPermission()

        Scaffold(
            bottomBar = {
                BottomBar(controller)
            }
        ) { innerPadding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                CameraPreview(
                    controller,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
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

    // --------------------------------------------------------------------------------------------
// CameraPreview
// --------------------------------------------------------------------------------------------
    @Composable
    fun CameraPreview(
        controller: LifecycleCameraController,
        modifier: Modifier
    ) {
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    this.controller = controller
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            //this.modifier = modifier
            //modifier = Modifier.padding(50.dp)
            modifier.fillMaxSize()
        )
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
                    Log.e(TAG, "Error", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Photo saved in: ${output.savedUri}")
                    Toast.makeText(context, "Photo saved", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // --------------------------------------------------------------------------------------------
// takeVideo
// --------------------------------------------------------------------------------------------
    fun takeVideo() {

    }

    // --------------------------------------------------------------------------------------------
// settings
// --------------------------------------------------------------------------------------------
    fun settings() {

    }

    // --------------------------------------------------------------------------------------------
// info
// --------------------------------------------------------------------------------------------
    fun info() {

        val intent = Intent(this@MainActivity, InfoActivity::class.java)
        val bundle = Bundle()
        bundle.putString("APP", APP_NAME)
        bundle.putString("VERSION", VERSION)
        intent.putExtra("main_activity_data", bundle)
        startActivity(intent)
    }

    // --------------------------------------------------------------------------------------------
// BottomBar
// --------------------------------------------------------------------------------------------
    @Composable
    fun BottomBar(controller: LifecycleCameraController) {

        val context = LocalContext.current

        NavigationBar {
            NavigationBarItem(
                selected = true,
                onClick = { takePhoto(context, controller) },
                icon = {
                    Icon(
                        painterResource(id = R.drawable.camera),
                        contentDescription = null
                    )
                },
                label = { Text("Photo") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Green,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
            NavigationBarItem(
                selected = true,
                onClick = { takeVideo() },
                icon = {
                    Icon(
                        painterResource(id = R.drawable.video),
                        contentDescription = null
                    )
                },
                label = { Text("Video") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Green,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
            NavigationBarItem(
                selected = true,

                onClick = { settings() },
                icon = {
                    Icon(
                        painterResource(id = R.drawable.settings),
                        contentDescription = null
                    )
                },
                label = { Text("Settings") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Green,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                selected = true,
                onClick = { info() },
                icon = {
                    Icon(
                        painterResource(id = R.drawable.info),
                        contentDescription = null
                    )
                },
                label = { Text("Info") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Green,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        PhotoStudioTheme {
            MainScreen()
        }
    }
}