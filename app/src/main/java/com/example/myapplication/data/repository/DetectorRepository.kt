package com.example.myapplication.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.myapplication.data.model.DetectionResult
import com.example.myapplication.yolo.detectors.ObjectDetectorHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 目标检测仓库，负责封装 ObjectDetectorHelper，提供协程接口供 ViewModel 调用。
 */
class DetectorRepository(private val context: Context) {

    private var detectorHelper: ObjectDetectorHelper? = null

    /**
     * 初始化检测器。这里默认使用 YOLO 模型，你可以根据需要修改参数。
     */
    fun initializeDetector() {
        if (detectorHelper == null) {
            detectorHelper = ObjectDetectorHelper(
                context = context,
                currentModel = ObjectDetectorHelper.MODEL_YOLO // 默认使用 YOLO 模型
            )
        }
    }

    /**
     * 在后台线程执行目标检测
     *
     * @param bitmap 待检测的图像
     * @param rotation 图像的旋转角度
     * @return 包含检测结果的 DetectionResult
     */
    suspend fun detect(bitmap: Bitmap, rotation: Int): DetectionResult =
        withContext(Dispatchers.Default) {
            if (detectorHelper == null) {
                initializeDetector()
            }
            
            try {
                val result = detectorHelper?.detect(bitmap, rotation)
                result ?: DetectionResult(
                    results = emptyList(),
                    inferenceTime = 0,
                    imageHeight = bitmap.height,
                    imageWidth = bitmap.width,
                    isSuccess = false,
                    errorMessage = "Detector is not initialized properly."
                )
            } catch (e: Exception) {
                DetectionResult(
                    results = emptyList(),
                    inferenceTime = 0,
                    imageHeight = bitmap.height,
                    imageWidth = bitmap.width,
                    isSuccess = false,
                    errorMessage = e.message ?: "Unknown error during detection."
                )
            }
        }

    /**
     * 释放检测器资源
     */
    fun release() {
        detectorHelper?.clearObjectDetector()
        detectorHelper = null
    }
}
