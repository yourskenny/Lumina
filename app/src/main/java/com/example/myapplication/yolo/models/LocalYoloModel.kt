package com.example.myapplication.yolo.models

class LocalYoloModel(
    task: String,
    format: String,
    val modelPath: String,
    val metadataPath: String
) : YoloModel(task, format) // 直接调用父类构造器进行赋值