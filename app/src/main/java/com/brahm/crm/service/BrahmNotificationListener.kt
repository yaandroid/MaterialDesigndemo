package com.brahm.crm.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.brahm.crm.BrahmApp
import com.brahm.crm.data.NotificationLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Reads THIS phone's own incoming notifications and stores them so the app can
 * show a per-person inbox. Read-only: it never replies, dismisses or sends
 * anything. Honours the "Inbox" feature toggle in Settings.
 */
class BrahmNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val settings = BrahmApp.settings
        if (!settings.inboxEnabled) return

        val pkg = sbn.packageName ?: return
        // Skip our own app and clearly ongoing/system notifications.
        if (pkg == packageName) return
        val extras = sbn.notification?.extras ?: return
        if (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (title.isEmpty() && text.isEmpty()) return

        val appLabel = resolveAppLabel(pkg)
        val person = title.ifEmpty { appLabel }

        // If this notification is a "message deleted" marker, flag the person's
        // last stored message as deleted instead of saving the marker itself.
        if (isDeletionMarker(text)) {
            scope.launch {
                runCatching {
                    val dao = BrahmApp.db.notificationDao()
                    dao.latestIdForPerson(person, pkg)?.let { dao.markDeleted(it) }
                }
            }
            return
        }

        val body = text.ifEmpty { "(no text)" }
        val log = NotificationLog(
            person = person,
            packageName = pkg,
            appLabel = appLabel,
            text = body,
            postedAt = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()
        )
        scope.launch {
            runCatching {
                val dao = BrahmApp.db.notificationDao()
                // Skip if the same message from the same person arrived in the last 60s
                // (messaging apps re-post a notification several times as it updates).
                val since = System.currentTimeMillis() - 60_000L
                if (dao.recentDuplicateCount(person, pkg, body, since) == 0) {
                    dao.insert(log)
                }
            }
        }
    }

    private fun isDeletionMarker(text: String): Boolean {
        if (text.isBlank()) return false
        val t = text.trim().lowercase()
        return DELETION_MARKERS.any { t.contains(it) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // read-only: nothing to do
    }

    private fun resolveAppLabel(pkg: String): String = try {
        val pm = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }

    companion object {
        // Lowercased fragments that appear when a chat message is deleted.
        private val DELETION_MARKERS = listOf(
            "this message was deleted",
            "you deleted this message",
            "deleted this message",
            "message was deleted",
            "यह संदेश हटा दिया गया",
            "यह मैसेज डिलीट कर दिया गया",
            "आपने यह संदेश हटा दिया",
            "संदेश हटा दिया गया"
        )
    }
}
