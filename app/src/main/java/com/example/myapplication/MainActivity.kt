package com.example.myapplication

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.myapplication.presentation.screen.CameraScreen
import com.example.myapplication.presentation.screen.PermissionScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 根据Android版本动态构建权限列表
                    val permissions = remember {
                        val basePermissions = mutableListOf(
                            android.Manifest.permission.CAMERA,
                            android.Manifest.permission.RECORD_AUDIO,
                            android.Manifest.permission.ACCESS_FINE_LOCATION, // 高德定位
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        
                        // Android 13 (API 33) 及以上使用细分媒体权限
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            basePermissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                            basePermissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
                            // 音频权限视需求而定，暂时加上以防万一
                            // basePermissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)
                        } 
                        // Android 10 (API 29) 到 Android 12 (API 32)
                        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            basePermissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            // Android 10+ 不需要 WRITE_EXTERNAL_STORAGE 来写入 MediaStore
                        }
                        // Android 9 (API 28) 及以下
                        else {
                            basePermissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            basePermissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                        
                        basePermissions.toList()
                    }
                    
                    val permissionState = rememberMultiplePermissionsState(permissions)

                    if (permissionState.allPermissionsGranted) {
                        CameraScreen()
                    } else {
                        PermissionScreen(
                            permissionState = permissionState
                        )
                    }
                }
            }
        }
    }
}
