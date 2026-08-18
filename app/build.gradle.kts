plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.ksp)
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
        versionCode = 86
        versionName = "0.16.1" // 0.16.1·创意工坊「更新」：① 阅读器端到端打通划词标注(官方高亮/书签，书签蓝下划线区分+可写备注，跨设备同步) ② 插件广场 UGC 闭环(DIY 提交→admin 审核上架、安装/点赞计数) ③ 新增能力槽：主题槽(插件配色进主题选择器)、工具栏槽(底栏插件按钮)、侧栏弹层槽(open_sheet)。

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

    // 本地书架（Room）—— 用 KSP 替代 kapt，避免 kapt 在 Kotlin 2.0 下强制 1.9 回退吞掉真实编译错误
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
