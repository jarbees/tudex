import org.jetbrains.kotlin.gradle.dsl.JvmTarget

repositories {
    mavenCentral()
    google()
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.skie)
}

kotlin {
    android {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        namespace = "tuda.campus.mobile.shared"
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
            framework(listOf(RELEASE)) {
                baseName = "Shared"
                isStatic = false

                linkerOpts += arrayOf(
                    "-Wl,-S",           // Strip debug symbols
                    "-Wl,-dead_strip"   // Remove unused code/data
                )
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            // put your Multiplatform dependencies here
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.persistent.cache)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ksoup)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.kvault)
            implementation(libs.korlibs.image.core)
            implementation(libs.korlibs.image)
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.filekit.core)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.android)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}