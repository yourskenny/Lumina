package com.example.myapplication.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.camera.video.Quality

/**
 * 设置仓库
 * 负责应用设置的存储和读取
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "accessible_camera_settings"

        // 设置键
        private const val KEY_SEGMENT_DURATION = "segment_duration"
        private const val KEY_VIDEO_QUALITY = "video_quality"
        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_AUTO_CLEANUP_ENABLED = "auto_cleanup_enabled"
        private const val KEY_CLEANUP_MAX_AGE = "cleanup_max_age"
        private const val KEY_CLEANUP_MAX_COUNT = "cleanup_max_count"
        private const val KEY_CLEANUP_MAX_SIZE = "cleanup_max_size"
        private const val KEY_EMERGENCY_CONTACT_NAME = "emergency_contact_name"
        private const val KEY_EMERGENCY_CONTACT_PHONE = "emergency_contact_phone"

        // 默认值
        const val DEFAULT_SEGMENT_DURATION = 300 // 5分钟
        const val DEFAULT_VIDEO_QUALITY = "HD"
        const val DEFAULT_TTS_SPEED = 0.9f
        const val DEFAULT_AUTO_CLEANUP_ENABLED = true
        const val DEFAULT_CLEANUP_MAX_AGE = 60 // 60分钟
        const val DEFAULT_CLEANUP_MAX_COUNT = 20
        const val DEFAULT_CLEANUP_MAX_SIZE = 1024L // 1GB (MB)
    }

    /**
     * 录像分段时长（秒）
     */
    var segmentDuration: Int
        get() = prefs.getInt(KEY_SEGMENT_DURATION, DEFAULT_SEGMENT_DURATION)
        set(value) = prefs.edit().putInt(KEY_SEGMENT_DURATION, value).apply()

    /**
     * 视频质量
     */
    var videoQuality: String
        get() = prefs.getString(KEY_VIDEO_QUALITY, DEFAULT_VIDEO_QUALITY) ?: DEFAULT_VIDEO_QUALITY
        set(value) = prefs.edit().putString(KEY_VIDEO_QUALITY, value).apply()

    /**
     * TTS语音速度
     */
    var ttsSpeed: Float
        get() = prefs.getFloat(KEY_TTS_SPEED, DEFAULT_TTS_SPEED)
        set(value) = prefs.edit().putFloat(KEY_TTS_SPEED, value).apply()

    /**
     * 是否启用自动清理
     */
    var autoCleanupEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CLEANUP_ENABLED, DEFAULT_AUTO_CLEANUP_ENABLED)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CLEANUP_ENABLED, value).apply()

    /**
     * 清理：最大保留时间（分钟）
     */
    var cleanupMaxAge: Int
        get() = prefs.getInt(KEY_CLEANUP_MAX_AGE, DEFAULT_CLEANUP_MAX_AGE)
        set(value) = prefs.edit().putInt(KEY_CLEANUP_MAX_AGE, value).apply()

    /**
     * 清理：最大视频数量
     */
    var cleanupMaxCount: Int
        get() = prefs.getInt(KEY_CLEANUP_MAX_COUNT, DEFAULT_CLEANUP_MAX_COUNT)
        set(value) = prefs.edit().putInt(KEY_CLEANUP_MAX_COUNT, value).apply()

    /**
     * 清理：最大总大小（MB）
     */
    var cleanupMaxSize: Long
        get() = prefs.getLong(KEY_CLEANUP_MAX_SIZE, DEFAULT_CLEANUP_MAX_SIZE)
        set(value) = prefs.edit().putLong(KEY_CLEANUP_MAX_SIZE, value).apply()

    /**
     * 紧急联系人姓名
     */
    var emergencyContactName: String
        get() = prefs.getString(KEY_EMERGENCY_CONTACT_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMERGENCY_CONTACT_NAME, value).apply()

    /**
     * 紧急联系人电话
     */
    var emergencyContactPhone: String
        get() = prefs.getString(KEY_EMERGENCY_CONTACT_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMERGENCY_CONTACT_PHONE, value).apply()

    /**
     * 获取视频质量枚举
     */
    fun getVideoQualityEnum(): Quality {
        return when (videoQuality) {
            "UHD" -> Quality.UHD
            "FHD" -> Quality.FHD
            "HD" -> Quality.HD
            "SD" -> Quality.SD
            else -> Quality.HD
        }
    }

    /**
     * 检查是否设置了紧急联系人
     */
    fun hasEmergencyContact(): Boolean {
        return emergencyContactPhone.isNotEmpty()
    }

    /**
     * 重置所有设置为默认值
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    /**
     * 获取分段时长描述
     */
    fun getSegmentDurationDescription(): String {
        val minutes = segmentDuration / 60
        return when {
            minutes < 1 -> "${segmentDuration}秒"
            minutes == 1 -> "1分钟"
            else -> "${minutes}分钟"
        }
    }

    /**
     * 获取清理策略描述
     */
    fun getCleanupPolicyDescription(): String {
        return if (autoCleanupEnabled) {
            "保留${cleanupMaxAge}分钟内的${cleanupMaxCount}个视频，最多${cleanupMaxSize}MB"
        } else {
            "已禁用自动清理"
        }
    }
}
