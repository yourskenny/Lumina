package com.example.myapplication.data.remote

import com.example.myapplication.data.model.ApiResponse
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface LuminaApiService {
    @Multipart
    @POST("analyze")
    suspend fun analyzeImage(@Part image: MultipartBody.Part): ApiResponse
}
