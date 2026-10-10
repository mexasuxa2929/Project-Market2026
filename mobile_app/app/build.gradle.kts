import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val apiBaseUrl: String = (project.findProperty("API_BASE_URL") as? String)
    ?: localProperties.getProperty("API_BASE_URL")
    ?: "http://10.0.2.2:8080/"

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val hasReleaseSigning = keystoreProperties.getProperty("storePassword") != null &&
        keystoreProperties.getProperty("keyPassword") != null

android {
    namespace = "com.example.mobile_app"
    compileSdk = 35

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile", "release-key.jks"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        // M-57: Maps API kaliti tracked fayldan olib tashlandi —
        // -PMAPS_API_KEY=... yoki local.properties (gitignore) orqali.
        manifestPlaceholders["MAPS_API_KEY"] = (project.findProperty("MAPS_API_KEY") as? String)
            ?: runCatching {
                rootProject.file("local.properties").takeIf { it.exists() }?.readLines()
                    ?.firstOrNull { it.trim().startsWith("MAPS_API_KEY=") }
                    ?.substringAfter("=")?.trim()
            }.getOrNull() ?: ""
        applicationId = "com.example.mobile_app"
        minSdk = 21
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // M-56: debug kalitda imzolangan release yasash taqiqlandi.
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.lifecycle.compose.ExperimentalLifecycleComposeApi",
            // Compose Compiler optimizatsiyalari — past darajadagi qurilmalar uchun
            "-P=plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
                "${project.buildDir}/compose_compiler"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Katta hajmdagi fayllarni chiqarib tashlash — APK hajmini kamaytirish
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }
}


// M-56: release uchun https majburiy + keystore majburiy.
// Konfiguratsiya bosqichida emas, faqat release task bajarilganda (debug build buzilmasligi uchun).
tasks.configureEach {
    if (name == "assembleRelease" || name == "bundleRelease") {
        doFirst {
            if (!hasReleaseSigning) {
                throw GradleException(
                    "M-56: release build uchun keystore.properties (storePassword/keyPassword) majburiy. " +
                        "Debug kalit bilan release yasash taqiqlangan."
                )
            }
            if (!apiBaseUrl.startsWith("https://")) {
                throw GradleException(
                    "M-56: release uchun API_BASE_URL https:// bo'lishi shart (hozir: $apiBaseUrl)."
                )
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.osmdroid.android)
    implementation(libs.google.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.identity.googleid)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
