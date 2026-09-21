import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import proguard.gradle.ProGuardTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}



kotlin {
    jvm()

    sourceSets {
        jvmMain {
            resources.srcDir("src/jvmMain/resources")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation("org.slf4j:slf4j-simple:2.0.13")
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.java)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenmodel)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)
            implementation(libs.koin.compose)
            // JCEF — Chromium Embedded Framework
            implementation("me.friwi:jcefmaven:146.0.10")
        }
    }
}

tasks.register("proguardJar", ProGuardTask::class) {
    val jarTask = tasks.findByName("jvmJar") ?: tasks.findByName("jar") ?: return@register
    dependsOn(jarTask)
    configuration("proguard.pro")
    injars(jarTask.outputs.files)
    outjars(layout.buildDirectory.file("libs/shrunk.jar"))

    doLast {
        val shrunk = layout.buildDirectory.file("libs/shrunk.jar").get().asFile
        if (shrunk.exists()) {
            val original = jarTask.outputs.files.singleFile
            shrunk.copyTo(original, overwrite = true)
            shrunk.delete()
        }
    }
}

compose.desktop {
    application {
        mainClass = "mexa.club.desktop_app.MainKt"

        jvmArgs(
            "-Djava.awt.headless=false",
            "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED",
            "--add-exports", "java.base/java.lang=ALL-UNNAMED",
            "--add-exports", "java.desktop/sun.awt=ALL-UNNAMED",
            "--add-exports", "java.desktop/sun.java2d=ALL-UNNAMED",
            "-Dprism.order=d3d,sw",
            "-Dprism.dirtyopts=false",
            "-Dprism.verbose=true",
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "mexa.club.desktop_app"
            packageVersion = "1.0.0"
        }
    }
}
