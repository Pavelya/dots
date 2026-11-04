plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.pavelya.dots"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.pavelya.dots"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1-poc"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
    }
}

dependencies {
    implementation(project(":engine"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.datastore.preferences)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.coroutines.test)
}

roborazzi {
    // Reference images live in the repository; `recordRoborazziDebug` writes them,
    // `verifyRoborazziDebug` (run by CI) fails when the app draws something different.
    outputDir.set(file("src/test/screenshots"))
}

tasks.withType<Test>().configureEach {
    // Robolectric reaches into JDK internals for file descriptors; newer JDKs must allow it.
    jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED", "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
    // Robolectric downloads its Android runtime itself, outside Gradle. Where Maven Central is
    // rate-limited, ROBOLECTRIC_REPO_URL can point it at a mirror; unset (as on CI), it uses Central.
    System.getenv("ROBOLECTRIC_REPO_URL")?.let { systemProperty("robolectric.dependency.repo.url", it) }
}
