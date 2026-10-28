plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tuvin.pixelautomations.wear"
    compileSdk = 36

    defaultConfig {
        // Same application ID (and debug signing key) as the phone app: the Wear OS Data Layer only
        // delivers messages between apps that match.
        applicationId = "com.tuvin.pixelautomations"
        minSdk = 30 // Wear OS 3+
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.health:health-services-client:1.0.0") // workout start/end on the watch
    implementation("com.google.android.gms:play-services-wearable:18.2.0") // send to the phone
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("com.google.guava:guava:33.3.1-android") // ListenableFuture, which Health Services returns

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
