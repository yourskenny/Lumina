package com.example.myapplication.presentation.viewmodel

import android.util.Log
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.CameraState
import com.example.myapplication.data.model.RecordingStats
import com.example.myapplication.data.repository.CameraRepository
import com.example.myapplication.data.repository.MediaRepository
import com.example.myapplication.domain.service.HapticFeedbackService
import com.example.myapplication.domain.service.TextToSpeechService
import com.example.myapplication.domain.service.VoiceCommand
import com.example.myapplication.domain.service.VoiceRecognitionService
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
    val isVoiceListening: Boolean = false
)

/**
 * 相机ViewModel
 * 协调相机、TTS、语音识别和触觉反馈服务
 */
class CameraViewModel(
    private val cameraRepository: CameraRepository,
    private val mediaRepository: MediaRepository,
    private val ttsService: TextToSpeechService,
    private val voiceService: VoiceRecognitionService,
    private val hapticService: HapticFeedbackService
) : ViewModel() {

    private val TAG = "CameraViewModel"

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var totalSegments = 0
    private var isAutoRecording = false

    init {
        // 监听相机状态变化
        viewModelScope.launch {
            cameraRepository.cameraState.collect { state ->
                _uiState.value = _uiState.value.copy(cameraState = state)
            }
        }
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

                    // 自动清理超过10分钟的旧录像
                    viewModelScope.launch {
                        val deletedCount = mediaRepository.deleteOldRecordings(maxAgeMinutes = 10)
                        if (deletedCount > 0) {
                            Log.d(TAG, "自动删除了 $deletedCount 个超过10分钟的旧录像")
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
     * 处理语音命令
     */
    fun handleVoiceCommand(command: VoiceCommand) {
        Log.d(TAG, "处理语音命令: $command")

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
     * 清理资源
     */
    override fun onCleared() {
        super.onCleared()
        cameraRepository.release()
        voiceService.release()
        ttsService.shutdown()
        Log.d(TAG, "ViewModel资源已释放")
    }
}
