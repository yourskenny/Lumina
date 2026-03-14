package com.example.myapplication.yolo.predict.detect

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import com.example.myapplication.yolo.ImageProcessing
import com.example.myapplication.yolo.models.LocalYoloModel
import com.example.myapplication.yolo.models.YoloModel
import com.example.myapplication.yolo.predict.Predictor
import com.example.myapplication.yolo.predict.PredictorException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.ArrayList

class OrtDetector(context: Context) : Detector(context) {

    companion object {
        private const val Nanos2Millis = 1 / 1e6f
    }

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    // 动态获取模型输入形状
    private var inputName: String = ""
    private var inputChannels: Int = 3
    private var inputWidth: Int = 320
    private var inputHeight: Int = 320

    // 动态获取模型输出形状，避免每帧重复分配内存
    private var outputName: String = ""
    private var outputShape2 = 0
    private var outputShape3 = 0
    private var outputArrays: Array<FloatArray>? = null

    // 阈值设置
    private var confidenceThreshold = 0.25f
    private var iouThreshold = 0.45f
    private var numItemsThreshold = 30

    // YOLO specific params
    private var numClasses = 16

    // 性能监控统计类
    inner class Stats {
        var imageSetupTime: Float = 0f    // 图像预处理耗时
        var inferenceTime: Float = 0f     // 模型核心推理耗时
        var postProcessTime: Float = 0f   // 后处理耗时
    }
    val stats = Stats()

    // 回调接口
    private var objectDetectionResultCallback: ObjectDetectionResultCallback? = null
    private var inferenceTimeCallback: FloatResultCallback? = null
    private var fpsRateCallback: FloatResultCallback? = null

    // 前处理需要的内存 (延迟初始化，基于模型的实际尺寸)
    private var imageProcessing = ImageProcessing()
    private var inputBuffer: ByteBuffer? = null

    @Throws(Exception::class)
    override fun loadModel(yoloModel: YoloModel, useGpu: Boolean) {
        if (yoloModel is LocalYoloModel) {
            if (yoloModel.modelPath.isNullOrEmpty()) {
                throw Exception("Model path is empty")
            }

            try {
                // Initialize ONNX Environment
                ortEnvironment = OrtEnvironment.getEnvironment()
                
                // Set session options
                val sessionOptions = OrtSession.SessionOptions()
                
                // 开启 NNAPI 硬件加速 (如果设备支持，这会极大提升 dynamic=False 的模型的帧率)
                if (useGpu) {
                    sessionOptions.addNnapi()
                }
                
                // Read model from assets
                val modelBytes = context.assets.open(yoloModel.modelPath).readBytes()
                ortSession = ortEnvironment?.createSession(modelBytes, sessionOptions)

                // --- 1. 动态加载输入形状 ---
                inputName = ortSession?.inputNames?.iterator()?.next() ?: ""
                val inputTensorInfo = ortSession?.inputInfo?.get(inputName)?.info as? TensorInfo
                val inShape = inputTensorInfo?.shape
                if (inShape != null && inShape.size == 4) {
                    inputChannels = inShape[1].toInt() // 3
                    inputHeight = inShape[2].toInt()   // 320, 640 etc.
                    inputWidth = inShape[3].toInt()    // 320, 640 etc.
                } else {
                    // 若获取失败则提供合理的默认保护，并同步给全局静态量方便外部可能用到
                    inputHeight = Predictor.INPUT_SIZE
                    inputWidth = Predictor.INPUT_SIZE
                }
                
                // 根据动态获取到的输入尺寸来创建准确大小的装载 Buffer
                val bufferSize = 1 * inputChannels * inputWidth * inputHeight * 4 
                inputBuffer = ByteBuffer.allocateDirect(bufferSize).apply {
                    order(ByteOrder.LITTLE_ENDIAN)
                }

                // --- 2. 动态加载输出形状 ---
                // YOLOE -seg 有两个输出 "output0" 和 "output1"，我们只取 "output0" 处理框
                val outputNames = ortSession?.outputNames ?: emptySet<String>()
                outputName = if (outputNames.contains("output0")) "output0" else outputNames.iterator().next()
                
                val outTensorInfo = ortSession?.outputInfo?.get(outputName)?.info as? TensorInfo
                val outShape = outTensorInfo?.shape
                if (outShape != null && outShape.size >= 3) {
                    outputShape2 = outShape[1].toInt() // 对于 yoloe-v8s-seg: 52 (4框 + 16类 + 32遮罩)
                    outputShape3 = outShape[2].toInt() // 对于 yoloe-v8s-seg: 2100 个候选框
                    outputArrays = Array(outputShape2) { FloatArray(outputShape3) }
                }

                // Optional: loads labels if metadata path is provided
                if (!yoloModel.metadataPath.isNullOrEmpty()) {
                    loadLabels(context.assets, yoloModel.metadataPath)
                    // 使用真实存在的标签数 (比如 16)，这样之后不论后面有多少 mask 参数都会被忽略
                    numClasses = labels.size
                } else {
                    // 如果没提供，默认按照 16 类来处理
                    // numClasses = 16
                }
                
            } catch (e: Exception) {
                throw PredictorException("Error loading ONNX model: ${e.message}")
            }
        }
    }

