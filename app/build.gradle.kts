plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.medieval.castledefense"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.medieval.castledefense"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            // Eigener Release-Key für direkte Distribution (GitHub Releases).
            // Für eine Play-Store-Veröffentlichung eigenen, geheimen Key verwenden!
            storeFile = rootProject.file("keystore/release.keystore")
            storePassword = "mcd-release-2026"
            keyAlias = "mcd_release"
            keyPassword = "mcd-release-2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
