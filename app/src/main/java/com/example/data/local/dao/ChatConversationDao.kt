package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.ChatConversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatConversationDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun getAllConversations(): Flow<List<ChatConversation>>

    @Query("SELECT * FROM conversations WHERE isArchived = 0 ORDER BY timestamp DESC")
    fun getActiveConversations(): Flow<List<ChatConversation>>

    @Query("SELECT * FROM conversations WHERE isArchived = 1 ORDER BY timestamp DESC")
    fun getArchivedConversations(): Flow<List<ChatConversation>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ChatConversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ChatConversation)

    @Update
    suspend fun updateConversation(conversation: ChatConversation)

    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: String, title: String)

    @Query("UPDATE conversations SET folderName = :folderName WHERE id = :id")
    suspend fun updateFolder(id: String, folderName: String?)

    @Query("UPDATE conversations SET isArchived = :isArchived WHERE id = :id")
    suspend fun updateArchived(id: String, isArchived: Boolean)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: String)

    @Delete
    suspend fun deleteConversation(conversation: ChatConversation)
}
