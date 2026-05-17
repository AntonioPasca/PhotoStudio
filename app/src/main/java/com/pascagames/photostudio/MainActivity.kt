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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme


private const val APP_NAME = "PhotoStudio"
private const val VERSION =  "Ver 0.5.2"
const val TAG = "PHOTO"

// --------------------------------------------------------------------------
// MainActivity
// --------------------------------------------------------------------------
class MainActivity : ComponentActivity() {

    // ----------------------------------------------------------------------
    // onCreate
    // ----------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PhotoStudioTheme {
                Scaffold { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // callPhotoActivity
    // ----------------------------------------------------------------------
    fun callPhotoActivity() {

        val intent = Intent(this@MainActivity, PhotoActivity::class.java)
        startActivity(intent)
    }

    // ----------------------------------------------------------------------
    // callVideoActivity
    // ----------------------------------------------------------------------
    fun callVideoActivity() {

        val intent = Intent(this@MainActivity, VideoActivity::class.java)
        startActivity(intent)
    }
    // ----------------------------------------------------------------------
    // settings
    // ----------------------------------------------------------------------
    fun settings() {

        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
        startActivity(intent)
    }

    // ----------------------------------------------------------------------
    // info
    // ----------------------------------------------------------------------
    fun info() {

        val intent = Intent(this@MainActivity, InfoActivity::class.java)
        val bundle = Bundle()
        bundle.putString("APP", APP_NAME)
        bundle.putString("VERSION", VERSION)
        intent.putExtra("main_activity_data", bundle)
        startActivity(intent)
    }

    // ----------------------------------------------------------------------
    // MainScreen
    // ----------------------------------------------------------------------
    @Composable
    fun MainScreen(modifier: Modifier = Modifier) {

        Scaffold(
            bottomBar = {
                BottomBar()
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
                Text(
                    text = "PhotoStudio",
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Blue,
                    modifier = Modifier.padding(start = 30.dp, top = 80.dp)
                )
                Text(
                    text = VERSION,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Blue,
                    modifier = Modifier.padding(start = 130.dp, top = 180.dp)
                )
            }
        }
    }

    // ----------------------------------------------------------------------
    // BottomBar
    // ----------------------------------------------------------------------
    @Composable
    fun BottomBar() {

        NavigationBar {
            NavigationBarItem(
                selected = true,
                onClick = {callPhotoActivity()},
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
                onClick = {callVideoActivity()},
                icon = {
                    Icon(
                        painterResource(id = R.drawable.movie),
                        contentDescription = null
                    )
                },
                label = { Text("Video")},
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