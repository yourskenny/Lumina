package com.example.myapplication.yolo.detectors

import android.graphics.Bitmap
import android.graphics.RectF

class Category (
    val label: String,
    val confidence: Float
)

class ObjectDetection(
    val boundingBox: RectF,
    val category: Category
)

interface LocalObjectDetector {
    fun detect(bitmap: Bitmap, imageRotation: Int): List<ObjectDetection>?
}