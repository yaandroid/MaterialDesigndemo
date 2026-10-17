package com.brahm.crm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Kanban stages for a lead. Order matters (board column order). */
enum class Stage(val label: String) {
    NEW("New"),
    FOLLOW_UP("Follow-up"),
    CLOSED("Closed");

    companion object {
        fun from(name: String?): Stage =
            entries.firstOrNull { it.name == name } ?: NEW
    }
}

@Entity(tableName = "leads")
data class Lead(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val source: String = "Manual",
    val note: String = "",
    val stage: String = Stage.NEW.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val leadId: Long? = null,
    val title: String,
    val dueAt: Long? = null,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** One stored notification event (this phone's own incoming notifications). */
@Entity(tableName = "notifications")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,          // title / sender as reported by the source app
    val packageName: String,     // e.g. com.whatsapp
    val appLabel: String,        // human readable app name
    val text: String,            // notification body text
    val postedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false // true if the sender later deleted this message
)

/** Lightweight grouped row for the inbox list (one line per person). */
data class InboxPerson(
    val person: String,
    val appLabel: String,
    val lastText: String,
    val lastAt: Long,
    val count: Int
)
