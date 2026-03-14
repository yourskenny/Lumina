package com.example.myapplication.domain.context

import android.content.Context
import android.os.BatteryManager
import com.example.myapplication.data.model.RawObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 上下文管理器
 * 负责聚合来自各个传感器(Camera, System, Location)的实时数据
 * 充当 Agent 的 "Working Memory" (短期记忆)
 */
class ContextManager(private val context: Context) {

    private val _snapshot = MutableStateFlow(ContextSnapshot())
    val snapshot: StateFlow<ContextSnapshot> = _snapshot.asStateFlow()

    /**
     * 更新视觉感知 (YOLO)
     */
    fun updateVisualContext(objects: List<RawObject>) {
        _snapshot.update { it.copy(detectedObjects = objects) }
    }

    /**
     * 更新系统状态 (Battery, Time, Location)
     * 可以在 Agent 思考前主动调用一次以确保数据最新
     */
    fun refreshSystemContext() {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging // 需要 API 23+, 假设 minSdk >= 24

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentTime = sdf.format(Date())
        
        // Location (暂且 Mock，后续接入 FusedLocationProvider)
        val location = "模拟位置: 北纬 39.9, 东经 116.4"

        _snapshot.update {
            it.copy(
                batteryLevel = level,
                isCharging = isCharging,
                currentTime = currentTime,
                location = location,
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * 获取最新的上下文 Prompt
     */
    fun getContextPrompt(): String {
        // 每次获取前刷新一下系统状态
        refreshSystemContext()
        return _snapshot.value.toPromptString()
    }
}
