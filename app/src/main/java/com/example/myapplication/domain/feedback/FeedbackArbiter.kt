package com.example.myapplication.domain.feedback

import android.util.Log
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
    private val TAG = "FeedbackArbiter"
    
    // 记录当前正在播放的优先级 (简化处理，实际上 TTS Service 内部也有队列)
    // 这里主要控制是否打断
    private var currentPriority = FeedbackPriority.LOW

    /**
     * 请求语音播报
     */
    fun speak(text: String, priority: FeedbackPriority = FeedbackPriority.NORMAL) {
        try {
            Log.d(TAG, "收到播报请求: '$text' [Priority: $priority]")
            val isUrgent = priority == FeedbackPriority.CRITICAL || priority == FeedbackPriority.HIGH
            
            ttsService.speak(text, urgent = isUrgent)
            currentPriority = priority
        } catch (e: Exception) {
            Log.e(TAG, "TTS 播报失败", e)
        }
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
