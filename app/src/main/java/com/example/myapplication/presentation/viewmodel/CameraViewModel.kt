package com.example.myapplication.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.CameraState
import com.example.myapplication.data.model.RawObject
import com.example.myapplication.data.model.RecordingStats
import com.example.myapplication.data.repository.CameraRepository
import com.example.myapplication.data.repository.MediaRepository
import com.example.myapplication.data.repository.MemoryRepository
import com.example.myapplication.data.repository.SettingsRepository
import com.example.myapplication.domain.agent.AgentService
import com.example.myapplication.domain.context.ContextManager
import com.example.myapplication.domain.dispatcher.IntentDispatcher
import com.example.myapplication.domain.feedback.FeedbackArbiter
import com.example.myapplication.domain.feedback.FeedbackPriority
import com.example.myapplication.domain.feedback.VibrationType
import com.example.myapplication.domain.detector.ObjectDetector
import com.example.myapplication.domain.service.GenerativeAIService
import com.example.myapplication.domain.service.HapticFeedbackService
import com.example.myapplication.domain.service.TextToSpeechService
import com.example.myapplication.domain.service.VoiceCommand
import com.example.myapplication.domain.service.VoiceRecognitionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 相机视图模型
 * 负责协调UI、相机、AI检测、语音和触觉反馈
 */
