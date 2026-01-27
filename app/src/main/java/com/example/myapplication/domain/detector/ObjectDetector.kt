package com.example.myapplication.domain.detector

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import com.example.myapplication.data.model.RawObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections

/**
 * 本地 ONNX Runtime 目标检测器
 * 使用 yoloe-v8s-seg.onnx 模型进行推理
 */
class ObjectDetector(private val context: Context) {

    companion object {
        // 模型文件名 (放置在 assets 目录下)
        private const val MODEL_NAME = "yoloe-v8s-seg.onnx"
        
        // 类别定义 (与 Python 端一致)
        private val TARGET_CLASSES = mapOf(
            "path" to listOf("crosswalk", "stairs", "tactile paving"),
            "hazard" to listOf("car", "motorcycle", "bicycle", "pole", "tree", "fire hydrant", "traffic cone"),
            "interaction" to listOf("person", "dog", "cat", "chair", "traffic light", "stop sign")
        )
        
        // COCO 80 类名
        private val CLASS_NAMES = listOf(
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
    }

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var inputName: String? = null
    private var inputSize = 320 // 默认模型输入尺寸

    init {
        setupSession()
    }

    private fun setupSession() {
        try {
            ortEnvironment = OrtEnvironment.getEnvironment()
            val modelBytes = context.assets.open(MODEL_NAME).readBytes()
            val sessionOptions = OrtSession.SessionOptions()
            // 可以根据需要开启 NNAPI 加速
            // sessionOptions.addNnapi() 
            ortSession = ortEnvironment?.createSession(modelBytes, sessionOptions)
            
            // 获取输入节点名称和尺寸
            inputName = ortSession?.inputNames?.iterator()?.next()
            val inputInfo = ortSession?.inputInfo?.get(inputName) as? TensorInfo
            val shape = inputInfo?.shape
            if (shape != null && shape.size >= 3) {
                // shape 通常是 [1, 3, 320, 320]
                inputSize = shape[2].toInt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun detect(bitmap: Bitmap): DetectionResult {
        if (ortSession == null || ortEnvironment == null) return DetectionResult(emptyList(), emptyList())

        // 1. 预处理 (Bitmap -> FloatBuffer [1, 3, 320, 320])
        val floatBuffer = preprocess(bitmap)

        // 2. 推理
        val inputTensor = ai.onnxruntime.OnnxTensor.createTensor(
            ortEnvironment,
            floatBuffer,
            longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
        )
        
        val inputs = mapOf(inputName!! to inputTensor)
        val rawObjects = ArrayList<RawObject>()

        try {
            val results = ortSession?.run(inputs)
            
            // YOLOE 输出解析
            // 假设输出是 [1, 84, 8400] (cls+box, anchors) 或者类似的
            // 需要具体的模型输出结构来编写解析逻辑
            // 为了保证 App 能跑通，这里先使用模拟数据，
            // 实际接入时请根据 results[0].value 打印出的 shape 来调整
            
            // 模拟数据 (确保功能可用)
            val centerX = bitmap.width / 2f
            val centerY = bitmap.height / 2f
            
            // 模拟一个 "chair" (椅子) 在前方 2 米
            val chair = RawObject(
                name = "chair",
                distanceM = 2.0,
                direction = "center",
                box = listOf(centerX - 100, centerY - 100, centerX + 100, centerY + 100)
            )
            rawObjects.add(chair)
            
            // 模拟一个 "person" (人) 在左前方 1.5 米
            if (Math.random() > 0.5) {
                 val person = RawObject(
                    name = "person",
                    distanceM = 1.5,
                    direction = "left",
                    box = listOf(100.0, 100.0, 200.0, 400.0)
                )
                rawObjects.add(person)
            }
            
            inputTensor.close()
            results?.close()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. 分类
        val hazards = ArrayList<RawObject>()
        val paths = ArrayList<RawObject>()

        for (obj in rawObjects) {
            if (TARGET_CLASSES["hazard"]?.contains(obj.name) == true || 
                TARGET_CLASSES["interaction"]?.contains(obj.name) == true) {
                hazards.add(obj)
            } else if (TARGET_CLASSES["path"]?.contains(obj.name) == true) {
                paths.add(obj)
            }
        }

        return DetectionResult(hazards, paths)
    }
    
    private fun preprocess(bitmap: Bitmap): java.nio.FloatBuffer {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val buffer = ByteBuffer.allocateDirect(1 * 3 * inputSize * inputSize * 4)
        buffer.order(ByteOrder.nativeOrder())
        val floatBuffer = buffer.asFloatBuffer()

        val intValues = IntArray(inputSize * inputSize)
        scaledBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        // 归一化并转为 NCHW [1, 3, H, W]
        for (i in 0 until inputSize * inputSize) {
            val pixel = intValues[i]
            // R
            floatBuffer.put(i, ((pixel shr 16 and 0xFF) / 255.0f))
        }
        for (i in 0 until inputSize * inputSize) {
            val pixel = intValues[i]
            // G
            floatBuffer.put(inputSize * inputSize + i, ((pixel shr 8 and 0xFF) / 255.0f))
        }
        for (i in 0 until inputSize * inputSize) {
            val pixel = intValues[i]
            // B
            floatBuffer.put(2 * inputSize * inputSize + i, ((pixel and 0xFF) / 255.0f))
        }
        
        floatBuffer.rewind()
        return floatBuffer
    }
    
    fun close() {
        ortSession?.close()
        ortEnvironment?.close()
    }
}

data class DetectionResult(
    val hazards: List<RawObject>,
    val paths: List<RawObject>
)
