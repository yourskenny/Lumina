package com.example.myapplication.data.repository

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * 视频播放仓库
 * 负责视频文件的查询、播放和分享
 */
class VideoPlaybackRepository(private val context: Context) {

    private val TAG = "VideoPlaybackRepository"

    /**
     * 视频信息数据类
     */
    data class VideoInfo(
        val id: Long,
        val uri: Uri,
        val displayName: String,
        val dateAdded: Long,
        val size: Long,
        val duration: Long = 0
    )

    /**
     * 获取所有录制的视频列表
     * @return 视频信息列表，按时间倒序排列（最新的在前）
     */
    fun getAllVideos(): List<VideoInfo> {
        val videos = mutableListOf<VideoInfo>()

        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.RELATIVE_PATH
            )

            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
            } else {
                "${MediaStore.Video.Media.DATA} LIKE ?"
            }

            val selectionArgs = arrayOf("%AccessibleCamera%")

            // 按时间倒序排序
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val dateAdded = cursor.getLong(dateColumn)
                    val size = cursor.getLong(sizeColumn)
                    val duration = if (durationColumn >= 0) cursor.getLong(durationColumn) else 0L

                    val uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    videos.add(VideoInfo(id, uri, name, dateAdded, size, duration))
                }
            }

            Log.d(TAG, "找到 ${videos.size} 个视频文件")
        } catch (e: Exception) {
            Log.e(TAG, "查询视频失败: ${e.message}", e)
        }

        return videos
    }

    /**
     * 获取最新的视频
     * @return 最新的视频信息，如果没有则返回null
     */
    fun getLatestVideo(): VideoInfo? {
        val videos = getAllVideos()
        return videos.firstOrNull()
    }

    /**
     * 播放指定的视频
     * @param videoInfo 视频信息
     * @return 是否成功启动播放
     */
    fun playVideo(videoInfo: VideoInfo): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(videoInfo.uri, "video/mp4")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
            Log.d(TAG, "启动视频播放: ${videoInfo.displayName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "播放视频失败: ${e.message}", e)
            false
        }
    }

    /**
     * 播放最新的视频
     * @return 是否成功启动播放
     */
    fun playLatestVideo(): Boolean {
        val latestVideo = getLatestVideo()
        return if (latestVideo != null) {
            playVideo(latestVideo)
        } else {
            Log.d(TAG, "没有找到可播放的视频")
            false
        }
    }

    /**
     * 分享指定的视频
     * @param videoInfo 视频信息
     * @return 是否成功启动分享
     */
    fun shareVideo(videoInfo: VideoInfo): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, videoInfo.uri)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val chooserIntent = Intent.createChooser(intent, "分享视频").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(chooserIntent)
            Log.d(TAG, "启动视频分享: ${videoInfo.displayName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "分享视频失败: ${e.message}", e)
            false
        }
    }

    /**
     * 分享最新的视频
     * @return 是否成功启动分享
     */
    fun shareLatestVideo(): Boolean {
        val latestVideo = getLatestVideo()
        return if (latestVideo != null) {
            shareVideo(latestVideo)
        } else {
            Log.d(TAG, "没有找到可分享的视频")
            false
        }
    }

    /**
     * 删除指定的视频
     * @param videoInfo 视频信息
     * @return 是否成功删除
     */
    fun deleteVideo(videoInfo: VideoInfo): Boolean {
        return try {
            context.contentResolver.delete(videoInfo.uri, null, null)
            Log.d(TAG, "删除视频: ${videoInfo.displayName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "删除视频失败: ${e.message}", e)
            false
        }
    }

    /**
     * 格式化视频大小
     * @param sizeBytes 文件大小（字节）
     * @return 格式化的文件大小字符串
     */
    fun formatFileSize(sizeBytes: Long): String {
        return when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
            sizeBytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", sizeBytes / (1024f * 1024f))
            else -> String.format("%.2f GB", sizeBytes / (1024f * 1024f * 1024f))
        }
    }

    /**
     * 格式化视频时长
     * @param durationMs 时长（毫秒）
     * @return 格式化的时长字符串
     */
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> String.format("%d小时%d分钟%d秒", hours, minutes, seconds)
            minutes > 0 -> String.format("%d分钟%d秒", minutes, seconds)
            else -> String.format("%d秒", seconds)
        }
    }
}
