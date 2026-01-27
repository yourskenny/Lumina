import time
import math
import numpy as np
import os
from ultralytics import YOLOE

class BlindNavigator:
    def __init__(self, model_path=None, device="cpu"):
        print(f"[Lumina] Initializing AI Engine ({device})...")
        self.device = device
        
        # 优先使用本地最佳模型，否则使用默认模型
        if model_path is None:
            if os.path.exists("best.pt"):
                model_path = "best.pt"
            elif os.path.exists("OpenSight_Core/best.pt"):
                model_path = "OpenSight_Core/best.pt"
            else:
                model_path = "jameslahm/yoloe-v8s-seg" # Fallback to HF/Hub model
        
        print(f"[Lumina] Loading model from: {model_path}")
        self.model = YOLOE.from_pretrained(model_path)
        
        # --- 创新点 1: 针对视障场景的语义定义 ---
        # 我们不再使用通用的80类，而是定义核心关注对象
        self.target_classes = {
            # 路径相关
            "path": ["crosswalk", "stairs", "tactile paving"], 
            # 危险障碍
            "hazard": ["car", "motorcycle", "bicycle", "pole", "tree", "fire hydrant", "traffic cone"],
            # 交互对象
            "interaction": ["person", "dog", "cat", "chair", "traffic light", "stop sign"]
        }
        # 将所有类别展平供模型使用
        self.all_targets = [item for sublist in self.target_classes.values() for item in sublist]

        # 核心修改: 设置文本提示 (Text Prompt) 以启用开放词汇检测
        # 这会调用 get_text_pe 生成文本嵌入，并绑定到模型
        # self.model.set_classes(self.all_targets, self.model.get_text_pe(self.all_targets))
        
        # YOLOE 的强大之处在于 open-set，我们这里显式关注这些词
        print(f"[Lumina] Configured for {len(self.all_targets)} specialized blind-assist categories.")

    def _calculate_spatial_info(self, box, frame_width, frame_height):
        """
        计算空间信息：距离（米）和 方向（左/中/右）
        """
        
        # 左上角坐标， 右下角坐标
        x1, y1, x2, y2 = box
        
        # 计算中心点 X 坐标，判断方位
        center_x = (x1 + x2) / 2
        relative_x = center_x / frame_width
        
        if relative_x < 0.33:
            direction = "left"      # 10点-11点方向
        elif relative_x > 0.66:
            direction = "right"     # 1点-2点方向
        else:
            direction = "center"    # 12点方向
            
        # 启发式测距 (基于脚点 y2)
        # 假设 y2=height 时距离为 0m，y2=height/2 时距离为 5m
        normalized_y_bottom = y2 / frame_height
        
        # 简单的反比例模拟: distance ≈ k * (1/y - 1)
        # 这里简化为分段估算，返回 float 类型以便逻辑判断
        if normalized_y_bottom > 0.9:
            distance = 0.5
        elif normalized_y_bottom > 0.75:
            distance = 1.5
        elif normalized_y_bottom > 0.5:
            distance = 4.0
        else:
            distance = 8.0
            
        return distance, direction
    
    def analyze_scene(self, frame: np.ndarray) -> dict:
        """
        处理每一帧，返回 JSON 友好的字典结构
        """
        start_time = time.time()
        height, width = frame.shape[:2]
        
        # 推理
        results = self.model.predict(frame, show=False, conf=0.4, verbose=False)
        result = results[0]
        
        # 初始化响应结构
        response = {
            "meta": {
                "timestamp": start_time,
                "latency_ms": 0
            },
            "status": {
                "state": "SAFE",
                "summary": "Path is clear"
            },
            "feedback": {
                "tts_message": "",
                "haptic_pattern": "none"
            },
            "environment": {
                "hazards": [],
                "paths": []
            },
            "raw_objects": [],
            "annotated_frame": result.plot()
        }
        
        nearest_hazard_dist = 999.0
        nearest_hazard_name = ""
        nearest_hazard_dir = "" # 方向

        if result.boxes:
            for box in result.boxes:
                class_id = int(box.cls[0])
                # 处理可能出现的索引越界（如果模型返回了意料之外的类）
                if hasattr(result, 'names') and class_id < len(result.names):
                    class_name = result.names[class_id]
                else:
                    class_name = "unknown"

                # box.xyxy是二维张量，取第0行，从GPU转存到CPU，再转numpy
                xyxy = box.xyxy[0].cpu().numpy()
                conf = float(box.conf[0])
                
                # 计算空间信息
                dist, direction = self._calculate_spatial_info(xyxy, width, height)
                
                obj_data = {
                    "name": class_name,
                    "distance_m": dist,
                    "direction": direction,
                    "box": xyxy.tolist() # 转为 list 以便 JSON 序列化
                }
                
                # 填充 raw_objects
                response["raw_objects"].append(obj_data)

                # 分类处理
                if class_name in self.target_classes["hazard"] or class_name in self.target_classes["interaction"]:
                    response["environment"]["hazards"].append(obj_data)
                    # 寻找最近的障碍物
                    if dist < nearest_hazard_dist:
                        nearest_hazard_dist = dist
                        nearest_hazard_name = class_name
                        nearest_hazard_dir = direction
                        
                elif class_name in self.target_classes["path"]:
                    response["environment"]["paths"].append(obj_data)

        # --- 决策逻辑 (Decision Logic) ---
        
        # 1. 危险判定 (Danger Check)
        if nearest_hazard_dist < 1.5: # 1.5米内有障碍
            response["status"]["state"] = "DANGER"
            response["status"]["summary"] = f"STOP! {nearest_hazard_name} ahead"
            response["feedback"]["haptic_pattern"] = "heavy_pulse"
            
            # 构建更自然的语音提示，包含方位
            dir_str = "ahead"
            if nearest_hazard_dir == "left": dir_str = "on your left"
            if nearest_hazard_dir == "right": dir_str = "on your right"
            
            response["feedback"]["tts_message"] = f"Warning. {nearest_hazard_name} {dir_str}. {nearest_hazard_dist} meters."

        # 2. 警示判定 (Caution Check)
        elif nearest_hazard_dist < 3.0:
            response["status"]["state"] = "CAUTION"
            response["status"]["summary"] = f"{nearest_hazard_name} detected"
            response["feedback"]["haptic_pattern"] = "light_tap"
            # 只有当没有正在进行的紧急播报时，这里才可能产生语音（可在前端控制频率）
            
        # 3. 安全/路径提示
        elif len(response["environment"]["paths"]) > 0:
            best_path = response["environment"]["paths"][0]
            response["status"]["state"] = "SAFE"
            response["status"]["summary"] = f"Follow {best_path['name']}"
            # 安全时通常不需要震动，偶尔播报路径即可

        # 计算耗时
        response["meta"]["latency_ms"] = int((time.time() - start_time) * 1000)
        
        return response

    def generate_voice_feedback(self, summary):
        """
        生成语音播报文本 (TTS 接口)
        """
        if "feedback" in summary and summary["feedback"].get("tts_message"):
            return summary["feedback"]["tts_message"]
        env = summary.get("environment", {})
        hazards = env.get("hazards", [])
        paths = env.get("paths", [])
        if hazards:
            nearest = hazards[0]
            return f"Warning: {nearest['name']} ahead, {nearest['distance_m']} meters."
        if paths:
            path = paths[0]
            return f"Safe: {path['name']} detected."
        return ""

# --- 模拟调用示例 ---
if __name__ == "__main__":
    import json
    import cv2
    # 模拟一个空的 numpy 数组作为帧
    dummy_frame = np.zeros((640, 640, 3), dtype=np.uint8)
    # dummy_frame = cv2.imread("../1.png")
    
    navigator = BlindNavigator()
    
    # 运行分析
    json_result = navigator.analyze_scene(dummy_frame)
    
    # 打印 JSON 结果
    print(json.dumps(json_result, indent=2))
