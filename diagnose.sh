#!/bin/bash

echo "======================================"
echo "   语音识别完整诊断脚本"
echo "======================================"
echo ""

# 清空日志
adb logcat -c
echo "✓ 日志已清空"
echo ""

# 重启应用
echo "正在重启应用..."
adb shell am force-stop com.example.myapplication
sleep 1
adb shell am start -n com.example.myapplication/.MainActivity
sleep 3
echo "✓ 应用已启动"
echo ""

echo "======================================"
echo "   请按以下步骤操作："
echo "======================================"
echo "1. 在应用中找到【极简测试】按钮"
echo "2. 点击按钮"
echo "3. 清楚地说：hello"
echo "4. 等待 5 秒"
echo ""
echo "按 Enter 键开始收集日志..."
read

echo ""
echo "======================================"
echo "   开始收集日志（15秒）"
echo "======================================"
echo ""

# 创建日志文件
LOG_FILE="/Users/mac/Desktop/project/myapplication/voice_debug_$(date +%Y%m%d_%H%M%S).log"

# 收集15秒的日志
timeout 15s adb logcat | tee "$LOG_FILE"

echo ""
echo "======================================"
echo "   日志已保存到："
echo "   $LOG_FILE"
echo "======================================"
echo ""

# 提取关键信息
echo "=== 关键日志分析 ==="
echo ""

echo "【应用日志】"
cat "$LOG_FILE" | grep -E "VoiceRecognition|CameraViewModel|MainActivity" | grep -E "极简|测试|识别|命令"
echo ""

echo "【Google语音服务日志】"
cat "$LOG_FILE" | grep -E "NetworkSpeechRecognizer|SodaSpeechRecognizer|RecognitionService"
echo ""

echo "【错误日志】"
cat "$LOG_FILE" | grep -E "ERROR|FATAL" | grep -E "Speech|Recognition|Voice"
echo ""

echo "======================================"
echo "诊断完成！"
echo "完整日志已保存到："
echo "$LOG_FILE"
echo "======================================"
