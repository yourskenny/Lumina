package com.example.myapplication.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.myapplication.domain.detector.ObjectDetector
import java.io.File

/**
 * AI测试工具类
 * 提供命令行式的测试方法
 */
object AITestUtil {

    private const val TAG = "AITestUtil"

    /**
     * 测试指定路径的图片
     *
     * @param context 上下文
     * @param imagePath 图片文件路径
     * @return 检测结果的字符串表示
     */
    fun testImage(context: Context, imagePath: String): String {
        val detector = ObjectDetector(context)

        return try {
            val file = File(imagePath)
            if (!file.exists()) {
                "❌ 图片文件不存在: $imagePath"
            } else {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap == null) {
                    "❌ 无法解码图片: $imagePath"
                } else {
                    val result = testBitmap(detector, bitmap, imagePath)
                    bitmap.recycle()
                    result
                }
            }
        } catch (e: Exception) {
            "❌ 测试失败: ${e.message}\n${e.stackTraceToString()}"
        } finally {
            detector.close()
        }
    }

    /**
     * 测试Bitmap对象
     *
     * @param context 上下文
     * @param bitmap 要测试的Bitmap
     * @param label 标签（用于日志）
     * @return 检测结果的字符串表示
     */
    fun testBitmap(context: Context, bitmap: Bitmap, label: String = "Bitmap"): String {
        val detector = ObjectDetector(context)

        return try {
            testBitmap(detector, bitmap, label)
        } catch (e: Exception) {
            "❌ 测试失败: ${e.message}\n${e.stackTraceToString()}"
        } finally {
            detector.close()
        }
    }

    /**
     * 内部方法：使用已有detector测试bitmap
     */
    private fun testBitmap(detector: ObjectDetector, bitmap: Bitmap, label: String): String {
        val sb = StringBuilder()

        sb.append("=" .repeat(50)).append("\n")
        sb.append("🧪 AI检测测试报告\n")
        sb.append("=" .repeat(50)).append("\n")
        sb.append("📷 测试图片: $label\n")
        sb.append("📐 图片尺寸: ${bitmap.width}x${bitmap.height}\n")
        sb.append("\n")

        // 执行检测
        val startTime = System.currentTimeMillis()
        val result = detector.detect(bitmap)
        val endTime = System.currentTimeMillis()
        val inferenceTime = endTime - startTime

        sb.append("⏱️  推理时间: ${inferenceTime}ms\n")
        sb.append("\n")

        // 危险物体
        sb.append("🚨 危险物体检测结果 (${result.hazards.size}):\n")
        sb.append("-" .repeat(50)).append("\n")
        if (result.hazards.isEmpty()) {
            sb.append("   无危险物体\n")
        } else {
            result.hazards.forEachIndexed { index, obj ->
                sb.append("   [${index + 1}] ${obj.name}\n")
                sb.append("       • 距离: ${String.format("%.2f", obj.distanceM)}米\n")
                sb.append("       • 方向: ${obj.direction}\n")
                sb.append("       • 坐标: [")
                obj.box.forEachIndexed { i, coord ->
                    sb.append(String.format("%.1f", coord))
                    if (i < obj.box.size - 1) sb.append(", ")
                }
                sb.append("]\n")
            }
        }
        sb.append("\n")

        // 路径信息
        sb.append("🛤️  路径信息检测结果 (${result.paths.size}):\n")
        sb.append("-" .repeat(50)).append("\n")
        if (result.paths.isEmpty()) {
            sb.append("   无路径信息\n")
        } else {
            result.paths.forEachIndexed { index, obj ->
                sb.append("   [${index + 1}] ${obj.name}\n")
                sb.append("       • 距离: ${String.format("%.2f", obj.distanceM)}米\n")
                sb.append("       • 方向: ${obj.direction}\n")
            }
        }
        sb.append("\n")

        // 分析总结
        sb.append("📊 分析总结:\n")
        sb.append("-" .repeat(50)).append("\n")
        sb.append("   • 检测到物体总数: ${result.hazards.size + result.paths.size}\n")
        sb.append("   • 危险等级: ${analyzeDangerLevel(result.hazards)}\n")
        sb.append("   • 最近物体距离: ${findClosestDistance(result.hazards)}米\n")

        // 建议
        val suggestions = generateSuggestions(result.hazards, result.paths)
        if (suggestions.isNotEmpty()) {
            sb.append("\n")
            sb.append("💡 建议:\n")
            sb.append("-" .repeat(50)).append("\n")
            suggestions.forEach { suggestion ->
                sb.append("   • $suggestion\n")
            }
        }

        sb.append("=" .repeat(50)).append("\n")

        val resultStr = sb.toString()
        Log.d(TAG, resultStr)

        return resultStr
    }

    /**
     * 分析危险等级
     */
    private fun analyzeDangerLevel(hazards: List<com.example.myapplication.data.model.RawObject>): String {
        if (hazards.isEmpty()) return "✅ 安全"

        val minDistance = hazards.minOfOrNull { it.distanceM } ?: Double.MAX_VALUE

        return when {
            minDistance < 1.5 -> "🔴 高危 (${String.format("%.1f", minDistance)}m)"
            minDistance < 3.0 -> "🟡 警告 (${String.format("%.1f", minDistance)}m)"
            else -> "🟢 注意 (${String.format("%.1f", minDistance)}m)"
        }
    }

    /**
     * 找到最近物体距离
     */
    private fun findClosestDistance(hazards: List<com.example.myapplication.data.model.RawObject>): String {
        if (hazards.isEmpty()) return "无"

        val minDistance = hazards.minOfOrNull { it.distanceM } ?: Double.MAX_VALUE
        return String.format("%.2f", minDistance)
    }

    /**
     * 生成建议
     */
    private fun generateSuggestions(
        hazards: List<com.example.myapplication.data.model.RawObject>,
        paths: List<com.example.myapplication.data.model.RawObject>
    ): List<String> {
        val suggestions = mutableListOf<String>()

        // 检查危险物体
        hazards.forEach { hazard ->
            when {
                hazard.distanceM < 1.5 -> {
                    suggestions.add("紧急警告：${hazard.name} 在${hazard.direction}方向，距离仅${String.format("%.1f", hazard.distanceM)}米，请立即停止！")
                }
                hazard.distanceM < 3.0 -> {
                    suggestions.add("注意：${hazard.name} 在${hazard.direction}方向，距离约${String.format("%.1f", hazard.distanceM)}米，请小心前进")
                }
            }
        }

        // 检查路径
        if (paths.isNotEmpty()) {
            val closestPath = paths.minByOrNull { it.distanceM }
            closestPath?.let {
                suggestions.add("发现${it.name}在${it.direction}方向，距离约${String.format("%.1f", it.distanceM)}米")
            }
        }

        // 如果没有检测到任何东西
        if (hazards.isEmpty() && paths.isEmpty()) {
            suggestions.add("未检测到任何物体，可能图片内容不在支持的类别中")
        }

        return suggestions
    }

    /**
     * 快速测试 - 打印到Logcat
     */
    fun quickTest(context: Context, imagePath: String) {
        Log.d(TAG, "开始快速测试: $imagePath")
        val result = testImage(context, imagePath)
        println(result)
    }

    /**
     * 批量测试多张图片
     */
    fun batchTest(context: Context, imagePaths: List<String>): String {
        val sb = StringBuilder()
        sb.append("\n")
        sb.append("=" .repeat(60)).append("\n")
        sb.append("🔬 批量测试报告\n")
        sb.append("=" .repeat(60)).append("\n")
        sb.append("📁 测试图片数量: ${imagePaths.size}\n")
        sb.append("\n")

        imagePaths.forEachIndexed { index, path ->
            sb.append("【测试 ${index + 1}/${imagePaths.size}】\n")
            sb.append(testImage(context, path))
            sb.append("\n")
        }

        val result = sb.toString()
        Log.d(TAG, result)
        return result
    }
}
