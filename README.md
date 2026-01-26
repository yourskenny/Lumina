# Lumina: Next-Gen Vision Assistant for the Visually Impaired

## � Project Overview
**Lumina** is a software-defined, open-vocabulary navigation assistant designed to run on ubiquitous hardware (standard laptops/smartphones). Unlike traditional solutions that rely on expensive depth sensors (LiDAR/RGB-D) or closed-set detection models, Lumina leverages **YOLOE (Open-Vocabulary Object Detection)** and **Monocular Depth Heuristics** to provide real-time, context-aware navigation aids.

##  Key Innovations

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

##  Architecture
*   **Core Engine**: `blind_navigator.py` - Encapsulates the YOLOE inference and distance logic.
*   **Interface**: `detect.py` - Real-time visualization and TTS (Text-to-Speech) trigger.
*   **Model**: YOLOE-v8s-seg (Segmentation enabled for precise path delineation).
*   **Mobile Deployment**: Includes export scripts (`export_for_mobile.py`) to convert models for Android (TFLite) and iOS.

## � Quick Start

1. **Clone the Repository**
   ```bash
   git clone https://github.com/yourskenny/Lumina.git
   cd Lumina/OpenSight_Core
   ```

2. **Install Dependencies**
   ```bash
   pip install ultralytics opencv-python
   ```

3. **Run the Assistant**
   ```bash
   python detect.py
   ```
   *The system will automatically download the necessary YOLOE weights on the first run.*

##  Mobile Deployment
To deploy Lumina to Android or iOS, please verify our deployment guide:
[DEPLOY_MOBILE.md](./OpenSight_Core/DEPLOY_MOBILE.md)

1. Run the export script:
   ```bash
   python export_for_mobile.py
   ```
2. Follow the integration steps in the guide to embed the `.tflite` model into your app.

##  Roadmap
*   [ ] Integration with Android/iOS Camera API.
*   [ ] LLM-based Scene Description (using VLM).
*   [ ] GPS + Visual Localization fusion.

---
*Powered by YOLOE & Monocular Depth Estimation*
