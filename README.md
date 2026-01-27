﻿# Lumina: Intelligent Offline Vision Assistant

> **Empowering the visually impaired with real-time, offline, on-device AI navigation.**

![Lumina Banner](https://via.placeholder.com/1200x400?text=Lumina+Vision+Assistant)

## 🌟 Project Overview

**Lumina** is a cutting-edge Android application designed to act as a digital eye for visually impaired users. Unlike traditional solutions that rely on cloud APIs or heavy server backends, Lumina runs **entirely offline** on the smartphone. 

It leverages **ONNX Runtime** to execute advanced **YOLOE (You Only Look At Coefficients)** object detection models directly on the device's CPU/NPU/GPU. This ensures **zero latency**, **perfect privacy** (no images leave the device), and **reliability** even in areas without internet connection.

## 🚀 Key Features

*   **⚡ Fully Offline Inference**: Powered by `onnxruntime-android`, eliminating network dependency and latency.
*   **🧠 Advanced Perception**: Uses a custom-trained **YOLOE-v8s-Seg** model optimized for blind assist scenarios.
*   **📏 Monocular Depth Estimation**: Innovative heuristic algorithm calculates distance and direction of obstacles using standard 2D cameras.
*   **🛡️ Privacy First**: All image processing happens locally in RAM. No data uploads.
*   **🔊 Multi-Modal Feedback**:
    *   **Text-to-Speech (TTS)**: Verbal warnings for hazards (e.g., "Car ahead, 2 meters").
    *   **Haptic Feedback**: Vibration patterns indicating proximity and danger levels.
*   **🗣️ Voice Control**: Hands-free operation with voice commands like "Capture", "Pause", etc.

## 🏗️ System Architecture

Lumina has evolved from a client-server model to a robust **Standalone Edge AI Architecture**.

```mermaid
graph TD
    subgraph "Android Device (Pixel/Samsung/etc.)"
        Camera[CameraX Input] -->|Bitmap Stream| ViewModel[CameraViewModel]
        
        subgraph "AI Core (ObjectDetector.kt)"
            ViewModel -->|Bitmap| Preprocess[Pre-processing\n(Resize/Normalize)]
            Preprocess -->|FloatBuffer| ONNX[ONNX Runtime Engine]
            
            subgraph "Assets"
                ModelFile[yoloe-v8s-seg.onnx]
            end
            
            ModelFile -.->|Load| ONNX
            ONNX -->|Raw Tensor| Postprocess[Post-processing]
            
            subgraph "Post-processing Logic"
                Postprocess --> Parser[Output Parser]
                Parser --> NMS[Non-Maximum Suppression]
                NMS --> Spatial[Spatial Analysis\n(Distance & Direction)]
            end
        end
        
        Spatial -->|DetectionResult| ViewModel
        ViewModel -->|State Update| UI[Jetpack Compose UI]
        ViewModel -->|Text| TTS[Text-To-Speech]
        ViewModel -->|Vibration| Haptic[Haptic Feedback]
    end
```

### Core Components

1.  **ObjectDetector (`domain/detector/ObjectDetector.kt`)**:
    *   The heart of the application.
    *   Manages the **ONNX Runtime Session**.
    *   Performs raw tensor data parsing from the YOLOE model.
    *   Implements **NMS (Non-Maximum Suppression)** to filter redundant detection boxes.
    *   Calculates **Spatial Depth** based on bounding box positioning.

2.  **CameraViewModel (`presentation/viewmodel/CameraViewModel.kt`)**:
    *   Manages the application state (Safe, Caution, Danger).
    *   Coordinations camera frames with the detector.
    *   Throttles TTS messages to prevent audio clutter.

3.  **UI Layer (`MainActivity.kt`)**:
    *   Built with **Jetpack Compose** for a modern, responsive interface.
    *   Displays a real-time camera feed with overlay bounding boxes (for sighted assistants/debugging).

## 🛠️ Technical Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose
*   **Camera**: CameraX
*   **AI Engine**: [ONNX Runtime for Android](https://onnxruntime.ai/)
*   **Model Architecture**: YOLOE (YOLO Efficient)
*   **Build System**: Gradle (Kotlin DSL)

## � Project Structure

```
Lumina/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   └── yoloe-v8s-seg.onnx      # The AI Model File
│   │   ├── java/com/example/myapplication/
│   │   │   ├── domain/detector/
│   │   │   │   └── ObjectDetector.kt   # Core Inference Logic
│   │   │   ├── presentation/
│   │   │   │   └── viewmodel/          # State Management
│   │   │   └── MainActivity.kt         # UI Entry Point
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts                # Dependencies
└── ...
```

## 🏁 Getting Started

### Prerequisites
*   Android Studio (Koala or newer recommended).
*   Android Device with Android 8.0 (Oreo) or higher.

### Installation

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/YourUsername/Lumina.git
    ```

2.  **Open in Android Studio**:
    *   Select `Open` and navigate to the `Lumina` directory.
    *   Wait for Gradle sync to complete.

3.  **Run on Device**:
    *   Connect your Android phone via USB.
    *   Click the **Run** (Play) button.
    *   Grant Camera and Microphone permissions when prompted.

## 🧩 Model Details

The current model is a **YOLOE-v8s-seg** variant exported to ONNX format.

*   **Input Size**: 320x320
*   **Classes**: Specialized subsets for blind assistance:
    *   **Hazards**: Car, Motorcycle, Pole, Tree, Fire Hydrant, etc.
    *   **Paths**: Crosswalk, Stairs, Tactile Paving.
    *   **Interactions**: Person, Chair, Traffic Light.
*   **Format**: ONNX (Opset 17)

## 🤝 Contributing

Contributions are welcome! Please follow these steps:
1.  Fork the project.
2.  Create your feature branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---
*Built with ❤️ for the Software Innovation Contest.*
