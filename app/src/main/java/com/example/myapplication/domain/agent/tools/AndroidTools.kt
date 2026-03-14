package com.example.myapplication.domain.agent.tools

import android.content.Context
import android.os.BatteryManager
import com.example.myapplication.domain.feedback.FeedbackArbiter
import com.example.myapplication.domain.feedback.FeedbackPriority
import com.example.myapplication.domain.feedback.VibrationType
import dev.langchain4j.agent.tool.Tool
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.myapplication.data.repository.AMapRepository
import com.example.myapplication.domain.service.LocationService
import kotlinx.coroutines.runBlocking

/**
 * Android系统工具集
 * 提供给LLM调用的本地能力
 */
class AndroidTools(
    private val context: Context,
    private val feedbackArbiter: FeedbackArbiter,
    private val locationService: LocationService,
    private val amapRepository: AMapRepository
) {

    @Tool("获取当前时间")
    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    @Tool("获取电池电量")
    fun getBatteryLevel(): String {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return "$level%"
    }

    @Tool("获取当前位置")
    fun getLocation(): String {
        return try {
            val location = runBlocking { locationService.getCurrentLocation() }
            val address = location.address ?: "${location.latitude},${location.longitude}"
            "当前位置: $address (${location.latitude}, ${location.longitude})"
        } catch (e: Exception) {
            "无法获取位置: ${e.message}"
        }
    }
    
    @Tool("搜索附近地点")
    fun searchNearby(keyword: String): String {
        return try {
            val location = runBlocking { locationService.getCurrentLocation() }
            val locationStr = "${location.longitude},${location.latitude}"
            val result = runBlocking { amapRepository.searchNearby(keyword, locationStr) }
            "搜索结果:\n$result"
        } catch (e: Exception) {
            "搜索失败: ${e.message}"
        }
    }

    @Tool("规划步行路线")
    fun planWalkingRoute(destination: String): String {
        // Destination needs to be lat,lon or keyword search first
        // For simplicity, assume user provides keyword, we search first result then plan
        return try {
            val location = runBlocking { locationService.getCurrentLocation() }
            val origin = "${location.longitude},${location.latitude}"
            
            // Search destination first
            val searchRes = runBlocking { amapRepository.searchPOI(destination, location.city ?: "") }
            // Extract first POI logic is complex here without JSON parsing again.
            // Let's assume searchPOI returns structured data or we parse it.
            // But AMapRepository returns String.
            // For now, let's just return "Not implemented: destination search required" or
            // modify searchPOI to return coordinates.
            
            "暂不支持路线规划，请使用高德地图APP查看。"
        } catch (e: Exception) {
            "规划失败: ${e.message}"
        }
    }
    
    @Tool("震动手机")
    fun vibrate(type: String): String {
        // ... (existing code)
        // type: "success", "warning", "error"
        val vibrationType = when (type.lowercase()) {
            "warning" -> VibrationType.WARNING
            "error" -> VibrationType.ERROR
            else -> VibrationType.SUCCESS
        }
        feedbackArbiter.vibrate(vibrationType, FeedbackPriority.HIGH) // 工具调用的震动通常比较重要
        return "已执行震动: $type"
    }
    
    @Tool("语音播报")
    fun speak(text: String): String {
        feedbackArbiter.speak(text, FeedbackPriority.HIGH) // 工具调用的语音通常比较重要
        return "已播报: $text"
    }
}
