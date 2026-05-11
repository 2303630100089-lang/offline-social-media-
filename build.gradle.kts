// Root build.gradle.kts

plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    kotlin("android") version "1.9.21" apply false
    kotlin("jvm") version "1.9.21" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
}

task("clean") {
    delete(rootProject.buildDir)
}

// Global version management
extraklass {
    "appVersion" {
        versionName = "0.1.0-alpha"
        versionCode = 1
        minSdk = 24  // Android 7.0
        compileSdk = 34 // Android 14
        targetSdk = 34
    }

    "deps" {
        // Core Android
        androidxCore = "1.12.0"
        androidxAppCompat = "1.6.1"
        androidxActivity = "1.8.1"
        androidxFragment = "1.6.2"
        
        // Jetpack Compose
        composeBom = "2024.01.00"
        composeCompiler = "1.5.8"
        
        // Room Database
        room = "2.6.1"
        
        // Hilt
        hilt = "2.48"
        
        // Networking
        okhttp = "4.11.0"
        retrofit = "2.9.0"
        
        // Security
        tink = "1.10.0"
        bcprov = "1.70"
        
        // Testing
        junit = "4.13.2"
        truth = "1.1.5"
        mockk = "1.13.8"
    }
}
