package com.example.myapplication.presentation.screen

import android.Manifest
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.CameraState
import com.example.myapplication.presentation.component.AccessibleButton
import com.example.myapplication.presentation.component.CameraPreview
import com.example.myapplication.presentation.component.DebugDrawer
import com.example.myapplication.presentation.component.DebugFab
import com.example.myapplication.presentation.viewmodel.CameraViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * 相机主界面
 * - 全屏相机预览
 * - 底部控制按钮(拍照、暂停/继续、关闭)
 * - 显示录像状态(时长、文件大小)
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var isDrawerOpen by remember { mutableStateOf(false) }

    // 请求必要的权限
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    // 检查权限状态
    val allPermissionsGranted = permissionsState.permissions.all { it.status.isGranted }

    // 当权限授予后初始化相机
    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted && !uiState.isPermissionGranted) {
            viewModel.onPermissionsGranted()
        }
    }

    // 当相机准备好后,初始化相机并开始自动录像
    LaunchedEffect(allPermissionsGranted, previewView) {
        if (allPermissionsGranted && previewView != null && uiState.cameraState is CameraState.Idle) {
            viewModel.initializeCamera(lifecycleOwner, previewView!!)
        }
    }

    // 当相机就绪后,自动开始录像
    LaunchedEffect(uiState.cameraState) {
        if (uiState.cameraState is CameraState.Ready) {
            viewModel.startAutoRecording()
        }
    }

    if (!allPermissionsGranted) {
        // 显示权限请求界面
        PermissionScreen(
            onPermissionsGranted = {
                viewModel.onPermissionsGranted()
            },
            onSpeakPermissionRationale = {
                // 这里可以添加语音播报权限说明的逻辑
            }
        )
    } else {
        // 显示相机界面
        Box(modifier = Modifier.fillMaxSize()) {
            // 相机预览
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onPreviewViewCreated = { preview ->
                    previewView = preview
                }
            )

            // 顶部状态栏
            StatusBar(
                cameraState = uiState.cameraState,
                durationSeconds = uiState.recordingStats.durationSeconds,
                fileSizeMB = uiState.recordingStats.fileSizeMB,
                totalSegments = uiState.recordingStats.totalSegments
            )

            // 右下角浮动按钮 - 打开控制面板
            DebugFab(
                onClick = { isDrawerOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 120.dp)
            )

            // 调试抽屉
            DebugDrawer(
                isVisible = isDrawerOpen,
                onDismiss = { isDrawerOpen = false },
                voiceText = uiState.voiceDebugText,
                volume = uiState.voiceVolume,
                isListening = uiState.isVoiceListening,
                voiceError = uiState.voiceError,
                currentLanguage = uiState.currentLanguage,
                isAudioRecording = uiState.isAudioRecording,
                audioRecordingCount = uiState.audioRecordingCount,
                // 开发者测试按钮
                onTestMicrophone = { viewModel.testMicrophone() },
                onSwitchLanguage = { viewModel.switchVoiceLanguage() },
                onTestRecognition = { viewModel.testRecognition() },
                onTestSimpleRecognition = { viewModel.testSimpleRecognition() },
                onToggleAudioRecording = { viewModel.toggleAudioRecording() },
                onClearAudioRecordings = { viewModel.clearAllAudioRecordings() },
                // 用户功能按钮
                onTakePhoto = { viewModel.testTextCommand("拍照") },
                onCheckBattery = { viewModel.testTextCommand("查询电池") },
                onPauseRecording = { viewModel.testTextCommand("暂停录像") },
                onResumeRecording = { viewModel.testTextCommand("继续录像") },
                onEmergencyCall = { viewModel.testTextCommand("紧急呼叫") }
            )

            // 底部控制按钮
            ControlButtons(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp),
                isRecording = uiState.cameraState is CameraState.Recording,
                isPaused = uiState.cameraState is CameraState.Paused,
                onCapturePhoto = { viewModel.capturePhotoWhileRecording() },
                onPauseResume = {
                    if (uiState.cameraState is CameraState.Recording) {
                        viewModel.pauseRecording()
                    } else {
                        viewModel.resumeRecording()
                    }
                },
                onClearRecordings = { viewModel.clearAllRecordings() }
            )
        }
    }
}

/**
 * 顶部状态栏
 */
