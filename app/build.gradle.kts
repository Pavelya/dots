import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
    // Crash reports (decision 0032). google-services reads google-services.json, the Firebase
    // project's public settings; the Crashlytics plugin sends R8's mapping file to Firebase.
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

/*
 * The game's app in AdMob and its rewarded ad unit, `Booster` (docs/release.md, Ads and "What is
 * set up"). Not secrets: every copy of the app holds them. Store builds show real ads from the unit.
 */
val ADMOB_APP_ID = "ca-app-pub-3639439691153326~3889232280"
val REWARDED_AD_UNIT = "ca-app-pub-3639439691153326/6766356268"

/** Google's rewarded test ad unit, for every debug build. */
val TEST_REWARDED_AD_UNIT = "ca-app-pub-3940256099942544/5224354917"

android {
    namespace = "io.github.pavelya.dots"
    // Android 37.1: Compose 1.13 needs it to build. Phones still run the app as Android 37 (targetSdk).
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }

    defaultConfig {
        // The store app's ID (decision 0016); it can never change once the app is on Google Play.
        // The code's package, `namespace` above, stays io.github.pavelya.dots: players never see it.
        applicationId = "io.github.pavelya.pinfold"
        minSdk = 24
        targetSdk = 37
        // CI sets these. Every CI build and POC test build: 100 + the number of commits, so a newer
        // build installs over an older one. Store builds on a v* tag: from the tag (decision 0031).
        versionCode = providers.environmentVariable("VERSION_CODE").map(String::toInt).getOrElse(1)
        versionName = providers.environmentVariable("VERSION_NAME").getOrElse("0.1-poc")
        // The game's app in AdMob (docs/release.md, AdMob). Every build uses it, so Google's consent
        // form (C-03) shows on test builds too; it is not a secret, as every copy of the app holds it.
        manifestPlaceholders["admobAppId"] = ADMOB_APP_ID
    }

    // The POC test key signs the debug builds testers install, so a new test build installs over
    // the previous one. CI writes it from the Actions secrets POC_KEYSTORE_BASE64 and
    // POC_KEYSTORE_PASSWORD and points POC_KEYSTORE_FILE at it. Without it (a local build, or a
    // Dependabot pull request, which gets no secrets) debug builds use the standard Android debug key.
    val pocKeystoreFile = providers.environmentVariable("POC_KEYSTORE_FILE").orNull
    // The upload key signs the store app's release bundle, which Google Play then re-signs with the
    // app signing key it keeps (docs/release.md). The release workflow writes it from the Actions
    // secrets UPLOAD_KEYSTORE_BASE64 and UPLOAD_KEYSTORE_PASSWORD and points UPLOAD_KEYSTORE_FILE at
    // it. Without it the release bundle is built unsigned, which Google Play refuses.
    val uploadKeystoreFile = providers.environmentVariable("UPLOAD_KEYSTORE_FILE").orNull

    signingConfigs {
        if (pocKeystoreFile != null) {
            val password = providers.environmentVariable("POC_KEYSTORE_PASSWORD").orNull
                ?: error("POC_KEYSTORE_FILE is set but POC_KEYSTORE_PASSWORD is not")
            create("pocTest") {
                storeFile = file(pocKeystoreFile)
                storePassword = password
                keyAlias = "poc-test"
                keyPassword = password
            }
        }
        if (uploadKeystoreFile != null) {
            val password = providers.environmentVariable("UPLOAD_KEYSTORE_PASSWORD").orNull
                ?: error("UPLOAD_KEYSTORE_FILE is set but UPLOAD_KEYSTORE_PASSWORD is not")
            create("upload") {
                storeFile = file(uploadKeystoreFile)
                storePassword = password
                keyAlias = "upload"
                keyPassword = password
            }
        }
    }

    buildTypes {
        debug {
            signingConfigs.findByName("pocTest")?.let { signingConfig = it }
            // Google's test ad: debug builds never show a real ad, so testers can't earn or cost money.
            buildConfigField("String", "REWARDED_AD_UNIT", "\"$TEST_REWARDED_AD_UNIT\"")
        }
        release {
            // R8 removes unused code and renames what is left; resource shrinking then drops unused
            // resources. The bundle carries R8's mapping file, so Google Play shows crash reports
            // with the real names.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfigs.findByName("upload")?.let { signingConfig = it }
            buildConfigField("String", "REWARDED_AD_UNIT", "\"$REWARDED_AD_UNIT\"")
            // Crashlytics needs the same mapping file. Only the release workflow sends it, for a
            // tagged build (CRASHLYTICS_MAPPING_UPLOAD=true); pull requests and dry runs don't.
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled =
                    providers.environmentVariable("CRASHLYTICS_MAPPING_UPLOAD").orNull == "true"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true // the version name goes into the game log
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        // "A newer version is available" checks look at what has been released upstream, not at
        // our code, so any new release would fail the build with warningsAsErrors. Dependabot
        // (.github/dependabot.yml) proposes updates instead.
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
    }
}

dependencies {
    implementation(project(":engine"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.core) // FileProvider, to share the game log
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.play.review) // Google Play's in-app review prompt
    implementation(libs.play.billing) // Google Play Billing: the Shop's purchases (C-02)
    implementation(libs.play.services.ads) // AdMob: the rewarded ads (C-03)
    implementation(libs.ump) // Google's consent form for ads in the EU and the UK (C-03)
    // The ads SDK brings WorkManager 2.7.0, whose Room 2.2 lets R8 remove the constructor that
    // Room calls by reflection: v0.2.1 crashed on start (Q-09). A current WorkManager brings a Room
    // whose rules keep it. The release build's check-release-dex step watches for it.
    implementation(libs.work.runtime)
    // Crash reports (decision 0032) and analytics (decision 0033) from store builds only. Debug
    // builds, which testers of GitHub builds and the tests use, don't contain Firebase and send
    // nothing; src/debug and src/release each give the game its `buildAnalytics`.
    releaseImplementation(platform(libs.firebase.bom))
    releaseImplementation(libs.firebase.crashlytics)
    releaseImplementation(libs.firebase.analytics)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit4)
    testImplementation(libs.espresso.core) // presses Back in the navigation test, also on dialogs
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
