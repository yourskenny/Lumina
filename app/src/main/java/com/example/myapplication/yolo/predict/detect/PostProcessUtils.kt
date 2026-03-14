package com.example.myapplication.yolo.predict.detect

import android.graphics.RectF
import java.util.ArrayList
import kotlin.math.max
import kotlin.math.min

// 使用 object 声明单例，完美替代 Java 中的全是 static 方法的工具类
object PostProcessUtils {

    // 计算两个 RectF 的交集面积
    private fun intersectionArea(a: RectF, b: RectF): Float {
        if (!RectF.intersects(a, b)) return 0f

        val left = max(a.left, b.left)
        val right = min(a.right, b.right)
        val top = max(a.top, b.top)
        val bottom = min(a.bottom, b.bottom)

        return max(0f, right - left) * max(0f, bottom - top)
    }

    // 非极大值抑制 (NMS) - Kotlin 改造版，不修改原集合，返回保留的索引
    private fun nmsSortedBboxes(
        objects: List<DetectedObject>,
        nmsThreshold: Float
    ): List<Int> {
        val picked = mutableListOf<Int>()
        val areas = objects.map { it.boundingBox.width() * it.boundingBox.height() }

        for (i in objects.indices) {
            val a = objects[i]
            var keep = true

            for (j in picked) {
                val b = objects[j]

                // 计算 IoU (交并比)
                val interArea = intersectionArea(a.boundingBox, b.boundingBox)
                val unionArea = areas[i] + areas[j] - interArea

                if (interArea / unionArea > nmsThreshold) {
                    keep = false
                    break
                }
            }

            if (keep) picked.add(i)
        }
        return picked
    }

    // 核心后处理函数
    fun postprocess(
        recognitions: Array<FloatArray>,
        w: Int,
        h: Int,
        confidenceThreshold: Float,
        iouThreshold: Float,
        numItemsThreshold: Int,
        numClasses: Int,
        labels: ArrayList<String>?,
        modelInputWidth: Int,
        modelInputHeight: Int
    ): ArrayList<DetectedObject> {
        val proposals = ArrayList<DetectedObject>()
        var highestScoreOverall = -Float.MAX_VALUE // 新增：用于调试，记录全图找到的最高置信度

        // 解析张量并过滤低置信度目标
        for (i in 0 until w) {
            var maxScore = -Float.MAX_VALUE
            var classIndex = -1

            for (c in 0 until numClasses) {
                // 根据 YOLOv8 格式，前4个是边界框 [x, y, w, h]，后面跟着类别置信度
                val score = recognitions[c + 4][i]
                if (score > maxScore) {
                    maxScore = score
                    classIndex = c
                }
            }
            
            if (maxScore > highestScoreOverall) {
                highestScoreOverall = maxScore
            }

            if (maxScore > confidenceThreshold) {
                // 根据输入到模型时的真实尺寸，动态地将输出的绝对像素转换为 `[0, 1]` 的归一化比例值
                // 这样无论模型是 320x320 还是 640x640，都能被完美映射回原图
                val dx = recognitions[0][i] / modelInputWidth.toFloat()
                val dy = recognitions[1][i] / modelInputHeight.toFloat()
                val dw = recognitions[2][i] / modelInputWidth.toFloat()
                val dh = recognitions[3][i] / modelInputHeight.toFloat()

                val rect = RectF(
                    dx - dw / 2f,
                    dy - dh / 2f,
                    dx + dw / 2f,
                    dy + dh / 2f
                )

                val label = if (labels != null && classIndex < labels.size) labels[classIndex] else "Unknown"
                val obj = DetectedObject(maxScore, rect, classIndex, label)

                proposals.add(obj)
            }
        }

        // 调试打印：检查整张图中模型能给出的最高置信度是多少
//        println("-------==========ONNX DEBUG==========-------")
//        println("[PostProcess Debug] Highest confidence score on this image: $highestScoreOverall. Threshold: $confidenceThreshold")
//        println("-------==========ONNX DEBUG==========-------")

        // 直接使用 Kotlin 的内联高阶排序函数
        proposals.sortByDescending { it.confidence }

        // 应用 NMS
        val pickedIndices = nmsSortedBboxes(proposals, iouThreshold)

        // 限制最大输出数量并规范化边界框到 [0, 1] 范围
        val result = ArrayList<DetectedObject>()
        val count = min(pickedIndices.size, numItemsThreshold)

        for (i in 0 until count) {
            val obj = proposals[pickedIndices[i]]

            // 限制框不越出图像边界
            val left = max(0f, obj.boundingBox.left)
            val top = max(0f, obj.boundingBox.top)
            val right = min(1f, obj.boundingBox.right)
            val bottom = min(1f, obj.boundingBox.bottom)

            val boundingBox = RectF(left, top, right, bottom)

            val det = DetectedObject(
                obj.confidence,
                boundingBox,
                obj.index,
                obj.label
            )
            result.add(det)
        }

        return result
    }
}
