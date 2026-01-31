# 麦克风诊断完整清单

## ✅ 已完成的配置检查

### 1. AndroidManifest.xml 权限声明
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```
**状态**: ✅ 已声明（第10行）

### 2. 应用运行时权限
```bash
# 检查结果
android.permission.RECORD_AUDIO: granted=true
```
**状态**: ✅ 已授予

### 3. 权限请求代码
**位置**: MainActivity.kt
- permissionLauncher 已实现
- checkPermissions() 方法已实现
**状态**: ✅ 已实现

## 🆕 新增的麦克风测试工具

### 1. MicrophoneTest.kt
一个专门的麦克风测试工具类：
- 使用 AudioRecord 直接读取麦克风数据
- 测试时长 5秒
- 实时显示音量等级
- 详细的错误信息

**位置**: `app/src/main/java/com/example/myapplication/util/MicrophoneTest.kt`

### 2. UI测试按钮
在语音识别调试面板添加了"测试麦克风"按钮

**功能**:
- 点击按钮开始测试
- 显示实时测试状态
- 显示测试结果（成功/失败）
- 自动语音播报结果

## 📋 完整的麦克风诊断步骤

### 步骤 1: 检查模拟器麦克风设置

1. **启动模拟器**
2. 在模拟器右侧边栏点击 **"..."** (三个点)
3. 选择 **"Settings"** (不是 "Extended controls")
4. 找到 **"Microphone"** 部分
5. ✅ 确保勾选: **"Virtual microphone uses host audio input"**

**截图位置**: Extended controls > Settings > Microphone

### 步骤 2: 检查 macOS 系统权限

**macOS Sequoia / Ventura / Monterey:**

1. 打开 **系统设置** (System Settings)
2. 点击 **"隐私与安全性"** (Privacy & Security)
3. 点击 **"麦克风"** (Microphone)
4. 在应用列表中找到以下应用：
   - ✅ **Android Emulator**
   - ✅ **qemu-system-aarch64**
   - ✅ **Android Studio** (如果使用)
5. 确保这些应用旁边的开关都是打开的

**如果找不到这些应用**:
- 首次启动模拟器时，macOS 会弹出权限请求
- 如果误点了"拒绝"，需要手动添加

**手动添加方法**:
```bash
# 查找模拟器进程
ps aux | grep emulator

# 找到进程路径，然后在系统设置中手动添加
```

### 步骤 3: 使用应用内测试工具

1. **启动应用**
2. 应用底部会显示语音识别调试面板
3. 点击 **"测试麦克风"** 按钮
4. **对着电脑麦克风说话 5 秒**
5. 查看测试结果：

**成功示例**:
```
✅ 麦克风工作正常! 最大音量: 2543
```

**失败示例**:
```
❌ 麦克风无信号，音量为0
❌ 麦克风有信号但音量太低: 45 (需要 >100)
❌ 没有麦克风权限
```

### 步骤 4: 查看详细日志

```bash
# 启动日志监控
adb logcat -s MicrophoneTest MainActivity VoiceRecognitionService

# 点击"测试麦克风"按钮后，应该看到：
# MicrophoneTest: AudioRecord buffer size: 3584
# MicrophoneTest: AudioRecord初始化成功
# MicrophoneTest: 开始录音测试...
# MicrophoneTest: 读取音频数据: 3584 字节, 音量: 1234
# MicrophoneTest: 检测到声音! 音量: 1234
```

### 步骤 5: 测试真实麦克风

```bash
# 在终端测试 macOS 麦克风是否工作
# 录制 5 秒音频
rec test.wav trim 0 5

# 或使用 QuickTime Player
# 文件 > 新建音频录制 > 测试是否能录音
```

## 🔍 常见问题诊断

### 问题 1: "麦克风无信号，音量为0"

**可能原因**:
1. ❌ 模拟器未启用虚拟麦克风
2. ❌ macOS 未授予麦克风权限
3. ❌ 电脑麦克风被其他应用占用

**解决步骤**:

**A. 检查模拟器设置**
```bash
# 查看模拟器启动参数
ps aux | grep emulator | grep avd

# 重启模拟器，确保启用麦克风
emulator -avd Medium_Phone_API_36.1 -no-snapshot-load &
```

**B. 检查系统权限**
```bash
# 检查麦克风访问权限（macOS）
# 在终端运行，看是否有权限提示
ffmpeg -f avfoundation -i ":0" -t 1 test.wav
```

**C. 关闭其他应用**
关闭可能占用麦克风的应用：
- Zoom
- Skype
- Discord
- Teams
- OBS
- 其他录音软件

**D. 重启模拟器**
```bash
# 完全关闭模拟器
adb emu kill

# 等待 5 秒
sleep 5

# 重新启动
emulator -avd Medium_Phone_API_36.1 -no-snapshot-load &

