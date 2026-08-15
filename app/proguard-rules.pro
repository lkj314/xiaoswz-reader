# 冲浪阅读 ProGuard 规则
# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.xiaoswz.reader.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.xiaoswz.reader.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# 后端 API DTO（同样用 kotlinx.serialization，release 混淆必须保留，否则 JSON 映射失败）
-keepclassmembers class com.xiaoswz.reader.data.api.** {
    *** Companion;
}
-keepclasseswithmembers class com.xiaoswz.reader.data.api.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# 保留整个 API 包（Retrofit 接口 + DTO）：@Path/@GET/@POST 等运行时注解与序列化器
# 绝不能剥掉，否则 release 包首个网络请求即崩。
-keep class com.xiaoswz.reader.data.api.** { *; }
-keep interface com.xiaoswz.reader.data.api.** { *; }

# Retrofit / OkHttp
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes Signature, Exceptions

# Coil（图片加载）— release 混淆必须保留，否则封面/图片在真机上静默加载失败
# （文字正常、图全空，正是当前"封面不显示"的症状）
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**
