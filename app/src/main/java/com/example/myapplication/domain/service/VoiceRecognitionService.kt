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
    PLAY_VIDEO,         // 播放视频
    SHARE_VIDEO,        // 分享视频
    CHECK_STORAGE,      // 查询存储空间
    SWITCH_CAMERA,      // 切换摄像头
    TOGGLE_FLASHLIGHT,  // 切换闪光灯
    CHECK_BATTERY,      // 查询电池状态
    CHECK_RECORDING_TIME, // 查询录像时长
    CHECK_LOCATION,     // 查询GPS位置
    EMERGENCY_CALL,     // 紧急呼叫
    CLOSE_APP,          // 关闭应用
    UNKNOWN             // 未识别的命令
}

/**
 * 语音识别调试信息
 */
data class VoiceDebugInfo(
    val text: String,              // 识别的文本
    val isPartial: Boolean,        // 是否为部分结果
    val volume: Float,             // 音量等级 (0-10)
    val isListening: Boolean,      // 是否正在监听
    val error: String? = null      // 错误信息
)

/**
 * 语音识别服务
 * 提供连续语音识别功能,识别特定的语音命令
 */
class VoiceRecognitionService(
    private val context: Context,
    private val onCommandDetected: (VoiceCommand) -> Unit,
    private val onDebugInfo: ((VoiceDebugInfo) -> Unit)? = null  // 调试信息回调
) {

    private val TAG = "VoiceRecognitionService"

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var shouldRestart = true // 是否自动重启监听
    private var currentVolume = 0f   // 当前音量
    private var currentLanguage = "zh-CN" // 当前语言：zh-CN(中文) 或 en-US(英文) - 默认中文
    private var languagePackError = false // 语言包是否出错
    private var fallbackToEnglish = false // 是否已降级到英文

    private fun createRecognitionIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

            // 简化配置，使用默认行为
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

            // 强制使用在线识别（离线语言包不可用）
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)

            // 不设置自定义超时，使用系统默认
            // 移除了 EXTRA_SPEECH_INPUT_* 参数，这些可能干扰默认行为

            Log.d(TAG, "创建识别Intent - 语言: $currentLanguage, 在线模式")
        }
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
                    onDebugInfo?.invoke(VoiceDebugInfo(
                        text = "准备接收语音...",
                        isPartial = false,
                        volume = 0f,
                        isListening = true
                    ))
                }

                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "开始说话")
                    onDebugInfo?.invoke(VoiceDebugInfo(
                        text = "检测到说话",
                        isPartial = false,
                        volume = currentVolume,
                        isListening = true
                    ))
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // 音量变化,可用于显示音量指示器
                    // 将分贝值转换为0-10的音量等级
                    currentVolume = ((rmsdB + 2) / 2).coerceIn(0f, 10f)
                    onDebugInfo?.invoke(VoiceDebugInfo(
                        text = "",
                        isPartial = true,
                        volume = currentVolume,
                        isListening = true
                    ))
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // 接收到音频缓冲区
                }

                override fun onEndOfSpeech() {
                    Log.d(TAG, "结束说话")
                    isListening = false
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "音频录制错误"
                        SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                        SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                        SpeechRecognizer.ERROR_NO_MATCH -> "听到声音但无法识别内容"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙碌"
                        SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时（未检测到声音）"
                        else -> "未知错误: $error"
                    }

                    // 检测是否为语言包错误（客户端错误通常表示语言包不可用）
                    if (error == SpeechRecognizer.ERROR_CLIENT && currentLanguage == "zh-CN" && !fallbackToEnglish) {
                        Log.w(TAG, "检测到中文语言包不可用，自动切换到英文")
                        fallbackToEnglish = true
                        currentLanguage = "en-US"

                        onDebugInfo?.invoke(VoiceDebugInfo(
                            text = "中文语言包不可用，已切换到英文",
                            isPartial = false,
                            volume = 0f,
                            isListening = false,
                            error = "语言包切换"
                        ))
                    }

                    Log.e(TAG, "========================================")
                    Log.e(TAG, "语音识别错误")
                    Log.e(TAG, "错误码: $error")
                    Log.e(TAG, "错误描述: $errorMessage")
                    Log.e(TAG, "当前语言: $currentLanguage")
                    Log.e(TAG, "========================================")
                    isListening = false

                    onDebugInfo?.invoke(VoiceDebugInfo(
                        text = errorMessage,
                        isPartial = false,
                        volume = 0f,
                        isListening = false,
                        error = errorMessage
                    ))

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

                        onDebugInfo?.invoke(VoiceDebugInfo(
                            text = "✓ $text",
                            isPartial = false,
                            volume = currentVolume,
                            isListening = true
                        ))

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
                        onDebugInfo?.invoke(VoiceDebugInfo(
                            text = text,
                            isPartial = true,
                            volume = currentVolume,
                            isListening = true
                        ))
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
        Log.d(TAG, "=== 开始处理命令 ===")
        Log.d(TAG, "原始文本: $text")
        val lowerText = text.lowercase()
        Log.d(TAG, "小写文本: $lowerText")

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
            (lowerText.contains("播放") || lowerText.contains("查看") || lowerText.contains("打开")) &&
                    (lowerText.contains("视频") || lowerText.contains("录像") || lowerText.contains("最新")) -> {
                VoiceCommand.PLAY_VIDEO
            }
            (lowerText.contains("分享") || lowerText.contains("发送")) &&
                    (lowerText.contains("视频") || lowerText.contains("录像") || lowerText.contains("最新")) -> {
                VoiceCommand.SHARE_VIDEO
            }
            (lowerText.contains("切换") || lowerText.contains("转换")) &&
                    (lowerText.contains("摄像头") || lowerText.contains("相机") || lowerText.contains("镜头")) -> {
                VoiceCommand.SWITCH_CAMERA
            }
            (lowerText.contains("打开") || lowerText.contains("关闭") || lowerText.contains("切换")) &&
                    (lowerText.contains("闪光灯") || lowerText.contains("手电筒") || lowerText.contains("补光")) -> {
                VoiceCommand.TOGGLE_FLASHLIGHT
            }
            (lowerText.contains("查询") || lowerText.contains("检查") || lowerText.contains("查看")) &&
                    (lowerText.contains("存储") || lowerText.contains("空间") || lowerText.contains("内存")) -> {
                VoiceCommand.CHECK_STORAGE
            }
            (lowerText.contains("查询") || lowerText.contains("检查") || lowerText.contains("查看")) &&
                    (lowerText.contains("电池") || lowerText.contains("电量") || lowerText.contains("充电")) -> {
                VoiceCommand.CHECK_BATTERY
            }
            (lowerText.contains("查询") || lowerText.contains("查看")) &&
                    (lowerText.contains("录像时长") || lowerText.contains("录了多久") || lowerText.contains("时长")) -> {
                VoiceCommand.CHECK_RECORDING_TIME
            }
            (lowerText.contains("查询") || lowerText.contains("查看") || lowerText.contains("检查")) &&
                    (lowerText.contains("位置") || lowerText.contains("定位") || lowerText.contains("GPS") ||
                            lowerText.contains("坐标") || lowerText.contains("在哪") || lowerText.contains("地点")) -> {
                VoiceCommand.CHECK_LOCATION
            }
            (lowerText.contains("紧急") || lowerText.contains("求助") || lowerText.contains("帮助")) &&
                    (lowerText.contains("呼叫") || lowerText.contains("电话") || lowerText.contains("联系")) -> {
                VoiceCommand.EMERGENCY_CALL
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
            Log.d(TAG, "✅ 检测到命令: $command")
            Log.d(TAG, "=== 命令处理完成 ===")
            onCommandDetected(command)
        } else {
            Log.w(TAG, "❌ 未匹配到任何命令")
            Log.w(TAG, "识别文本: '$text'")
            Log.w(TAG, "请检查命令词是否正确")
            Log.d(TAG, "=== 命令处理完成（未匹配） ===")
        }
    }

    /**
     * 公开的测试命令方法（用于调试）
     */
    fun testCommand(text: String): VoiceCommand {
        Log.d(TAG, "【手动测试】测试命令: $text")
        processCommand(text)

        val lowerText = text.lowercase()
        return when {
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
            (lowerText.contains("查询") || lowerText.contains("检查") || lowerText.contains("查看")) &&
                    (lowerText.contains("电池") || lowerText.contains("电量") || lowerText.contains("充电")) -> {
                VoiceCommand.CHECK_BATTERY
            }
            (lowerText.contains("查询") || lowerText.contains("检查") || lowerText.contains("查看")) &&
                    (lowerText.contains("存储") || lowerText.contains("空间") || lowerText.contains("内存")) -> {
                VoiceCommand.CHECK_STORAGE
            }
            else -> VoiceCommand.UNKNOWN
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
        val intent = createRecognitionIntent()
        speechRecognizer?.startListening(intent)
        Log.d(TAG, "开始监听语音命令 - 语言: $currentLanguage")
    }

    /**
     * 切换语言
     */
    fun switchLanguage() {
        currentLanguage = if (currentLanguage == "zh-CN") {
            "en-US"
        } else {
            "zh-CN"
        }
        Log.d(TAG, "切换语言到: $currentLanguage")

        onDebugInfo?.invoke(VoiceDebugInfo(
            text = "语言已切换: ${if (currentLanguage == "zh-CN") "中文" else "英文"}",
            isPartial = false,
            volume = 0f,
            isListening = isListening
        ))

        // 重启监听以应用新语言
        if (isListening) {
            stopListening()
            startListening()
        }
    }

    /**
     * 获取当前语言
     */
    fun getCurrentLanguage(): String {
        val langName = if (currentLanguage == "zh-CN") "中文" else "英文"
        return if (fallbackToEnglish) "$langName(自动降级)" else langName
    }

    /**
     * 极简化识别测试（用于诊断问题）
     */
    fun testSimpleRecognition(onResult: (String?) -> Unit) {
        Log.d(TAG, "======================================")
        Log.d(TAG, "开始极简化识别测试")
        Log.d(TAG, "使用最基本的配置，无额外参数")
        Log.d(TAG, "======================================")

        // 先停止主识别器，避免冲突
        speechRecognizer?.stopListening()
        shouldRestart = false

        val simpleIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // 不设置任何其他参数，使用系统默认
        }

        val testRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        testRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "[极简测试] ✓ 准备接收语音")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "[极简测试] ✓ 检测到说话开始")
            }

            override fun onRmsChanged(rmsdB: Float) {
                val vol = ((rmsdB + 2) / 2).coerceIn(0f, 10f)
                if (vol > 3) {
                    Log.d(TAG, "[极简测试] 音量: ${vol.toInt()}/10")
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "[极简测试] ✓ 说话结束，等待结果...")
            }

            override fun onError(error: Int) {
                val msg = when (error) {
                    7 -> "ERROR_NO_MATCH (7): 听到声音但无法识别"
                    6 -> "ERROR_SPEECH_TIMEOUT (6): 未检测到声音"
                    5 -> "ERROR_CLIENT (5): 客户端错误"
                    else -> "错误码: $error"
                }
                Log.e(TAG, "[极简测试] ✗ 识别失败: $msg")
                onResult(null)
                testRecognizer.destroy()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d(TAG, "[极简测试] ✓✓✓ 识别成功！")
                Log.d(TAG, "[极简测试] 结果: $matches")
                onResult(matches?.firstOrNull())
                testRecognizer.destroy()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d(TAG, "[极简测试] 部分结果: $matches")
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        Log.d(TAG, "[极简测试] 开始监听...")
        testRecognizer.startListening(simpleIntent)
    }

    /**
     * 一次性识别测试（不自动重启）
     */
    fun testRecognition(onResult: (String?) -> Unit) {
        Log.d(TAG, "开始一次性识别测试")

        // 先停止主识别器，避免冲突
        speechRecognizer?.stopListening()
        shouldRestart = false  // 不自动重启

        val intent = createRecognitionIntent()

        onDebugInfo?.invoke(VoiceDebugInfo(
            text = "开始识别测试，请说话...",
            isPartial = false,
            volume = 0f,
            isListening = true
        ))

        // 临时监听器，用于测试
        val testRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        testRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "[测试] 准备接收语音")
                onDebugInfo?.invoke(VoiceDebugInfo(
                    text = "准备接收，请说话...",
                    isPartial = false,
                    volume = 0f,
                    isListening = true
                ))
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "[测试] 检测到说话")
                onDebugInfo?.invoke(VoiceDebugInfo(
                    text = "检测到说话...",
                    isPartial = false,
                    volume = 0f,
                    isListening = true
                ))
            }

            override fun onRmsChanged(rmsdB: Float) {
                val vol = ((rmsdB + 2) / 2).coerceIn(0f, 10f)
                onDebugInfo?.invoke(VoiceDebugInfo(
                    text = "",
                    isPartial = true,
                    volume = vol,
                    isListening = true
                ))
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "[测试] 说话结束")
                onDebugInfo?.invoke(VoiceDebugInfo(
                    text = "处理中...",
                    isPartial = false,
                    volume = 0f,
                    isListening = true
                ))
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "音频错误"
                    SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                    SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                    SpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音内容"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙碌"
                    SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时（未检测到声音）"
                    else -> "未知错误: $error"
                }
                Log.e(TAG, "[测试] 识别错误: $errorMsg")
                onResult(null)
                testRecognizer.destroy()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                Log.d(TAG, "[测试] 识别结果: $text")
                Log.d(TAG, "[测试] 所有结果: $matches")
                onResult(text)
                testRecognizer.destroy()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                Log.d(TAG, "[测试] 部分结果: $text")
                if (text != null) {
                    onDebugInfo?.invoke(VoiceDebugInfo(
                        text = "部分: $text",
                        isPartial = true,
                        volume = 0f,
                        isListening = true
                    ))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        testRecognizer.startListening(intent)
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
