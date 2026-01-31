# 无障碍相机 (Accessible Camera)

专为视力障碍人群设计的Android相机应用

[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com/)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![Language](https://img.shields.io/badge/language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/license-待定-lightgrey.svg)](LICENSE)

---

## 📖 目录

- [简介](#-简介)
- [核心功能](#-核心功能)
- [技术特性](#-技术特性)
- [快速开始](#-快速开始)
- [使用指南](#-使用指南)
- [系统要求](#-系统要求)
- [项目结构](#-项目结构)
- [开发文档](#-开发文档)
- [贡献指南](#-贡献指南)
- [许可证](#-许可证)

---

## 🌟 简介

**无障碍相机**是一款专门为视力障碍人群设计的Android原生相机应用。通过语音交互、触觉反馈和高对比度UI，实现完全无视觉操作的录像和拍照功能。

### 设计理念

- **完全无障碍**：无需看屏幕即可完成所有操作
- **语音优先**：语音命令 + 语音反馈
- **触觉感知**：独特的振动模式反馈
- **自动化**：打开即录，无需手动操作
- **稳定可靠**：前台服务保护，后台持续运行

### 适用人群

- 视力障碍者（全盲/低视力）
- 运动障碍者（操作困难）
- 老年人（视力退化）
- 需要快速录像的场景

---

## ✨ 核心功能

### 🎥 自动录像系统
- ✅ 打开应用即开始录像
- ✅ 自动分段保存（每5分钟）
- ✅ 录像中随时拍照
- ✅ 后台/锁屏持续录像（前台服务保护）
- ✅ 暂停/继续控制

### 🎤 语音交互
**支持的语音命令**（共14个）：
| 功能 | 命令示例 | 版本 |
|------|---------|------|
| 拍照 | "拍照" / "拍一张" | v1.0 |
| 暂停录像 | "暂停录像" | v1.0 |
| 继续录像 | "继续录像" / "开始" | v1.0 |
| 播放视频 | "播放视频" / "查看最新录像" | v1.1 |
| 分享视频 | "分享视频" / "发送录像" | v1.1 |
| 查询存储 | "查询存储空间" | v1.1 |
| 切换摄像头 ⭐ | "切换摄像头" / "切换相机" | v1.2 |
| 闪光灯 ⭐ | "打开闪光灯" / "关闭闪光灯" | v1.2 |
| 查询电池 ⭐ | "查询电池" / "查看电量" | v1.2 |
| 查询时长 ⭐ | "录了多久" / "查询录像时长" | v1.2 |
| 查询位置 🆕 | "查询位置" / "我在哪" | v1.3 |
| 紧急呼叫 ⭐ | "紧急呼叫" / "紧急求助" | v1.2 |
| 清空录像 | "清空录像" | v1.0 |
| 关闭应用 | "关闭应用" | v1.0 |

### 🔊 语音播报
完整的操作反馈：
- 应用启动和状态变化
- 录像开始/暂停/保存
- 拍照成功/失败
- 存储空间信息
- 错误提示和建议

### 📳 触觉反馈
| 操作 | 振动模式 |
|------|---------|
| 拍照 | 短振（50ms） |
| 开始录像 | 连续两次短振 |
| 暂停录像 | 长振（200ms） |
| 成功 | 短振 |
| 错误 | 快速三次短振 |

### 🎬 视频管理 ⭐ 新增
- ✅ 视频回放（调用系统播放器）
- ✅ 一键分享（微信、QQ、邮件等）
- ✅ 智能存储管理（自动清理旧文件）
- ✅ 存储空间查询

### 🗂️ 智能存储 ⭐ 新增
**自动清理策略**：
- 保留最近60分钟的视频
- 最多保留20个视频
- 总大小不超过1GB
- 确保至少500MB剩余空间

### 📍 GPS定位功能 🆕 v1.3
**位置记录与查询**：
- ✅ 自动记录录像时的GPS位置
- ✅ 视频文件包含GPS元数据（经纬度）
- ✅ 照片文件名包含位置信息
- ✅ 语音查询当前位置坐标
- ✅ 支持后台定位（录像时持续更新）
- ✅ 位置历史记录（最近100个位置）

**语音命令**：
- "查询位置" / "我在哪" / "查看定位"
- 播报内容：纬度、经度、精度信息

**技术实现**：
- GPS Provider + Network Provider双重定位
- 5秒更新间隔，10米最小位移
- 精确到小数点后4位（约11米精度）
- 自动写入MediaStore视频元数据

---

## 🛠️ 技术特性

### 核心技术栈
- **开发语言**：Kotlin 2.0.21
- **UI框架**：Jetpack Compose + Material 3
- **相机引擎**：CameraX 1.4.0
- **架构模式**：MVVM + Clean Architecture

### 无障碍设计
- **高对比度主题**：自动检测系统设置
- **大触摸目标**：最小72dp × 72dp
- **大字体显示**：最小24sp
- **TalkBack支持**：完整的语义标签

### 技术亮点
- ✅ 前台服务保护（防止后台杀死）
- ✅ MediaStore API（兼容Android 10+ Scoped Storage）
- ✅ 连续语音识别（自动重启监听）
- ✅ 多策略智能存储管理
- ✅ H.264 + AAC高质量编码
- ✅ HD/SD自适应质量

---

## 🚀 快速开始

### 1. 克隆项目
```bash
git clone https://github.com/your-username/accessible-camera.git
cd accessible-camera
```

### 2. 打开项目
使用 **Android Studio Hedgehog (2023.1.1)** 或更高版本打开项目

### 3. 同步依赖
项目使用 Gradle 版本目录（Version Catalog）管理依赖：
```bash
./gradlew sync
```

### 4. 运行应用
连接Android设备或启动模拟器：
```bash
./gradlew installDebug
```

或在Android Studio中点击 **Run** 按钮

---

## 📱 使用指南

### 首次使用

1. **安装应用**
   - 从Google Play下载（待上线）
   - 或下载APK文件安装

2. **授予权限**
   - 相机权限（必需）
   - 录音权限（必需）
   - 通知权限（Android 13+，推荐）

3. **开始使用**
   - 打开应用 → 自动开始录像
   - 说出语音命令即可操作

### 日常使用

#### 场景1：记录生活
```
1. 打开应用（自动开始录像）
2. 说"拍照"抓拍重要瞬间
3. 说"暂停录像"停止录制
4. 说"分享视频"发给家人
```

#### 场景2：查看录像
```
1. 说"播放视频"
2. 系统播放器打开最新视频
3. 可在播放器中调整音量、进度等
```

#### 场景3：管理存储
```
1. 说"查询存储空间"
2. 语音播报："当前有12个视频，占用850兆，剩余空间2500兆"
3. 应用会自动清理旧视频，无需手动操作
```

### 省电建议

- 录像时关闭屏幕（锁屏仍会继续录像）
- 不需要时及时关闭应用
- 将应用加入电池优化白名单（推荐）

---

## 💻 系统要求

### 最低要求
- **Android版本**：Android 7.0 (API 24)
- **RAM**：2GB+
- **存储空间**：建议≥1GB可用空间
- **相机**：支持Camera2 API

### 推荐配置
- **Android版本**：Android 10 或更高
- **RAM**：4GB+
- **存储空间**：≥3GB可用空间
- **处理器**：中端及以上

### 兼容性
已测试的Android版本：
- ✅ Android 7.0 - 7.1 (Nougat)
- ✅ Android 8.0 - 8.1 (Oreo)
- ✅ Android 9 (Pie)
- ✅ Android 10 (Q)
- ✅ Android 11 (R)
- ✅ Android 12 - 12L (S)
- ✅ Android 13 (T)
- ✅ Android 14 (U)

---

## 📁 项目结构

```
myapplication/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/myapplication/
│   │   │   ├── data/                    # 数据层
│   │   │   │   ├── model/              # 数据模型
│   │   │   │   │   ├── CameraState.kt
│   │   │   │   │   └── RecordingStats.kt
│   │   │   │   └── repository/         # 数据仓库
│   │   │   │       ├── CameraRepository.kt
│   │   │   │       ├── MediaRepository.kt
│   │   │   │       ├── VideoPlaybackRepository.kt ⭐
│   │   │   │       ├── StorageManagementRepository.kt ⭐
│   │   │   │       ├── SettingsRepository.kt ⭐
│   │   │   │       └── LocationRepository.kt 🆕
│   │   │   │
│   │   │   ├── domain/                  # 业务层
│   │   │   │   └── service/            # 业务服务
│   │   │   │       ├── TextToSpeechService.kt
│   │   │   │       ├── VoiceRecognitionService.kt
│   │   │   │       ├── HapticFeedbackService.kt
│   │   │   │       ├── CameraForegroundService.kt ⭐
│   │   │   │       ├── EmergencyService.kt ⭐
│   │   │   │       └── LocationService.kt 🆕
│   │   │   │
│   │   │   ├── presentation/            # 表现层
│   │   │   │   ├── viewmodel/          # 视图模型
│   │   │   │   │   └── CameraViewModel.kt
│   │   │   │   ├── screen/             # 屏幕
│   │   │   │   │   ├── CameraScreen.kt
│   │   │   │   │   └── PermissionScreen.kt
│   │   │   │   └── component/          # 组件
│   │   │   │       ├── AccessibleButton.kt
│   │   │   │       └── CameraPreview.kt
│   │   │   │
│   │   │   ├── ui/theme/               # UI主题
│   │   │   │   ├── Color.kt
│   │   │   │   ├── Theme.kt
│   │   │   │   └── Type.kt
│   │   │   │
│   │   │   ├── util/                   # 工具类
│   │   │   │   ├── StorageUtils.kt
│   │   │   │   ├── PermissionUtils.kt
│   │   │   │   └── BatteryUtils.kt ⭐
│   │   │   │
│   │   │   └── MainActivity.kt         # 应用入口
│   │   │
│   │   ├── AndroidManifest.xml         # 清单文件
│   │   └── res/                        # 资源文件
│   │
│   └── build.gradle.kts                # 应用级构建配置
│
├── gradle/
│   └── libs.versions.toml              # 依赖版本管理
│
├── FEATURES.md                         # 功能详细说明
├── DEVELOPMENT_LOG.md                  # 开发日志
├── CHANGELOG.md                        # 更新日志
├── README.md                           # 本文件
└── README_IMPLEMENTATION.md            # 实现报告
```

---

## 📚 开发文档

完整的开发文档：

| 文档 | 说明 |
|------|------|
| [FEATURES.md](FEATURES.md) | 完整功能说明、使用场景、技术细节 |
| [DEVELOPMENT_LOG.md](DEVELOPMENT_LOG.md) | 详细开发日志、版本历史、技术实现 |
| [CHANGELOG.md](CHANGELOG.md) | 版本更新记录 |
| [README_IMPLEMENTATION.md](README_IMPLEMENTATION.md) | v1.0实现报告 |

### 架构说明

本应用采用 **Clean Architecture + MVVM** 模式：

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (UI, ViewModel, Screens, Components)   │
└───────────────┬─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│          Domain Layer                   │
│     (Services, Business Logic)          │
└───────────────┬─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│           Data Layer                    │
│  (Repositories, Models, Data Sources)   │
└─────────────────────────────────────────┘
```

**优势**：
- 清晰的职责分离
- 易于测试
- 高可维护性
- 易于扩展

---

## 🔧 构建与发布

### Debug构建
```bash
./gradlew assembleDebug
```

输出：`app/build/outputs/apk/debug/app-debug.apk`

### Release构建
```bash
./gradlew assembleRelease
```

输出：`app/build/outputs/apk/release/app-release.apk`

### 签名配置
在 `keystore.properties` 中配置签名信息：
```properties
storeFile=your-keystore.jks
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

---

## 🤝 贡献指南

欢迎贡献代码、报告问题或提出建议！

### 报告问题
- 使用GitHub Issues报告bug
- 提供详细的复现步骤
- 附上设备信息和Android版本

### 提交代码
1. Fork本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开Pull Request

### 提交规范
```
<type>(<scope>): <subject>

<body>

<footer>
```

**type类型**：
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式
- `refactor`: 重构
- `perf`: 性能优化
- `test`: 测试
- `chore`: 构建/工具变动

**示例**：
```
feat(video): 添加视频回放功能

- 实现VideoPlaybackRepository
- 添加语音命令支持
- 集成系统视频播放器

Closes #123
```

---

## 🗺️ 路线图

### v1.2（✅ 已完成 - 2026-01-28）
- [x] 摄像头切换（前/后）
- [x] 闪光灯控制
- [x] 扩展查询功能（电池、时长）
- [x] 设置数据管理
- [x] 紧急求助功能

### v1.3（✅ 已完成 - 2026-01-28）
- [x] GPS定位功能
- [x] 视频GPS元数据记录
- [x] 位置语音查询
- [x] 位置历史记录

### v1.4（计划中）
- [ ] 录像质量调整（高清/标清/省电）
- [ ] 视频压缩（节省空间）
- [ ] 电池优化（省电模式）
- [ ] 离线语音识别
- [ ] 单元测试覆盖

### v2.0（未来）
- [ ] AI场景识别
- [ ] 物体识别与朗读
- [ ] 多语言支持
- [ ] 云端备份
- [ ] 地理围栏提醒

---

## ❓ 常见问题

### Q: 后台录像被终止怎么办？
A:
1. 确保授予了前台服务权限
2. 将应用加入电池优化白名单
3. 部分厂商ROM需要手动允许后台运行

### Q: 语音识别不准确？
A:
1. 确保网络连接正常（使用在线识别）
2. 在安静环境使用
3. 说话清晰、语速适中
4. v2.0将支持离线识别

### Q: 视频文件在哪里？
A:
- 视频：`Movies/AccessibleCamera/`
- 照片：`Pictures/AccessibleCamera/`
- 可在系统相册中查看

### Q: 如何分享视频给他人？
A:
1. 说"分享视频"
2. 选择分享方式（微信、QQ、邮件等）
3. 选择联系人发送

### Q: 存储空间不足怎么办？
A:
- 应用会自动清理60分钟以前的视频
- 手动说"清空录像"删除所有视频
- 清理其他应用释放空间

---

## 📄 许可证

本项目许可证待定。

---

## 🙏 致谢

感谢所有为无障碍技术发展做出贡献的开发者和组织。

### 使用的开源项目
- [CameraX](https://developer.android.com/training/camerax) - Google
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Google
- [Accompanist](https://github.com/google/accompanist) - Google
- [Material Design](https://material.io/) - Google

---

## 📞 联系方式

- **项目主页**：待定
- **Issue追踪**：[GitHub Issues](https://github.com/your-username/accessible-camera/issues)
- **邮箱**：待定

---

## 📊 项目状态

![GitHub last commit](https://img.shields.io/github/last-commit/your-username/accessible-camera)
![GitHub issues](https://img.shields.io/github/issues/your-username/accessible-camera)
![GitHub stars](https://img.shields.io/github/stars/your-username/accessible-camera)

**当前版本**：v1.3.0
**更新日期**：2026-01-28
**开发状态**：积极开发中
**Phase 3**：GPS定位功能完成 ✅

---

<p align="center">
  <b>让科技惠及每一个人</b><br>
  <i>Made with ❤️ for accessibility</i>
</p>
