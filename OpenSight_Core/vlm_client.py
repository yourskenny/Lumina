import os
import time
import random
import logging
from abc import ABC, abstractmethod
from typing import List, Dict, Any, Optional

try:
    from openai import OpenAI
except ImportError:
    OpenAI = None

logger = logging.getLogger(__name__)

class BaseVLMClient(ABC):
    """
    视觉语言模型（VLM）客户端的抽象基类。
    """
    
    @abstractmethod
    def analyze(self, image_base64: str, local_context: Optional[List[Dict[str, Any]]] = None, prompt: Optional[str] = None) -> str:
        """
        分析图像，可选择传入本地上下文（如检测结果）和提示词。
        
        Args:
            image_base64 (str): Base64 编码的图像字符串。
            local_context (list, optional): 本地检测结果或上下文信息的列表。
            prompt (str, optional): 用于引导分析的可选提示词。
            
        Returns:
            str: 模型的分析结果文本。
        """
        pass

class MockCloudVLMClient(BaseVLMClient):
    """
    模拟云端 VLM 客户端，可以接受图像和本地上下文，模拟网络延迟并返回智能响应。
    """
    
    def __init__(self, min_delay: float = 1.0, max_delay: float = 3.0):
        """
        初始化模拟客户端。
        
        Args:
            min_delay (float): 最小模拟网络延迟（秒）。
            max_delay (float): 最大模拟网络延迟（秒）。
        """
        self.min_delay = min_delay
        self.max_delay = max_delay
        
    def analyze(self, image_base64: str, local_context: Optional[List[Dict[str, Any]]] = None, prompt: Optional[str] = None) -> str:
        """
        模拟将图像和上下文发送到云端 VLM 并获取响应的过程。
        """
        logger.info("MockCloudVLMClient: 开始分析图像...")
        
        # 模拟网络和处理延迟
        delay = random.uniform(self.min_delay, self.max_delay)
        logger.debug(f"正在模拟网络延迟: {delay:.2f} 秒...")
        time.sleep(delay)
        
        # 根据上下文生成看似智能的响应模板
        response_templates = [
            "基于视觉分析，我可以确认本地检测到的物体确实存在于场景中。",
            "图像包含多个值得关注的元素。本地检测结果与全局上下文高度吻合。",
            "我已经分析了该场景。这似乎是一个复杂的环境，存在多个相互作用的实体。",
            "提供的图像展示了一个典型的场景，与给定的本地上下文一致。",
            "我的分析表明，检测到物体的空间排列暗示着一个动态的场景情况。"
        ]
        
        base_response = random.choice(response_templates)
        
        # 结合本地上下文（如果存在）构建最终响应
        if local_context and len(local_context) > 0:
            objects = [str(obj.get('label', '未知物体')) for obj in local_context[:3]]
            context_str = "、".join(objects)
            if len(local_context) > 3:
                context_str += " 等"
                
            return f"{base_response} 具体来说，我注意到了以下关键元素：{context_str}。整体情况需要进一步监控和评估。"
        else:
            return f"{base_response} 没有提供具体的本地上下文，但整体场景结构很清晰，未发现明显异常。"

class QwenCloudVLMClient(BaseVLMClient):
    """
    基于 Qwen (通义千问) 视觉语言模型的云端客户端。
    通过 OpenAI 兼容接口调用。
    """
    
    def __init__(self, model_name: str = "qwen-vl-max"):
        """
        初始化 Qwen VLM 客户端。
        
        Args:
            model_name (str): 要使用的 Qwen 模型名称。
        """
        if OpenAI is None:
            raise ImportError("未安装 openai 库。请运行 `pip install openai`。")
            
        self.model_name = model_name
        self.api_key = os.environ.get("QWEN_API_KEY")
        self.base_url = os.environ.get("QWEN_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
        
        if not self.api_key:
            logger.warning("未设置 QWEN_API_KEY 环境变量。Qwen VLM 客户端可能无法正常工作。")
            
        self.client = OpenAI(
            api_key=self.api_key or "dummy_key",  # 避免缺少 key 报错，虽然调用时可能依然失败
            base_url=self.base_url
        )
        
    def analyze(self, image_base64: str, local_context: Optional[List[Dict[str, Any]]] = None, prompt: Optional[str] = None) -> str:
        """
        将图像和上下文发送到 Qwen 云端 VLM 并获取响应。
        """
        logger.info(f"QwenCloudVLMClient: 开始使用 {self.model_name} 分析图像...")
        
        system_prompt = (
            "你是一个帮助视障人士的智能导航助手。"
            "请结合我提供的本地图像检测数据和用户的提问，给出安全建议或环境描述。"
            "【严格要求】："
            "1. 你的回答必须极其简短、一针见血，通常不要超过30个字。"
            "2. 直接报告最近的危险或障碍物的位置。"
            "3. 绝对不要有任何寒暄（如'你好'、'好的'、'根据画面显示'等废话），直接说出重点。"
        )
        
        # 构建默认提示词
        if not prompt:
            prompt = "请描述前方的障碍物和环境信息。"
            
        # 整合本地上下文
        if local_context and len(local_context) > 0:
            context_desc = "本地系统已检测到以下物体：\n"
            for obj in local_context:
                label = obj.get('label', '未知')
                conf = obj.get('confidence', 0.0)
                context_desc += f"- {label} (置信度: {conf:.2f})\n"
            prompt = f"{context_desc}\n基于以上本地检测结果，结合你对图像的理解，{prompt}"
            
        try:
            # 格式化请求图像 URL
            if not image_base64.startswith("data:image"):
                image_url = f"data:image/jpeg;base64,{image_base64}"
            else:
                image_url = image_base64
                
            messages = [
                {
                    "role": "system",
                    "content": system_prompt
                },
                {
                    "role": "user",
                    "content": [
                        {"type": "image_url", "image_url": {"url": image_url}},
                        {"type": "text", "text": prompt}
                    ]
                }
            ]
            
            response = self.client.chat.completions.create(
                model=self.model_name,
                messages=messages
            )
            
            return response.choices[0].message.content
            
        except Exception as e:
            logger.error(f"调用 Qwen VLM API 时发生错误: {str(e)}")
            return f"抱歉，云端视觉分析暂时不可用。错误信息: {str(e)}"
