import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val placesApiKey = localProperties.getProperty("PLACES_API_KEY") ?: ""

// Without a key the Places SDK is never initialized and restaurant search silently disappears,
// so refuse to build a release that would ship that way. Debug builds still work without one.
val checkPlacesApiKey by tasks.registering {
    val key = placesApiKey
    doLast {
        if (key.isBlank()) {
            throw GradleException("PLACES_API_KEY is missing from local.properties; release builds require it.")
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(checkPlacesApiKey) }

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    jvmToolchain(11)
}

android {
    namespace = "com.example.tiptracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jerrywang.tiptracker"
        minSdk = 28
        targetSdk = 36
        versionCode = 15
        versionName = "1.3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PLACES_API_KEY", "\"$placesApiKey\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
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
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui.text.google.fonts)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Material Design 3
    implementation(libs.androidx.material3)

    // Android Studio Preview support
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Room Database
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Datastore
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.preferences.core)

    // Graph
    implementation (libs.compose.charts)
    implementation(libs.reorderable)
    implementation(libs.coil.compose)

    // UI Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.ui.test.manifest)
    // MigrationTestHelper parses the exported schemas with Room's serialization classes, which need a
    // newer kotlinx.serialization than the 1.7.3 the app would otherwise resolve. Declared for every
    // build type (not debug-only) so debug, tests and release all run the same version.
    implementation(libs.kotlinx.serialization.json)

    // Integration with activities
    implementation(libs.androidx.activity.compose)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // Google Places SDK
    implementation(libs.google.places)
    implementation(libs.play.services.location)
}