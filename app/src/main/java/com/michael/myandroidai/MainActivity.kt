package com.michael.myandroidai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var transcript: TextView
    private lateinit var commandInput: EditText
    private val speechRequestCode = 41
    private val audioPermissionRequestCode = 42

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(12, 18, 17)
        window.navigationBarColor = android.graphics.Color.rgb(12, 18, 17)
        buildInterface()
    }

    private fun buildInterface() {
        val background = android.graphics.Color.rgb(12, 18, 17)
        val panel = android.graphics.Color.rgb(25, 34, 31)
        val accent = android.graphics.Color.rgb(83, 210, 157)
        val white = android.graphics.Color.rgb(240, 246, 243)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(20))
            setBackgroundColor(background)
        }

        val title = TextView(this).apply {
            text = "MY ANDROID AI"
            textSize = 13f
            letterSpacing = 0.16f
            setTextColor(accent)
        }
        root.addView(title)

        val headline = TextView(this).apply {
            text = "How can I help?"
            textSize = 30f
            setTextColor(white)
            setPadding(0, dp(10), 0, dp(6))
        }
        root.addView(headline)

        val subtitle = TextView(this).apply {
            text = "Your assistant, on your device. Start with a text command or use voice."
            textSize = 15f
            setTextColor(android.graphics.Color.rgb(174, 190, 183))
        }
        root.addView(subtitle)

        transcript = TextView(this).apply {
            text = "Ready when you are.\n\nApp control will work only through Android features and permissions you choose to enable."
            textSize = 16f
            setTextColor(white)
            setPadding(dp(16), dp(18), dp(16), dp(18))
            setBackgroundColor(panel)
        }
        val transcriptScroll = ScrollView(this).apply {
            addView(transcript)
        }
        root.addView(transcriptScroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { topMargin = dp(24); bottomMargin = dp(16) })

        commandInput = EditText(this).apply {
            hint = "Type a command…"
            setHintTextColor(android.graphics.Color.rgb(139, 158, 149))
            setTextColor(white)
            textSize = 16f
            setSingleLine(false)
            minLines = 2
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(panel)
        }
        root.addView(commandInput, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val send = Button(this).apply {
            text = "Send command"
            setOnClickListener { handleCommand(commandInput.text.toString().trim()) }
        }
        root.addView(send, fullWidthButton())

        val voice = Button(this).apply {
            text = "Speak"
            setOnClickListener { startVoiceInput() }
        }
        root.addView(voice, fullWidthButton())

        val settings = Button(this).apply {
            text = "Set up app assistance"
            setOnClickListener {
                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        root.addView(settings, fullWidthButton())

        setContentView(root)
    }

    private fun handleCommand(command: String) {
        if (command.isBlank()) {
            Toast.makeText(this, "Type or say a command first.", Toast.LENGTH_SHORT).show()
            return
        }
        val normalized = command.lowercase(Locale.ROOT)
        val response = when {
            normalized in listOf("help", "what can you do", "commands") ->
                "I can receive text and voice commands. Use “Set up app assistance” to open Android Accessibility settings. More actions will be added in stages."
            normalized == "open settings" -> {
                startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                "Opening Android Settings."
            }
            normalized == "open accessibility settings" || normalized == "set up app assistance" -> {
                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                "Opening Accessibility settings. You choose whether to enable the service."
            }
            normalized == "go back" || normalized == "back" -> {
                @Suppress("DEPRECATION")
                onBackPressed()
                "Going back where Android allows it."
            }
            else ->
                "I heard: “$command”\n\nThis starter does not yet execute arbitrary app actions. Next, commands will be connected to explicit, user-authorized Android actions."
        }
        transcript.text = "You: $command\n\nAssistant: $response"
        commandInput.text.clear()
    }

    private fun startVoiceInput() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), audioPermissionRequestCode)
            return
        }
        launchSpeechRecognizer()
    }

    private fun launchSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to My Android AI")
        }
        try {
            startActivityForResult(intent, speechRequestCode)
        } catch (_: Exception) {
            Toast.makeText(this, "No speech recognition service is available on this device.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Uses the platform speech recognition activity for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == speechRequestCode && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            commandInput.setText(spoken)
            handleCommand(spoken)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == audioPermissionRequestCode && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            launchSpeechRecognizer()
        } else if (requestCode == audioPermissionRequestCode) {
            Toast.makeText(this, "Microphone permission is needed for voice input. Text input still works.", Toast.LENGTH_LONG).show()
        }
    }

    private fun fullWidthButton() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(7) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
