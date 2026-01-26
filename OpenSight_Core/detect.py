import cv2
import time
import sys
# 引入我们自定义的核心模块
from blind_navigator import BlindNavigator

def run_lumina():
    print("=========================================")
    print("   Lumina: Intelligent Vision for the Visually Impaired")
    print("   Powered by Lumina Engine (YOLOE + Depth Estimation)")
    print("=========================================")
    
    # 初始化核心处理器
    try:
        processor = BlindNavigator()
    except Exception as e:
        print(f"Critical Error: Failed to initialize AI engine. {e}")
        return

    # 打开摄像头
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("Error: Camera not found.")
        return

    print("System Ready. Press 'q' to exit.")
    
    last_tts_time = 0
    tts_interval = 3.0 # 每3秒最多播报一次，防止刷屏
    
    while True:
        ret, frame = cap.read()
        if not ret:
            break

        # 1. 核心处理
        scene_data = processor.analyze_scene(frame)
        
        # 2. 获取可视化结果
        display_frame = scene_data["annotated_frame"]
        
        # 3. 智能语音反馈逻辑 (模拟)
        current_time = time.time()
        if current_time - last_tts_time > tts_interval:
            feedback_text = processor.generate_voice_feedback(scene_data)
            if feedback_text:
                print(f"🗣️ [TTS]: {feedback_text}")
                # TODO: 这里可以接入 pyttsx3 进行真实的语音播报
                last_tts_time = current_time
        
        # 4. 在屏幕上绘制额外的UI信息 (给演示者看)
        cv2.putText(display_frame, "Lumina Mode: Active", (20, 40), 
                    cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)
        
        if scene_data["environment"]["hazards"]:
            cv2.putText(display_frame, "WARNING: OBSTACLE", (20, 80), 
                        cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)

        cv2.imshow('Lumina Dashboard', display_frame)

        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()

if __name__ == '__main__':
    run_lumina()
