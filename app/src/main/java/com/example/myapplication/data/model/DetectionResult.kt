package com.example.myapplication.data.model

import com.example.myapplication.yolo.detectors.ObjectDetection

data class DetectionResult(
    val results: List<ObjectDetection> = emptyList(),
    val inferenceTime: Long = 0L,
    val imageHeight: Int = 0,
    val imageWidth: Int = 0,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)
