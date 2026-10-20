package com.michael.myandroidai

import android.accessibilityservice.AccessibilityService
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.graphics.PixelFormat
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Opt-in, user-controlled app assistance.
 *
 * This service only starts after the device owner manually enables it in Android
 * Accessibility settings. Commands are initiated through the visible overlay.
 * It does not record accessibility events or upload/store screen contents.
 */
class AssistantAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile
        var activeService: AssistantAccessibilityService? = null
            private set
    }

    private var windowManager: WindowManager? = null
    private var overlay: LinearLayout? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var robotFaceView: RobotFaceView? = null
    private var commandField: EditText? = null
    private var responseLabel: TextView? = null
    private var expanded = true
    private var textToSpeech: TextToSpeech? = null
    private val movementHandler = Handler(Looper.getMainLooper())
    private var movementDirection = 1
    private var movementPhase = 0
    private val movementRunnable = object : Runnable {
        override fun run() {
            if (RobotPreferences.autoMove(this@AssistantAccessibilityService) && !expanded) {
                val params = overlayParams
                if (params != null) {
                    val maxX = (resources.displayMetrics.widthPixels - dp(60)).coerceAtLeast(0)
                    params.x += movementDirection * dp(2)
                    if (params.x >= maxX) { params.x = maxX; movementDirection = -1 }
                    if (params.x <= 0) { params.x = 0; movementDirection = 1 }
                    movementPhase = (movementPhase + 1) % 80
                    if (movementPhase % 8 == 0) {
                        params.y = (params.y + if (movementPhase < 40) dp(1) else -dp(1))
                            .coerceIn(dp(70), (resources.displayMetrics.heightPixels - dp(100)).coerceAtLeast(dp(70)))
                    }
                    updateOverlayLayout()
                }
            }
            movementHandler.postDelayed(this, 90L)
        }
    }

    private val appPackages = mapOf(
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
        "messages" to "com.google.android.apps.messaging",
        "play store" to "com.android.vending"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (!BuildConfig.ACCESSIBILITY_ASSISTANCE_ENABLED) {
            // The safer distribution must never expose cross-app controls.
            disableSelf()
            return
        }
        activeService = this
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) textToSpeech?.language = Locale.getDefault()
        }
        showAssistantOverlay()
        movementHandler.postDelayed(movementRunnable, 90L)
    }

    override fun onDestroy() {
        movementHandler.removeCallbacks(movementRunnable)
        if (activeService === this) activeService = null
        removeOverlay()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No background screen monitoring, event logging, or message collection.
    }

    override fun onInterrupt() = Unit

    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)

    private fun foregroundAppRoot(): AccessibilityNodeInfo? {
        // Prefer the real app window rather than this assistant's own accessibility overlay.
        val appWindow = windows.firstOrNull {
            it.isActive && it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY
        } ?: windows.firstOrNull { it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }
        return appWindow?.root ?: rootInActiveWindow
    }

    fun visibleTextSnapshot(): List<String> {
        val root = foregroundAppRoot() ?: return emptyList()
        val result = mutableListOf<String>()
        collectVisibleText(root, result)
        return result
    }

    fun clickVisibleText(label: String): Boolean {
        val root = foregroundAppRoot() ?: return false
        return findAndAct(root, label.trim().lowercase(Locale.ROOT), AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun setFocusedText(value: String): Boolean {
        val root = foregroundAppRoot() ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun showAssistantOverlay() {
        if (overlay != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(18, 28, 40))
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.rgb(83, 210, 157))
            }
        }
        overlay = panel
        val displayWidth = resources.displayMetrics.widthPixels
        overlayParams = WindowManager.LayoutParams(
            dp(60),
            dp(60),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = (displayWidth - dp(76)).coerceAtLeast(dp(8))
            y = dp(180)
        }
        renderCollapsedPanel()
        try {
            windowManager?.addView(panel, overlayParams)
        } catch (_: Exception) {
            overlay = null
            overlayParams = null
        }
    }

    private fun renderExpandedPanel() {
        val panel = overlay ?: return
        expanded = true
        panel.removeAllViews()
        panel.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.rgb(18, 28, 40))
            cornerRadius = dp(18).toFloat()
            setStroke(dp(1), Color.rgb(83, 210, 157))
        }
        overlayParams?.let { it.width = dp(320); it.height = WindowManager.LayoutParams.WRAP_CONTENT }
        updateOverlayLayout()
        panel.addView(TextView(this).apply {
            text = "${RobotPreferences.name(this)}   •   Ready to help"
            textSize = 14f
            setTextColor(Color.rgb(83, 210, 157))
        })
        commandField = EditText(this).apply {
            hint = "e.g. open WhatsApp, tap Chats, type hello"
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setSingleLine(true)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        panel.addView(commandField)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(Button(this).apply {
            text = "Run"
            setOnClickListener {
                val raw = commandField?.text?.toString().orEmpty().trim()
                runCommandFromOverlay(raw)
            }
        }, LinearLayout.LayoutParams(0, dp(44), 1f))
        actions.addView(Button(this).apply {
            text = "Speak"
            setOnClickListener {
                try {
                    startActivity(Intent(this@AssistantAccessibilityService, VoiceCommandActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) {
                    responseLabel?.text = "Couldn't start voice input. Try using the assistant app screen."
                }
            }
        }, LinearLayout.LayoutParams(0, dp(44), 1f))
        actions.addView(Button(this).apply {
            text = "Minimize"
            setOnClickListener { renderCollapsedPanel() }
        }, LinearLayout.LayoutParams(0, dp(44), 1f))
        actions.addView(Button(this).apply {
            text = "Robot"
            setOnClickListener { showRobotSettings() }
        }, LinearLayout.LayoutParams(0, dp(44), 1f))
        panel.addView(actions)
        responseLabel = TextView(this).apply {
            text = "Commands run only when you press Run."
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(0, dp(4), 0, 0)
        }
        panel.addView(responseLabel)
        updateOverlayFlags(focusable = true)
    }

    private fun renderCollapsedPanel() {
        val panel = overlay ?: return
        expanded = false
        panel.removeAllViews()
        panel.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.rgb(18, 28, 40))
            cornerRadius = dp(30).toFloat()
            setStroke(dp(2), Color.rgb(83, 210, 157))
        }
        robotFaceView = RobotFaceView(this).apply {
            setOnClickListener { renderExpandedPanel() }
            setOnTouchListener(object : android.view.View.OnTouchListener {
                private var downRawX = 0f
                private var downRawY = 0f
                private var startX = 0
                private var startY = 0
                private var moved = false
                override fun onTouch(view: android.view.View, event: android.view.MotionEvent): Boolean {
                    val params = overlayParams ?: return false
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            downRawX = event.rawX
                            downRawY = event.rawY
                            startX = params.x
                            startY = params.y
                            moved = false
                            return true
                        }
                        android.view.MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - downRawX).toInt()
                            val dy = (event.rawY - downRawY).toInt()
                            if (kotlin.math.abs(dx) > dp(4) || kotlin.math.abs(dy) > dp(4)) moved = true
                            if (moved) {
                                params.x = (startX + dx).coerceIn(0, (resources.displayMetrics.widthPixels - dp(60)).coerceAtLeast(0))
                                params.y = (startY + dy).coerceAtLeast(0)
                                updateOverlayLayout()
                            }
                            return true
                        }
                        android.view.MotionEvent.ACTION_UP -> {
                            if (!moved) renderExpandedPanel()
                            return true
                        }
                    }
                    return false
                }
            })
        }
        panel.addView(robotFaceView, LinearLayout.LayoutParams(dp(60), dp(60)))
        overlayParams?.let {
            it.width = dp(60)
            it.height = dp(60)
            it.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        updateOverlayLayout()
    }

    private fun updateOverlayFlags(focusable: Boolean) {
        val params = overlayParams ?: return
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            (if (focusable) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        updateOverlayLayout()
    }

    private fun updateOverlayLayout() {
        val panel = overlay ?: return
        val params = overlayParams ?: return
        try {
            if (panel.isAttachedToWindow) windowManager?.updateViewLayout(panel, params)
        } catch (_: Exception) { }
    }

    fun runCommandFromOverlay(raw: String): String {
        val reply = runOverlayCommand(raw)
        responseLabel?.text = reply
        textToSpeech?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "assistant-reply")
        return reply
    }

    private fun runOverlayCommand(raw: String): String {
        val cmd = AssistantCommandRouter.parse(raw)
        if (cmd.action == "empty") return "Type a command first."
        return when (cmd.action) {
            "help" -> "Try “search weather in Lagos”, open an app/site, tap a visible label, type text, back, home, read screen, time, date or battery."
            "open_app" -> launchApp(cmd.argument)
            "open_url" -> openUrl(cmd.argument)
            "search" -> {
                if (cmd.argument.isBlank()) "What would you like me to search for?"
                else {
                    SearchPreferences.search(this, cmd.argument) { query ->
                        val intent = Intent(this, MainActivity::class.java)
                            .setAction("com.michael.myandroidai.ACTION_SEARCH")
                            .putExtra("search_query", query)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(intent)
                    }
                    "Searching for: ${cmd.argument}"
                }
            }
            "back" -> if (goBack()) "Back action sent." else "Back action failed."
            "home" -> if (goHome()) "Home action sent." else "Home action failed."
            "settings" -> openSettings(false)
            "robot_settings" -> { showRobotSettings(); "Opening robot settings." }
            "robot_stay" -> {
                RobotPreferences.setAutoMove(this, false)
                "Okay, I'll stay right here."
            }
            "robot_move" -> {
                RobotPreferences.setAutoMove(this, true)
                "I'll move gently around the screen. Say stay when you want me to stop."
            }
            "robot_name" -> "My name is ${RobotPreferences.name(this)}."
            "accessibility_settings" -> openSettings(true)
            "tap_text" -> if (clickVisibleText(cmd.argument)) "Tapped “${cmd.argument}”." else "Couldn't find a clickable visible label: ${cmd.argument}"
            "type_text" -> if (setFocusedText(cmd.argument)) "Typed into the focused field." else "Couldn't find a focused text field. Tap the destination field first."
            "read_screen" -> visibleTextSnapshot().take(25).joinToString("\n").ifBlank { "No visible text found." }
            "time" -> "Time: " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())
            "date" -> "Date: " + DateFormat.getDateInstance(DateFormat.FULL).format(Date())
            "battery" -> {
                val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
                val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                if (level in 0..100) "Battery: $level%" else "Battery level unavailable."
            }
            "stop" -> { renderCollapsedPanel(); "Assistant minimized." }
            else -> {
                if (!AiProviderClient.hasKey(this)) {
                    "No AI key configured. Open My Android AI and tap Configure AI first."
                } else {
                    AiProviderClient.ask(this, cmd.argument) { result ->
                        val answer = result.getOrElse { "AI request failed: ${it.message ?: "unknown error"}" }
                        responseLabel?.post {
                            responseLabel?.text = answer
                            textToSpeech?.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "assistant-ai-reply")
                        }
                    }
                    "Thinking…"
                }
            }
        }
    }

    private fun showRobotSettings() {
        val nameField = EditText(this).apply {
            setSingleLine(true)
            hint = "Robot name"
            setText(RobotPreferences.name(this@AssistantAccessibilityService))
            setSelection(text.length)
        }
        val moveToggle = android.widget.CheckBox(this).apply {
            text = "Move gently around the screen"
            setTextColor(Color.WHITE)
            isChecked = RobotPreferences.autoMove(this@AssistantAccessibilityService)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
            addView(TextView(this@AssistantAccessibilityService).apply {
                text = "Choose a name and movement style for your floating robot."
                setTextColor(Color.WHITE)
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
                responseLabel?.text = "Saved robot settings for ${RobotPreferences.name(this)}."
                if (expanded) renderExpandedPanel()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchApp(rawName: String): String {
        val name = rawName.trim()
        val pkg = appPackages[name.lowercase(Locale.ROOT)]
        if (pkg != null) {
            val launch = packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launch)
                return "Opening $name."
            }
        }
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = packageManager.queryIntentActivities(query, 0).firstOrNull {
            it.loadLabel(packageManager).toString().equals(name, true)
        } ?: packageManager.queryIntentActivities(query, 0).firstOrNull {
            it.loadLabel(packageManager).toString().contains(name, true)
        }
        val launch = match?.let { packageManager.getLaunchIntentForPackage(it.activityInfo.packageName) }
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
            return "Opening ${match?.loadLabel(packageManager) ?: name}."
        }
        return "Couldn't find an installed app named “$name”."
    }

    private fun openUrl(raw: String): String {
        val url = if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) raw else "https://$raw"
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "Opening $url."
        } catch (_: Exception) { "Couldn't open $url." }
    }

    private fun openSettings(accessibility: Boolean): String {
        val action = if (accessibility) android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS else android.provider.Settings.ACTION_SETTINGS
        startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return if (accessibility) "Opening Accessibility settings." else "Opening Android Settings."
    }

    private fun collectVisibleText(node: AccessibilityNodeInfo, result: MutableList<String>) {
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let(result::add)
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(result::add)
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            collectVisibleText(child, result)
            child.recycle()
        }
    }

    private fun findAndAct(node: AccessibilityNodeInfo, target: String, action: Int): Boolean {
        val label = (node.text?.toString() ?: node.contentDescription?.toString())
            ?.trim()?.lowercase(Locale.ROOT)
        if (label != null && (label == target || label.contains(target))) {
            if (node.isClickable && node.performAction(action)) return true
            val parent = node.parent
            if (parent != null) {
                val succeeded = parent.isClickable && parent.performAction(action)
                parent.recycle()
                if (succeeded) return true
            }
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            val succeeded = findAndAct(child, target, action)
            child.recycle()
            if (succeeded) return true
        }
        return false
    }

    private fun removeOverlay() {
        val current = overlay ?: return
        try { windowManager?.removeView(current) } catch (_: Exception) { }
        overlay = null
        overlayParams = null
        robotFaceView = null
        commandField = null
        responseLabel = null
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
