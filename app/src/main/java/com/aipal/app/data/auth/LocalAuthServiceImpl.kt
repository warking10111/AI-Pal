package com.aipal.app.data.auth

import android.content.Context
import com.aipal.app.data.local.AppDatabase
import com.aipal.app.data.local.entity.UserEntity
import com.aipal.app.data.repository.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.UUID

class LocalAuthServiceImpl(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository
) : AuthService {

    private val prefs = context.getSharedPreferences("aipal_auth", Context.MODE_PRIVATE)
    private val userDao = database.userDao()
    private val conversationDao = database.conversationDao()
    private val memoryDao = database.memoryDao()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Restore active session upon initialization
        val activeUserId = prefs.getString("active_user_id", null)
        val isGuest = prefs.getBoolean("is_guest", false)

        if (activeUserId != null) {
            _authState.value = AuthState.Loading
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                try {
                    val userEntity = userDao.getUserById(activeUserId)
                    if (userEntity != null) {
                        _authState.value = AuthState.Authenticated(userEntity.toDomain())
                    } else if (isGuest) {
                        // Re-initialize guest
                        val guestProfile = UserProfile(
                            id = activeUserId,
                            email = "guest@aipal.local",
                            displayName = "Guest User",
                            isGuest = true,
                            isEmailVerified = false,
                            avatarUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=$activeUserId",
                            credits = settingsRepository.getCredits()
                        )
                        _authState.value = AuthState.Authenticated(guestProfile)
                    } else {
                        _authState.value = AuthState.Unauthenticated
                    }
                } catch (e: Exception) {
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }

    private fun UserEntity.toDomain() = UserProfile(
        id = id,
        email = email,
        displayName = displayName,
        isGuest = isGuest,
        isEmailVerified = isEmailVerified,
        avatarUrl = avatarUrl,
        credits = credits,
        lastSyncTime = lastSyncTime
    )

    private fun hashPassword(password: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(password.toByteArray())
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            password // Fallback if hashing fails
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            _authState.value = AuthState.Loading
            val normalizedEmail = email.trim().lowercase()
            val user = userDao.getUserByEmail(normalizedEmail)
            
            if (user == null) {
                val errorMsg = "Account not found for $email."
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val inputHash = hashPassword(password)
            if (user.passwordHash != inputHash) {
                val errorMsg = "Incorrect password. Please try again."
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val domainUser = user.toDomain()
            prefs.edit().putString("active_user_id", user.id).putBoolean("is_guest", false).apply()
            
            // Sync settings credits
            settingsRepository.setCredits(user.credits)

            _authState.value = AuthState.Authenticated(domainUser)
            Result.success(domainUser)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            _authState.value = AuthState.Loading
            val normalizedEmail = email.trim().lowercase()
            val existing = userDao.getUserByEmail(normalizedEmail)
            
            if (existing != null) {
                val errorMsg = "An account with email $email already exists."
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            if (password.length < 6) {
                val errorMsg = "Password must be at least 6 characters."
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val userId = UUID.randomUUID().toString()
            val userEntity = UserEntity(
                id = userId,
                email = normalizedEmail,
                displayName = displayName.trim().ifEmpty { "User" },
                passwordHash = hashPassword(password),
                isEmailVerified = false,
                isGuest = false,
                avatarUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=${displayName.trim()}",
                credits = 100
            )

            userDao.insertUser(userEntity)
            val domainUser = userEntity.toDomain()
            
            prefs.edit().putString("active_user_id", userId).putBoolean("is_guest", false).apply()
            settingsRepository.setCredits(userEntity.credits)

            _authState.value = AuthState.Authenticated(domainUser)
            Result.success(domainUser)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Registration failed")
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String?, email: String?, displayName: String?): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            _authState.value = AuthState.Loading
            
            val finalEmail = email?.trim()?.lowercase() ?: "google.user@gmail.com"
            val finalName = displayName?.trim() ?: "Google Explorer"
            
            var user = userDao.getUserByEmail(finalEmail)
            if (user == null) {
                // Register Google user automatically
                val userId = UUID.randomUUID().toString()
                user = UserEntity(
                    id = userId,
                    email = finalEmail,
                    displayName = finalName,
                    passwordHash = "google-oauth-external",
                    isEmailVerified = true, // Google Sign-In emails are pre-verified
                    isGuest = false,
                    avatarUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=$finalName",
                    credits = 150 // Bonus credits for Google account link!
                )
                userDao.insertUser(user)
            }

            val domainUser = user.toDomain()
            prefs.edit().putString("active_user_id", user.id).putBoolean("is_guest", false).apply()
            settingsRepository.setCredits(user.credits)

            _authState.value = AuthState.Authenticated(domainUser)
            Result.success(domainUser)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Google Sign-In failed")
            Result.failure(e)
        }
    }

    override suspend fun signInAsGuest(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            _authState.value = AuthState.Loading
            val guestId = "guest_" + UUID.randomUUID().toString().take(8)
            val guestProfile = UserProfile(
                id = guestId,
                email = "guest@aipal.local",
                displayName = "Guest User",
                isGuest = true,
                isEmailVerified = false,
                avatarUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=$guestId",
                credits = 100
            )

            prefs.edit().putString("active_user_id", guestId).putBoolean("is_guest", true).apply()
            settingsRepository.setCredits(guestProfile.credits)

            _authState.value = AuthState.Authenticated(guestProfile)
            Result.success(guestProfile)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Guest login failed")
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserByEmail(email.trim().lowercase())
            if (user == null) {
                return@withContext Result.failure(Exception("No account exists with this email address."))
            }
            // In a mock/local environment, we simulate sending a verification link.
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val state = _authState.value
            if (state is AuthState.Authenticated) {
                val user = state.user
                if (!user.isGuest) {
                    val entity = userDao.getUserById(user.id)
                    if (entity != null) {
                        val updated = entity.copy(isEmailVerified = true) // Automatically verify during simulation
                        userDao.updateUser(updated)
                        _authState.value = AuthState.Authenticated(updated.toDomain())
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun upgradeGuestAccount(email: String, password: String, displayName: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val currentAuth = _authState.value
            if (currentAuth !is AuthState.Authenticated || !currentAuth.user.isGuest) {
                return@withContext Result.failure(Exception("No active guest session to upgrade."))
            }

            val guestId = currentAuth.user.id
            val normalizedEmail = email.trim().lowercase()
            val existing = userDao.getUserByEmail(normalizedEmail)
            if (existing != null) {
                return@withContext Result.failure(Exception("Email $email is already in use by another account."))
            }

            // Create new real account
            val newUserId = UUID.randomUUID().toString()
            val userEntity = UserEntity(
                id = newUserId,
                email = normalizedEmail,
                displayName = displayName.trim().ifEmpty { "User" },
                passwordHash = hashPassword(password),
                isEmailVerified = false,
                isGuest = false,
                avatarUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=${displayName.trim()}",
                credits = settingsRepository.getCredits() // Carry over guest credits
            )

            userDao.insertUser(userEntity)

            // CRITICAL: Migrate guest data to the new user without losing anything!
            migrateUserData(fromUserId = guestId, toUserId = newUserId)

            val domainUser = userEntity.toDomain()
            prefs.edit().putString("active_user_id", newUserId).putBoolean("is_guest", false).apply()
            
            _authState.value = AuthState.Authenticated(domainUser)
            Result.success(domainUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun upgradeGuestAccountWithGoogle(idToken: String?, email: String?, displayName: String?): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val currentAuth = _authState.value
            if (currentAuth !is AuthState.Authenticated || !currentAuth.user.isGuest) {
                return@withContext Result.failure(Exception("No active guest session to upgrade."))
            }

            val guestId = currentAuth.user.id
            val finalEmail = email?.trim()?.lowercase() ?: "google.user@gmail.com"
            val finalName = displayName?.trim() ?: "Google Explorer"

            var user = userDao.getUserByEmail(finalEmail)
            val newUserId = if (user == null) {
                val createdId = UUID.randomUUID().toString()
                user = UserEntity(
                    id = createdId,
                    email = finalEmail,
                    displayName = finalName,
                    passwordHash = "google-oauth-external",
                    isEmailVerified = true,
                    isGuest = false,
                    avatarUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=$finalName",
                    credits = settingsRepository.getCredits() + 50 // Carry over + grant link bonus!
                )
                userDao.insertUser(user)
                createdId
            } else {
                user.id
            }

            // CRITICAL: Migrate guest data to the Google account!
            migrateUserData(fromUserId = guestId, toUserId = newUserId)

            val domainUser = user.toDomain()
            prefs.edit().putString("active_user_id", newUserId).putBoolean("is_guest", false).apply()
            settingsRepository.setCredits(domainUser.credits)

            _authState.value = AuthState.Authenticated(domainUser)
            Result.success(domainUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun migrateUserData(fromUserId: String, toUserId: String) {
        // 1. Migrate general "guest" keyword threads and specifically tagged guest threads
        val conversations = conversationDao.getConversationsSync()
        for (con in conversations) {
            if (con.userId == fromUserId || con.userId == "guest") {
                val updatedCon = con.copy(userId = toUserId)
                conversationDao.insertConversation(updatedCon) // Room will replace on conflict (or insert)
            }
        }

        // 2. Migrate memories
        val memories = memoryDao.getMemoriesSync()
        for (mem in memories) {
            if (mem.userId == fromUserId || mem.userId == "guest") {
                val updatedMem = mem.copy(userId = toUserId)
                memoryDao.insertMemory(updatedMem)
            }
        }
    }

    override suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val state = _authState.value
            if (state is AuthState.Authenticated) {
                val user = state.user
                if (!user.isGuest) {
                    val entity = userDao.getUserById(user.id)
                    if (entity != null) {
                        userDao.deleteUser(entity)
                    }
                }
                
                // Clear user conversations and memories as part of strict account deletion
                val conversations = conversationDao.getConversationsSync()
                for (con in conversations) {
                    if (con.userId == user.id) {
                        conversationDao.deleteConversation(con)
                    }
                }

                val memories = memoryDao.getMemoriesSync()
                for (mem in memories) {
                    if (mem.userId == user.id) {
                        memoryDao.deleteMemory(mem)
                    }
                }
            }
            logout()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun synchronizeProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val state = _authState.value
            if (state is AuthState.Authenticated) {
                val user = state.user
                if (!user.isGuest) {
                    val entity = userDao.getUserById(user.id)
                    if (entity != null) {
                        // Simulate updating credits or syncing verified states
                        val synced = entity.copy(
                            credits = settingsRepository.getCredits(),
                            lastSyncTime = System.currentTimeMillis()
                        )
                        userDao.updateUser(synced)
                        val domain = synced.toDomain()
                        _authState.value = AuthState.Authenticated(domain)
                        return@withContext Result.success(domain)
                    }
                }
                return@withContext Result.success(user)
            }
            Result.failure(Exception("User not authenticated"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            prefs.edit().remove("active_user_id").remove("is_guest").apply()
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
