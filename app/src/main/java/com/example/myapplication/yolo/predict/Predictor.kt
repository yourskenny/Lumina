package com.example.myapplication.yolo.predict

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import androidx.annotation.Keep
import com.example.myapplication.yolo.models.YoloModel
import org.yaml.snakeyaml.Yaml
import java.io.IOException
import java.util.ArrayList
import kotlin.collections.get
import kotlin.math.max

abstract class Predictor protected constructor(
    protected val context: Context
) {
    // 静态变量的处理
    companion object {
        // @JvmField 保证它在底层还是 Java 的 public static int，如果其它还没移植的 Java 代码用到了它，完全不报错。
        @JvmField
        var INPUT_SIZE: Int = 320
    }

    // 同样保留 ArrayList 防止底层不可预见的反射坑
    val labels: ArrayList<String> = ArrayList()

    @Throws(Exception::class)
    abstract fun loadModel(yoloModel: YoloModel, useGpu: Boolean)

    // 更加安全的异常抛出和文件读取
    @Throws(IOException::class)
    protected fun loadLabels(assetManager: AssetManager, metadataPath: String) {
        val yaml = Yaml()

        // .use 会在代码块执行完后自动安全地关闭数据流，绝不会导致内存泄漏。
        assetManager.open(metadataPath).use { inputStream ->

            // 更加安全的类型转换 (Safe Cast)
            // yaml.load 会返回一个未知的类型，我们使用 as? 进行安全转换，转换失败也不会崩溃
            val data = yaml.load(inputStream) as? Map<*, *> ?: return

            @Suppress("UNCHECKED_CAST")
            val names = data["names"] as? Map<Int, String>

            @Suppress("UNCHECKED_CAST")
            val imgszArray = data["imgsz"] as? List<Int>

            if (imgszArray != null && imgszArray.size == 2) {
                INPUT_SIZE = max(imgszArray[0], imgszArray[1])
                println("INPUT_SIZE:$INPUT_SIZE")
            }

            labels.clear()
            if (names != null) {
                labels.addAll(names.values)
            }
        }
    }

    // Java 中的 Object 对应 Kotlin 中的 Any?
    abstract fun predict(bitmap: Bitmap): Any?

    // 屏蔽了的代码同样为你保留
    // abstract fun predict(imageProxy: ImageProxy, isMirrored: Boolean)

    abstract fun setConfidenceThreshold(confidence: Float)

    abstract fun setInferenceTimeCallback(callback: FloatResultCallback)

    abstract fun setFpsRateCallback(callback: FloatResultCallback)

    interface FloatResultCallback {
        // JNI 回调，@Keep 必须保留
        @Keep
        fun onResult(result: Float)
    }
}