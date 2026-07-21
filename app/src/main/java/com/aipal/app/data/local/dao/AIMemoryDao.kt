package com.aipal.app.data.local.dao

import androidx.room.*
import com.aipal.app.data.local.entity.AIMemory
import kotlinx.coroutines.flow.Flow

@Dao
interface AIMemoryDao {
    @Query("SELECT * FROM memories WHERE isDeleted = 0 AND userId = :userId ORDER BY timestamp DESC")
    fun getAllMemories(userId: String): Flow<List<AIMemory>>

    @Query("SELECT * FROM memories WHERE isDeleted = 0 AND isEnabled = 1 AND userId = :userId ORDER BY timestamp DESC")
    fun getEnabledMemories(userId: String): Flow<List<AIMemory>>

    @Query("SELECT * FROM memories WHERE isDeleted = 0 AND isEnabled = 1 AND userId = :userId ORDER BY timestamp DESC")
    suspend fun getEnabledMemoriesSync(userId: String): List<AIMemory>

    @Query("SELECT * FROM memories WHERE isDeleted = 0")
    suspend fun getMemoriesSync(): List<AIMemory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: AIMemory)

    @Query("UPDATE memories SET isEnabled = :isEnabled, modifiedDate = :modifiedDate, updatedAt = :modifiedDate WHERE id = :id")
    suspend fun updateMemoryEnabled(id: String, isEnabled: Boolean, modifiedDate: Long = System.currentTimeMillis())

    @Query("UPDATE memories SET isPinned = :isPinned, modifiedDate = :modifiedDate, updatedAt = :modifiedDate WHERE id = :id")
    suspend fun updateMemoryPinned(id: String, isPinned: Boolean, modifiedDate: Long = System.currentTimeMillis())

    @Query("UPDATE memories SET isArchived = :isArchived, modifiedDate = :modifiedDate, updatedAt = :modifiedDate WHERE id = :id")
    suspend fun updateMemoryArchived(id: String, isArchived: Boolean, modifiedDate: Long = System.currentTimeMillis())

    @Query("UPDATE memories SET isDeleted = :isDeleted, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteMemory(id: String, isDeleted: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE memories SET isFavourite = :isFavourite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateMemoryFavourite(id: String, isFavourite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: String)

    @Delete
    suspend fun deleteMemory(memory: AIMemory)
}
