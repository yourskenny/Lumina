package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    val meta: Meta,
    val status: Status,
    val feedback: Feedback,
    val environment: Environment,
    @SerializedName("raw_objects") val rawObjects: List<RawObject>
)

data class Meta(
    val timestamp: Double,
    @SerializedName("latency_ms") val latencyMs: Int
)

data class Status(
    val state: String,
    val summary: String
)

data class Feedback(
    @SerializedName("tts_message") val ttsMessage: String,
    @SerializedName("haptic_pattern") val hapticPattern: String
)

data class Environment(
    val hazards: List<RawObject>,
    val paths: List<RawObject>
)

data class RawObject(
    val name: String,
    @SerializedName("distance_m") val distanceM: Double,
    val direction: String,
    val box: List<Double>
)
