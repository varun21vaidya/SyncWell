# Strip verbose logging from release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Room entities are read through generated code, but keep them safe for JSON/reflection paths
-keep class com.example.data.** { *; }

# Moshi codegen adapters and Retrofit/OkHttp optional platforms
-keepclasseswithmembers class * { @com.squareup.moshi.* <methods>; }
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