@Composable
fun StatusBar(
    cameraState: CameraState,
    durationSeconds: Int,
    fileSizeMB: Float,
    totalSegments: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(16.dp)
            .semantics {
                contentDescription = "录像状态信息"
            }
    ) {
        val stateText = when (cameraState) {
            is CameraState.Idle -> "正在初始化..."
            is CameraState.Ready -> "准备就绪"
            is CameraState.Recording -> "录像中"
            is CameraState.Paused -> "已暂停"
            is CameraState.Error -> "错误: ${cameraState.message}"
        }

        Text(
            text = "状态: $stateText",
            fontSize = 20.sp,
            style = MaterialTheme.typography.titleMedium,
            color = if (cameraState is CameraState.Recording)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurface
        )

        if (cameraState is CameraState.Recording || cameraState is CameraState.Paused) {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            Text(
                text = "时长: ${String.format("%02d:%02d", minutes, seconds)}",
                fontSize = 18.sp,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "大小: ${String.format("%.1f", fileSizeMB)} MB",
                fontSize = 18.sp,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "片段: $totalSegments",
                fontSize = 18.sp,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * 底部控制按钮
 */
@Composable
fun ControlButtons(
    modifier: Modifier = Modifier,
    isRecording: Boolean,
    isPaused: Boolean,
    onCapturePhoto: () -> Unit,
    onPauseResume: () -> Unit,
    onClearRecordings: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 第一行：拍照和暂停/继续按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 拍照按钮
            AccessibleButton(
                text = "拍照",
                onClick = onCapturePhoto,
                contentDescriptionText = "在录像过程中拍摄照片"
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 暂停/继续按钮
            AccessibleButton(
                text = if (isRecording) "暂停" else "继续",
                onClick = onPauseResume,
                contentDescriptionText = if (isRecording) "暂停录像" else "继续录像"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 第二行：清空录像按钮
        AccessibleButton(
            text = "清空录像",
            onClick = onClearRecordings,
            contentDescriptionText = "清空所有录像和照片"
        )
    }
}

/**
 * 语音识别调试面板
 * 显示实时字幕、音量指示器和错误信息
 */
@Composable
fun VoiceDebugOverlay(
    modifier: Modifier = Modifier,
    voiceText: String,
    volume: Float,
    isListening: Boolean,
    error: String?,
    micTestResult: String?,
    isMicTesting: Boolean,
    currentLanguage: String,
    isRecognitionTesting: Boolean,
    testCommandResult: String?,
    isAudioRecording: Boolean,
    audioRecordingCount: Int,
    onTestMicrophone: () -> Unit,
    onSwitchLanguage: () -> Unit,
    onTestRecognition: () -> Unit,
    onTestSimpleRecognition: () -> Unit,
    onTestCommand: (String) -> Unit,
    onToggleAudioRecording: () -> Unit,
    onClearAudioRecordings: () -> Unit
) {
    // 总是显示调试面板（因为有按钮）
    // if (voiceText.isEmpty() && error == null && !isListening && micTestResult == null && !isMicTesting) {
    //     return
    // }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(16.dp)
            .semantics {
                contentDescription = "语音识别调试信息"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 监听状态指示器
        Text(
            text = if (isListening) "🎤 监听中..." else "🔇 未监听",
            fontSize = 16.sp,
            color = if (isListening)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 音量指示器（显示为进度条）
        if (isListening && volume > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "音量:",
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.width(8.dp))

                // 使用方块显示音量等级
                val volumeBars = (volume.toInt()).coerceIn(0, 10)
                Text(
                    text = "█".repeat(volumeBars) + "░".repeat(10 - volumeBars),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = " ${volume.toInt()}/10",
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 识别的文本（实时字幕）
        if (voiceText.isNotEmpty()) {
            Text(
                text = "识别: $voiceText",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )
        }

        // 错误信息
        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⚠️ $error",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // 麦克风测试结果
        if (micTestResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = micTestResult,
                fontSize = 16.sp,
                color = if (micTestResult.startsWith("✅"))
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // 当前语言显示
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "当前语言: $currentLanguage",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium
        )

        // 录音状态显示
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isAudioRecording) "🔴 正在录音" else "⚫ 未录音",
                fontSize = 16.sp,
                color = if (isAudioRecording)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "录音文件: $audioRecordingCount",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        // 按钮区域
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 测试麦克风按钮
            AccessibleButton(
                text = if (isMicTesting) "测试中..." else "测试麦克风",
                onClick = onTestMicrophone,
                contentDescriptionText = "测试麦克风是否正常工作",
                enabled = !isMicTesting && !isRecognitionTesting
            )

            // 切换语言按钮
            AccessibleButton(
                text = "切换语言",
                onClick = onSwitchLanguage,
                contentDescriptionText = "切换识别语言（中文/英文）",
                enabled = !isMicTesting && !isRecognitionTesting
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 测试识别按钮
            AccessibleButton(
                text = if (isRecognitionTesting) "识别中..." else "测试识别",
                onClick = onTestRecognition,
                contentDescriptionText = "测试语音识别功能",
                enabled = !isMicTesting && !isRecognitionTesting
            )

            // 极简测试按钮（诊断用）
            AccessibleButton(
                text = "极简测试",
                onClick = onTestSimpleRecognition,
                contentDescriptionText = "使用最简配置测试识别",
                enabled = !isMicTesting && !isRecognitionTesting
            )
        }

        // 录音控制按钮
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 开始/停止录音按钮
            AccessibleButton(
                text = if (isAudioRecording) "⏹ 停止录音" else "🎙 开始录音",
                onClick = onToggleAudioRecording,
                contentDescriptionText = if (isAudioRecording) "停止录音并保存" else "开始录音（同时识别）"
            )

            // 清空录音按钮
            AccessibleButton(
                text = "清空录音",
                onClick = onClearAudioRecordings,
                contentDescriptionText = "清空所有录音文件",
                enabled = audioRecordingCount > 0
            )
        }

        // 命令测试结果
        if (testCommandResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = testCommandResult,
                fontSize = 14.sp,
                color = if (testCommandResult.startsWith("✅"))
                    MaterialTheme.colorScheme.primary
                else if (testCommandResult.startsWith("❌"))
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // 分隔线
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "═══ 快捷命令测试 ═══",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall
        )

        // 快捷命令按钮
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AccessibleButton(
                text = "拍照",
                onClick = { onTestCommand("拍照") },
                contentDescriptionText = "测试拍照命令"
            )
            AccessibleButton(
                text = "查询电池",
                onClick = { onTestCommand("查询电池") },
                contentDescriptionText = "测试查询电池命令"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AccessibleButton(
                text = "暂停录像",
                onClick = { onTestCommand("暂停录像") },
                contentDescriptionText = "测试暂停录像命令"
            )
            AccessibleButton(
                text = "继续录像",
                onClick = { onTestCommand("继续录像") },
                contentDescriptionText = "测试继续录像命令"
            )
        }
    }
}
