plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.tiptracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jerrywang.tiptracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "1.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        jvmToolchain(11)
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.01.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Material Design 3
    implementation(libs.androidx.material3)

    // Android Studio Preview support
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // UI Tests
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Add window size utils
    implementation(libs.androidx.adaptive)

    // Integration with activities
    implementation(libs.androidx.activity.compose)
    // Integration with ViewModels
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Integration with LiveData
    implementation(libs.androidx.runtime.livedata)

}