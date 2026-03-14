package com.example.myapplication.domain.service

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.myapplication.data.repository.SettingsRepository
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.data.message.ImageContent
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.model.openai.OpenAiChatModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * 生成式AI服务
 * 负责与多模态 VLM (如 Qwen-VL, Gemini via OpenAI adapter) 交互
 */
class GenerativeAIService(private val settingsRepository: SettingsRepository) {

    private val TAG = "GenerativeAIService"
    private var chatModel: OpenAiChatModel? = null
    private var lastApiKey: String = ""

    // 每次调用前尝试刷新模型配置
    private suspend fun ensureModelInitialized() {
        val apiKey = settingsRepository.vlmApiKey.first()
        // 使用配置中的 baseUrl，如果未配置则使用默认的 Coding Plan URL
        val baseUrl = settingsRepository.baseUrl.first() 
        val modelName = settingsRepository.modelName.first() // 使用配置的模型名

        if (apiKey.isNotBlank()) {
            if (chatModel == null || lastApiKey != apiKey) {
                try {
                    chatModel = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .build()
                    lastApiKey = apiKey
                    Log.d(TAG, "VLM服务初始化成功: $modelName")
                } catch (e: Exception) {
                    Log.e(TAG, "VLM服务初始化失败", e)
                }
            }
        } else {
            chatModel = null
            Log.e(TAG, "未配置 VLM API Key")
        }
    }

    /**
     * 生成内容描述
     * @param bitmap 图像
     * @param detectedObjects 本地模型检测到的物体标签
     * @param prompt 提示词
     * @return AI生成的文本描述
     */
    suspend fun generateDescription(
        bitmap: Bitmap, 
        detectedObjects: List<String> = emptyList(),
        prompt: String = "Describe this scene for a blind person"
    ): String {
        ensureModelInitialized()
        val model = chatModel ?: return "AI服务未配置"

        return withContext(Dispatchers.IO) {
            try {
                // 1. 将 Bitmap 转换为 Base64
                val base64Image = bitmapToBase64(bitmap)
                
                // 2. 构建增强型 Prompt
                val enhancedPrompt = if (detectedObjects.isNotEmpty()) {
                    val objectsStr = detectedObjects.joinToString(", ")
                    "$prompt. I have locally detected these objects: [$objectsStr]. Please verify them and describe their spatial relationships and actions."
                } else {
                    prompt
                }
                
                // 3. 构建多模态消息
                val userMessage = UserMessage.from(
                    TextContent.from(enhancedPrompt),
                    ImageContent.from(base64Image, "image/jpeg")
                )

                // 4. 调用模型
                val response = model.generate(userMessage)
                response.content().text() ?: "无法生成描述"
            } catch (e: Exception) {
                Log.e(TAG, "AI推理失败", e)
                "识别失败: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Bitmap 转 Base64
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // 压缩图片以减少 Token 消耗和网络传输
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * 检查服务是否可用
     */
    fun isAvailable(): Boolean {
        val apiKey = runBlocking { settingsRepository.vlmApiKey.first() }
        return apiKey.isNotBlank()
    }
}
