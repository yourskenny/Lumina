# 更新日志

所有重要的项目变更都会记录在此文件中。

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/) 规范。

---

## [1.3.0] - 2026-01-28

### 🌍 GPS定位功能 - 位置记录与追踪

本次更新实现了完整的GPS定位功能，支持录像时自动记录位置信息，并提供位置查询语音命令。

### ✨ 新增功能

#### 1. GPS定位服务（LocationService）
- **功能**：管理GPS位置获取和更新
- **特性**：
  - 双重定位源：GPS Provider + Network Provider
  - 自动更新间隔：5秒
  - 最小位移：10米
  - 持续后台定位支持
- **数据结构**：LocationData（经纬度、海拔、精度、速度、方位、时间戳）
- **文件**：`domain/service/LocationService.kt` (新增，265行)

#### 2. 位置数据仓库（LocationRepository）
- **功能**：位置数据管理和访问
- **特性**：
  - 位置历史记录（最多100个位置）
  - 生成带GPS信息的文件名
  - 位置元数据字符串生成
  - 位置统计信息查询
- **方法**：
  - `startTracking()` - 开始位置跟踪
  - `stopTracking()` - 停止位置跟踪
  - `getCurrentLocation()` - 获取当前位置
  - `recordLocation()` - 记录位置到历史
  - `getLocationDescription()` - 获取语音描述
- **文件**：`data/repository/LocationRepository.kt` (新增，200行)

#### 3. 视频GPS元数据记录
- **功能**：自动在视频文件中记录GPS坐标
- **实现**：
  - 在`MediaStore.Video.Media`中写入`LATITUDE`和`LONGITUDE`字段
  - 视频保存时自动添加当前GPS位置
  - 照片文件名包含GPS坐标信息
- **格式**：
  - 视频：`AccessibleCamera_20260128_153022.mp4` + GPS元数据
  - 照片：`AccessibleCamera_Photo_20260128_153022_39_9150N_116_4040E.jpg`
- **文件**：`MediaRepository.kt` (修改，+35行)

#### 4. 位置语音查询
- **功能**：通过语音命令查询当前GPS位置
- **语音命令**：
  - "查询位置" / "我在哪" / "查看定位"
  - "查询GPS" / "查看坐标" / "当前地点"
- **语音反馈**：
  - "当前位置: 纬度39.9150, 经度116.4040, 精度15米"
  - "位置信息不可用"（GPS未启用或无信号）
  - "未授予定位权限"（缺少权限）
- **文件**：`CameraViewModel.kt` (+25行)

#### 5. 位置历史记录
- **功能**：自动记录录像期间的位置变化
- **容量**：最多保存最近100个位置
- **用途**：
  - 轨迹回放
  - 位置数据分析
  - 紧急情况位置追溯

---

### 🔄 改进

#### 语音命令系统扩展
- **新增语音命令**：`CHECK_LOCATION` - 查询GPS位置
- **总命令数**：14个（从13个增加到14个）
- **识别关键词**：
  - 查询：查询、查看、检查
  - 位置：位置、定位、GPS、坐标、在哪、地点

#### MediaRepository增强
- 添加`LocationRepository`依赖注入
- `createVideoOutputOptions()`自动写入GPS元数据
- `createPhotoOutputFile()`文件名包含GPS坐标
- 支持无位置信息时的降级处理

#### CameraViewModel集成
- 新增依赖：`LocationRepository`
- 初始化时自动启动GPS跟踪（如果有权限）
- 新增方法：`checkLocation()` - 查询位置
- 在`onCleared()`中释放位置资源
- 视频保存时自动记录位置到历史

---

### 📝 配置更新

#### AndroidManifest.xml
- **新增权限**：
  ```xml
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
  <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
  ```

---

### 📊 代码统计

**新增文件**：2个 (465行)
- `domain/service/LocationService.kt` - 265行
- `data/repository/LocationRepository.kt` - 200行

**修改文件**：4个 (+85行)
- `MediaRepository.kt` - +35行
- `CameraViewModel.kt` - +30行
- `VoiceRecognitionService.kt` - +10行
- `MainActivity.kt` - +10行

