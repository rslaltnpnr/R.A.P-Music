import java.io.FileInputStream
import java.time.LocalDate
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// versionCode: a simple, monotonically increasing timestamp-derived scheme
// (days since 2024-01-01), documented in README.md.
val versionCodeValue: Int = run {
    val epochDay = LocalDate.now().toEpochDay()
    val baseDay = LocalDate.of(2024, 1, 1).toEpochDay()
    (epochDay - baseDay).toInt().coerceAtLeast(1)
}

// versionName is overridable via -PversionName=1.2.3 (used by the release CI,
// which derives it from the pushed git tag). Falls back to gradle.properties.
val versionNameValue: String =
    (project.findProperty("versionName") as String?) ?: "0.1.0"

// Single source of truth for the app's display name (also see gradle.properties#appName).
val appNameValue: String =
    (project.findProperty("appName") as String?) ?: "R.A.P Music"

android {
    namespace = "com.ozin.music"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ozin.music"
        minSdk = 26
        targetSdk = 35
        versionCode = versionCodeValue
        versionName = versionNameValue

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resValue("string", "app_name", appNameValue)
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            val ksPath = System.getenv("ANDROID_KEYSTORE_PATH")
            val ksPass = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            val alias = System.getenv("ANDROID_KEY_ALIAS")
            val keyPass = System.getenv("ANDROID_KEY_PASSWORD")
            if (!ksPath.isNullOrBlank() && !ksPass.isNullOrBlank() &&
                !alias.isNullOrBlank() && !keyPass.isNullOrBlank()
            ) {
                storeFile = file(ksPath)
                storePassword = ksPass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Falls back to debug signing automatically when release env vars are
            // absent (e.g. local dev / PR builds), because the "release" signing
            // config above only sets a storeFile when all four env vars are present.
            signingConfig = if (signingConfigs.getByName("release").storeFile != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.palette)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.navigation)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.common)
    implementation(libs.media3.datasource)

    implementation(libs.okhttp)
    implementation(libs.androidx.security.crypto)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.room.runtime)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(libs.androidx.test.ext.junit)
}
