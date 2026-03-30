# PC Demo 环境 (PC Demo Environment)

## 项目概述 (Project Overview)
本项目是一个基于 PC 端的演示环境，集成了计算机视觉（Ultralytics YOLO, OpenCV）、语音交互（SpeechRecognition, pyttsx3, PyAudio）以及大语言模型智能对话（OpenAI）。用户可以通过图形界面与系统进行交互，实现视觉检测、语音识别和智能对话的综合体验。

## 环境要求 (Prerequisites)
运行本项目前，请确保您的系统满足以下条件：
- **操作系统**：Windows / macOS / Linux
- **Python 版本**：Python 3.8 或更高版本
- **硬件设备**：
  - 麦克风和扬声器（用于语音识别与合成）
  - 摄像头（用于视觉处理和实时检测）

## 安装步骤 (Installation)
请按照以下步骤配置开发环境：

1. **克隆或下载项目**：
   将本项目代码克隆或下载到本地目录，并进入项目文件夹：
   ```bash
   cd pc_demo_env
   ```

2. **创建虚拟环境（可选但推荐）**：
   ```bash
   python -m venv venv
   ```
   - **Windows** 激活虚拟环境:
     ```bash
     venv\Scripts\activate
     ```
   - **macOS / Linux** 激活虚拟环境:
     ```bash
     source venv/bin/activate
     ```

3. **安装项目依赖**：
   使用 `pip` 安装所需的第三方库：
   ```bash
   pip install -r requirements.txt
   ```

4. **配置环境变量**：
   在项目根目录下创建一个 `.env` 文件，并根据需要填入相关的 API 密钥配置信息（例如 OpenAI API Key）：
   ```env
   OPENAI_API_KEY=your_openai_api_key_here
   ```

## 运行指南 (How to Run)
在完成上述依赖安装和环境配置后，执行以下命令即可启动演示界面：

```bash
python demo_ui.py
```

启动后，您将看到图形化界面，并可以通过界面控件进行各项功能的交互演示。
