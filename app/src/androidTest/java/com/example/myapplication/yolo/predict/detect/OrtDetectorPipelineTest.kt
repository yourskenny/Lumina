package com.example.myapplication.yolo.predict.detect

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.myapplication.yolo.ImageProcessing
import com.example.myapplication.yolo.models.LocalYoloModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class OrtDetectorPipelineTest {

    /**
     * 测试 1：专门测试底层的 C++ (image_processing.cpp) 图像预处理是否正常工作
     * 验证是否能成功处理 Bitmap 像素且没有引发内存越界
     */
    @Test
    fun testImageProcessingJni() {
        val width = 320
        val height = 320
        
        // 构建全红色的假想图
        val pixels = IntArray(width * height) { Color.RED } 
        // 红色在 ARGB 中是 (255, 255, 0, 0)
        
        val bufferSize = 1 * 3 * width * height * 4
        val inputBuffer = ByteBuffer.allocateDirect(bufferSize).apply {
            order(ByteOrder.LITTLE_ENDIAN)
        }

        val imageProcessing = ImageProcessing()
        
        try {
            // 如果底层 C++ 实现有内存越界，这里会直接引发 SIGSEGV 崩溃
            imageProcessing.argb2yolo(pixels, inputBuffer, width, height)
            
            inputBuffer.rewind()
            val floatBuffer = inputBuffer.asFloatBuffer()
            
            // YOLO / ONNX 通常的 NCHW 排序是先存放所有的 R 再存放所有的 G 再存放 B
            // 我们验证一下通道 0 (R通道) 的第一个浮点数是不是接近 1.0f (255/255.0)
            val rValue = floatBuffer.get(0)
            // G 通道开头应该在 offset = 320*320 的位置，值应该接近 0.0f
            val gValue = floatBuffer.get(width * height)
            
            println("JNI Check - R value: $rValue, G value: $gValue")
            
            assertTrue("R 通道应该被归一化为 1.0f", rValue > 0.99f)
            assertTrue("G 通道应该被归一化为 0.0f", gValue < 0.01f)
            
        } catch (e: Exception) {
            fail("JNI 前处理发生异常: ${e.message}")
        }
    }

    /**
     * 测试 2：端到端推理管线测试
     * 验证 OrtDetector 能够顺利加载模型并利用假图跑通一遍流程
     */
    @Test
    fun testOrtDetectorEndToEnd() {
        // 获取测试应用上下文
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // 1. 初始化模型配置 (请确保 src/main/assets 下放置了这些真实文件!)
        val config = LocalYoloModel(
            task = "detect",
            format = "onnx",
            modelPath = "yoloe-v8s-seg.onnx", 
            metadataPath = "metadata.yaml"
        )

        val detector = OrtDetector(context)
        
        try {
            // 2. 加载模型
            detector.loadModel(config, useGpu = false)
            
            // 3. 构造或获取相机模拟帧
            // 我们通过 Assets 加载一张真实的附带测试图片进行预测
            // 请在 app/src/main/assets/ 下放一张 `test_image.jpg` 以供检测。
            // 假设图片中包含真实世界目标（例如狗、人、车等能被你的 YOLO 模型识别出）
            val testBitmap: Bitmap
            try {
                context.assets.open("test_image.jpg").use { inputStream ->
                    testBitmap = BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                println("[WARN] test_image.jpg 没有在 assets 中找到，降级使用画笔黑块进行形状验证。")
                val fallbackBitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(fallbackBitmap)
                canvas.drawColor(Color.WHITE) 
                val paint = Paint().apply { color = Color.BLACK }
                canvas.drawRect(100f, 100f, 300f, 300f, paint) 
                throw Exception("请放一张测试图像") // 我们强制跑出异常要求更换真图
            }

            // 4. 开始推理前，我们将置信度设为极低的负数用于强力测试
            // 哪怕它是乱码图像，只要模型执行了矩阵乘法，它就一定会吐出带有分数的“哪怕很像垃圾一样的框”
//            detector.setConfidenceThreshold(-1.0f)
            val results = detector.predict(testBitmap)

            // 5. 打印测试报告
            println("======= ONNX DETECTOR TEST REPORT =======")
            println("Image Pre-processing time: ${detector.stats.imageSetupTime} ms")
            println("Inference execution time : ${detector.stats.inferenceTime} ms")
            println("Post-processing (NMS)    : ${detector.stats.postProcessTime} ms")
            println("Detected elements count  : ${results.size}")
            
            for (obj in results) {
                println("-> [${obj.label}] Conf: ${obj.confidence} BBox: ${obj.boundingBox}")
                // 验证坐标必须按照规范还原到 0~1 的归一化比例之内
                assertTrue("边界框 Left 不应当小于 0", obj.boundingBox.left >= 0f)
                assertTrue("边界框 Right 不应当大于 1", obj.boundingBox.right <= 1.0f)
            }
            println("=========================================")

            // 如果能走到这里且没崩，说明整条管线的张量维数彻底握手成功！
            assertNotNull(results)

        } catch (e: Exception) {
            e.printStackTrace()
            fail("推理管线发生崩溃: ${e.message}")
        }
    }
}