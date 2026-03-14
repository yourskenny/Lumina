package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 聊天消息实体
 * 记录用户和Agent的对话历史
 */
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val role: String, // "user" or "agent"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
