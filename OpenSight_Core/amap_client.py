import os
import logging
from typing import Optional

logger = logging.getLogger(__name__)

class AMapClient:
    """
    高德地图 API 客户端，用于获取位置和周边 POI 信息。
    """
    
    def __init__(self):
        self.api_key = os.environ.get("AMAP_API_KEY")
        if not self.api_key:
            logger.warning("未设置 AMAP_API_KEY 环境变量，将使用模拟定位数据。")

    def get_nearby_poi(self, location: Optional[str] = None) -> str:
        """
        获取附近的 POI 信息。
        
        Args:
            location (str, optional): 经纬度坐标，例如 "116.481488,39.990464"。
            
        Returns:
            str: 周边环境的描述文本。
        """
        if not self.api_key:
            return "模拟定位: 当前位置前方有红绿灯和斑马线，右侧是清华大学东门"
            
        # TODO: 实现真实的高德地图 API 调用
        return "高德地图 API 调用尚未实现，请配置真实逻辑。"
