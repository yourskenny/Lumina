import tkinter as tk
from tkinter import ttk, scrolledtext
import cv2
from PIL import Image, ImageTk
from ultralytics import YOLO
import math
import base64
import threading
import os
import pyttsx3
import speech_recognition as sr
from dotenv import load_dotenv
from vlm_client import MockCloudVLMClient, QwenCloudVLMClient

load_dotenv()

class DemoUI:
    def __init__(self, root):
        self.root = root
        self.root.title("OpenSight - 视障辅助终端演示")
        self.root.geometry("1050x650")
        
        # 尝试加载模型
        try:
            self.model = YOLO("yolov8n.pt")
        except Exception as e:
            print(f"Error loading YOLO model: {e}")
            self.model = None

        # 打开测试视频
        self.cap = cv2.VideoCapture("test_video.mp4")
        if not self.cap.isOpened():
            print("Warning: 无法打开 test_video.mp4")
            
        self.current_detections = []
        self.current_frame = None
        
        # 初始化 VLM 客户端
        qwen_api_key = os.getenv("QWEN_API_KEY")
        if qwen_api_key and qwen_api_key.strip():
            self.vlm_client = QwenCloudVLMClient()
            self.vlm_mode = "QwenCloudVLMClient"
        else:
            self.vlm_client = MockCloudVLMClient()
            self.vlm_mode = "MockCloudVLMClient"
        
        self.setup_ui()
        
        # 启动视频播放循环
        self.playing = True
        self.update_video()

    def setup_ui(self):
        # 左侧：视频显示区域
        self.left_frame = ttk.Frame(self.root, padding=10)
        self.left_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        
        self.video_label = tk.Label(self.left_frame, text="视频加载中...", bg="black")
        self.video_label.pack(fill=tk.BOTH, expand=True)

        # 右侧：信息与聊天区域
        self.right_frame = ttk.Frame(self.root, padding=10, width=380)
        self.right_frame.pack(side=tk.RIGHT, fill=tk.Y)
        self.right_frame.pack_propagate(False) # 固定宽度

        # 右侧上：检测信息面板
        ttk.Label(self.right_frame, text="实时环境感知信息", font=("Microsoft YaHei", 12, "bold")).pack(anchor=tk.W, pady=(0, 5))
        
        self.info_text = scrolledtext.ScrolledText(self.right_frame, height=12, state='disabled', font=("Microsoft YaHei", 10))
        self.info_text.pack(fill=tk.X, pady=(0, 15))

        # 右侧下：Agent 聊天界面
        ttk.Label(self.right_frame, text="智能导航助手 (Agent)", font=("Microsoft YaHei", 12, "bold")).pack(anchor=tk.W, pady=(0, 5))
        
        self.chat_history = scrolledtext.ScrolledText(self.right_frame, height=12, state='disabled', font=("Microsoft YaHei", 10))
        self.chat_history.pack(fill=tk.BOTH, expand=True, pady=(0, 5))
        
        self.append_chat("System", f"当前 VLM 模式: {self.vlm_mode}")
        self.append_chat("Agent", "你好！我是你的智能导航助手。请问有什么可以帮你的？可以问我'前面有什么'。")

        self.input_frame = ttk.Frame(self.right_frame)
        self.input_frame.pack(fill=tk.X)
        
        self.chat_input = ttk.Entry(self.input_frame, font=("Microsoft YaHei", 10))
        self.chat_input.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 5))
        self.chat_input.bind("<Return>", lambda event: self.send_message())
        
        self.speak_btn = ttk.Button(self.input_frame, text="播报当前视野", command=self.speak_closest_object)
        self.speak_btn.pack(side=tk.LEFT, padx=(0, 5))
        
        self.speech_btn = ttk.Button(self.input_frame, text="语音输入", command=self.start_speech_recognition)
        self.speech_btn.pack(side=tk.LEFT, padx=(0, 5))
        
        self.send_btn = ttk.Button(self.input_frame, text="发送", command=self.send_message)
        self.send_btn.pack(side=tk.RIGHT)

    def update_video(self):
        if not self.playing:
            return
            
        ret, frame = self.cap.read()
        if not ret:
            # 视频循环播放
            self.cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
            ret, frame = self.cap.read()
            if not ret:
                # 视频读取彻底失败
                self.root.after(100, self.update_video)
                return

        self.current_frame = frame.copy()

        # 调整大小以适应画布，这里固定一个合理大小
        frame = cv2.resize(frame, (640, 480))
        
        if self.model:
            results = self.model(frame, verbose=False)
            annotated_frame = results[0].plot()
            self.extract_detections(results[0])
        else:
            annotated_frame = frame
            
        # 转换为 Tkinter 可用的图像格式 (RGB)
        rgb_frame = cv2.cvtColor(annotated_frame, cv2.COLOR_BGR2RGB)
        img = Image.fromarray(rgb_frame)
        imgtk = ImageTk.PhotoImage(image=img)
        
        self.video_label.imgtk = imgtk # 防止被垃圾回收
        self.video_label.configure(image=imgtk)
        
        self.update_info_panel()
        
        # 每 30 毫秒更新一次（约 33 帧/秒）
        self.root.after(30, self.update_video)

    def extract_detections(self, result):
        self.current_detections = []
        boxes = result.boxes
        if boxes is None:
            return
            
        for box in boxes:
            # 类别 ID 和名称
            cls_id = int(box.cls[0].item())
            name = result.names[cls_id]
            
            # 置信度
            conf = float(box.conf[0].item())
            
            # 边界框坐标
            x1, y1, x2, y2 = box.xyxy[0].tolist()
            width = x2 - x1
            height = y2 - y1
            
            # 伪距离计算：假设物体在图像中占比越大，距离越近
            area = width * height
            if area > 0:
                # 启发式常数，让距离看起来更真实
                pseudo_distance = 1500.0 / math.sqrt(area)
                pseudo_distance = round(max(0.5, min(pseudo_distance, 15.0)), 1)
            else:
                pseudo_distance = 99.9
                
            self.current_detections.append({
                "name": name,
                "conf": conf,
                "distance": pseudo_distance
            })

    def update_info_panel(self):
        self.info_text.config(state='normal')
        self.info_text.delete(1.0, tk.END)
        
        if not self.current_detections:
            self.info_text.insert(tk.END, "当前视野内未检测到显著物体。")
        else:
            self.info_text.insert(tk.END, f"检测到 {len(self.current_detections)} 个目标:\n\n")
            # 按距离从小到大排序
            sorted_dets = sorted(self.current_detections, key=lambda x: x["distance"])
            for det in sorted_dets:
                name_zh = self.translate_label(det['name'])
                self.info_text.insert(tk.END, f"• {name_zh} ({det['name']})\n")
                self.info_text.insert(tk.END, f"  估计距离: {det['distance']} 米\n")
                self.info_text.insert(tk.END, f"  置信度: {det['conf']:.2f}\n\n")
                
        self.info_text.config(state='disabled')
        
    def translate_label(self, name):
        # 常见 COCO 类别翻译字典
        trans = {
            "person": "人", "car": "汽车", "bicycle": "自行车", "motorcycle": "摩托车",
            "airplane": "飞机", "bus": "公交车", "train": "火车", "truck": "卡车",
            "traffic light": "红绿灯", "stop sign": "停车标志", "bench": "长椅",
            "bird": "鸟", "cat": "猫", "dog": "狗", "horse": "马", "sheep": "羊",
            "cow": "牛", "elephant": "大象", "bear": "熊", "zebra": "斑马",
            "backpack": "背包", "umbrella": "雨伞", "handbag": "手提包", "tie": "领带",
            "suitcase": "行李箱", "bottle": "瓶子", "cup": "杯子", "chair": "椅子",
            "sofa": "沙发", "potted plant": "盆栽", "bed": "床", "dining table": "餐桌",
            "tv": "电视", "laptop": "笔记本电脑", "mouse": "鼠标", "remote": "遥控器",
            "keyboard": "键盘", "cell phone": "手机", "book": "书", "clock": "时钟"
        }
        return trans.get(name, name)

    def append_chat(self, sender, message):
        self.chat_history.config(state='normal')
        
        # 根据发送者设置不同前缀
        prefix = f"[{sender}] "
        self.chat_history.insert(tk.END, prefix)
        self.chat_history.insert(tk.END, f"{message}\n\n")
        
        self.chat_history.see(tk.END)
        self.chat_history.config(state='disabled')

    def speak_async(self, text):
        def _speak():
            engine = pyttsx3.init()
            engine.say(text)
            engine.runAndWait()
        threading.Thread(target=_speak, daemon=True).start()

    def speak_closest_object(self):
        if not self.current_detections:
            self.speak_async("当前视野内未检测到显著物体。")
            return
            
        sorted_dets = sorted(self.current_detections, key=lambda x: x["distance"])
        closest = sorted_dets[0]
        name_zh = self.translate_label(closest['name'])
        distance = closest['distance']
        self.speak_async(f"前方 {distance} 米处有 {name_zh}。")

    def start_speech_recognition(self):
        self.speech_btn.config(state='disabled')
        self.append_chat("System", "请开始说话，正在聆听...")
        self.speak_async("请开始说话")
        threading.Thread(target=self._recognize_speech_thread, daemon=True).start()

    def _recognize_speech_thread(self):
        recognizer = sr.Recognizer()
        try:
            with sr.Microphone() as source:
                recognizer.adjust_for_ambient_noise(source, duration=0.5)
                audio = recognizer.listen(source, timeout=5, phrase_time_limit=10)
            
            self.root.after(0, lambda: self.append_chat("System", "正在识别..."))
            text = recognizer.recognize_google(audio, language='zh-CN')
            
            self.root.after(0, lambda: self._handle_speech_success(text))
            
        except sr.WaitTimeoutError:
            self.root.after(0, lambda: self._handle_speech_error("聆听超时，未检测到语音。"))
        except sr.UnknownValueError:
            self.root.after(0, lambda: self._handle_speech_error("抱歉，未能听清您说的话。"))
        except sr.RequestError as e:
            self.root.after(0, lambda: self._handle_speech_error(f"语音识别服务请求失败: {e}"))
        except Exception as e:
            self.root.after(0, lambda: self._handle_speech_error(f"语音输入发生错误: {str(e)}"))
        finally:
            self.root.after(0, lambda: self.speech_btn.config(state='normal'))

    def _handle_speech_success(self, text):
        self.chat_input.delete(0, tk.END)
        self.chat_input.insert(0, text)
        self.send_message()

    def _handle_speech_error(self, error_msg):
        self.append_chat("System", error_msg)
        self.speak_async(error_msg)

    def send_message(self):
        user_msg = self.chat_input.get().strip()
        if not user_msg:
            return
            
        # 显示用户消息
        self.append_chat("User", user_msg)
        self.chat_input.delete(0, tk.END)
        
        # 使用多线程异步调用 VLM 客户端，防止界面卡顿
        if self.current_frame is not None:
            # 将当前帧编码为 Base64
            _, buffer = cv2.imencode('.jpg', self.current_frame)
            frame_base64 = base64.b64encode(buffer).decode('utf-8')
            
            # 打包当前检测结果作为本地上下文
            local_context = []
            for d in self.current_detections:
                local_context.append({
                    "label": self.translate_label(d["name"]),
                    "confidence": d["conf"],
                    "distance": d["distance"]
                })
                
            # 启动异步线程调用 VLM
            threading.Thread(
                target=self._call_vlm_async,
                args=(frame_base64, local_context, user_msg),
                daemon=True
            ).start()
        else:
            self.append_chat("Agent", "抱歉，目前无法获取摄像头画面进行分析。")
            
    def _call_vlm_async(self, frame_base64, local_context, user_msg):
        try:
            # 提示用户正在思考
            self.root.after(0, lambda: self.append_chat("Agent", "正在分析环境，请稍候..."))
            
            # 调用云端 VLM 客户端
            reply = self.vlm_client.analyze(
                image_base64=frame_base64,
                local_context=local_context,
                prompt=user_msg
            )
            
            # 将回复显示到 UI (必须在主线程更新 UI)
            self.root.after(0, lambda: self.append_chat("Agent", reply))
            self.speak_async(reply)
        except Exception as e:
            error_msg = f"分析失败: {str(e)}"
            self.root.after(0, lambda: self.append_chat("Agent", error_msg))
            self.speak_async("分析失败")

    def on_closing(self):
        self.playing = False
        if self.cap.isOpened():
            self.cap.release()
        self.root.destroy()

if __name__ == "__main__":
    root = tk.Tk()
    app = DemoUI(root)
    root.protocol("WM_DELETE_WINDOW", app.on_closing)
    root.mainloop()
