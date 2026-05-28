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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme
import androidx.core.net.toUri


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
    // stack
    // ----------------------------------------------------------------------
    // ToDo
    //      Intermediate messages
    //      a) Evaluating shifs
    //      b) Aligning Images
    //      c) STacking images
    // ----------------------------------------------------------------------
    @Composable
    fun Stack(context: Context) {
        Box(modifier = Modifier.fillMaxSize()) {

            Toast.makeText(context, "Stacking started", Toast.LENGTH_SHORT).show()
            //ShowMessage("Stacking in progress", 80.sp)
            if (Settings.stackerBeepEnabled) {
                beep(100, 20)
            }

            val result = stacker.executeStacking(context, Settings.photoPath)
            if (!result) {
                beep(100, 20)
                Toast.makeText(context, "Nothing to stack", Toast.LENGTH_SHORT).show()
            }

            if (Settings.stackerBeepEnabled) {
                beep(100, 20)
            }
            Toast.makeText(context, "Done", Toast.LENGTH_SHORT).show()
        }
    }

    // ----------------------------------------------------------------------
    // analyzeImages
    // ----------------------------------------------------------------------
    // ToDo
    //  !!!!! SERVE UI PER VISUALIZZARE SHIFTS
    // ----------------------------------------------------------------------
    private fun analyzeImages(context: Context) {

        val (shifts, w, h) = stacker.getImagesShifts()
        if (shifts == null)
            Toast.makeText(context, "No images to analyze", Toast.LENGTH_SHORT).show()
        else {
            val n = shifts.count()
            for (i in 0..<n)
                Log.v(TAG, shifts[i].toString())
            Toast.makeText(context, "Done", Toast.LENGTH_SHORT).show()
        }
    }

    // ----------------------------------------------------------------------
    // computeVariance
    // ----------------------------------------------------------------------
    private fun computeVariance(context: Context) {

        val images: List<Uri> = listAllImages(context)
        Log.v(TAG, images.count().toString())

        for (uri in images) {
            val bmp = stacker.loadBitmapFromUri(context, uri)
            if (bmp != null) {
                val sharpness = stacker.laplacianVariance(bmp)
                Log.e(TAG, "File=${uri.lastPathSegment} sharpness=$sharpness")
                bmp.recycle()
            }
        }
    }

    fun listAllImages(context: Context): List<Uri> {
        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        //val uriString = prefs.getString("cameraX_tree_uri", null) ?: return emptyList()
        val uriString = prefs.getString("cameraX", null) ?: return emptyList()

        val treeUri = uriString.toUri()
        val docTree = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()

        return docTree.listFiles()
            .filter { it.isFile && it.name?.endsWith(".jpg", true) == true }
            .map { it.uri }
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

    // ----------------------------------------------------------------------
    // MainScreen
    // ----------------------------------------------------------------------
    @Composable
    fun MainScreen(modifier: Modifier = Modifier) {

        val context = LocalContext.current
        var doStacking by remember { mutableStateOf(false) }
        var doAnalyze by remember { mutableStateOf(false) }
        var doComputeVariance by remember { mutableStateOf(false) }

        // Stack
        if (doStacking) {
            Stack(context)
            doStacking = false
        }

        // Analyze (images shifts)
        if (doAnalyze)  {
            Toast.makeText(context, "Analyzing images", Toast.LENGTH_SHORT).show()
            analyzeImages(context)
            doAnalyze = false
        }

        // Compute Variance
        if (doComputeVariance)  {
            Toast.makeText(context, "Compute Variance", Toast.LENGTH_SHORT).show()
            computeVariance(context)
            doComputeVariance = false
        }

        // UI DI STACKING --------------  RIFARE --------------------
        Scaffold(
            bottomBar = {
                BottomBar(
                    onStacking = { doStacking = true },
                    onAnalyzing = {doAnalyze = true},
                    onComputeVariance = {doComputeVariance = true})
            }
        ) { innerPadding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Text(
                    text = "Stack",
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Green,
                    modifier = Modifier.padding(start = 90.dp, top = 120.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.stack),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 10.dp, top = 250.dp)
                )
            }
        }
    }

    // ----------------------------------------------------------------------
    // BottomBar
    // ----------------------------------------------------------------------
    @Composable
    fun BottomBar(
                    onStacking: () -> Unit,
                    onAnalyzing: () -> Unit,
                    onComputeVariance: () -> Unit) {

        NavigationBar {

            // Stack
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
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
            // Analyze
            NavigationBarItem(
                selected = true,
                onClick = onAnalyzing,
                icon = {
                    Icon(
                        painterResource(id = R.drawable.analyze),
                        contentDescription = null
                    )
                },
                label = { Text("Analyze") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Green,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
            // Stack
            NavigationBarItem(
                selected = true,
                onClick = onComputeVariance,
                icon = {
                    Icon(
                        painterResource(id = R.drawable.variance),
                        contentDescription = null
                    )
                },
                label = { Text("Variance") },
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
}
