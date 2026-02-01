# Windows 环境验证脚本
# PowerShell 脚本

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "   Android 开发环境验证工具" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

$allPassed = $true

# 函数定义
function Test-Command {
    param($CommandName)
    $command = Get-Command $CommandName -ErrorAction SilentlyContinue
    return $null -ne $command
}

function Write-Success {
    param($Message)
    Write-Host "✅ $Message" -ForegroundColor Green
}

function Write-Failure {
    param($Message)
    Write-Host "❌ $Message" -ForegroundColor Red
    $script:allPassed = $false
}

function Write-Info {
    param($Message)
    Write-Host "ℹ️  $Message" -ForegroundColor Cyan
}

# 1. 检查 Git
Write-Host "1. 检查 Git..." -ForegroundColor Yellow
if (Test-Command "git") {
    $gitVersion = git --version
    Write-Success "Git 已安装: $gitVersion"
} else {
    Write-Failure "Git 未安装"
    Write-Info "请从 https://git-scm.com/download/win 下载安装"
}
Write-Host ""

# 2. 检查 Java
Write-Host "2. 检查 Java..." -ForegroundColor Yellow
if (Test-Command "java") {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Success "Java 已安装: $javaVersion"
} else {
    Write-Failure "Java 未找到"
    Write-Info "Android Studio 会自动安装 JDK"
}
Write-Host ""

# 3. 检查 Android SDK
Write-Host "3. 检查 Android SDK..." -ForegroundColor Yellow
$sdkPath = "$env:LOCALAPPDATA\Android\Sdk"
if (Test-Path $sdkPath) {
    Write-Success "Android SDK 已安装: $sdkPath"

    # 检查 platform-tools
    $adbPath = "$sdkPath\platform-tools\adb.exe"
    if (Test-Path $adbPath) {
        Write-Success "ADB 已安装"

        # 测试 adb
        $adbVersion = & $adbPath version 2>&1 | Select-String "Android Debug Bridge"
        Write-Info "ADB 版本: $adbVersion"
    } else {
        Write-Failure "ADB 未找到"
    }
} else {
    Write-Failure "Android SDK 未找到"
    Write-Info "请先安装 Android Studio"
}
Write-Host ""

# 4. 检查项目文件
Write-Host "4. 检查项目文件..." -ForegroundColor Yellow
if (Test-Path "build.gradle") {
    Write-Success "找到 build.gradle"
} else {
    Write-Failure "未找到 build.gradle，请确认在项目根目录"
}

if (Test-Path "app\build.gradle") {
    Write-Success "找到 app\build.gradle"
} else {
    Write-Failure "未找到 app\build.gradle"
}

if (Test-Path "settings.gradle") {
    Write-Success "找到 settings.gradle"
} else {
    Write-Failure "未找到 settings.gradle"
}
Write-Host ""

# 5. 检查 Git 分支
Write-Host "5. 检查 Git 分支..." -ForegroundColor Yellow
$branch = git branch --show-current
if ($branch -eq "Androidd") {
    Write-Success "当前分支: $branch"
} else {
    Write-Failure "当前分支不是 Androidd，是: $branch"
    Write-Info "运行: git checkout Androidd"
}
Write-Host ""

# 6. 检查 ADB 连接
Write-Host "6. 检查 ADB 设备..." -ForegroundColor Yellow
if (Test-Path $adbPath) {
    $devices = & $adbPath devices | Select-String "device$"
    if ($devices) {
        Write-Success "找到设备:"
        & $adbPath devices
    } else {
        Write-Failure "未找到设备"
        Write-Info "请连接 Android 设备或启动模拟器"
        Write-Info "使用: adb devices 查看设备"
    }
} else {
    Write-Failure "ADB 未安装，无法检查设备"
}
Write-Host ""

# 7. 检查 Gradle Wrapper
Write-Host "7. 检查 Gradle Wrapper..." -ForegroundColor Yellow
if (Test-Path "gradlew.bat") {
    Write-Success "找到 gradlew.bat"

    Write-Info "测试 Gradle 版本..."
    $gradleVersion = .\gradlew.bat --version 2>&1 | Select-String "Gradle"
    if ($gradleVersion) {
        Write-Success "Gradle 版本: $gradleVersion"
    }
} else {
    Write-Failure "未找到 gradlew.bat"
}
Write-Host ""

# 8. 测试构建（可选）
Write-Host "8. 测试构建（可选，需要时间）..." -ForegroundColor Yellow
$testBuild = Read-Host "是否测试构建项目？这需要几分钟时间 (y/n)"
if ($testBuild -eq "y" -or $testBuild -eq "Y") {
    Write-Info "开始构建..."
    .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -eq 0) {
        Write-Success "构建成功！"
    } else {
        Write-Failure "构建失败"
        Write-Info "查看错误信息或运行: gradlew.bat assembleDebug --stacktrace"
    }
} else {
    Write-Info "跳过构建测试"
}
Write-Host ""

# 总结
Write-Host "======================================" -ForegroundColor Cyan
if ($allPassed) {
    Write-Host "   ✅ 所有检查通过！" -ForegroundColor Green
    Write-Host "   环境配置完成，可以开始开发" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  部分检查未通过" -ForegroundColor Yellow
    Write-Host "   请查看上面的错误信息并修复" -ForegroundColor Yellow
}
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# 下一步建议
Write-Host "📱 下一步:" -ForegroundColor Cyan
Write-Host "1. 在 Android Studio 中打开项目"
Write-Host "2. 等待 Gradle Sync 完成"
Write-Host "3. 连接 Android 设备或启动模拟器"
Write-Host "4. 点击绿色三角形 ▶️ 运行应用"
Write-Host "5. 测试快捷命令按钮功能"
Write-Host ""
Write-Host "📖 查看 WINDOWS_SETUP.md 获取详细说明" -ForegroundColor Cyan
