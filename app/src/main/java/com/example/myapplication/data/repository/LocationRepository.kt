package com.example.myapplication.data.repository

import android.content.Context
import android.util.Log
import com.example.myapplication.domain.service.LocationData
import com.example.myapplication.domain.service.LocationService
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * 位置数据仓库
 * 管理位置数据的获取、存储和访问
 */
class LocationRepository(context: Context) {

    private val TAG = "LocationRepository"

    private val locationService = LocationService(context)

    // 位置历史记录（最多保存最近100个位置）
    private val locationHistory = mutableListOf<LocationData>()
    private val maxHistorySize = 100

    /**
     * 当前位置状态流
     */
    val currentLocation: StateFlow<LocationData?> = locationService.currentLocation

    /**
     * 定位是否启用
     */
    val isLocationEnabled: StateFlow<Boolean> = locationService.isLocationEnabled

    init {
        Log.d(TAG, "LocationRepository初始化")
    }

    /**
     * 检查是否有定位权限
     */
    fun hasLocationPermission(): Boolean {
        return locationService.hasLocationPermission()
    }

    /**
     * 检查GPS是否启用
     */
    fun isGpsEnabled(): Boolean {
        return locationService.isGpsEnabled()
    }

    /**
     * 开始位置跟踪
     */
    fun startTracking() {
        Log.d(TAG, "开始位置跟踪")
        locationService.startLocationUpdates()
    }

    /**
     * 停止位置跟踪
     */
    fun stopTracking() {
        Log.d(TAG, "停止位置跟踪")
        locationService.stopLocationUpdates()
    }

    /**
     * 获取当前位置
     */
    fun getCurrentLocation(): LocationData? {
        return locationService.getCurrentLocation()
    }

    /**
     * 获取位置描述（用于语音播报）
     */
    fun getLocationDescription(): String {
        return locationService.getLocationDescription()
    }

    /**
     * 记录位置到历史
     */
    fun recordLocation(location: LocationData?) {
        if (location != null && location.isValid()) {
            locationHistory.add(location)

            // 限制历史记录大小
            if (locationHistory.size > maxHistorySize) {
                locationHistory.removeAt(0)
            }

            Log.d(TAG, "位置已记录: ${location.getShortDescription()}")
        }
    }

    /**
     * 获取位置历史记录
     */
    fun getLocationHistory(): List<LocationData> {
        return locationHistory.toList()
    }

    /**
     * 清空位置历史
     */
    fun clearHistory() {
        locationHistory.clear()
        Log.d(TAG, "位置历史已清空")
    }

    /**
     * 为视频文件生成包含GPS信息的文件名
     */
    fun generateVideoFileNameWithLocation(): String {
        val location = getCurrentLocation()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())

        return if (location != null && location.isValid()) {
            val latStr = String.format("%.4f", location.latitude).replace(".", "_")
            val lonStr = String.format("%.4f", location.longitude).replace(".", "_")
            "VID_${timestamp}_${latStr}N_${lonStr}E.mp4"
        } else {
            "VID_${timestamp}.mp4"
        }
    }

    /**
     * 为照片文件生成包含GPS信息的文件名
     */
    fun generatePhotoFileNameWithLocation(): String {
        val location = getCurrentLocation()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())

        return if (location != null && location.isValid()) {
            val latStr = String.format("%.4f", location.latitude).replace(".", "_")
            val lonStr = String.format("%.4f", location.longitude).replace(".", "_")
            "IMG_${timestamp}_${latStr}N_${lonStr}E.jpg"
        } else {
            "IMG_${timestamp}.jpg"
        }
    }

    /**
     * 获取位置元数据字符串（用于写入视频元数据）
     */
    fun getLocationMetadata(): String? {
        val location = getCurrentLocation()
        return if (location != null && location.isValid()) {
            location.toMetadataString()
        } else {
            null
        }
    }

    /**
     * 获取人类可读的位置信息
     */
    fun getReadableLocationInfo(): String {
        val location = getCurrentLocation()
        return if (location != null && location.isValid()) {
            buildString {
                append("GPS位置信息:\n")
                append("纬度: ${String.format("%.6f", location.latitude)}°\n")
                append("经度: ${String.format("%.6f", location.longitude)}°\n")
                append("海拔: ${String.format("%.1f", location.altitude)}米\n")
                append("精度: ${String.format("%.1f", location.accuracy)}米\n")
                if (location.speed > 0) {
                    append("速度: ${String.format("%.1f", location.speed * 3.6)}公里/小时\n")
                }
                val date = Date(location.timestamp)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                append("时间: ${dateFormat.format(date)}\n")
                append("来源: ${location.provider}")
            }
        } else {
            "GPS位置信息不可用"
        }
    }

    /**
     * 获取位置统计信息
     */
    fun getLocationStats(): LocationStats {
        return LocationStats(
            isEnabled = isGpsEnabled(),
            hasPermission = hasLocationPermission(),
            isTracking = locationService.currentLocation.value != null,
            currentLocation = getCurrentLocation(),
            historyCount = locationHistory.size
        )
    }

    /**
     * 释放资源
     */
    fun release() {
        stopTracking()
        clearHistory()
        locationService.release()
        Log.d(TAG, "LocationRepository已释放")
    }
}

/**
 * 位置统计信息
 */
data class LocationStats(
    val isEnabled: Boolean,
    val hasPermission: Boolean,
    val isTracking: Boolean,
    val currentLocation: LocationData?,
    val historyCount: Int
) {
    fun toDescription(): String {
        return buildString {
            append("GPS状态: ${if (isEnabled) "已启用" else "未启用"}\n")
            append("定位权限: ${if (hasPermission) "已授予" else "未授予"}\n")
            append("正在跟踪: ${if (isTracking) "是" else "否"}\n")
            append("当前位置: ${if (currentLocation?.isValid() == true) "有效" else "无效"}\n")
            append("历史记录: $historyCount 条")
        }
    }
}