    fun preprocess(bitmap: Bitmap): Bitmap {
        return if (bitmap.width != inputWidth || bitmap.height != inputHeight) {
            Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        } else {
            bitmap
        }
    }

    override fun predict(bitmap: Bitmap): ArrayList<DetectedObject> {
        val session = ortSession ?: return ArrayList()
        val env = ortEnvironment ?: return ArrayList()
        val detectedObjects = ArrayList<DetectedObject>()
        val buffer = inputBuffer ?: return ArrayList()

        try {
            var startTime = System.nanoTime()

            // 1. 图像预处理: 将 bitmap 缩放为模型所需的大小
            val resizedBitmap = preprocess(bitmap)

            val pixels = IntArray(inputWidth * inputHeight)
            resizedBitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

            // 先把 ByteBuffer 清空
            buffer.rewind()
            
            // 将像素交给 JNI 转换为模型需要的张量格式放入 inputBuffer
            imageProcessing.argb2yolo(pixels, buffer, inputWidth, inputHeight)
            
            // 构造 ONNX Tensor (Float 型张量，形状如 [1, 3, 320, 320] 或 [1, 3, 640, 640])
            val shape = longArrayOf(1, inputChannels.toLong(), inputHeight.toLong(), inputWidth.toLong())
            
            // 将 ByteBuffer 转换为 FloatBuffer 然后创建 Tensor
            buffer.rewind()
            val floatBuffer: FloatBuffer = buffer.asFloatBuffer()
            val inputTensor = OnnxTensor.createTensor(env, floatBuffer, shape)
            
            stats.imageSetupTime = (System.nanoTime() - startTime) * Nanos2Millis

            startTime = System.nanoTime()

            // 2. 核心推理执行
            val inputs = mapOf(inputName to inputTensor)
            val results = session.run(inputs)
            
            stats.inferenceTime = (System.nanoTime() - startTime) * Nanos2Millis

            startTime = System.nanoTime()

            // 3. 解析结果 (从缓存和已知的 shape 直接读取)
            // YOLOE -seg 有两个输出 "output0" 和 "output1"，我们只取 "output0" 边界框
            val outputNames = session.outputNames
            val outputName = if (outputNames.contains("output0")) "output0" else outputNames.iterator().next()

            val resultTensorValue = results.get(outputName)
            val resultTensor = (if (resultTensorValue.isPresent) resultTensorValue.get() else null) as? OnnxTensor

            if (resultTensor != null && outputArrays != null) {
                // 直接使用 FloatBuffer 获取数据，这比使用 .value 自动转换再创建一堆 Java Array 对象要快得多
                val floatBuffer = resultTensor.floatBuffer
                floatBuffer.rewind()
                
                // 将数据灌入我们事先声明好的二维数组缓存中
                for (j in 0 until outputShape2) {
                    floatBuffer.get(outputArrays!![j])
                }
                
                // h: 维度数 (例如 52), w: 候选框数 (例如 2100)
                val h = outputShape2
                val w = outputShape3
                
                // 注意这里的核心变更：如果它是分割模型 (h = 52，其中包含 mask 维度)，
                // 我们绝对不能使用 `numClasses = h - 4` (这样算出来是 48)，否则在后处理中读取到 mask 系数会导致闪退或者结果错乱。
                // 我们强制固定读取它实际的有效检测类别 `numClasses` (例如 16)，这样剩余尾部的 mask 维度就会在 `PostProcessUtils` 的循环中被自然遗弃跳过。
                if (numClasses <= 0) {
                   numClasses = 16 
                }

                val bboxes = PostProcessUtils.postprocess(
                    outputArrays!!,
                    w,
                    h,
                    confidenceThreshold,
                    iouThreshold,
                    numItemsThreshold,
                    numClasses,
                    labels,
                    inputWidth,
                    inputHeight
                )
                
                detectedObjects.addAll(bboxes)
            }
            
            // 清理 tensor 防止内存泄漏
            inputTensor.close()
            results.close()
            
            stats.postProcessTime = (System.nanoTime() - startTime) * Nanos2Millis

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return detectedObjects
    }

    override fun setConfidenceThreshold(confidence: Float) {
        this.confidenceThreshold = confidence
    }

    override fun setIouThreshold(iou: Float) {
        this.iouThreshold = iou
    }

    override fun setNumItemsThreshold(numItems: Int) {
        this.numItemsThreshold = numItems
    }

    override fun setObjectDetectionResultCallback(callback: ObjectDetectionResultCallback) {
        objectDetectionResultCallback = callback
    }

    override fun setInferenceTimeCallback(callback: FloatResultCallback) {
        inferenceTimeCallback = callback
    }

    override fun setFpsRateCallback(callback: FloatResultCallback) {
        fpsRateCallback = callback
    }
}
