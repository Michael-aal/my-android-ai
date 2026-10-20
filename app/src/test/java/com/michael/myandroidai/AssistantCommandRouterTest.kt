package com.michael.myandroidai

import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantCommandRouterTest {
    @Test
    fun parsesAppAndWebsiteCommands() {
        assertEquals("open_app", AssistantCommandRouter.parse("open WhatsApp").action)
        assertEquals("WhatsApp", AssistantCommandRouter.parse("open WhatsApp").argument)
        assertEquals("open_url", AssistantCommandRouter.parse("open youtube.com").action)
        assertEquals("https://example.com", AssistantCommandRouter.parse("open https://example.com").argument)
    }

    @Test
    fun parsesExplicitDeviceActions() {
        assertEquals("back", AssistantCommandRouter.parse("go back").action)
        assertEquals("home", AssistantCommandRouter.parse("go home").action)
        assertEquals("settings", AssistantCommandRouter.parse("open settings").action)
        assertEquals("battery", AssistantCommandRouter.parse("check battery").action)
    }

    @Test
    fun parsesAccessibilityCommandsWithoutExecutingThem() {
        assertEquals("tap_text", AssistantCommandRouter.parse("tap Send").action)
        assertEquals("Send", AssistantCommandRouter.parse("tap Send").argument)
        assertEquals("type_text", AssistantCommandRouter.parse("type hello there").action)
        assertEquals("read_screen", AssistantCommandRouter.parse("read screen").action)
    }

    @Test
    fun parsesRobotCompanionCommands() {
        assertEquals("robot_stay", AssistantCommandRouter.parse("stay").action)
        assertEquals("robot_stay", AssistantCommandRouter.parse("don't move").action)
        assertEquals("robot_move", AssistantCommandRouter.parse("move around").action)
        assertEquals("robot_settings", AssistantCommandRouter.parse("robot settings").action)
        assertEquals("robot_name", AssistantCommandRouter.parse("what is your name").action)
    }

    @Test
    fun parsesParallelTaskCommandsExplicitly() {
        val command = AssistantCommandRouter.parse("parallel: explain promises || summarize this error || plan revision")
        assertEquals("parallel_tasks", command.action)
        assertEquals(3, AssistantCommandRouter.splitParallelTasks(command.argument).size)
        assertEquals(listOf("one", "two"), AssistantCommandRouter.splitParallelTasks("one || two || three", maxTasks = 2))
        assertEquals(listOf("one"), AssistantCommandRouter.splitParallelTasks("one || one || "))
    }

    @Test
    fun handlesBlankAndUnknownCommandsSafely() {
        assertEquals("empty", AssistantCommandRouter.parse("   ").action)
        assertEquals("unknown", AssistantCommandRouter.parse("do something unexpected").action)
    }
}
