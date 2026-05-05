// --------------------------------------------------------------------------
// Language:    Kotlin
//
// Framework:   Google Jetpack Compose
//
// Package:     com.pascagames.photostudio
//
// Author:      Antonio Pascarella
//
// Version:     Rel. 0.3.0
//
// Date:        May 2026
//
// Module:      SettingsActivity.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme

// --------------------------------------------------------------------------------------------
// SettingsActivity
// --------------------------------------------------------------------------------------------
class SettingsActivity : ComponentActivity() {

    private var backToCaller: (Unit) -> Unit = { back() }

    // --------------------------------------------------------------------------------------------
    // onCreate
    // --------------------------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PhotoStudioTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { TopBar("Settings", backToCaller) }) { innerPadding ->
                    MainScreen(
                        //modifier = Modifier.padding(innerPadding)
                    )
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

    // --------------------------------------------------------------------------
    // MainScreen
    // --------------------------------------------------------------------------
    @Composable
    fun MainScreen() {
    //fun MainScreen(viewModel: SettingsViewModel = viewModel()) {
    //    val settings by viewModel.settings.collectAsState()

        Column {
            SettingSwitch(
                label = "Photo beep",
                description = "Enable beep when taking a photo",
                value = true,
                //onValueChange = { viewModel.setPhotoBeep() }
                onValueChange = {}
            )

            SettingSwitch(
                label = "Video beep",
                value = true,
                //onValueChange = { viewModel.setStartVideoBeep() }
                onValueChange = {}
            )
        }
    }
}