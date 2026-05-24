plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tuusuario.votoelectronico"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tuusuario.votoelectronico"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    // Android Base y Compose
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // UI - Compose Material 3
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.ui:ui-graphics:1.5.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")

    // ── NUEVAS: Iconos extendidos y Animaciones ──────────────────────────────
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("androidx.compose.animation:animation:1.5.4")
    // ────────────────────────────────────────────────────────────────────────

    // Biometría, DNI, Red
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // CameraX
    val camerax_version = "1.3.0"
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    // ML Kit Detección Facial
    implementation("com.google.mlkit:face-detection:16.1.6")

    // Coil (imágenes)
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.compose.foundation:foundation:1.5.4")
}