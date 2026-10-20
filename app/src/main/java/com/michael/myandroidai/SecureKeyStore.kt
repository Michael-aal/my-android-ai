package com.michael.myandroidai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts each provider key with an AES key held by Android Keystore. */
object SecureKeyStore {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "my_android_ai_provider_key_v1"
    private const val PREFS = "secure_ai_settings"

    fun saveProviderKey(context: Context, provider: String, apiKey: String) {
        require(apiKey.isNotBlank()) { "API key cannot be empty." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(apiKey.trim().toByteArray(Charsets.UTF_8))
        val packed = cipher.iv + encrypted
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(storageName(provider), Base64.encodeToString(packed, Base64.NO_WRAP))
            .apply()
    }

    fun getProviderKey(context: Context, provider: String): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Migrate the original Gemini-only key without exposing it or requiring the user to re-enter it.
        val storageName = storageName(provider)
        val encoded = prefs.getString(storageName, null)
            ?: (if (provider.equals("gemini", true)) {
                prefs.getString("encrypted_gemini_key", null)
            } else {
                null
            })
            ?: return null
        return try {
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            if (packed.size <= 12) return null
            val iv = packed.copyOfRange(0, 12)
            val ciphertext = packed.copyOfRange(12, packed.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun clearProviderKey(context: Context, provider: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(storageName(provider))
            .apply()
        if (provider.equals("gemini", true)) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove("encrypted_gemini_key").apply()
        }
    }

    // Backward-compatible helpers for any existing code paths.
    fun saveGeminiKey(context: Context, apiKey: String) = saveProviderKey(context, "gemini", apiKey)
    fun getGeminiKey(context: Context): String? = getProviderKey(context, "gemini")
    fun clearGeminiKey(context: Context) = clearProviderKey(context, "gemini")

    private fun storageName(provider: String) =
        "encrypted_${provider.lowercase().filter { it.isLetterOrDigit() }}_key"

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
