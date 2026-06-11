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
// Module:      CombinedAnalyzer
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import android.graphics.Matrix

// --------------------------------------------------------------------------
// CLASS CombinedAnalyzer
// --------------------------------------------------------------------------
class CombinedAnalyzer(
    private val onFocusPeaking: (Bitmap?) -> Unit,
    private val onHistogram: (FloatArray) -> Unit
) : ImageAnalysis.Analyzer {

    // ----------------------------------------------------------------------
    // analyze
    // ----------------------------------------------------------------------
    override fun analyze(image: ImageProxy) {
        image.use { image ->
            // --- Focus peaking
            val edges = FocusPeakingProcessor.process(image)
            val rotatedEdges = rotateBitmap(edges, image.imageInfo.rotationDegrees)
            onFocusPeaking(rotatedEdges)

            // --- Histogram ---
            val luma = extractLuma(image)
            val hist = computeHistogram(luma)
            val normalized = normalizeHistogram(hist)
            onHistogram(normalized)
        }
    }

    // ----------------------------------------------------------------------
    // computeHistogram
    // ----------------------------------------------------------------------
    private fun computeHistogram(luma: ByteArray): IntArray {
        val hist = IntArray(256)
        for (v in luma) {
            hist[v.toInt() and 0xFF]++
        }
        return hist
    }

    // ----------------------------------------------------------------------
    // extractLuma
    // ----------------------------------------------------------------------
    private fun extractLuma(image: ImageProxy): ByteArray {
        val buffer = image.planes[0].buffer
        buffer.rewind()
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        return data
    }

    // ----------------------------------------------------------------------
    // normalizeHistogram
    // ----------------------------------------------------------------------
    private fun normalizeHistogram(hist: IntArray): FloatArray {
        val max = hist.maxOrNull()?.toFloat() ?: 1f
        return FloatArray(256) { i -> hist[i] / max }
    }

    // ----------------------------------------------------------------------
    // rotateBitmap
    // ----------------------------------------------------------------------
    private fun rotateBitmap(src: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return src

        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }
}