**总新增代码**：约550行
**总项目代码**：约4,294行

---

### 🎯 技术细节

#### GPS定位精度
- **坐标格式**：小数点后4位（约11米精度）
- **更新策略**：
  - 时间间隔：最少5秒
  - 距离间隔：最少10米
- **定位源优先级**：
  1. GPS Provider（高精度）
  2. Network Provider（快速定位）

#### 数据持久化
- **视频元数据**：MediaStore LATITUDE/LONGITUDE字段
- **照片文件名**：GPS坐标嵌入文件名
- **位置历史**：内存缓存（应用运行期间）

#### 权限处理
- **运行时权限请求**：
  - `ACCESS_FINE_LOCATION`（必需）
  - `ACCESS_COARSE_LOCATION`（必需）
  - `ACCESS_BACKGROUND_LOCATION`（Android 10+，可选）
- **降级策略**：
  - 无权限：不启动GPS跟踪，功能优雅降级
  - GPS未启用：语音提示用户开启定位服务

---

### 🔧 使用场景

#### 场景1：录像自动记录位置
```
1. 打开应用（自动开始录像 + GPS跟踪）
2. 正常录像（位置每5秒自动更新）
3. 视频保存（GPS坐标写入元数据）
4. 在图库中查看（显示拍摄位置）
```

#### 场景2：语音查询当前位置
```
1. 说"查询位置"
2. 语音播报："当前位置: 纬度39.9150, 经度116.4040, 精度15米"
3. 位置记录到历史
```

#### 场景3：紧急情况位置追溯
```
1. 查看位置历史记录
2. 获取过去录像的GPS轨迹
3. 定位紧急事件发生地点
```

---

### ⚠️ 注意事项

#### 1. GPS权限
- **首次使用需授予定位权限**
- **Android 10+后台定位**需要单独授权`ACCESS_BACKGROUND_LOCATION`
- **建议**：在权限请求说明中解释GPS用途

#### 2. 电池消耗
- **GPS持续定位会增加耗电**
- **优化策略**：
  - 仅在录像时启动GPS
  - 使用合理的更新间隔（5秒）
  - 优先使用Network Provider节省电量

#### 3. 位置精度
- **室内环境GPS信号弱**
- **建议**：在室外开阔地区使用以获得最佳精度
- **降级方案**：无GPS信号时使用Network Provider

#### 4. 隐私保护
- **GPS数据仅存储在本地**
- **不上传到服务器**
- **用户可以选择不授予定位权限**

---

### 📈 性能影响

#### 内存
- **LocationService**：约2MB
- **位置历史（100个）**：约50KB
- **总增加**：约2.5MB

#### 电池
- **GPS定位**：中等影响（5秒更新间隔）
- **建议**：录像时关闭屏幕节省电量

#### 存储
- **GPS元数据**：每个视频增加约100字节
- **可忽略不计**

---

### 🐛 已知限制

1. **室内定位精度低**
   - GPS信号弱导致精度下降
   - 建议在室外使用

2. **冷启动定位慢**
   - 首次启动GPS可能需要30秒-2分钟
   - 后续定位会快速更新

3. **后台定位限制**
   - Android 10+后台定位需要额外权限
   - 部分厂商ROM可能限制后台GPS

---

### 🔮 未来计划（v1.4）

- [ ] 离线地图集成（显示位置）
- [ ] 地理围栏提醒（进入/离开特定区域）
- [ ] 轨迹回放可视化
- [ ] 位置数据导出（KML/GPX格式）
- [ ] 逆地理编码（坐标转地址）

---

## [1.2.0] - 2026-01-28

### ⭐⭐ Phase 2 功能扩展 - 显著提升体验

本次更新实现了 Phase 2 的所有功能扩展，大幅提升应用的可用性和安全性。

### ✨ 新增功能

#### 1. 摄像头切换功能
- **功能**：支持前置/后置摄像头切换
- **限制**：录像中无法切换（需暂停后切换）
- **语音命令**：
  - "切换摄像头" / "切换相机" / "切换镜头"
