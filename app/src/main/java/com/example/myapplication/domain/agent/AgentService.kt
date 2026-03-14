package com.example.myapplication.domain.agent

import android.content.Context
import com.example.myapplication.data.repository.MemoryRepository
import com.example.myapplication.data.repository.SettingsRepository
import com.example.myapplication.domain.agent.tools.AndroidTools
import com.example.myapplication.domain.context.ContextManager
import com.example.myapplication.domain.feedback.FeedbackArbiter
import com.example.myapplication.domain.feedback.FeedbackPriority
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.SystemMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 智能体服务
 * 使用LangChain4j编排LLM和Tools
 */
import com.example.myapplication.data.repository.AMapRepository
import com.example.myapplication.domain.service.LocationService

class AgentService(
    private val context: Context,
    private val memoryRepository: MemoryRepository,
    private val settingsRepository: SettingsRepository,
    private val contextManager: ContextManager, 
    private val feedbackArbiter: FeedbackArbiter,
    private val locationService: LocationService, // New dependency
    private val amapRepository: AMapRepository    // New dependency
) {

    interface Assistant {
        @SystemMessage("你是一个视障人士的智能助手。你可以调用工具来获取信息、震动手机或语音播报。请根据用户的历史对话和当前场景描述来回答。如果场景有危险，请立即震动并警告。请务必参考[当前环境状态]来回答与环境相关的问题。")
        fun chat(userMessage: String): String
    }

    private var assistant: Assistant? = null
    private var lastConfigHash: String = ""

    private suspend fun ensureAssistantInitialized() {
        val provider = settingsRepository.provider.first()
        val apiKey = settingsRepository.apiKey.first()
        val baseUrl = settingsRepository.baseUrl.first()
        val modelName = settingsRepository.modelName.first()
        
        val currentHash = "$provider-$apiKey-$baseUrl-$modelName"
        
        if (assistant == null || currentHash != lastConfigHash) {
            if (apiKey.isBlank()) {
                assistant = null
                return
            }
            
            try {
                // 目前仅支持 OpenAI 兼容接口 (DeepSeek 也支持)
                val model = OpenAiChatModel.builder()
                    .apiKey(apiKey) 
                    .baseUrl(baseUrl) 
                    .modelName(modelName)
                    .build()

                assistant = AiServices.builder(Assistant::class.java)
                    .chatLanguageModel(model)
                    .tools(AndroidTools(context, feedbackArbiter, locationService, amapRepository))
                    .build()
                
                lastConfigHash = currentHash
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun chat(query: String): String {
        ensureAssistantInitialized()
        return withContext(Dispatchers.IO) {
            try {
                // 获取最近的记忆作为上下文
                val recentMessages = memoryRepository.getRecentContext(5)
                val contextString = recentMessages.joinToString("\n") { "${it.role}: ${it.content}" }
                
                // 获取实时环境上下文 (YOLO, Location, Battery)
                val realTimeContext = contextManager.getContextPrompt()

                val fullPrompt = """
                    $realTimeContext

                    历史对话:
                    $contextString
                    
                    用户: $query
                """.trimIndent()
                
                assistant?.chat(fullPrompt) ?: "智能体未配置 (请在设置中配置 API Key)"
            } catch (e: Exception) {
                "思考过程中出错: ${e.message}"
            }
        }
    }
    
    /**
     * 分析场景 (Proactive Trigger)
     */
    suspend fun analyzeScene(description: String) {
        ensureAssistantInitialized()
        withContext(Dispatchers.IO) {
            try {
                val prompt = "当前场景描述: $description。请分析是否有危险或值得注意的事项。如果有，请调用工具进行反馈。"
                assistant?.chat(prompt)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun isAvailable(): Boolean {
        return assistant != null
    }
}
