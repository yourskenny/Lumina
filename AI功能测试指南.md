# AI功能快速测试指南

## ✅ 编译和安装成功

- **编译状态**: BUILD SUCCESSFUL
- **APK大小**: 112MB
- **安装状态**: 成功安装到模拟器
- **应用启动**: 正常运行

---

## 🎮 如何测试AI功能

### 方法1: 使用模拟器相机

由于模拟器限制，建议以下测试方法：

#### 步骤1: 准备测试图片
在电脑上准备包含以下内容的图片：
- 汽车
- 人物
- 自行车
- 路标
- 人行横道

#### 步骤2: 推送图片到模拟器
```bash
# 推送测试图片到模拟器
adb push test_image.jpg /sdcard/Pictures/test.jpg
```

#### 步骤3: 使用测试代码触发分析
在应用中添加一个测试按钮，直接加载并分析/sdcard/Pictures/test.jpg

---

### 方法2: 真机测试（推荐）

#### 在Windows真机上部署

```powershell
# 1. 拉取最新代码
git pull origin ui-improvement

# 2. 编译APK
.\gradlew clean assembleDebug

# 3. 安装到真机
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 4. 启动应用
adb shell am start -n com.example.myapplication/.MainActivity

# 5. 测试AI功能
# - 对准汽车拍照
# - 听语音播报结果
# - 观察触觉反馈
```

#### 真机测试场景

**场景1: 危险近距离**
- 对准停在路边的汽车（1米内）
- 点击"拍照"按钮
- 预期: 手机震动 + 语音："Warning. car ahead. 0.5 meters."

**场景2: 警告中距离**
- 对准2-3米外的人
- 点击"拍照"
- 预期: 轻微震动 + 语音："Caution. person center. 2.5 meters."

**场景3: 安全路径**
- 对准人行横道
- 点击"拍照"
- 预期: 语音："crosswalk detected center"

---

## 📊 监控AI日志

### Mac本地（模拟器）

```bash
# 监控AI相关日志
adb logcat -s "CameraViewModel:*" "ObjectDetector:*" | grep -E "AI|检测|分析|DANGER|CAUTION|SAFE"
```

### Windows真机

```powershell
# 实时监控
adb logcat -s "CameraViewModel:*" | findstr "AI 检测 分析"

# 保存到文件
adb logcat -s "CameraViewModel:*" > ai_test_log.txt
```

---

## 🔍 预期日志输出

### 正常AI分析流程

```
D CameraViewModel: 照片已保存: /storage/emulated/0/Pictures/Lumina/photo_20260202_004530.jpg
D CameraViewModel: 🤖 开始AI分析: /storage/.../photo_20260202_004530.jpg
D CameraViewModel: ✅ AI检测完成: 1 个危险, 0 个路径
D CameraViewModel: 📊 分析结果: CAUTION - car detected at 2.5m
I TextToSpeechService: 播报: Caution. car center. 2.5 meters.
```

### 检测到多个物体

```
D CameraViewModel: 🤖 开始AI分析
D CameraViewModel: ✅ AI检测完成: 3 个危险, 1 个路径
D CameraViewModel: 📊 分析结果: DANGER - person detected at 1.2m
I TextToSpeechService: 播报: Warning. person left. 1.2 meters.
```

### 安全场景

```
D CameraViewModel: 🤖 开始AI分析
D CameraViewModel: ✅ AI检测完成: 0 个危险, 1 个路径
D CameraViewModel: 📊 分析结果: SAFE - Follow crosswalk center
I TextToSpeechService: 播报: crosswalk detected center
```

---

## 🐛 故障排查

### 问题1: 拍照后没有AI分析日志

**检查**:
```bash
adb logcat -s "CameraViewModel:E" "*:E"
```

**可能原因**:
- 图片保存失败
- ObjectDetector初始化失败
- ONNX模型文件缺失

**解决**:
```bash
# 检查模型文件
adb shell ls -lh /data/app/*/com.example.myapplication*/base.apk
adb shell unzip -l /data/app/.../base.apk | grep onnx
```

---

### 问题2: AI分析很慢

