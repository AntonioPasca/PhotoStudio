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
// Module:      Stacker.kt
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Environment
import android.util.Log
import java.io.File
import kotlin.math.sqrt
import androidx.core.graphics.createBitmap

// ----------------------------------------------------------------------
// CLASS Stacker
// ----------------------------------------------------------------------
// Public Methods
//      fun executeStacking(context: Context, folder: String)
//      fun getImagesShifts(): Triple<List<Pair<Int, Int>>?, Int, Int>
//      fun getPictures(): Array<File>
//      fun laplacianVariance(bitmap: Bitmap): Double {
//      fun unsharpMask(original: Bitmap, amount: Float = 0.3f): Bitmap
// ----------------------------------------------------------------------
// Used component
//      CameraLib
// ----------------------------------------------------------------------
class Stacker {

    val cameraLib = CameraLib()
    var buffers = mutableListOf<ByteArray>()

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
        onImageShifting: (Int) -> Unit,
        onImagesShifting: () -> Unit
    ): Pair<Boolean, Bitmap?> {

        // Get the shift of the images
        val (shifts, width, height)  = getImagesShifts(onImageShifting)
        
        // No stack possible if there are less than two files
        if (shifts == null)
            return Pair(false, null)

        onImagesShifting()

        // Do the final stack
        val resultArray = stack(buffers, shifts, width, height)

        // Convert to bmp
        val finalBitmap = cameraLib.floatArrayToBitmap(resultArray, width, height)

        // Save
        //val name = "Stacked_${System.currentTimeMillis()}.jpg"
        val name = "StackedImg.jpg"
        cameraLib.saveBitmapToGallery(context, finalBitmap, name, Settings.photoPath)
        return Pair(true, finalBitmap)
    }

    // ----------------------------------------------------------------------
    // getImagesShifts
    // ----------------------------------------------------------------------
    fun getImagesShifts(
        onShifting: (Int) -> Unit
    ): Triple<List<Pair<Int, Int>>?, Int, Int> {

        // Get all the pictures - Return if less than two
        val files = getPictures()

        Log.v(TAG, "Images?")
        Log.v(TAG, files.count().toString())

        buffers.clear()

        if (files.count() < 2)
            return Triple(null, 0, 0)

        // Take the first image to get dimensions
        val refImgIdx = files.count()/2
        val firstBmp = cameraLib.fixBitmapRotation(files[refImgIdx].absolutePath)
        val width = firstBmp.width
        val height = firstBmp.height

        // Add all images
        for ((index, file) in files.withIndex()) {

            onShifting(index)
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

    // ----------------------------------------------------------------------
    // getPictures
    // ----------------------------------------------------------------------
    fun getPictures(): Array<File>{

        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val path = File(picturesDir, "AstroPhoto").absolutePath
        val dir = File(path)

        val files = dir.listFiles { f ->
            f.isFile && (f.extension.lowercase() in listOf("jpg", "jpeg", "png", "dng"))
        } ?: emptyArray()

        return files
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

    // ----------------------------------------------------------------------
    // unsharpMask
    // ----------------------------------------------------------------------
    fun unsharpMask(context: Context, original: Bitmap, amount: Float = 0.3f): Bitmap {
        val w = original.width
        val h = original.height

        val blurred = boxBlur(original)
        val sharpedBitmap = createBitmap(w, h)

        val origPx = IntArray(w * h)
        val blurPx = IntArray(w * h)
        val outPx = IntArray(w * h)

        original.getPixels(origPx, 0, w, 0, 0, w, h)
        blurred.getPixels(blurPx, 0, w, 0, 0, w, h)

        for (i in origPx.indices) {
            val o = origPx[i]
            val b = blurPx[i]

            val rO = (o shr 16) and 0xFF
            val gO = (o shr 8) and 0xFF
            val bO = o and 0xFF

            val rB = (b shr 16) and 0xFF
            val gB = (b shr 8) and 0xFF
            val bB = b and 0xFF

            val r = (rO + (rO - rB) * amount).coerceIn(0f, 255f).toInt()
            val g = (gO + (gO - gB) * amount).coerceIn(0f, 255f).toInt()
            val bC = (bO + (bO - bB) * amount).coerceIn(0f, 255f).toInt()

            outPx[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or bC
        }

        sharpedBitmap.setPixels(outPx, 0, w, 0, 0, w, h)

        // Save the bitmap
        val name = "SharpedImg.jpg"
        cameraLib.saveBitmapToGallery(context, sharpedBitmap, name, Settings.photoPath)
        return sharpedBitmap
    }

    // Private Methods

    // ----------------------------------------------------------------------
    // boxBlur
    // ----------------------------------------------------------------------
    fun boxBlur(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val bmp = createBitmap(w, h)

        val pixels = IntArray(w * h)
        val result = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                var r = 0
                var g = 0
                var b = 0

                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val c = pixels[(y + dy) * w + (x + dx)]
                        r += (c shr 16) and 0xFF
                        g += (c shr 8) and 0xFF
                        b += c and 0xFF
                    }
                }

                r /= 9
                g /= 9
                b /= 9

                result[y * w + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        bmp .setPixels(result, 0, w, 0, 0, w, h)
        return bmp
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
}

// ----------------------------------------------------------------------
// Bitmap.rotate
// ----------------------------------------------------------------------
// Bitmap extension to create a rotation
// ----------------------------------------------------------------------
/*fun Bitmap.rotate(degrees: Float): Bitmap {

    val matrix = Matrix().apply { postRotate(degrees) }
    val bmp = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    return bmp
}*/
