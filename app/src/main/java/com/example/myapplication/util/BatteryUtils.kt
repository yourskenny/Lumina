package com.example.myapplication.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * 电池工具类
 * 提供电池状态查询功能
 */
object BatteryUtils {

    /**
     * 获取当前电池电量百分比
     * @param context 上下文
     * @return 电池电量（0-100）
     */
    fun getBatteryLevel(context: Context): Int {
        val batteryStatus: Intent? = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        return if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            -1
        }
    }

    /**
     * 获取电池充电状态
     * @param context 上下文
     * @return 充电状态描述
     */
    fun getChargingStatus(context: Context): String {
        val batteryStatus: Intent? = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
            BatteryManager.BATTERY_STATUS_FULL -> "已充满"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
            else -> "未知"
        }
    }

    /**
     * 检查是否正在充电
     * @param context 上下文
     * @return 是否正在充电
     */
    fun isCharging(context: Context): Boolean {
        val batteryStatus: Intent? = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    /**
     * 获取电池温度（摄氏度）
     * @param context 上下文
     * @return 电池温度
     */
    fun getBatteryTemperature(context: Context): Float {
        val batteryStatus: Intent? = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val temperature = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1

        return if (temperature >= 0) {
            temperature / 10f // 转换为摄氏度
        } else {
            -1f
        }
    }

    /**
     * 获取完整的电池信息描述
     * @param context 上下文
     * @return 电池信息描述
     */
    fun getBatteryInfo(context: Context): String {
        val level = getBatteryLevel(context)
        val status = getChargingStatus(context)

        return if (level >= 0) {
            "电池电量${level}%，${status}"
        } else {
            "无法获取电池信息"
        }
    }
}
