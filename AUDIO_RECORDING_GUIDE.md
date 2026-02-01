# 录音功能使用指南

## 功能概述

应用现在支持**语音识别同步录音**功能，可以在进行语音识别的同时录制音频，方便后续核对和调试。

## 主要功能

### 1. 录音控制
- **开始录音按钮** (`🎙 开始录音`)
  - 点击后开始录制音频
  - 录音期间可以同时进行语音识别
  - 按钮会变为 `⏹ 停止录音`

- **停止录音按钮** (`⏹ 停止录音`)
  - 点击后停止录制并保存音频文件
  - 文件自动保存到应用私有目录

### 2. 录音状态显示
- **🔴 正在录音** - 录音进行中
- **⚫ 未录音** - 当前未录音
- **录音文件: N** - 显示已保存的录音文件数量

### 3. 文件管理
- **清空录音按钮** - 删除所有录音文件
- 当有录音文件时此按钮可用

## 文件保存位置

录音文件保存在：
```
/storage/emulated/0/Android/data/com.example.myapplication/files/VoiceRecordings/
```

文件命名格式：
```
voice_yyyyMMdd_HHmmss.m4a
```

例如：`voice_20260201_184500.m4a`

## 音频格式

- **格式**: MPEG-4 (.m4a)
- **编码**: AAC
- **采样率**: 16,000 Hz
- **比特率**: 128 kbps

## 使用场景

### 场景1：调试语音识别
1. 点击"开始录音"
2. 说出语音命令（如"拍照"）
3. 观察识别结果
4. 点击"停止录音"
5. 导出音频文件进行核对

### 场景2：测试命令匹配
1. 开始录音
2. 使用界面上的快捷命令按钮测试
3. 查看录音文件数量增加
4. 停止录音并保存

### 场景3：长时间监听测试
1. 开始录音
2. 让应用持续监听一段时间
3. 说出多个命令
4. 停止录音
5. 通过录音文件回顾整个过程

## 导出录音文件

### 方法1：使用ADB
```bash
# 列出所有录音文件
adb shell ls -la /storage/emulated/0/Android/data/com.example.myapplication/files/VoiceRecordings/

# 导出单个文件
adb pull /storage/emulated/0/Android/data/com.example.myapplication/files/VoiceRecordings/voice_20260201_184500.m4a ~/Desktop/

# 导出所有录音文件
adb pull /storage/emulated/0/Android/data/com.example.myapplication/files/VoiceRecordings/ ~/Desktop/VoiceRecordings/
```

### 方法2：在设备上
1. 使用文件管理器导航到上述路径
2. 选择录音文件
3. 分享到其他应用或设备

## 注意事项

1. **权限要求**
   - 需要麦克风权限（`RECORD_AUDIO`）
   - 首次使用时会自动请求权限

2. **存储空间**
   - 录音文件会占用设备存储空间
   - 建议定期清空旧的录音文件
   - 每小时约占用 56 MB（16kHz, 128kbps）

3. **隐私提示**
   - 录音文件存储在应用私有目录
   - 卸载应用时会自动删除所有录音文件
   - 其他应用无法直接访问

4. **性能影响**
   - 录音和语音识别同时运行
   - 在低端设备上可能影响性能
   - 建议在测试时使用，正常使用时关闭

## 技术细节

### AudioRecordingService 类
- 位置: `domain/service/AudioRecordingService.kt`
- 功能: 管理音频录制、文件保存和删除
- 使用 MediaRecorder API

### 集成到 ViewModel
- `CameraViewModel.startAudioRecording()` - 开始录音
- `CameraViewModel.stopAudioRecording()` - 停止录音
- `CameraViewModel.toggleAudioRecording()` - 切换录音状态
- `CameraViewModel.clearAllAudioRecordings()` - 清空所有录音

### UI 组件
- `VoiceDebugOverlay` - 显示录音控制和状态
- 录音按钮、状态指示器、文件计数器

## 故障排查

### 问题1：录音按钮无响应
- 检查麦克风权限是否授予
- 查看 Logcat 中的错误信息
- 重启应用

### 问题2：录音文件无法播放
- 确认文件已完整保存（停止录音后）
- 检查文件大小（不应为0）
- 使用支持 AAC/M4A 格式的播放器

### 问题3：找不到录音文件
- 确认录音已停止（显示"未录音"）
- 使用 ADB 命令检查文件列表
- 检查存储空间是否充足

## 示例日志

正常录音流程的日志输出：
```
D/CameraViewModel: 录音文件总数: 0
D/AudioRecordingService: 开始录音: /storage/.../voice_20260201_184500.m4a
D/CameraViewModel: 开始录音: /storage/.../voice_20260201_184500.m4a
D/AudioRecordingService: 停止录音: /storage/.../voice_20260201_184500.m4a
D/CameraViewModel: 录音已保存: /storage/.../voice_20260201_184500.m4a
D/CameraViewModel: 录音文件总数: 1
```

## 更新日志

### 2026-02-01
- ✅ 添加 AudioRecordingService 服务
- ✅ 集成录音功能到 CameraViewModel
- ✅ 添加录音控制按钮到 UI
- ✅ 显示录音状态和文件计数
- ✅ 支持清空所有录音文件
- ✅ 自动释放资源避免内存泄漏

## 下一步优化

可以考虑的改进方向：
1. 添加录音时长显示
2. 支持播放录音文件
3. 支持重命名录音文件
4. 添加录音质量选项（高/中/低）
5. 支持暂停/恢复录音
6. 添加录音波形显示
7. 支持导出为其他格式（WAV, MP3）
