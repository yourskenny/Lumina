package com.example.myapplication.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    // TODO: Change this to your computer's IP address if running on a physical device.
    // Emulator uses 10.0.2.2 to access host localhost.
    private const val BASE_URL = "http://10.0.2.2:5000/" 

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: LuminaApiService by lazy {
        retrofit.create(LuminaApiService::class.java)
    }
}
