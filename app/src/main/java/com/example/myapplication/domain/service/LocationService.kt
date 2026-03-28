package com.example.myapplication.domain.service

import android.content.Context
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 高德定位服务封装
 */
class LocationService(private val context: Context) {

    private var locationClient: AMapLocationClient? = null

    private val TAG = "LocationService"

    /**
     * 获取单次定位结果
     */
    suspend fun getCurrentLocation(): AMapLocation = suspendCancellableCoroutine { continuation ->
        try {
            // 初始化定位客户端
            locationClient = AMapLocationClient(context)

            val option = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                isOnceLocation = true
                isNeedAddress = true
                httpTimeOut = 5000
            }
            
            locationClient?.setLocationOption(option)
            locationClient?.setLocationListener { location ->
                if (location != null) {
                    if (location.errorCode == 0) {
                        if (continuation.isActive) continuation.resume(location)
                    } else {
                        val error = "定位失败: ${location.errorInfo} (Code:${location.errorCode})"
                        Log.e(TAG, error)
                        if (continuation.isActive) continuation.resumeWithException(Exception(error))
                    }
                } else {
                    Log.e(TAG, "Location is NULL")
                    if (continuation.isActive) continuation.resumeWithException(Exception("定位结果为空"))
                }
                stopLocation()
            }
            locationClient?.startLocation()
            
            continuation.invokeOnCancellation { 
                stopLocation() 
            }
        } catch (e: Exception) {
            Log.e(TAG, "定位服务异常", e)
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }
    
    private fun stopLocation() {
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        locationClient = null
    }
}
