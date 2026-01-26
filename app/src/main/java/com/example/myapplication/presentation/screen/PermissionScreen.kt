package com.example.myapplication.presentation.screen

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.presentation.component.AccessibleButton
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * 权限请求界面
 * 清晰地说明为什么需要权限,并提供授予权限的按钮
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(
    onPermissionsGranted: () -> Unit,
    onSpeakPermissionRationale: () -> Unit
) {
    // 请求相机、录音权限
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    // 当权限全部授予时,触发回调
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            onPermissionsGranted()
        }
    }

    // 首次加载时播报权限说明
    LaunchedEffect(Unit) {
        onSpeakPermissionRationale()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .semantics {
                contentDescription = "权限请求页面"
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "无障碍相机",
            fontSize = 32.sp,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "为了正常使用,本应用需要以下权限:",
            fontSize = 24.sp,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "• 相机权限:用于录像和拍照\n\n• 录音权限:用于录制视频声音和识别语音命令",
            fontSize = 20.sp,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        AccessibleButton(
            text = "授予权限",
            onClick = {
                permissionsState.launchMultiplePermissionRequest()
            },
            contentDescriptionText = "点击授予相机和录音权限"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 如果部分权限被拒绝,显示额外说明
        if (permissionsState.shouldShowRationale) {
            Text(
                text = "请在系统设置中授予权限",
                fontSize = 20.sp,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}
