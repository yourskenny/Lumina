#include <jni.h>

#include <android/log.h>
#define LOG_TAG "ImageProcessing"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT void JNICALL
Java_com_example_myapplication_yolo_ImageProcessing_argb2yolo(JNIEnv *env, jobject thiz,
                                                              jintArray src, jobject dest,
                                                              jint width, jint height) {
    // TODO: implement argb2yolo()
    // 1. 安全检查：如果传入的数组或 Buffer 为空，直接返回，防止 C++ 崩溃导致 App 闪退
    if (!src || !dest) {
        LOGE("Source array or Destination buffer is null");
        return;
    }

    // 2. 获取源 int 数组的指针
    jint* srcArray = env->GetIntArrayElements(src, nullptr);

    // 3. 获取目标 DirectByteBuffer 的指针，并强制转换为 float 指针
    float* destBuffer = (float*) env->GetDirectBufferAddress(dest);

    // 再次安全检查：确保内存获取成功
    if (!srcArray || !destBuffer) {
        if (srcArray) env->ReleaseIntArrayElements(src, srcArray, JNI_ABORT);
        LOGE("Failed to get array elements or direct buffer address");
        return;
    }

    int numPixels = width * height;

    // 4. 核心性能优化：在 CPU 中，乘法运算比除法快得多。
    // 将 / 255.0f 替换为 * (1.0f / 255.0f)，可以显著降低每帧处理的延迟
    const float inv255 = 0.003921568f;

    // 5. 执行 ARGB 提取、归一化并按 CHW (而非 HWC) 顺序写入目标 FloatBuffer
    // ONNX 模型要求输入格式为 NCHW (即 [1, 3, height, width])，而不是 HWC
    for (int i = 0; i < numPixels; ++i) {
        uint32_t pixel = srcArray[i];  // ARGB 格式

        // 提取 R, G, B 通道并归一化到 0.0 ~ 1.0
        float r = ((pixel >> 16) & 0xFF) * inv255;
        float g = ((pixel >> 8)  & 0xFF) * inv255;
        float b = (pixel & 0xFF)       * inv255;

        // 分通道写入 FloatBuffer: 所有的 R，接着所有的 G，接着所有的 B
        destBuffer[i]                  = r;     // Red 通道
        destBuffer[i + numPixels]      = g;     // Green 通道
        destBuffer[i + 2 * numPixels]  = b;     // Blue 通道
    }

    // 6. 释放资源
    // 核心性能优化：使用 JNI_ABORT。因为我们只是"读取"了 src 数组，并没有修改它。
    // JNI_ABORT 告诉底层不需要把 C++ 中的修改拷贝回 Java 层，节省了一次巨大的内存拷贝开销。
    env->ReleaseIntArrayElements(src, srcArray, JNI_ABORT);
}