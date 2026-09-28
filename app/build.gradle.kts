// SPDX-License-Identifier: AGPL-3.0-only

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.inok546.headphoneactions"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.inok546.headphoneactions"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "0.7.0"
    }

    // The release key lives outside the repository. Its location and passwords come from
    // headphoneActions.* properties, normally in ~/.gradle/gradle.properties; without them
    // the release APK is built unsigned.
    val releaseStoreFile = providers.gradleProperty("headphoneActions.releaseStoreFile").orNull
    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = providers.gradleProperty("headphoneActions.releaseStorePassword").get()
                keyAlias = providers.gradleProperty("headphoneActions.releaseKeyAlias").get()
                keyPassword = providers.gradleProperty("headphoneActions.releaseKeyPassword").get()
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Lets JVM unit tests run code that logs through android.util.Log.
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
}
