package com.michael.myandroidai

import android.content.Context

/** Preferences for the user's floating robot companion. */
object RobotPreferences {
    private const val FILE = "robot_companion"
    private const val KEY_NAME = "robot_name"
    private const val KEY_AUTO_MOVE = "auto_move"

    fun name(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_NAME, "AmyK")?.trim()?.takeIf { it.isNotEmpty() } ?: "AmyK"

    fun setName(context: Context, name: String) {
        val clean = name.trim().take(32)
        if (clean.isNotEmpty()) {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit().putString(KEY_NAME, clean).apply()
        }
    }

    fun autoMove(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_AUTO_MOVE, false)

    fun setAutoMove(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_MOVE, enabled).apply()
    }
}
