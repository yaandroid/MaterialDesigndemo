package com.brahm.crm.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.brahm.crm.BrahmApp
import com.brahm.crm.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fires a scheduled broadcast at its due time.
 *  - SMS: sends to all numbers automatically.
 *  - WhatsApp: posts a reminder notification (assisted — the user opens the
 *    Broadcast screen and taps Send per contact); never auto-sends.
 */
class BroadcastAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0) return
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            runCatching {
                val dao = BrahmApp.db.scheduledBroadcastDao()
                val b = dao.byId(id) ?: return@launch
                if (b.sent) return@launch
                val phones = b.phonesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (b.channel == "sms") {
                    BulkSender.sendSms(context, phones, b.message, BrahmApp.settings.callSimSubId)
                    notify(context, id.toInt(), "Broadcast sent", "SMS sent to ${phones.size} contact(s).")
                } else {
                    notify(
                        context, id.toInt(),
                        "WhatsApp broadcast ready",
                        "Open BRAHM CRM → Broadcast to send to ${phones.size} contact(s)."
                    )
                }
                dao.markSent(id)
            }
        }
    }

    private fun notify(context: Context, nid: Int, title: String, text: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            nm.getNotificationChannel(CHANNEL) == null
        ) {
            nm.createNotificationChannel(
                android.app.NotificationChannel(CHANNEL, "Broadcasts", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = PendingIntent.getActivity(
            context, nid, open,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
        )
        nm.notify(
            nid,
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
        )
    }

    companion object {
        const val EXTRA_ID = "broadcast_id"
        private const val CHANNEL = "brahm_broadcast"
        private const val ACTION = "com.brahm.crm.BROADCAST_FIRE"

        fun schedule(context: Context, id: Long, dueAt: Long) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = pending(context, id)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pi)
                else am.set(AlarmManager.RTC_WAKEUP, dueAt, pi)
            } catch (t: Throwable) {
                am.set(AlarmManager.RTC_WAKEUP, dueAt, pi)
            }
        }

        private fun pending(context: Context, id: Long): PendingIntent {
            val intent = Intent(context, BroadcastAlarmReceiver::class.java).apply {
                action = ACTION
                putExtra(EXTRA_ID, id)
            }
            return PendingIntent.getBroadcast(
                context, (id + 100000).toInt(), intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                else PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
