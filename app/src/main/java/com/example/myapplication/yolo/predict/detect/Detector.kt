package com.example.myapplication.yolo.predict.detect

import android.content.Context
import androidx.annotation.Keep
import com.example.myapplication.yolo.predict.Predictor
import java.util.ArrayList

abstract class Detector protected constructor(context: Context) : Predictor(context) {

    abstract fun setObjectDetectionResultCallback(callback: ObjectDetectionResultCallback)

    abstract fun setIouThreshold(iou: Float)

    abstract fun setNumItemsThreshold(numItems: Int)

    interface ObjectDetectionResultCallback {
        // @Keep 注解极其重要！
        // C++ 层在检测完成后，会通过 JNI 寻找这个 onResult 方法，并把结果传回 Android。
        @Keep
        fun onResult(detections: ArrayList<DetectedObject>)
    }
}