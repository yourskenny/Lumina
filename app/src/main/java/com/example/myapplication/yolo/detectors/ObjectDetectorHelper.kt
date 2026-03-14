package com.example.myapplication.yolo.detectors

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock

/**
 * 目标检测辅助类，负责管理检测器的生命周期、配置参数以及执行推理。
 * 作为数据层的一部分，不再直接依赖 UI 回调。
 */
class ObjectDetectorHelper(
    var threshold: Float = 0.5f,          // 置信度阈值：低于该分数的检测框将被过滤
    var numThreads: Int = 2,              // 推理线程数：用于 CPU 推理时的多线程加速
    var maxResults: Int = 3,              // 最大结果数：每张图片最多返回几个检测目标
    var currentDelegate: Int = 0,         // 硬件代理类型：0=CPU, 1=GPU, 2=NNAPI
    var currentModel: Int = 4,            // 模型类型：决定加载哪个具体的模型文件
    val context: Context                  // 上下文：用于从 Assets 中读取模型文件
) {

    // 统一的检测器接口实例。因为可能会在运行时切换模型或配置，所以声明为可变的 var
    private var objectDetector: LocalObjectDetector? = null

    init {
        setupObjectDetector()
    }

    fun clearObjectDetector() {
        objectDetector = null
    }

    /**
     * 初始化目标检测器。
     * 根据当前选择的模型类型 (YOLO 或其他 MediaPipe 支持的模型)，
     * 实例化对应的检测器，并直接传入基础配置参数。
     */
    fun setupObjectDetector() {
        if (currentModel == MODEL_YOLO) {
            // 初始化 YOLO 检测器
            objectDetector = YoloDetector(
                threshold,
                0.3f, // NMS IOU 阈值 (YOLO 特有)
                numThreads,
                maxResults,
                currentDelegate,
                currentModel,
                context
            )
        } else {
            // 已移除 MediaPipe 的通用检测器，如果以后需要其他模型，也需要通过 ONNX 实现
            throw UnsupportedOperationException("TaskVisionDetector is no longer supported.")
        }
    }

    /**
     * 执行目标检测。
     * 接收原始 Bitmap 和旋转角度，直接交由底层检测器处理，并返回封装好的结果。
     */
    fun detect(image: Bitmap, imageRotation: Int): com.example.myapplication.data.model.DetectionResult {
        if (objectDetector == null) {
            setupObjectDetector()
        }

        // 记录推理开始时间
        var inferenceTime = SystemClock.uptimeMillis()

        // 直接传入 Bitmap 和 旋转角度。
        val detections = objectDetector?.detect(image, imageRotation)

        // 计算推理总耗时
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        return if (detections != null) {
            com.example.myapplication.data.model.DetectionResult(
                results = detections,
                inferenceTime = inferenceTime,
                imageHeight = image.height,
                imageWidth = image.width,
                isSuccess = true
            )
        } else {
            com.example.myapplication.data.model.DetectionResult(
                results = emptyList(),
                inferenceTime = 0,
                imageHeight = image.height,
                imageWidth = image.width,
                isSuccess = false,
                errorMessage = "Detection returned null"
            )
        }
    }

    companion object {
        // 硬件代理常量
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2

        // 模型常量
        const val MODEL_MOBILENETV1 = 0
        const val MODEL_EFFICIENTDETV0 = 1
        const val MODEL_EFFICIENTDETV1 = 2
        const val MODEL_EFFICIENTDETV2 = 3
        const val MODEL_YOLO = 4
    }
}