package com.example.myapplication.domain.detector

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import com.example.myapplication.data.model.RawObject
import kotlin.math.exp
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
        private const val CONFIDENCE_THRESHOLD = 0.4f
        private const val IOU_THRESHOLD = 0.5f
        
        // 类别定义 (与 Python 端一致)
        private val TARGET_CLASSES = mapOf(
            "path" to setOf("crosswalk", "stairs", "tactile paving"),
            "hazard" to setOf("car", "motorcycle", "bicycle", "pole", "tree", "fire hydrant", "traffic cone"),
            "interaction" to setOf("person", "dog", "cat", "chair", "traffic light", "stop sign")
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
            sessionOptions.addNnapi()
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
        var rawObjects = ArrayList<RawObject>()

        try {
            val results = ortSession?.run(inputs)
            
            // YOLOE 输出解析
            // 输出 shape: [1, 116, num_anchors] (例如 [1, 116, 2100])
            // 0-3: cx, cy, w, h
            // 4-83: 80 class scores
            // 84-115: 32 mask protos (ignore for now)
            
            val outputTensor = results?.get(0) as? ai.onnxruntime.OnnxTensor
            val outputBuffer = outputTensor?.floatBuffer
            val shape = outputTensor?.info?.shape // [1, 116, 2100]
            
            if (outputBuffer != null && shape != null && shape.size == 3) {
                val dim1 = shape[1].toInt()
                val dim2 = shape[2].toInt()
                val isChannelMajor = dim1 == 116
                val channels = if (isChannelMajor) dim1 else dim2
                val anchors = if (isChannelMajor) dim2 else dim1
                val flattened = FloatArray(channels * anchors)
                outputBuffer.get(flattened)
                fun idx(channel: Int, anchor: Int): Int {
                    return if (isChannelMajor) channel * anchors + anchor else anchor * channels + channel
                }
                val candidates = ArrayList<DetectionCandidate>()
                for (i in 0 until anchors) {
                    var maxScore = -Float.MAX_VALUE
                    var maxClassId = -1
                    for (c in 0 until 80) {
                        val scoreRaw = flattened[idx(4 + c, i)]
                        val score = (1f / (1f + exp(-scoreRaw)))
                        if (score > maxScore) {
                            maxScore = score
                            maxClassId = c
                        }
                    }
                    if (maxScore > CONFIDENCE_THRESHOLD) {
                        val cx = flattened[idx(0, i)]
                        val cy = flattened[idx(1, i)]
                        val w = flattened[idx(2, i)]
                        val h = flattened[idx(3, i)]
                        val scaleX = bitmap.width.toFloat() / inputSize
                        val scaleY = bitmap.height.toFloat() / inputSize
                        val x1 = (cx - w / 2) * scaleX
                        val y1 = (cy - h / 2) * scaleY
                        val x2 = (cx + w / 2) * scaleX
                        val y2 = (cy + h / 2) * scaleY
                        candidates.add(DetectionCandidate(
                            classId = maxClassId,
                            score = maxScore,
                            box = floatArrayOf(x1, y1, x2, y2)
                        ))
                    }
                }
                
                // NMS (非极大值抑制)
                val finalCandidates = nms(candidates)
                
                // 转换为业务对象 RawObject
                for (cand in finalCandidates) {
                    val className = if (cand.classId in CLASS_NAMES.indices) CLASS_NAMES[cand.classId] else "unknown"
                    val (dist, dir) = calculateSpatialInfo(cand.box, bitmap.width, bitmap.height)
                    
                    rawObjects.add(RawObject(
                        name = className,
                        distanceM = dist,
                        direction = dir,
                        box = cand.box.map { it.toDouble() }
                    ))
                }
            }
            
            inputTensor.close()
            results?.close()
            outputTensor?.close()
            
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
    
    // NMS 实现
    private fun nms(candidates: List<DetectionCandidate>): List<DetectionCandidate> {
        val sorted = candidates.sortedByDescending { it.score }
        val selected = ArrayList<DetectionCandidate>()
        
        val active = BooleanArray(sorted.size) { true }
        
        for (i in sorted.indices) {
            if (!active[i]) continue
            
            val boxA = sorted[i]
            selected.add(boxA)
            
            for (j in i + 1 until sorted.size) {
                if (!active[j]) continue
                
                val boxB = sorted[j]
                if (calculateIoU(boxA.box, boxB.box) > IOU_THRESHOLD) {
                    active[j] = false
                }
            }
        }
        return selected
    }
    
    private fun calculateIoU(boxA: FloatArray, boxB: FloatArray): Float {
        val xA = maxOf(boxA[0], boxB[0])
        val yA = maxOf(boxA[1], boxB[1])
        val xB = minOf(boxA[2], boxB[2])
        val yB = minOf(boxA[3], boxB[3])
        
        val interArea = maxOf(0f, xB - xA) * maxOf(0f, yB - yA)
        
        val boxAArea = (boxA[2] - boxA[0]) * (boxA[3] - boxA[1])
        val boxBArea = (boxB[2] - boxB[0]) * (boxB[3] - boxB[1])
        
        return interArea / (boxAArea + boxBArea - interArea)
    }
    
    // 空间信息计算 (移植自 Python 代码)
    private fun calculateSpatialInfo(box: FloatArray, frameWidth: Int, frameHeight: Int): Pair<Double, String> {
        val x1 = box[0]
        val y1 = box[1]
        val x2 = box[2]
        val y2 = box[3]
        
        // 计算中心点 X 坐标，判断方位
        val centerX = (x1 + x2) / 2
        val relativeX = centerX / frameWidth
        
        val direction = when {
            relativeX < 0.33 -> "left"
            relativeX > 0.66 -> "right"
            else -> "center"
        }
        
        // 启发式测距 (基于脚点 y2)
        val normalizedYBottom = y2 / frameHeight
        
        val distance = when {
            normalizedYBottom > 0.9 -> 0.5
            normalizedYBottom > 0.75 -> 1.5
            normalizedYBottom > 0.5 -> 4.0
            else -> 8.0
        }
        
        return Pair(distance, direction)
    }
    
    private data class DetectionCandidate(
        val classId: Int,
        val score: Float,
        val box: FloatArray // [x1, y1, x2, y2]
    )
    
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
