package com.example.myapplication.domain.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * 触觉反馈服务
 * 提供不同类型的振动反馈,增强无障碍体验
 */
class HapticFeedbackService(private val context: Context) {

    private val TAG = "HapticFeedbackService"

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * 检查设备是否支持振动
     */
    fun hasVibrator(): Boolean {
        return vibrator.hasVibrator()
    }

    /**
     * 拍照反馈 - 单次短振(50ms)
     */
    fun feedbackCapture() {
        vibrate(50)
        Log.d(TAG, "拍照反馈")
    }

    /**
     * 开始录像反馈 - 两次短振(100ms + 100ms, 间隔100ms)
     */
    fun feedbackRecordingStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 100, 100, 100)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 100, 100), -1)
        }
        Log.d(TAG, "开始录像反馈")
    }

    /**
     * 暂停录像反馈 - 一次长振(200ms)
     */
    fun feedbackRecordingPause() {
        vibrate(200)
        Log.d(TAG, "暂停录像反馈")
    }

    /**
     * 错误反馈 - 三次快速短振(50ms × 3, 间隔50ms)
     */
    fun feedbackError() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 50, 50, 50, 50, 50)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 50, 50, 50, 50, 50), -1)
        }
        Log.d(TAG, "错误反馈")
    }

    /**
     * 成功反馈 - 两次短振,第二次稍长(50ms + 100ms, 间隔50ms)
     */
    fun feedbackSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 50, 50, 100)
            val amplitudes = intArrayOf(0, 200, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 50, 50, 100), -1)
        }
        Log.d(TAG, "成功反馈")
    }

    /**
     * 警告反馈 - 两次长振(150ms × 2, 间隔100ms)
     */
    fun feedbackWarning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 150, 100, 150)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 150, 100, 150), -1)
        }
        Log.d(TAG, "警告反馈")
    }

    /**
     * 通用振动
     * @param durationMs 振动时长(毫秒)
     */
    private fun vibrate(durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    /**
     * 取消所有振动
     */
    fun cancel() {
        vibrator.cancel()
        Log.d(TAG, "取消振动")
    }
}
