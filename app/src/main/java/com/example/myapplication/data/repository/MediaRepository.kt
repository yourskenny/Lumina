package com.example.myapplication.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 媒体存储仓库
 * 负责管理视频和照片的存储,使用MediaStore API
 */
class MediaRepository(
    private val context: Context,
    private var locationRepository: LocationRepository? = null
) {

    /**
     * 设置LocationRepository（用于依赖注入）
     */
    fun setLocationRepository(repository: LocationRepository) {
        this.locationRepository = repository
    }

    /**
     * 创建视频输出配置
     * 使用MediaStore API将视频保存到Movies/AccessibleCamera目录
     * 包含GPS位置信息（如果可用）
     * @return MediaStoreOutputOptions配置对象
     */
    fun createVideoOutputOptions(): MediaStoreOutputOptions {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "AccessibleCamera_$timestamp.mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/AccessibleCamera")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            // 添加GPS位置信息
            locationRepository?.getCurrentLocation()?.let { location ->
                if (location.isValid()) {
                    put(MediaStore.Video.Media.LATITUDE, location.latitude)
                    put(MediaStore.Video.Media.LONGITUDE, location.longitude)
                    android.util.Log.d("MediaRepository", "视频添加GPS信息: ${location.getShortDescription()}")
                }
            }
        }

        return MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()
    }

    /**
     * 创建照片输出文件
     * 保存到公共Pictures/AccessibleCamera目录
     * 文件名包含GPS位置信息（如果可用）
     * @return File对象
     */
    fun createPhotoOutputFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        // 如果有位置信息，在文件名中包含GPS坐标
        val fileName = locationRepository?.getCurrentLocation()?.let { location ->
            if (location.isValid()) {
                val latStr = String.format("%.4f", location.latitude).replace(".", "_")
                val lonStr = String.format("%.4f", location.longitude).replace(".", "_")
                android.util.Log.d("MediaRepository", "照片添加GPS信息: ${location.getShortDescription()}")
                "AccessibleCamera_Photo_${timestamp}_${latStr}N_${lonStr}E.jpg"
            } else {
                "AccessibleCamera_Photo_$timestamp.jpg"
            }
        } ?: "AccessibleCamera_Photo_$timestamp.jpg"

        // 统一使用公共目录，方便用户在相册中查看
        val photoDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "AccessibleCamera"
        )

        if (!photoDir.exists()) {
            photoDir.mkdirs()
        }

        return File(photoDir, fileName)
    }

    /**
     * 将照片文件添加到媒体库
     * 使照片在系统相册中可见
     * @param photoFile 照片文件
     */
    fun addPhotoToMediaStore(photoFile: File) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, photoFile.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AccessibleCamera")
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                } else {
                    put(MediaStore.Images.Media.DATA, photoFile.absolutePath)
                }

                // 添加GPS位置信息
                locationRepository?.getCurrentLocation()?.let { location ->
                    if (location.isValid()) {
                        put(MediaStore.Images.Media.LATITUDE, location.latitude)
                        put(MediaStore.Images.Media.LONGITUDE, location.longitude)
                        android.util.Log.d("MediaRepository", "照片MediaStore添加GPS: ${location.getShortDescription()}")
                    }
                }
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            if (uri != null) {
                // 复制文件内容到MediaStore
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    photoFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                android.util.Log.d("MediaRepository", "照片已添加到媒体库: $uri")
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "添加照片到媒体库失败: ${e.message}", e)
        }
    }

    /**
     * 检查可用存储空间
     * @return 可用空间大小(MB)
     */
    fun getAvailableStorageMB(): Long {
        val statFs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.os.StatFs(context.getExternalFilesDir(null)?.absolutePath ?: "")
        } else {
            android.os.StatFs(Environment.getExternalStorageDirectory().absolutePath)
        }

        val availableBytes = statFs.availableBytes
        return availableBytes / (1024 * 1024) // 转换为MB
    }

    /**
     * 检查是否有足够的存储空间
     * @param requiredMB 需要的空间大小(MB)
     * @return 是否有足够空间
     */
    fun hasEnoughStorage(requiredMB: Long = 500): Boolean {
        return getAvailableStorageMB() >= requiredMB
    }

    /**
     * 生成带时间戳的文件名
     * @param prefix 文件名前缀
     * @param extension 文件扩展名
     * @return 完整的文件名
     */
    fun generateFileName(prefix: String = "AccessibleCamera", extension: String = "mp4"): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${prefix}_${timestamp}.${extension}"
    }

    /**
     * 清空所有录像文件
     * @return 删除的文件数量
     */
    fun clearAllRecordings(): Int {
        var deletedCount = 0

        // 删除MediaStore中的视频文件
        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
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
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val uri = android.content.ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    try {
                        context.contentResolver.delete(uri, null, null)
                        deletedCount++
                    } catch (e: Exception) {
                        android.util.Log.e("MediaRepository", "删除视频失败: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "查询视频失败: ${e.message}")
        }

        // 删除照片文件
        try {
            val photoDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "AccessibleCamera"
                )
            } else {
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "AccessibleCamera"
                )
            }

            if (photoDir.exists() && photoDir.isDirectory) {
                photoDir.listFiles()?.forEach { file ->
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "删除照片失败: ${e.message}")
        }

        return deletedCount
    }

    /**
     * 删除超过指定时长的旧录像
     * @param maxAgeMinutes 最大保留时间(分钟)
     * @return 删除的文件数量
     */
    fun deleteOldRecordings(maxAgeMinutes: Int = 10): Int {
        var deletedCount = 0
        val cutoffTime = System.currentTimeMillis() - (maxAgeMinutes * 60 * 1000)

        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
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
                    val uri = android.content.ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    try {
                        context.contentResolver.delete(uri, null, null)
                        deletedCount++
                    } catch (e: Exception) {
                        android.util.Log.e("MediaRepository", "删除旧视频失败: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "查询旧视频失败: ${e.message}")
        }

        return deletedCount
    }

    /**
     * 获取所有录像文件的总数
     */
    fun getRecordingsCount(): Int {
        var count = 0

        try {
            val projection = arrayOf(MediaStore.Video.Media._ID)

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
                count = cursor.count
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "统计视频失败: ${e.message}")
        }

        return count
    }
}
