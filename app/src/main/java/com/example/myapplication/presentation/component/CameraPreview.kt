package com.example.myapplication.presentation.component

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 相机预览组件
 * 使用AndroidView包装CameraX的PreviewView
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onPreviewViewCreated: (PreviewView) -> Unit
) {
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                onPreviewViewCreated(this)
            }
        },
        modifier = modifier.semantics {
            contentDescription = "相机预览画面"
        }
    )
}
