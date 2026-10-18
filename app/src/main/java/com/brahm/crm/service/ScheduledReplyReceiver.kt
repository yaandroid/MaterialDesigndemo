package com.brahm.crm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.brahm.crm.BrahmApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fires at a scheduled reply's due time and posts a one-tap WhatsApp assisted
 * notification (never auto-sends). Also exposes helpers to (re)schedule.
 */
class ScheduledReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0) return
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            runCatching {
                val dao = BrahmApp.db.scheduledReplyDao()
                val r = dao.byId(id) ?: return@launch
                if (r.sent) return@launch
                // Reuse the same assisted-notification path as missed-call WhatsApp.
                CallReplyManager.postAssisted(context, r.person, r.phone, r.text, r.target)
                dao.markSent(id)
            }
        }
    }

    companion object {
        const val EXTRA_ID = "scheduled_reply_id"
        private const val ACTION = "com.brahm.crm.SCHEDULED_REPLY"

        fun schedule(context: Context, id: Long, dueAt: Long) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = pending(context, id)
            try {
                // Inexact alarm — needs no special permission; fine for a reply reminder.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pi)
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, dueAt, pi)
                }
            } catch (t: Throwable) {
                am.set(AlarmManager.RTC_WAKEUP, dueAt, pi)
            }
        }

        fun cancel(context: Context, id: Long) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pending(context, id))
        }

        private fun pending(context: Context, id: Long): PendingIntent {
            val intent = Intent(context, ScheduledReplyReceiver::class.java).apply {
                action = ACTION
                putExtra(EXTRA_ID, id)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
            return PendingIntent.getBroadcast(context, id.toInt(), intent, flags)
        }
    }
}
