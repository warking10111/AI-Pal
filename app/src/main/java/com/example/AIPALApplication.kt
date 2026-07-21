package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.ChatRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class AIPALApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val chatRepository by lazy { ChatRepository(database) }
    val settingsRepository by lazy { SettingsRepository(this) }
}
