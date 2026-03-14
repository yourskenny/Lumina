package com.example.myapplication.domain.service

import android.content.Context
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 高德定位服务封装
 */
class LocationService(private val context: Context) {

    private var locationClient: AMapLocationClient? = null

    /**
     * 获取单次定位结果
     */
    suspend fun getCurrentLocation(): AMapLocation = suspendCancellableCoroutine { continuation ->
        try {
            // 初始化定位客户端
            locationClient = AMapLocationClient(context)
            
            // 设置定位参数
            val option = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                isOnceLocation = true // 单次定位
                isNeedAddress = true  // 返回地址信息
                httpTimeOut = 5000    // 超时时间
            }
            
            locationClient?.setLocationOption(option)
            
            // 设置定位监听
            locationClient?.setLocationListener(object : AMapLocationListener {
                override fun onLocationChanged(location: AMapLocation?) {
                    if (location != null) {
                        if (location.errorCode == 0) {
                            if (continuation.isActive) {
                                continuation.resume(location)
                            }
                        } else {
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    Exception("定位失败: ${location.errorCode} - ${location.errorInfo}")
                                )
                            }
                        }
                    } else {
                        if (continuation.isActive) {
                            continuation.resumeWithException(Exception("定位结果为空"))
                        }
                    }
                    stopLocation()
                }
            })
            
            // 启动定位
            locationClient?.startLocation()
            
            // 协程取消时停止定位
            continuation.invokeOnCancellation {
                stopLocation()
            }
            
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }
    
    private fun stopLocation() {
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        locationClient = null
    }
}
