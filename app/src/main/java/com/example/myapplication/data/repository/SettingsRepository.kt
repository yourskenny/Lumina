package com.example.myapplication.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.myapplication.BuildConfig

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 设置仓库
 * 管理API配置 (Provider, Key, URL, Model)
 */
class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_PROVIDER = stringPreferencesKey("provider") // "gemini", "openai", "deepseek"
        val KEY_API_KEY = stringPreferencesKey("api_key")
        val KEY_BASE_URL = stringPreferencesKey("base_url")
        val KEY_MODEL_NAME = stringPreferencesKey("model_name")
        val KEY_VLM_API_KEY = stringPreferencesKey("vlm_api_key") // 专门用于 VLM (Gemini) 的 Key
    }

    val provider: Flow<String> = context.dataStore.data.map { it[KEY_PROVIDER] ?: "dashscope" } 
    val apiKey: Flow<String> = context.dataStore.data.map { it[KEY_API_KEY] ?: BuildConfig.DASHSCOPE_API_KEY } 
    val baseUrl: Flow<String> = context.dataStore.data.map { it[KEY_BASE_URL] ?: "https://coding.dashscope.aliyuncs.com/v1" } // Coding Plan 专属
    val modelName: Flow<String> = context.dataStore.data.map { it[KEY_MODEL_NAME] ?: "qwen3.5-plus" } // 默认模型 qwen3.5-plus
    
    val vlmApiKey: Flow<String> = context.dataStore.data.map { it[KEY_VLM_API_KEY] ?: BuildConfig.DASHSCOPE_API_KEY }

    suspend fun updateSettings(
        provider: String,
        apiKey: String,
        baseUrl: String,
        modelName: String,
        vlmApiKey: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PROVIDER] = provider
            preferences[KEY_API_KEY] = apiKey
            preferences[KEY_BASE_URL] = baseUrl
            preferences[KEY_MODEL_NAME] = modelName
            preferences[KEY_VLM_API_KEY] = vlmApiKey
        }
    }
}
