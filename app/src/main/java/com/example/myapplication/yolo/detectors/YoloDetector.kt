package com.example.myapplication.yolo.detectors

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.example.myapplication.yolo.models.LocalYoloModel
import com.example.myapplication.yolo.predict.detect.DetectedObject
import com.example.myapplication.yolo.predict.detect.OrtDetector


class YoloDetector(
    var confidenceThreshold: Float = 0.5f,
    var iouThreshold: Float = 0.3f,
    var numThreads: Int = 2,
    var maxResults: Int = 3,
    var currentDelegate: Int = 0,
    var currentModel: Int = 0,
    val context: Context
): LocalObjectDetector {

    private var yolo: OrtDetector

    init {
        yolo = OrtDetector(context)
        yolo.setIouThreshold(iouThreshold)
        yolo.setConfidenceThreshold(confidenceThreshold)
        yolo.setNumItemsThreshold(maxResults) // 将 maxResults 传递给底层

        // 切换为 ONNX 模型 (请确保 app/src/main/assets 下有这个文件)
        val modelPath = "yoloe-v8s-seg.onnx"
        val metadataPath = "metadata.yaml" 

        val config = LocalYoloModel(
            "detect",
            "onnx",
            modelPath,
            metadataPath,
        )

        val useGPU = currentDelegate == 0
        yolo.loadModel(
            config,
            useGPU
        )
    }

    override fun detect(bitmap: Bitmap, imageRotation: Int): List<ObjectDetection>? {

        // yolo.predict 中使用 ONNX 完成预测
        val results = yolo.predict(bitmap)

        val detections = ArrayList<ObjectDetection>()

        for (result: DetectedObject in results) {
            val category = Category(
                result.label,
                result.confidence ?: 0f,
            )

            // ONNX 解析出来的结果是从 [0, 1] 归一化的，因此需要映射回原始图片的宽高
            val bbox = RectF(
                result.boundingBox.left * bitmap.width,
                result.boundingBox.top * bitmap.height,
                result.boundingBox.right * bitmap.width,
                result.boundingBox.bottom * bitmap.height
            )

            val detection = ObjectDetection(
                bbox,
                category
            )
            detections.add(detection)
        }

        return detections
    }
}