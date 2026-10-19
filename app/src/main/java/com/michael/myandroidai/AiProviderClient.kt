package com.michael.myandroidai

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Small Android-native Gemini adapter ported from the original ai_engine.py provider pattern.
 * Provider credentials stay encrypted on this device and are sent only over HTTPS.
 */
object AiProviderClient {
    private val executor = Executors.newSingleThreadExecutor()
    private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    fun hasKey(context: Context): Boolean = !SecureKeyStore.getGeminiKey(context).isNullOrBlank()

    fun ask(context: Context, prompt: String, callback: (Result<String>) -> Unit) {
        val apiKey = SecureKeyStore.getGeminiKey(context)
        if (apiKey.isNullOrBlank()) {
            callback(Result.failure(IllegalStateException("No Gemini API key configured. Use Configure AI first.")))
            return
        }
        executor.execute {
            try {
                val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 25000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("x-goog-api-key", apiKey)
                }
                val body = JSONObject()
                    .put("contents", org.json.JSONArray().put(
                        JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", prompt)))
                    ))
                    .put("generationConfig", JSONObject().put("temperature", 0.4).put("maxOutputTokens", 700))
                    .toString()
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                connection.disconnect()
                if (status !in 200..299) {
                    val message = try {
                        JSONObject(responseText).optJSONObject("error")?.optString("message")
                    } catch (_: Exception) { null }
                    throw IllegalStateException(message?.takeIf { it.isNotBlank() } ?: "AI request failed (HTTP $status).")
                }
                val json = JSONObject(responseText)
                val answer = json.optJSONArray("candidates")
                    ?.optJSONObject(0)?.optJSONObject("content")
                    ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                    ?.trim().orEmpty()
                if (answer.isBlank()) throw IllegalStateException("The AI provider returned an empty response.")
                callback(Result.success(answer))
            } catch (error: Exception) {
                callback(Result.failure(error))
            }
        }
    }
}