import com.example.myapplication.data.repository.AMapRepository
import com.example.myapplication.domain.service.LocationService

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "CameraViewModel"

    // 仓库和服务
    private val settingsRepository = SettingsRepository(application) // 新增设置仓库
    private val cameraRepository = CameraRepository(application)
    private val mediaRepository = MediaRepository(application)
    private val memoryRepository = MemoryRepository(application) 
    private val ttsService = TextToSpeechService(application)
    private val hapticService = HapticFeedbackService(application)
    private val feedbackArbiter = FeedbackArbiter(ttsService, hapticService) 
    
    private val locationService = LocationService(application)
    private val amapRepository = AMapRepository()
    
    private val contextManager = ContextManager(application) 
    private val generativeAIService = GenerativeAIService(settingsRepository) 
    private val agentService = AgentService(application, memoryRepository, settingsRepository, contextManager, feedbackArbiter, locationService, amapRepository) // 注入依赖
    private val objectDetector = ObjectDetector()
    
    private val intentDispatcher = IntentDispatcher(
        cameraRepository, mediaRepository, memoryRepository, agentService, feedbackArbiter, viewModelScope
    )
    
    private var voiceService: VoiceRecognitionService? = null

    // UI状态流
    val cameraState = cameraRepository.cameraState
    
    private val _recordingStats = MutableStateFlow(RecordingStats())
    val recordingStats: StateFlow<RecordingStats> = _recordingStats.asStateFlow()
    
    private val _detectedObjects = MutableStateFlow<List<RawObject>>(emptyList())
    val detectedObjects: StateFlow<List<RawObject>> = _detectedObjects.asStateFlow()

    // 暴露 settingsRepository 给 UI
    fun getSettingsRepository(): SettingsRepository {
        return settingsRepository
    }
    
    // 自动清理定时器
    private var autoCleanupJob: kotlinx.coroutines.Job? = null
    
    // AI检测控制
    private var isDetecting = false
    private var lastSpeakTime = 0L
    private val SPEAK_INTERVAL = 3000L // 语音播报最小间隔(3秒)

    init {
        // ... (existing code)
        
        // 初始化 IntentDispatcher Action Handler
        intentDispatcher.setActionHandler(object : IntentDispatcher.ActionHandler {
            override fun onCapturePhoto() {
                capturePhoto()
            }
            override fun onDescribeScene() {
                describeCurrentScene()
            }
            override fun onCloseApp() {
                // UI Event to close app?
            }
        })

        // 初始化语音识别
        voiceService = VoiceRecognitionService(application) { command ->
            intentDispatcher.dispatch(command)
        }
        
        // ...
    }

    /**
     * 使用Gemini描述当前场景
     */
    fun describeCurrentScene() {
        if (!generativeAIService.isAvailable()) {
            feedbackArbiter.speak("AI服务不可用,请检查API Key配置", FeedbackPriority.HIGH)
            return
        }

        feedbackArbiter.speak("正在观察...", FeedbackPriority.NORMAL)
        
        // 我们需要抓取当前帧。
        // 由于我们没有直接保存最新的Bitmap (processImageProxy中用完即弃),
        // 我们可以复用 capturePhoto 的逻辑, 或者添加一个 flag 让 processImageProxy 保存下一帧。
        
        // 这里采用最简单的方式: 复用拍照逻辑获取高画质图片, 然后发送给 Gemini
        val photoFile = mediaRepository.createTempPhotoFile()
        
        cameraRepository.takePicture(
            photoFile,
            onSuccess = {
                // 读取图片文件为 Bitmap
                val bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
                
                // 获取当前的检测结果作为参考
                val currentObjects = _detectedObjects.value.map { it.name }
                
                viewModelScope.launch {
                    val description = generativeAIService.generateDescription(
                        bitmap = bitmap,
                        detectedObjects = currentObjects
                    )
                    
                    // 保存记忆
                    memoryRepository.saveAgentMessage(description)
                    memoryRepository.saveSceneLog(description)
                    
                    feedbackArbiter.speak(description, FeedbackPriority.NORMAL)
                    
                    // 触发 Proactive Loop: 让 Agent 分析场景是否有危险
                    agentService.analyzeScene(description)
                    
                    // 删除临时文件
                    photoFile.delete()
                }
            },
            onError = {
                feedbackArbiter.speak("获取图像失败", FeedbackPriority.HIGH)
            }
        )
    }

    // ... (processImageFrame is fine)

    /**
     * 处理图像帧用于AI检测
     * @param bitmap 相机预览帧
     */
    fun processImageFrame(bitmap: Bitmap) {
        if (isDetecting) return
        
        isDetecting = true
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val results = objectDetector.detect(bitmap)
                _detectedObjects.value = results
                
                // 更新 ContextManager
                contextManager.updateVisualContext(results)
                
                // 处理检测结果反馈
                processDetectionFeedback(results)
            } catch (e: Exception) {
                Log.e(TAG, "检测失败", e)
            } finally {
                isDetecting = false
            }
        }
    }

    /**
     * 处理检测结果的反馈逻辑
     */
    private fun processDetectionFeedback(objects: List<RawObject>) {
        if (objects.isEmpty()) return
        
        Log.d(TAG, "Feedback处理: ${objects.size} 个物体")

        // 找到最近的物体
        val nearestObject = objects.minByOrNull { it.distanceM } ?: return
        
        // 危险检测 (< 1.5m)
        if (nearestObject.distanceM < 1.5) {
            feedbackArbiter.vibrate(VibrationType.WARNING, FeedbackPriority.CRITICAL)
            
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastSpeakTime > 2000) { // 紧急情况缩短间隔
                feedbackArbiter.speak("注意! 前方 ${String.format("%.1f", nearestObject.distanceM)} 米有 ${nearestObject.name}", FeedbackPriority.CRITICAL)
                lastSpeakTime = currentTime
            }
        } 
        // 警告检测 (< 3.0m)
        else if (nearestObject.distanceM < 3.0) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastSpeakTime > SPEAK_INTERVAL) {
                feedbackArbiter.vibrate(VibrationType.SUCCESS, FeedbackPriority.NORMAL)
                feedbackArbiter.speak("前方发现 ${nearestObject.name}", FeedbackPriority.NORMAL)
                lastSpeakTime = currentTime
            }
        }
    }

    /**
     * 开始录像
     */
    fun startRecording() {
        if (!mediaRepository.hasEnoughStorage()) {
            feedbackArbiter.speak("存储空间不足,无法录像", FeedbackPriority.HIGH)
            feedbackArbiter.vibrate(VibrationType.ERROR)
            return
        }
        
        feedbackArbiter.vibrate(VibrationType.RECORDING_START)
        
        cameraRepository.startRecording(mediaRepository) { event ->
            // 更新录像状态
            // 这里简化处理,实际应该解析event更新_recordingStats
        }
    }

    /**
     * 暂停录像
     */
    fun pauseRecording() {
        cameraRepository.pauseRecording()
        feedbackArbiter.vibrate(VibrationType.RECORDING_PAUSE)
    }

    /**
     * 恢复录像
     */
    fun resumeRecording() {
        cameraRepository.resumeRecording()
        feedbackArbiter.vibrate(VibrationType.RECORDING_START)
    }

    /**
     * 停止录像
     */
    fun stopRecording() {
        cameraRepository.stopRecording()
        feedbackArbiter.vibrate(VibrationType.SUCCESS)
    }

    /**
     * 拍照
     */
    fun capturePhoto() {
        val photoFile = mediaRepository.createPhotoOutputFile()
        
        if (cameraRepository.isRecording()) {
            cameraRepository.capturePhotoWhileRecording(
                photoFile,
                onSuccess = {
                    feedbackArbiter.vibrate(VibrationType.CAPTURE)
                    feedbackArbiter.speak("拍照成功", FeedbackPriority.HIGH)
                },
                onError = {
                    feedbackArbiter.vibrate(VibrationType.ERROR)
                    feedbackArbiter.speak("拍照失败", FeedbackPriority.HIGH)
                }
            )
        } else {
            cameraRepository.takePicture(
                photoFile,
                onSuccess = {
                    feedbackArbiter.vibrate(VibrationType.CAPTURE)
                    feedbackArbiter.speak("拍照成功", FeedbackPriority.HIGH)
                },
                onError = {
                    feedbackArbiter.vibrate(VibrationType.ERROR)
                    feedbackArbiter.speak("拍照失败", FeedbackPriority.HIGH)
                }
            )
        }
    }


    /**
     * 启动语音识别
     */
    fun startVoiceListening() {
        voiceService?.startListening()
    }

    /**
     * 停止语音识别
     */
    fun stopVoiceListening() {
        voiceService?.stopListening()
    }
    
    /**
     * 初始化相机
     */
    fun initializeCamera(
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        previewView: androidx.camera.view.PreviewView
    ) {
        viewModelScope.launch {
            cameraRepository.initializeCamera(lifecycleOwner, previewView) { imageProxy ->
                processImageProxy(imageProxy)
            }
        }
    }

    private fun processImageProxy(imageProxy: androidx.camera.core.ImageProxy) {
        if (isDetecting) {
            imageProxy.close()
            return
        }
        
        try {
            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val rotatedBitmap = if (rotationDegrees != 0) {
                val matrix = android.graphics.Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            processImageFrame(rotatedBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "图像转换失败", e)
        } finally {
            imageProxy.close()
        }
    }

    /**
     * 检查是否已暂停
     */
    fun isPaused(): Boolean {
        return cameraRepository.isPaused()
    }

    /**
     * 启动自动清理任务
     */
    private fun startAutoCleanup() {
        autoCleanupJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(60 * 1000) // 每分钟检查一次
                
                // 清理超过10分钟的旧视频
                val deletedCount = mediaRepository.deleteOldRecordings(10)
                if (deletedCount > 0) {
                    Log.d(TAG, "自动清理了 $deletedCount 个旧文件")
                }
                
                // 检查存储空间
                if (!mediaRepository.hasEnoughStorage(200)) { // 如果少于200MB
                    withContext(Dispatchers.Main) {
                        ttsService.speak("警告,存储空间严重不足")
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraRepository.release()
        ttsService.shutdown()
        voiceService?.release()
        // hapticService 不需要显示释放
        objectDetector.close()
        autoCleanupJob?.cancel()
    }
}
