package com.aipal.app.data.local.dao

import androidx.room.*
import com.aipal.app.data.local.entity.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM messages WHERE isDeleted = 0 AND conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM messages WHERE isDeleted = 0 AND conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesForConversationSync(conversationId: String): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("UPDATE messages SET text = :text, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateMessageText(id: String, text: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE messages SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePinned(id: String, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE messages SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateArchived(id: String, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE messages SET isDeleted = :isDeleted, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteMessage(id: String, isDeleted: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE messages SET isFavourite = :isFavourite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavourite(id: String, isFavourite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)
}
