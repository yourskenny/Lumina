package com.example.myapplication.yolo.predict.detect

import android.graphics.RectF
import androidx.annotation.Keep

// @Keep 注解非常重要，它告诉代码混淆工具（如 R8/ProGuard）不要修改或删除这个类。
// 因为 C++ 层的 JNI 代码可能是通过包名和类名的硬编码来寻找它的。
@Keep
data class DetectedObject(
    // 极其重要：这里必须使用 Float? (可空)，而不能是 Float
    val confidence: Float?,

    val boundingBox: RectF,

    // 原始 Java 中是 int，所以 Kotlin 使用 Int (不可空)
    val index: Int,

    val label: String
) {
    // 补充说明：Kotlin 的 data class 会自动为上面的 4 个属性生成 getter 方法。
    // 即：getConfidence(), getBoundingBox(), getIndex(), getLabel()
    // 这样就完美兼容了原 Java 代码中使用 @Keep 注解的那些 getter 方法，JNI 调用不会断裂。

    // （可选）还原 Java 中的安全拷贝特性：
    // 在你的原 Java 代码中，getBoundingBox() 返回的是 new RectF(boundingBox)。
    // 这是一个防御性拷贝（防止外部修改 RectF 的坐标）。
    // 如果你希望在 Kotlin 中 100% 保持这种行为，可以显式重写 getter（如下）：
    /*
    val boundingBoxCopy: RectF
        @Keep get() = RectF(boundingBox)
    */
}