package com.brahm.crm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Lead::class, TaskItem::class, NotificationLog::class,
        PersonSetting::class, ScheduledReply::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun leadDao(): LeadDao
    abstract fun taskDao(): TaskDao
    abstract fun notificationDao(): NotificationDao
    abstract fun personSettingDao(): PersonSettingDao
    abstract fun scheduledReplyDao(): ScheduledReplyDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "brahm_crm.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
