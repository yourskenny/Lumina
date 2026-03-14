package com.example.myapplication.domain.detector

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.util.Log
import com.example.myapplication.data.model.RawObject
import java.nio.FloatBuffer
import java.util.Collections
import kotlin.math.max
import kotlin.math.min

/**
 * 目标检测器
 * 使用ONNX Runtime运行yoloe-v8s-seg模型
 * 包含完整的后处理逻辑 (NMS, 坐标转换)
 */
class ObjectDetector(
    private val modelPath: String = "yoloe-v8s-seg.onnx"
) {

    private val TAG = "ObjectDetector"

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var classes: List<String> = emptyList()

    // 默认的COCO数据集标签 (80类)
    private val defaultLabels = listOf(
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
        "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
        "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
        "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
        "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
        "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
        "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
        "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
        "hair drier", "toothbrush"
    )

    // 配置参数
    private val CONFIDENCE_THRESHOLD = 0.5f
    private val IOU_THRESHOLD = 0.5f
    private val INPUT_SIZE = 320

    /**
     * 初始化检测器
     * @param assetsManager 资源管理器,用于加载模型
     */
    fun init(assetsManager: android.content.res.AssetManager) {
        try {
            // 初始化ONNX环境
            ortEnv = OrtEnvironment.getEnvironment()
            
            // 读取模型文件
            val modelBytes = assetsManager.open(modelPath).readBytes()
            
            // 创建会话
            ortSession = ortEnv?.createSession(modelBytes, OrtSession.SessionOptions())
            
            // 使用默认标签
            classes = defaultLabels
            
            Log.d(TAG, "ObjectDetector初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "ObjectDetector初始化失败", e)
        }
    }

    /**
     * 检测图像中的物体
     * @param bitmap 输入图像
     * @return 检测到的物体列表
     */
    fun detect(bitmap: Bitmap): List<RawObject> {
        if (ortSession == null || ortEnv == null) {
            Log.e(TAG, "检测器未初始化")
            return emptyList()
        }

        try {
            // 1. 预处理
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            val floatBuffer = preprocess(resizedBitmap)
            
            // 2. 创建Tensor
            val inputName = ortSession?.inputNames?.iterator()?.next() ?: "images"
            val shape = longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
            val inputTensor = OnnxTensor.createTensor(ortEnv, floatBuffer, shape)
            
            // 3. 运行推理
            val results = ortSession?.run(Collections.singletonMap(inputName, inputTensor))
            val outputTensor = results?.get(0) as OnnxTensor
            val outputData = outputTensor.floatBuffer
            
            val outputShape = outputTensor.info.shape
            Log.d(TAG, "Output Shape: ${outputShape.contentToString()}")
            
            // 4. 解析输出 [1, 84, 8400]
            // 注意: ONNX Runtime Java 的 outputData 是展平的 FloatBuffer
            // YOLOv8 输出维度: Batch(1) x Channels(84) x Anchors(8400)
            // 84通道 = 4个坐标(cx,cy,w,h) + 80个类别置信度
            
            val detections = processOutput(outputData, bitmap.width, bitmap.height)
            
            if (detections.isNotEmpty()) {
                Log.d(TAG, "检测到 ${detections.size} 个物体: ${detections.first().name} ${detections.first().box}")
            }

            // 5. 释放资源
            inputTensor.close()
            results.close()
            
            return detections
            
        } catch (e: Exception) {
            Log.e(TAG, "推理失败", e)
            return emptyList()
        }
    }

    /**
     * 图像预处理: 归一化并转换为FloatBuffer (CHW格式)
     */
    private fun preprocess(bitmap: Bitmap): FloatBuffer {
        val floatBuffer = FloatBuffer.allocate(1 * 3 * INPUT_SIZE * INPUT_SIZE)
        floatBuffer.rewind()
        
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        // 转换为CHW格式并归一化 [0, 1]
        for (i in 0 until INPUT_SIZE * INPUT_SIZE) {
            val pixel = pixels[i]
            // R
            floatBuffer.put(i, ((pixel shr 16 and 0xFF) / 255.0f))
        }
        for (i in 0 until INPUT_SIZE * INPUT_SIZE) {
            val pixel = pixels[i]
            // G
            floatBuffer.put(INPUT_SIZE * INPUT_SIZE + i, ((pixel shr 8 and 0xFF) / 255.0f))
        }
        for (i in 0 until INPUT_SIZE * INPUT_SIZE) {
            val pixel = pixels[i]
            // B
            floatBuffer.put(2 * INPUT_SIZE * INPUT_SIZE + i, ((pixel and 0xFF) / 255.0f))
        }
        
        floatBuffer.rewind()
        return floatBuffer
    }

    /**
     * 解析模型输出
     */
    private fun processOutput(output: FloatBuffer, imgWidth: Int, imgHeight: Int): List<RawObject> {
        val candidates = ArrayList<Detection>()
        
        val numChannels = 84 // 4 + 80
        val numAnchors = output.capacity() / numChannels
        
        output.rewind()
        val data = FloatArray(output.capacity())
        output.get(data)
        
        for (i in 0 until numAnchors) {
            // 找到该锚点的最大类别置信度
            var maxScore = -Float.MAX_VALUE
            var maxClassIndex = -1
            
            // 遍历80个类别 (从索引4开始)
            for (c in 0 until 80) {
                // index = (4 + c) * 8400 + i
                val score = data[(4 + c) * numAnchors + i]
                if (score > maxScore) {
                    maxScore = score
                    maxClassIndex = c
                }
            }
            
            if (maxScore > CONFIDENCE_THRESHOLD) {
                // 获取坐标
                // cx = data[0 * 8400 + i]
                val cx = data[0 * numAnchors + i]
                val cy = data[1 * numAnchors + i]
                val w = data[2 * numAnchors + i]
                val h = data[3 * numAnchors + i]
                
                // 转换为左上角坐标 (x, y, w, h)
                val x = cx - w / 2
                val y = cy - h / 2
                
                candidates.add(Detection(x, y, w, h, maxScore, maxClassIndex))
            }
        }
        
        // NMS 非极大值抑制
        val finalDetections = nms(candidates)
        
        // 映射回原图尺寸并封装结果
        return finalDetections.map { det ->
            // 坐标缩放
            val scaleX = imgWidth.toFloat() / INPUT_SIZE
            val scaleY = imgHeight.toFloat() / INPUT_SIZE
            
            val realX = det.x * scaleX
            val realY = det.y * scaleY
            val realW = det.w * scaleX
            val realH = det.h * scaleY
            
            val className = if (det.classIndex in classes.indices) classes[det.classIndex] else "unknown"
            
            // 估算距离 (启发式: 假设人高1.7米, 这里的系数 1000 是经验值, 需要校准)
            // distance = focal_length * real_height / image_height
            // 简化为: factor / normalized_height
            // YOLO输出的 h 是相对于640的，所以 normalized_h = h / 640
            val normalizedH = det.h / INPUT_SIZE
            val distance = estimateDistance(className, normalizedH)
            
            // 计算方位 (左/中/右)
            val centerX = det.x + det.w / 2
            val direction = when {
                centerX < INPUT_SIZE / 3 -> "left"
                centerX > INPUT_SIZE * 2 / 3 -> "right"
                else -> "center"
            }
            
            RawObject(
                name = className,
                distanceM = distance,
                direction = direction,
                box = listOf(
                    realX.toDouble(), 
                    realY.toDouble(), 
                    (realX + realW).toDouble(), 
                    (realY + realH).toDouble()
                )
            )
        }
    }

    /**
     * NMS 非极大值抑制
     */
    private fun nms(candidates: List<Detection>): List<Detection> {
        val sorted = candidates.sortedByDescending { it.score }
        val selected = ArrayList<Detection>()
        val active = BooleanArray(sorted.size) { true }
        
        for (i in sorted.indices) {
            if (!active[i]) continue
            
            val a = sorted[i]
            selected.add(a)
            
            for (j in i + 1 until sorted.size) {
                if (!active[j]) continue
                
                val b = sorted[j]
                if (calculateIoU(a, b) > IOU_THRESHOLD) {
                    active[j] = false
                }
            }
        }
        return selected
    }

    /**
     * 计算 IoU (交并比)
     */
    private fun calculateIoU(a: Detection, b: Detection): Float {
        val x1 = max(a.x, b.x)
        val y1 = max(a.y, b.y)
        val x2 = min(a.x + a.w, b.x + b.w)
        val y2 = min(a.y + a.h, b.y + b.h)
        
        if (x2 < x1 || y2 < y1) return 0f
        
        val intersection = (x2 - x1) * (y2 - y1)
        val areaA = a.w * a.h
        val areaB = b.w * b.h
        
        return intersection / (areaA + areaB - intersection)
    }
    
    /**
     * 简单的距离估算
     */
    private fun estimateDistance(className: String, normalizedHeight: Float): Double {
        // 基于物体平均高度的经验系数
        val factor = when (className) {
            "person" -> 1.7 // 米
            "car" -> 1.5
            "bus" -> 3.0
            "truck" -> 3.0
            "bicycle" -> 1.0
            "motorcycle" -> 1.0
            "stop sign" -> 0.9
            else -> 1.0
        }
        
        // 假设焦距/传感器高度比率常数 K ≈ 0.8 (需要实测校准)
        // D = (RealH * K) / ImgH
        val K = 0.8
        
        // 避免除零
        val h = if (normalizedHeight < 0.01f) 0.01f else normalizedHeight
        
        return (factor * K) / h
    }

    /**
     * 内部检测结果类
     */
    private data class Detection(
        val x: Float, val y: Float, val w: Float, val h: Float,
        val score: Float, val classIndex: Int
    )

    /**
     * 释放资源
     */
    fun close() {
        try {
            ortSession?.close()
            ortEnv?.close()
            ortSession = null
            ortEnv = null
        } catch (e: Exception) {
            Log.e(TAG, "关闭失败", e)
        }
    }
}
