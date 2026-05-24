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
// Module:      Utils.kt
// --------------------------------------------------------------------------
// Utils functions
//      fun formatTime(seconds: Int): String
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.annotation.SuppressLint
import android.media.AudioManager
import android.media.ToneGenerator

// ----------------------------------------------------------------------
// beep
// ----------------------------------------------------------------------
fun beep(volume: Int, duration: Int) {

    val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, volume)
    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, duration)
}

// ----------------------------------------------------------------------
// formatTime
// ----------------------------------------------------------------------
@SuppressLint("DefaultLocale")
fun formatTime(seconds: Int): String {

    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
