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
// Separate keys so each can be restricted in GCP to its own package name and signing certificate.
val placesApiKeyDebug = localProperties.getProperty("PLACES_API_KEY_DEBUG") ?: ""
val placesApiKeyRelease = localProperties.getProperty("PLACES_API_KEY_RELEASE") ?: ""

val mapsApiKey = localProperties.getProperty("MAPS_API_KEY") ?: ""

val checkPlacesApiKey = tasks.register("checkPlacesApiKey") {
    description = "Fails release builds when PLACES_API_KEY_RELEASE is missing from local.properties."
    val key = placesApiKeyRelease
    doLast {
        if (key.isBlank()) {
            throw GradleException("PLACES_API_KEY_RELEASE is missing from local.properties; release builds require it.")
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(checkPlacesApiKey) }

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.example.tiptracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jerrywang.tiptracker"
        minSdk = 28
        targetSdk = 36
        versionCode = 21
        versionName = "1.3.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["mapsApiKey"] = mapsApiKey
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        debug {
            // Lets debug builds install alongside the Play Store app instead of conflicting with it
            applicationIdSuffix = ".debug"
            buildConfigField("String", "PLACES_API_KEY", "\"$placesApiKeyDebug\"")
        }
        release {
            buildConfigField("String", "PLACES_API_KEY", "\"$placesApiKeyRelease\"")
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
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
    implementation(libs.maps.compose)
    implementation(libs.maps.compose.utils)
    implementation(libs.play.services.location)
}