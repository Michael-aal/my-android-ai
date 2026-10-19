package com.michael.myandroidai

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * User-enabled UI assistance. Nothing runs until the device owner manually
 * enables this service in Android Settings and issues a command in the app.
 * Screen content is inspected only on demand and is never uploaded or stored.
 */
class AssistantAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile
        var activeService: AssistantAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeService = this
    }

    override fun onDestroy() {
        if (activeService === this) activeService = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally do not collect events, messages, passwords, or screen text in background.
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
}
