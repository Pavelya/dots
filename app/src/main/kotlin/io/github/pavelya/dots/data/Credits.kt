package io.github.pavelya.dots.data

/**
 * What Credits in Settings lists: who made the game, every font, sound and music track the app
 * ships with, and the open-source libraries built into it, each with its licence. CreditsTest
 * checks this list against the app's files and dependencies, so nothing shipped is left out.
 * Every asset also has a row in docs/assets.md, the asset register.
 */
object Credits {
    /** A licence, with its full text in the app's assets at [file], shown on the Licences page. */
    enum class Licence(val title: String, val file: String) {
        APACHE_2("Apache License 2.0", "licences/apache-2.0.txt"),
    }

    /**
     * A font, sound or music track: its [name], who made it or where it came from ([source]), its
     * [licence] (null when it is our own work), and the app's [files] it covers, as paths from
     * `app/src/main` (`res/font/caveat.ttf`, `assets/audio/sfx_pen_tap.ogg`).
     */
    data class Asset(val name: String, val source: String, val licence: Licence?, val files: List<String>)

    /** A library built into the app; [groups] are its Maven groups, checked against the app's dependencies. */
    data class Library(val name: String, val licence: Licence, val groups: List<String>)

    /** The name under Made by. */
    const val MADE_BY = "Pavelya"

    /** The fonts the app ships with. Caveat and Nunito arrive with the Notebook theme (V-03). */
    val fonts: List<Asset> = emptyList()

    /** The sound effects and music the app ships with (V-08, V-09). */
    val sounds: List<Asset> = emptyList()

    val libraries: List<Library> = listOf(
        Library("Kotlin", Licence.APACHE_2, listOf("org.jetbrains.kotlin")),
        Library("Kotlin coroutines", Licence.APACHE_2, listOf("org.jetbrains.kotlinx")),
        Library(
            "Jetpack Compose",
            Licence.APACHE_2,
            listOf("androidx.compose.ui", "androidx.compose.foundation", "androidx.compose.runtime", "androidx.compose.animation"),
        ),
        Library("Material 3 for Compose", Licence.APACHE_2, listOf("androidx.compose.material3")),
        Library("AndroidX Core", Licence.APACHE_2, listOf("androidx.core")),
        Library("AndroidX Activity", Licence.APACHE_2, listOf("androidx.activity")),
        Library("AndroidX Lifecycle", Licence.APACHE_2, listOf("androidx.lifecycle")),
        Library("AndroidX Navigation", Licence.APACHE_2, listOf("androidx.navigation")),
        Library("AndroidX DataStore", Licence.APACHE_2, listOf("androidx.datastore")),
    )

    /** Every licence something in the app relies on, each once. */
    val licences: List<Licence>
        get() = ((fonts + sounds).mapNotNull { it.licence } + libraries.map { it.licence }).distinct()
}
