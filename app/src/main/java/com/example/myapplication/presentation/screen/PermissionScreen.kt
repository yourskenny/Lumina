package com.example.myapplication.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(
    permissionState: MultiplePermissionsState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Lumina 需要相机和麦克风权限才能工作",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "请授予以下权限:\n1. 相机: 用于环境识别和录像\n2. 麦克风: 用于语音控制和录音\n3. 存储: 用于保存您的视频",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = { permissionState.launchMultiplePermissionRequest() }
        ) {
            Text(
                text = "授予权限",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