**正常推理时间**:
- 模拟器: 1-3秒
- 中端真机: 300-500ms
- 高端真机: 100-200ms

**如果超过5秒**:
- 检查设备性能
- 查看CPU占用
- 确认NNAPI是否启用

---

### 问题3: 检测不到物体

**可能原因**:
- 物体太小
- 光照不足
- 角度不佳
- 物体类别不在COCO 80类中

**改善方法**:
- 近距离拍摄
- 确保光照充足
- 正面拍摄物体
- 使用常见物体测试（car, person, chair等）

---

## 📸 模拟器测试替代方案

由于模拟器相机限制，可以添加一个测试功能：

### 添加测试按钮

在 `DebugDrawer.kt` 的开发者工具区添加：

```kotlin
Button(
    onClick = {
        // 测试AI功能 - 加载预设图片
        viewModel.testAIWithSampleImage()
    }
) {
    Text("测试AI", fontSize = 12.sp)
}
```

### 在 CameraViewModel 添加测试方法

```kotlin
fun testAIWithSampleImage() {
    viewModelScope.launch {
        try {
            // 从assets加载测试图片
            val testBitmap = BitmapFactory.decodeStream(
                context.assets.open("test_car.jpg")
            )

            // 直接执行AI分析
            val result = objectDetector.detect(testBitmap)

            Log.d(TAG, "测试AI: 检测到 ${result.hazards.size} 个危险")

            // 更新UI...
        } catch (e: Exception) {
            Log.e(TAG, "AI测试失败", e)
        }
    }
}
```

然后在 `app/src/main/assets/` 添加一张包含汽车的测试图片 `test_car.jpg`

---

## 🎯 验证清单

测试AI功能是否正常工作：

### 基础验证
- [ ] APK成功安装
- [ ] 应用正常启动
- [ ] 可以拍照
- [ ] 拍照后有"🤖 开始AI分析"日志
- [ ] 有"✅ AI检测完成"日志
- [ ] 有"📊 分析结果"日志

### 功能验证
- [ ] 检测到汽车并播报
- [ ] 检测到人并播报
- [ ] 检测到路径并播报
- [ ] 危险距离触发Error振动
- [ ] 警告距离触发Warning振动
- [ ] 语音播报包含物体名称、方向和距离

### 性能验证
- [ ] 推理时间 < 3秒（模拟器）或 < 1秒（真机）
- [ ] 没有崩溃
- [ ] 内存占用正常
- [ ] 多次拍照仍然正常

---

## 📝 测试报告模板

```markdown
## AI功能测试报告

### 测试环境
- 设备: [模拟器/真机型号]
- Android版本: [版本号]
- APK版本: [commit hash]

### 测试场景1: 汽车检测
- 距离: [实际距离]
- 检测结果: [检测到的类别]
- 估算距离: [AI估算距离]
- 语音播报: [播报内容]
- 触觉反馈: [震动类型]
- 推理时间: [毫秒]

### 测试场景2: 人物检测
- ...

### 问题记录
- [问题描述]
- [日志截图]

### 总体评价
- 准确率: [好/中/差]
- 性能: [快/中/慢]
- 用户体验: [好/中/差]
```

---

## 🚀 下一步

### 如果测试成功
1. ✅ 标记AI功能验证通过
2. 📝 记录测试结果
3. 🎯 规划UI改进（显示检测框）
4. 🔧 性能优化

### 如果遇到问题
1. 📋 记录详细日志
2. 🔍 分析问题原因
3. 🐛 提交issue
4. 🔧 修复和重新测试

---

**当前状态**: ✅ 已安装到模拟器，等待测试
**推荐**: 在真机上测试以获得最佳体验
**测试日期**: 2026-02-02

---

## 💡 快速命令参考

```bash
# Mac/模拟器
adb logcat -s "CameraViewModel:*" | grep AI

# Windows/真机
adb logcat -s "CameraViewModel:*" | findstr AI

# 清理并重装
adb uninstall com.example.myapplication
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 重启应用
adb shell am force-stop com.example.myapplication
adb shell am start -n com.example.myapplication/.MainActivity
```
