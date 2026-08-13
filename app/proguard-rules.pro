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
