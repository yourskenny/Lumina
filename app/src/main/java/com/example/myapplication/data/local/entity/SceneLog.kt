package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 场景日志实体
 * 记录VLM生成的场景描述
 */
@Entity(tableName = "scene_logs")
data class SceneLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    
    // 预留 Embedding 字段 (FloatArray在Room中需要TypeConverter,这里暂时用String占位)
    // 未来可以用Gson序列化或专门的表
    val embedding: String? = null 
)
