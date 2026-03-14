package com.example.myapplication.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.repository.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    val provider by settingsRepository.provider.collectAsState(initial = "deepseek")
    val apiKey by settingsRepository.apiKey.collectAsState(initial = "")
    val baseUrl by settingsRepository.baseUrl.collectAsState(initial = "")
    val modelName by settingsRepository.modelName.collectAsState(initial = "")
    val vlmApiKey by settingsRepository.vlmApiKey.collectAsState(initial = "")

    var inputProvider by remember(provider) { mutableStateOf(provider) }
    var inputApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    var inputBaseUrl by remember(baseUrl) { mutableStateOf(baseUrl) }
    var inputModelName by remember(modelName) { mutableStateOf(modelName) }
    var inputVlmApiKey by remember(vlmApiKey) { mutableStateOf(vlmApiKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("API 配置", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = inputProvider,
            onValueChange = { inputProvider = it },
            label = { Text("Provider (deepseek/openai)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = inputBaseUrl,
            onValueChange = { inputBaseUrl = it },
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = inputApiKey,
            onValueChange = { inputApiKey = it },
            label = { Text("LLM API Key") },
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = inputModelName,
            onValueChange = { inputModelName = it },
            label = { Text("Model Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("视觉模型 (Gemini)", style = MaterialTheme.typography.titleMedium)
        
        OutlinedTextField(
            value = inputVlmApiKey,
            onValueChange = { inputVlmApiKey = it },
            label = { Text("Gemini API Key") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    settingsRepository.updateSettings(
                        inputProvider,
                        inputApiKey,
                        inputBaseUrl,
                        inputModelName,
                        inputVlmApiKey
                    )
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存并返回")
        }
    }
}
