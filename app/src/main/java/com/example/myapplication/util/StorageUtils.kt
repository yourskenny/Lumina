package com.example.myapplication.util

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs

/**
 * 存储空间检查工具类
 */
object StorageUtils {

    /**
     * 获取可用存储空间(MB)
     */
    fun getAvailableStorageMB(context: Context): Long {
        val statFs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            StatFs(context.getExternalFilesDir(null)?.absolutePath ?: "")
        } else {
            @Suppress("DEPRECATION")
            StatFs(Environment.getExternalStorageDirectory().absolutePath)
        }

        val availableBytes = statFs.availableBytes
        return availableBytes / (1024 * 1024) // 转换为MB
    }

    /**
     * 获取可用存储空间(GB)
     */
    fun getAvailableStorageGB(context: Context): Float {
        return getAvailableStorageMB(context) / 1024f
    }

    /**
     * 检查是否有足够的存储空间
     * @param requiredMB 需要的空间大小(MB)
     */
    fun hasEnoughStorage(context: Context, requiredMB: Long = 500): Boolean {
        return getAvailableStorageMB(context) >= requiredMB
    }

    /**
     * 格式化存储空间大小
     * @param bytes 字节数
     * @return 格式化后的字符串(如 "1.5 GB", "500 MB")
     */
    fun formatStorageSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
    }
}
