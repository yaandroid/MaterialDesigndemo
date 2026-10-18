package com.brahm.crm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

/**
 * Detects call events and triggers the auto-reply:
 *   MISSED   — rang but never answered
 *   INCOMING — answered incoming call (ended)
 *   OUTGOING — a call the user placed (ended)
 * Also captures the SIM subscription id when the platform provides it, so the
 * reply can go from the same SIM the call used.
 */
class PhoneStateReceiver : BroadcastReceiver() {

    enum class Event { MISSED, INCOMING, OUTGOING }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_NEW_OUTGOING_CALL -> {
                val num = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                if (!num.isNullOrBlank()) {
                    lastNumber = num
                    isOutgoing = true
                }
                captureSubId(intent)
            }
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> handlePhoneState(context, intent)
        }
    }

    private fun handlePhoneState(context: Context, intent: Intent) {
        captureSubId(intent)
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incoming = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                if (!incoming.isNullOrBlank()) lastNumber = incoming
                isOutgoing = false
                wasAnswered = false
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                wasAnswered = true
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                val number = lastNumber
                val event = when {
                    isOutgoing -> Event.OUTGOING
                    wasAnswered -> Event.INCOMING
                    else -> Event.MISSED
                }
                val sub = lastSubId
                // reset state
                lastNumber = null
                isOutgoing = false
                wasAnswered = false
                lastSubId = -1
                if (!number.isNullOrBlank()) {
                    CallReplyManager.handleCall(context, number, event, sub)
                }
            }
        }
    }

    /** Best-effort SIM subscription id from the broadcast extras (varies by OEM). */
    private fun captureSubId(intent: Intent) {
        val keys = listOf(
            "subscription", "android.telephony.extra.SUBSCRIPTION_INDEX",
            "simId", "subId", "slot", "com.android.phone.extra.slot"
        )
        for (k in keys) {
            if (intent.hasExtra(k)) {
                val v = intent.getIntExtra(k, -1)
                if (v >= 0) { lastSubId = v; return }
            }
        }
    }

    companion object {
        @Volatile private var lastNumber: String? = null
        @Volatile private var wasAnswered: Boolean = false
        @Volatile private var isOutgoing: Boolean = false
        @Volatile private var lastSubId: Int = -1
    }
}
