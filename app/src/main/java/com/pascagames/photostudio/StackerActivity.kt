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
// Module:      StackerActivity.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.pascagames.photostudio.ui.theme.PhotoStudioTheme
import kotlinx.coroutines.delay


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
    // ----------------------------------------------------------------------
    @Composable
    fun Stack(
                context: Context,
                onImageShifting: (Int) -> Unit,
                onImagesShifting: () -> Unit) {
        Box(modifier = Modifier.fillMaxSize()) {

            Toast.makeText(context, "Stacking started", Toast.LENGTH_SHORT).show()
            if (Settings.stackerBeepEnabled) {
                beep(100, 20)
            }

            val (result, stackedBitmap) = stacker.executeStacking(context, onImageShifting, onImagesShifting)
            if (result) {
                Toast.makeText(context, "Sharpening", Toast.LENGTH_SHORT).show()
                val sharpened = stacker.unsharpMask(context,stackedBitmap!!, Settings.stackerSharpening.toFloat())
            }
            else {
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
    private fun analyzeImages(context: Context,
                              onImageShifting: (Int) -> Unit,): List<Pair<Int, Int>>? {

        val (shifts, _, _) = stacker.getImagesShifts(onImageShifting)
        if (shifts == null)
            Toast.makeText(context, "No images to analyze", Toast.LENGTH_SHORT).show()
        else {
            val n = shifts.count()
            for (i in 0..<n)
                Log.v(TAG, shifts[i].toString())
            Toast.makeText(context, "Done", Toast.LENGTH_SHORT).show()
        }
        return shifts
    }

    // ----------------------------------------------------------------------
    // computeVariance
    // ----------------------------------------------------------------------
    private fun computeVariance(context: Context): List<Double> {

        val sharps = mutableListOf<Double>()
        val files = stacker.getPictures()
        for ((_, file) in files.withIndex()) {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            if (bmp != null) {
                val sharpness = stacker.laplacianVariance(bmp)
                sharps.add(sharpness)
                bmp.recycle()
                Log.v(TAG, sharpness.toString())
            }
        }
        return sharps
    }


    /* How to use variance to discard bad images
    val scores = frames.map { frame ->
        laplacianVariance(frame)
    }

    // Sort
    val sorted = scores.zip(frames).sortedByDescending { it.first }

    // Get top 30%
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
        var shifts by remember { mutableStateOf<List<Pair<Int, Int>>?>(emptyList()) }
        var sharps by remember { mutableStateOf<List<Double>>(emptyList()) }

        var message by remember { mutableStateOf<String?>(null) }

        // Auto-hide message
        LaunchedEffect(message) {
            if (message != null) {
                delay(5000)
                message = null
            }
        }

        // Show message
        if (message != null)
            ShowMessage(message!!, 22.sp)

        // Stack
        if (doStacking) {
            Stack(
                context,
                onImageShifting = {message = "Image shifted"},
                onImagesShifting = {message = "Images shifted"})
            doStacking = false
        }

        // Analyze (images shifts)
        if (doAnalyze)  {
            Toast.makeText(context, "Analyzing images", Toast.LENGTH_SHORT).show()
            shifts = analyzeImages(
                                    context,
                                    onImageShifting = {message = "Image shifted"},
                                  )
            doAnalyze = false
        }

        // Compute Variance
        if (doComputeVariance)  {
            Toast.makeText(context, "Compute Variance", Toast.LENGTH_SHORT).show()
            sharps = computeVariance(context)
            doComputeVariance = false
        }

        // Stacking UI
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Stack",
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Green,
                        modifier = Modifier.padding(start = 90.dp, top = 80.dp)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.stack),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 10.dp, top = 40.dp)
                    )

                    // Show the shifts of each image
                    if (!shifts.isNullOrEmpty()) {
                        Text(
                            text = "Shifts",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 40.dp)
                        )
                        shifts!!.forEachIndexed { index, v ->
                            val msg1 = "Photo " + (index+1).toString()
                            val msg2 = " = (" + v.first.toString() + ", " + v.second.toString() + ")"
                            Text(
                                 msg1+msg2,
                                 fontSize = 20.sp
                            )
                        }
                    }
                    else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        )
                    }

                    // Show the variance of each image
                    if (sharps.isNotEmpty()) {
                        Text(
                            text = "Variance",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 40.dp)
                        )

                        sharps.forEachIndexed { index, v ->
                            Text(
                                "Photo" + (index+1).toString() + " → ${"%.2f".format(v)}",
                                fontSize = 20.sp)
                        }
                    }
                }
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
