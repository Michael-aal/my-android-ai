package com.michael.myandroidai

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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
    private var commandField: EditText? = null
    private var responseLabel: TextView? = null
    private var expanded = true

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
        activeService = this
        showAssistantOverlay()
    }

    override fun onDestroy() {
        if (activeService === this) activeService = null
        removeOverlay()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No background screen monitoring, event logging, or message collection.
    }

    override fun onInterrupt() = Unit

    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)

    fun visibleTextSnapshot(): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val result = mutableListOf<String>()
        collectVisibleText(root, result)
        return result
    }

    fun clickVisibleText(label: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return findAndAct(root, label.trim().lowercase(Locale.ROOT), AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun setFocusedText(value: String): Boolean {
        val root = rootInActiveWindow ?: return false
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
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.rgb(18, 28, 24))
        }
        overlay = panel
        renderExpandedPanel()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = dp(24)
        }
        try {
            windowManager?.addView(panel, params)
        } catch (_: Exception) {
            overlay = null
        }
    }

    private fun renderExpandedPanel() {
        val panel = overlay ?: return
        expanded = true
        panel.removeAllViews()
        panel.addView(TextView(this).apply {
            text = "MY ANDROID AI   •   Type a command"
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
                val reply = runOverlayCommand(raw)
                responseLabel?.text = reply
            }
        }, LinearLayout.LayoutParams(0, dp(44), 1f))
        actions.addView(Button(this).apply {
            text = "Minimize"
            setOnClickListener { renderCollapsedPanel() }
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
        panel.addView(Button(this).apply {
            text = "Open My Android AI"
            setOnClickListener { renderExpandedPanel() }
        })
        updateOverlayFlags(focusable = false)
    }

    private fun updateOverlayFlags(focusable: Boolean) {
        val panel = overlay ?: return
        val params = panel.layoutParams as? WindowManager.LayoutParams ?: return
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            (if (focusable) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        try { windowManager?.updateViewLayout(panel, params) } catch (_: Exception) { }
    }

    private fun runOverlayCommand(raw: String): String {
        val cmd = AssistantCommandRouter.parse(raw)
        if (cmd.action == "empty") return "Type a command first."
        return when (cmd.action) {
            "help" -> "Try open an app/site, tap a visible label, type text, back, home, read screen, time, date or battery."
            "open_app" -> launchApp(cmd.argument)
            "open_url" -> openUrl(cmd.argument)
            "back" -> if (goBack()) "Back action sent." else "Back action failed."
            "home" -> if (goHome()) "Home action sent." else "Home action failed."
            "settings" -> openSettings(false)
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
            else -> "I received “${cmd.argument}”. AI reasoning is not connected yet."
        }
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
        commandField = null
        responseLabel = null
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
