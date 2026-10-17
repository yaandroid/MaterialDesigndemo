package com.brahm.crm

import android.app.Application
import com.brahm.crm.data.AppDatabase
import com.brahm.crm.data.SettingsStore

class BrahmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: BrahmApp
            private set

        val db: AppDatabase get() = AppDatabase.get(instance)
        val settings: SettingsStore get() = SettingsStore.get(instance)
    }
}
