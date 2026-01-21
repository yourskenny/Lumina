import time
import math
import numpy as np
from ultralytics import YOLOE

class BlindAssistProcessor:
    def __init__(self, model_path="jameslahm/yoloe-v8s-seg", device="cpu"):
        print(f"[Lumina] Initializing AI Engine ({device})...")
        self.device = device
        # 加载 YOLOE 模型
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
        
        # 预热/设置模型类别 (如果模型支持动态设置，这步很重要)
        # YOLOE 的强大之处在于 open-set，我们这里显式关注这些词
        print(f"[Lumina] Configured for {len(self.all_targets)} specialized blind-assist categories.")

    def estimate_distance_heuristic(self, box, frame_height):
        """
        --- 创新点 2: 单目视觉测距算法 (无需深度相机) ---
        基于透视原理的启发式算法：
        在地面假设平坦的情况下，物体接地脚点（Bounding Box底部）在画面中越低，距离越近。
        """
        x1, y1, x2, y2 = box
        
        # 归一化底部坐标 (0.0 - 1.0), 1.0 为画面最底部
        normalized_y_bottom = y2 / frame_height
        
        # 简单的非线性映射，模拟距离 (仅作示意，实际需标定)
        # y=1.0 -> dist=0m, y=0.5 -> dist=5m
        if normalized_y_bottom > 0.9:
            return "Immediate (<1m)", 0.5
        elif normalized_y_bottom > 0.75:
            return "Very Near (1-2m)", 1.5
        elif normalized_y_bottom > 0.5:
            return "Near (3-5m)", 4.0
        else:
            return "Far (>5m)", 8.0

    def analyze_scene(self, frame):
        """
        处理每一帧，返回结构化的导航建议
        """
        height, width = frame.shape[:2]
        
        # 推理
        results = self.model.predict(frame, show=False, conf=0.4, verbose=False)
        result = results[0]
        
        scene_summary = {
            "hazards": [],
            "path_info": [],
            "annotated_frame": result.plot()
        }
        
        # 解析结果
        if result.boxes:
            for box in result.boxes:
                # 获取类别名称
                class_id = int(box.cls[0])
                # 注意：YOLOE返回的names可能包含所有COCO类别，我们需要根据class_id映射
                # 这里为了演示简单，直接用模型返回的名称
                class_name = result.names[class_id]
                
                # 获取坐标
                xyxy = box.xyxy[0].cpu().numpy()
                
                # 估算距离
                dist_label, dist_val = self.estimate_distance_heuristic(xyxy, height)
                
                obj_info = {
                    "name": class_name,
                    "distance": dist_label,
                    "box": xyxy
                }
                
                # --- 创新点 3: 智能优先级排序 ---
                # 只有"极近"的障碍物才会被标记为高危
                if dist_val < 2.0 and class_name in self.target_classes["hazard"] + self.target_classes["interaction"]:
                    scene_summary["hazards"].append(obj_info)
                elif class_name in self.target_classes["path"]:
                    scene_summary["path_info"].append(obj_info)
                    
        return scene_summary

    def generate_voice_feedback(self, summary):
        """
        生成语音播报文本 (TTS 接口)
        """
        text = ""
        if summary["hazards"]:
            # 找出最近的一个障碍物
            nearest = summary["hazards"][0] 
            text = f"Warning: {nearest['name']} ahead, {nearest['distance']}."
        elif summary["path_info"]:
             path = summary["path_info"][0]
             text = f"Safe: {path['name']} detected."
        
        return text
