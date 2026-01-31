package com.example.myapplication.domain.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import com.example.myapplication.data.repository.SettingsRepository

/**
 * 紧急求助服务
 * 提供紧急联系人拨打电话和发送短信功能
 */
class EmergencyService(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val TAG = "EmergencyService"

    /**
     * 拨打紧急联系人电话
     * @return 是否成功启动拨号
     */
    fun callEmergencyContact(): Boolean {
        val phoneNumber = settingsRepository.emergencyContactPhone

        if (phoneNumber.isEmpty()) {
            Log.w(TAG, "未设置紧急联系人")
            return false
        }

        return try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)
            Log.d(TAG, "拨打紧急电话: $phoneNumber")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "拨打电话权限不足", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "拨打电话失败", e)
            false
        }
    }

    /**
     * 发送紧急求助短信
     * @param message 短信内容（可选，默认使用预设内容）
     * @return 是否成功发送
     */
    fun sendEmergencySMS(message: String? = null): Boolean {
        val phoneNumber = settingsRepository.emergencyContactPhone

        if (phoneNumber.isEmpty()) {
            Log.w(TAG, "未设置紧急联系人")
            return false
        }

        val defaultMessage = "【紧急求助】我需要帮助，请尽快联系我。- 无障碍相机应用"
        val smsContent = message ?: defaultMessage

        return try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            smsManager.sendTextMessage(
                phoneNumber,
                null,
                smsContent,
                null,
                null
            )

            Log.d(TAG, "发送紧急短信到: $phoneNumber")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "发送短信权限不足", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "发送短信失败", e)
            false
        }
    }

    /**
     * 获取紧急联系人信息
     * @return 联系人姓名和电话
     */
    fun getEmergencyContactInfo(): Pair<String, String> {
        return Pair(
            settingsRepository.emergencyContactName,
            settingsRepository.emergencyContactPhone
        )
    }

    /**
     * 设置紧急联系人
     * @param name 联系人姓名
     * @param phone 联系人电话
     */
    fun setEmergencyContact(name: String, phone: String) {
        settingsRepository.emergencyContactName = name
        settingsRepository.emergencyContactPhone = phone
        Log.d(TAG, "设置紧急联系人: $name ($phone)")
    }

    /**
     * 检查是否设置了紧急联系人
     * @return 是否已设置
     */
    fun hasEmergencyContact(): Boolean {
        return settingsRepository.hasEmergencyContact()
    }

    /**
     * 清除紧急联系人
     */
    fun clearEmergencyContact() {
        settingsRepository.emergencyContactName = ""
        settingsRepository.emergencyContactPhone = ""
        Log.d(TAG, "清除紧急联系人")
    }

    /**
     * 格式化电话号码（添加分隔符）
     * @param phone 原始电话号码
     * @return 格式化后的电话号码
     */
    fun formatPhoneNumber(phone: String): String {
        if (phone.length == 11 && phone.startsWith("1")) {
            // 中国手机号：138-1234-5678
            return "${phone.substring(0, 3)}-${phone.substring(3, 7)}-${phone.substring(7)}"
        }
        return phone
    }
}
