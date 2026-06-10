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
// Module:      Utils.kt
// --------------------------------------------------------------------------
// Utils functions
//      fun formatTime(seconds: Int): String
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.annotation.SuppressLint
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Environment
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ----------------------------------------------------------------------
// beep
// ----------------------------------------------------------------------
fun beep(volume: Int, duration: Int) {

    val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, volume)
    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, duration)
}

// ----------------------------------------------------------------------
// createSessionDirectory
// ----------------------------------------------------------------------
// Creates a new folder in the format <Month><DD>_<HHMMSS> starting from
// Pictures/AstroPhoto
// Output
//      the created relative path
// ----------------------------------------------------------------------
fun createSessionDirectory(): File {
    val baseDir = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_PICTURES
    )

    val astroDir = File(baseDir, "AstroPhoto")
    if (!astroDir.exists()) astroDir.mkdirs()

    val formatter = DateTimeFormatter.ofPattern("MMMdd_HHmmss", Locale.US)
    val sessionName = LocalDateTime.now().format(formatter)

    val sessionDir = File(astroDir, sessionName)
    if (!sessionDir.exists()) sessionDir.mkdirs()

    val relativePath = File(Settings.photoPath, sessionName)

    //return sessionDir
    return relativePath
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
