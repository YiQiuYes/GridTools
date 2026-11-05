import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

var keystoreProperties = Properties()
var keystorePropertiesFile = rootProject.file("app/key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
} else {
    keystoreProperties.setProperty("storePassword", System.getenv("KEY_STORE_PASSWORD"))
    keystoreProperties.setProperty("keyPassword", System.getenv("KEY_PASSWORD"))
    keystoreProperties.setProperty("keyAlias", System.getenv("ALIAS"))
    keystoreProperties.setProperty("storeFile", "key.jks")
}

android {
    signingConfigs {
        getByName("debug") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            keyAlias = keystoreProperties["keyAlias"] as String
            storePassword = keystoreProperties["storePassword"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            keyAlias = keystoreProperties["keyAlias"] as String
            storePassword = keystoreProperties["storePassword"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
    namespace = "com.yiqiu.toolkit"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.yiqiu.toolkit"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs["release"]

            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }

        debug {
            signingConfig = signingConfigs["debug"]
        }
    }
    compileOptions {
        // Keep desugaring enabled for safety (Aalto/POI don't strictly need it at
        // minSdk 26, but it's harmless and future-proofs any Java 8 API usage).
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            // Compose Preview runfiles 需要编译后的 Android 资源
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Compose BOM: single source of versions for every Compose artifact.
    // MUST be repeated on androidTest: that configuration does NOT inherit
    // platform constraints from `implementation` (ui-test-junit4 then resolves
    // with no version). The IDE "declared multiple times" hint is a false
    // positive for BOMs spread across configurations.
    val composeBom = platform(libs.androidx.compose.bom)

    // Core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Compose + Material 3 Expressive
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)

    // Adaptive layouts: multi-pane scenes, window size classes, nav-suite
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)

    // Navigation 3 + serialization (saveable back stack)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.core)

    // Apache POI + Aalto STAX: fill the .docx template without altering format
    implementation(libs.poi.ooxml)
    implementation(libs.aalto.xml)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// AGP 9 hides android.lint from the script DSL (@HiddenInDefinition), so lint
// options are configured programmatically via finalizeDsl instead.
androidComponents {
    finalizeDsl { ext ->
        // Third-party dependency noise only; all project code stays warning-free:
        // - NewerVersionAvailable: POI is deliberately pinned (5.4.0 verified with
        //   the template fill + unit tests; 6.0.0 changes packaging/API).
        // - TrustAllX509TrustManager: an empty X509TrustManager inside the POI jar
        //   itself; we never make network calls through POI.
        ext.lint.disable += setOf("NewerVersionAvailable", "TrustAllX509TrustManager")
    }
}
