package com.example.myapplication.presentation.viewmodel

import android.util.Log
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.CameraState
import com.example.myapplication.data.model.RecordingStats
import android.content.Context
import com.example.myapplication.data.repository.CameraRepository
import com.example.myapplication.data.repository.MediaRepository
import com.example.myapplication.data.repository.VideoPlaybackRepository
import com.example.myapplication.data.repository.StorageManagementRepository
import com.example.myapplication.data.repository.SettingsRepository
import com.example.myapplication.data.repository.LocationRepository
import com.example.myapplication.domain.service.HapticFeedbackService
import com.example.myapplication.domain.service.EmergencyService
import com.example.myapplication.util.BatteryUtils
import com.example.myapplication.util.MicrophoneTest
import com.example.myapplication.domain.service.TextToSpeechService
import com.example.myapplication.domain.service.VoiceCommand
import com.example.myapplication.domain.service.VoiceRecognitionService
import com.example.myapplication.domain.service.VoiceDebugInfo
import com.example.myapplication.domain.service.AudioRecordingService
import com.example.myapplication.domain.detector.ObjectDetector
import com.example.myapplication.data.model.RawObject
import android.graphics.BitmapFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 相机UI状态
 */
data class CameraUiState(
    val cameraState: CameraState = CameraState.Idle,
    val recordingStats: RecordingStats = RecordingStats(),
    val isPermissionGranted: Boolean = false,
    val errorMessage: String? = null,
    val isVoiceListening: Boolean = false,
    val voiceDebugText: String = "",           // 语音识别的文本
    val voiceVolume: Float = 0f,                // 麦克风音量 (0-10)
    val voiceError: String? = null,             // 语音识别错误
    val micTestResult: String? = null,          // 麦克风测试结果
    val isMicTesting: Boolean = false,          // 是否正在测试麦克风
    val currentLanguage: String = "中文",       // 当前识别语言
    val isRecognitionTesting: Boolean = false,  // 是否正在测试识别
    val showTextInput: Boolean = true,          // 是否显示文本输入（调试用）
    val testCommandResult: String? = null,      // 命令测试结果
    val isAudioRecording: Boolean = false,      // 是否正在录音
    val audioRecordingFile: String? = null,     // 当前录音文件路径
    val audioRecordingCount: Int = 0,           // 录音文件总数
    // AI 检测相关
    val isRealtimeAnalysisEnabled: Boolean = false, // 是否启用实时AI分析
    val analysisResult: String? = null,         // AI分析结果描述
    val detectedHazards: List<RawObject> = emptyList(), // 检测到的危险对象
    val detectedPaths: List<RawObject> = emptyList()    // 检测到的路径
)

/**
 * 相机ViewModel
 * 协调相机、TTS、语音识别和触觉反馈服务
 */
