package com.brahm.crm.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * One central place for all app settings:
 *  - Groq API key (stored encrypted)
 *  - feature on/off toggles
 * Falls back to plain SharedPreferences if the encrypted store can't init.
 */
class SettingsStore private constructor(private val prefs: SharedPreferences) {

    // ---- Groq ----
    var groqApiKey: String
        get() = prefs.getString(KEY_GROQ, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GROQ, value.trim()).apply()

    var groqModel: String
        get() = prefs.getString(KEY_GROQ_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_GROQ_MODEL, value).apply()

    // ---- Feature toggles ----
    var inboxEnabled: Boolean
        get() = prefs.getBoolean(KEY_INBOX, true)
        set(v) = prefs.edit().putBoolean(KEY_INBOX, v).apply()

    var aiEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI, true)
        set(v) = prefs.edit().putBoolean(KEY_AI, v).apply()

    var contactsEnabled: Boolean
        get() = prefs.getBoolean(KEY_CONTACTS, true)
        set(v) = prefs.edit().putBoolean(KEY_CONTACTS, v).apply()

    var defaultReplyTone: String
        get() = prefs.getString(KEY_TONE, "Friendly, professional") ?: "Friendly, professional"
        set(v) = prefs.edit().putString(KEY_TONE, v).apply()

    fun registerListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(l)

    fun unregisterListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(l)

    companion object {
        private const val FILE = "brahm_crm_settings"
        private const val KEY_GROQ = "groq_api_key"
        private const val KEY_GROQ_MODEL = "groq_model"
        private const val KEY_INBOX = "feature_inbox"
        private const val KEY_AI = "feature_ai"
        private const val KEY_CONTACTS = "feature_contacts"
        private const val KEY_TONE = "reply_tone"
        const val DEFAULT_MODEL = "llama-3.3-70b-versatile"

        @Volatile private var INSTANCE: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsStore(build(context.applicationContext)).also { INSTANCE = it }
            }

        private fun build(ctx: Context): SharedPreferences = try {
            val key = MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                ctx,
                FILE,
                key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (t: Throwable) {
            // Fallback so the app never crashes if the keystore is unavailable.
            ctx.getSharedPreferences(FILE + "_plain", Context.MODE_PRIVATE)
        }
    }
}
