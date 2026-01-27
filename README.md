﻿﻿﻿# Lumina: Next-Gen Vision Assistant for the Visually Impaired

## 🌟 Project Overview
**Lumina** is a software-defined, open-vocabulary navigation assistant designed to run on ubiquitous hardware (standard laptops/smartphones). Unlike traditional solutions that rely on expensive depth sensors (LiDAR/RGB-D) or closed-set detection models, Lumina leverages **YOLOE (Open-Vocabulary Object Detection)** and **Monocular Depth Heuristics** to provide real-time, context-aware navigation aids.

## 🚀 Key Innovations

### 1. Open-Vocabulary Perception (Powered by Lumina Engine)
Traditional blind assistants can only detect pre-trained categories (e.g., "car", "person"). Lumina introduces a dynamic perception engine that can understand complex scene semantics.
*   **Current Mode**: Optimized for urban navigation (detecting tactile paving, crosswalks, traffic lights).
*   **Future Capability**: Users can issue voice commands like "Find the empty seat" or "Locate the elevator," and the model adapts in real-time.

### 2. Software-Defined Depth Estimation
We eliminated the need for heavy depth cameras. Lumina implements a lightweight **Monocular Depth Heuristic Algorithm**.
*   By analyzing the grounding point (bounding box bottom) relative to the horizon, the system estimates distance zones (Immediate, Near, Far) with zero hardware cost.
*   This makes the solution deployable on any standard webcam or phone camera.

### 3. Intelligent Hazard Prioritization
The system doesn't just "detect" objects; it "understands" danger.
*   Objects are classified into `Path` (Safe), `Interaction` (Neutral), and `Hazard` (Danger).
*   Voice feedback is prioritized: Immediate Hazards > Path Confirmation > General Description.

## 🏗️ System Architecture

The project consists of two main components working together:

1.  **Backend (Brain)**:
    *   **Tech Stack**: Python, FastAPI, YOLOE (Ultralytics), OpenCV.
    *   **Core Logic**: `OpenSight_Core/blind_navigator.py` encapsulates the inference and spatial analysis.
    *   **Interface**: `OpenSight_Core/api.py` provides a REST API (`POST /analyze`) for clients.

2.  **Frontend (Eyes & Ears)**:
    *   **Tech Stack**: Android (Kotlin), Jetpack Compose, CameraX, Retrofit.
    *   **Features**:
        *   **Real-time Analysis**: Captures images periodically and sends them to the backend.
        *   **Voice Commands**: Hands-free control (e.g., "Pause", "Capture").
        *   **Feedback**: Text-to-Speech (TTS) for alerts and Haptic feedback for danger warnings.
        *   **Video Recording**: Continuous background recording for safety logging.

## 📦 Directory Structure

```
Lumina/
├── OpenSight_Core/           # Backend System
│   ├── api.py               # FastAPI Server Entry Point
│   ├── blind_navigator.py   # Core AI Logic & YOLOE Wrapper
│   ├── detect.py            # Desktop/Local Demo Interface
│   ├── best.pt              # Trained Model Weights
│   └── ...
├── app/                      # Android Application
│   ├── src/main/java/...    # Kotlin Source Code (MVVM Architecture)
│   ├── src/main/AndroidManifest.xml
│   └── ...
├── gradle/                   # Android Build System
└── requirements.txt          # Python Dependencies
```

## 🏁 Quick Start

### 1. Backend Setup
Prerequisite: Python 3.8+

```bash
# Clone the repository
git clone https://github.com/yourskenny/Lumina.git
cd Lumina

# Install dependencies
pip install -r requirements.txt

# Start the Server
python OpenSight_Core/api.py
```
*Server will start at `http://0.0.0.0:5000`*

### 2. Android App Setup
Prerequisite: Android Studio Koala+

1.  Open `Lumina/` project in Android Studio.
2.  Navigate to `app/src/main/java/com/example/myapplication/data/remote/NetworkModule.kt`.
3.  Update `BASE_URL` to your computer's IP address (e.g., `http://192.168.1.100:5000/`).
4.  Build and Run on a physical device (ensure phone and PC are on the same WiFi).

## 📱 Features Checklist
- [x] **Object Detection**: Recognizes hazards, paths, and interactions.
- [x] **Distance Estimation**: Estimates distance without depth sensors.
- [x] **Voice Feedback**: TTS alerts for immediate dangers.
- [x] **Haptic Feedback**: Vibration patterns for different alert levels.
- [x] **Voice Control**: "Capture", "Pause", "Resume", "Close App".
- [x] **Video Recording**: Background loop recording.
- [x] **Cloud/Edge Integration**: App offloads processing to the Python backend.

---
*Powered by YOLOE & Monocular Depth Estimation*
