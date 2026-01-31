package com.example.myapplication.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 存储管理仓库
 * 负责智能存储管理，包括自动清理、空间监控等
 */
class StorageManagementRepository(private val context: Context) {

    private val TAG = "StorageManagementRepository"

    /**
     * 存储统计信息
     */
    data class StorageStats(
        val totalVideoCount: Int,
        val totalSizeMB: Long,
        val availableSpaceMB: Long,
        val oldestVideoAgeMinutes: Long
    )

    /**
     * 清理策略配置
     */
    data class CleanupPolicy(
        val maxAgeMinutes: Int = 60,           // 最大保留时间（默认60分钟）
        val maxVideoCount: Int = 20,           // 最大视频数量（默认20个）
        val maxTotalSizeMB: Long = 1024,       // 最大总大小（默认1GB）
        val minFreeSpaceMB: Long = 500,        // 最小剩余空间（默认500MB）
        val enableAutoCleanup: Boolean = true  // 是否启用自动清理
    )

    /**
     * 获取存储统计信息
     */
    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        var totalCount = 0
        var totalSize = 0L
        var oldestTimestamp = System.currentTimeMillis()

        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.RELATIVE_PATH
            )

            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
            } else {
                "${MediaStore.Video.Media.DATA} LIKE ?"
            }

            val selectionArgs = arrayOf("%AccessibleCamera%")

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    totalCount++
                    totalSize += cursor.getLong(sizeColumn)

                    val timestamp = cursor.getLong(dateColumn) * 1000L
                    if (timestamp < oldestTimestamp) {
                        oldestTimestamp = timestamp
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取存储统计失败: ${e.message}", e)
        }

        val availableSpace = getAvailableStorageMB()
        val oldestAgeMinutes = (System.currentTimeMillis() - oldestTimestamp) / (60 * 1000)

        StorageStats(
            totalVideoCount = totalCount,
            totalSizeMB = totalSize / (1024 * 1024),
            availableSpaceMB = availableSpace,
            oldestVideoAgeMinutes = oldestAgeMinutes
        )
    }

    /**
     * 根据策略执行智能清理
     * @param policy 清理策略
     * @return 清理结果（删除的文件数量）
     */
    suspend fun performSmartCleanup(policy: CleanupPolicy): Int = withContext(Dispatchers.IO) {
        if (!policy.enableAutoCleanup) {
            Log.d(TAG, "自动清理已禁用")
            return@withContext 0
        }

        var deletedCount = 0
        val stats = getStorageStats()

        Log.d(TAG, "开始智能清理 - 当前状态: $stats")

        // 策略1: 删除超过最大年龄的视频
        if (stats.oldestVideoAgeMinutes >= policy.maxAgeMinutes) {
            val deleted = deleteOldVideos(policy.maxAgeMinutes)
            deletedCount += deleted
            Log.d(TAG, "按年龄清理: 删除 $deleted 个文件")
        }

        // 策略2: 如果视频数量超标，删除最旧的
        val updatedStats = getStorageStats()
        if (updatedStats.totalVideoCount > policy.maxVideoCount) {
            val toDelete = updatedStats.totalVideoCount - policy.maxVideoCount
            val deleted = deleteOldestVideos(toDelete)
            deletedCount += deleted
            Log.d(TAG, "按数量清理: 删除 $deleted 个文件")
        }

        // 策略3: 如果总大小超标，删除最旧的直到满足条件
        val finalStats = getStorageStats()
        if (finalStats.totalSizeMB > policy.maxTotalSizeMB) {
            val deleted = deleteVideosUntilSizeLimit(policy.maxTotalSizeMB)
            deletedCount += deleted
            Log.d(TAG, "按大小清理: 删除 $deleted 个文件")
        }

        // 策略4: 如果剩余空间不足，清理最旧的视频
        val lastStats = getStorageStats()
        if (lastStats.availableSpaceMB < policy.minFreeSpaceMB) {
            val deleted = deleteVideosToFreeSpace(policy.minFreeSpaceMB)
            deletedCount += deleted
            Log.d(TAG, "按空间清理: 删除 $deleted 个文件")
        }

        Log.d(TAG, "智能清理完成 - 总共删除 $deletedCount 个文件")
        deletedCount
    }

    /**
     * 删除超过指定年龄的视频
     */
    private fun deleteOldVideos(maxAgeMinutes: Int): Int {
        var deletedCount = 0
        val cutoffTime = System.currentTimeMillis() - (maxAgeMinutes * 60 * 1000L)

        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.RELATIVE_PATH
            )

            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ? AND ${MediaStore.Video.Media.DATE_ADDED} < ?"
            } else {
                "${MediaStore.Video.Media.DATA} LIKE ? AND ${MediaStore.Video.Media.DATE_ADDED} < ?"
            }

            val selectionArgs = arrayOf(
                "%AccessibleCamera%",
                (cutoffTime / 1000).toString()
            )

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    try {
                        context.contentResolver.delete(uri, null, null)
                        deletedCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "删除视频失败: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "查询旧视频失败: ${e.message}")
        }

        return deletedCount
    }

    /**
     * 删除最旧的N个视频
     */
    private fun deleteOldestVideos(count: Int): Int {
        var deletedCount = 0

        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.RELATIVE_PATH
            )

            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
            } else {
                "${MediaStore.Video.Media.DATA} LIKE ?"
            }

            val selectionArgs = arrayOf("%AccessibleCamera%")
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} ASC LIMIT $count"

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    try {
                        context.contentResolver.delete(uri, null, null)
                        deletedCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "删除视频失败: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "查询最旧视频失败: ${e.message}")
        }

        return deletedCount
    }

    /**
     * 删除视频直到总大小低于限制
     */
    private suspend fun deleteVideosUntilSizeLimit(maxSizeMB: Long): Int {
        var deletedCount = 0
        var currentSize = getStorageStats().totalSizeMB

        while (currentSize > maxSizeMB) {
            val deleted = deleteOldestVideos(1)
            if (deleted == 0) break // 没有更多可删除的文件

            deletedCount += deleted
            currentSize = getStorageStats().totalSizeMB
        }

        return deletedCount
    }

    /**
     * 删除视频以释放空间
     */
    private suspend fun deleteVideosToFreeSpace(targetFreeMB: Long): Int {
        var deletedCount = 0
        var currentFree = getStorageStats().availableSpaceMB

        while (currentFree < targetFreeMB) {
            val deleted = deleteOldestVideos(1)
            if (deleted == 0) break // 没有更多可删除的文件

            deletedCount += deleted
            currentFree = getStorageStats().availableSpaceMB
        }

        return deletedCount
    }

    /**
     * 获取可用存储空间（MB）
     */
    private fun getAvailableStorageMB(): Long {
        return try {
            val statFs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.os.StatFs(context.getExternalFilesDir(null)?.absolutePath ?: "")
            } else {
                android.os.StatFs(android.os.Environment.getExternalStorageDirectory().absolutePath)
            }

            statFs.availableBytes / (1024 * 1024)
        } catch (e: Exception) {
            Log.e(TAG, "获取可用空间失败: ${e.message}")
            0L
        }
    }

    /**
     * 检查是否需要清理
     * @param policy 清理策略
     * @return 是否需要清理
     */
    suspend fun needsCleanup(policy: CleanupPolicy): Boolean {
        val stats = getStorageStats()

        return stats.totalVideoCount > policy.maxVideoCount ||
                stats.totalSizeMB > policy.maxTotalSizeMB ||
                stats.availableSpaceMB < policy.minFreeSpaceMB ||
                stats.oldestVideoAgeMinutes >= policy.maxAgeMinutes
    }
}
