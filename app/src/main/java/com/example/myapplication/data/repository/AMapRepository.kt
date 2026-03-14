package com.example.myapplication.data.repository

import com.example.myapplication.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AMapRepository {

    private val client = OkHttpClient()
    private val webKey = BuildConfig.AMAP_WEB_KEY // Assume user added this or use placeholder
    
    // API Endpoints
    private val SEARCH_API = "https://restapi.amap.com/v3/place/text"
    private val AROUND_API = "https://restapi.amap.com/v3/place/around"
    private val WALKING_API = "https://restapi.amap.com/v3/direction/walking"

    /**
     * 关键字搜索 (POI)
     */
    suspend fun searchPOI(keyword: String, city: String = ""): String = withContext(Dispatchers.IO) {
        if (webKey.isBlank()) return@withContext "API Key not configured"
        
        val url = "$SEARCH_API?keywords=$keyword&city=$city&key=$webKey&extensions=base"
        
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val json = response.body?.string() ?: return@withContext "Empty response"
            
            val jsonObj = JSONObject(json)
            if (jsonObj.getString("status") == "1") {
                val pois = jsonObj.getJSONArray("pois")
                if (pois.length() > 0) {
                    val result = StringBuilder()
                    for (i in 0 until minOf(3, pois.length())) {
                        val poi = pois.getJSONObject(i)
                        result.append("${poi.getString("name")} (${poi.getString("address")})\n")
                    }
                    return@withContext result.toString()
                } else {
                    return@withContext "未找到相关地点"
                }
            } else {
                return@withContext "API Error: ${jsonObj.optString("info")}"
            }
        } catch (e: Exception) {
            return@withContext "Search failed: ${e.message}"
        }
    }

    /**
     * 周边搜索
     */
    suspend fun searchNearby(keyword: String, location: String): String = withContext(Dispatchers.IO) {
        // location format: "longitude,latitude"
        if (webKey.isBlank()) return@withContext "API Key not configured"

        val url = "$AROUND_API?keywords=$keyword&location=$location&key=$webKey&radius=1000&extensions=base"
        
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val json = response.body?.string() ?: return@withContext "Empty response"
            
            val jsonObj = JSONObject(json)
            if (jsonObj.getString("status") == "1") {
                val pois = jsonObj.getJSONArray("pois")
                if (pois.length() > 0) {
                    val result = StringBuilder()
                    for (i in 0 until minOf(3, pois.length())) {
                        val poi = pois.getJSONObject(i)
                        val distance = poi.optString("distance", "?")
                        result.append("${poi.getString("name")} (距离${distance}米)\n")
                    }
                    return@withContext result.toString()
                } else {
                    return@withContext "附近未找到相关地点"
                }
            } else {
                return@withContext "API Error: ${jsonObj.optString("info")}"
            }
        } catch (e: Exception) {
            return@withContext "Nearby search failed: ${e.message}"
        }
    }
    
    /**
     * 步行规划
     */
    suspend fun planWalkingRoute(origin: String, destination: String): String = withContext(Dispatchers.IO) {
        if (webKey.isBlank()) return@withContext "API Key not configured"
        
        val url = "$WALKING_API?origin=$origin&destination=$destination&key=$webKey"
        
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val json = response.body?.string() ?: return@withContext "Empty response"
            
            val jsonObj = JSONObject(json)
            if (jsonObj.getString("status") == "1") {
                val route = jsonObj.getJSONObject("route")
                val paths = route.getJSONArray("paths")
                if (paths.length() > 0) {
                    val path = paths.getJSONObject(0)
                    val distance = path.getString("distance")
                    val duration = path.getString("duration")
                    val steps = path.getJSONArray("steps")
                    
                    val result = StringBuilder("距离: ${distance}米, 预计耗时: ${duration.toLong() / 60}分钟\n")
                    for (i in 0 until minOf(3, steps.length())) {
                        val step = steps.getJSONObject(i)
                        result.append("- ${step.getString("instruction")}\n")
                    }
                    return@withContext result.toString()
                } else {
                    return@withContext "未找到路径方案"
                }
            } else {
                return@withContext "API Error: ${jsonObj.optString("info")}"
            }
        } catch (e: Exception) {
            return@withContext "Route planning failed: ${e.message}"
        }
    }
}
