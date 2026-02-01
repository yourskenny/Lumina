# 🪟 Windows 环境配置和验证指南

## 📋 第一步：验证项目已下载

### 打开 PowerShell 或 Git Bash

按 `Win + X`，选择 **Windows Terminal** 或 **PowerShell**

```powershell
# 进入项目目录
cd 你的项目路径\Lumina

# 确认在正确的分支
git branch
# 应该显示: * Androidd

# 查看项目文件
dir
# 或
ls
```

**应该看到**：
```
app\
gradle\
build.gradle
settings.gradle
README.md
各种.md文档
```

---

## 🔧 第二步：安装必要软件

### 1. Java Development Kit (JDK)

**检查是否已安装**：
```powershell
java -version
```

**如果未安装**：
- Android Studio 会自动安装 JDK
- 或下载：https://www.oracle.com/java/technologies/downloads/
- 版本：JDK 17 或更高

### 2. Android Studio

**下载和安装**：
1. 访问：https://developer.android.com/studio
2. 下载 Windows 版本
3. 运行安装程序
4. 选择 **Standard** 安装
5. 接受所有默认设置

**安装时间**：首次安装约 15-20 分钟

### 3. Git for Windows（如果还没有）

如果 `git` 命令不可用：
1. 访问：https://git-scm.com/download/win
2. 下载并安装
3. 选择默认设置

---

## 🎯 第三步：在 Android Studio 中打开项目

### 1. 启动 Android Studio

第一次启动会看到欢迎界面

### 2. 导入项目

1. 点击 **Open**
2. 浏览到项目目录（包含 `build.gradle` 的文件夹）
3. 选择整个项目文件夹，点击 **OK**

### 3. 等待 Gradle Sync

**这是最重要的一步！**

Android Studio 底部会显示：
```
Gradle Sync in progress...
```

**状态指示**：
- 🔄 正在下载依赖（第一次需要 10-20 分钟）
- ✅ "Gradle Sync finished" - 成功
- ❌ "Gradle Sync failed" - 需要排查

**耐心等待！** 第一次 Gradle Sync 会下载：
- Gradle 构建工具
- Android SDK
- Kotlin 编译器
- 各种依赖库

---

## ✅ 第四步：环境验证

### 验证 1：检查 Gradle Sync 状态

**如果成功**：
- ✅ 底部状态栏显示 "Gradle Sync finished"
- ✅ 左侧项目树能正常展开
- ✅ 没有红色错误标记

**如果失败**：
跳到 **故障排除** 部分

### 验证 2：检查 Android SDK

1. **File → Settings** (或 `Ctrl + Alt + S`)
2. **Appearance & Behavior → System Settings → Android SDK**

**确认已安装**：
- ✅ Android API 36 (或最新版本)
- ✅ Android SDK Build-Tools
- ✅ Android SDK Platform-Tools
- ✅ Android SDK Tools
- ✅ Google Play Services

**如果缺少**：
1. 勾选需要的项目
2. 点击 **Apply**
3. 等待下载完成

### 验证 3：检查项目结构

在 Android Studio 左侧，切换到 **Android** 视图：

```
myapplication
├── app
│   ├── manifests
│   │   └── AndroidManifest.xml ✅
│   ├── java
│   │   └── com.example.myapplication
│   │       ├── MainActivity ✅
│   │       ├── data
│   │       ├── domain
│   │       ├── presentation
│   │       └── util
│   └── res
├── Gradle Scripts
│   ├── build.gradle (Project) ✅
│   └── build.gradle (Module: app) ✅
```

**所有文件都应该没有红色错误标记**

### 验证 4：运行 Gradle 命令

在 Android Studio 底部点击 **Terminal**，运行：

```powershell
# Windows 使用 gradlew.bat
gradlew.bat --version
```

**应该显示**：
```
Gradle 8.x
```

**测试构建**：
```powershell
gradlew.bat build
```

**如果成功**：
```
BUILD SUCCESSFUL in 30s
```

---

## 📱 第五步：创建和配置设备

### 选项 A：创建 Android 虚拟设备（AVD）

#### 1. 打开 Device Manager

**Tools → Device Manager** (或点击工具栏的手机图标 📱)

#### 2. 创建新设备

1. 点击 **Create Device**
2. 选择设备：**Phone → Pixel 6** (推荐)
3. 点击 **Next**

#### 3. 选择系统镜像

**重要：选择带 Google Play 的版本！**

1. 选择：**API Level 36** 或最新版本
2. 确保选择 **x86_64** 架构（带 Google Play 标志）
3. 如果需要下载，点击 **Download** 旁边的链接
4. 下载完成后点击 **Next**

#### 4. 完成配置

1. 名称：保持默认或自定义
2. 点击 **Finish**

#### 5. 启动模拟器

1. 在 Device Manager 中找到你的设备
2. 点击 ▶️ 播放按钮
3. 等待模拟器启动（第一次需要 2-3 分钟）

**验证模拟器**：
- ✅ 看到 Android 主屏幕
- ✅ 可以点击和滑动

