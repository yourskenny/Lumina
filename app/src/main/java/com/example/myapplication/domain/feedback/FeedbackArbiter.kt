package com.example.myapplication.domain.feedback

import com.example.myapplication.domain.service.HapticFeedbackService
import com.example.myapplication.domain.service.TextToSpeechService

/**
 * 反馈优先级
 */
enum class FeedbackPriority {
    CRITICAL, // 紧急 (避障警告): 立即打断当前播报
    HIGH,     // 高 (用户指令响应): 排队优先
    NORMAL,   // 普通 (环境描述): 排队
    LOW       // 低 (后台状态): 可丢弃
}

/**
 * 反馈仲裁者
 * 统一管理 TTS 和 Haptic 反馈，防止冲突和信息过载
 */
class FeedbackArbiter(
    private val ttsService: TextToSpeechService,
    private val hapticService: HapticFeedbackService
) {

    // 记录当前正在播放的优先级 (简化处理，实际上 TTS Service 内部也有队列)
    // 这里主要控制是否打断
    private var currentPriority = FeedbackPriority.LOW

    /**
     * 请求语音播报
     */
    fun speak(text: String, priority: FeedbackPriority = FeedbackPriority.NORMAL) {
        val urgent = priority == FeedbackPriority.CRITICAL
        // 如果是 CRITICAL，强制打断
        // 如果是 HIGH，也打断 NORMAL/LOW，但如果当前是 CRITICAL 则不打断 (需要更复杂的逻辑，目前简化为 urgent=true 即打断)
        
        // 为了简化，我们只在 CRITICAL 时使用 urgent=true (QUEUE_FLUSH)
        // HIGH/NORMAL/LOW 都使用 urgent=false (QUEUE_ADD)
        // 这样可以保证紧急消息立即播报，其他消息排队
        
        // 但如果想让 HIGH 插队到 NORMAL 前面，Android TTS API 原生不支持插队，只能 FLUSH 或 ADD。
        // 所以我们可能需要自己维护一个 PriorityQueue，但这比较复杂。
        // 目前策略：
        // CRITICAL -> FLUSH (打断一切)
        // HIGH -> ADD (排队)
        // NORMAL -> ADD (排队)
        // LOW -> ADD (排队)
        
        // 修正：如果当前正在播报 LOW/NORMAL，HIGH 应该打断吗？
        // 如果我们希望 HIGH (如"拍照成功") 立即反馈，那么它应该也是 urgent。
        
        val isUrgent = priority == FeedbackPriority.CRITICAL || priority == FeedbackPriority.HIGH
        
        ttsService.speak(text, urgent = isUrgent)
        currentPriority = priority
    }

    /**
     * 请求震动反馈
     */
    fun vibrate(type: VibrationType, priority: FeedbackPriority = FeedbackPriority.NORMAL) {
        // 震动通常是非阻塞的，可以并行，但为了避免震动太频繁，也可以加逻辑
        // 这里简化为直接调用
        when (type) {
            VibrationType.SUCCESS -> hapticService.feedbackSuccess()
            VibrationType.WARNING -> hapticService.feedbackWarning()
            VibrationType.ERROR -> hapticService.feedbackError()
            VibrationType.CAPTURE -> hapticService.feedbackCapture()
            VibrationType.RECORDING_START -> hapticService.feedbackRecordingStart()
            VibrationType.RECORDING_PAUSE -> hapticService.feedbackRecordingPause()
        }
    }
    
    /**
     * 停止所有反馈
     */
    fun stopAll() {
        ttsService.stop()
        currentPriority = FeedbackPriority.LOW
    }
}

enum class VibrationType {
    SUCCESS, WARNING, ERROR, CAPTURE, RECORDING_START, RECORDING_PAUSE
}
