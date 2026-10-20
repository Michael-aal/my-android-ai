plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.michael.myandroidai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.michael.myandroidai"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("safe") {
            dimension = "distribution"
            applicationIdSuffix = ".safe"
            versionNameSuffix = "-safe"
            buildConfigField("boolean", "ACCESSIBILITY_ASSISTANCE_ENABLED", "false")
        }
        create("fullControl") {
            dimension = "distribution"
            applicationIdSuffix = ".fullcontrol"
            versionNameSuffix = "-full-control"
            buildConfigField("boolean", "ACCESSIBILITY_ASSISTANCE_ENABLED", "true")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
