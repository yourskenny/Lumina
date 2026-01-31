package com.example.myapplication.domain.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 位置信息数据类
 */
data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val timestamp: Long = 0L,
    val provider: String = ""
) {
    /**
     * 获取位置的简短描述
     */
    fun getShortDescription(): String {
        return "纬度${String.format("%.4f", latitude)}, 经度${String.format("%.4f", longitude)}"
    }

    /**
     * 获取位置的详细描述（用于语音播报）
     */
    fun getVoiceDescription(): String {
        return "当前位置: 纬度${String.format("%.4f", latitude)}, " +
                "经度${String.format("%.4f", longitude)}, " +
                "精度${String.format("%.0f", accuracy)}米"
    }

    /**
     * 获取用于视频元数据的字符串
     */
    fun toMetadataString(): String {
        return "Lat:$latitude,Lon:$longitude,Alt:$altitude,Acc:$accuracy"
    }

    /**
     * 检查位置是否有效
     */
    fun isValid(): Boolean {
        return latitude != 0.0 && longitude != 0.0
    }
}

/**
 * GPS定位服务
 * 管理位置获取、更新和状态
 */
class LocationService(private val context: Context) {

    private val TAG = "LocationService"

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // 当前位置状态
    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    val currentLocation: StateFlow<LocationData?> = _currentLocation.asStateFlow()

    // 定位是否启用
    private val _isLocationEnabled = MutableStateFlow(false)
    val isLocationEnabled: StateFlow<Boolean> = _isLocationEnabled.asStateFlow()

    private var isListening = false

    // 位置监听器
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d(TAG, "位置更新: ${location.latitude}, ${location.longitude}")
            updateLocation(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.d(TAG, "定位状态变化: $provider, status=$status")
        }

        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "定位提供者启用: $provider")
            _isLocationEnabled.value = true
        }

        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "定位提供者禁用: $provider")
            _isLocationEnabled.value = isGpsEnabled()
        }
    }

    init {
        // 检查初始定位状态
        _isLocationEnabled.value = isGpsEnabled()

        // 如果有权限，获取最后已知位置
        if (hasLocationPermission()) {
            getLastKnownLocation()
        }
    }

    /**
     * 检查是否有定位权限
     */
    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    /**
     * 检查GPS是否启用
     */
    fun isGpsEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * 开始位置更新
     */
    fun startLocationUpdates() {
        if (isListening) {
            Log.d(TAG, "位置更新已在运行中")
            return
        }

        if (!hasLocationPermission()) {
            Log.e(TAG, "缺少定位权限")
            return
        }

        if (!isGpsEnabled()) {
            Log.w(TAG, "GPS未启用")
            return
        }

        try {
            // 从GPS和网络提供者请求位置更新
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    locationListener
                )
                Log.d(TAG, "GPS位置更新已启动")
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    locationListener
                )
                Log.d(TAG, "网络位置更新已启动")
            }

            isListening = true

            // 立即获取一次位置
            getLastKnownLocation()

        } catch (e: SecurityException) {
            Log.e(TAG, "位置更新启动失败: ${e.message}")
        }
    }

    /**
     * 停止位置更新
     */
    fun stopLocationUpdates() {
        if (!isListening) {
            return
        }

        try {
            locationManager.removeUpdates(locationListener)
            isListening = false
            Log.d(TAG, "位置更新已停止")
        } catch (e: SecurityException) {
            Log.e(TAG, "位置更新停止失败: ${e.message}")
        }
    }

    /**
     * 获取最后已知位置
     */
    private fun getLastKnownLocation() {
        if (!hasLocationPermission()) {
            return
        }

        try {
            // 尝试从GPS获取
            var lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            // 如果GPS没有，尝试从网络获取
            if (lastLocation == null) {
                lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            if (lastLocation != null) {
                updateLocation(lastLocation)
                Log.d(TAG, "获取到最后已知位置")
            } else {
                Log.d(TAG, "无最后已知位置")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "获取最后位置失败: ${e.message}")
        }
    }

    /**
     * 更新位置数据
     */
    private fun updateLocation(location: Location) {
        _currentLocation.value = LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracy = location.accuracy,
            speed = location.speed,
            bearing = location.bearing,
            timestamp = location.time,
            provider = location.provider ?: "unknown"
        )
    }

    /**
     * 获取当前位置（同步方法）
     */
    fun getCurrentLocation(): LocationData? {
        return _currentLocation.value
    }

    /**
     * 获取位置描述文本
     */
    fun getLocationDescription(): String {
        val location = _currentLocation.value
        return if (location != null && location.isValid()) {
            location.getVoiceDescription()
        } else {
            "位置信息不可用"
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        stopLocationUpdates()
        Log.d(TAG, "LocationService已释放")
    }

    companion object {
        // 最小时间间隔（毫秒）
        private const val MIN_TIME_BETWEEN_UPDATES = 5000L // 5秒

        // 最小距离变化（米）
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES = 10f // 10米
    }
}
