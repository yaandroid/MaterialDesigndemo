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

    /** "system" | "light" | "dark" — default light to match the brand design. */
    var themeMode: String
        get() = prefs.getString(KEY_THEME, "light") ?: "light"
        set(v) = prefs.edit().putString(KEY_THEME, v).apply()

    // ---- Missed-call auto-reply ----
    var callReplyEnabled: Boolean
        get() = prefs.getBoolean(KEY_CALL_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_CALL_ENABLED, v).apply()

    var callSmsEnabled: Boolean
        get() = prefs.getBoolean(KEY_CALL_SMS, true)
        set(v) = prefs.edit().putBoolean(KEY_CALL_SMS, v).apply()

    var callSmsText: String
        get() = prefs.getString(KEY_CALL_SMS_TEXT, DEFAULT_MISSED_SMS) ?: DEFAULT_MISSED_SMS
        set(v) = prefs.edit().putString(KEY_CALL_SMS_TEXT, v).apply()

    /** -1 = system default SIM; otherwise a SubscriptionInfo subscriptionId. */
    var callSimSubId: Int
        get() = prefs.getInt(KEY_CALL_SIM, -1)
        set(v) = prefs.edit().putInt(KEY_CALL_SIM, v).apply()

    var callWhatsappEnabled: Boolean
        get() = prefs.getBoolean(KEY_CALL_WA, true)
        set(v) = prefs.edit().putBoolean(KEY_CALL_WA, v).apply()

    /** "wa" = normal WhatsApp, "wab" = WhatsApp Business */
    var callWhatsappTarget: String
        get() = prefs.getString(KEY_CALL_WA_TARGET, "wa") ?: "wa"
        set(v) = prefs.edit().putString(KEY_CALL_WA_TARGET, v).apply()

    var callWhatsappText: String
        get() = prefs.getString(KEY_CALL_WA_TEXT, DEFAULT_MISSED_WA) ?: DEFAULT_MISSED_WA
        set(v) = prefs.edit().putString(KEY_CALL_WA_TEXT, v).apply()

    /** Comma/newline separated numbers that must NOT get an auto reply. */
    var callExcludeNumbers: String
        get() = prefs.getString(KEY_CALL_EXCLUDE, "") ?: ""
        set(v) = prefs.edit().putString(KEY_CALL_EXCLUDE, v).apply()

    /** Reply from whichever SIM the call came on (overrides callSimSubId when detectable). */
    var callReplyFromSameSim: Boolean
        get() = prefs.getBoolean(KEY_CALL_SAME_SIM, true)
        set(v) = prefs.edit().putBoolean(KEY_CALL_SAME_SIM, v).apply()

    var callOnMissed: Boolean
        get() = prefs.getBoolean(KEY_CALL_ON_MISSED, true)
        set(v) = prefs.edit().putBoolean(KEY_CALL_ON_MISSED, v).apply()

    var callOnIncoming: Boolean
        get() = prefs.getBoolean(KEY_CALL_ON_INCOMING, false)
        set(v) = prefs.edit().putBoolean(KEY_CALL_ON_INCOMING, v).apply()

    var callOnOutgoing: Boolean
        get() = prefs.getBoolean(KEY_CALL_ON_OUTGOING, false)
        set(v) = prefs.edit().putBoolean(KEY_CALL_ON_OUTGOING, v).apply()

    /** Delay in seconds between each SMS in a bulk send (0 = no delay). */
    var bulkDelaySeconds: Int
        get() = prefs.getInt(KEY_BULK_DELAY, 0)
        set(v) = prefs.edit().putInt(KEY_BULK_DELAY, v).apply()

    fun isExcluded(number: String): Boolean {
        val digits = number.filter { it.isDigit() }
        if (digits.isEmpty()) return true
        val last10 = digits.takeLast(10)
        return callExcludeNumbers.split(',', '\n', ' ', ';')
            .map { it.filter { c -> c.isDigit() } }
            .filter { it.isNotEmpty() }
            .any { it.takeLast(10) == last10 }
    }

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
        private const val KEY_THEME = "theme_mode"
        private const val KEY_CALL_ENABLED = "call_reply_enabled"
        private const val KEY_CALL_SMS = "call_sms_enabled"
        private const val KEY_CALL_SMS_TEXT = "call_sms_text"
        private const val KEY_CALL_SIM = "call_sim_subid"
        private const val KEY_CALL_WA = "call_wa_enabled"
        private const val KEY_CALL_WA_TARGET = "call_wa_target"
        private const val KEY_CALL_WA_TEXT = "call_wa_text"
        private const val KEY_CALL_EXCLUDE = "call_exclude"
        private const val KEY_CALL_SAME_SIM = "call_same_sim"
        private const val KEY_CALL_ON_MISSED = "call_on_missed"
        private const val KEY_CALL_ON_INCOMING = "call_on_incoming"
        private const val KEY_CALL_ON_OUTGOING = "call_on_outgoing"
        private const val KEY_BULK_DELAY = "bulk_delay_seconds"
        const val DEFAULT_MODEL = "llama-3.3-70b-versatile"
        const val DEFAULT_MISSED_SMS =
            "Namaste, abhi hum aapki call nahi utha paaye. Kripya batayein hum kaise madad karein. — BRAHM InnerBrain"
        const val DEFAULT_MISSED_WA =
            "Namaste 🙏 abhi call miss ho gayi. Batayein hum kaise madad karein? — BRAHM InnerBrain"

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
