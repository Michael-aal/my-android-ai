package com.michael.myandroidai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Small provider adapter for Gemini and OpenAI-compatible APIs (Groq and OpenRouter).
 * API keys are encrypted at rest by SecureKeyStore and sent only over HTTPS.
 */
object AiProviderClient {
    data class Provider(
        val id: String,
        val label: String,
        val endpoint: String,
        val model: String,
        val apiStyle: ApiStyle
    )

    enum class ApiStyle { GEMINI, OPENAI_COMPATIBLE }

    val providers = listOf(
        Provider(
            id = "gemini",
            label = "Google Gemini",
            endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent",
            model = "gemini-2.0-flash",
            apiStyle = ApiStyle.GEMINI
        ),
        Provider(
            id = "groq",
            label = "Groq",
            endpoint = "https://api.groq.com/openai/v1/chat/completions",
            model = "openai/gpt-oss-120b",
            apiStyle = ApiStyle.OPENAI_COMPATIBLE
        ),
        Provider(
            id = "openrouter",
            label = "OpenRouter",
            endpoint = "https://openrouter.ai/api/v1/chat/completions",
            model = "openrouter/auto",
            apiStyle = ApiStyle.OPENAI_COMPATIBLE
        )
    )

    private const val PREFS = "ai_provider_settings"
    private const val ACTIVE_PROVIDER_KEY = "active_provider"
    private val executor = Executors.newSingleThreadExecutor()

    fun activeProviderId(context: Context): String {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ACTIVE_PROVIDER_KEY, "gemini")
        return providers.firstOrNull { it.id == saved }?.id ?: "gemini"
    }

    fun activeProvider(context: Context): Provider =
        providers.first { it.id == activeProviderId(context) }

    fun setActiveProvider(context: Context, providerId: String) {
        require(providers.any { it.id == providerId }) { "Unsupported AI provider." }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(ACTIVE_PROVIDER_KEY, providerId)
            .apply()
    }

    fun hasKey(context: Context): Boolean =
        !SecureKeyStore.getProviderKey(context, activeProviderId(context)).isNullOrBlank()

    fun ask(context: Context, prompt: String, callback: (Result<String>) -> Unit) {
        val provider = activeProvider(context)
        val apiKey = SecureKeyStore.getProviderKey(context, provider.id)
        if (apiKey.isNullOrBlank()) {
            callback(Result.failure(IllegalStateException(
                "No API key is saved for ${provider.label}. Open Configure AI, select this provider, and save its key."
            )))
            return
        }

        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(provider.endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    if (provider.apiStyle == ApiStyle.GEMINI) {
                        setRequestProperty("x-goog-api-key", apiKey)
                    } else {
                        setRequestProperty("Authorization", "Bearer $apiKey")
                    }
                }

                val body = when (provider.apiStyle) {
                    ApiStyle.GEMINI -> JSONObject()
                        .put("contents", JSONArray().put(
                            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                        ))
                        .put("generationConfig", JSONObject().put("temperature", 0.4).put("maxOutputTokens", 700))
                    ApiStyle.OPENAI_COMPATIBLE -> JSONObject()
                        .put("model", provider.model)
                        .put("messages", JSONArray().put(
                            JSONObject().put("role", "user").put("content", prompt)
                        ))
                        .put("temperature", 0.4)
                        .put("max_tokens", 700)
                }.toString()

                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val responseText = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (status !in 200..299) {
                    throw IllegalStateException(friendlyError(provider, status, responseText))
                }

                val json = JSONObject(responseText)
                val answer = when (provider.apiStyle) {
                    ApiStyle.GEMINI -> json.optJSONArray("candidates")
                        ?.optJSONObject(0)?.optJSONObject("content")
                        ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                    ApiStyle.OPENAI_COMPATIBLE -> json.optJSONArray("choices")
                        ?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                }?.trim().orEmpty()

                if (answer.isBlank()) {
                    throw IllegalStateException("${provider.label} returned an empty response. Try again or check the provider's model availability.")
                }
                callback(Result.success(answer))
            } catch (error: Exception) {
                callback(Result.failure(error))
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun friendlyError(provider: Provider, status: Int, responseText: String): String {
        val detail = try {
            val json = JSONObject(responseText)
            json.optJSONObject("error")?.optString("message")
                ?.takeIf { it.isNotBlank() && it != "null" }
        } catch (_: Exception) { null }

        return when (status) {
            401, 403 -> "Authentication failed for ${provider.label} (HTTP $status). Check that the API key belongs to ${provider.label}, is copied completely, and is active. The key was not changed."
            400 -> "${provider.label} rejected the request (HTTP 400). ${detail ?: "Check the provider model and request settings."}"
            404 -> "${provider.label} endpoint or model was not found (HTTP 404). ${detail ?: "Check the configured model ID and API endpoint."}"
            429 -> "${provider.label} rate limit or quota reached (HTTP 429). Wait and try again, or check your provider quota."
            in 500..599 -> "${provider.label} is temporarily unavailable (HTTP $status). Try again shortly."
            else -> "${provider.label} request failed (HTTP $status). ${detail ?: "Please try again."}"
        }
    }
}
