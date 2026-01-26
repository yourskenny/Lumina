package com.example.myapplication.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * 权限检查工具类
 */
object PermissionUtils {

    /**
     * 检查相机权限是否已授予
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查录音权限是否已授予
     */
    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查所有必要权限是否已授予
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasCameraPermission(context) && hasAudioPermission(context)
    }

    /**
     * 获取所有必要的权限列表
     */
    fun getRequiredPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}