- **语音反馈**：
  - "已切换到后置摄像头"
  - "已切换到前置摄像头"
- **文件**：`CameraRepository.kt` (+80行)

#### 2. 闪光灯控制功能
- **功能**：开启/关闭闪光灯（手电筒）
- **状态**：自动记忆闪光灯状态
- **语音命令**：
  - "打开闪光灯" / "关闭闪光灯" / "切换闪光灯"
  - "打开手电筒" / "关闭补光"
- **语音反馈**：
  - "闪光灯已打开"
  - "闪光灯已关闭"
- **文件**：`CameraRepository.kt`

#### 3. 扩展查询功能

##### 3.1 电池状态查询
- **功能**：查询电池电量和充电状态
- **语音命令**：
  - "查询电池" / "查看电量" / "检查充电"
- **语音反馈示例**：
  - "电池电量75%，充电中"
  - "电池电量30%，放电中"
- **文件**：`util/BatteryUtils.kt` (新增，103行)

##### 3.2 录像时长查询
- **功能**：查询当前正在录制的视频时长
- **语音命令**：
  - "查询录像时长" / "录了多久" / "查看时长"
- **语音反馈示例**：
  - "已录像3分钟25秒"
  - "当前未在录像"
- **文件**：`CameraRepository.kt`

#### 4. 设置数据管理
- **功能**：应用设置的持久化存储
- **支持的设置**：
  - 录像分段时长（1/3/5/10分钟）
  - 视频质量（UHD/FHD/HD/SD）
  - TTS语音速度（0.5-2.0倍速）
  - 自动清理策略配置
  - 紧急联系人信息
- **存储方式**：SharedPreferences
- **文件**：`data/repository/SettingsRepository.kt` (新增，157行)

#### 5. 紧急求助功能 ⭐ 重要安全功能

##### 5.1 紧急联系人管理
- **功能**：设置紧急联系人姓名和电话
- **存储**：持久化保存在设置中
- **验证**：自动检查是否已设置联系人

##### 5.2 紧急呼叫
- **功能**：一键拨打紧急联系人电话并发送短信
- **语音命令**：
  - "紧急呼叫" / "紧急求助" / "帮助电话"
- **执行动作**：
  1. 播报："正在拨打紧急联系人XXX的电话"
  2. 自动拨打电话（需要 CALL_PHONE 权限）
  3. 同时发送紧急短信（需要 SEND_SMS 权限）
  4. 短信内容："【紧急求助】我需要帮助，请尽快联系我。- 无障碍相机应用"
- **安全提示**：
  - 未设置联系人时提示："未设置紧急联系人"
  - 拨打失败时提示："拨打电话失败"
- **文件**：`domain/service/EmergencyService.kt` (新增，134行)

---

### 🔄 改进

#### 语音命令系统增强
- **新增5个语音命令**：
  - `SWITCH_CAMERA` - 切换摄像头
  - `TOGGLE_FLASHLIGHT` - 切换闪光灯
  - `CHECK_BATTERY` - 查询电池
  - `CHECK_RECORDING_TIME` - 查询录像时长
  - `EMERGENCY_CALL` - 紧急呼叫
- **总命令数**：13个（从8个增加到13个）

#### CameraRepository 扩展
- 新增字段：`camera`、`lifecycleOwner`、`previewView`、`currentCameraSelector`、`isBackCamera`、`flashEnabled`
- 新增方法：`switchCamera()`、`toggleFlashlight()`、`setFlashlight()`、`isFlashlightOn()`、`getCurrentCameraType()`、`getCurrentRecordingDuration()`
- 重构方法：`rebindCamera()` - 支持动态重新绑定相机

#### CameraViewModel 扩展
- 新增依赖：`Context`、`SettingsRepository`、`EmergencyService`
- 新增方法：`switchCamera()`、`toggleFlashlight()`、`checkBatteryInfo()`、`checkRecordingTime()`、`emergencyCall()`
- 更新方法：`handleVoiceCommand()` - 处理5个新命令

---

### 📝 配置更新

