package com.aipal.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val isEmailVerified: Boolean = false,
    val isGuest: Boolean = false,
    val avatarUrl: String = "https://api.dicebear.com/7.x/avataaars/svg?seed=Hein",
    val credits: Int = 100,
    val lastSyncTime: Long = System.currentTimeMillis()
)
