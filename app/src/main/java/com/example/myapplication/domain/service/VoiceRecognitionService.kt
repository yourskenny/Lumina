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
    private var testRecognizer: SpeechRecognizer? = null // 测试用识别器
    private var isTesting = false // 是否正在测试
    private var consecutiveErrors = 0 // 连续错误计数
    private val MAX_CONSECUTIVE_ERRORS = 5 // 最大连续错误次数

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
                    consecutiveErrors = 0 // 成功启动，重置错误计数
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
                    Log.d(TAG, "结束说话 - 等待识别结果...")
                    // 不在这里设置isListening = false，让onResults或onError来设置
                    // 避免状态不一致
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
                    Log.e(TAG, "连续错误次数: $consecutiveErrors")
                    Log.e(TAG, "========================================")
                    isListening = false

                    onDebugInfo?.invoke(VoiceDebugInfo(
                        text = errorMessage,
                        isPartial = false,
                        volume = 0f,
                        isListening = false,
                        error = errorMessage
                    ))

                    // 根据错误类型决定是否增加错误计数
                    // ERROR_NO_MATCH 和 ERROR_SPEECH_TIMEOUT 不算严重错误
                    val isSeriousError = error !in listOf(
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    )

                    if (isSeriousError) {
                        consecutiveErrors++
                        Log.e(TAG, "严重错误，计数: $consecutiveErrors")
                    } else {
                        Log.d(TAG, "非严重错误（无匹配/超时），不计入错误次数")
                    }

                    // 检查是否超过最大错误次数
                    if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                        Log.e(TAG, "⚠️ 连续错误过多(${consecutiveErrors}次)，暂停自动重启10秒")
                        shouldRestart = false

                        // 10秒后重置并允许重启
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(10000)
                            consecutiveErrors = 0
                            shouldRestart = true
                            Log.d(TAG, "✓ 错误计数已重置，恢复自动重启")
                        }
                        return
                    }

                    // 自动重启监听
                    if (shouldRestart) {
                        // 根据错误类型决定延迟时间
                        val delayTime = when (error) {
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                                Log.w(TAG, "识别器忙碌，彻底重新初始化...")
                                // 先彻底清理
                                speechRecognizer?.destroy()
                                speechRecognizer = null
                                3000L // 识别器忙碌时等待3秒
                            }
                            SpeechRecognizer.ERROR_CLIENT -> {
                                2000L // 客户端错误时等待2秒
                            }
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                                200L // 无匹配/超时快速重启，保持连续监听
                            }
                            else -> 1000L // 其他错误等待1秒
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            delay(delayTime)
                            if (shouldRestart) {
                                Log.d(TAG, "🔄 尝试重新启动识别器...")
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
                            isListening = false  // 识别完成，设置为false
                        ))

                        processCommand(text)
                    }

                    // 关键修复：识别完成后，立即设置为false以允许重启
                    isListening = false

                    // 自动重启监听 - 缩短延迟以实现连续监听
                    if (shouldRestart) {
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(200)  // 减少延迟从500ms到200ms
                            if (shouldRestart) {
                                Log.d(TAG, "🔄 自动重启监听...")
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
            // 拍照 - Take photo
            lowerText.contains("拍照") || lowerText.contains("拍一张") || lowerText.contains("照相") ||
                    lowerText.contains("take photo") || lowerText.contains("take picture") ||
                    lowerText.contains("capture") || lowerText.contains("photo") -> {
                VoiceCommand.CAPTURE_PHOTO
            }
            // 暂停录像 - Pause recording
            (lowerText.contains("暂停") && (lowerText.contains("录像") || lowerText.contains("录制"))) ||
                    (lowerText.contains("pause") && (lowerText.contains("recording") || lowerText.contains("video"))) ||
                    lowerText.contains("pause recording") -> {
                VoiceCommand.PAUSE_RECORDING
            }
            // 继续录像 - Resume recording
            ((lowerText.contains("继续") || lowerText.contains("开始") || lowerText.contains("恢复")) &&
                    (lowerText.contains("录像") || lowerText.contains("录制"))) ||
                    ((lowerText.contains("resume") || lowerText.contains("continue") || lowerText.contains("start")) &&
                            (lowerText.contains("recording") || lowerText.contains("video"))) ||
                    lowerText.contains("resume recording") || lowerText.contains("continue recording") -> {
                VoiceCommand.RESUME_RECORDING
            }
            // 播放视频 - Play video
            ((lowerText.contains("播放") || lowerText.contains("查看") || lowerText.contains("打开")) &&
                    (lowerText.contains("视频") || lowerText.contains("录像") || lowerText.contains("最新"))) ||
                    (lowerText.contains("play") && (lowerText.contains("video") || lowerText.contains("latest"))) ||
                    lowerText.contains("play video") || lowerText.contains("play latest") -> {
                VoiceCommand.PLAY_VIDEO
            }
            // 分享视频 - Share video
            ((lowerText.contains("分享") || lowerText.contains("发送")) &&
                    (lowerText.contains("视频") || lowerText.contains("录像") || lowerText.contains("最新"))) ||
                    (lowerText.contains("share") && (lowerText.contains("video") || lowerText.contains("latest"))) ||
                    lowerText.contains("share video") || lowerText.contains("send video") -> {
                VoiceCommand.SHARE_VIDEO
            }
            // 切换摄像头 - Switch camera
            ((lowerText.contains("切换") || lowerText.contains("转换")) &&
                    (lowerText.contains("摄像头") || lowerText.contains("相机") || lowerText.contains("镜头"))) ||
                    (lowerText.contains("switch") && lowerText.contains("camera")) ||
                    lowerText.contains("switch camera") || lowerText.contains("flip camera") -> {
                VoiceCommand.SWITCH_CAMERA
            }
            // 切换闪光灯 - Toggle flashlight
            ((lowerText.contains("打开") || lowerText.contains("关闭") || lowerText.contains("切换")) &&
                    (lowerText.contains("闪光灯") || lowerText.contains("手电筒") || lowerText.contains("补光"))) ||
                    (lowerText.contains("flash") || lowerText.contains("flashlight") || lowerText.contains("torch")) ||
                    lowerText.contains("toggle flash") || lowerText.contains("turn on flash") ||
                    lowerText.contains("turn off flash") -> {
                VoiceCommand.TOGGLE_FLASHLIGHT
            }
            // 查询存储 - Check storage
            ((lowerText.contains("查询") || lowerText.contains("检查") || lowerText.contains("查看")) &&
                    (lowerText.contains("存储") || lowerText.contains("空间") || lowerText.contains("内存"))) ||
                    ((lowerText.contains("check") || lowerText.contains("show")) &&
                            (lowerText.contains("storage") || lowerText.contains("space") || lowerText.contains("memory"))) ||
                    lowerText.contains("check storage") || lowerText.contains("storage info") -> {
                VoiceCommand.CHECK_STORAGE
            }
            // 查询电池 - Check battery
            ((lowerText.contains("查询") || lowerText.contains("检查") || lowerText.contains("查看")) &&
                    (lowerText.contains("电池") || lowerText.contains("电量") || lowerText.contains("充电"))) ||
                    ((lowerText.contains("check") || lowerText.contains("show")) &&
                            (lowerText.contains("battery") || lowerText.contains("power"))) ||
                    lowerText.contains("check battery") || lowerText.contains("battery level") -> {
                VoiceCommand.CHECK_BATTERY
            }
            // 查询录像时长 - Check recording time
            ((lowerText.contains("查询") || lowerText.contains("查看")) &&
                    (lowerText.contains("录像时长") || lowerText.contains("录了多久") || lowerText.contains("时长"))) ||
                    ((lowerText.contains("check") || lowerText.contains("show")) &&
                            (lowerText.contains("recording time") || lowerText.contains("duration"))) ||
                    lowerText.contains("how long") || lowerText.contains("check time") -> {
                VoiceCommand.CHECK_RECORDING_TIME
            }
            // 查询位置 - Check location
            ((lowerText.contains("查询") || lowerText.contains("查看") || lowerText.contains("检查")) &&
                    (lowerText.contains("位置") || lowerText.contains("定位") || lowerText.contains("GPS") ||
                            lowerText.contains("坐标") || lowerText.contains("在哪") || lowerText.contains("地点"))) ||
                    ((lowerText.contains("check") || lowerText.contains("show") || lowerText.contains("where")) &&
                            (lowerText.contains("location") || lowerText.contains("gps") || lowerText.contains("position"))) ||
                    lowerText.contains("check location") || lowerText.contains("my location") ||
                    lowerText.contains("where am i") -> {
                VoiceCommand.CHECK_LOCATION
            }
            // 紧急呼叫 - Emergency call
            ((lowerText.contains("紧急") || lowerText.contains("求助") || lowerText.contains("帮助")) &&
                    (lowerText.contains("呼叫") || lowerText.contains("电话") || lowerText.contains("联系"))) ||
                    (lowerText.contains("emergency") && (lowerText.contains("call") || lowerText.contains("help"))) ||
                    lowerText.contains("emergency call") || lowerText.contains("call help") ||
                    lowerText.contains("help me") || lowerText.contains("sos") -> {
                VoiceCommand.EMERGENCY_CALL
            }
            // 清空录像 - Clear recordings
            ((lowerText.contains("清空") || lowerText.contains("删除") || lowerText.contains("清理")) &&
                    (lowerText.contains("录像") || lowerText.contains("视频") || lowerText.contains("缓存"))) ||
                    ((lowerText.contains("clear") || lowerText.contains("delete") || lowerText.contains("remove")) &&
                            (lowerText.contains("recording") || lowerText.contains("video") || lowerText.contains("cache"))) ||
                    lowerText.contains("clear recordings") || lowerText.contains("delete videos") -> {
                VoiceCommand.CLEAR_RECORDINGS
            }
            // 关闭应用 - Close app
            lowerText.contains("停止应用") || lowerText.contains("关闭应用") || lowerText.contains("退出") ||
                    lowerText.contains("close app") || lowerText.contains("exit") || lowerText.contains("quit") ||
                    lowerText.contains("stop app") -> {
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
            // 拍照 - Take photo
            lowerText.contains("拍照") || lowerText.contains("拍一张") || lowerText.contains("照相") ||
                    lowerText.contains("take photo") || lowerText.contains("take picture") ||
                    lowerText.contains("capture") || lowerText.contains("photo") -> {
                VoiceCommand.CAPTURE_PHOTO
            }
            // 暂停录像 - Pause recording
            (lowerText.contains("暂停") && (lowerText.contains("录像") || lowerText.contains("录制"))) ||
                    (lowerText.contains("pause") && (lowerText.contains("recording") || lowerText.contains("video"))) ||
                    lowerText.contains("pause recording") -> {
                VoiceCommand.PAUSE_RECORDING
            }
            // 继续录像 - Resume recording
            ((lowerText.contains("继续") || lowerText.contains("开始") || lowerText.contains("恢复")) &&
                    (lowerText.contains("录像") || lowerText.contains("录制"))) ||
                    ((lowerText.contains("resume") || lowerText.contains("continue") || lowerText.contains("start")) &&
                            (lowerText.contains("recording") || lowerText.contains("video"))) ||
                    lowerText.contains("resume recording") || lowerText.contains("continue recording") -> {
                VoiceCommand.RESUME_RECORDING
            }
            // 播放视频 - Play video
            ((lowerText.contains("播放") || lowerText.contains("查看") || lowerText.contains("打开")) &&
                    (lowerText.contains("视频") || lowerText.contains("录像") || lowerText.contains("最新"))) ||
                    (lowerText.contains("play") && (lowerText.contains("video") || lowerText.contains("latest"))) ||
                    lowerText.contains("play video") || lowerText.contains("play latest") -> {
                VoiceCommand.PLAY_VIDEO
            }
            // 分享视频 - Share video
            ((lowerText.contains("分享") || lowerText.contains("发送")) &&
                    (lowerText.contains("视频") || lowerText.contains("录像") || lowerText.contains("最新"))) ||
                    (lowerText.contains("share") && (lowerText.contains("video") || lowerText.contains("latest"))) ||
                    lowerText.contains("share video") || lowerText.contains("send video") -> {
                VoiceCommand.SHARE_VIDEO
            }
            // 切换摄像头 - Switch camera
            ((lowerText.contains("切换") || lowerText.contains("转换")) &&
                    (lowerText.contains("摄像头") || lowerText.contains("相机") || lowerText.contains("镜头"))) ||
                    (lowerText.contains("switch") && lowerText.contains("camera")) ||
                    lowerText.contains("switch camera") || lowerText.contains("flip camera") -> {
                VoiceCommand.SWITCH_CAMERA
            }
            // 切换闪光灯 - Toggle flashlight
            ((lowerText.contains("打开") || lowerText.contains("关闭") || lowerText.contains("切换")) &&
                    (lowerText.contains("闪光灯") || lowerText.contains("手电筒") || lowerText.contains("补光"))) ||
                    (lowerText.contains("flash") || lowerText.contains("flashlight") || lowerText.contains("torch")) ||
                    lowerText.contains("toggle flash") || lowerText.contains("turn on flash") ||
                    lowerText.contains("turn off flash") -> {
                VoiceCommand.TOGGLE_FLASHLIGHT
            }
            // 查询存储 - Check storage
            ((lowerText.contains("查询") || lowerText.contains("检查") || lowerText.contains("查看")) &&
                    (lowerText.contains("存储") || lowerText.contains("空间") || lowerText.contains("内存"))) ||
                    ((lowerText.contains("check") || lowerText.contains("show")) &&
                            (lowerText.contains("storage") || lowerText.contains("space") || lowerText.contains("memory"))) ||
                    lowerText.contains("check storage") || lowerText.contains("storage info") -> {
                VoiceCommand.CHECK_STORAGE
            }
            // 查询电池 - Check battery
            (lowerText.contains("查询") || lowerText.contains("检查") || lowerText.contains("查看")) &&
                    (lowerText.contains("电池") || lowerText.contains("电量") || lowerText.contains("充电")) ||
                    ((lowerText.contains("check") || lowerText.contains("show")) &&
                            (lowerText.contains("battery") || lowerText.contains("power"))) ||
                    lowerText.contains("check battery") || lowerText.contains("battery level") -> {
                VoiceCommand.CHECK_BATTERY
            }
            // 查询录像时长 - Check recording time
            ((lowerText.contains("查询") || lowerText.contains("查看")) &&
                    (lowerText.contains("录像时长") || lowerText.contains("录了多久") || lowerText.contains("时长"))) ||
                    ((lowerText.contains("check") || lowerText.contains("show")) &&
                            (lowerText.contains("recording time") || lowerText.contains("duration"))) ||
                    lowerText.contains("how long") || lowerText.contains("check time") -> {
                VoiceCommand.CHECK_RECORDING_TIME
            }
            // 查询位置 - Check location
            ((lowerText.contains("查询") || lowerText.contains("查看") || lowerText.contains("检查")) &&
                    (lowerText.contains("位置") || lowerText.contains("定位") || lowerText.contains("GPS") ||
                            lowerText.contains("坐标") || lowerText.contains("在哪") || lowerText.contains("地点"))) ||
                    ((lowerText.contains("check") || lowerText.contains("show") || lowerText.contains("where")) &&
                            (lowerText.contains("location") || lowerText.contains("gps") || lowerText.contains("position"))) ||
                    lowerText.contains("check location") || lowerText.contains("my location") ||
                    lowerText.contains("where am i") -> {
                VoiceCommand.CHECK_LOCATION
            }
            // 紧急呼叫 - Emergency call
            ((lowerText.contains("紧急") || lowerText.contains("求助") || lowerText.contains("帮助")) &&
                    (lowerText.contains("呼叫") || lowerText.contains("电话") || lowerText.contains("联系"))) ||
                    (lowerText.contains("emergency") && (lowerText.contains("call") || lowerText.contains("help"))) ||
                    lowerText.contains("emergency call") || lowerText.contains("call help") ||
                    lowerText.contains("help me") || lowerText.contains("sos") -> {
                VoiceCommand.EMERGENCY_CALL
            }
            // 清空录像 - Clear recordings
            ((lowerText.contains("清空") || lowerText.contains("删除") || lowerText.contains("清理")) &&
                    (lowerText.contains("录像") || lowerText.contains("视频") || lowerText.contains("缓存"))) ||
                    ((lowerText.contains("clear") || lowerText.contains("delete") || lowerText.contains("remove")) &&
                            (lowerText.contains("recording") || lowerText.contains("video") || lowerText.contains("cache"))) ||
                    lowerText.contains("clear recordings") || lowerText.contains("delete videos") -> {
                VoiceCommand.CLEAR_RECORDINGS
            }
            // 关闭应用 - Close app
            lowerText.contains("停止应用") || lowerText.contains("关闭应用") || lowerText.contains("退出") ||
                    lowerText.contains("close app") || lowerText.contains("exit") || lowerText.contains("quit") ||
                    lowerText.contains("stop app") -> {
                VoiceCommand.CLOSE_APP
            }
            else -> VoiceCommand.UNKNOWN
        }
    }

    /**
     * 开始监听
     */
    fun startListening() {
        if (isListening) {
            Log.w(TAG, "⚠️ 已在监听中，跳过重启 (isListening=$isListening)")
            return
        }

        // 如果识别器为null，重新初始化
        if (speechRecognizer == null) {
            Log.d(TAG, "识别器为null，重新初始化...")
            initializeSpeechRecognizer()
        }

        shouldRestart = true
        val intent = createRecognitionIntent()
        speechRecognizer?.startListening(intent)
        Log.d(TAG, "✓ 开始监听语音命令 - 语言: $currentLanguage (isListening将在onReadyForSpeech时设为true)")
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
        // 检查是否已经在测试中
        if (isTesting) {
            Log.w(TAG, "已有测试正在进行中，请稍后再试")
            onResult(null)
            return
        }

        Log.d(TAG, "======================================")
        Log.d(TAG, "开始极简化识别测试")
        Log.d(TAG, "使用最基本的配置，无额外参数")
        Log.d(TAG, "======================================")

        isTesting = true

        // 先清理并停止所有识别器
        cleanupTestRecognizer()
        speechRecognizer?.stopListening()
        shouldRestart = false

        // 延迟200ms确保识别器完全停止
        CoroutineScope(Dispatchers.Main).launch {
            delay(200)

            val simpleIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // 不设置任何其他参数，使用系统默认
            }

            testRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            testRecognizer?.setRecognitionListener(object : RecognitionListener {
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
                    8 -> "ERROR_RECOGNIZER_BUSY (8): 识别器忙碌"
                    else -> "错误码: $error"
                }
                Log.e(TAG, "[极简测试] ✗ 识别失败: $msg")
                isTesting = false
                onResult(null)
                cleanupTestRecognizer()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d(TAG, "[极简测试] ✓✓✓ 识别成功！")
                Log.d(TAG, "[极简测试] 结果: $matches")
                isTesting = false
                onResult(matches?.firstOrNull())
                cleanupTestRecognizer()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d(TAG, "[极简测试] 部分结果: $matches")
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

            Log.d(TAG, "[极简测试] 开始监听...")
            testRecognizer?.startListening(simpleIntent)
        }
    }

    /**
     * 一次性识别测试（不自动重启）
     */
    fun testRecognition(onResult: (String?) -> Unit) {
        // 检查是否已经在测试中
        if (isTesting) {
            Log.w(TAG, "已有测试正在进行中，请稍后再试")
            onResult(null)
            return
        }

        Log.d(TAG, "开始一次性识别测试")
        isTesting = true

        // 先清理并停止所有识别器
        cleanupTestRecognizer()
        speechRecognizer?.stopListening()
        shouldRestart = false  // 不自动重启

        onDebugInfo?.invoke(VoiceDebugInfo(
            text = "开始识别测试，请说话...",
            isPartial = false,
            volume = 0f,
            isListening = true
        ))

        // 延迟200ms确保识别器完全停止
        CoroutineScope(Dispatchers.Main).launch {
            delay(200)

            val intent = createRecognitionIntent()

            // 临时监听器，用于测试
            testRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            testRecognizer?.setRecognitionListener(object : RecognitionListener {
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
                isTesting = false
                onResult(null)
                cleanupTestRecognizer()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                Log.d(TAG, "[测试] 识别结果: $text")
                Log.d(TAG, "[测试] 所有结果: $matches")
                isTesting = false
                onResult(text)
                cleanupTestRecognizer()
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

            testRecognizer?.startListening(intent)
        }
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
     * 清理测试识别器
     */
    private fun cleanupTestRecognizer() {
        try {
            testRecognizer?.destroy()
            testRecognizer = null
            Log.d(TAG, "测试识别器已清理")
        } catch (e: Exception) {
            Log.e(TAG, "清理测试识别器失败", e)
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        shouldRestart = false
        isTesting = false
        stopListening()
        cleanupTestRecognizer()
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
