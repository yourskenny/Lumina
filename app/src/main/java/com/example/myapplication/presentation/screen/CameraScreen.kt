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
