plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace  = "com.ftt.signal"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ftt.signal"
        minSdk        = 26
        targetSdk     = 34
        versionCode   = 2
        versionName   = "7.0.0"
    }

    buildTypes {
        debug {
            isDebuggable    = true
            isMinifyEnabled = false
            // Use stable BuildConfig field instead of hardcoded strings
            buildConfigField("String", "API_BASE",     "\"https://asignal.umuhammadiswa.workers.dev\"")
            buildConfigField("String", "OTC_API_BASE", "\"https://asignal.umuhammadiswa.workers.dev\"")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_BASE",     "\"https://asignal.umuhammadiswa.workers.dev\"")
            buildConfigField("String", "OTC_API_BASE", "\"https://asignal.umuhammadiswa.workers.dev\"")
        }
    }

    buildFeatures {
        compose     = true
        buildConfig = true   // enable BuildConfig generation
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.activity.compose)
    implementation(libs.splashscreen)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)

    // Network
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Coroutines
    implementation(libs.coroutines.android)

    // Storage
    implementation(libs.datastore.prefs)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.ext.compiler)

    // WorkManager
    implementation(libs.work.runtime)

    debugImplementation(libs.compose.ui.tooling)
}
