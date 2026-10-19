package com.michael.myandroidai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Android-native front end for the original my_ai intent/action architecture.
 * Platform-specific Windows actions are replaced by Android intents and
 * explicitly user-enabled AccessibilityService operations.
 */
class MainActivity : Activity() {
    private lateinit var transcript: TextView
    private lateinit var commandInput: EditText
    private val speechRequestCode = 41
    private val audioPermissionRequestCode = 42

    private val knownPackages = mapOf(
        "whatsapp" to "com.whatsapp",
        "telegram" to "org.telegram.messenger",
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "spotify" to "com.spotify.music",
        "settings" to "com.android.settings",
        "phone" to "com.google.android.dialer",
        "messages" to "com.google.android.apps.messaging",
        "camera" to "com.android.camera2",
        "play store" to "com.android.vending"
    )

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
        root.addView(TextView(this).apply {
            text = "MY ANDROID AI"
            textSize = 13f
            letterSpacing = 0.16f
            setTextColor(accent)
        })
        root.addView(TextView(this).apply {
            text = "Your AI. Your Android."
            textSize = 28f
            setTextColor(white)
            setPadding(0, dp(10), 0, dp(6))
        })
        root.addView(TextView(this).apply {
            text = "Built from your original my_ai intent, AI, voice and action architecture."
            textSize = 14f
            setTextColor(android.graphics.Color.rgb(174, 190, 183))
        })

        transcript = TextView(this).apply {
            text = "Ready. Try “open WhatsApp”, “open youtube.com”, “what time is it”, or “help”.\n\nFor on-screen app assistance, manually enable My Android AI in Android Accessibility settings."
            textSize = 15f
            setTextColor(white)
            setPadding(dp(16), dp(18), dp(16), dp(18))
            setBackgroundColor(panel)
        }
        val scroll = ScrollView(this).apply { addView(transcript) }
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { topMargin = dp(22); bottomMargin = dp(14) })

        commandInput = EditText(this).apply {
            hint = "Type a command…"
            setHintTextColor(android.graphics.Color.rgb(139, 158, 149))
            setTextColor(white)
            textSize = 16f
            minLines = 2
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(panel)
        }
        root.addView(commandInput, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(Button(this).apply {
            text = "Send command"
            setOnClickListener { handleCommand(commandInput.text.toString().trim()) }
        }, fullWidthButton())
        root.addView(Button(this).apply {
            text = "Speak"
            setOnClickListener { startVoiceInput() }
        }, fullWidthButton())
        root.addView(Button(this).apply {
            text = "Set up app assistance"
            setOnClickListener {
                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }, fullWidthButton())
        setContentView(root)
    }

    private fun handleCommand(raw: String) {
        val command = AssistantCommandRouter.parse(raw)
        if (command.action == "empty") {
            Toast.makeText(this, "Type or say a command first.", Toast.LENGTH_SHORT).show()
            return
        }
        val response = when (command.action) {
            "help" -> "Try “open WhatsApp”, “open Chrome”, “open youtube.com”, “what time is it”, “what is the date”, “battery”, “back”, “home”, or “read screen”.\n\nAI-provider integration is not yet connected to a secure API backend."
            "open_app" -> openApp(command.argument)
            "open_url" -> openUrl(command.argument)
            "settings" -> launchSettings(false)
            "accessibility_settings" -> launchSettings(true)
            "back" -> if (AssistantAccessibilityService.activeService?.goBack() == true) "Back action sent." else "Accessibility assistance is not active. Enable My Android AI in Accessibility settings first."
            "home" -> if (AssistantAccessibilityService.activeService?.goHome() == true) "Home action sent." else "Accessibility assistance is not active. Enable it in Accessibility settings first."
            "tap_text" -> if (AssistantAccessibilityService.activeService?.clickVisibleText(command.argument) == true) "Tapped “${command.argument}”." else "I couldn't tap that label. Make sure the target app is currently visible and accessibility assistance is enabled."
            "type_text" -> if (AssistantAccessibilityService.activeService?.setFocusedText(command.argument) == true) "Typed the requested text into the focused field." else "I couldn't type into a focused field. Open the target app and select its text field first."
            "read_screen" -> {
                val lines = AssistantAccessibilityService.activeService?.visibleTextSnapshot().orEmpty()
                if (lines.isEmpty()) "No visible text available. Make sure the target app is in the foreground and accessibility assistance is enabled."
                else "Visible screen text:\n" + lines.take(35).joinToString("\n")
            }
            "time" -> "The time is " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())
            "date" -> "Today is " + DateFormat.getDateInstance(DateFormat.FULL).format(Date())
            "battery" -> batteryStatus()
            "stop" -> "Okay. You can close this screen whenever you're ready."
            else -> "I haven't connected an AI reasoning provider for this request yet. I received: “${command.argument}”."
        }
        transcript.text = "You: $raw\n\nAssistant: $response"
        commandInput.text.clear()
    }

    private fun openApp(rawName: String): String {
        val name = rawName.trim()
        if (name.isBlank()) return "Tell me which app to open."
        val packageName = knownPackages[name.lowercase(Locale.ROOT)]
        if (packageName != null) {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return "Opening $name."
            }
        }

        // Resolve installed launcher apps by visible app label; no hidden app inventory is uploaded.
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val matches = packageManager.queryIntentActivities(launcherIntent, 0)
        val match = matches.firstOrNull {
            it.loadLabel(packageManager).toString().equals(name, ignoreCase = true)
        } ?: matches.firstOrNull {
            it.loadLabel(packageManager).toString().contains(name, ignoreCase = true)
        }
        val launch = match?.let { packageManager.getLaunchIntentForPackage(it.activityInfo.packageName) }
        if (launch != null) {
            startActivity(launch)
            return "Opening ${match?.loadLabel(packageManager) ?: name}."
        }
        return "I couldn't find an installed app named “$name”. Check the app name or install it first."
    }

    private fun openUrl(rawUrl: String): String {
        val url = if (rawUrl.startsWith("http://", true) || rawUrl.startsWith("https://", true)) rawUrl else "https://$rawUrl"
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            "Opening $url."
        } catch (_: Exception) {
            "I couldn't open that link: $url"
        }
    }

    private fun launchSettings(accessibility: Boolean): String {
        startActivity(Intent(if (accessibility) android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS else android.provider.Settings.ACTION_SETTINGS))
        return if (accessibility) "Opening Accessibility settings. You decide whether to enable app assistance." else "Opening Android Settings."
    }

    private fun batteryStatus(): String {
        val manager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (level in 0..100) "Battery is at $level%." else "Battery level is unavailable."
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

    @Deprecated("Uses Android speech recognition activity for broad device compatibility")
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
        if (requestCode == audioPermissionRequestCode && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) launchSpeechRecognizer()
        else if (requestCode == audioPermissionRequestCode) Toast.makeText(this, "Microphone permission is needed for voice input. Text input still works.", Toast.LENGTH_LONG).show()
    }

    private fun fullWidthButton() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(7) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
