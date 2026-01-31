# 语音识别调试功能 - 实现总结

## ✅ 已实现的功能

### 1. 实时字幕显示
- **位置**: 屏幕底部中间（控制按钮上方）
- **显示内容**:
  - 识别的文本（部分结果和最终结果）
  - 成功识别的命令会显示 "✓" 标记
- **文件**: `CameraScreen.kt` 的 `VoiceDebugOverlay` 组件

### 2. 麦克风音量指示器
- **显示方式**: 10级可视化进度条（█ 和 ░ 字符）
- **实时更新**: 根据onRmsChanged回调更新
- **范围**: 0-10级
- **示例**: `████████░░ 8/10`

### 3. 监听状态指示器
- **状态图标**:
  - 🎤 监听中... (蓝色)
  - 🔇 未监听 (灰色)
- **实时反馈**: 显示当前是否在接收语音

### 4. 详细错误信息
- **错误类型显示**:
  - 音频录制错误
  - 权限不足
  - 网络错误
  - 未识别到语音
  - 等等...
- **显示方式**: 红色警告文字 ⚠️
- **位置**: 调试面板底部

### 5. 完整日志输出
所有语音事件都会输出到logcat，包括：
- 准备接收语音
- 开始/结束说话
- 识别结果（部分和完整）
- 音量变化
- 错误详情

## 📝 修改的文件

### 1. VoiceRecognitionService.kt
```kotlin
// 新增数据类
data class VoiceDebugInfo(
    val text: String,
    val isPartial: Boolean,
    val volume: Float,
    val isListening: Boolean,
    val error: String? = null
)

// 新增构造函数参数
class VoiceRecognitionService(
    // ...
    private val onDebugInfo: ((VoiceDebugInfo) -> Unit)? = null
)

// 新增功能
- 音量监测 (onRmsChanged)
- 详细错误分类
- 调试信息回调
```

### 2. MainActivity.kt
```kotlin
// 新增导入
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts

// 新增功能
- 权限请求器 (permissionLauncher)
- 权限检查方法 (checkPermissions)
- 调试信息处理 (handleDebugInfo)
- 语音服务初始化时传递调试回调
```

### 3. CameraViewModel.kt
```kotlin
// CameraUiState 新增字段
data class CameraUiState(
    // ...
    val voiceDebugText: String = "",
    val voiceVolume: Float = 0f,
    val voiceError: String? = null
)

// 新增方法
fun updateVoiceDebugInfo(debugInfo: VoiceDebugInfo)
```

### 4. CameraScreen.kt
```kotlin
// 新增 VoiceDebugOverlay 组件
@Composable
fun VoiceDebugOverlay(
    modifier: Modifier,
    voiceText: String,
    volume: Float,
    isListening: Boolean,
    error: String?
)

// 在主界面添加调试面板
VoiceDebugOverlay(
    modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 120.dp),
    voiceText = uiState.voiceDebugText,
    volume = uiState.voiceVolume,
    isListening = uiState.isVoiceListening,
    error = uiState.voiceError
)
```

## 🔍 调试方法

### 方法 1: 查看UI调试面板
在应用运行时，屏幕底部会显示：
```
🎤 监听中...
音量: ████████░░ 8/10
识别: 拍照
```

### 方法 2: 查看logcat日志
```bash
# 实时查看所有语音事件
adb logcat -s MainActivity VoiceRecognitionService CameraViewModel

# 只看错误
adb logcat -s VoiceRecognitionService | grep "错误"

# 查看音量变化
adb logcat -s MainActivity | grep "音量"
```

### 方法 3: 检查权限状态
```bash
# 查看麦克风权限
adb shell dumpsys package com.example.myapplication | grep RECORD_AUDIO

# 手动授予权限
adb shell pm grant com.example.myapplication android.permission.RECORD_AUDIO
```

## ⚠️ 常见问题

### 1. 模拟器麦克风不工作
**症状**: 音量一直为0，显示"未识别到语音"

**解决步骤**:
1. 模拟器右侧点击 "..." → Microphone
2. 勾选 "Virtual microphone uses host audio input"
3. macOS: 系统设置 → 隐私与安全性 → 麦克风 → 允许Android Emulator
4. 重启模拟器

### 2. 显示"权限不足"
**症状**: 显示 ⚠️ 权限不足

**解决方法**:
```bash
adb shell pm grant com.example.myapplication android.permission.RECORD_AUDIO
```

### 3. 显示"客户端错误"
**症状**: 频繁显示"客户端错误"

**原因**: Google语音识别服务可能不可用

**解决方法**:
- 使用带Google Play的系统镜像
- 或使用真实设备测试
- 或集成第三方语音SDK

## 📊 测试结果

根据logcat日志分析：

### ✅ 工作正常的部分
- 语音识别服务启动
- 权限已正确授予
- 可以检测到说话开始
- 调试信息正确输出到日志和UI

### ❌ 需要解决的问题
- 未能成功识别文本
- 音量等级为0（可能麦克风未配置）
- Google语音服务可能在模拟器中不稳定

## 🎯 下一步建议

1. **使用真实Android设备测试** - 这是最可靠的方法
2. **配置模拟器麦克风** - 按照 VOICE_DEBUG_GUIDE.md 的步骤
3. **添加文本输入测试** - 作为备用调试方案
4. **考虑离线语音SDK** - 如Vosk或PocketSphinx

## 📱 快速测试命令

```bash
# 1. 重新安装应用
./gradlew installDebug

# 2. 启动应用
adb shell am start -n com.example.myapplication/.MainActivity

# 3. 实时监控
adb logcat -s VoiceRecognitionService MainActivity | grep -E "监听|识别|音量|错误"

# 4. 测试语音命令
# 对着麦克风说："拍照"、"暂停录像"、"查询电池"
```

## 📄 相关文档

- `VOICE_DEBUG_GUIDE.md` - 详细的调试步骤和问题解决
- `VoiceRecognitionService.kt` - 语音识别服务实现
- `CameraScreen.kt` - UI调试面板实现

## ✨ UI效果预览

调试面板会显示如下内容：

```
┌─────────────────────────────────────┐
│   🎤 监听中...                       │
│   音量: ████████░░ 8/10              │
│   识别: ✓ 拍照                       │
└─────────────────────────────────────┘
```

或错误状态：

```
┌─────────────────────────────────────┐
│   🔇 未监听                          │
│   ⚠️ 权限不足                        │
└─────────────────────────────────────┘
```
