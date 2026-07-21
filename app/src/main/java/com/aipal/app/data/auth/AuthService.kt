package com.aipal.app.data.auth

import kotlinx.coroutines.flow.StateFlow

data class UserProfile(
    val id: String,
    val email: String,
    val displayName: String,
    val isGuest: Boolean,
    val isEmailVerified: Boolean,
    val avatarUrl: String,
    val credits: Int = 100,
    val lastSyncTime: Long = System.currentTimeMillis()
)

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
    object Loading : AuthState()
}

interface AuthService {
    val authState: StateFlow<AuthState>
    
    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile>
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<UserProfile>
    suspend fun signInWithGoogle(idToken: String?, email: String?, displayName: String?): Result<UserProfile>
    suspend fun signInAsGuest(): Result<UserProfile>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun upgradeGuestAccount(email: String, password: String, displayName: String): Result<UserProfile>
    suspend fun upgradeGuestAccountWithGoogle(idToken: String?, email: String?, displayName: String?): Result<UserProfile>
    suspend fun deleteAccount(): Result<Unit>
    suspend fun synchronizeProfile(): Result<UserProfile>
    suspend fun logout(): Result<Unit>
}
