package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.myapplication.data.local.entity.ChatMessage
import com.example.myapplication.data.local.entity.SceneLog
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    // 聊天记录操作
    @Insert
    suspend fun insertMessage(message: ChatMessage)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<ChatMessage>

    // 场景日志操作
    @Insert
    suspend fun insertSceneLog(log: SceneLog)

    @Query("SELECT * FROM scene_logs ORDER BY timestamp DESC")
    fun getAllSceneLogs(): Flow<List<SceneLog>>
    
    @Query("SELECT * FROM scene_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSceneLogs(limit: Int): List<SceneLog>
    
    // 清理操作
    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()
    
    @Query("DELETE FROM scene_logs")
    suspend fun clearSceneLogs()
}
