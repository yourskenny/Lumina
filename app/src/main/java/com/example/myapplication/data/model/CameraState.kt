package com.example.myapplication.data.model

/**
 * 相机状态的封闭类
 * 表示相机的各种工作状态
 */
sealed class CameraState {
    /**
     * 空闲状态 - 相机未初始化
     */
    object Idle : CameraState()

    /**
     * 就绪状态 - 相机已初始化,准备好使用
     */
    object Ready : CameraState()

    /**
     * 录像中状态
     * @param durationSeconds 当前录像已持续的秒数
     */
    data class Recording(val durationSeconds: Int) : CameraState()

    /**
     * 暂停状态 - 录像已暂停
     */
    object Paused : CameraState()

    /**
     * 错误状态
     * @param message 错误描述信息
     */
    data class Error(val message: String) : CameraState()
}
