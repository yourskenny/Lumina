package com.example.myapplication.yolo

import java.nio.ByteBuffer

class ImageProcessing {

    // 静态代码块必须放在 companion object 的 init 中
    companion object {
        init {
            System.loadLibrary("image_processing")
        }
    }

    // native 关键字在 Kotlin 中变成了 external
    // 参数类型必须绝对精准对接 C++ 签名
    external fun argb2yolo(
        src: IntArray,
        dest: ByteBuffer,
        width: Int,
        height: Int
    )
}