package com.brahm.crm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

/**
 * Detects a missed incoming call and triggers the auto-reply.
 *
 * Logic: when the phone RINGS we remember the incoming number. If the phone
 * then goes IDLE without ever going OFFHOOK (i.e. the call was never answered),
 * that is a missed call → reply. If it went OFFHOOK (answered), we do nothing.
 */
class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incoming = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                if (!incoming.isNullOrBlank()) lastRingingNumber = incoming
                wasAnswered = false
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Either an outgoing call or the incoming one got answered.
                wasAnswered = true
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                val number = lastRingingNumber
                val missed = !wasAnswered && !number.isNullOrBlank()
                lastRingingNumber = null
                wasAnswered = false
                if (missed) {
                    // Small pass to the manager; it checks settings + exclude list.
                    CallReplyManager.handleMissedCall(context, number)
                }
            }
        }
    }

    companion object {
        // Broadcast receiver instances are short-lived; keep call state in statics.
        @Volatile private var lastRingingNumber: String? = null
        @Volatile private var wasAnswered: Boolean = false
    }
}
