package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AIMemory
import kotlinx.coroutines.flow.Flow

@Dao
interface AIMemoryDao {
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<AIMemory>>

    @Query("SELECT * FROM memories WHERE isEnabled = 1 ORDER BY timestamp DESC")
    fun getEnabledMemories(): Flow<List<AIMemory>>

    @Query("SELECT * FROM memories WHERE isEnabled = 1 ORDER BY timestamp DESC")
    suspend fun getEnabledMemoriesSync(): List<AIMemory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: AIMemory)

    @Query("UPDATE memories SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateMemoryEnabled(id: String, isEnabled: Boolean)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: String)

    @Delete
    suspend fun deleteMemory(memory: AIMemory)
}