### 选项 B：连接真实 Android 设备（推荐！）

#### 1. 在手机上启用开发者选项

**步骤**：
1. 打开手机 **设置**
2. 进入 **关于手机**
3. 找到 **版本号** 或 **内部版本号**
4. **连续点击 7 次**
5. 看到 "您已处于开发者模式" 提示

#### 2. 启用 USB 调试

1. 返回 **设置** 主界面
2. 找到 **开发者选项** (可能在 **系统** 或 **更多设置** 中)
3. 启用 **USB 调试**
4. 启用 **USB 安装** (如果有)

#### 3. 连接设备

1. 用 USB 线连接手机到电脑
2. 手机会弹出 **允许 USB 调试** 提示
3. 勾选 **始终允许这台计算机**
4. 点击 **允许**

#### 4. 验证连接

在 Android Studio 的 **Terminal** 中运行：

```powershell
# 查看连接的设备
adb devices
```

**应该显示**：
```
List of devices attached
ABC123456789    device
```

**如果显示 `unauthorized`**：
- 在手机上再次确认 USB 调试授权

**如果显示 `no devices`**：
- 检查 USB 线（使用数据线，不是充电线）
- 在手机上重新启用 USB 调试
- 重启 adb：`adb kill-server && adb start-server`

---

## 🚀 第六步：构建和运行应用

### 1. 选择运行配置

在 Android Studio 顶部：
- 左边下拉：**app**
- 右边下拉：选择你的设备（模拟器或真实设备）

### 2. 构建项目

点击 **Build → Make Project** (或按 `Ctrl + F9`)

**如果成功**：
- ✅ 底部显示 "Build Successful"
- ✅ 没有红色错误

### 3. 运行应用

点击绿色三角形 ▶️ **Run** 按钮 (或按 `Shift + F10`)

**等待**：
1. 应用正在编译...
2. 应用正在安装到设备...
3. 应用正在启动...

**如果成功**：
- ✅ 设备上看到应用界面
- ✅ 底部 Logcat 显示日志
- ✅ 应用正常显示相机预览

---

## ✅ 第七步：功能验证

### 1. 验证应用界面

**应该看到**：
- 📹 相机预览画面
- 📊 顶部状态栏（显示录像状态）
- 🎤 底部调试面板（如果展开）
- 🔘 底部控制按钮

### 2. 测试基本功能

**测试录像**：
- 应用启动后会自动开始录像
- ✅ 顶部显示 "录像中"
- ✅ 时长在增加

**测试暂停/继续**：
1. 点击 **暂停** 按钮
2. ✅ 状态变为 "已暂停"
3. 点击 **继续** 按钮
4. ✅ 状态变为 "录像中"

### 3. 测试快捷命令（重要！）

滚动到调试面板底部，找到快捷按钮：

**测试拍照**：
1. 点击 **拍照** 按钮
2. ✅ 听到咔嚓声
3. ✅ 看到提示 "照片已保存"

**测试查询电池**：
1. 点击 **查询电池** 按钮
2. ✅ 听到语音播报电池信息
3. ✅ 显示 "✅ 匹配到命令: CHECK_BATTERY"

### 4. 查看日志（验证功能正常）

在 Android Studio 底部点击 **Logcat**

**过滤日志**：
在搜索框输入：`CameraViewModel`

**点击"拍照"按钮后应该看到**：
```
D/CameraViewModel: ✅ 命令匹配成功: CAPTURE_PHOTO
D/CameraViewModel: >>> 开始执行命令: CAPTURE_PHOTO
D/CameraViewModel: 照片已保存: /storage/.../photo.jpg
```

**如果看到这些日志，说明功能完全正常！** ✅

---

## 🎯 完整验证清单

完成所有这些步骤，确认环境配置正确：

### 软件安装
- [ ] Android Studio 已安装
- [ ] JDK 已安装（通过 Android Studio 或单独安装）
- [ ] Git for Windows 已安装

### 项目配置
- [ ] 项目已克隆到本地
- [ ] 在 Androidd 分支
- [ ] Android Studio 成功打开项目
- [ ] Gradle Sync 成功完成（没有红色错误）

### SDK 配置
- [ ] Android SDK Platform 36 已安装
- [ ] Android SDK Build-Tools 已安装
- [ ] Android SDK Platform-Tools 已安装

### 设备配置
- [ ] AVD 模拟器已创建 或 真实设备已连接
- [ ] `adb devices` 能看到设备
- [ ] 设备状态显示 `device`（不是 unauthorized）

### 构建和运行
- [ ] `gradlew.bat build` 成功
- [ ] 应用成功安装到设备
- [ ] 应用成功启动
- [ ] 能看到相机预览

### 功能测试
- [ ] 录像功能正常
- [ ] 暂停/继续按钮正常
- [ ] 点击"拍照"按钮能拍照
- [ ] 点击"查询电池"能语音播报
- [ ] Logcat 显示正确的日志

---

