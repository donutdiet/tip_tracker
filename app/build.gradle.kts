plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("org.jetbrains.kotlin.plugin.serialization")

//    id("com.google.dagger.hilt.android")
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
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        getByName("release") {
            // Enables code shrinking, obfuscation, and optimization for only
            // your project's release build type. Make sure to use a build
            // variant with `isDebuggable=false`.
            isMinifyEnabled = true

            // Enables resource shrinking, which is performed by the
            // Android Gradle plugin.
            isShrinkResources = true

            proguardFiles(
                // Includes the default ProGuard rules files that are packaged with
                // the Android Gradle plugin. To learn more, go to the section about
                // R8 configuration files.
                getDefaultProguardFile("proguard-android-optimize.txt"),

                // Includes a local, custom Proguard rules file
                "proguard-rules.pro"
            )

            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Design System
    implementation(libs.androidx.material3)

    // Android Studio Preview
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // UI Tests
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Integrate with activities
    implementation(libs.androidx.activity.compose)

    // Integrate with lifecycle dependencies
    implementation(libs.androidx.lifecycle.viewmodel.compose) // viewModel()
    implementation(libs.androidx.lifecycle.runtime.ktx) // viewModelScope.launch {}
    implementation(libs.androidx.lifecycle.runtime.compose) // collectAsStateWithLifecycle()

    // Room database
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Calendar dialog library
    // https://github.com/maxkeppeler/sheets-compose-dialogs
    implementation(libs.core)
    implementation(libs.calendar)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    // Core runtime for Jetpack Navigation 3 library — provides navigation components and APIs
    implementation(libs.androidx.navigation3.runtime)
    // UI components for Navigation 3 — includes NavDisplay etc.
    implementation(libs.androidx.navigation3.ui)
    // ViewModel integration with Navigation 3 — provides lifecycle-aware ViewModels scoped to navigation destinations
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.androidx.adaptive.navigation3)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.gson)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.foundation)

    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)

    // Hilt
    // implementation(libs.hilt.android)
    // kapt(libs.hilt.android.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.ui.tooling)
}