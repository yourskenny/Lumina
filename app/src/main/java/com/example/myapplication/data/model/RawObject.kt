package com.example.myapplication.data.model

data class RawObject(
    val name: String,
    val distanceM: Double,
    val direction: String,
    val box: List<Double>
)
