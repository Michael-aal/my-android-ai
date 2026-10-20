package com.michael.myandroidai

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Android-native front end for the original my_ai intent/action architecture. */
class MainActivity : Activity() {
    private lateinit var transcript: TextView
    private lateinit var commandInput: EditText
    private var textToSpeech: TextToSpeech? = null
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
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) textToSpeech?.language = Locale.getDefault()
        }
        buildInterface()
        if (intent?.action == "com.michael.myandroidai.ACTION_SEARCH") {
            val query = intent.getStringExtra("search_query").orEmpty()
            if (query.isNotBlank()) SearchPreferences.showSearchChoice(this, query, rememberChoice = false)
        } else if (intent?.action == "com.michael.myandroidai.ACTION_ROBOT_SETTINGS") {
            showRobotSettingsDialog()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.action == "com.michael.myandroidai.ACTION_SEARCH") {
            val query = intent.getStringExtra("search_query").orEmpty()
            if (query.isNotBlank()) SearchPreferences.showSearchChoice(this, query, rememberChoice = false)
        } else if (intent?.action == "com.michael.myandroidai.ACTION_ROBOT_SETTINGS") {
            showRobotSettingsDialog()
        }
    }

    private fun showRobotSettingsDialog() {
        val nameField = EditText(this).apply {
            setSingleLine(true)
            hint = "Robot name"
            setText(RobotPreferences.name(this@MainActivity))
            setSelection(text.length)
        }
        val moveToggle = android.widget.CheckBox(this).apply {
            text = "Move gently around the screen"
            isChecked = RobotPreferences.autoMove(this@MainActivity)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
            addView(TextView(this@MainActivity).apply {
                text = "Choose a name and movement style for your floating robot."
                setTextColor(android.graphics.Color.WHITE)
            })
            addView(nameField)
            addView(moveToggle)
        }
        AlertDialog.Builder(this)
            .setTitle("Robot companion")
            .setView(content)
            .setPositiveButton("Save") { _, _ ->
                RobotPreferences.setName(this, nameField.text.toString())
                RobotPreferences.setAutoMove(this, moveToggle.isChecked)
                Toast.makeText(this, "Saved settings for ${RobotPreferences.name(this)}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun buildInterface() {
        val background = android.graphics.Color.rgb(8, 12, 22)
        val panel = android.graphics.Color.rgb(17, 24, 39)
        val panelRaised = android.graphics.Color.rgb(23, 32, 50)
        val accent = android.graphics.Color.rgb(110, 231, 255)
        val purple = android.graphics.Color.rgb(167, 139, 250)
        val white = android.graphics.Color.rgb(241, 245, 249)
        val muted = android.graphics.Color.rgb(148, 163, 184)
        val stroke = android.graphics.Color.rgb(48, 61, 83)

        fun rounded(color: Int, radius: Int, borderColor: Int = android.graphics.Color.TRANSPARENT) =
            android.graphics.drawable.GradientDrawable().apply {
                setColor(color)
                cornerRadius = dp(radius).toFloat()
                if (borderColor != android.graphics.Color.TRANSPARENT) {
                    setStroke(dp(1), borderColor)
                }
            }

        fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
            TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

        window.statusBarColor = background
        window.navigationBarColor = background
        window.decorView.systemUiVisibility = 0

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(14))
            setBackgroundColor(background)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val brandMark = TextView(this).apply {
            text = "✦"
            textSize = 25f
            gravity = android.view.Gravity.CENTER
            setTextColor(background)
            this.background = rounded(accent, 15)
        }
        header.addView(brandMark, LinearLayout.LayoutParams(dp(48), dp(48)))
        val brandText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            addView(label("MY ANDROID AI", 15f, white, true))
            addView(label("PERSONAL INTELLIGENCE", 10f, muted, true).apply {
                letterSpacing = 0.12f
                setPadding(0, dp(3), 0, 0)
            })
        }
        header.addView(brandText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val online = TextView(this).apply {
            text = "● READY"
            textSize = 10f
            letterSpacing = 0.06f
            setTextColor(accent)
            gravity = android.view.Gravity.CENTER
            setPadding(dp(11), dp(8), dp(11), dp(8))
            this.background = rounded(panelRaised, 18, stroke)
        }
        header.addView(online)
        root.addView(header)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            this.background = rounded(panel, 22, stroke)
        }
        hero.addView(label("YOUR ASSISTANT, REIMAGINED", 10f, purple, true).apply {
            letterSpacing = 0.12f
        })
        hero.addView(label("Think beyond\nthe screen.", 29f, white, true).apply {
            setPadding(0, dp(8), 0, dp(7))
            setLineSpacing(dp(1).toFloat(), 1.0f)
        })
        hero.addView(label("Ask a question, speak naturally, or tell me what to do on your phone.", 13f, muted).apply {
            setLineSpacing(dp(4).toFloat(), 1.0f)
        })
        root.addView(hero, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(20) })

        root.addView(label("CONVERSATION", 11f, muted, true).apply {
            letterSpacing = 0.13f
            setPadding(dp(2), dp(20), 0, dp(9))
        })

        transcript = TextView(this).apply {
            text = if (BuildConfig.ACCESSIBILITY_ASSISTANCE_ENABLED) {
                "You're all set. ✦\n\nTry “open WhatsApp”, “search weather in Lagos”, “open youtube.com”, “what time is it”, or “help”.\n\nCross-app assistance is included in this full-control build and requires explicit activation in Android Accessibility settings."
            } else {
                "You're all set. ✦\n\nTry “open WhatsApp”, “open youtube.com”, “what time is it”, or “help”.\n\nThis safer sideload build can open apps and websites, handle voice input, and use your configured AI."
            }
            textSize = 14f
            setTextColor(white)
            setLineSpacing(dp(5).toFloat(), 1.0f)
            setPadding(dp(16), dp(17), dp(16), dp(17))
            this.background = rounded(panel, 18, stroke)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(transcript, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { bottomMargin = dp(14) })

        commandInput = EditText(this).apply {
            hint = "Message your assistant…"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 15f
            minLines = 1
            maxLines = 4
            setPadding(dp(15), dp(12), dp(15), dp(12))
            this.background = rounded(panelRaised, 17, stroke)
            setSingleLine(false)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEND
        }
        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        composer.addView(commandInput, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val sendButton = Button(this).apply {
            text = "↑"
            textSize = 25f
            isAllCaps = false
            setTextColor(background)
            this.background = rounded(accent, 17)
            contentDescription = "Send command"
            setPadding(0, 0, 0, dp(3))
            setOnClickListener { handleCommand(commandInput.text.toString().trim()) }
        }
        composer.addView(sendButton, LinearLayout.LayoutParams(dp(54), dp(54)).apply {
            leftMargin = dp(9)
        })
        root.addView(composer)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        fun actionButton(title: String, tint: Int, onClick: () -> Unit) = Button(this).apply {
            text = title
            textSize = 12f
            isAllCaps = false
            setTextColor(tint)
            this.background = rounded(panelRaised, 15, stroke)
            setPadding(dp(8), 0, dp(8), 0)
            setOnClickListener { onClick() }
        }
        actions.addView(actionButton("◉  Speak", accent) { startVoiceInput() },
            LinearLayout.LayoutParams(0, dp(46), 1f).apply { rightMargin = dp(6) })
        actions.addView(actionButton("⌕  Search settings", purple) { configureSearch() },
            LinearLayout.LayoutParams(0, dp(46), 1f).apply { rightMargin = dp(6) })
        actions.addView(actionButton("⚙  Configure AI", purple) { configureAi() },
            LinearLayout.LayoutParams(0, dp(46), 1f).apply { leftMargin = dp(6) })
        root.addView(actions, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(9) })

        if (BuildConfig.ACCESSIBILITY_ASSISTANCE_ENABLED) {
            root.addView(actionButton("Set up app assistance", muted) {
                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44)
            ).apply { topMargin = dp(8) })
        }

        root.addView(label("PRIVATE BY DESIGN  •  YOUR KEY STAYS ON THIS DEVICE", 9f, muted).apply {
            gravity = android.view.Gravity.CENTER
            letterSpacing = 0.04f
            setPadding(0, dp(12), 0, 0)
        })
        setContentView(root)
    }

    private fun handleCommand(raw: String) {
        val command = AssistantCommandRouter.parse(raw)
        if (command.action == "empty") {
            Toast.makeText(this, "Type or say a command first.", Toast.LENGTH_SHORT).show()
            return
        }
        val accessibilityUnavailable = "Cross-app assistance isn't included in this safer sideload build."
        val response = when (command.action) {
            "help" -> if (BuildConfig.ACCESSIBILITY_ASSISTANCE_ENABLED)
                "Try searching the web, opening apps or websites, time, date, battery, back, home, reading the screen, tapping visible labels, or typing into a focused field."
            else
                "Try searching the web, opening apps or websites, time, date, battery, voice input, or ask the configured AI."
            "open_app" -> openApp(command.argument)
            "open_url" -> openUrl(command.argument)
            "search" -> {
                if (command.argument.isBlank()) "What would you like me to search for?"
                else {
                    SearchPreferences.search(this, command.argument) { query ->
                        SearchPreferences.showSearchChoice(this, query, rememberChoice = false)
                    }
                    "Searching for: ${command.argument}"
                }
            }
            "settings" -> launchSettings(false)
            "accessibility_settings" -> if (BuildConfig.ACCESSIBILITY_ASSISTANCE_ENABLED) launchSettings(true) else accessibilityUnavailable
            "back" -> if (!BuildConfig.ACCESSIBILITY_ASSISTANCE_ENABLED) accessibilityUnavailable else if (AssistantAccessibilityService.activeService?.goBack() == true) "Back action sent." else "Accessibility assistance is not active. Enable My Android AI in Accessibility settings first."
            "home" -> if (!BuildConfig.ACCESSIBILITY_ASSISTANCE_ENABLED) accessibilityUnavailable else if (AssistantAccessibilityService.activeService?.goHome() == true) "Home action sent." else "Accessibility assistance is not active. Enable it in Accessibility settings first."
            "tap_text" -> if (!BuildConfig.ACCESSIBILITY_ASSISTANCE_ENABLED) accessibilityUnavailable else if (AssistantAccessibilityService.activeService?.clickVisibleText(command.argument) == true) "Tapped “${command.argument}”." else "I couldn't tap that label. Make sure the target app is visible and assistance is enabled."
            "type_text" -> if (!BuildConfig.ACCESSIBILITY_ASSISTANCE_ENABLED) accessibilityUnavailable else if (AssistantAccessibilityService.activeService?.setFocusedText(command.argument) == true) "Typed the requested text into the focused field." else "I couldn't type into a focused field. Select the destination text field first."
            "read_screen" -> if (!BuildConfig.ACCESSIBILITY_ASSISTANCE_ENABLED) accessibilityUnavailable else {
                val lines = AssistantAccessibilityService.activeService?.visibleTextSnapshot().orEmpty()
                if (lines.isEmpty()) "No visible text available. Make sure the target app is in the foreground and accessibility assistance is enabled."
                else "Visible screen text:\n" + lines.take(35).joinToString("\n")
            }
            "time" -> "The time is " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())
            "date" -> "Today is " + DateFormat.getDateInstance(DateFormat.FULL).format(Date())
            "battery" -> batteryStatus()
            "stop" -> "Okay. You can close this screen whenever you're ready."
            else -> {
                if (!AiProviderClient.hasKey(this)) {
                    "No API key configured for ${AiProviderClient.activeProvider(this).label}. Tap Configure AI, choose a provider, and save its API key."
                } else {
                    askAi(command.argument)
                    "Thinking… I'll reply here when the AI responds."
                }
            }
        }
        transcript.text = "You: $raw\n\nAssistant: $response"
        textToSpeech?.speak(response, TextToSpeech.QUEUE_FLUSH, null, "assistant-response")
        commandInput.text.clear()
    }


    private fun configureSearch() {
        val options = SearchPreferences.options
        val current = SearchPreferences.selectedEngine(this)
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                options.map { it.label }
            )
            setSelection(options.indexOfFirst { it.id == current }.coerceAtLeast(0))
        }
        AlertDialog.Builder(this)
            .setTitle("Search preferences")
            .setMessage("Choose where commands like “search weather in Lagos” should go. Browser options use your Android default browser; Google app uses the Google app when available.")
            .setView(spinner)
            .setPositiveButton("Save") { _, _ ->
                val option = options[spinner.selectedItemPosition.coerceIn(0, options.lastIndex)]
                SearchPreferences.saveEngine(this, option.id)
                Toast.makeText(this, "Search preference saved: ${option.label}", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun configureAi() {
        val providers = AiProviderClient.providers
        val activeId = AiProviderClient.activeProviderId(this)
        val providerSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                providers.map { it.label }
            )
            setSelection(providers.indexOfFirst { it.id == activeId }.coerceAtLeast(0))
        }
        val keyInput = EditText(this).apply {
            hint = "Paste API key for selected provider"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }
        val status = TextView(this).apply {
            val provider = providers.first { it.id == activeId }
            text = if (SecureKeyStore.getProviderKey(this@MainActivity, provider.id).isNullOrBlank()) {
                "No key saved for ${provider.label}. Keys are stored separately for each provider."
            } else {
                "A key is saved for ${provider.label}. Enter a new key only if you want to replace it."
            }
            textSize = 13f
            setPadding(0, dp(8), 0, dp(8))
        }
        providerSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val provider = providers[position]
                status.text = if (SecureKeyStore.getProviderKey(this@MainActivity, provider.id).isNullOrBlank()) {
                    "No key saved for ${provider.label}. Keys are stored separately for each provider."
                } else {
                    "A key is saved for ${provider.label}. Enter a new key only if you want to replace it."
                }
            }
        }
        val dialogContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), 0)
            addView(TextView(this@MainActivity).apply {
                text = "AI provider"
                textSize = 14f
            })
            addView(providerSpinner, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            addView(status, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            addView(keyInput, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        AlertDialog.Builder(this)
            .setTitle("Configure AI")
            .setMessage("Choose the provider that matches your API key. A Groq key will not work with Gemini, and a Gemini key will not work with Groq. Keys are encrypted with Android Keystore and stored on this device.")
            .setView(dialogContent)
            .setPositiveButton("Save / use provider") { _, _ ->
                val provider = providers[providerSpinner.selectedItemPosition.coerceIn(0, providers.lastIndex)]
                val key = keyInput.text.toString().trim()
                try {
                    if (key.isNotBlank()) SecureKeyStore.saveProviderKey(this, provider.id, key)
                    AiProviderClient.setActiveProvider(this, provider.id)
                    if (SecureKeyStore.getProviderKey(this, provider.id).isNullOrBlank()) {
                        Toast.makeText(this, "Selected ${provider.label}. Add its API key before sending AI requests.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "${provider.label} is selected. Its API key is stored securely on this device.", Toast.LENGTH_LONG).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(this, "Couldn't securely save or select this provider.", Toast.LENGTH_LONG).show()
                }
            }
            .setNeutralButton("Remove selected key") { _, _ ->
                val provider = providers[providerSpinner.selectedItemPosition.coerceIn(0, providers.lastIndex)]
                SecureKeyStore.clearProviderKey(this, provider.id)
                Toast.makeText(this, "Saved key for ${provider.label} removed.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun askAi(prompt: String) {
        if (!AiProviderClient.hasKey(this)) {
            runOnUiThread {
                transcript.text = "Assistant: No API key is configured for ${AiProviderClient.activeProvider(this).label}. Tap Configure AI and save a key for that provider."
            }
            return
        }
        AiProviderClient.ask(this, prompt) { result ->
            runOnUiThread {
                val answer = result.getOrElse { "AI request failed: ${it.message ?: "unknown error"}" }
                transcript.text = "You: $prompt\n\nAssistant: $answer"
                textToSpeech?.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "assistant-ai-response")
            }
        }
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

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        super.onDestroy()
    }

    private fun fullWidthButton() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(7) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
