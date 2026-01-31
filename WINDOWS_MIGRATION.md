# 🪟 Windows 平台迁移指南

## ✅ 已完成

所有代码已成功提交到远端：
- 分支：`Androidd`
- Commit: `ce5b33e - feat(android): 完整语音识别功能和调试系统`
- 远端：`origin/Androidd`

## 📋 Windows 上的准备工作

### 1. 安装必要软件

#### Android Studio
```
下载地址: https://developer.android.com/studio
版本: 最新稳定版 (2024+)
```

#### Git for Windows
```
下载地址: https://git-scm.com/download/win
推荐: Git Bash
```

#### JDK
```
Android Studio会自动安装JDK
或下载: https://www.oracle.com/java/technologies/downloads/
版本: JDK 17 或更高
```

### 2. 克隆项目

打开 **Git Bash** 或 **PowerShell**：

```bash
# 克隆仓库
cd C:/Users/YourUsername/Projects  # 选择你的工作目录
git clone https://github.com/yourskenny/Lumina.git
cd Lumina

# 切换到Android分支
git checkout Androidd

# 确认代码是最新的
git pull origin Androidd
```

### 3. 用Android Studio打开项目

1. **启动Android Studio**
2. **File → Open**
3. 选择克隆的项目目录（包含 `build.gradle` 的目录）
4. 等待 **Gradle Sync** 完成（首次可能需要10-20分钟）

### 4. 配置Android SDK

Android Studio会自动提示安装：
- ✅ Android SDK Platform 36 (API Level 36)
- ✅ Android SDK Build-Tools
- ✅ Android Emulator
- ✅ Google Play Services

### 5. 创建Android虚拟设备（AVD）

1. **Tools → Device Manager**
2. **Create Device**
3. 选择：**Phone → Pixel 6** 或类似
4. 系统镜像：**API 36** (with Google Play)
5. 点击 **Finish**

## 🚀 在Windows上运行

### 方法1: 使用Android Studio

1. 点击顶部工具栏的 **绿色三角形 ▶️** (Run)
2. 选择你创建的AVD
3. 等待模拟器启动
4. 应用会自动安装和启动

### 方法2: 使用命令行

打开 **Terminal** (Android Studio底部):

```bash
# 构建项目
./gradlew build

# 安装到设备
./gradlew installDebug

# 启动应用
adb shell am start -n com.example.myapplication/.MainActivity
```

**注意**：Windows上使用 `gradlew.bat` 而不是 `./gradlew`：
```powershell
gradlew.bat build
gradlew.bat installDebug
```

## 📱 连接真实Android设备（推荐）

### 1. 启用开发者选项

在手机上：
1. **设置 → 关于手机**
2. 连续点击 **版本号** 7次
3. 返回，找到 **开发者选项**
4. 启用 **USB调试**

### 2. 连接到电脑

1. 用USB线连接手机到Windows电脑
2. 手机会弹出授权提示，点击 **允许**
3. 在Android Studio或命令行运行：

```bash
adb devices
```

应该看到你的设备：
```
List of devices attached
ABC123456789    device
```

### 3. 运行应用

- Android Studio: 点击 Run，选择你的真实设备
- 命令行: `gradlew.bat installDebug`

**真实设备上语音识别通常100%可用！** 🎉

## 🔧 Windows特定命令

### Gradle命令（PowerShell/CMD）

```powershell
# 清理构建
gradlew.bat clean

# 构建Debug版本
gradlew.bat assembleDebug

# 安装Debug版本
gradlew.bat installDebug

# 运行测试
gradlew.bat test

# 查看所有任务
gradlew.bat tasks
```

### ADB命令（通用）

```bash
# 查看连接的设备
adb devices

# 查看应用日志
adb logcat -s VoiceRecognitionService CameraViewModel

# 清空日志
adb logcat -c

# 安装APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 卸载应用
adb uninstall com.example.myapplication

# 启动应用
adb shell am start -n com.example.myapplication/.MainActivity

# 停止应用
adb shell am force-stop com.example.myapplication
```

