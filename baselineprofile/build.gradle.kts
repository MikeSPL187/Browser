import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Generates app/src/main/baseline-prof.txt and measures cold start (stage S3b). Runs only from the
// "Baseline profile" workflow on an emulator, against the app's benchmark build type.
plugins {
    alias(libs.plugins.android.test)
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.sk2andy.materialbrowser.baselineprofile"
    compileSdk = 37
    compileSdkMinor = 1
    targetProjectPath = ":app"

    defaultConfig {
        minSdk = 31
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // CI measures on an emulator: numbers are a trend there, not a gate.
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("full") { dimension = "distribution" }
        create("systemwebview") { dimension = "distribution" }
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    experimentalProperties["android.experimental.self-instrumenting"] = true
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

androidComponents {
    beforeVariants(selector().all()) { variant ->
        variant.enable = variant.buildType == "benchmark" && variant.flavorName == "full"
    }
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
