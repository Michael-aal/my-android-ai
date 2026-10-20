package com.michael.myandroidai

import android.content.Context

/**
 * Runs a small batch of independent AI tasks concurrently.
 *
 * Only explicit, user-submitted prompts are sent. This runner does not dispatch
 * Android screen actions; those remain sequential to avoid UI/action conflicts.
 */
object ParallelTaskRunner {
    const val MAX_CONCURRENT_TASKS = 3

    fun run(
        context: Context,
        requestedTasks: List<String>,
        onUpdate: (message: String, finished: Boolean) -> Unit
    ) {
        val tasks = requestedTasks.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_CONCURRENT_TASKS)
        if (tasks.isEmpty()) {
            onUpdate("Add tasks separated by ||. Example: parallel: explain promises || plan revision", true)
            return
        }
        if (!AiProviderClient.hasKey(context)) {
            onUpdate("No API key configured. Open Configure AI and save a key before running parallel tasks.", true)
            return
        }

        val results = MutableList<String?>(tasks.size) { null }
        onUpdate("Started ${tasks.size} independent tasks (maximum ${MAX_CONCURRENT_TASKS} at once)…", false)
        tasks.forEachIndexed { index, task ->
            val prompt = "Complete this task independently and clearly. Task: $task"
            AiProviderClient.ask(context, prompt) { result ->
                val snapshot: String
                val finished: Boolean
                synchronized(results) {
                    val answer = result.fold(
                        onSuccess = { it },
                        onFailure = { "Task failed: ${it.message ?: "unknown error"}" }
                    )
                    results[index] = answer
                    val completed = results.count { it != null }
                    finished = completed == tasks.size
                    snapshot = buildString {
                        append("Parallel tasks: $completed/${tasks.size} complete\n\n")
                        tasks.forEachIndexed { taskIndex, taskText ->
                            append("${taskIndex + 1}. $taskText\n")
                            val value = results[taskIndex]
                            append(
                                when {
                                    value == null -> "   Waiting…"
                                    value.length > 360 -> "   " + value.take(357).trimEnd() + "…"
                                    else -> "   $value"
                                }
                            )
                            append("\n\n")
                        }
                        if (tasks.size < requestedTasks.count { it.isNotBlank() }) {
                            append("Only the first ${tasks.size} tasks were started to keep requests bounded.")
                        } else if (finished) {
                            append("All tasks finished.")
                        }
                    }
                }
                onUpdate(snapshot, finished)
            }
        }
    }
}
