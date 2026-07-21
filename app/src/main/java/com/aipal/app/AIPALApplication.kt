package com.aipal.app

import android.app.Application
import com.aipal.app.data.auth.AuthService
import com.aipal.app.data.auth.LocalAuthServiceImpl
import com.aipal.app.data.local.AppDatabase
import com.aipal.app.data.repository.ChatRepository
import com.aipal.app.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class AIPALApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val chatRepository by lazy { ChatRepository(database, settingsRepository) }
    val authService: AuthService by lazy { LocalAuthServiceImpl(this, database, settingsRepository) }
}
