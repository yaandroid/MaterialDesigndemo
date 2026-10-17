package com.brahm.crm.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateUtils
import androidx.core.net.toUri

object Permissions {
    /** True if the user has granted notification-access to our listener. */
    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        val cn = ComponentName(
            context, com.brahm.crm.service.BrahmNotificationListener::class.java
        )
        return enabled.split(":").any {
            val comp = ComponentName.unflattenFromString(it)
            comp != null && comp == cn
        }
    }

    fun openNotificationAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}

object Share {
    fun copyToClipboard(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
    }

    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** Opens WhatsApp chat with a prefilled message for the user to send manually. */
    fun openWhatsApp(context: Context, phone: String, message: String) {
        val num = phone.filter { it.isDigit() }
        val uri = ("https://wa.me/" + num + "?text=" + Uri.encode(message)).toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}

fun Long.asRelativeTime(): String =
    DateUtils.getRelativeTimeSpanString(
        this, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
    ).toString()
