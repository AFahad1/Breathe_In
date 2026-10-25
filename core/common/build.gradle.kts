plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.breathein.core.common"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
