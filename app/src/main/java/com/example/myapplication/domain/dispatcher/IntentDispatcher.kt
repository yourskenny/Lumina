package com.example.myapplication.domain.dispatcher

import com.example.myapplication.data.repository.CameraRepository
import com.example.myapplication.data.repository.MediaRepository
import com.example.myapplication.data.repository.MemoryRepository
import com.example.myapplication.domain.agent.AgentService
import com.example.myapplication.domain.feedback.FeedbackArbiter
import com.example.myapplication.domain.feedback.FeedbackPriority
import com.example.myapplication.domain.feedback.VibrationType
import com.example.myapplication.domain.service.VoiceCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 意图分发器
 * 负责将 VoiceCommand 路由到正确的处理者
 */
class IntentDispatcher(
    private val cameraRepository: CameraRepository,
    private val mediaRepository: MediaRepository,
    private val memoryRepository: MemoryRepository,
    private val agentService: AgentService,
    private val feedbackArbiter: FeedbackArbiter,
    private val scope: CoroutineScope // 传入 ViewModelScope 或 GlobalScope
) {

    // 定义回调，用于触发 UI 层无法直接处理的操作 (如退出 App, 拍照)
    // 拍照比较特殊，因为它依赖 CameraController 的回调，所以我们可能还是要在 ViewModel 中处理一部分逻辑
    // 或者我们将拍照逻辑完全封装在 CameraRepository 中
    
    // 为了简化，我们定义一个接口，ViewModel 实现它来处理必须在 ViewModel 中做的事
    interface ActionHandler {
        fun onCapturePhoto()
        fun onDescribeScene()
        fun onCloseApp()
    }
    
    private var actionHandler: ActionHandler? = null
    
    fun setActionHandler(handler: ActionHandler) {
        this.actionHandler = handler
    }

    /**
     * 分发语音命令
     */
    fun dispatch(command: VoiceCommand) {
        scope.launch {
            when (command) {
                VoiceCommand.CAPTURE_PHOTO -> {
                    feedbackArbiter.speak("正在拍照", FeedbackPriority.HIGH)
                    actionHandler?.onCapturePhoto()
                }
                VoiceCommand.PAUSE_RECORDING -> {
                    if (cameraRepository.isRecording()) {
                        cameraRepository.pauseRecording()
                        feedbackArbiter.vibrate(VibrationType.RECORDING_PAUSE)
                        feedbackArbiter.speak("录像已暂停", FeedbackPriority.HIGH)
                    }
                }
                VoiceCommand.RESUME_RECORDING -> {
                    if (cameraRepository.isPaused()) {
                        cameraRepository.resumeRecording()
                        feedbackArbiter.vibrate(VibrationType.RECORDING_START)
                        feedbackArbiter.speak("录像已恢复", FeedbackPriority.HIGH)
                    } else if (!cameraRepository.isRecording()) {
                        // 如果没有录像，则开始录像?
                        // 这里逻辑有点复杂，需要 MediaRepository 配合
                        // 暂时我们假设只能恢复暂停的
                        feedbackArbiter.speak("没有暂停的录像", FeedbackPriority.NORMAL)
                    }
                }
                VoiceCommand.CLEAR_RECORDINGS -> {
                    val count = withContext(Dispatchers.IO) {
                        mediaRepository.clearAllRecordings()
                    }
                    feedbackArbiter.speak("已清理 $count 个文件", FeedbackPriority.NORMAL)
                }
                VoiceCommand.CLOSE_APP -> {
                    feedbackArbiter.speak("正在退出", FeedbackPriority.HIGH)
                    delay(1000)
                    actionHandler?.onCloseApp()
                }
                VoiceCommand.DESCRIBE_SCENE -> {
                    memoryRepository.saveUserMessage("请描述当前场景")
                    actionHandler?.onDescribeScene()
                }
                else -> {
                    // 交给 Agent
                    // 理想情况下 VoiceCommand 应该包含 text，但现在只有 enum
                    // 所以这里只是一个 placeholder
                }
            }
        }
    }
    
    /**
     * 分发纯文本查询 (来自 ASR 的未知命令)
     */
    fun dispatchText(text: String) {
        scope.launch {
            // 1. 记录用户输入
            memoryRepository.saveUserMessage(text)
            
            // 2. 尝试本地规则匹配 (简单版)
            if (text.contains("拍照") || text.contains("拍张照")) {
                dispatch(VoiceCommand.CAPTURE_PHOTO)
                return@launch
            }
            
            // 3. 交给 Agent
            val response = agentService.chat(text)
            
            // 4. 记录 Agent 回复
            memoryRepository.saveAgentMessage(response)
            
            // 5. 播报
            feedbackArbiter.speak(response, FeedbackPriority.NORMAL)
        }
    }
}
