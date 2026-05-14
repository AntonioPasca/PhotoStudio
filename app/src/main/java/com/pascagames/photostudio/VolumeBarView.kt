// --------------------------------------------------------------------------
// Language:    Kotlin
//
// Framework:   Google Jetpack Compose
//
// Package:     com.pascagames.photostudio
//
// Author:      Antonio Pascarella
//
// Version:     Rel. 0.4.0
//
// Date:        May 2026
//
// Module:      VolumeBarView.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun VolumeBar(
    volume: Float,
    modifier: Modifier = Modifier
) {
    val sizeW = 80f
    val sizeH = 150f

    val scaledVolume = (volume + 2) * 8
    val fillVol = (sizeH - 1) - scaledVolume

    val color = when {
        scaledVolume < 10f -> Color.Yellow
        scaledVolume > 150f -> Color.Red
        else -> Color.Green
    }

    Canvas(
        modifier = modifier
            .size(width = sizeW.dp, height = sizeH.dp)
            .padding(start = 50.dp)
    ) {
        // Full rectangle
        drawRect(
            color = color,
            topLeft = Offset(1f, fillVol),
            size = Size(sizeW, sizeH - fillVol - 1)
        )

        // Border
        drawRect(
            color = Color.Black,
            topLeft = Offset(2f, 2f),
            size = Size(sizeW - 4, sizeH - 4),
            style = Stroke(width = 2f)
        )
    }
}
