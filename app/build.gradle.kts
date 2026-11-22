plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val stableDebugStorePath = System.getenv("MYDAY_KEYSTORE_PATH")

android {
    namespace = "com.haskatech.myday"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.haskatech.myday"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "0.2.7"
    }

    // GitHub Actions supplies a persistent keystore for development APKs.
    // Local builds without these environment variables retain normal Android debug signing.
    if (!stableDebugStorePath.isNullOrBlank()) {
        val stableStorePassword = System.getenv("MYDAY_KEYSTORE_PASSWORD")
            ?: error("MYDAY_KEYSTORE_PASSWORD is required when MYDAY_KEYSTORE_PATH is set")
        val stableKeyAlias = System.getenv("MYDAY_KEY_ALIAS")
            ?: error("MYDAY_KEY_ALIAS is required when MYDAY_KEYSTORE_PATH is set")
        val stableKeyPassword = System.getenv("MYDAY_KEY_PASSWORD")
            ?: error("MYDAY_KEY_PASSWORD is required when MYDAY_KEYSTORE_PATH is set")

        signingConfigs.create("stableDebug") {
            storeFile = file(stableDebugStorePath)
            storePassword = stableStorePassword
            keyAlias = stableKeyAlias
            keyPassword = stableKeyPassword
        }
        buildTypes.getByName("debug") {
            signingConfig = signingConfigs.getByName("stableDebug")
        }
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
