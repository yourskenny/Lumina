# AI 物体检测测试使用指南

本文档介绍如何独立测试项目中集成的 AI 物体检测功能。

## 方案1：使用测试应用（推荐）

### 📱 使用步骤

1. **编译并安装应用**
   ```bash
   cd /Users/mac/Desktop/project/myapplication
   ./gradlew installDebug
   ```

2. **启动AI测试应用**
   - 在设备上找到"AI测试"应用图标（独立于主应用）
   - 点击启动

3. **选择测试图片**
   - 点击「选择测试图片」按钮
   - 从相册中选择一张图片
   - 可以选择包含人、车、自行车等物体的图片

4. **执行检测**
   - 点击「🧠 开始AI检测」按钮
   - 等待检测完成（通常100-500ms）

5. **查看结果**
   - 推理时间
   - 危险物体列表（名称、距离、方向、坐标）
   - 路径信息列表
   - 完整的检测详情

### 📸 测试图片建议

为了获得最佳测试效果，建议使用包含以下内容的图片：

**危险物体类别：**
- 🚗 车辆：car, motorcycle, bicycle
- 👤 人物：person
- 🐕 动物：dog, cat
- 🚦 交通设施：traffic light, stop sign
- 🌳 障碍物：pole, tree, fire hydrant

**路径类别：**
- 🚶 人行横道：crosswalk
- 🪜 楼梯：stairs
- 🟨 盲道：tactile paving

### 📊 结果解读

检测结果包含以下信息：

```
✅ 检测完成！
⏱️ 推理时间: 234ms

🚨 危险物体 (2):
  1. car
     距离: 3.5m
     方向: center
     坐标: [120.5, 200.3, 450.8, 600.2]

  2. person
     距离: 1.5m
     方向: left
     坐标: [50.2, 150.4, 180.6, 500.9]

🛤️ 路径信息 (1):
  1. crosswalk
     距离: 4.0m
     方向: center
```

**字段说明：**
- **距离**：基于启发式算法估算（0.5m ~ 8m）
- **方向**：left（左）、center（中）、right（右）
- **坐标**：[x1, y1, x2, y2] 边界框坐标

---

## 方案2：使用代码调用测试

### 📝 在代码中调用

创建了 `AITestUtil` 工具类，可以在任何地方调用：

#### 示例1：测试单张图片

```kotlin
import com.example.myapplication.util.AITestUtil

// 在Activity或Fragment中
val imagePath = "/storage/emulated/0/DCIM/Camera/test.jpg"
val result = AITestUtil.testImage(this, imagePath)
Log.d("AITest", result)
```

#### 示例2：测试Bitmap对象

```kotlin
val bitmap = BitmapFactory.decodeResource(resources, R.drawable.test_image)
val result = AITestUtil.testBitmap(this, bitmap, "测试图片")
Log.d("AITest", result)
```

#### 示例3：快速测试（打印到Logcat）

```kotlin
AITestUtil.quickTest(this, "/path/to/image.jpg")
```

#### 示例4：批量测试

```kotlin
val imagePaths = listOf(
    "/sdcard/DCIM/test1.jpg",
    "/sdcard/DCIM/test2.jpg",
    "/sdcard/DCIM/test3.jpg"
)
val result = AITestUtil.batchTest(this, imagePaths)
Log.d("AITest", result)
```

### 📋 输出格式

```
==================================================
🧪 AI检测测试报告
==================================================
📷 测试图片: test.jpg
📐 图片尺寸: 1920x1080

⏱️  推理时间: 234ms

🚨 危险物体检测结果 (2):
--------------------------------------------------
   [1] car
       • 距离: 3.50米
       • 方向: center
       • 坐标: [120.5, 200.3, 450.8, 600.2]
   [2] person
       • 距离: 1.50米
       • 方向: left
       • 坐标: [50.2, 150.4, 180.6, 500.9]

🛤️  路径信息检测结果 (0):
--------------------------------------------------
   无路径信息

📊 分析总结:
--------------------------------------------------
   • 检测到物体总数: 2
   • 危险等级: 🟡 警告 (1.5m)
   • 最近物体距离: 1.50米

💡 建议:
--------------------------------------------------
   • 注意：person 在left方向，距离约1.5米，请小心前进
==================================================
```

---

## 方案3：使用ADB命令测试

### 📲 通过ADB查看日志

```bash
# 过滤AI相关日志
adb logcat -s "AITestUtil:*" "ObjectDetector:*"

# 清空日志后测试
adb logcat -c
adb logcat -s "AITestUtil:*"
```

### 🔧 调试命令

