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
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED || text.isBlank()
        ) return SmsResult(0, phones.size)

        var sent = 0
        var failed = 0
        val sms = resolveSmsManager(context, subId)
        phones.map { it.trim() }.filter { it.isNotEmpty() }.forEach { number ->
            try {
                val parts = sms.divideMessage(text)
                if (parts.size > 1) sms.sendMultipartTextMessage(number, null, parts, null, null)
                else sms.sendTextMessage(number, null, text, null, null)
                sent++
            } catch (t: Throwable) {
                failed++
            }
        }
        return SmsResult(sent, failed)
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
