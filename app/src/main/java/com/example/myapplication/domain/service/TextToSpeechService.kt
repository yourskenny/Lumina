package com.example.myapplication.domain.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * 语音合成服务
 * 提供文本转语音功能,用于无障碍语音反馈
 */
class TextToSpeechService(context: Context) {

    private val TAG = "TextToSpeechService"

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // 设置中文语言
                var result = tts?.setLanguage(Locale.CHINESE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "不支持中文TTS, 尝试英文")
                    result = tts?.setLanguage(Locale.US)
                }

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS初始化失败: 语言不支持")
                    isInitialized = false
                } else {
                    // 设置语速(稍慢,便于理解)
                    tts?.setSpeechRate(0.9f)

                    // 设置音调
                    tts?.setPitch(1.0f)

                    isInitialized = true
                    Log.d(TAG, "TTS初始化成功")
                }
            } else {
                Log.e(TAG, "TTS初始化失败")
            }
        }

        // 设置监听器
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "开始播报: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "播报完成: $utteranceId")
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "播报错误: $utteranceId")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "播报错误: $utteranceId, 错误码: $errorCode")
            }
        })
    }

    /**
     * 播报文本
     * @param text 要播报的文本
     * @param urgent 是否紧急(紧急消息会打断当前播报)
     */
    fun speak(text: String, urgent: Boolean = false) {
        if (!isInitialized) {
            Log.w(TAG, "TTS未初始化,无法播报")
            return
        }

        val queueMode = if (urgent) {
            TextToSpeech.QUEUE_FLUSH // 打断当前播报
        } else {
            TextToSpeech.QUEUE_ADD // 加入队列
        }

        tts?.speak(text, queueMode, null, text.hashCode().toString())
        Log.d(TAG, "播报文本: $text")
    }

    /**
     * 停止播报
     */
    fun stop() {
        tts?.stop()
        Log.d(TAG, "停止播报")
    }

    /**
     * 检查TTS是否正在播报
     */
    fun isSpeaking(): Boolean {
        return tts?.isSpeaking ?: false
    }

    /**
     * 释放TTS资源
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        Log.d(TAG, "TTS资源已释放")
    }

    /**
     * 检查TTS是否可用
     */
    fun isAvailable(): Boolean {
        return isInitialized
    }
}
