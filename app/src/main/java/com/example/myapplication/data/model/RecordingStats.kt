package com.example.myapplication.data.model

/**
 * 录像统计信息数据类
 * 包含当前录像的详细统计数据
 */
data class RecordingStats(
    /**
     * 是否正在录像
     */
    val isRecording: Boolean = false,

    /**
     * 录像持续时长（秒）
     */
    val durationSeconds: Int = 0,

    /**
     * 当前文件大小（MB）
     */
    val fileSizeMB: Float = 0f,

    /**
     * 当前录像文件路径
     */
    val currentFilePath: String? = null,

    /**
     * 已录制的总段数
     */
    val totalSegments: Int = 0
)
