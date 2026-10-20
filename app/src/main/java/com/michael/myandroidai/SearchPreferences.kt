package com.michael.myandroidai

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** User-configurable web search destination. Search text is passed to the selected app/site. */
object SearchPreferences {
    const val PREFS = "search_preferences"
    const val KEY_ENGINE = "engine"
    const val ASK_EACH_TIME = "ask"
    const val GOOGLE_APP = "google_app"
    const val GOOGLE_BROWSER = "google_browser"
    const val BING = "bing"
    const val DUCKDUCKGO = "duckduckgo"
    const val BRAVE = "brave"
    const val ECOSIA = "ecosia"

    data class Option(val id: String, val label: String)

    val options = listOf(
        Option(ASK_EACH_TIME, "Ask me each time"),
        Option(GOOGLE_APP, "Google app"),
        Option(GOOGLE_BROWSER, "Google in my default browser"),
        Option(BING, "Bing in my default browser"),
        Option(DUCKDUCKGO, "DuckDuckGo in my default browser"),
        Option(BRAVE, "Brave Search in my default browser"),
        Option(ECOSIA, "Ecosia in my default browser")
    )

    fun selectedEngine(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ENGINE, GOOGLE_BROWSER) ?: GOOGLE_BROWSER

    fun saveEngine(context: Context, engine: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ENGINE, engine).apply()
    }

    fun search(context: Context, query: String, onAsk: (String) -> Unit = {}) {
        val cleaned = query.trim()
        if (cleaned.isBlank()) {
            Toast.makeText(context, "What would you like me to search for?", Toast.LENGTH_SHORT).show()
            return
        }
        val engine = selectedEngine(context)
        if (engine == ASK_EACH_TIME) {
            onAsk(cleaned)
            return
        }
        val encoded = URLEncoder.encode(cleaned, StandardCharsets.UTF_8.name())
        val url = when (engine) {
            BING -> "https://www.bing.com/search?q=$encoded"
            DUCKDUCKGO -> "https://duckduckgo.com/?q=$encoded"
            BRAVE -> "https://search.brave.com/search?q=$encoded"
            ECOSIA -> "https://www.ecosia.org/search?q=$encoded"
            else -> "https://www.google.com/search?q=$encoded"
        }
        try {
            if (engine == GOOGLE_APP) {
                val googleSearch = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    setPackage("com.google.android.googlequicksearchbox")
                    putExtra(android.app.SearchManager.QUERY, cleaned)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(googleSearch)
                    return
                } catch (_: Exception) {
                    // The Google app may not be installed; use the default browser instead.
                }
            }
            val view = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (engine == ASK_EACH_TIME) {
                context.startActivity(Intent.createChooser(view, "Choose a browser to search with")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                context.startActivity(view)
            }
        } catch (_: Exception) {
            Toast.makeText(context, "Couldn't open a search app. Check that a browser is installed.", Toast.LENGTH_LONG).show()
        }
    }

    fun showSearchChoice(context: Context, query: String, rememberChoice: Boolean = false) {
        val labels = options.map { it.label }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle("Search with")
            .setItems(labels) { _, which ->
                val selected = options[which]
                if (rememberChoice) saveEngine(context, selected.id)
                if (selected.id == ASK_EACH_TIME) {
                    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded"))
                    try {
                        context.startActivity(Intent.createChooser(intent, "Choose a browser to search with")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: Exception) {
                        Toast.makeText(context, "Couldn't open a browser.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val old = selectedEngine(context)
                    saveEngine(context, selected.id)
                    search(context, query)
                    if (!rememberChoice) saveEngine(context, old)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
