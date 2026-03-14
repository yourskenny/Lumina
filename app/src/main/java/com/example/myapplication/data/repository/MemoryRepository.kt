package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.local.database.AppDatabase
import com.example.myapplication.data.local.entity.ChatMessage
import com.example.myapplication.data.local.entity.SceneLog
import kotlinx.coroutines.flow.Flow

/**
 * 记忆仓库
 * 管理所有持久化记忆 (聊天记录, 场景日志)
 */
class MemoryRepository(context: Context) {
    
    private val memoryDao = AppDatabase.getDatabase(context).memoryDao()

    // --- 聊天记录 ---
    
    val allMessages: Flow<List<ChatMessage>> = memoryDao.getAllMessages()

    suspend fun saveUserMessage(content: String) {
        memoryDao.insertMessage(ChatMessage(role = "user", content = content))
    }

    suspend fun saveAgentMessage(content: String) {
        memoryDao.insertMessage(ChatMessage(role = "agent", content = content))
    }
    
    suspend fun getRecentContext(limit: Int = 10): List<ChatMessage> {
        return memoryDao.getRecentMessages(limit)
    }

    // --- 场景日志 ---
    
    val allSceneLogs: Flow<List<SceneLog>> = memoryDao.getAllSceneLogs()

    suspend fun saveSceneLog(description: String) {
        memoryDao.insertSceneLog(SceneLog(description = description))
    }
    
    suspend fun getRecentSceneLogs(limit: Int = 5): List<SceneLog> {
        return memoryDao.getRecentSceneLogs(limit)
    }
    
    // --- 清理 ---
    
    suspend fun clearAllMemory() {
        memoryDao.clearChatHistory()
        memoryDao.clearSceneLogs()
    }
}
