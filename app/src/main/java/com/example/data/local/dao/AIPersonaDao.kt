package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AIPersona
import kotlinx.coroutines.flow.Flow

@Dao
interface AIPersonaDao {
    @Query("SELECT * FROM personas ORDER BY isCustom ASC, name ASC")
    fun getAllPersonas(): Flow<List<AIPersona>>

    @Query("SELECT * FROM personas WHERE isCustom = 1 ORDER BY name ASC")
    fun getCustomPersonas(): Flow<List<AIPersona>>

    @Query("SELECT * FROM personas WHERE id = :id")
    suspend fun getPersonaById(id: String): AIPersona?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: AIPersona)

    @Query("DELETE FROM personas WHERE id = :id")
    suspend fun deletePersonaById(id: String)

    @Delete
    suspend fun deletePersona(persona: AIPersona)
}
