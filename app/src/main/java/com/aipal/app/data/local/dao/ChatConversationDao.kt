package com.aipal.app.data.local.dao

import androidx.room.*
import com.aipal.app.data.local.entity.ChatConversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatConversationDao {
    @Query("SELECT * FROM conversations WHERE isDeleted = 0 AND userId = :userId ORDER BY timestamp DESC")
    fun getAllConversations(userId: String): Flow<List<ChatConversation>>

    @Query("SELECT * FROM conversations WHERE isDeleted = 0 AND isArchived = 0 AND userId = :userId ORDER BY timestamp DESC")
    fun getActiveConversations(userId: String): Flow<List<ChatConversation>>

    @Query("SELECT * FROM conversations WHERE isDeleted = 0 AND isArchived = 1 AND userId = :userId ORDER BY timestamp DESC")
    fun getArchivedConversations(userId: String): Flow<List<ChatConversation>>

    @Query("SELECT * FROM conversations WHERE isDeleted = 0")
    suspend fun getConversationsSync(): List<ChatConversation>

    @Query("SELECT * FROM conversations WHERE isDeleted = 0 AND id = :id")
    suspend fun getConversationById(id: String): ChatConversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ChatConversation)

    @Update
    suspend fun updateConversation(conversation: ChatConversation)

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: String, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET folderName = :folderName, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFolder(id: String, folderName: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateArchived(id: String, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePinned(id: String, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET isDeleted = :isDeleted, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteConversation(id: String, isDeleted: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET isFavourite = :isFavourite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavourite(id: String, isFavourite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: String)

    @Delete
    suspend fun deleteConversation(conversation: ChatConversation)
}
