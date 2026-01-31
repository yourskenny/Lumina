# 语音识别调试指南

## 当前状态

✅ **已添加的调试功能：**
1. **实时字幕显示** - 在屏幕底部中间位置显示识别的文本
2. **麦克风音量指示器** - 显示音量等级（0-10级）的可视化进度条
3. **监听状态指示器** - 显示 "🎤 监听中..." 或 "🔇 未监听"
4. **错误信息显示** - 当出现错误时以红色文字显示具体错误原因
5. **详细日志输出** - 在logcat中输出所有语音识别事件

## 日志分析结果

从logcat日志来看，语音识别服务正在运行，但遇到以下问题：

### 错误类型：
- **错误码 5** (ERROR_CLIENT): 客户端错误 - 通常是识别器忙碌或配置问题
- **错误码 7** (ERROR_NO_MATCH): 未识别到语音 - 可能是音量太低或麦克风未工作

### 当前状态：
- ✅ 语音识别服务已启动
- ✅ 可以检测到说话开始
- ❌ 未能成功识别任何文本
- ⚠️ 音量等级为0（可能麦克风未接收到音频）

## 模拟器麦克风调试步骤

### 步骤 1: 检查模拟器麦克风设置

1. 在模拟器窗口右侧，点击 **"..."** (Extended controls)
2. 选择 **"Microphone"** 选项
3. 确保：
   - ✅ "Virtual microphone uses host audio input" 已勾选
   - ✅ 选择了正确的电脑麦克风设备

### 步骤 2: 检查电脑麦克风权限

**macOS 系统：**
1. 打开 **系统设置** → **隐私与安全性** → **麦克风**
2. 确保 **Android Emulator** 或 **qemu-system-aarch64** 有麦克风权限
3. 如果没有，勾选允许访问

### 步骤 3: 检查应用权限

在应用启动时应该会弹出权限请求，确保授予：
- ✅ 相机权限
- ✅ **麦克风权限（RECORD_AUDIO）**
- ✅ 通知权限（Android 13+）

### 步骤 4: 测试麦克风

运行以下命令测试麦克风是否工作：

```bash
# 检查应用权限状态
adb shell dumpsys package com.example.myapplication | grep "android.permission.RECORD_AUDIO"

# 查看实时日志
adb logcat -s MainActivity VoiceRecognitionService CameraViewModel | grep -E "语音|音量|识别"
```

### 步骤 5: 在UI上观察调试信息

应用界面底部中间应该显示：

```
🎤 监听中...
音量: ████████░░ 8/10
识别: 拍照
```

或者显示错误：
```
🔇 未监听
⚠️ 权限不足
```

## 常见问题解决

### 问题 1: 显示 "权限不足"

**解决方法：**
```bash
# 重新授予权限
adb shell pm grant com.example.myapplication android.permission.RECORD_AUDIO

# 重启应用
adb shell am force-stop com.example.myapplication
adb shell am start -n com.example.myapplication/.MainActivity
```

### 问题 2: 音量一直为 0

**可能原因：**
1. 模拟器未选择主机麦克风
2. macOS 未授予模拟器麦克风权限
3. 电脑麦克风被其他应用占用

**解决方法：**
1. 关闭其他使用麦克风的应用（Zoom、Discord等）
2. 重启模拟器
3. 检查 macOS 系统麦克风权限

### 问题 3: 一直显示 "客户端错误"

**可能原因：**
- Google语音识别服务在模拟器中不可用
- 需要Google Play服务支持

**解决方法：**
```bash
# 检查Google Play服务状态
adb shell pm list packages | grep google

# 如果没有Google Play服务，需要使用带Play的镜像
# 创建新的AVD时选择 "Play Store" 版本的系统镜像
```

### 问题 4: 识别的语言不对

当前代码设置为中文识别，如果需要测试英文：

在 `VoiceRecognitionService.kt` 的第54行修改：
```kotlin
// 改为英文
putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())

// 或中文（当前设置）
putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINESE.toString())
```

## 实时监控命令

### 查看完整调试信息
```bash
adb logcat -s MainActivity VoiceRecognitionService CameraViewModel
```

### 只看错误信息
```bash
adb logcat *:E | grep -E "Voice|Speech|Audio"
```

### 查看麦克风音量变化
```bash
adb logcat -s MainActivity | grep "音量"
```

## 测试语音命令

在UI上看到 "🎤 监听中..." 时，可以尝试以下命令：

### 相机控制
- "拍照"
- "暂停录像"
- "继续录像"
- "切换摄像头"
- "打开闪光灯"

### 查询命令
- "查询存储空间"
- "查询电池"
- "查询位置"
- "查询录像时长"

### 视频管理
- "播放视频"
- "分享视频"
- "清空录像"

## 如果还是无法工作

### 方案 1: 使用真实Android设备
```bash
# 连接手机
adb devices

# 安装应用
./gradlew installDebug

# 查看日志
adb logcat -s MainActivity VoiceRecognitionService
```

### 方案 2: 添加备用输入方式
可以考虑添加：
- 文本输入框进行命令测试
- 按钮式命令选择界面
- 蓝牙耳机语音输入

### 方案 3: 使用离线语音识别
如果Google服务不可用，可以集成第三方SDK：
- Vosk（离线语音识别）
- PocketSphinx
- 百度语音识别SDK

## 代码修改位置

如果需要进一步调试，可以修改这些文件：

1. **VoiceRecognitionService.kt** (第1-257行)
   - 语音识别核心逻辑
   - 错误处理和调试信息

2. **MainActivity.kt** (第1-201行)
   - 权限检查
   - 调试信息处理

3. **CameraScreen.kt** (第261-324行)
   - VoiceDebugOverlay UI组件
   - 实时字幕显示

4. **CameraViewModel.kt** (第495-505行)
   - updateVoiceDebugInfo 方法
   - 语音命令处理

## 联系方式

如果问题仍然存在，请提供：
1. logcat完整日志
2. 模拟器/设备型号
3. Android版本
4. 屏幕截图（显示调试面板）
