package com.brahm.crm.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LeadDao {
    @Query("SELECT * FROM leads ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Lead>>

    @Query("SELECT * FROM leads WHERE stage = :stage ORDER BY updatedAt DESC")
    fun observeByStage(stage: String): Flow<List<Lead>>

    @Query("SELECT * FROM leads WHERE id = :id")
    suspend fun byId(id: Long): Lead?

    @Query("SELECT COUNT(*) FROM leads")
    fun countAll(): Flow<Int>

    @Query("SELECT COUNT(*) FROM leads WHERE stage = :stage")
    fun countByStage(stage: String): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM leads WHERE phone = :phone AND phone != '')")
    suspend fun phoneExists(phone: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(lead: Lead): Long

    @Update
    suspend fun update(lead: Lead)

    @Delete
    suspend fun delete(lead: Lead)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY done ASC, COALESCE(dueAt, createdAt) ASC")
    fun observeAll(): Flow<List<TaskItem>>

    @Query("SELECT COUNT(*) FROM tasks WHERE done = 0")
    fun countOpen(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskItem): Long

    @Update
    suspend fun update(task: TaskItem)

    @Delete
    suspend fun delete(task: TaskItem)
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: NotificationLog): Long

    /** Guards against WhatsApp etc. re-posting the same message many times. */
    @Query(
        "SELECT COUNT(*) FROM notifications WHERE person = :person AND packageName = :pkg " +
            "AND text = :text AND postedAt >= :since"
    )
    suspend fun recentDuplicateCount(person: String, pkg: String, text: String, since: Long): Int

    @Query(
        """
        SELECT person AS person,
               appLabel AS appLabel,
               text AS lastText,
               MAX(postedAt) AS lastAt,
               COUNT(*) AS count
        FROM notifications
        GROUP BY person, packageName
        ORDER BY lastAt DESC
        """
    )
    fun observePeople(): Flow<List<InboxPerson>>

    @Query("SELECT * FROM notifications WHERE person = :person ORDER BY postedAt DESC")
    fun observeForPerson(person: String): Flow<List<NotificationLog>>

    @Query("SELECT COUNT(DISTINCT person) FROM notifications")
    fun countPeople(): Flow<Int>

    @Query(
        "SELECT id FROM notifications WHERE person = :person AND packageName = :pkg " +
            "AND deleted = 0 ORDER BY postedAt DESC LIMIT 1"
    )
    suspend fun latestIdForPerson(person: String, pkg: String): Long?

    @Query("UPDATE notifications SET deleted = 1 WHERE id = :id")
    suspend fun markDeleted(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("DELETE FROM notifications WHERE postedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Dao
interface PersonSettingDao {
    @Query("SELECT * FROM person_settings WHERE person = :person")
    fun observe(person: String): Flow<PersonSetting?>

    @Query("SELECT * FROM person_settings WHERE person = :person")
    suspend fun get(person: String): PersonSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: PersonSetting)
}

@Dao
interface ScheduledReplyDao {
    @Query("SELECT * FROM scheduled_replies ORDER BY sent ASC, dueAt ASC")
    fun observeAll(): Flow<List<ScheduledReply>>

    @Query("SELECT * FROM scheduled_replies WHERE person = :person ORDER BY dueAt ASC")
    fun observeForPerson(person: String): Flow<List<ScheduledReply>>

    @Query("SELECT * FROM scheduled_replies WHERE id = :id")
    suspend fun byId(id: Long): ScheduledReply?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(r: ScheduledReply): Long

    @Query("UPDATE scheduled_replies SET sent = 1 WHERE id = :id")
    suspend fun markSent(id: Long)

    @Delete
    suspend fun delete(r: ScheduledReply)
}

@Dao
interface ScheduledBroadcastDao {
    @Query("SELECT * FROM scheduled_broadcasts ORDER BY sent ASC, dueAt ASC")
    fun observeAll(): Flow<List<ScheduledBroadcast>>

    @Query("SELECT * FROM scheduled_broadcasts WHERE id = :id")
    suspend fun byId(id: Long): ScheduledBroadcast?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(b: ScheduledBroadcast): Long

    @Query("UPDATE scheduled_broadcasts SET sent = 1 WHERE id = :id")
    suspend fun markSent(id: Long)

    @Delete
    suspend fun delete(b: ScheduledBroadcast)
}
