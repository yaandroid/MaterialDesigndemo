package com.brahm.crm.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.brahm.crm.BrahmApp
import com.brahm.crm.MainActivity

/**
 * Handles what happens after a missed call:
 *   1. (optional) sends an auto-reply SMS from the chosen SIM
 *   2. (optional) posts a one-tap notification that opens WhatsApp / WA Business
 *      with a prefilled message for the user to send manually.
 *
 * It never auto-sends anything on WhatsApp — the user taps Send themselves.
 */
object CallReplyManager {

    private const val CHANNEL_ID = "brahm_call_reply"
    private const val CHANNEL_NAME = "Call auto-reply"

    fun handleCall(
        context: Context,
        rawNumber: String?,
        event: PhoneStateReceiver.Event,
        callSubId: Int
    ) {
        val number = rawNumber?.trim().orEmpty()
        if (number.isEmpty()) return

        val s = BrahmApp.settings
        if (!s.callReplyEnabled) return
        if (s.isExcluded(number)) return

        // Honour per-event toggles.
        val allowed = when (event) {
            PhoneStateReceiver.Event.MISSED -> s.callOnMissed
            PhoneStateReceiver.Event.INCOMING -> s.callOnIncoming
            PhoneStateReceiver.Event.OUTGOING -> s.callOnOutgoing
        }
        if (!allowed) return

        // Choose the SIM: same SIM the call used (if detected + toggle), else the chosen default.
        val subId = if (s.callReplyFromSameSim && callSubId >= 0) callSubId else s.callSimSubId

        val (message, channel, label) = when (event) {
            PhoneStateReceiver.Event.MISSED -> Triple(s.msgMissed, s.channelMissed, "Missed call: $number")
            PhoneStateReceiver.Event.INCOMING -> Triple(s.msgIncoming, s.channelIncoming, "Call: $number")
            PhoneStateReceiver.Event.OUTGOING -> Triple(s.msgOutgoing, s.channelOutgoing, "You called: $number")
        }

        if (channel == "sms" || channel == "both") {
            trySendSms(context, number, message, subId)
        }
        if (channel == "wa" || channel == "both") {
            postAssisted(context, number, number, message, s.callWhatsappTarget, label)
        }
    }

    /**
     * Public entry used by missed-call and scheduled-reply flows: posts a
     * one-tap notification that opens WhatsApp with the message prefilled.
     */
    fun postAssisted(
        context: Context,
        person: String,
        phone: String,
        text: String,
        target: String,
        title: String = "Reply to $person"
    ) {
        postWhatsAppReplyNotification(context, phone.ifBlank { person }, text, target, title)
    }

    // ---------------- SMS ----------------
    private fun trySendSms(context: Context, number: String, text: String, subId: Int) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        if (text.isBlank()) return

        try {
            val sms: SmsManager = resolveSmsManager(context, subId)
            val parts = sms.divideMessage(text)
            if (parts.size > 1) {
                sms.sendMultipartTextMessage(number, null, parts, null, null)
            } else {
                sms.sendTextMessage(number, null, text, null, null)
            }
        } catch (t: Throwable) {
            // Never crash on a telephony edge case (e.g. no SIM / VoIP-only device).
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveSmsManager(context: Context, subId: Int): SmsManager {
        // subId < 0 means "system default SIM"
        if (subId >= 0) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
                    .createForSubscriptionId(subId)
            } else {
                SmsManager.getSmsManagerForSubscriptionId(subId)
            }
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }
    }

    // ---------------- WhatsApp (assisted) ----------------
    private fun postWhatsAppReplyNotification(
        context: Context,
        number: String,
        text: String,
        target: String,
        title: String = "Reply on WhatsApp"
    ) {
        ensureChannel(context)

        val pkg = if (target == "wab") "com.whatsapp.w4b" else "com.whatsapp"
        val waPackage = if (isInstalled(context, pkg)) pkg
        else if (isInstalled(context, "com.whatsapp")) "com.whatsapp"
        else if (isInstalled(context, "com.whatsapp.w4b")) "com.whatsapp.w4b"
        else null

        val digits = number.filter { it.isDigit() || it == '+' }.removePrefix("+")
        val uri = Uri.parse("https://wa.me/$digits?text=" + Uri.encode(text))
        val waIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            if (waPackage != null) setPackage(waPackage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pi = PendingIntent.getActivity(
            context,
            number.hashCode(),
            waIntent,
            pendingFlags()
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle(title)
            .setContentText("Tap to reply on WhatsApp (you press Send)")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Tap to open WhatsApp with your reply ready. You press Send."))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .build()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(number.hashCode(), notif)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }
    }

    private fun isInstalled(context: Context, pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0); true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun pendingFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
}