#### AndroidManifest.xml
- **新增权限**：
  ```xml
  <uses-permission android:name="android.permission.CALL_PHONE" />
  <uses-permission android:name="android.permission.SEND_SMS" />
  ```

---

### 📊 代码统计

**新增文件**：3个 (394行)
- `util/BatteryUtils.kt` - 103行
- `data/repository/SettingsRepository.kt` - 157行
- `domain/service/EmergencyService.kt` - 134行

**修改文件**：5个 (+280行)
- `CameraRepository.kt` - +120行
- `VoiceRecognitionService.kt` - +50行
- `CameraViewModel.kt` - +85行
- `MainActivity.kt` - +15行
- `AndroidManifest.xml` - +10行

**总新增代码**：约674行
**总项目代码**：约3,744行

---

### 🎯 功能对照表

| 功能 | 命令示例 | v1.0 | v1.1 | v1.2 |
|------|----------|------|------|------|
| 拍照 | "拍照" | ✅ | ✅ | ✅ |
| 暂停/继续录像 | "暂停录像" | ✅ | ✅ | ✅ |
| 播放视频 | "播放视频" | ❌ | ✅ | ✅ |
| 分享视频 | "分享视频" | ❌ | ✅ | ✅ |
| 查询存储 | "查询存储空间" | ❌ | ✅ | ✅ |
| 切换摄像头 | "切换摄像头" | ❌ | ❌ | ⭐ 新增 |
| 闪光灯控制 | "打开闪光灯" | ❌ | ❌ | ⭐ 新增 |
| 查询电池 | "查询电池" | ❌ | ❌ | ⭐ 新增 |
| 查询时长 | "录了多久" | ❌ | ❌ | ⭐ 新增 |
| 紧急求助 | "紧急呼叫" | ❌ | ❌ | ⭐ 新增 |

---

### ⚠️ 注意事项

#### 1. 紧急呼叫权限
- **首次使用前需设置紧急联系人**（通过应用设置或手动配置）
- **需要授予权限**：
  - CALL_PHONE（拨打电话）
  - SEND_SMS（发送短信）
- **建议**：在安装后首次启动时引导用户设置紧急联系人

#### 2. 摄像头切换限制
- **录像中无法切换摄像头**
- **解决方法**：先说"暂停录像"，切换后再说"继续录像"

#### 3. 闪光灯兼容性
- **部分前置摄像头不支持闪光灯**
- **表现**：命令执行但无效果

---

### 🔮 后续计划（v1.3）

#### Phase 3：优化增强
- [ ] 视频压缩（节省空间）
- [ ] 离线语音识别（提高准确率）
- [ ] GPS位置标记
- [ ] 电池优化（省电模式）
- [ ] 单元测试覆盖

---

## [1.1.0] - 2026-01-28

### ⭐ 重大更新

本次更新从业务角度完善核心功能，解决多个关键用户体验痛点。

### ✨ 新增功能

#### 1. 前台服务支持
- **问题**：应用切换到后台或锁屏后，录像被系统终止
- **解决**：实现前台服务，确保录像持续运行
- **特性**：
  - 通知栏显示运行状态
  - 支持后台录像
  - 支持锁屏录像
  - 服务崩溃后自动重启
  - 低优先级通知（不打扰用户）
- **文件**：`domain/service/CameraForegroundService.kt`

#### 2. 视频回放功能
- **问题**：用户录制大量视频但无法查看
- **解决**：支持播放最新录制的视频
- **特性**：
  - 自动调用系统视频播放器
  - 支持所有系统播放器
  - 查询所有应用录制的视频
  - 按时间倒序排列
- **语音命令**：
  - "播放视频" / "播放最新视频"
  - "查看视频" / "打开录像"
- **文件**：`data/repository/VideoPlaybackRepository.kt`

#### 3. 视频分享功能
- **问题**：无法将录像发送给家人朋友
- **解决**：一键分享最新视频
- **特性**：
  - 调用系统分享面板
  - 支持微信、QQ、邮件、蓝牙等
  - 自动选择最新视频
  - 正确授予URI读取权限
- **语音命令**：
  - "分享视频" / "分享最新视频"
  - "发送视频" / "发送录像"
