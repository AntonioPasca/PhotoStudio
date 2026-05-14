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
// Module:      VoiceRecognizer.kt
// --------------------------------------------------------------------------
//  VoiceModel
//      fun init(context: Context)
//      fun startListening()
//      fun updateVolume(newValue: Float) - Not Used
//
// VoiceRecognizer
//      fun initSpeakRecognizer(language: String, context: Context)
//      fun startListening()
//      override fun onReadyForSpeech(params: Bundle?)
//      override fun onRmsChanged(rmsdB: Float)
//      override fun onBufferReceived(buffer: ByteArray?)
//      override fun onPartialResults(partialResults: Bundle?)
//      override fun onEvent(eventType: Int, params: Bundle?)
//      override fun onBeginningOfSpeech()
//      override fun onEndOfSpeech()
//      override fun onError(error: Int)
//      override fun onResults(results: Bundle?)
// --------------------------------------------------------------------------
package com.pascagames.photostudio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ----------------------------------------------------------------------------
// CLASS VoiceViewModel
// ----------------------------------------------------------------------------
class VoiceViewModel : ViewModel() {

    private val _volume = MutableStateFlow(0f)
    val volume = _volume.asStateFlow()

    private val _recognizedWord = MutableStateFlow<List<String>>(emptyList())
    val recognizedWord = _recognizedWord.asStateFlow()

    lateinit var recognizer: VoiceRecognizer

    // ----------------------------------------------------------------------------
    // init
    // ----------------------------------------------------------------------------
    fun init(context: Context) {
        recognizer = VoiceRecognizer(
            onVolumeChanged = { v -> _volume.value = v },
            onTransWordReady = { words -> _recognizedWord.value = words }
        )
        recognizer.initSpeakRecognizer("it-IT", context)
    }

    // ----------------------------------------------------------------------------
    // startListening
    // ----------------------------------------------------------------------------
    fun startListening() {

        recognizer.startListening()
    }

    // ----------------------------------------------------------------------------
    // updateVolume
    // ----------------------------------------------------------------------------
    fun updateVolume(newValue: Float) {
        _volume.value = newValue
    }
}

// ----------------------------------------------------------------------------
// CLASS VoiceRecognizer
// ----------------------------------------------------------------------------
class VoiceRecognizer(
        private val onVolumeChanged: (Float) -> Unit,
        private val onTransWordReady: (List<String>) -> Unit) : RecognitionListener {

    private lateinit var recognizerIntent: Intent
    private lateinit var speechRecognizer: SpeechRecognizer

    // ---------------------------------------------------------------
    // initSpeakRecognizer
    // ------------------------------------------------------------------------
    fun initSpeakRecognizer(language: String, context: Context) {

        // Set Recognizer Intent
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
        )
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

        // If partial result should be used
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
    }

    // ------------------------------------------------------------------------
    // startListening
    // ------------------------------------------------------------------------
    fun startListening() {

        speechRecognizer.startListening(recognizerIntent)
    }

    // ------------------------------------------------------------------------
    // Recognition Listener Interface
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // onReadyForSpeech
    // ------------------------------------------------------------------------
    override fun onReadyForSpeech(params: Bundle?) {
    }

    // ------------------------------------------------------------------------
    // onRmsChanged
    // ------------------------------------------------------------------------
    override fun onRmsChanged(rmsdB: Float) {

        onVolumeChanged(rmsdB)
    }

    // ------------------------------------------------------------------------
    // onBufferReceived
    // ------------------------------------------------------------------------
    override fun onBufferReceived(buffer: ByteArray?) {
    }

    // ------------------------------------------------------------------------
    // onPartialResults
    // ------------------------------------------------------------------------
    override fun onPartialResults(partialResults: Bundle?) {

        val matches = partialResults
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?: return

        onTransWordReady(matches)
    }

    // ------------------------------------------------------------------------
    // onEvent
    // ------------------------------------------------------------------------
    override fun onEvent(eventType: Int, params: Bundle?) {
    }

    // ------------------------------------------------------------------------
    // onBeginningOfSpeech
    // ------------------------------------------------------------------------
    override fun onBeginningOfSpeech() {
    }

    // ------------------------------------------------------------------------
    // onEndOfSpeec
    // ------------------------------------------------------------------------
    override fun onEndOfSpeech() {

        //speechRecognizer.startListening(recognizerIntent)
    }

    // ------------------------------------------------------------------------
    // onError
    // ------------------------------------------------------------------------
    override fun onError(error: Int) {

        onTransWordReady(listOf("ERROR: $error"))
    }

    // ------------------------------------------------------------------------
    // onResults
    // ------------------------------------------------------------------------
    override fun onResults(results: Bundle?) {

        if (results != null) {
            val matches: java.util.ArrayList<String>? =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

            if (matches != null) {
                onTransWordReady(matches)
            }
        }
    }
}
