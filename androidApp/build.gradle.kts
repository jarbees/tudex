import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3.alpha) // Needed for Material 3 Expressive

    implementation(projects.composeApp)
}

val localProperties = Properties()
val localPropertiesFile = File(rootDir, "local.properties")
if (localPropertiesFile.exists() && localPropertiesFile.isFile) {
    localPropertiesFile.inputStream().use {
        localProperties.load(it)
    }
}

android {
    namespace = "tuda.campus.mobile"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    androidResources {
        generateLocaleConfig = true
    }

    defaultConfig {
        applicationId = "tuda.campus.mobile"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 45
        versionName = "0.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        defaultConfig {
            manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("GOOGLE_MAPS_API_KEY") ?: ""
        }

        getByName("release") {
            isMinifyEnabled = false
            // Sometime in the future perhaps enable minification
//            isMinifyEnabled = true
//            isShrinkResources = true
//            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}