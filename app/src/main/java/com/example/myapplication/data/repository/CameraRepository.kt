package com.example.myapplication.data.repository

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication.data.model.CameraState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 相机仓库
 * 负责CameraX的初始化、预览、录像和拍照功能
 */
class CameraRepository(private val context: Context) {

    private val TAG = "CameraRepository"

    // 相机执行器
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // CameraX组件
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: androidx.camera.core.Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: PreviewView? = null

    // 相机选择器（前/后摄像头）
    private var currentCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var isBackCamera = true

    // 闪光灯状态
    private var flashEnabled = false

    // 相机状态流
    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    val cameraState: Flow<CameraState> = _cameraState.asStateFlow()

    // 录像时长计数
    private var recordingDurationSeconds = 0

    /**
     * 初始化相机
     * @param lifecycleOwner 生命周期所有者
     * @param previewView 预览视图
     */
    suspend fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ): Result<Unit> = suspendCoroutine { continuation ->
        // 保存引用以便后续切换摄像头
        this.lifecycleOwner = lifecycleOwner
        this.previewView = previewView

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                // 构建预览用例
                preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // 构建图像捕获用例
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                // 构建视频捕获用例
                val qualitySelector = QualitySelector.from(
                    Quality.HD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                )
                val recorder = Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)

                // 使用当前摄像头选择器
                val cameraSelector = currentCameraSelector

                // 解绑所有用例
                cameraProvider?.unbindAll()

                // 绑定用例到相机
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture
                )

                _cameraState.value = CameraState.Ready
                continuation.resume(Result.success(Unit))

            } catch (e: Exception) {
                Log.e(TAG, "相机初始化失败", e)
                _cameraState.value = CameraState.Error("相机初始化失败: ${e.message}")
                continuation.resume(Result.failure(e))
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * 开始录像
     * @param mediaRepository 媒体仓库,用于创建输出配置
     * @param onEvent 录像事件回调
     */
    fun startRecording(
        mediaRepository: MediaRepository,
        onEvent: (VideoRecordEvent) -> Unit
    ) {
        val videoCapture = this.videoCapture ?: run {
            Log.e(TAG, "VideoCapture未初始化")
            return
        }

        // 创建输出选项
        val outputOptions = mediaRepository.createVideoOutputOptions()

        // 开始录像
        activeRecording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        Log.d(TAG, "录像开始")
                        recordingDurationSeconds = 0
                        _cameraState.value = CameraState.Recording(recordingDurationSeconds)
                    }
                    is VideoRecordEvent.Status -> {
                        recordingDurationSeconds = (event.recordingStats.recordedDurationNanos / 1_000_000_000).toInt()
                        _cameraState.value = CameraState.Recording(recordingDurationSeconds)
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            Log.e(TAG, "录像错误: ${event.cause?.message}")
                            _cameraState.value = CameraState.Error("录像失败: ${event.cause?.message}")
                        } else {
                            Log.d(TAG, "录像完成: ${event.outputResults.outputUri}")
                            _cameraState.value = CameraState.Ready
                        }
                        activeRecording = null
                    }
                }
                onEvent(event)
            }
    }

    /**
     * 暂停录像
     */
    fun pauseRecording() {
        activeRecording?.pause()
        _cameraState.value = CameraState.Paused
        Log.d(TAG, "录像已暂停")
    }

    /**
     * 恢复录像
     */
    fun resumeRecording() {
        activeRecording?.resume()
        _cameraState.value = CameraState.Recording(recordingDurationSeconds)
        Log.d(TAG, "录像已恢复")
    }

    /**
     * 停止录像
     */
    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
        _cameraState.value = CameraState.Ready
        Log.d(TAG, "录像已停止")
    }

    /**
     * 在录像过程中拍照
     * @param outputFile 输出文件
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    fun capturePhotoWhileRecording(
        outputFile: java.io.File,
        onSuccess: () -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        val imageCapture = this.imageCapture ?: run {
            Log.e(TAG, "ImageCapture未初始化")
            return
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "照片已保存: ${outputFile.absolutePath}")
                    onSuccess()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "拍照失败", exception)
                    onError(exception)
                }
            }
        )
    }

    /**
     * 检查是否正在录像
     */
    fun isRecording(): Boolean {
        return activeRecording != null && _cameraState.value is CameraState.Recording
    }

    /**
     * 检查是否已暂停
     */
    fun isPaused(): Boolean {
        return _cameraState.value is CameraState.Paused
    }

    /**
     * 切换摄像头（前/后）
     * @return 切换后的摄像头类型（"前置"/"后置"）
     */
    fun switchCamera(): String {
        if (isRecording()) {
            Log.w(TAG, "录像中无法切换摄像头")
            return if (isBackCamera) "后置" else "前置"
        }

        // 切换摄像头选择器
        currentCameraSelector = if (isBackCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        isBackCamera = !isBackCamera

        // 重新绑定相机
        rebindCamera()

        val cameraType = if (isBackCamera) "后置" else "前置"
        Log.d(TAG, "切换到${cameraType}摄像头")
        return cameraType
    }

    /**
     * 重新绑定相机
     */
    private fun rebindCamera() {
        val lifecycleOwner = this.lifecycleOwner ?: return
        val previewView = this.previewView ?: return

        try {
            // 解绑所有用例
            cameraProvider?.unbindAll()

            // 重新绑定
            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                currentCameraSelector,
                preview,
                imageCapture,
                videoCapture
            )

            Log.d(TAG, "相机重新绑定成功")
        } catch (e: Exception) {
            Log.e(TAG, "相机重新绑定失败", e)
        }
    }

    /**
     * 切换闪光灯状态
     * @return 新的闪光灯状态（true=开启，false=关闭）
     */
    fun toggleFlashlight(): Boolean {
        flashEnabled = !flashEnabled
        setFlashlightInternal(flashEnabled)
        return flashEnabled
    }

    /**
     * 设置闪光灯状态
     * @param enabled 是否开启闪光灯
     */
    fun setFlashlight(enabled: Boolean) {
        flashEnabled = enabled
        setFlashlightInternal(enabled)
    }

    /**
     * 内部设置闪光灯方法
     */
    private fun setFlashlightInternal(enabled: Boolean) {
        try {
            val cameraControl = camera?.cameraControl
            if (cameraControl == null) {
                Log.w(TAG, "相机控制器未初始化")
                return
            }

            cameraControl.enableTorch(enabled)
            Log.d(TAG, "闪光灯${if (enabled) "开启" else "关闭"}")
        } catch (e: Exception) {
            Log.e(TAG, "设置闪光灯失败", e)
        }
    }

    /**
     * 查询闪光灯状态
     * @return 闪光灯是否开启
     */
    fun isFlashlightOn(): Boolean {
        return flashEnabled
    }

    /**
     * 获取当前摄像头类型
     * @return "前置" 或 "后置"
     */
    fun getCurrentCameraType(): String {
        return if (isBackCamera) "后置" else "前置"
    }

    /**
     * 获取当前录像时长（秒）
     * @return 录像时长
     */
    fun getCurrentRecordingDuration(): Int {
        return recordingDurationSeconds
    }

    /**
     * 释放相机资源
     */
    fun release() {
        stopRecording()
        setFlashlight(false) // 关闭闪光灯
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        Log.d(TAG, "相机资源已释放")
    }
}
