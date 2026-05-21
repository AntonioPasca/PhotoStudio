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
// Module:      MainActivity.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import android.widget.Toast
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay


// --------------------------------------------------------------------------
// PhotoActivity
// --------------------------------------------------------------------------
class PhotoActivity : ComponentActivity() {

    private var backToCaller: (Unit) -> Unit = { back() }
    var cameraLib = CameraLib()

    // ----------------------------------------------------------------------
    // onCreate
    // ----------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val actions = listOf(
            TopBarAction.Settings { settings() },
        )
        setContent {
            PhotoStudioTheme {
                Scaffold(topBar = { TopBarEx("Photo", actions, backToCaller) })  {
                    innerPadding ->
                        MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    // --------------------------------------------------------------------------
    // back
    // --------------------------------------------------------------------------
    fun back() {
        finish()
    }

    // ----------------------------------------------------------------------
    // settings
    // ----------------------------------------------------------------------
    fun settings() {

        val intent = Intent(this@PhotoActivity, SettingsActivity::class.java)
        val bundle = Bundle()
        bundle.putInt("SETTINGS_INDEX", SETTINGS_PHOTO_INDEX)
        intent.putExtra("activity_data", bundle)
        startActivity(intent)
    }

    // ----------------------------------------------------------------------
    // MainScreen
    // ----------------------------------------------------------------------
    @Composable
    fun MainScreen(modifier: Modifier = Modifier) {

        val context = LocalContext.current
        val controller = cameraLib.rememberCameraController(context)
        val lifecycleOwner = LocalLifecycleOwner.current
        var doSinglePhoto by remember {mutableStateOf(false)}
        var doMultiplePhotos by remember {mutableStateOf(false)}
        var doStacking by remember {mutableStateOf(false)}

        if (doSinglePhoto) {
            TakeSinglePhoto(
                controller = controller,
                onFinished = {doSinglePhoto = false})
        }

        if (doMultiplePhotos) {
            TakeMultiplePhotos(
                controller = controller,
                onFinishedSingle = {},
                onFinishedAll = {doMultiplePhotos = false})
        }

        if (doStacking) {
            // Toast.makeText(context, "Stacking started", Toast.LENGTH_SHORT).show()
            Box(modifier = Modifier.fillMaxSize()) {
            PersistentMessage(
                "Stacking in progress",
                modifier = Modifier.align(Alignment.TopCenter)
                        .padding(top = 250.dp)
            )
            if (Settings.photoStackingBeepEnabled) {
                beep(100,20)
            }
            cameraLib.executeStacking(context, Settings.photoPath)
            if (Settings.photoStackingBeepEnabled) {
                beep(100,20)
            }
            Toast.makeText(context, "Done", Toast.LENGTH_SHORT).show()
            doStacking = false
        }
        }

        LaunchedEffect(Unit) {
            controller.bindToLifecycle(lifecycleOwner)
        }

        cameraLib.getCameraPermission()

        Scaffold(
            bottomBar = {
                BottomBar(
                    {doSinglePhoto = true},
                    onMultiplePhotos = {doMultiplePhotos = true},
                    onStacking = {doStacking = true}
               )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                cameraLib.CameraPreview(
                    controller,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // ----------------------------------------------------------------------
    // TakeSinglePhoto
    // ----------------------------------------------------------------------
    @Composable
    fun TakeSinglePhoto(
        controller: LifecycleCameraController,
        onFinished: () -> Unit
    ) {
        var delay by remember { mutableIntStateOf(Settings.photoBeepDelay) }
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            while (delay > 0) {
                delay(1000)
                delay--
                if (Settings.photoDelayBeepEnabled) {
                    beep(100,20)
                }
            }

            cameraLib.takePhoto(context, controller)
            onFinished()
        }

        // UI countdown
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .zIndex(10f),                                   // important!
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = delay.toString(),
                fontSize = 100.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    // ----------------------------------------------------------------------
    // TakeMultiplePhotos
    // ----------------------------------------------------------------------
    @Composable
    fun TakeMultiplePhotos(
        controller: LifecycleCameraController,
        onFinishedSingle: () -> Unit,
        onFinishedAll: () -> Unit
    ) {
        var delay by remember { mutableIntStateOf(Settings.photoBeepDelay) }
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            while (delay > 0) {
                delay(Settings.delayBetweenPhotos)
                delay--
                if (Settings.photoDelayBeepEnabled) {
                    beep(100,20)
                }
            }

            for (i in 0 until Settings.photoNumMultiple) {
                cameraLib.takePhoto(context, controller)
                delay(200)
                onFinishedSingle()
            }
            onFinishedAll()
        }

        // UI countdown
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .zIndex(10f),                                   // important!
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = delay.toString(),
                fontSize = 100.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    // ----------------------------------------------------------------------
    // BottomBar
    // ----------------------------------------------------------------------
    @Composable
    fun BottomBar(
        onPhoto: () -> Unit,
        onMultiplePhotos: () -> Unit,
        onStacking: () ->Unit){

        NavigationBar {
            NavigationBarItem(
                selected = true,
                onClick = onPhoto,
                icon = {
                    Icon(
                        painterResource(id = R.drawable.photo),
                        contentDescription = null
                    )
                },
                label = { Text("Photo") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Green,
                    unselectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                selected = true,
                onClick = onMultiplePhotos,
                icon = {
                    Icon(
                        painterResource(id = R.drawable.photos),
                        contentDescription = null
                    )
                },
                label = { Text("Multiple Photos") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Green,
                    unselectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                selected = true,
                onClick = onStacking,
                icon = {
                    Icon(
                        painterResource(id = R.drawable.stacking),
                        contentDescription = null
                    )
                },
                label = { Text("Stacking") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Green,
                    unselectedIconColor = Color.White,
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