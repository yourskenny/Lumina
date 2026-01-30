# Deploying Lumina to Mobile Devices

This guide explains how to deploy the **Lumina** obstacle detection engine to Android and iOS devices.

## 1. Model Export (ONNX)
The `export_model.py` script now focuses on exporting to ONNX format, which is the primary inference engine for the Lumina Android app.

```bash
# Export YOLOE model to ONNX format
python export_model.py --model yoloe-v8s-seg --format onnx --opset 12
```

This will generate:
- `yoloe-v8s-seg.onnx` (Standard ONNX model)

## 2. Android Integration
Copy the exported `.onnx` file to the Android project assets directory:

```bash
cp runs/export/yoloe-v8s-seg.onnx ../app/src/main/assets/
```

The Android app is configured to load `yoloe-v8s-seg.onnx` using the ONNX Runtime library. TFLite support has been deprecated in favor of ONNX Runtime for better performance and feature support with YOLOE models.

## 3. iOS Deployment (CoreML)

1.  **Convert to CoreML**:
    (Requires macOS) Run: `yolo export model=yoloe-v8s-seg.pt format=coreml`
2.  **Import to Xcode**: Drag the `.mlpackage` into your Xcode project.
3.  **Run Inference**: Use the Vision framework (`VNCoreMLRequest`) to drive the model.

## 4. Performance Optimization Tips
- **Quantization**: Always use INT8 quantization (enabled in our export script) to reduce model size by 4x.
- **Resolution**: For mobile real-time performance, input resolution is set to `320x320` by default.
- **NPU/GPU Delegate**: On Android, enable the GPU delegate for 5x faster inference.
