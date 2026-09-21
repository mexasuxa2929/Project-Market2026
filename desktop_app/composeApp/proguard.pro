-dontwarn

# Keep Compose runtime
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.compose.** { *; }

# Keep JCEF
-keep class org.cef.** { *; }

# Keep Ktor (service loader)
-keep class io.ktor.** { *; }
-keep class ch.qos.logback.** { *; }

# Keep kotlinx.serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepclassmembers enum * { *; }

# Keep Koin
-keep class org.koin.** { *; }

# Keep Voyager
-keep class cafe.adriel.voyager.** { *; }

# Keep SLF4J
-keep class org.slf4j.** { *; }

# Keep app classes
-keep class mexa.club.** { *; }
