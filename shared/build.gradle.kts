plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rambridge.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }
}
