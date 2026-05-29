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
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import kotlin.math.sqrt

// ----------------------------------------------------------------------
// CLASS Stacker
// ----------------------------------------------------------------------
// Public Methods
//      fun executeStacking(context: Context, folder: String)
//      fun getImagesShifts(): Triple<List<Pair<Int, Int>>?, Int, Int>
// ----------------------------------------------------------------------
// Used component
//      CameraLib
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
    ): Boolean {

        // Get the shift of the images
        val (shifts, width, height)  = getImagesShifts()
        
        // No stack possible if there are less than two files
        if (shifts == null)
            return false

        // Do the final stack
        val resultArray = stack(buffers, shifts, width, height)

        // Convert to bmp
        val finalBitmap = cameraLib.floatArrayToBitmap(resultArray, width, height)

        // Save
        val name = "Stacked_${System.currentTimeMillis()}.jpg"
        cameraLib.saveBitmapToGallery(context, finalBitmap, name, Settings.photoPath)
        return true
    }

    // ----------------------------------------------------------------------
    // getImagesShifts
    // ----------------------------------------------------------------------
    fun getImagesShifts(): Triple<List<Pair<Int, Int>>?, Int, Int> {

        // Get all the pictures - Return if less than two
        val files = getPictures()
        if (files.count() < 2)
            return Triple(null, 0, 0)

        // Take the first image to get dimensions
        val refImgIdx = files.count()/2
        val firstBmp = cameraLib.fixBitmapRotation(files[refImgIdx].absolutePath)
        val width = firstBmp.width
        val height = firstBmp.height

        // Add all images
        for ((index, file) in files.withIndex()) {

            val bmpRotated = cameraLib.fixBitmapRotation(file.absolutePath)
            buffers += cameraLib.bitmapToByteArray(bmpRotated)
            bmpRotated.recycle()

            // Free memory every three images
            if (index % 3 == 0) {
                System.gc()
            }
        }

        Log.v(TAG, "BUFFER")
        Log.v(TAG, buffers.size.toString())
        Log.v(TAG, buffers[0].size.toString())

        // Compute shifts
        val shifts = computeShifts(buffers = buffers, w = width, h = height,
                                    maxShift = Settings.stackerMaxShift, refImgIdx)

        return Triple(shifts, width, height)
    }

    // Private Methods

    // ----------------------------------------------------------------------
    // getPictures
    // ----------------------------------------------------------------------
    fun getPictures(): Array<File>{

        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val path = File(picturesDir, "CameraX").absolutePath

        val dir = File(path)

        val files = dir.listFiles { f ->
            f.isFile && (f.extension.lowercase() in listOf("jpg", "jpeg", "png", "dng"))
        } ?: emptyArray()

        return files
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

        // Central window to eliminate borders  ------
        val areaPercentage = Settings.stackerAreaPercentage.toDouble()
        val starRatio = sqrt(areaPercentage) /20
        val endRatio = starRatio *3

        Log.v(TAG,"RATIO")
        Log.v(TAG, starRatio.toString())
        Log.v(TAG, endRatio.toString())
        Log.v(TAG, maxShift.toString())

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

    // ----------------------------------------------------------------------
    // stack
    // ----------------------------------------------------------------------
    private fun stack(
        buffers: List<ByteArray>,
        shifts: List<Pair<Int, Int>>?,
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
                val (dx, dy) = shifts!![b]

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
    // laplacianVariance
    // ----------------------------------------------------------------------
    fun laplacianVariance(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Convert to luminance (Y)
        val gray = DoubleArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            gray[i] = 0.299*r + 0.587*g + 0.114*b
        }

        // Laplacian Kernel  3x3
        val kernel = arrayOf(
            intArrayOf(0,  1, 0),
            intArrayOf(1, -4, 1),
            intArrayOf(0,  1, 0)
        )

        val lap = DoubleArray(width * height)

        for (y in 1 until height-1) {
            for (x in 1 until width-1) {
                var sum = 0.0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val px = x + kx
                        val py = y + ky
                        val weight = kernel[ky+1][kx+1]
                        sum += gray[py * width + px] * weight
                    }
                }
                lap[y * width + x] = sum
            }
        }

        // Compute variance
        val mean = lap.average()
        var variance = 0.0
        for (v in lap) variance += (v - mean) * (v - mean)
        return variance / lap.size
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
