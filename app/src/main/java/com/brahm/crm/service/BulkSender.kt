package com.brahm.crm.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

/**
 * Sends SMS to a list of numbers (the user's own leads). Used for both
 * send-now and scheduled broadcasts. WhatsApp is never auto-sent — that is
 * handled as an assisted, one-tap-per-contact queue in the UI.
 */
object BulkSender {

    data class SmsResult(val sent: Int, val failed: Int)

    fun sendSms(context: Context, phones: List<String>, text: String, subId: Int): SmsResult {
        val msgs = phones.associateWith { text }
        return sendSmsPersonalized(context, msgs, subId)
    }

    /**
     * Sends a (possibly personalised) message per phone. Returns counts.
     * Delay between messages is handled by the caller (coroutine), so this
     * stays synchronous and simple.
     */
    fun sendSmsPersonalized(context: Context, perPhone: Map<String, String>, subId: Int): SmsResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) return SmsResult(0, perPhone.size)

        var sent = 0
        var failed = 0
        val sms = resolveSmsManager(context, subId)
        perPhone.forEach { (rawNum, body) ->
            val number = rawNum.trim()
            if (number.isEmpty() || body.isBlank()) { failed++; return@forEach }
            try {
                val parts = sms.divideMessage(body)
                if (parts.size > 1) sms.sendMultipartTextMessage(number, null, parts, null, null)
                else sms.sendTextMessage(number, null, body, null, null)
                sent++
            } catch (t: Throwable) {
                failed++
            }
        }
        return SmsResult(sent, failed)
    }

    /** Sends one SMS (used by delayed sequential bulk send). */
    fun sendOne(context: Context, number: String, body: String, subId: Int): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED || number.isBlank() || body.isBlank()
        ) return false
        return try {
            val sms = resolveSmsManager(context, subId)
            val parts = sms.divideMessage(body)
            if (parts.size > 1) sms.sendMultipartTextMessage(number.trim(), null, parts, null, null)
            else sms.sendTextMessage(number.trim(), null, body, null, null)
            true
        } catch (t: Throwable) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveSmsManager(context: Context, subId: Int): SmsManager {
        if (subId >= 0) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                context.getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
            else SmsManager.getSmsManagerForSubscriptionId(subId)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.getSystemService(SmsManager::class.java)
        else SmsManager.getDefault()
    }
}