- **文件**：`data/repository/VideoPlaybackRepository.kt`

#### 4. 智能存储管理
- **问题**：5分钟一段视频快速占满空间
- **解决**：多策略智能自动清理
- **特性**：
  - 每次视频保存后自动执行
  - 始终保留最新视频
  - 四种清理策略：
    - 时间：删除超过60分钟的视频
    - 数量：最多保留20个视频
    - 大小：总大小不超过1GB
    - 空间：确保至少500MB剩余空间
- **语音命令**：
  - "查询存储空间" / "查看存储" / "检查空间"
  - 反馈："当前有X个视频，占用X兆，剩余空间X兆"
- **文件**：`data/repository/StorageManagementRepository.kt`

#### 5. 扩展语音命令
- **新增命令**：
  - `PLAY_VIDEO` - 播放视频
  - `SHARE_VIDEO` - 分享视频
  - `CHECK_STORAGE` - 查询存储空间
- **更新识别逻辑**：
  - 支持更多同义词
  - 模糊匹配算法优化

### 🔄 改进

#### 存储清理策略优化
- **原逻辑**：固定删除超过10分钟的视频
- **新逻辑**：多策略智能清理
- **优势**：
  - 更灵活
  - 更智能
  - 更符合实际使用场景

#### ViewModel架构重构
- 新增 `videoPlaybackRepository` 参数
- 新增 `storageManagementRepository` 参数
- 新增 `playLatestVideo()` 方法
- 新增 `shareLatestVideo()` 方法
- 新增 `checkStorageInfo()` 方法
- 优化 `handleVoiceCommand()` 方法

#### MainActivity生命周期管理
- 在 `onResume()` 中启动前台服务
- 在 `onDestroy()` 中停止前台服务
- 新增前台服务运行状态标记

### 📝 配置更新

#### AndroidManifest.xml
- **新增权限**：
  ```xml
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
  ```
- **新增服务声明**：
  ```xml
  <service
      android:name=".domain.service.CameraForegroundService"
      android:enabled="true"
      android:exported="false"
      android:foregroundServiceType="camera" />
  ```

### 📊 代码统计
- **新增文件**：3个 (677行)
- **修改文件**：4个 (+172行)
- **总新增代码**：约850行
- **总项目代码**：约3,070行

### 🐛 Bug修复
- 修复后台录像被系统杀死的问题
- 修复存储空间快速耗尽的问题
- 优化自动清理逻辑

### 📚 文档
- **新增**：`FEATURES.md` - 完整功能说明文档
- **新增**：`DEVELOPMENT_LOG.md` - 详细开发日志
- **新增**：`CHANGELOG.md` - 本更新日志
- **待更新**：`README.md` - 项目说明

### ⚠️ 已知问题
1. **通知权限**（Android 13+）
   - 如果用户拒绝通知权限，前台服务仍能运行
   - 但无法显示通知，用户无感知
   - 影响：低，不影响核心功能

2. **电池优化**
   - 部分厂商（小米、华为）可能限制后台录像
   - 建议：用户将应用加入电池优化白名单

3. **存储清理确认**
   - MediaStore删除需要用户确认（Android 11+）
   - 批量删除可能触发多次确认弹窗
   - 影响：中等，影响用户体验

4. **分享大文件**
   - 大于100MB的视频分享可能缓慢
   - 某些应用（如微信）有文件大小限制（通常100MB）

### 🔮 未来计划
- 摄像头切换（前后摄像头）
- 闪光灯控制
- 录像质量调整（高清/标清/省电）
- 视频压缩
- 离线语音识别
- 紧急求助功能
- GPS位置标记
- AI场景识别

---

## [1.0.0] - 2026-01-26

### ✨ 初始版本

#### 核心功能

##### 1. 自动录像系统
- 应用启动后自动开始录像
- 自动分段保存（每5分钟）
- 暂停/继续录像
- 录像中拍照

**技术实现**：
- CameraX 1.4.0
- MediaStore API
- H.264 + AAC编码
- HD/SD自适应质量

##### 2. 语音交互系统
- 连续语音识别（中文）
- 自动重启监听
- 模糊命令匹配

