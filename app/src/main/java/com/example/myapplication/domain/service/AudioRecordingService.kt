package com.example.myapplication.domain.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 音频录制服务
 * 用于在语音识别时同步录制音频，方便调试和核对
 */
class AudioRecordingService(
    private val context: Context
) {
    private val TAG = "AudioRecordingService"

    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var isRecording = false

    /**
     * 开始录音
     * @return 录音文件路径，如果失败返回null
     */
    fun startRecording(): String? {
        if (isRecording) {
            Log.w(TAG, "已经在录音中")
            return currentRecordingFile?.absolutePath
        }

        try {
            // 创建录音文件
            val audioDir = File(context.getExternalFilesDir(null), "VoiceRecordings")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            currentRecordingFile = File(audioDir, "voice_${timestamp}.m4a")

            // 初始化MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(128000)
                setOutputFile(currentRecordingFile?.absolutePath)

                prepare()
                start()

                isRecording = true
                Log.d(TAG, "开始录音: ${currentRecordingFile?.absolutePath}")
            }

            return currentRecordingFile?.absolutePath

        } catch (e: Exception) {
            Log.e(TAG, "启动录音失败", e)
            releaseRecorder()
            return null
        }
    }

    /**
     * 停止录音
     * @return 录音文件路径，如果失败返回null
     */
    fun stopRecording(): String? {
        if (!isRecording) {
            Log.w(TAG, "当前没有在录音")
            return null
        }

        try {
            mediaRecorder?.apply {
                stop()
                Log.d(TAG, "停止录音: ${currentRecordingFile?.absolutePath}")
            }

            val filePath = currentRecordingFile?.absolutePath
            isRecording = false
            releaseRecorder()

            return filePath

        } catch (e: Exception) {
            Log.e(TAG, "停止录音失败", e)
            releaseRecorder()
            return null
        }
    }

    /**
     * 释放录音器资源
     */
    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "释放MediaRecorder失败", e)
        } finally {
            mediaRecorder = null
            isRecording = false
        }
    }

    /**
     * 获取当前录音文件路径
     */
    fun getCurrentRecordingFile(): String? {
        return currentRecordingFile?.absolutePath
    }

    /**
     * 检查是否正在录音
     */
    fun isRecording(): Boolean {
        return isRecording
    }

    /**
     * 获取所有录音文件列表
     */
    fun getAllRecordings(): List<File> {
        val audioDir = File(context.getExternalFilesDir(null), "VoiceRecordings")
        if (!audioDir.exists()) {
            return emptyList()
        }

        return audioDir.listFiles()?.filter { it.extension == "m4a" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * 删除指定的录音文件
     */
    fun deleteRecording(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除录音文件失败", e)
            false
        }
    }

    /**
     * 清空所有录音文件
     */
    fun clearAllRecordings(): Int {
        val recordings = getAllRecordings()
        var deletedCount = 0

        recordings.forEach { file ->
            if (file.delete()) {
                deletedCount++
            }
        }

        Log.d(TAG, "清空录音文件: $deletedCount/${recordings.size}")
        return deletedCount
    }

    /**
     * 释放服务资源
     */
    fun release() {
        if (isRecording) {
            stopRecording()
        }
        releaseRecorder()
        Log.d(TAG, "AudioRecordingService已释放")
    }
}
