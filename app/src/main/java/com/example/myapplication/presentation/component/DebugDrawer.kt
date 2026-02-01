package com.example.myapplication.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 调试抽屉组件
 * 包含开发者测试按钮和用户功能按钮
 */
@Composable
fun DebugDrawer(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    // 状态信息
    voiceText: String,
    volume: Float,
    isListening: Boolean,
    voiceError: String?,
    currentLanguage: String,
    isAudioRecording: Boolean,
    audioRecordingCount: Int,
    // 开发者测试按钮回调
    onTestMicrophone: () -> Unit,
    onSwitchLanguage: () -> Unit,
    onTestRecognition: () -> Unit,
    onTestSimpleRecognition: () -> Unit,
    onToggleAudioRecording: () -> Unit,
    onClearAudioRecordings: () -> Unit,
    // 用户功能按钮回调
    onTakePhoto: () -> Unit,
    onCheckBattery: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onPlayVideo: () -> Unit,
    onShareVideo: () -> Unit,
    onCheckStorage: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onCheckRecordingTime: () -> Unit,
    onCheckLocation: () -> Unit,
    onCloseApp: () -> Unit,
    onClearRecordings: () -> Unit,
    onEmergencyCall: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 半透明背景，点击关闭
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            // 右侧抽屉
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .align(Alignment.CenterEnd),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 标题栏
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "控制面板",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 可滚动内容
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 语音识别状态显示
                        VoiceStatusCard(
                            voiceText = voiceText,
                            volume = volume,
                            isListening = isListening,
                            voiceError = voiceError,
                            currentLanguage = currentLanguage,
                            isAudioRecording = isAudioRecording,
                            audioRecordingCount = audioRecordingCount
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 用户功能按钮区域
                        UserFunctionSection(
                            onTakePhoto = onTakePhoto,
                            onCheckBattery = onCheckBattery,
                            onPauseRecording = onPauseRecording,
                            onResumeRecording = onResumeRecording,
                            onPlayVideo = onPlayVideo,
                            onShareVideo = onShareVideo,
                            onCheckStorage = onCheckStorage,
                            onSwitchCamera = onSwitchCamera,
                            onToggleFlashlight = onToggleFlashlight,
                            onCheckRecordingTime = onCheckRecordingTime,
                            onCheckLocation = onCheckLocation,
                            onCloseApp = onCloseApp,
                            onClearRecordings = onClearRecordings,
                            onEmergencyCall = onEmergencyCall
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 开发者测试按钮区域
                        DeveloperTestSection(
                            onTestMicrophone = onTestMicrophone,
                            onSwitchLanguage = onSwitchLanguage,
                            onTestRecognition = onTestRecognition,
                            onTestSimpleRecognition = onTestSimpleRecognition,
                            onToggleAudioRecording = onToggleAudioRecording,
                            onClearAudioRecordings = onClearAudioRecordings,
                            isAudioRecording = isAudioRecording,
                            audioRecordingCount = audioRecordingCount
                        )
                    }
                }
            }
        }
    }
}

/**
 * 语音状态卡片
 */
@Composable
private fun VoiceStatusCard(
    voiceText: String,
    volume: Float,
    isListening: Boolean,
    voiceError: String?,
    currentLanguage: String,
    isAudioRecording: Boolean,
    audioRecordingCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "语音识别状态",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 监听状态
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isListening) Color.Green else Color.Gray,
                            shape = MaterialTheme.shapes.small
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isListening) "监听中" else "未监听",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // 语言
            Text(
                text = "语言: $currentLanguage",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // 音量
            if (volume > 0) {
                Text(
                    text = "音量: ${volume.toInt()}/10",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // 识别文本
            if (voiceText.isNotEmpty()) {
                Text(
                    text = "识别: $voiceText",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // 错误信息
            if (voiceError != null) {
                Text(
                    text = "错误: $voiceError",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // 录音状态
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isAudioRecording) Color.Red else Color.Gray,
                            shape = MaterialTheme.shapes.small
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAudioRecording) "录音中" else "未录音",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "文件: $audioRecordingCount",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * 用户功能按钮区域
 */
@Composable
private fun UserFunctionSection(
    onTakePhoto: () -> Unit,
    onCheckBattery: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onPlayVideo: () -> Unit,
    onShareVideo: () -> Unit,
    onCheckStorage: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onCheckRecordingTime: () -> Unit,
    onCheckLocation: () -> Unit,
    onCloseApp: () -> Unit,
    onClearRecordings: () -> Unit,
    onEmergencyCall: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "快捷功能",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 按钮网格
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 第1行：拍照 + 查询电池
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTakePhoto,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("拍照", fontSize = 12.sp)
                }
                Button(
                    onClick = onCheckBattery,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("查询电池", fontSize = 12.sp)
                }
            }

            // 第2行：暂停录像 + 继续录像
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPauseRecording,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("暂停录像", fontSize = 12.sp)
                }
                Button(
                    onClick = onResumeRecording,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("继续录像", fontSize = 12.sp)
                }
            }

            // 第3行：播放视频 + 分享视频
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPlayVideo,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("播放视频", fontSize = 12.sp)
                }
                Button(
                    onClick = onShareVideo,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("分享视频", fontSize = 12.sp)
                }
            }

            // 第4行：查询存储 + 查询时长
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCheckStorage,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("查询存储", fontSize = 12.sp)
                }
                Button(
                    onClick = onCheckRecordingTime,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("查询时长", fontSize = 12.sp)
                }
            }

            // 第5行：切换相机 + 切换闪光
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSwitchCamera,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("切换相机", fontSize = 12.sp)
                }
                Button(
                    onClick = onToggleFlashlight,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("切换闪光", fontSize = 12.sp)
                }
            }

            // 第6行：查询位置 + 关闭应用
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCheckLocation,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("查询位置", fontSize = 12.sp)
                }
                Button(
                    onClick = onCloseApp,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("关闭应用", fontSize = 12.sp)
                }
            }

            // 第7行：清空录像（全宽）
            Button(
                onClick = onClearRecordings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("清空录像", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 紧急呼叫按钮 - 使用红色突出显示
            Button(
                onClick = onEmergencyCall,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("🚨 紧急呼叫", fontSize = 16.sp)
            }
        }
    }
}

/**
 * 开发者测试按钮区域
 */
@Composable
private fun DeveloperTestSection(
    onTestMicrophone: () -> Unit,
    onSwitchLanguage: () -> Unit,
    onTestRecognition: () -> Unit,
    onTestSimpleRecognition: () -> Unit,
    onToggleAudioRecording: () -> Unit,
    onClearAudioRecordings: () -> Unit,
    isAudioRecording: Boolean,
    audioRecordingCount: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "开发者工具",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 按钮网格
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTestMicrophone,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("测试麦克风", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onSwitchLanguage,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("切换语言", fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTestRecognition,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("测试识别", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onTestSimpleRecognition,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("极简测试", fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleAudioRecording,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (isAudioRecording) "停止录音" else "开始录音",
                        fontSize = 12.sp
                    )
                }
                OutlinedButton(
                    onClick = onClearAudioRecordings,
                    modifier = Modifier.weight(1f),
                    enabled = audioRecordingCount > 0
                ) {
                    Text("清空录音", fontSize = 12.sp)
                }
            }
        }
    }
}
