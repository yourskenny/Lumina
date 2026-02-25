package com.example.myapplication.presentation.screen

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.domain.detector.ObjectDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AITestScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var testResult by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val detector = remember { ObjectDetector(context) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        testResult = ""
        bitmap = null

        uri?.let {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                } catch (e: Exception) {
                    testResult = "图片加载失败: ${e.message}"
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            detector.close()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AI 物体检测测试",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("选择测试图片")
        }

        Spacer(modifier = Modifier.height(16.dp))

        bitmap?.let { bmp ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Test Image",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        isProcessing = true
                        testResult = "正在处理..."

                        withContext(Dispatchers.Default) {
                            try {
                                val startTime = System.currentTimeMillis()
                                val result = detector.detect(bmp)
                                val endTime = System.currentTimeMillis()
                                val inferenceTime = endTime - startTime

                                val sb = StringBuilder()
                                sb.append("✅ 检测完成！\n")
                                sb.append("⏱️ 推理时间: ${inferenceTime}ms\n\n")

                                sb.append("🚨 危险物体 (${result.hazards.size}):\n")
                                if (result.hazards.isEmpty()) {
                                    sb.append("  无\n")
                                } else {
                                    result.hazards.forEachIndexed { index, obj ->
                                        sb.append("  ${index + 1}. ${obj.name}\n")
                                        sb.append("     距离: ${String.format("%.1f", obj.distanceM)}m\n")
                                        sb.append("     方向: ${obj.direction}\n")
                                        sb.append("     坐标: [${obj.box.map { String.format("%.1f", it) }.joinToString(", ")}]\n")
                                    }
                                }

                                sb.append("\n🛤️ 路径信息 (${result.paths.size}):\n")
                                if (result.paths.isEmpty()) {
                                    sb.append("  无\n")
                                } else {
                                    result.paths.forEachIndexed { index, obj ->
                                        sb.append("  ${index + 1}. ${obj.name}\n")
                                        sb.append("     距离: ${String.format("%.1f", obj.distanceM)}m\n")
                                        sb.append("     方向: ${obj.direction}\n")
                                    }
                                }

                                testResult = sb.toString()
                            } catch (e: Exception) {
                                testResult = "❌ 检测失败: ${e.message}\n${e.stackTraceToString()}"
                            }
                        }

                        isProcessing = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Text(if (isProcessing) "处理中..." else "🧠 开始AI检测")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (testResult.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = testResult,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📖 使用说明",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. 点击「选择测试图片」按钮\n" +
                          "2. 从相册选择一张图片\n" +
                          "3. 点击「开始AI检测」按钮\n" +
                          "4. 查看检测结果和推理时间\n\n" +
                          "支持检测：\n" +
                          "• 危险物体：car, person, bicycle等\n" +
                          "• 路径信息：crosswalk, stairs等\n" +
                          "• 共80个COCO类别",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
