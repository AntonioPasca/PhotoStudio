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
// Module:      StackerActivity.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme

// --------------------------------------------------------------------------
// StackerActivity
// --------------------------------------------------------------------------
class StackerActivity : ComponentActivity() {

    private var backToCaller: (Unit) -> Unit = { back() }
    val stacker = Stacker()

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
                Scaffold(topBar = {TopBarEx("Stacker", actions, backToCaller)}) {
                    innerPadding ->
                        MainScreen(
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

    // ----------------------------------------------------------------------
    // settings
    // ----------------------------------------------------------------------
    fun settings() {

        val intent = Intent(this@StackerActivity, SettingsActivity::class.java)
        val bundle = Bundle()
        bundle.putInt("SETTINGS_INDEX", SETTINGS_STACKER_INDEX)
        intent.putExtra("activity_data", bundle)
        startActivity(intent)
    }

    // ----------------------------------------------------------------------
    // MainScreen
    // ----------------------------------------------------------------------
    @Composable
    fun MainScreen(modifier: Modifier = Modifier) {

        val context = LocalContext.current
        var doStacking by remember { mutableStateOf(false) }

        if (doStacking) {

            // Toast.makeText(context, "Stacking started", Toast.LENGTH_SHORT).show()
            Box(modifier = Modifier.fillMaxSize()) {
                PersistentMessage(
                    "Stacking in progress",
                    modifier = Modifier.align(Alignment.TopCenter)
                        .padding(top = 250.dp)
                )
                if (Settings.stackerBeepEnabled) {
                    beep(100, 20)
                }

                stacker.executeStacking(context, Settings.photoPath)

                if (Settings.stackerBeepEnabled) {
                    beep(100, 20)
                }
                Toast.makeText(context, "Done", Toast.LENGTH_SHORT).show()
                doStacking = false
            }
        }

        Scaffold(
            bottomBar = {
                BottomBar(onStacking = { doStacking = true })
            }
        ) { innerPadding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.moon),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 0.dp, top = 350.dp)
                )
            }
        }
    }

    // ----------------------------------------------------------------------
    // BottomBar
    // ----------------------------------------------------------------------
    @Composable
    fun BottomBar(onStacking: () -> Unit) {

        NavigationBar {
            // Stacking
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
}
