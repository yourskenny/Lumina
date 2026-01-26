package com.example.myapplication.domain.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 语音命令枚举
 */
enum class VoiceCommand {
    CAPTURE_PHOTO,      // 拍照
    PAUSE_RECORDING,    // 暂停录像
    RESUME_RECORDING,   // 继续录像
    CLEAR_RECORDINGS,   // 清空录像
    CLOSE_APP,          // 关闭应用
    UNKNOWN             // 未识别的命令
}

/**
 * 语音识别服务
 * 提供连续语音识别功能,识别特定的语音命令
 */
class VoiceRecognitionService(
    private val context: Context,
    private val onCommandDetected: (VoiceCommand) -> Unit
) {

    private val TAG = "VoiceRecognitionService"

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var shouldRestart = true // 是否自动重启监听

    private val recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINESE.toString())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    init {
        initializeSpeechRecognizer()
    }

    /**
     * 初始化语音识别器
     */
    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "设备不支持语音识别")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "准备好接收语音")
                    isListening = true
                }

                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "开始说话")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // 音量变化,可用于显示音量指示器
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // 接收到音频缓冲区
                }

                override fun onEndOfSpeech() {
                    Log.d(TAG, "结束说话")
                    isListening = false
                }

                override fun onError(error: Int) {
                    Log.e(TAG, "语音识别错误: $error")
                    isListening = false

                    // 自动重启监听
                    if (shouldRestart) {
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(1000) // 等待1秒后重启
                            if (shouldRestart) {
                                startListening()
                            }
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { text ->
                        Log.d(TAG, "识别结果: $text")
                        processCommand(text)
                    }

                    // 自动重启监听
                    if (shouldRestart) {
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(500)
                            if (shouldRestart) {
                                startListening()
                            }
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    // 部分识别结果
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { text ->
                        Log.d(TAG, "部分结果: $text")
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    // 其他事件
                }
            })
        }
    }

    /**
     * 处理语音命令
     */
    private fun processCommand(text: String) {
        val lowerText = text.lowercase()

        val command = when {
            lowerText.contains("拍照") || lowerText.contains("拍一张") || lowerText.contains("照相") -> {
                VoiceCommand.CAPTURE_PHOTO
            }
            lowerText.contains("暂停") && (lowerText.contains("录像") || lowerText.contains("录制")) -> {
                VoiceCommand.PAUSE_RECORDING
            }
            (lowerText.contains("继续") || lowerText.contains("开始") || lowerText.contains("恢复")) &&
                    (lowerText.contains("录像") || lowerText.contains("录制")) -> {
                VoiceCommand.RESUME_RECORDING
            }
            (lowerText.contains("清空") || lowerText.contains("删除") || lowerText.contains("清理")) &&
                    (lowerText.contains("录像") || lowerText.contains("视频") || lowerText.contains("缓存")) -> {
                VoiceCommand.CLEAR_RECORDINGS
            }
            lowerText.contains("停止应用") || lowerText.contains("关闭应用") || lowerText.contains("退出") -> {
                VoiceCommand.CLOSE_APP
            }
            else -> {
                VoiceCommand.UNKNOWN
            }
        }

        if (command != VoiceCommand.UNKNOWN) {
            Log.d(TAG, "检测到命令: $command")
            onCommandDetected(command)
        }
    }

    /**
     * 开始监听
     */
    fun startListening() {
        if (isListening) {
            Log.d(TAG, "已在监听中")
            return
        }

        shouldRestart = true
        speechRecognizer?.startListening(recognitionIntent)
        Log.d(TAG, "开始监听语音命令")
    }

    /**
     * 停止监听
     */
    fun stopListening() {
        shouldRestart = false
        isListening = false
        speechRecognizer?.stopListening()
        Log.d(TAG, "停止监听语音命令")
    }

    /**
     * 释放资源
     */
    fun release() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        Log.d(TAG, "语音识别资源已释放")
    }

    /**
     * 检查是否正在监听
     */
    fun isListening(): Boolean {
        return isListening
    }
}
