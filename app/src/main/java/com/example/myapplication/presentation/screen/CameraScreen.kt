package com.example.myapplication.presentation.screen

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.CameraState
import com.example.myapplication.data.model.RecordingStats
import com.example.myapplication.presentation.component.AccessibleButton
import com.example.myapplication.presentation.viewmodel.CameraViewModel
import com.example.myapplication.ui.theme.AlertRed
import com.example.myapplication.ui.theme.RecordingRed

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraState by viewModel.cameraState.collectAsState(initial = CameraState.Idle)
    val recordingStats by viewModel.recordingStats.collectAsState(initial = RecordingStats())
    val detectedObjects by viewModel.detectedObjects.collectAsState(initial = emptyList())
    
    // 控制设置页面显示
    var showSettings by remember { mutableStateOf(false) }
    
    if (showSettings) {
        Dialog(onDismissRequest = { showSettings = false }) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                SettingsScreen(
                    settingsRepository = viewModel.getSettingsRepository(),
                    onBack = { showSettings = false }
                )
            }
        }
    }

    // 相机预览
    Box(modifier = Modifier.fillMaxSize()) {
        val previewView = remember { PreviewView(context) }
        
        AndroidView(
            factory = { 
                previewView.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                // ImageAnalysis 负责处理图像，这里不需要做任何事
            }
        )
        
        LaunchedEffect(lifecycleOwner) {
            viewModel.initializeCamera(lifecycleOwner, previewView)
        }

        // 绘制检测框
        Canvas(modifier = Modifier.fillMaxSize()) {
            detectedObjects.forEach { obj ->
                val box = obj.box
                if (box.size >= 4) {
                    val x1 = box[0].toFloat()
                    val y1 = box[1].toFloat()
                    val x2 = box[2].toFloat()
                    val y2 = box[3].toFloat()
                    
                    val width = x2 - x1
                    val height = y2 - y1
                    
                    // 根据距离决定颜色
                    val color = when {
                        obj.distanceM < 1.5 -> Color.Red
                        obj.distanceM < 3.0 -> Color.Yellow
                        else -> Color.Green
                    }
                    
                    // 绘制矩形框
                    drawRect(
                        color = color,
                        topLeft = Offset(x1, y1),
                        size = Size(width, height),
                        style = Stroke(width = 8f) // 加粗线条，方便看清
                    )
                    
                    // 绘制标签 (使用原生Canvas绘制文字)
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            this.color = android.graphics.Color.WHITE
                            this.textSize = 48f
                            this.isFakeBoldText = true
                            this.setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                        }
                        
                        val label = "${obj.name} ${String.format("%.1fm", obj.distanceM)}"
                        drawText(label, x1, y1 - 10f, paint)
                    }
                }
            }
        }
        
        // 录像状态指示器
        if (recordingStats.isRecording) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .background(RecordingRed.copy(alpha = 0.8f), MaterialTheme.shapes.medium)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "正在录像 ${formatDuration(recordingStats.durationSeconds)}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        
        // 底部控制栏
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 设置按钮
            IconButton(
                onClick = { showSettings = true },
                modifier = Modifier.size(64.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // 语音按钮
            AccessibleButton(
                onClick = { /* 长按触发 */ },
                onLongClick = { viewModel.startVoiceListening() },
                modifier = Modifier.padding(16.dp),
                contentDescription = "按住说话",
                icon = Icons.Default.Mic,
                backgroundColor = MaterialTheme.colorScheme.secondary
            )
            
            // 拍照/录像按钮
            if (recordingStats.isRecording) {
                AccessibleButton(
                    onClick = { viewModel.stopRecording() },
                    modifier = Modifier.size(96.dp),
                    contentDescription = "停止录像",
                    icon = Icons.Default.VideocamOff,
                    backgroundColor = RecordingRed
                )
            } else {
                AccessibleButton(
                    onClick = { viewModel.capturePhoto() },
                    onLongClick = { viewModel.startRecording() },
                    modifier = Modifier.size(96.dp),
                    contentDescription = "点击拍照，长按录像",
                    icon = Icons.Default.PhotoCamera,
                    backgroundColor = MaterialTheme.colorScheme.primary
                )
            }
            
            // 暂停/恢复按钮 (仅录像时显示)
            if (recordingStats.isRecording) {
                AccessibleButton(
                    onClick = { 
                        if (viewModel.isPaused()) viewModel.resumeRecording() 
                        else viewModel.pauseRecording() 
                    },
                    modifier = Modifier.padding(16.dp),
                    contentDescription = if (viewModel.isPaused()) "恢复录像" else "暂停录像",
                    icon = Icons.Default.Videocam, 
                    backgroundColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
