plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.breathein.core.settings"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core:common"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
