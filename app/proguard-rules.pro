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
