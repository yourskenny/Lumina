# 构建与验证任务说明

- 运行环境
  - Windows: 需配置 JAVA_HOME 指向 JDK 17
  - 项目根目录: d:\coding\Software_Innovation_Contest\Lumina

- 类型检查
  - 执行: `./gradlew.bat :app:compileDebugKotlin`

- Lint 检查
  - 执行: `./gradlew.bat :app:lint`

- 构建（调试包）
  - 执行: `./gradlew.bat assembleDebug`

- 构建（完整构建，跳过测试）
  - 执行: `./gradlew.bat build -x test`
