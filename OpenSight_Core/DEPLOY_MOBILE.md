# Deploying Lumina to Mobile Devices

This guide explains how to deploy the **Lumina** obstacle detection engine to Android and iOS devices.

## 1. Export the Model
First, run the export script to generate mobile-optimized models from the Python environment:
```bash
python export_for_mobile.py
```
This will generate:
- `yoloe-v8s-seg-int8.tflite` (Optimized for Android)
- `yoloe-v8s-seg.onnx` (Universal format)

## 2. Android Deployment (TFLite)

### Prerequisites
- Android Studio Koala or later
- Android Device (Android 10+)

### Integration Steps
1.  **Create Assets Folder**: In your Android project, create `app/src/main/assets/`.
2.  **Copy Model**: Copy `yoloe-v8s-seg-int8.tflite` to the assets folder.
3.  **Add Dependencies**:
    Add the following to your `build.gradle`:
    ```gradle
    implementation 'org.tensorflow:tensorflow-lite:2.14.0'
    implementation 'org.tensorflow:tensorflow-lite-support:0.4.4'
    implementation 'org.tensorflow:tensorflow-lite-gpu:2.14.0'
    ```
4.  **Inference Code (Java/Kotlin)**:
    Use the `Interpreter` class to load the model:
    ```java
    try (Interpreter interpreter = new Interpreter(loadModelFile("yoloe-v8s-seg-int8.tflite"))) {
        // Run inference
        interpreter.run(inputBitmap, outputBuffer);
    }
    ```

## 3. iOS Deployment (CoreML)

1.  **Convert to CoreML**:
    (Requires macOS) Run: `yolo export model=yoloe-v8s-seg.pt format=coreml`
2.  **Import to Xcode**: Drag the `.mlpackage` into your Xcode project.
3.  **Run Inference**: Use the Vision framework (`VNCoreMLRequest`) to drive the model.

## 4. Performance Optimization Tips
- **Quantization**: Always use INT8 quantization (enabled in our export script) to reduce model size by 4x.
- **Resolution**: For mobile real-time performance, input resolution is set to `320x320` by default.
- **NPU/GPU Delegate**: On Android, enable the GPU delegate for 5x faster inference.
