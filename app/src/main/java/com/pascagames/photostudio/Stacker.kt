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
// Module:      Stacker.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.io.File

// ----------------------------------------------------------------------
// CLASS Stacker
// ----------------------------------------------------------------------
// Uses component CameraLib
// ----------------------------------------------------------------------
class Stacker {

    val cameraLib = CameraLib()
    val buffers = mutableListOf<ByteArray>()

    // ----------------------------------------------------------------------
    // executeStacking
    // ----------------------------------------------------------------------
    // Todo
    //      più veloce (con buffer preallocati)
    //      più preciso (stacking a colori, non solo grayscale)
    //      più “astro” (sigma‑clipping, dark frame, flat field)
    // ----------------------------------------------------------------------
    fun executeStacking(
        context: Context,
        folder: String
    ) {
        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val path = File(picturesDir, "CameraX").absolutePath

        val dir = File(path)

        val files = dir.listFiles { f ->
            f.isFile && (f.extension.lowercase() in listOf("jpg", "jpeg", "png"))
        } ?: emptyArray()

        if (files.isEmpty()) return

        // Save the first image to get dimensions
        val refImgIdx = files.count()/2
        val firstBmp = cameraLib.fixBitmapRotation(files[refImgIdx].absolutePath)
        val width = firstBmp.width
        val height = firstBmp.height

        Log.v(TAG,"EX_1")
        Log.v(TAG,refImgIdx.toString() )

        // Add all images
        for ((index, file) in files.withIndex()) {

            val bmpRotated = cameraLib.fixBitmapRotation(file.absolutePath)
            addFrame(bmpRotated)

            // Free memory every three images
            if (index % 3 == 0) {
                System.gc()
            }
        }

        Log.v(TAG,"EX_2")
        // Compute shifts
        val shifts = computeShifts(buffers = buffers, w = width, h = height, maxShift = 50, refImgIdx)
        Log.v(TAG, shifts.toString())

        // Do the final stack
        val resultArray = stack(buffers, shifts, width, height)

        // Convert to bmp
        val finalBitmap = cameraLib.floatArrayToBitmap(resultArray, width, height)

        // Save
        val name = "Stacked_${System.currentTimeMillis()}.jpg"
        cameraLib.saveBitmapToGallery(context, finalBitmap, name, Settings.photoPath)
    }
    // Private Methods

    // ----------------------------------------------------------------------
    // addFrame
    // ----------------------------------------------------------------------
    private fun addFrame(bmp: Bitmap) {

        buffers += cameraLib.bitmapToByteArray(bmp)
        bmp.recycle()
    }

    // ----------------------------------------------------------------------
    // stack
    // ----------------------------------------------------------------------
    private fun stack(
        buffers: List<ByteArray>,
        shifts: List<Pair<Int, Int>>,
        w: Int,
        h: Int
    ): FloatArray {

        val totalPixels = w * h
        val result = FloatArray(totalPixels)
        val temp = IntArray(buffers.size)

        for (i in 0 until totalPixels) {

            val x = i % w
            val y = i / w

            for (b in buffers.indices) {
                val (dx, dy) = shifts[b]

                val xx = x + dx
                val yy = y + dy

                temp[b] =
                    if (xx !in 0 until w || yy !in 0 until h) {
                        0
                    } else {
                        val idx = yy * w + xx
                        buffers[b][idx].toInt() and 0xFF
                    }
            }
            temp.sort()
            result[i] = temp[temp.size / 2].toFloat()
        }
        return result
    }

    // ----------------------------------------------------------------------
    // computeShifts
    // ----------------------------------------------------------------------
    private fun computeShifts(
        buffers: List<ByteArray>,
        w: Int,
        h: Int,
        maxShift: Int,
        refImgIdx: Int
    ): List<Pair<Int, Int>> {

        val shifts = MutableList(buffers.size) { Pair(0, 0) }
        val refImage = buffers[refImgIdx]

        for (i in 0 until buffers.size) {
            val img = buffers[i]
            if (refImgIdx != i) {
                shifts[i] = estimateShift(imageRef = refImage, img = img, w = w, h = h,
                        maxShift = maxShift
            )}
            else
                shifts[i] = Pair(0,0)

            Log.v(TAG, shifts[i].toString())
        }
        return shifts
    }

    // ----------------------------------------------------------------------
    // estimateShift
    // ----------------------------------------------------------------------
    // Estimates the shift (dxm dy) between two images (represented as
    // liner FloatArray) in grayscale
    //
    //  imageRef    reference image
    //  img         imagine da align
    //  w           width
    //  h           height
    //  maxShift    max shift to consider
    // ----------------------------------------------------------------------
    private fun estimateShift(
        imageRef: ByteArray,
        img: ByteArray,
        w: Int,
        h: Int,
        maxShift: Int
    ): Pair<Int, Int> {

        val start = System.currentTimeMillis()
        var bestDx = 0
        var bestDy = 0
        var bestScore = Float.NEGATIVE_INFINITY

        // Central window to eliminate borders  ------ USARE SETTINGS
        val startX = w / 4
        val endX = w * 3 / 4
        val startY = h / 4
        val endY = h * 3 / 4

        for (dy in -maxShift..maxShift) {
            for (dx in -maxShift..maxShift) {

                var score = 0f

                for (y in startY until endY) {
                    val yy = y + dy
                    if (yy !in 0 until h) continue

                    val baseRef = y * w
                    val baseImg = yy * w

                    for (x in startX until endX) {
                        val xx = x + dx
                        if (xx !in 0 until w) continue

                        val r = imageRef[baseRef + x].toInt() and 0xFF
                        val v = img[baseImg + xx].toInt() and 0xFF

                        score += r * v
                    }
                }

                if (score > bestScore) {
                    bestScore = score
                    bestDx = dx
                    bestDy = dy
                }
            }
        }
        val end = System.currentTimeMillis()
        Log.v(TAG, "Execution time = ${end - start} ms")
        return Pair(bestDx, bestDy)
    }
}

// ----------------------------------------------------------------------
// Bitmap.rotate
// ----------------------------------------------------------------------
// Bitmap extension to create a rotation
// ----------------------------------------------------------------------
fun Bitmap.rotate(degrees: Float): Bitmap {

    val matrix = Matrix().apply { postRotate(degrees) }
    val bmp = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    return bmp
}