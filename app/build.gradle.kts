plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // for compiling native source
        externalNativeBuild {
            cmake {
                cppFlags += " "
            }
        }

        // 限制 CPU 架构
        // 只编译 64 位 ARM 架构，缩减 APK 体减，加快编译速度
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    // 指定 CMakeLists.txt 的路径和 CMake 版本
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            // 请填入你查到的 CMake 版本号
            version = "3.22.1"
        }
    }

    // 防止打包时压缩 tflite/task 模型
    // TFLite/MediaPipe 也是通过内存映射直接读取模型的，如果被压缩，App 运行直接闪退
    androidResources {
        noCompress.add("tflite")
        noCompress.add("task") // MediaPipe 模型有时以后缀 .task 结尾
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.video)

    // Permissions
    implementation(libs.accompanist.permissions)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

//    // Network
//
//    // 1. TFLite 核心库 (使用 2.16.1，它是旧名称的最后一个稳定版本，修复了命名空间冲突)
//    implementation("org.tensorflow:tensorflow-lite:2.16.1")
//    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")
//    implementation("org.tensorflow:tensorflow-lite-gpu-api:2.16.1") // 显式引入 API 以防找不到类
//
//    // 2. TFLite Support 库
//    // 注意：Support 库 0.4.4 有 Bug，会自带一个冲突的 -api 库。
//    // 我们引入它，但在下面全局排除它的 api 模块。
//    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
//
//    // 3. 针对 ObjectDetector 的核心库 (你报错说找不到 ObjectDetector，是因为它在这个库里)
//    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
//    implementation("org.tensorflow:tensorflow-lite-task-base:0.4.4")

    // 添加 SnakeYAML 依赖
    implementation("org.yaml:snakeyaml:2.2")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ONNX Runtime
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")
}

//configurations.all {
//    // 只排除 support 库中那个重复的 api 模块
//    exclude(group = "org.tensorflow", module = "tensorflow-lite-support-api")
//
//    // 强制把所有传递依赖的 tflite 核心版本统一，防止 litert 自动跳出来
//    resolutionStrategy {
//        force("org.tensorflow:tensorflow-lite:2.16.1")
//        force("org.tensorflow:tensorflow-lite-api:2.16.1")
//    }
//}