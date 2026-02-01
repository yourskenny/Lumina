# Windows 部署指南

## 📋 前提条件

1. ✅ 已下载 `gradle-9.1.0-bin.zip`
2. ✅ 已安装 Android Studio 或 Android SDK
3. ✅ 已安装 ADB 工具
4. ✅ 已连接 Android 真机或模拟器

---

## 🔧 步骤1: 修改Gradle配置

### 找到gradle文件位置

假设你已经下载了 `gradle-9.1.0-bin.zip`，确认文件位置：

```powershell
# 示例位置
E:\gradle-9.1.0-bin.zip
# 或
C:\Users\YourUsername\Downloads\gradle-9.1.0-bin.zip
# 或
E:\Lumina\gradle-9.1.0-bin.zip
```

### 修改配置文件

编辑 `gradle/wrapper/gradle-wrapper.properties`：

**修改前**:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.1.0-bin.zip
validateDistributionUrl=true
```

**修改后**:
```properties
# 改为你的本地文件路径
distributionUrl=file\:///E\:/gradle-9.1.0-bin.zip
validateDistributionUrl=false
```

**重要**:
- 路径中使用 `/` 而不是 `\`
- 盘符后面使用 `\:`（例如 `E\:`）
- 路径要用三个斜杠开头: `file\:///`

---

## 📱 步骤2: 编译和安装

### 2.1 拉取最新代码

```powershell
# 切换到ui-improvement分支
git checkout ui-improvement

# 拉取最新代码
git pull origin ui-improvement
```

### 2.2 清理并编译

```powershell
# 清理旧的构建
.\gradlew clean

# 编译Debug版本
.\gradlew assembleDebug
```

**预期输出**:
```
BUILD SUCCESSFUL in XXs
```

### 2.3 检查APK文件

```powershell
# 检查APK是否生成
dir app\build\outputs\apk\debug\app-debug.apk
```

### 2.4 安装到设备

```powershell
# 检查连接的设备
adb devices

# 安装APK（-r表示覆盖安装）
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 2.5 启动应用

```powershell
# 启动应用
adb shell am start -n com.example.myapplication/.MainActivity
```

---

## 🐛 常见问题

### 问题1: SSL证书错误

**错误信息**:
```
SSLHandshakeException: PKIX path building failed
```

**解决方法**: 使用本地文件路径（见步骤1）

---

### 问题2: Gradle文件找不到

**错误信息**:
```
FileNotFoundException: /E:/gradle-9.1.0-bin.zip
```

**解决方法**:
1. 确认文件存在
2. 检查路径格式是否正确
3. 使用绝对路径

---

### 问题3: 编译失败

**错误信息**:
```
Compilation failed; see the compiler error output for details
```

**解决方法**:
```powershell
# 清理缓存
.\gradlew clean

# 删除.gradle文件夹
rmdir /s /q .gradle

# 重新编译
.\gradlew assembleDebug
```

---

### 问题4: ADB设备未找到

**错误信息**:
```
error: no devices/emulators found
```

**解决方法**:
1. 检查USB调试是否开启
2. 重启ADB服务:
   ```powershell
   adb kill-server
   adb start-server
   adb devices
   ```

---

### 问题5: 真机识别器忙碌

**错误**: 语音识别显示"识别器忙碌"

**解决方法**:
1. 重启设备
2. 清除应用数据:
   ```powershell
   adb shell pm clear com.example.myapplication
   ```
3. 关闭其他使用麦克风的应用
4. 等待10秒后自动恢复

---

## 🎙️ 测试语音命令

### 中文命令测试

在真机上说出以下命令：

```
"拍照"
"查询电池"
"暂停录像"
"继续录像"
"切换摄像头"
```

### 英文命令测试

```
"take photo"
"check battery"
"pause recording"
"resume recording"
"switch camera"
```

### 查看日志

```powershell
# 实时查看应用日志
adb logcat | findstr "VoiceRecognitionService CameraViewModel"

# 保存日志到文件
adb logcat > log.txt
```

---

## 📊 UI功能测试

### 打开控制面板

1. 启动应用
2. 授予相机和麦克风权限
3. 点击右下角浮动按钮（☰）
4. 查看侧边抽屉

### 快捷功能区（14个按钮）

测试所有按钮：

| 按钮 | 预期结果 |
|------|----------|
| 拍照 | 拍照并TTS反馈 |
| 查询电池 | TTS播报电量 |
| 暂停录像 | 暂停并TTS反馈 |
| 继续录像 | 继续并TTS反馈 |
| 播放视频 | 打开播放器 |
| 分享视频 | 打开分享界面 |
| 查询存储 | TTS播报存储信息 |
| 查询时长 | TTS播报录像时长 |
| 切换相机 | 前后摄像头切换 |
| 切换闪光 | 闪光灯开关 |
| 查询位置 | TTS播报GPS位置 |
| 关闭应用 | 应用关闭 |
| 清空录像 | 删除所有视频 |
| 🚨紧急呼叫 | 拨打紧急电话 |

### 开发者工具区（6个按钮）

| 按钮 | 预期结果 |
|------|----------|
| 测试麦克风 | 显示麦克风状态 |
| 切换语言 | 中英文切换 |
| 测试识别 | 单次识别测试 |
| 极简测试 | 最简识别测试 |
| 开始/停止录音 | 开关调试录音 |
| 清空录音 | 删除调试录音 |

---

## 📝 性能测试清单

### 语音识别性能

- [ ] 中文命令识别率 > 90%
- [ ] 英文命令识别率 > 90%
- [ ] 识别延迟 < 2秒
- [ ] 无连续错误循环
- [ ] 熔断机制正常工作

### UI性能

- [ ] 侧边抽屉动画流畅
- [ ] 按钮点击响应迅速
- [ ] 状态卡片实时更新
- [ ] 可以上下滚动内容

### 相机功能

- [ ] 自动开始录像
- [ ] 暂停/继续正常
- [ ] 拍照不中断录像
- [ ] 切换摄像头正常
- [ ] 闪光灯开关正常

---

## 🚀 快速部署命令

完整的一键部署流程：

```powershell
# 1. 拉取代码
git checkout ui-improvement
git pull origin ui-improvement

# 2. 编译
.\gradlew clean assembleDebug

# 3. 安装
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 4. 启动
adb shell am start -n com.example.myapplication/.MainActivity

# 5. 查看日志
adb logcat | findstr "VoiceRecognitionService CameraViewModel"
```

---

## 📞 支持资源

### 文档

- `语音命令完整文档.md` - 所有语音命令的详细说明
- `UI美化说明.md` - UI改进的详细说明
- `ERROR_RECOGNIZER_BUSY修复报告.md` - 识别器错误修复详情

### Git分支

- `main` - 稳定版本
- `Androidd` - Android开发分支
- `ui-improvement` - UI改进分支（当前）

### 相关Commits

- `36f1aee` - 添加英文语音命令支持
- `13e89b7` - 移除底部按钮，统一到侧边抽屉
- `c7bc0db` - 添加所有缺失的语音命令按钮
- `10d7c25` - 添加紧急呼叫按钮

---

**文档版本**: v1.0
**更新日期**: 2026-02-02
**适用平台**: Windows 10/11
**作者**: Claude Opus 4.5