## ⚠️ 故障排除

### 问题 1：Gradle Sync 失败

**错误**: "Gradle Sync failed: ..."

**解决方案**：

1. **检查网络**（Gradle 需要下载依赖）
```powershell
# 测试网络
ping google.com
```

2. **使用国内镜像**（如果网络慢）

打开项目根目录的 `build.gradle`，在最顶部添加：
```gradle
buildscript {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/jcenter' }
        maven { url 'https://maven.aliyun.com/repository/public' }
        google()
        mavenCentral()
    }
}

allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/jcenter' }
        maven { url 'https://maven.aliyun.com/repository/public' }
        google()
        mavenCentral()
    }
}
```

3. **清理和重建**
```
File → Invalidate Caches → Invalidate and Restart
```

4. **手动指定 JDK**
```
File → Settings → Build, Execution, Deployment → Build Tools → Gradle
- Gradle JDK: 选择 Android Studio 自带的 JDK
```

### 问题 2：SDK 未找到

**错误**: "SDK location not found"

**解决方案**：

1. **设置 SDK 路径**
```
File → Settings → Appearance & Behavior → System Settings → Android SDK
- Android SDK Location: 记下这个路径
```

2. **创建 local.properties**

在项目根目录创建 `local.properties` 文件：
```properties
sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
```

注意：反斜杠要双写 `\\`

### 问题 3：adb 不是内部命令

**错误**: `'adb' 不是内部或外部命令`

**解决方案**：

1. **找到 adb 路径**
```
C:\Users\YourUsername\AppData\Local\Android\Sdk\platform-tools
```

2. **添加到 PATH**
   - 按 `Win` 键，搜索 "环境变量"
   - 点击 **编辑系统环境变量**
   - 点击 **环境变量**
   - 在 **系统变量** 中找到 **Path**
   - 点击 **编辑** → **新建**
   - 粘贴上面的路径
   - 点击 **确定**
   - **重启 PowerShell/Terminal**

3. **验证**
```powershell
adb version
# 应该显示版本号
```

### 问题 4：模拟器启动失败

**错误**: "Emulator: ERROR: ..."

**解决方案**：

1. **启用硬件加速**
   - 在 BIOS 中启用 VT-x 或 AMD-V
   - 安装 Intel HAXM（Android Studio 会提示）

2. **检查 Hyper-V**（Windows 10/11）
```powershell
# 以管理员身份运行 PowerShell
Get-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V-All
```

3. **如果 Hyper-V 冲突**
   - 使用 Windows Hypervisor Platform
   - 或禁用 Hyper-V：
   ```powershell
   # 以管理员身份
   bcdedit /set hypervisorlaunchtype off
   ```

4. **使用真实设备**（更简单）

### 问题 5：设备未识别

**错误**: `adb devices` 显示空

**解决方案**：

1. **重启 adb 服务**
```powershell
adb kill-server
adb start-server
adb devices
```

2. **检查 USB 驱动**
   - 手机厂商官网下载 USB 驱动
   - 设备管理器中查看是否有黄色感叹号

3. **使用不同的 USB 端口**
   - 尝试 USB 2.0 端口
   - 更换 USB 线（数据线，不是充电线）

4. **手机设置**
   - USB 连接模式选择 **文件传输** 或 **MTP**
   - 重新授权 USB 调试

---

## 📊 验证命令总结

在 PowerShell 中运行这些命令来验证环境：

```powershell
# 1. 验证 Git
git --version

# 2. 验证 Java
java -version

# 3. 验证 Android SDK
cd %LOCALAPPDATA%\Android\Sdk\platform-tools
.\adb.exe version

# 4. 查看连接的设备
adb devices

# 5. 验证 Gradle
cd 你的项目路径\Lumina
gradlew.bat --version

# 6. 测试构建
gradlew.bat assembleDebug

# 7. 查看应用日志（应用运行时）
adb logcat -s CameraViewModel:D VoiceRecognitionService:D
```

---

## 🎉 验证成功标志

如果你能做到以下所有点，说明环境完全配置好了：

✅ Android Studio 成功打开项目
✅ Gradle Sync 完成，没有错误
✅ `adb devices` 能看到你的设备
✅ 应用成功安装到设备
✅ 应用启动并显示相机预览
✅ 点击"拍照"按钮能拍照
✅ Logcat 显示 "照片已保存" 日志

**恭喜！你的 Windows 开发环境已经完全配置好了！** 🎊

---

## 📞 需要帮助？

如果遇到问题：

1. **查看 Android Studio 的 Build Output**
   - View → Tool Windows → Build

2. **查看详细错误**
```powershell
gradlew.bat build --stacktrace
```

3. **检查系统要求**
   - Windows 10/11 64位
   - 至少 8GB RAM（推荐 16GB）
   - 至少 20GB 可用磁盘空间
   - 稳定的网络连接

提供这些信息可以更好地诊断问题：
- 错误的完整文本
- Android Studio 版本
- Windows 版本
- 是否使用模拟器或真实设备
