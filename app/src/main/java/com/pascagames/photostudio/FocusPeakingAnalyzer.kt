package com.pascagames.photostudio

import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.graphics.createBitmap

// ----------------------------------------------------------------------
// CLASS FocusPeakingAnalyzer
// ----------------------------------------------------------------------
// Public Methods
//      override fun analyze(image: ImageProxy)
// ----------------------------------------------------------------------
class FocusPeakingAnalyzer(
    private val onEdgesReady: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    // ----------------------------------------------------------------------
    // analyze
    // ----------------------------------------------------------------------
    override fun analyze(image: ImageProxy) {

        val bitmap = image.toBitmap()
        val edges = detectEdges(bitmap)
        onEdgesReady(edges)
        image.close()
    }

    // ----------------------------------------------------------------------
    // detectEdges
    // ----------------------------------------------------------------------
    private fun detectEdges(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val out = createBitmap(w, h)

        val laplacian = arrayOf(
            intArrayOf(0,  1, 0),
            intArrayOf(1, -4, 1),
            intArrayOf(0,  1, 0)
        )

        val pixels = IntArray(w * h)
        val result = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                var sum = 0

                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val px = pixels[(y + ky) * w + (x + kx)]
                        val gray = (px shr 16 and 0xFF) +
                                (px shr 8 and 0xFF) +
                                (px and 0xFF)
                        sum += gray * laplacian[ky + 1][kx + 1]
                    }
                }

                val v = if (sum > 200) 255 else 0  // threshold
                result[y * w + x] = Color.argb(v, 255, 0, 0) // red
            }
        }

        out.setPixels(result, 0, w, 0, 0, w, h)
        return out
    }
}