**支持命令**：
- 拍照：`"拍照"` / `"拍一张"` / `"照相"`
- 暂停录像：`"暂停录像"` / `"暂停"`
- 继续录像：`"继续录像"` / `"开始"` / `"恢复"`
- 清空录像：`"清空录像"` / `"删除所有视频"`
- 关闭应用：`"关闭应用"` / `"退出"`

##### 3. 语音播报系统
- TextToSpeech（中文）
- 语速：0.9倍
- 操作反馈播报
- 优先级队列

**播报场景**：
- 应用启动
- 相机初始化
- 录像开始/暂停/继续
- 拍照成功/失败
- 视频保存
- 错误提示

##### 4. 触觉反馈系统
- 不同操作有独特振动模式
- 无需看屏幕即可感知反馈

**振动模式**：
- 拍照：50ms 短振
- 开始录像：100ms × 2
- 暂停录像：200ms 长振
- 成功：50ms
- 警告：100ms
- 错误：50ms × 3

##### 5. 无障碍UI设计
- Material 3 主题
- 高对比度支持
- 大字体（24sp+）
- 大触摸目标（72dp+）
- TalkBack支持
- 完整的contentDescription

##### 6. 文件管理
- 视频：`Movies/AccessibleCamera/`
- 照片：`Pictures/AccessibleCamera/`
- MediaStore集成
- Android 10+ Scoped Storage支持
- 基础存储空间检查

#### 架构设计
- **架构模式**：MVVM + Clean Architecture
- **UI框架**：Jetpack Compose
- **编程语言**：Kotlin 2.0.21

**分层结构**：
```
├── data/          # 数据层
│   ├── model/
│   └── repository/
├── domain/        # 业务层
│   └── service/
├── presentation/  # 表现层
│   ├── viewmodel/
│   ├── screen/
│   └── component/
└── ui/           # UI主题
    └── theme/
```

#### 依赖
- `androidx-compose-bom: 2024.09.00`
- `androidx-camera-*: 1.4.0`
- `accompanist-permissions: 0.32.0`
- `kotlin: 2.0.21`

#### 权限
- `CAMERA` - 录像和拍照
- `RECORD_AUDIO` - 录像音频
- `VIBRATE` - 触觉反馈
- `FOREGROUND_SERVICE` - 前台服务（预留）
- `FOREGROUND_SERVICE_CAMERA` - 相机前台服务（预留）
- `WRITE_EXTERNAL_STORAGE` - 存储文件（Android 9及以下）

#### 兼容性
- **最低版本**：Android 7.0 (API 24)
- **目标版本**：Android 14 (API 36)

#### 文档
- `README_IMPLEMENTATION.md` - 实现报告

---

## 版本格式说明

版本号格式：`主版本.次版本.修订号`

- **主版本**：不兼容的API变更
- **次版本**：向后兼容的功能新增
- **修订号**：向后兼容的问题修复

---

## 类型标签说明

- ✨ **新增功能** - 新功能或重要特性
- 🔄 **改进** - 功能优化或重构
- 🐛 **Bug修复** - 修复已知问题
- 📝 **配置更新** - 配置文件或依赖更新
- 📚 **文档** - 文档更新
- ⚠️ **已知问题** - 当前版本的已知限制
- 🔮 **未来计划** - 计划中的功能
- 🗑️ **移除** - 移除的功能或文件
- 🔒 **安全** - 安全相关更新

---

## 贡献指南

欢迎提交Issue和Pull Request！

提交格式：
```
<type>(<scope>): <subject>

<body>

<footer>
```

类型（type）：
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式（不影响代码运行的变动）
- `refactor`: 重构（既不是新增功能，也不是修复bug的代码变动）
- `perf`: 性能优化
- `test`: 增加测试
- `chore`: 构建过程或辅助工具的变动

---

## 联系方式

如有问题或建议，请通过以下方式联系：

- **项目地址**：待定
- **邮箱**：待定
- **Issue追踪**：GitHub Issues

---

*本文档最后更新：2026-01-28*
*当前版本：v1.1.0*
