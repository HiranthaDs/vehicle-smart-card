plugins {
    id("com.android.application")
}

android {
    namespace = "com.roadping.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.roadping.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "2.0.1"

        buildConfigField("String", "SUPABASE_URL", "\"https://avixsqfvfonlioslhxif.supabase.co\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"sb_publishable_K2wL48ChCSml70JYwIqCfQ_FtyMbQBN\"")
        buildConfigField("String", "PUBLIC_APP_URL", "\"https://roadping-vehicle-card.hiirantha.chatgpt.site\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("com.google.zxing:core:3.5.3")
}
