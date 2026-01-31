package com.example.myapplication.util

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 麦克风测试工具
 * 用于诊断麦克风是否正常工作
 */
class MicrophoneTest(private val context: Context) {

    private val TAG = "MicrophoneTest"

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    /**
     * 测试麦克风是否可用
     */
    fun testMicrophone(onResult: (Boolean, String) -> Unit) {
        try {
            // 检查buffer大小
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                onResult(false, "无法获取音频缓冲区大小")
                return
            }

            Log.d(TAG, "AudioRecord buffer size: $bufferSize")

            // 创建AudioRecord
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            // 检查状态
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onResult(false, "AudioRecord初始化失败")
                return
            }

            Log.d(TAG, "AudioRecord初始化成功")

            // 开始录音
            audioRecord?.startRecording()
            isRecording = true

            Log.d(TAG, "开始录音测试...")

            // 读取音频数据
            val buffer = ShortArray(bufferSize)
            var hasSound = false
            var maxAmplitude = 0

            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                var iterations = 0
                while (isActive && iterations < 50) { // 测试5秒
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                    if (readSize > 0) {
                        // 计算音量
                        var sum = 0L
                        for (i in 0 until readSize) {
                            sum += abs(buffer[i].toInt())
                        }
                        val amplitude = (sum / readSize).toInt()

                        if (amplitude > maxAmplitude) {
                            maxAmplitude = amplitude
                        }

                        if (amplitude > 100) { // 检测到声音
                            hasSound = true
                            Log.d(TAG, "检测到声音! 音量: $amplitude")
                        }

                        Log.d(TAG, "读取音频数据: $readSize 字节, 音量: $amplitude")
                    } else {
                        Log.e(TAG, "读取音频数据失败: $readSize")
                    }

                    iterations++
                    delay(100)
                }

                // 测试完成
                stopRecording()

                val result = if (maxAmplitude > 100) {
                    onResult(true, "麦克风工作正常! 最大音量: $maxAmplitude")
                } else if (maxAmplitude > 0) {
                    onResult(false, "麦克风有信号但音量太低: $maxAmplitude (需要 >100)")
                } else {
                    onResult(false, "麦克风无信号，音量为0")
                }
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "权限错误", e)
            onResult(false, "没有麦克风权限: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "麦克风测试失败", e)
            onResult(false, "测试失败: ${e.message}")
        }
    }

    /**
     * 停止录音
     */
    fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d(TAG, "AudioRecord已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放AudioRecord失败", e)
        }
    }

    /**
     * 获取麦克风状态信息
     */
    fun getMicrophoneInfo(): String {
        val sb = StringBuilder()
        sb.append("=== 麦克风配置信息 ===\n")
        sb.append("采样率: $sampleRate Hz\n")
        sb.append("缓冲区大小: $bufferSize 字节\n")
        sb.append("音频格式: PCM 16位\n")
        sb.append("声道: 单声道\n")

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            sb.append("最小缓冲区: $minBufferSize 字节\n")

            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                sb.append("⚠️ 警告: 无法获取最小缓冲区大小\n")
            }
        } catch (e: Exception) {
            sb.append("错误: ${e.message}\n")
        }

        return sb.toString()
    }
}
