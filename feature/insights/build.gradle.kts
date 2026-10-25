import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

// The Gemini key comes from the GEMINI_API_KEY environment variable (or, failing that, the gitignored
// local.properties), so it never lands in the repo. Demo only: a key in BuildConfig can still be pulled out
// of the APK; before release, call Gemini through a small backend proxy. Without a key, content is sorted
// by simple keyword matching.
val geminiApiKey: String = System.getenv("GEMINI_API_KEY")?.takeIf { it.isNotBlank() }
    ?: Properties().run {
        rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use(::load)
        getProperty("GEMINI_API_KEY").orEmpty()
    }

android {
    namespace = "com.breathein.feature.insights"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
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
    implementation(project(":core:common"))

    implementation(libs.google.genai)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