class CameraViewModel(
    private val context: Context,
    private val cameraRepository: CameraRepository,
    private val mediaRepository: MediaRepository,
    private val videoPlaybackRepository: VideoPlaybackRepository,
    private val storageManagementRepository: StorageManagementRepository,
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
    private val emergencyService: EmergencyService,
    private val ttsService: TextToSpeechService,
    private val voiceService: VoiceRecognitionService,
    private val hapticService: HapticFeedbackService
) : ViewModel() {

    private val TAG = "CameraViewModel"

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var totalSegments = 0
    private var isAutoRecording = false

    // 麦克风测试工具
    private var microphoneTest: MicrophoneTest? = null

    // 音频录制服务
    private val audioRecordingService = AudioRecordingService(context)

    // AI 对象检测器
    private val objectDetector = ObjectDetector(context)

    init {
        // 监听相机状态变化
        viewModelScope.launch {
            cameraRepository.cameraState.collect { state ->
                _uiState.value = _uiState.value.copy(cameraState = state)
            }
        }

        // 将LocationRepository注入到MediaRepository
        mediaRepository.setLocationRepository(locationRepository)

        // 设置初始语言
        _uiState.value = _uiState.value.copy(currentLanguage = voiceService.getCurrentLanguage())

        // 启动GPS位置跟踪
        if (locationRepository.hasLocationPermission()) {
            locationRepository.startTracking()
            Log.d(TAG, "GPS位置跟踪已启动")
        } else {
            Log.w(TAG, "缺少GPS定位权限，位置跟踪未启动")
        }

        // 更新录音文件计数
        updateAudioRecordingCount()
    }

    /**
     * 初始化相机
     */
    suspend fun initializeCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        ttsService.speak("正在初始化相机")

        val result = cameraRepository.initializeCamera(lifecycleOwner, previewView)

        result.onSuccess {
            ttsService.speak("相机准备就绪")
            hapticService.feedbackSuccess()
            _uiState.value = _uiState.value.copy(isPermissionGranted = true)
        }.onFailure { error ->
            val message = "相机初始化失败: ${error.message}"
            ttsService.speak(message, urgent = true)
            hapticService.feedbackError()
            _uiState.value = _uiState.value.copy(errorMessage = message)
        }
    }

    /**
     * 权限已授予时调用
     */
    fun onPermissionsGranted() {
        ttsService.speak("权限已授予,正在启动相机")
        _uiState.value = _uiState.value.copy(isPermissionGranted = true)

        // 启动语音识别
        startVoiceRecognition()
    }

    /**
     * 启动语音识别
     */
    private fun startVoiceRecognition() {
        voiceService.startListening()
        _uiState.value = _uiState.value.copy(isVoiceListening = true)
        Log.d(TAG, "语音识别已启动")
    }

    /**
     * 开始自动录像
     */
    fun startAutoRecording() {
        if (isAutoRecording) {
            Log.d(TAG, "已在自动录像中")
            return
        }

        // 检查存储空间
        if (!mediaRepository.hasEnoughStorage()) {
            val message = "存储空间不足,请清理后继续"
            ttsService.speak(message, urgent = true)
            hapticService.feedbackWarning()
            _uiState.value = _uiState.value.copy(errorMessage = message)
            return
        }

        isAutoRecording = true
        viewModelScope.launch {
            startRecordingSegment()
        }
    }

    /**
     * 开始录制一个片段
     */
    private fun startRecordingSegment() {
        Log.d(TAG, "开始录制片段 #${totalSegments + 1}")

        cameraRepository.startRecording(mediaRepository) { event ->
            handleRecordingEvent(event)
        }

        ttsService.speak("开始录像")
        hapticService.feedbackRecordingStart()
    }

    /**
     * 处理录像事件
     */
    private fun handleRecordingEvent(event: VideoRecordEvent) {
        when (event) {
            is VideoRecordEvent.Start -> {
                Log.d(TAG, "录像已开始")
            }
            is VideoRecordEvent.Status -> {
                val durationSeconds = (event.recordingStats.recordedDurationNanos / 1_000_000_000).toInt()
                val fileSizeMB = event.recordingStats.numBytesRecorded / (1024f * 1024f)

                _uiState.value = _uiState.value.copy(
                    recordingStats = RecordingStats(
                        isRecording = true,
                        durationSeconds = durationSeconds,
                        fileSizeMB = fileSizeMB,
                        totalSegments = totalSegments
                    )
                )

                // 每5分钟自动分段
                if (durationSeconds >= 300 && isAutoRecording) { // 300秒 = 5分钟
                    stopRecordingAndStartNext()
                }
            }
            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    val message = "录像失败: ${event.cause?.message}"
                    Log.e(TAG, message)
                    ttsService.speak(message, urgent = true)
                    hapticService.feedbackError()
                    _uiState.value = _uiState.value.copy(errorMessage = message)

                    // 尝试重新开始录像
                    if (isAutoRecording) {
                        viewModelScope.launch {
                            delay(5000) // 等待5秒
                            startRecordingSegment()
                        }
                    }
                } else {
                    totalSegments++
                    val durationMin = _uiState.value.recordingStats.durationSeconds / 60
                    ttsService.speak("视频已保存,时长 $durationMin 分钟")
                    hapticService.feedbackSuccess()

                    Log.d(TAG, "视频已保存: ${event.outputResults.outputUri}")

                    // 智能存储管理：自动清理
                    viewModelScope.launch {
                        val cleanupPolicy = StorageManagementRepository.CleanupPolicy(
                            maxAgeMinutes = 60,        // 保留最近60分钟的视频
                            maxVideoCount = 20,        // 最多保留20个视频
                            maxTotalSizeMB = 1024,     // 最多占用1GB空间
                            minFreeSpaceMB = 500,      // 确保至少500MB剩余空间
                            enableAutoCleanup = true   // 启用自动清理
                        )

                        val deletedCount = storageManagementRepository.performSmartCleanup(cleanupPolicy)
                        if (deletedCount > 0) {
                            Log.d(TAG, "智能清理：自动删除了 $deletedCount 个文件")
                        }
                    }

                    // 自动开始下一段录制
                    if (isAutoRecording) {
                        viewModelScope.launch {
                            delay(1000) // 等待1秒
                            startRecordingSegment()
                        }
                    }
                }
            }
        }
    }

    /**
     * 停止当前录像并开始下一段
     */
    private fun stopRecordingAndStartNext() {
        cameraRepository.stopRecording()
    }

    /**
     * 暂停录像
     */
    fun pauseRecording() {
        if (!cameraRepository.isRecording()) {
            Log.d(TAG, "当前未在录像")
            return
        }

        cameraRepository.pauseRecording()
        isAutoRecording = false
        ttsService.speak("录像已暂停")
        hapticService.feedbackRecordingPause()
        Log.d(TAG, "录像已暂停")
    }

    /**
     * 恢复录像
     */
    fun resumeRecording() {
        if (cameraRepository.isPaused()) {
            cameraRepository.resumeRecording()
            isAutoRecording = true
            ttsService.speak("继续录像")
            hapticService.feedbackRecordingStart()
            Log.d(TAG, "录像已恢复")
        } else {
            // 如果不是暂停状态,则开始新的录像
            startAutoRecording()
        }
    }

    /**
     * 在录像过程中拍照
     */
    fun capturePhotoWhileRecording() {
        val outputFile = mediaRepository.createPhotoOutputFile()

        cameraRepository.capturePhotoWhileRecording(
            outputFile = outputFile,
            onSuccess = {
                ttsService.speak("咔嚓,照片已保存")
                hapticService.feedbackCapture()
                Log.d(TAG, "照片已保存: ${outputFile.absolutePath}")

                // 添加照片到媒体库，使其在系统相册中可见
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    mediaRepository.addPhotoToMediaStore(outputFile)
                }

                // AI 分析照片
                performAIAnalysis(outputFile)
            },
            onError = { exception ->
                val message = "拍照失败: ${exception.message}"
                ttsService.speak(message)
                hapticService.feedbackError()
                Log.e(TAG, message)
            }
        )
    }

    /**
     * 执行 AI 物体检测分析
     */
    private fun performAIAnalysis(file: java.io.File) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🤖 开始AI分析: ${file.absolutePath}")

                // 1. 加载图片
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap == null) {
                    Log.e(TAG, "无法加载图片进行分析")
                    return@launch
                }

                // 2. 执行 ONNX 推理
                val result = objectDetector.detect(bitmap)

                Log.d(TAG, "✅ AI检测完成: ${result.hazards.size} 个危险, ${result.paths.size} 个路径")

                // 3. 分析结果并生成状态描述
                var state = "SAFE"
                var summary = "Path is clear"

                var nearestHazardDist = 999.0
                var nearestHazardName = ""
                var nearestHazardDir = ""

                // 查找最近的危险
                for (hazard in result.hazards) {
                    if (hazard.distanceM < nearestHazardDist) {
                        nearestHazardDist = hazard.distanceM
                        nearestHazardName = hazard.name
                        nearestHazardDir = hazard.direction
                    }
                }

                // 根据距离判断危险等级
                if (nearestHazardDist < 1.5) {
                    state = "DANGER"
                    summary = "STOP! $nearestHazardName ahead"
                    hapticService.feedbackError()
                    ttsService.speak("Warning. $nearestHazardName $nearestHazardDir. ${String.format("%.1f", nearestHazardDist)} meters.", urgent = true)
                } else if (nearestHazardDist < 3.0) {
                    state = "CAUTION"
                    summary = "$nearestHazardName detected at ${String.format("%.1f", nearestHazardDist)}m"
                    hapticService.feedbackWarning()
                    ttsService.speak("Caution. $nearestHazardName $nearestHazardDir. ${String.format("%.1f", nearestHazardDist)} meters.")
                } else if (result.paths.isNotEmpty()) {
                    state = "SAFE"
                    val path = result.paths[0]
                    summary = "Follow ${path.name} ${path.direction}"
                    ttsService.speak("${path.name} detected ${path.direction}")
                }

                // 4. 更新 UI 状态
                _uiState.value = _uiState.value.copy(
                    analysisResult = "$state: $summary",
                    detectedHazards = result.hazards,
                    detectedPaths = result.paths
                )

                Log.d(TAG, "📊 分析结果: $state - $summary")

                // 清理 bitmap
                bitmap.recycle()

            } catch (e: Exception) {
                Log.e(TAG, "AI分析失败", e)
                _uiState.value = _uiState.value.copy(
                    analysisResult = "Analysis failed: ${e.message}"
                )
            }
        }
    }

    /**
     * 清空所有录像
     */
    fun clearAllRecordings() {
        viewModelScope.launch {
            ttsService.speak("正在清空所有录像", urgent = true)

            val deletedCount = mediaRepository.clearAllRecordings()

            if (deletedCount > 0) {
                val message = "已删除 $deletedCount 个文件"
                ttsService.speak(message)
                hapticService.feedbackSuccess()
                Log.d(TAG, message)
            } else {
                val message = "没有找到需要删除的文件"
                ttsService.speak(message)
                hapticService.feedbackWarning()
                Log.d(TAG, message)
            }
        }
    }

    /**
     * 播放最新视频
     */
    fun playLatestVideo() {
        viewModelScope.launch {
            val success = videoPlaybackRepository.playLatestVideo()
            if (success) {
                ttsService.speak("正在播放最新视频")
                hapticService.feedbackSuccess()
                Log.d(TAG, "启动视频播放")
            } else {
                val message = "没有找到可播放的视频"
                ttsService.speak(message)
                hapticService.feedbackWarning()
                Log.d(TAG, message)
            }
        }
    }

    /**
     * 分享最新视频
     */
    fun shareLatestVideo() {
        viewModelScope.launch {
            val success = videoPlaybackRepository.shareLatestVideo()
            if (success) {
                ttsService.speak("正在分享视频")
                hapticService.feedbackSuccess()
                Log.d(TAG, "启动视频分享")
            } else {
                val message = "没有找到可分享的视频"
                ttsService.speak(message)
                hapticService.feedbackWarning()
                Log.d(TAG, message)
            }
        }
    }

    /**
     * 查询存储空间
     */
    fun checkStorageInfo() {
        viewModelScope.launch {
            val stats = storageManagementRepository.getStorageStats()
            val message = "当前有 ${stats.totalVideoCount} 个视频，" +
                    "占用 ${stats.totalSizeMB} 兆，" +
                    "剩余空间 ${stats.availableSpaceMB} 兆"
            ttsService.speak(message)
            hapticService.feedbackSuccess()
            Log.d(TAG, "存储信息: $stats")
        }
    }

    /**
     * 切换摄像头（前/后）
     */
    fun switchCamera() {
        val cameraType = cameraRepository.switchCamera()
        ttsService.speak("已切换到${cameraType}摄像头")
        hapticService.feedbackSuccess()
        Log.d(TAG, "切换到${cameraType}摄像头")
    }

    /**
     * 切换闪光灯
     */
    fun toggleFlashlight() {
        val enabled = cameraRepository.toggleFlashlight()
        val message = if (enabled) "闪光灯已打开" else "闪光灯已关闭"
        ttsService.speak(message)
        hapticService.feedbackSuccess()
        Log.d(TAG, message)
    }

    /**
     * 查询电池状态
     */
    fun checkBatteryInfo() {
        val batteryInfo = BatteryUtils.getBatteryInfo(context)
        ttsService.speak(batteryInfo)
        hapticService.feedbackSuccess()
        Log.d(TAG, "电池信息: $batteryInfo")
    }

    /**
     * 查询当前录像时长
     */
    fun checkRecordingTime() {
        if (!cameraRepository.isRecording()) {
            ttsService.speak("当前未在录像")
            return
        }

        val durationSeconds = cameraRepository.getCurrentRecordingDuration()
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60

        val message = if (minutes > 0) {
            "已录像${minutes}分钟${seconds}秒"
        } else {
            "已录像${seconds}秒"
        }

        ttsService.speak(message)
        hapticService.feedbackSuccess()
        Log.d(TAG, "录像时长: $message")
    }

    /**
     * 紧急呼叫
     */
    fun emergencyCall() {
        if (!emergencyService.hasEmergencyContact()) {
            val message = "未设置紧急联系人"
            ttsService.speak(message, urgent = true)
            hapticService.feedbackWarning()
            Log.w(TAG, message)
            return
        }

        val (name, phone) = emergencyService.getEmergencyContactInfo()
        ttsService.speak("正在拨打紧急联系人${name}的电话", urgent = true)

        val success = emergencyService.callEmergencyContact()
        if (success) {
            // 同时发送短信
            emergencyService.sendEmergencySMS()
            hapticService.feedbackSuccess()
            Log.d(TAG, "紧急呼叫成功: $phone")
        } else {
            ttsService.speak("拨打电话失败", urgent = true)
            hapticService.feedbackError()
            Log.e(TAG, "紧急呼叫失败")
        }
    }

    /**
     * 查询当前GPS位置
     */
    fun checkLocation() {
        if (!locationRepository.hasLocationPermission()) {
            val message = "未授予定位权限"
            ttsService.speak(message, urgent = true)
            hapticService.feedbackWarning()
            Log.w(TAG, message)
            return
        }

        if (!locationRepository.isGpsEnabled()) {
            val message = "GPS未启用，请在设置中开启定位服务"
            ttsService.speak(message, urgent = true)
            hapticService.feedbackWarning()
            Log.w(TAG, message)
            return
        }

        val locationDescription = locationRepository.getLocationDescription()
        ttsService.speak(locationDescription)
        hapticService.feedbackSuccess()
        Log.d(TAG, "位置查询: $locationDescription")

        // 记录当前位置到历史
        locationRepository.recordLocation(locationRepository.getCurrentLocation())
    }

    /**
     * 测试麦克风
     */
    fun testMicrophone() {
        _uiState.value = _uiState.value.copy(
            isMicTesting = true,
            micTestResult = "正在测试麦克风，请对着麦克风说话..."
        )

        ttsService.speak("开始麦克风测试，请对着麦克风说话")
        Log.d(TAG, "开始麦克风测试")

        // 停止语音识别以避免冲突
        voiceService.stopListening()

        microphoneTest = MicrophoneTest(context)

        // 先显示配置信息
        val info = microphoneTest!!.getMicrophoneInfo()
        Log.d(TAG, info)

        // 开始测试
        microphoneTest!!.testMicrophone { success, message ->
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    isMicTesting = false,
                    micTestResult = if (success) "✅ $message" else "❌ $message"
                )

                ttsService.speak(if (success) "麦克风测试成功" else "麦克风测试失败")
                Log.d(TAG, "麦克风测试结果: $message")

                // 测试完成后恢复语音识别
                delay(2000)
                voiceService.startListening()

                // 5秒后清除测试结果
                delay(5000)
                _uiState.value = _uiState.value.copy(micTestResult = null)
            }
        }
    }

    /**
     * 更新语音调试信息
     */
    fun updateVoiceDebugInfo(debugInfo: VoiceDebugInfo) {
        _uiState.value = _uiState.value.copy(
            voiceDebugText = debugInfo.text,
            voiceVolume = debugInfo.volume,
            isVoiceListening = debugInfo.isListening,
            voiceError = debugInfo.error
        )
    }

    /**
     * 切换识别语言
     */
    fun switchVoiceLanguage() {
        voiceService.switchLanguage()
        val newLanguage = voiceService.getCurrentLanguage()
        _uiState.value = _uiState.value.copy(currentLanguage = newLanguage)
        ttsService.speak("已切换到${newLanguage}识别")
        Log.d(TAG, "切换语言到: $newLanguage")
    }

    /**
     * 切换语音识别启用/禁用
     */
    fun toggleVoiceRecognition() {
        if (voiceService.isEnabled()) {
            voiceService.disable()
            ttsService.speak("语音识别已禁用")
            Log.d(TAG, "语音识别已禁用")
        } else {
            voiceService.enable()
            ttsService.speak("语音识别已启用")
            Log.d(TAG, "语音识别已启用")
        }
    }

    /**
     * 极简化识别测试
     */
    fun testSimpleRecognition() {
        _uiState.value = _uiState.value.copy(
            isRecognitionTesting = true,
            voiceDebugText = "极简测试（无额外参数）..."
        )

        ttsService.speak("极简测试，请说话")
        Log.d(TAG, "开始极简识别测试")

        voiceService.testSimpleRecognition { result ->
            viewModelScope.launch {
                if (result != null) {
                    _uiState.value = _uiState.value.copy(
                        isRecognitionTesting = false,
                        voiceDebugText = "✅ 极简测试成功: $result"
                    )
                    ttsService.speak("识别成功: $result")
                    Log.d(TAG, "极简测试成功: $result")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isRecognitionTesting = false,
                        voiceDebugText = "❌ 极简测试失败，查看日志"
                    )
                    ttsService.speak("识别失败")
                    Log.e(TAG, "极简测试失败")
                }

                // 5秒后清除结果
                delay(5000)
                if (!_uiState.value.isVoiceListening) {
                    _uiState.value = _uiState.value.copy(voiceDebugText = "")
                }
            }
        }
    }

    /**
     * 测试语音识别
     */
    fun testRecognition() {
        _uiState.value = _uiState.value.copy(
            isRecognitionTesting = true,
            voiceDebugText = "开始识别测试..."
        )

        ttsService.speak("开始识别测试，请说话")
        Log.d(TAG, "开始识别测试")

        voiceService.testRecognition { result ->
            viewModelScope.launch {
                if (result != null) {
                    _uiState.value = _uiState.value.copy(
                        isRecognitionTesting = false,
                        voiceDebugText = "✅ 识别成功: $result"
                    )
                    ttsService.speak("识别成功: $result")
                    Log.d(TAG, "识别测试成功: $result")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isRecognitionTesting = false,
                        voiceDebugText = "❌ 识别失败，请检查网络和语言设置"
                    )
                    ttsService.speak("识别失败")
                    Log.e(TAG, "识别测试失败")
                }

                // 5秒后清除结果
                delay(5000)
                if (!_uiState.value.isVoiceListening) {
                    _uiState.value = _uiState.value.copy(voiceDebugText = "")
                }
            }
        }
    }

    /**
     * 测试文本命令（绕过语音识别）
     */
    fun testTextCommand(text: String) {
        if (text.isBlank()) {
            _uiState.value = _uiState.value.copy(
                testCommandResult = "⚠️ 请输入命令文本"
            )
            return
        }

        Log.d(TAG, "=== 开始测试文本命令 ===")
        Log.d(TAG, "输入文本: $text")

        _uiState.value = _uiState.value.copy(
            voiceDebugText = "测试命令: $text",
            testCommandResult = "正在处理..."
        )

        // 使用voiceService的testCommand方法
        val command = voiceService.testCommand(text)

        if (command != VoiceCommand.UNKNOWN) {
            _uiState.value = _uiState.value.copy(
                testCommandResult = "✅ 匹配到命令: $command"
            )
            Log.d(TAG, "✅ 命令匹配成功: $command")

            // 执行命令
            handleVoiceCommand(command)

            ttsService.speak("执行命令: ${command.name}")
        } else {
            _uiState.value = _uiState.value.copy(
                testCommandResult = "❌ 未匹配到命令\n输入: '$text'\n请尝试: '拍照', '查询电池', '暂停录像'"
            )
            Log.w(TAG, "❌ 命令匹配失败: $text")
        }

        // 5秒后清除结果
        viewModelScope.launch {
            delay(5000)
            _uiState.value = _uiState.value.copy(testCommandResult = null)
        }

        Log.d(TAG, "=== 文本命令测试完成 ===")
    }

    /**
     * 处理语音命令
     */
    fun handleVoiceCommand(command: VoiceCommand) {
        Log.d(TAG, ">>> 开始执行命令: $command")
        ttsService.speak(command.name) // 先语音播报命令名称

        when (command) {
            VoiceCommand.CAPTURE_PHOTO -> {
                capturePhotoWhileRecording()
            }
            VoiceCommand.PAUSE_RECORDING -> {
                pauseRecording()
            }
            VoiceCommand.RESUME_RECORDING -> {
                resumeRecording()
            }
            VoiceCommand.PLAY_VIDEO -> {
                playLatestVideo()
            }
            VoiceCommand.SHARE_VIDEO -> {
                shareLatestVideo()
            }
            VoiceCommand.CHECK_STORAGE -> {
                checkStorageInfo()
            }
            VoiceCommand.SWITCH_CAMERA -> {
                switchCamera()
            }
            VoiceCommand.TOGGLE_FLASHLIGHT -> {
                toggleFlashlight()
            }
            VoiceCommand.CHECK_BATTERY -> {
                checkBatteryInfo()
            }
            VoiceCommand.CHECK_RECORDING_TIME -> {
                checkRecordingTime()
            }
            VoiceCommand.EMERGENCY_CALL -> {
                emergencyCall()
            }
            VoiceCommand.CHECK_LOCATION -> {
                checkLocation()
            }
            VoiceCommand.CLEAR_RECORDINGS -> {
                clearAllRecordings()
            }
            VoiceCommand.CLOSE_APP -> {
                ttsService.speak("正在关闭应用")
                // 在实际应用中,这里应该通知Activity关闭
                Log.d(TAG, "请求关闭应用")
            }
            VoiceCommand.UNKNOWN -> {
                Log.d(TAG, "未识别的命令")
            }
        }
    }

    /**
     * 开始录音（同时进行语音识别）
     */
    fun startAudioRecording() {
        if (_uiState.value.isAudioRecording) {
            Log.w(TAG, "已经在录音中")
            ttsService.speak("已经在录音中")
            return
        }

        val filePath = audioRecordingService.startRecording()
        if (filePath != null) {
            _uiState.value = _uiState.value.copy(
                isAudioRecording = true,
                audioRecordingFile = filePath
            )
            ttsService.speak("开始录音")
            hapticService.feedbackSuccess()
            Log.d(TAG, "开始录音: $filePath")
        } else {
            _uiState.value = _uiState.value.copy(isAudioRecording = false)
            ttsService.speak("录音启动失败", urgent = true)
            hapticService.feedbackError()
            Log.e(TAG, "录音启动失败")
        }
    }

    /**
     * 停止录音
     */
    fun stopAudioRecording() {
        if (!_uiState.value.isAudioRecording) {
            Log.w(TAG, "当前没有在录音")
            ttsService.speak("当前没有在录音")
            return
        }

        val filePath = audioRecordingService.stopRecording()
        if (filePath != null) {
            _uiState.value = _uiState.value.copy(
                isAudioRecording = false,
                audioRecordingFile = null
            )
            ttsService.speak("录音已保存")
            hapticService.feedbackSuccess()
            Log.d(TAG, "录音已保存: $filePath")

            // 更新录音文件计数
            updateAudioRecordingCount()
        } else {
            _uiState.value = _uiState.value.copy(isAudioRecording = false)
            ttsService.speak("录音保存失败", urgent = true)
            hapticService.feedbackError()
            Log.e(TAG, "录音保存失败")
        }
    }

    /**
     * 切换录音状态（开始/停止）
     */
    fun toggleAudioRecording() {
        if (_uiState.value.isAudioRecording) {
            stopAudioRecording()
        } else {
            startAudioRecording()
        }
    }

    /**
     * 更新录音文件计数
     */
    private fun updateAudioRecordingCount() {
        val count = audioRecordingService.getAllRecordings().size
        _uiState.value = _uiState.value.copy(audioRecordingCount = count)
        Log.d(TAG, "录音文件总数: $count")
    }

    /**
     * 获取所有录音文件
     */
    fun getAllAudioRecordings(): List<String> {
        return audioRecordingService.getAllRecordings().map { it.absolutePath }
    }

    /**
     * 清空所有录音文件
     */
    fun clearAllAudioRecordings() {
        val count = audioRecordingService.clearAllRecordings()
        ttsService.speak("已清空 $count 个录音文件")
        hapticService.feedbackSuccess()
        Log.d(TAG, "已清空 $count 个录音文件")

        // 更新录音文件计数
        updateAudioRecordingCount()
    }

    /**
     * 清理资源
     */
    override fun onCleared() {
        super.onCleared()
        cameraRepository.release()
        voiceService.release()
        ttsService.shutdown()
        locationRepository.release()
        audioRecordingService.release()
        objectDetector.close()
        Log.d(TAG, "ViewModel资源已释放")
    }
}
