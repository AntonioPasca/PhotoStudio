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
// Module:      InfoActivity.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme


// --------------------------------------------------------------------------------------------
// InfoActivity
// --------------------------------------------------------------------------------------------
class InfoActivity : ComponentActivity() {

    private var backToCaller: (Unit) -> Unit = { back() }

    // --------------------------------------------------------------------------------------------
    // onCreate
    // --------------------------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get vars from Main
        val bundle = intent.getBundleExtra("main_activity_data")
        val app = bundle!!.getString("APP")
        val version = bundle.getString("VERSION")

        setContent {
            PhotoStudioTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { TopBar("Info", backToCaller) }) { innerPadding ->
                    MainScreen(
                        app,
                        version,
                        modifier = Modifier.padding(innerPadding)
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

    // --------------------------------------------------------------------------------------------
    // MainScreen
    // --------------------------------------------------------------------------------------------
    @Composable
    fun MainScreen(app: String?, version: String?, modifier: Modifier = Modifier) {
        Column(
            modifier = Modifier.padding(start = 0.dp, top = 120.dp),
        ) {
            ViewItem("App version", "$app - $version", modifier)
            ViewItem("SDK version", "SDK Version = " + Build.VERSION.SDK_INT.toString(), modifier)
            ViewItem("OS version", Build.VERSION.RELEASE, modifier)
            ViewItem("Security Patch", Build.VERSION.SECURITY_PATCH, modifier)
            ViewItem("CameraX", "1.6.1", modifier)
        }
    }

    // --------------------------------------------------------------------------------------------
    // ViewItem
    // --------------------------------------------------------------------------------------------
    @Composable
    fun ViewItem(title: String, subTitle: String, modifier: Modifier) {
        Column {
            Text(
                text = title,
                modifier = Modifier.padding(start = 20.dp, top = 40.dp),
                style = TextStyle(
                    fontSize = 18.sp,
                )
            )
            Text(
                text = subTitle,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp),
                style = TextStyle(
                    fontSize = 14.sp
                )
            )
        }
    }
}