# 等待启动完成
adb wait-for-device

# 重新安装应用
./gradlew installDebug

# 启动应用
adb shell am start -n com.example.myapplication/.MainActivity
```

### 问题 2: "客户端错误" 或频繁重启识别

**可能原因**:
- Google 语音识别服务在模拟器中不稳定
- 网络连接问题

**解决方法**:

**方案 A: 使用真实设备（推荐）**
```bash
# 连接手机（USB 调试模式）
adb devices

# 安装到手机
./gradlew installDebug

# 启动
adb shell am start -n com.example.myapplication/.MainActivity

# 查看日志
adb logcat -s MicrophoneTest VoiceRecognitionService
```

**方案 B: 使用带 Google Play 的模拟器镜像**
- 创建新 AVD 时选择带 "Play Store" 图标的系统镜像
- 确保模拟器已登录 Google 账号

### 问题 3: "音量太低"

**可能原因**:
- 电脑麦克风音量设置太低
- 距离麦克风太远

**解决方法**:

**A. 调整系统麦克风音量**
- macOS: 系统设置 > 声音 > 输入 > 输入音量
- 将滑块调到中等以上
- 对着麦克风说话，观察音量指示器

**B. 测试距离**
- 距离麦克风 10-30cm
- 音量正常说话（不要太小声）

**C. 检查麦克风设备**
```bash
# macOS - 查看可用的音频设备
system_profiler SPAudioDataType

# 确保选择了正确的麦克风
```

## 🎯 验证清单

完成以下所有项目后，麦克风应该能正常工作：

### 模拟器配置
- [ ] Extended controls > Settings > "Virtual microphone uses host audio input" 已勾选
- [ ] 模拟器成功启动且无错误

### 系统权限
- [ ] macOS 系统设置中已授予 Android Emulator 麦克风权限
- [ ] macOS 系统设置中已授予 qemu-system-aarch64 麦克风权限

### 应用权限
- [ ] 应用已获得 RECORD_AUDIO 权限
- [ ] 应用启动时无权限错误

### 硬件测试
- [ ] macOS 系统麦克风工作正常（QuickTime 测试）
- [ ] 没有其他应用占用麦克风

### 应用测试
- [ ] 点击"测试麦克风"按钮
- [ ] 看到"AudioRecord初始化成功"日志
- [ ] 看到"读取音频数据"日志
- [ ] 对着麦克风说话时音量 > 100
- [ ] 测试结果显示 "✅ 麦克风工作正常"

## 📱 使用真实设备（最可靠的方法）

如果模拟器问题难以解决，使用真实设备：

```bash
# 1. 启用手机开发者选项和 USB 调试

# 2. 连接手机到电脑
adb devices

# 3. 安装应用
./gradlew installDebug

# 4. 启动应用
adb shell am start -n com.example.myapplication/.MainActivity

# 5. 查看日志
adb logcat -s MicrophoneTest VoiceRecognitionService

# 6. 点击"测试麦克风"，对着手机麦克风说话
```

**优势**:
- ✅ 真实硬件，100% 可靠
- ✅ 无需配置虚拟麦克风
- ✅ Google 语音服务稳定
- ✅ 真实的用户体验

## 📊 测试命令汇总

```bash
# === 环境检查 ===
# 检查模拟器状态
adb devices

# 检查应用权限
adb shell dumpsys package com.example.myapplication | grep RECORD_AUDIO

# === 实时监控 ===
# 麦克风测试日志
adb logcat -s MicrophoneTest | grep -E "音量|AudioRecord|检测到"

# 语音识别日志
adb logcat -s VoiceRecognitionService | grep -E "识别|错误|监听"

# 完整调试日志
adb logcat -s MicrophoneTest MainActivity VoiceRecognitionService CameraViewModel

# === 重启流程 ===
# 完全重启应用
adb shell am force-stop com.example.myapplication
./gradlew installDebug
adb shell am start -n com.example.myapplication/.MainActivity

# === macOS 麦克风测试 ===
# 录制 5 秒测试
rec test.wav trim 0 5

# 播放测试
play test.wav
```

## 💡 下一步

1. **首先使用应用内测试工具**
   - 点击"测试麦克风"按钮
   - 查看测试结果

2. **如果测试失败**
   - 按照上面的诊断步骤逐一检查
   - 查看日志找出具体原因

3. **如果仍然无法工作**
   - 使用真实 Android 设备测试
   - 或考虑集成离线语音识别 SDK

## 📞 技术支持

如果按照以上步骤仍无法解决，请提供：
1. `adb logcat -s MicrophoneTest` 的完整输出
2. macOS 系统版本
3. Android 模拟器版本
4. 麦克风测试结果截图
5. 系统设置 > 麦克风权限的截图
