package com.michael.myandroidai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import java.util.Locale

/** Short-lived speech capture activity used when the floating assistant is over another app. */
class VoiceCommandActivity : Activity() {
    private val requestCode = 71
    private val permissionCode = 72

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), permissionCode)
        } else {
            launchRecognizer()
        }
    }

    private fun launchRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to My Android AI")
        }
        try {
            startActivityForResult(intent, requestCode)
        } catch (_: Exception) {
            Toast.makeText(this, "No speech recognition service is available.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    @Deprecated("Uses Android speech recognition activity for compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == this.requestCode && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) {
                val service = AssistantAccessibilityService.activeService
                if (service != null) service.runCommandFromOverlay(spoken)
                else Toast.makeText(this, "Enable My Android AI in Accessibility settings first.", Toast.LENGTH_LONG).show()
            }
        }
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionCode && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) launchRecognizer()
        else {
            Toast.makeText(this, "Microphone permission is needed for voice input.", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
