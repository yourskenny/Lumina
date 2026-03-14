package com.example.myapplication.yolo.models

abstract class YoloModel(
    var task: String? = null,
    var format: String? = null
) {
    // val 对应 Java 中的 final，表示引用不可变
    val labels = ArrayList<String>()
}