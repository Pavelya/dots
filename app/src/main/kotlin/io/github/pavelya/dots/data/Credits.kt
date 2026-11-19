package io.github.pavelya.dots.data

/**
 * What Credits in Settings lists: who made the game, every font, sound and music track the app
 * ships with, and the open-source libraries built into it, each with its licence. CreditsTest
 * checks this list against the app's files and dependencies, so nothing shipped is left out.
 * Every asset also has a row in docs/assets.md, the asset register.
 */
object Credits {
    /**
     * A licence, with its full text in the app's assets at [file], shown on the Licences page. Its
     * [title] fits one line of a heading (CreditsTest).
     */
    enum class Licence(val title: String, val file: String) {
        APACHE_2("Apache License 2.0", "licences/apache-2.0.txt"),
        OFL_1_1("SIL Open Font License 1.1", "licences/ofl-1.1.txt"),

        /**
         * Google's Play Core Software Development Kit Terms of Service, for its Play libraries such
         * as the in-app review prompt; not an open-source licence. The text starts with the full name.
         */
        PLAY_CORE_TERMS("Play Core SDK Terms", "licences/play-core-terms.txt"),
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
    const val MADE_BY = "School 25"

    /** The fonts the app ships with: the Notebook theme's Caveat and Nunito. */
    val fonts: List<Asset> = listOf(
        Asset(
            "Caveat",
            "The Caveat Project Authors, from Google Fonts",
            Licence.OFL_1_1,
            listOf("res/font/caveat_semibold.ttf", "res/font/caveat_bold.ttf"),
        ),
        Asset(
            "Nunito",
            "The Nunito Project Authors, from Google Fonts",
            Licence.OFL_1_1,
            listOf("res/font/nunito_medium.ttf", "res/font/nunito_bold.ttf", "res/font/nunito_extrabold.ttf"),
        ),
    )

    /**
     * The sound effects and music the app ships with. The effects come from
     * docs/design/sound/make_effects.py: three from the owner's Suno sounds, the rest made in code,
     * Star chart's and Neon arcade's board sounds among them. The music loops are cut from the
     * owner's Suno tracks by docs/design/sound/make_music.py.
     */
    val sounds: List<Asset> = listOf(
        Asset(
            "Pen and paper sounds",
            "$MADE_BY, with Suno",
            null,
            listOf("place_1", "place_2", "place_3", "ring_draw", "bomb").map(::effect),
        ),
        Asset(
            "Notes and chimes",
            "$MADE_BY, made in code",
            null,
            listOf(
                "ring_close", "captured", "capture_big", "threat", "dots_low", "boss_turn", "double_dot",
                "area_light", "win", "lose", "draw", "star", "world_open", "reward",
            ).map(::effect) + listOf("star_chart", "neon_arcade").flatMap(::themeEffects),
        ),
        Asset(
            "Music",
            "$MADE_BY, with Suno",
            null,
            listOf("map", "notebook", "garden", "night_sky", "sea", "boss").map(::music),
        ),
    )

    /** A sound effect's file in the app's assets: `sfx_win` is `assets/audio/sfx_win.ogg`. */
    private fun effect(name: String) = "assets/audio/sfx_$name.ogg"

    /** A theme's board sounds (sound.md, "What a theme replaces"): `star_chart` gives `assets/audio/sfx_star_chart_place_1.ogg` and the rest. */
    private fun themeEffects(theme: String) = listOf(
        "place_1", "place_2", "place_3", "ring_draw", "ring_close", "captured", "capture_big", "threat", "bomb",
        "double_dot", "area_light",
    ).map { effect("${theme}_$it") }

    /** A music loop's file in the app's assets: `map` is `assets/audio/music_map.ogg`. */
    private fun music(name: String) = "assets/audio/music_$name.ogg"

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
        Library("Google Play In-App Review", Licence.PLAY_CORE_TERMS, listOf("com.google.android.play")),
    )

    /** Every licence something in the app relies on, each once. */
    val licences: List<Licence>
        get() = ((fonts + sounds).mapNotNull { it.licence } + libraries.map { it.licence }).distinct()
}
