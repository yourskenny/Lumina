package com.example.myapplication.domain.context

import com.example.myapplication.data.model.RawObject

/**
 * 实时上下文快照
 * 包含视觉、系统状态等瞬时信息
 */
data class ContextSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    
    // 视觉感知 (来自 YOLO)
    val detectedObjects: List<RawObject> = emptyList(),
    
    // 系统感知
    val batteryLevel: Int = -1,
    val isCharging: Boolean = false,
    val location: String = "Unknown", // 经纬度或地名
    val currentTime: String = "",
    
    // 用户意图 (可选)
    val lastUserQuery: String? = null
) {
    /**
     * 生成适合 LLM 阅读的 Context String
     */
    fun toPromptString(): String {
        val objectsStr = if (detectedObjects.isEmpty()) {
            "当前视野中没有检测到明显物体。"
        } else {
            val details = detectedObjects.joinToString(", ") { obj ->
                "${obj.name} (距离: ${String.format("%.1f", obj.distanceM)}米)"
            }
            "当前视野中检测到: $details"
        }
        
        return """
            [当前环境状态]
            时间: $currentTime
            位置: $location
            电量: $batteryLevel% ${if(isCharging) "(充电中)" else ""}
            视觉感知: $objectsStr
        """.trimIndent()
    }
}
