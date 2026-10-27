import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.skie)
}

kotlin {
    android {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        namespace = "tuda.campus.mobile.composeApp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        androidResources.enable = true
    }


    listOf(
        //iosX64(),         // enable this if you're using an Intel Mac
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries {
            framework(listOf(DEBUG)) {
                baseName = "Shared"
                isStatic = false
                export(projects.shared)
            }
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.browser)
            implementation(libs.maps.compose)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3.alpha) // Needed for Material 3 Expressive
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.runtime.livedata)
            implementation(libs.qrcode.kotlin)
            implementation(libs.kotlinx.datetime)
            implementation(libs.coil.compose)
            implementation(libs.coil.svg)
            api(projects.shared)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}