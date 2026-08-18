plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// 强制使用 Java 17（Kotlin 1.9 不支持 Java 25）
kotlin {
    jvmToolchain(17)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

android {
    namespace = "com.mynote.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mynote.android"
        minSdk = 21
        targetSdk = 34
        versionCode = 55
        versionName = "5.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        viewBinding = true
        compose = true
    }
}

dependencies {
    // AndroidX 核心
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")

    // Material Design 3
    implementation("com.google.android.material:material:1.12.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Room 数据库（2.7.2：修复 KSP2 的 suspend DAO "jvm signature V" 崩溃，且 minSdk 仍是 21；2.8.x 已把 minSdk 提到 23，故不用）
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // 网络请求(百度网盘备份用)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON 解析
    implementation("com.google.code.gson:gson:2.11.0")

    // ML Kit 中文文字识别（离线模型，已内置于 APK）
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")

    // 指纹识别
    implementation("androidx.biometric:biometric:1.1.0")

    // 文件选择 SAF
    implementation("androidx.documentfile:documentfile:1.0.1")

    // ── Jetpack Compose（为 Liquid Glass / backdrop 玻璃效果引入） ──
    val composeBom = platform("androidx.compose:compose-bom:2025.07.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Liquid Glass 玻璃效果（Kyant0/backdrop，纯 Android Jetpack Compose 版）
    implementation("io.github.kyant0:backdrop:1.0.0")

    // Haze 玻璃模糊（对话框玻璃材质：hazeSource 标记背景内容 + hazeEffect 跨窗口模糊）
    implementation("dev.chrisbanes.haze:haze:1.6.10")
    // HazeMaterials 预置材质（regular()/ultraThin() 等）在独立 artifact 里，不在核心包
    implementation("dev.chrisbanes.haze:haze-materials:1.6.10")
}
