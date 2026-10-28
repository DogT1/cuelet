package com.tuvin.pixelautomations

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.tuvin.pixelautomations.engine.Notifications
import com.tuvin.pixelautomations.setup.I18n
import com.tuvin.pixelautomations.ui.ThemeSettings
import com.tuvin.pixelautomations.model.AppDb
import com.tuvin.pixelautomations.model.AutomationRepository

class App : Application() {
    val db: AppDb by lazy { Room.databaseBuilder(this, AppDb::class.java, "automations.db").build() }
    val repo: AutomationRepository by lazy { AutomationRepository(db) }

    override fun onCreate() {
        super.onCreate()
        I18n.load(this)
        ThemeSettings.load(this)
        Notifications.createChannels(this)
    }
}

val Context.app: App get() = applicationContext as App
