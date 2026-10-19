package com.michael.myandroidai

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Opt-in foundation for user-requested app assistance.
 *
 * Android requires the device owner to enable this service manually in Settings.
 * Keep actions visible, user initiated, and limited to the command the user asked for.
 */
class AssistantAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Deliberately do not collect, upload, or persist screen contents.
        // Future command handling should inspect only the active UI for a specific
        // user-requested action and should avoid passwords, payment details, and private messages.
    }

    override fun onInterrupt() = Unit

    fun visibleTextSnapshot(): List<String> {
        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return emptyList()
        val result = mutableListOf<String>()
        collectVisibleText(root, result)
        return result
    }

    private fun collectVisibleText(node: AccessibilityNodeInfo, result: MutableList<String>) {
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let(result::add)
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
                collectVisibleText(child, result)
                child.recycle()
            }
        }
    }
}