## 📝 项目结构（Windows路径）

```
C:\Users\YourUsername\Projects\Lumina\
├── app\
│   ├── src\
│   │   ├── main\
│   │   │   ├── java\com\example\myapplication\
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── data\repository\
│   │   │   │   ├── domain\service\
│   │   │   │   ├── presentation\
│   │   │   │   └── util\
│   │   │   ├── AndroidManifest.xml
│   │   │   └── res\
│   │   └── androidTest\
│   └── build.gradle
├── gradle\
├── build.gradle
├── settings.gradle
├── README.md
└── 各种文档.md
```

## ⚠️ 常见问题

### 问题1: Gradle Sync失败

**解决**：
1. File → Invalidate Caches → Invalidate and Restart
2. 删除 `.gradle` 文件夹，重新Sync
3. 检查网络连接（Gradle需要下载依赖）

### 问题2: SDK未找到

**解决**：
1. File → Settings → Appearance & Behavior → System Settings → Android SDK
2. 确保安装了 API Level 36
3. 点击 Apply

### 问题3: 模拟器启动慢

**解决**：
1. 确保启用了 HAXM 或 Hyper-V（Windows硬件加速）
2. 在BIOS中启用 VT-x 或 AMD-V
3. 或使用真实设备（更快）

### 问题4: adb不是内部命令

**解决**：
添加到PATH环境变量：
```
C:\Users\YourUsername\AppData\Local\Android\Sdk\platform-tools
```

1. 右键 **此电脑** → **属性** → **高级系统设置**
2. **环境变量** → **Path** → **新建**
3. 添加上面的路径
4. 重启命令行

## 🎯 快速开始（Windows）

```powershell
# 1. 克隆项目
git clone https://github.com/yourskenny/Lumina.git
cd Lumina
git checkout Androidd

# 2. 打开Android Studio
# File -> Open -> 选择Lumina文件夹

# 3. 等待Gradle Sync完成

# 4. 连接真实Android设备（推荐）
#    或创建AVD模拟器

# 5. 点击Run按钮（绿色三角形）

# 6. 测试快捷按钮功能
#    - 点击"拍照"按钮
#    - 点击"查询电池"按钮
#    - 所有功能都应该正常工作！
```

## 📚 文档位置

所有文档都在项目根目录：

```
README.md                     # 项目说明
FEATURES.md                   # 功能列表
语音识别使用指南.md           # 语音功能说明
快捷命令测试指南.md           # 命令测试
诊断报告.md                   # 问题诊断
MICROPHONE_CHECKLIST.md       # 麦克风配置
```

## 💡 推荐工作流程（Windows）

1. **使用真实设备**（最佳体验）
   - 语音识别通常100%可用
   - 性能更好
   - 更真实的用户体验

2. **或使用模拟器 + 快捷按钮**
   - 快捷按钮可以完全测试所有功能
   - 模拟器中语音识别受限是正常的

3. **开发新功能**
   - 使用快捷按钮测试命令逻辑
   - 在真实设备上测试完整语音流程

## 🎉 迁移完成清单

在Windows上完成这些步骤：

- [ ] 安装Android Studio
- [ ] 安装Git for Windows
- [ ] 克隆项目
- [ ] 切换到Androidd分支
- [ ] 在Android Studio中打开项目
- [ ] 等待Gradle Sync完成
- [ ] 创建AVD或连接真实设备
- [ ] 运行应用
- [ ] 测试快捷按钮功能
- [ ] 查看日志: `adb logcat`

## 📞 需要帮助？

如果在Windows上遇到问题：
1. 检查Android Studio的 **Build Output** 窗口
2. 查看 **Logcat** 窗口的错误信息
3. 运行 `gradlew.bat --stacktrace` 查看详细错误

所有功能和文档都已经在代码中，可以无缝迁移到Windows！🚀
