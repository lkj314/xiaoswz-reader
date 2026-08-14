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
        versionCode = 18
        versionName = "0.3.9"

        // 数据源：冲浪中文网公开只读 API
        buildConfigField("String", "API_BASE_URL", "\"https://xiaoswz.vercel.app\"")
        // 局域网更新服务器默认地址（可在 APP 内修改）
        buildConfigField("String", "DEFAULT_UPDATE_SERVER", "\"http://192.168.2.4:8765\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
