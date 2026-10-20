package com.michael.myandroidai

import java.util.Locale

data class AssistantCommand(val action: String, val argument: String = "")

/**
 * Android-native port of the original my_ai intent_engine.py.
 * Commands are deliberately explicit; unknown requests are not executed.
 */
object AssistantCommandRouter {
    fun parse(raw: String): AssistantCommand {
        val text = raw.trim()
        val normalized = text.lowercase(Locale.ROOT)
        if (normalized.isBlank()) return AssistantCommand("empty")

        return when {
            normalized.startsWith("parallel:") -> AssistantCommand("parallel_tasks", text.substringAfter(":", "").trim())
            normalized.startsWith("do these at once:") -> AssistantCommand("parallel_tasks", text.substringAfter("do these at once:", "").trim())
            normalized in setOf("help", "what can you do", "commands") -> AssistantCommand("help")
            normalized in setOf("back", "go back", "press back") -> AssistantCommand("back")
            normalized in setOf("home", "go home", "press home") -> AssistantCommand("home")
            normalized in setOf("open settings", "settings") -> AssistantCommand("settings")
            normalized in setOf("open accessibility settings", "set up app assistance") -> AssistantCommand("accessibility_settings")
            normalized in setOf("what time is it", "time", "tell me the time", "current time") -> AssistantCommand("time")
            normalized in setOf("what is the date", "today's date", "today date", "date") -> AssistantCommand("date")
            normalized in setOf("battery", "check battery", "battery level") -> AssistantCommand("battery")
            normalized in setOf("stay", "stay there", "stop moving", "don't move", "do not move") -> AssistantCommand("robot_stay")
            normalized in setOf("move around", "start moving", "roam around", "move freely") -> AssistantCommand("robot_move")
            normalized in setOf("robot settings", "assistant settings", "change your name") -> AssistantCommand("robot_settings")
            normalized in setOf("what is your name", "what's your name", "who are you") -> AssistantCommand("robot_name")
            normalized == "search" || normalized == "search this" -> AssistantCommand("search", "")
            normalized.startsWith("search the web for ") -> AssistantCommand("search", text.substringAfter("search the web for ", "").trim())
            normalized.startsWith("search for ") -> AssistantCommand("search", text.substringAfter("search for ", "").trim())
            normalized.startsWith("search ") -> AssistantCommand("search", text.substringAfter("search ", "").trim())
            normalized.startsWith("look up ") -> AssistantCommand("search", text.substringAfter("look up ", "").trim())
            normalized.startsWith("google ") -> AssistantCommand("search", text.substringAfter("google ", "").trim())
            normalized.startsWith("open ") -> {
                val target = text.substringAfter(" ", "").trim()
                if (target.startsWith("http://", true) || target.startsWith("https://", true) ||
                    (target.contains(".") && !target.contains(" "))) {
                    AssistantCommand("open_url", target)
                } else AssistantCommand("open_app", target)
            }
            normalized.startsWith("go to ") -> AssistantCommand("open_url", text.substringAfter("go to ").trim())
            normalized.startsWith("visit ") -> AssistantCommand("open_url", text.substringAfter("visit ").trim())
            normalized.startsWith("tap ") -> AssistantCommand("tap_text", text.substringAfter("tap ").trim())
            normalized.startsWith("click ") -> AssistantCommand("tap_text", text.substringAfter("click ").trim())
            normalized.startsWith("type ") -> AssistantCommand("type_text", text.substringAfter("type ").trim())
            normalized == "read screen" || normalized == "what is on my screen" -> AssistantCommand("read_screen")
            normalized in setOf("stop listening", "go offline", "dismiss") -> AssistantCommand("stop")
            else -> AssistantCommand("unknown", text)
        }
    }

    /** Split only on an explicit delimiter; commas in natural language are not task boundaries. */
    fun splitParallelTasks(raw: String, maxTasks: Int = 3): List<String> =
        raw.split("||").map { it.trim() }.filter { it.isNotBlank() }.distinct().take(maxTasks.coerceAtLeast(1))
}
