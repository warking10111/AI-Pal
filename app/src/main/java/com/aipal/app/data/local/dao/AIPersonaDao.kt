package com.aipal.app.data.local.dao

import androidx.room.*
import com.aipal.app.data.local.entity.AIPersona
import kotlinx.coroutines.flow.Flow

@Dao
interface AIPersonaDao {
    @Query("SELECT * FROM personas WHERE isDeleted = 0 ORDER BY isCustom ASC, name ASC")
    fun getAllPersonas(): Flow<List<AIPersona>>

    @Query("SELECT * FROM personas WHERE isDeleted = 0 AND isCustom = 1 ORDER BY name ASC")
    fun getCustomPersonas(): Flow<List<AIPersona>>

    @Query("SELECT * FROM personas WHERE isDeleted = 0 AND id = :id")
    suspend fun getPersonaById(id: String): AIPersona?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: AIPersona)

    @Query("UPDATE personas SET isDeleted = :isDeleted, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeletePersona(id: String, isDeleted: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE personas SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePinned(id: String, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE personas SET isFavourite = :isFavourite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavourite(id: String, isFavourite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM personas WHERE id = :id")
    suspend fun deletePersonaById(id: String)

    @Delete
    suspend fun deletePersona(persona: AIPersona)
}