```bash
# 查看AI测试应用是否已安装
adb shell pm list packages | grep myapplication

# 启动AI测试应用
adb shell am start -n com.example.myapplication/.AITestActivity

# 推送测试图片到设备
adb push /path/to/test.jpg /sdcard/Pictures/
```

---

## 常见问题

### Q1: 检测结果为空怎么办？

**可能原因：**
1. 图片中没有支持的物体类别（仅支持COCO 80类）
2. 物体太小或被遮挡
3. 置信度低于阈值（0.4）

**解决方法：**
- 使用包含明显物体的图片
- 尝试调整 `ObjectDetector.CONFIDENCE_THRESHOLD`

### Q2: 推理时间过长（>1秒）

**可能原因：**
1. 设备性能较低
2. NNAPI未启用

**解决方法：**
- 检查 `setupSession()` 中的 `sessionOptions.addNnapi()`
- 在高性能设备上测试
- 考虑使用更小的模型

### Q3: 距离估算不准确

**说明：**
- 当前使用启发式算法，非精确测距
- 基于物体在图像中的垂直位置估算
- 适合相对距离判断，不适合精确测量

**改进方法：**
- 需要相机标定和深度估计算法
- 可考虑集成单目深度估计模型

### Q4: 方向判断简单

**说明：**
- 仅根据物体中心X坐标划分三个区域
- left: X < 33%
- center: 33% ≤ X ≤ 66%
- right: X > 66%

---

## 技术指标

| 指标 | 值 |
|------|-----|
| 模型名称 | YOLOE-v8s-seg |
| 模型大小 | 46MB (INT8量化) |
| 输入分辨率 | 320x320 |
| 输出维度 | [1, 116, 2100] |
| 支持类别 | 80类 (COCO数据集) |
| 置信度阈值 | 0.4 |
| IOU阈值 | 0.5 |
| 推理时间 | 100-500ms (取决于设备) |
| NNAPI加速 | ✅ 支持 |

---

## 支持的物体类别

### 🚨 危险物体类别
car, motorcycle, bicycle, pole, tree, fire hydrant, traffic cone

### 🤝 交互物体类别
person, dog, cat, chair, traffic light, stop sign

### 🛤️ 路径类别
crosswalk, stairs, tactile paving

### 📦 完整COCO 80类列表
person, bicycle, car, motorcycle, airplane, bus, train, truck, boat, traffic light, fire hydrant, stop sign, parking meter, bench, bird, cat, dog, horse, sheep, cow, elephant, bear, zebra, giraffe, backpack, umbrella, handbag, tie, suitcase, frisbee, skis, snowboard, sports ball, kite, baseball bat, baseball glove, skateboard, surfboard, tennis racket, bottle, wine glass, cup, fork, knife, spoon, bowl, banana, apple, sandwich, orange, broccoli, carrot, hot dog, pizza, donut, cake, chair, couch, potted plant, bed, dining table, toilet, tv, laptop, mouse, remote, keyboard, cell phone, microwave, oven, toaster, sink, refrigerator, book, clock, vase, scissors, teddy bear, hair drier, toothbrush

---

## 下一步优化建议

1. **精确测距**
   - 集成单目深度估计模型
   - 相机标定获取内参

2. **更丰富的空间信息**
   - 3D边界框
   - 物体相对速度估计

3. **性能优化**
   - 模型量化到INT4
   - GPU/NPU加速
   - 多线程推理

4. **功能扩展**
   - 语义分割可视化
   - 物体追踪
   - 姿态估计

---

## 相关文件

- **核心检测器**: `app/src/main/java/com/example/myapplication/domain/detector/ObjectDetector.kt`
- **测试界面**: `app/src/main/java/com/example/myapplication/presentation/screen/AITestScreen.kt`
- **测试Activity**: `app/src/main/java/com/example/myapplication/AITestActivity.kt`
- **测试工具类**: `app/src/main/java/com/example/myapplication/util/AITestUtil.kt`
- **AI模型**: `app/src/main/assets/yoloe-v8s-seg.onnx`

---

## 示例测试流程

```bash
# 1. 编译安装
cd /Users/mac/Desktop/project/myapplication
./gradlew clean installDebug

# 2. 推送测试图片
adb push ~/Pictures/test_street.jpg /sdcard/Pictures/

# 3. 启动测试应用
adb shell am start -n com.example.myapplication/.AITestActivity

# 4. 监控日志
adb logcat -s "AITestUtil:*" "ObjectDetector:*"

# 5. 在应用中选择图片并测试
```

---

**测试愉快！🎉**

如有问题，请参考：
- [AI功能集成说明.md](AI功能集成说明.md)
- [AI功能测试指南.md](AI功能测试指南.md)
