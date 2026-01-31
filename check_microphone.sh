#!/bin/bash

echo "======================================"
echo "   麦克风配置自动诊断工具"
echo "======================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查函数
check() {
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ $1${NC}"
        return 0
    else
        echo -e "${RED}❌ $1${NC}"
        return 1
    fi
}

warn() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

info() {
    echo -e "ℹ️  $1"
}

# 1. 检查 ADB 连接
echo "📱 1. 检查设备连接..."
adb devices | grep -q "emulator\|device"
if check "设备已连接"; then
    adb devices | grep -v "List"
else
    echo ""
    echo "请先启动 Android 模拟器或连接真实设备"
    exit 1
fi
echo ""

# 2. 检查应用是否安装
echo "📦 2. 检查应用安装..."
adb shell pm list packages | grep -q "com.example.myapplication"
check "应用已安装"
echo ""

# 3. 检查应用权限
echo "🔐 3. 检查应用权限..."
RECORD_AUDIO=$(adb shell dumpsys package com.example.myapplication | grep "android.permission.RECORD_AUDIO: granted=" | grep -o "granted=[a-z]*")
CAMERA=$(adb shell dumpsys package com.example.myapplication | grep "android.permission.CAMERA: granted=" | grep -o "granted=[a-z]*")

if [[ $RECORD_AUDIO == *"true"* ]]; then
    echo -e "${GREEN}✅ RECORD_AUDIO 权限已授予${NC}"
else
    echo -e "${RED}❌ RECORD_AUDIO 权限未授予${NC}"
    warn "正在尝试授予权限..."
    adb shell pm grant com.example.myapplication android.permission.RECORD_AUDIO
    sleep 1
    echo -e "${GREEN}✅ 权限已授予${NC}"
fi

if [[ $CAMERA == *"true"* ]]; then
    echo -e "${GREEN}✅ CAMERA 权限已授予${NC}"
else
    echo -e "${RED}❌ CAMERA 权限未授予${NC}"
    warn "正在尝试授予权限..."
    adb shell pm grant com.example.myapplication android.permission.CAMERA
    sleep 1
    echo -e "${GREEN}✅ 权限已授予${NC}"
fi
echo ""

# 4. 检查 macOS 系统麦克风权限
echo "🎤 4. 检查 macOS 系统麦克风权限..."
info "请确保以下应用已获得麦克风权限："
info "  系统设置 > 隐私与安全性 > 麦克风"
info "  - Android Emulator"
info "  - qemu-system-aarch64"
echo ""

# 5. 检查模拟器进程
echo "🖥️  5. 检查模拟器进程..."
EMU_PROCESS=$(ps aux | grep "[q]emu-system" | wc -l)
if [ $EMU_PROCESS -gt 0 ]; then
    echo -e "${GREEN}✅ 模拟器进程运行中${NC}"
    ps aux | grep "[q]emu-system" | head -1 | awk '{print "   进程: " $11}'
else
    warn "未找到模拟器进程"
fi
echo ""

# 6. 测试麦克风
echo "🔊 6. 准备麦克风测试..."
info "即将启动应用并开始麦克风测试"
info "请准备对着麦克风说话"
echo ""
echo "按 Enter 键继续..."
read

# 启动应用
echo "启动应用..."
adb shell am force-stop com.example.myapplication
sleep 1
adb shell am start -n com.example.myapplication/.MainActivity
sleep 3

# 开始监控日志
echo ""
echo "======================================"
echo "   开始实时日志监控"
echo "   请在应用中点击【测试麦克风】按钮"
echo "   然后对着麦克风说话"
echo "   按 Ctrl+C 停止监控"
echo "======================================"
echo ""

# 监控日志
adb logcat -c
adb logcat -s MicrophoneTest MainActivity VoiceRecognitionService | grep --line-buffered -E "音量|AudioRecord|检测到|麦克风|测试|识别|错误" | while read line; do
    if echo "$line" | grep -q "成功"; then
        echo -e "${GREEN}$line${NC}"
    elif echo "$line" | grep -q "错误\|失败\|ERROR"; then
        echo -e "${RED}$line${NC}"
    elif echo "$line" | grep -q "警告\|WARN"; then
        echo -e "${YELLOW}$line${NC}"
    else
        echo "$line"
    fi
done
