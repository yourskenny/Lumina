from ultralytics import YOLOE
import os

def export_model():
    print("=========================================")
    print("   Lumina: Mobile Deployment Exporter")
    print("=========================================")
    
    # 1. 加载模型
    model_name = "jameslahm/yoloe-v8s-seg"
    print(f"[Exporter] Loading model: {model_name}...")
    try:
        model = YOLOE.from_pretrained(model_name)
    except Exception as e:
        print(f"Error loading model: {e}")
        return

    # 2. 导出为 TFLite (Android)
    print("[Exporter] Exporting to TFLite (for Android)...")
    # int8 量化可以显著减小模型体积，适合手机运行
    # imgsz=320 降低分辨率以提高手机上的帧率
    try:
        # Note: 实际导出可能需要 tensorflow 库支持。
        # 如果环境缺少依赖，这里会抛出提示。
        path = model.export(format="tflite", imgsz=320, int8=True)
        print(f"[Success] TFLite model saved at: {path}")
    except Exception as e:
        print(f"[Warning] TFLite export failed (likely missing tensorflow): {e}")
        print("To fix: pip install tensorflow")

    # 3. 导出为 ONNX (通用移动端)
    print("\n[Exporter] Exporting to ONNX (Universal Mobile Format)...")
    try:
        path = model.export(format="onnx", imgsz=320, dynamic=True)
        print(f"[Success] ONNX model saved at: {path}")
    except Exception as e:
        print(f"[Error] ONNX export failed: {e}")

    print("\n[Summary] Export process completed.")
    print("You can now deploy these models to Android (TFLite) or iOS (CoreML/ONNX).")

if __name__ == "__main__":
    export_model()
