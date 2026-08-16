plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
}

// 本机 safe-delete 在回收站不可用时阻断一切"删除"操作，导致 Gradle 增量构建
// 无法清理旧产物（dexBuilder/packageDebug 均报拒绝访问）。
// 规避办法：每次构建写入独立全新输出目录（只新建、不删除），绕开删除拦截。
val buildSeqFile = rootDir.resolve(".build_seq")
val buildSeq = runCatching { buildSeqFile.readText().trim().toIntOrNull() ?: 0 }.getOrDefault(0) + 1
buildSeqFile.writeText(buildSeq.toString())
layout.buildDirectory.set(rootDir.resolve("builds/app_$buildSeq"))

android {
    namespace = "com.xiaoswz.reader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xiaoswz.reader"
        minSdk = 26
        targetSdk = 35
        versionCode = 57
        versionName = "0.8.2" // 0.8.2·书城拆首页+书库：书城拆为「首页」（Hero+热度轮播+浏览书库入口，简洁）与「书库」（搜索+分区浏览+状态/排序+热榜+月票榜+最新上架网格）；底部导航不新增书库 tab，书库仅由首页入口进入。后端新增 GET /api/catalog/categories 支撑分区浏览。独立后端、主站单源、物理隔离不变

        // 数据源：冲浪中文网公开只读 API
        buildConfigField("String", "API_BASE_URL", "\"https://xiaoswz.vercel.app\"")
        // 冲浪阅读专属后端（独立项目 + 独立 Neon 数据库 chonglang，与主站 xiaoswz 物理隔离）。
        // 0.6.6 起：发布版默认指向云端 Vercel 独立后端（读者可达，与局域网开发后端解耦）。
        buildConfigField("String", "BACKEND_BASE_URL", "\"https://xiaoswz-reader-backend.vercel.app\"")
        // 应用更新服务器：云端走 GitHub raw（绕开 Vercel 域名污染）；局域网地址仍可手填兜底
        buildConfigField("String", "DEFAULT_UPDATE_SERVER", "\"https://raw.githubusercontent.com/lkj314/xiaoswz-reader/main/lan-update\"")
    }

    // 测试构建：release 复用 AGP 默认 debug 密钥库（~/.android/debug.keystore）签名，
    // 确保局域网分发的 APK 可安装。⚠️ 正式发布前请替换为专属发布密钥库。
    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 测试期用调试密钥库签名，保证可分发包可安装（生产需换正式密钥）
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    // M2 material 仅用于 androidx.compose.material.pullrefresh（下拉刷新），主题仍由 M3 提供
    implementation("androidx.compose.material:material")
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // 网络层
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // 图片加载
    implementation(libs.coil.compose)

    // 设置持久化
    implementation(libs.androidx.datastore.preferences)

    // 本地书架（Room）
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
}
