package dots.cli

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The privacy policy (site/privacy/index.html) and the Data safety answers (docs/release.md,
 * "Privacy") name every library of the store build that sends data off the phone, and nothing
 * else (R-03). The libraries come from app/build.gradle.kts and gradle/libs.versions.toml, so a new
 * library fails this test until it is either known to stay on the phone or named in both places.
 */
class PrivacyPolicyTest {
    @Test
    fun `every library of the store build is on-device only or named in the policy and the Data safety answers`() {
        val root = AssetRegister.repositoryRoot()
        val libraries = storeLibraries(File(root, "app/build.gradle.kts").readText(), File(root, "gradle/libs.versions.toml").readText())
        val policy = File(root, POLICY).readText()
        val dataSafety = File(root, "docs/release.md").readText()
            .substringAfter("\n## Privacy\n", missingDelimiterValue = "")
            .substringBefore("\n## ")

        val problems = mutableListOf<String>()
        for (module in libraries) {
            val name = SENDS[module]
            when {
                name == null && ON_DEVICE.none { module.startsWith(it) } -> problems +=
                    "$module is in the store build, but the privacy policy doesn't name it. Find out what it sends (its " +
                    "Data safety page), describe it in $POLICY and docs/release.md (Privacy), then add it to SENDS here."
                name != null && name !in policy -> problems += "$POLICY doesn't name \"$name\" ($module)."
                name != null && name !in dataSafety -> problems += "docs/release.md, section Privacy, doesn't name \"$name\" ($module)."
            }
        }
        for ((module, name) in SENDS) {
            if (module !in libraries) {
                problems += "$module (\"$name\") is no longer in the store build: take it out of $POLICY, docs/release.md (Privacy) and SENDS."
            }
        }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun `the game's link to the policy is the page in site`() {
        val root = AssetRegister.repositoryRoot()
        val strings = File(root, "app/src/main/res/values/strings.xml").readText()
        val url = Regex("""<string name="privacy_policy_url"[^>]*>([^<]+)</string>""").find(strings)?.groupValues?.get(1)
        assertEquals("$SITE${POLICY.removePrefix("site/").removeSuffix("index.html")}", url)
        assertTrue(File(root, POLICY).isFile)
    }

    @Test
    fun `store libraries are the app's implementation and release dependencies, as Maven modules`() {
        val build = """
            dependencies {
                implementation(project(":engine"))
                implementation(platform(libs.compose.bom))
                implementation(libs.play.billing) // the Shop (C-02)
                releaseImplementation(libs.firebase_analytics)
                implementation("com.example:tracker:1.0")
                debugImplementation(libs.debug.only)
                testImplementation(libs.junit4)
            }
        """.trimIndent()
        val catalog = """
            [libraries]
            compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
            play-billing = { module = "com.android.billingclient:billing", version.ref = "play-billing" }
            firebase-analytics = { module = "com.google.firebase:firebase-analytics" }
            debug-only = { module = "com.example:debug" }
            junit4 = { module = "junit:junit", version.ref = "junit4" }
        """.trimIndent()
        assertEquals(
            setOf("com.android.billingclient:billing", "com.google.firebase:firebase-analytics", "com.example:tracker"),
            storeLibraries(build, catalog),
        )
    }

    private companion object {
        const val POLICY = "site/privacy/index.html"
        const val SITE = "https://pavelya.github.io/dots/"

        /** Libraries that send data off the phone, and the name the policy and the Data safety answers give each. */
        val SENDS = mapOf(
            "com.google.firebase:firebase-crashlytics" to "Firebase Crashlytics",
            "com.google.firebase:firebase-analytics" to "Google Analytics for Firebase",
            "com.google.android.gms:play-services-ads" to "Google AdMob",
            "com.google.android.ump:user-messaging-platform" to "Google's consent form",
            "com.android.billingclient:billing" to "Google Play Billing",
            "com.google.android.play:review" to "In-App Review",
        )

        /** Groups whose libraries send nothing off the phone: Android's own (Jetpack) and Kotlin's. */
        val ON_DEVICE = listOf("androidx.", "org.jetbrains.")

        /** Configurations that reach the store build. Debug and test ones never go to Google Play. */
        val STORE_CONFIGURATIONS = setOf("implementation", "api", "runtimeOnly", "releaseImplementation", "releaseApi", "releaseRuntimeOnly")

        /**
         * The Maven modules (`group:name`) of the store build's dependencies in [build], an
         * app/build.gradle.kts, with `libs.` aliases looked up in [catalog], a libs.versions.toml.
         * Other modules (`project(...)`) and BOMs (`platform(...)`) are left out: a BOM only sets
         * versions.
         */
        fun storeLibraries(build: String, catalog: String): Set<String> {
            val modules = Regex("""^([\w-]+)\s*=\s*\{\s*module\s*=\s*"([^":]+:[^":]+)"""", RegexOption.MULTILINE)
                .findAll(catalog)
                .associate { it.groupValues[1].replace('-', '.').replace('_', '.') to it.groupValues[2] }
            return Regex("""^\s*(\w+)\((.+?)\)\s*(//.*)?$""", RegexOption.MULTILINE).findAll(build)
                .filter { it.groupValues[1] in STORE_CONFIGURATIONS }
                .map { it.groupValues[2].trim() }
                .filterNot { it.startsWith("project(") || it.startsWith("platform(") }
                .map { dependency ->
                    if (dependency.startsWith("\"")) {
                        dependency.trim('"').split(':').take(2).joinToString(":")
                    } else {
                        val alias = dependency.removePrefix("libs.").replace('_', '.').replace('-', '.')
                        modules[alias] ?: error("app/build.gradle.kts uses $dependency, which gradle/libs.versions.toml doesn't list")
                    }
                }
                .toSet()
        }
    }
}
