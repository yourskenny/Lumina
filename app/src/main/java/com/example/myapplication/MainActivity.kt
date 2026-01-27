package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.myapplication.data.repository.CameraRepository
import com.example.myapplication.data.repository.MediaRepository
import com.example.myapplication.domain.service.HapticFeedbackService
import com.example.myapplication.domain.service.TextToSpeechService
import com.example.myapplication.domain.service.VoiceCommand
import com.example.myapplication.domain.service.VoiceRecognitionService
import com.example.myapplication.domain.detector.ObjectDetector
import com.example.myapplication.presentation.screen.CameraScreen
import com.example.myapplication.presentation.viewmodel.CameraViewModel
import com.example.myapplication.ui.theme.MyApplicationTheme

/**
 * 主Activity
 * 无障碍相机应用的入口点
 */
class MainActivity : ComponentActivity() {

    // 服务实例
    private lateinit var ttsService: TextToSpeechService
    private lateinit var voiceService: VoiceRecognitionService
    private lateinit var hapticService: HapticFeedbackService
    private lateinit var cameraRepository: CameraRepository
    private lateinit var mediaRepository: MediaRepository
    private lateinit var objectDetector: ObjectDetector // 新增
    private lateinit var viewModel: CameraViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化服务
        initializeServices()

        // 初始化仓库
        initializeRepositories()
        
        // 初始化检测器
        objectDetector = ObjectDetector(this)

        // 初始化ViewModel
        initializeViewModel()

        // 设置内容
        setContent {
            MyApplicationTheme {
                CameraScreen(viewModel = viewModel)
            }
        }

        // 启动语音提示
        ttsService.speak("无障碍相机已启动")
    }

    /**
     * 初始化服务
     */
    private fun initializeServices() {
        // 初始化TTS服务
        ttsService = TextToSpeechService(this)

        // 初始化触觉反馈服务
        hapticService = HapticFeedbackService(this)

        // 初始化语音识别服务
        voiceService = VoiceRecognitionService(this) { command ->
            handleVoiceCommand(command)
        }
    }

    /**
     * 初始化仓库
     */
    private fun initializeRepositories() {
        cameraRepository = CameraRepository(this)
        mediaRepository = MediaRepository(this)
    }

    /**
     * 初始化ViewModel
     */
    private fun initializeViewModel() {
        viewModel = CameraViewModel(
            cameraRepository = cameraRepository,
            mediaRepository = mediaRepository,
            ttsService = ttsService,
            voiceService = voiceService,
            hapticService = hapticService,
            objectDetector = objectDetector
        )
    }

    /**
     * 处理语音命令
     */
    private fun handleVoiceCommand(command: VoiceCommand) {
        when (command) {
            VoiceCommand.CLOSE_APP -> {
                ttsService.speak("正在关闭应用", urgent = true)
                finish()
            }
            else -> {
                // 其他命令由ViewModel处理
                viewModel.handleVoiceCommand(command)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 恢复语音识别
        voiceService.startListening()
    }

    override fun onPause() {
        super.onPause()
        // 暂停语音识别
        voiceService.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放所有资源
        ttsService.shutdown()
        voiceService.release()
        cameraRepository.release()
        objectDetector.close() // 释放模型
    }
}
