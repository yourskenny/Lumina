package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.myapplication.data.repository.CameraRepository
import com.example.myapplication.data.repository.MediaRepository
import com.example.myapplication.data.repository.VideoPlaybackRepository
import com.example.myapplication.data.repository.StorageManagementRepository
import com.example.myapplication.data.repository.SettingsRepository
import com.example.myapplication.data.repository.LocationRepository
import com.example.myapplication.domain.service.CameraForegroundService
import com.example.myapplication.domain.service.EmergencyService
import com.example.myapplication.domain.service.HapticFeedbackService
import com.example.myapplication.domain.service.TextToSpeechService
import com.example.myapplication.domain.service.VoiceCommand
import com.example.myapplication.domain.service.VoiceRecognitionService
import com.example.myapplication.presentation.screen.CameraScreen
import com.example.myapplication.presentation.viewmodel.CameraViewModel
import com.example.myapplication.ui.theme.MyApplicationTheme

/**
 * 主Activity
 * 无障碍相机应用的入口点
 */
class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    // 服务实例
    private lateinit var ttsService: TextToSpeechService
    private lateinit var voiceService: VoiceRecognitionService
    private lateinit var hapticService: HapticFeedbackService

    // 仓库实例
    private lateinit var cameraRepository: CameraRepository
    private lateinit var mediaRepository: MediaRepository
    private lateinit var videoPlaybackRepository: VideoPlaybackRepository
    private lateinit var storageManagementRepository: StorageManagementRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var locationRepository: LocationRepository

    // 紧急服务
    private lateinit var emergencyService: EmergencyService

    private lateinit var viewModel: CameraViewModel

    private var isForegroundServiceRunning = false

    // 权限请求
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Log.d(TAG, "所有权限已授予")
            ttsService.speak("权限已授予，可以开始使用语音命令")
            voiceService.startListening()
        } else {
            Log.e(TAG, "部分权限未授予: ${permissions.filter { !it.value }}")
            ttsService.speak("警告：麦克风权限未授予，无法使用语音命令")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化服务
        initializeServices()

        // 初始化仓库
        initializeRepositories()

        // 初始化ViewModel
        initializeViewModel()

        // 设置内容
        setContent {
            MyApplicationTheme {
                CameraScreen(viewModel = viewModel)
            }
        }

        // 检查权限
        checkPermissions()

        // 启动语音提示
        ttsService.speak("无障碍相机已启动")
    }

    /**
     * 检查必要权限
     */
    private fun checkPermissions() {
        val requiredPermissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        // Android 13+ 需要通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "请求权限: $permissionsToRequest")
            ttsService.speak("请授予麦克风和相机权限")
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d(TAG, "所有权限已授予")
        }
    }

    /**
     * 初始化服务
     */
    private fun initializeServices() {
        // 初始化TTS服务
        ttsService = TextToSpeechService(this)

        // 初始化触觉反馈服务
        hapticService = HapticFeedbackService(this)

        // 初始化语音识别服务，带调试信息回调
        voiceService = VoiceRecognitionService(
            context = this,
            onCommandDetected = { command ->
                handleVoiceCommand(command)
            },
            onDebugInfo = { debugInfo ->
                handleDebugInfo(debugInfo)
            }
        )
    }

    /**
     * 处理调试信息
     */
    private fun handleDebugInfo(debugInfo: com.example.myapplication.domain.service.VoiceDebugInfo) {
        // 记录调试信息
        if (debugInfo.error != null) {
            Log.e(TAG, "语音识别: ${debugInfo.error}")
        } else if (debugInfo.text.isNotEmpty()) {
            Log.d(TAG, "语音识别: ${debugInfo.text} | 音量: ${debugInfo.volume} | 部分结果: ${debugInfo.isPartial}")
        }

        // 更新ViewModel中的调试信息（如果存在）
        if (::viewModel.isInitialized) {
            viewModel.updateVoiceDebugInfo(debugInfo)
        }
    }

    /**
     * 初始化仓库
     */
    private fun initializeRepositories() {
        cameraRepository = CameraRepository(this)
        mediaRepository = MediaRepository(this)
        videoPlaybackRepository = VideoPlaybackRepository(this)
        storageManagementRepository = StorageManagementRepository(this)
        settingsRepository = SettingsRepository(this)
        locationRepository = LocationRepository(this)

        // 初始化紧急服务（依赖 settingsRepository）
        emergencyService = EmergencyService(this, settingsRepository)
    }

    /**
     * 初始化ViewModel
     */
    private fun initializeViewModel() {
        viewModel = CameraViewModel(
            context = this,
            cameraRepository = cameraRepository,
            mediaRepository = mediaRepository,
            videoPlaybackRepository = videoPlaybackRepository,
            storageManagementRepository = storageManagementRepository,
            settingsRepository = settingsRepository,
            locationRepository = locationRepository,
            emergencyService = emergencyService,
            ttsService = ttsService,
            voiceService = voiceService,
            hapticService = hapticService
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

        // 启动前台服务
        startForegroundService()
    }

    /**
     * 启动前台服务
     */
    private fun startForegroundService() {
        if (!isForegroundServiceRunning) {
            try {
                val intent = Intent(this, CameraForegroundService::class.java).apply {
                    action = CameraForegroundService.ACTION_START_FOREGROUND
                    putExtra(CameraForegroundService.EXTRA_STATUS_TEXT, "录像运行中")
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(this, intent)
                } else {
                    startService(intent)
                }

                isForegroundServiceRunning = true
                Log.d(TAG, "前台服务启动成功")
            } catch (e: Exception) {
                Log.w(TAG, "前台服务启动失败（这不会影响应用功能）: ${e.message}")
                // 不影响应用继续运行，前台服务只是为了保持后台录像
                isForegroundServiceRunning = false
            }
        }
    }

    /**
     * 停止前台服务
     */
    private fun stopForegroundService() {
        if (isForegroundServiceRunning) {
            val intent = Intent(this, CameraForegroundService::class.java).apply {
                action = CameraForegroundService.ACTION_STOP_FOREGROUND
            }
            startService(intent)
            isForegroundServiceRunning = false
        }
    }

    override fun onPause() {
        super.onPause()
        // 暂停语音识别
        voiceService.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()

        // 停止前台服务
        stopForegroundService()

        // 释放所有资源
        ttsService.shutdown()
        voiceService.release()
        cameraRepository.release()
    }
}
