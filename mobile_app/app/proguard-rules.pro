# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Compose
-dontwarn androidx.compose.**

# Data models (Gson serialization)
-keep class com.example.mobile_app.data.model.** { *; }
-keep class com.example.mobile_app.data.model.cart.** { *; }
-keep class com.example.mobile_app.data.model.order.** { *; }
-keep class com.example.mobile_app.data.model.wishlist.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Coil
-dontwarn coil3.**
-keep class coil3.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**

# Material Icons - faqat ishlatilganlarni saqlash
-keep class androidx.compose.material.icons.** { *; }

# Past darajadagi qurilmalar uchun qo'shimcha optimizatsiyalar
-repackageclasses ''
-allowaccessmodification
-optimizationpasses 5
-mergeinterfacesaggressively
-optimizations !code/simplification/variable,!code/simplification/!field/*,!class/merging/*
