// --------------------------------------------------------------------------
// Language:    Kotlin
//
// Framework:   Google Jetpack Compose
//
// Package:     com.pascagames.photostudio
//
// Author:      Antonio Pascarella
//
// Version:     Rel. 0.7.0
//
// Date:        June 2026
//
// Module:      FocusPeaking.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.core.graphics.createBitmap
import android.graphics.Color as AndroidColor

// --------------------------------------------------------------------------
// OBJECT FocusPeakingProcessor
// --------------------------------------------------------------------------
object FocusPeakingProcessor {

    fun process(image: ImageProxy): Bitmap? {
        val yBuffer = image.planes[0].buffer
        val width = image.width
        val height = image.height

        // Copy buffer Y in an array
        val yData = ByteArray(yBuffer.remaining())
        yBuffer.get(yData)

        // Use Laplacian to detect edges
        val edges = laplacianEdgeDetect(yData, width, height)

        // Convert to Bitmap
        return bitmapFromGray(edges, width, height)
    }

    // ----------------------------------------------------------------------
    // laplacianEdgeDetect
    // ----------------------------------------------------------------------
    private fun laplacianEdgeDetect(yData: ByteArray, width: Int, height: Int): ByteArray {
        val out = ByteArray(width * height)

        val kernel = arrayOf(
            intArrayOf(0, -1, 0),
            intArrayOf(-1, 4, -1),
            intArrayOf(0, -1, 0)
        )

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var sum = 0

                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = yData[(x + kx) + (y + ky) * width].toInt() and 0xFF
                        sum += pixel * kernel[ky + 1][kx + 1]
                    }
                }

                val v = sum.coerceIn(0, 255)
                out[x + y * width] = v.toByte()
            }
        }

        return out
    }

    // ----------------------------------------------------------------------
    // bitmapFromGray
    // ----------------------------------------------------------------------
    private fun bitmapFromGray(data: ByteArray, width: Int, height: Int): Bitmap {
        val bmp = createBitmap(width, height)
        val pixels = IntArray(width * height)

        for (i in data.indices) {
            val v = data[i].toInt() and 0xFF
            pixels[i] = AndroidColor.argb(255, v, v, v)
        }

        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return bmp
    }
}