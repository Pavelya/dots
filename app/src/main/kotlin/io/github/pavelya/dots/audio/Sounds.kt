package io.github.pavelya.dots.audio

/** The volume of a sound heard often (docs/design/sound.md, "Volume": half). */
private const val HALF = 0.5f

/** The volume of every other sound ("full"). */
private const val FULL = 1f

/**
 * Every sound effect of docs/design/sound.md, as a file in `assets/audio/` ([file]). [share] is
 * its volume, from the page's "Volume" column; [millis] is how long it lasts. While an effect that
 * [ducksMusic] plays, the music drops to half its volume (sound.md, "Music").
 */
enum class Effect(val file: String, val share: Float, val millis: Long, val ducksMusic: Boolean = false) {
    PLACE_1("sfx_place_1", HALF, 150),
    PLACE_2("sfx_place_2", HALF, 150),
    PLACE_3("sfx_place_3", HALF, 150),
    RING_DRAW("sfx_ring_draw", FULL, 400),
    RING_CLOSE("sfx_ring_close", FULL, 400),
    CAPTURED("sfx_captured", FULL, 500),
    CAPTURE_BIG("sfx_capture_big", FULL, 700),
    THREAT("sfx_threat", FULL, 300),
    DOTS_LOW("sfx_dots_low", HALF, 200),
    BOSS_TURN("sfx_boss_turn", FULL, 800),
    BOMB("sfx_bomb", FULL, 450),
    DOUBLE_DOT("sfx_double_dot", FULL, 300),
    AREA_LIGHT("sfx_area_light", HALF, 300),
    WIN("sfx_win", FULL, 1_500, ducksMusic = true),
    LOSE("sfx_lose", FULL, 1_200, ducksMusic = true),
    DRAW("sfx_draw", FULL, 800, ducksMusic = true),
    STAR("sfx_star", FULL, 400),
    WORLD_OPEN("sfx_world_open", FULL, 2_000, ducksMusic = true),
    REWARD("sfx_reward", FULL, 600),
    ;

    /** Where the app keeps it: `audio/sfx_win.ogg` in the assets. */
    val asset: String get() = "audio/$file.ogg"

    companion object {
        /** The dot's three takes, played in turn (sound.md, rule 7). */
        val PLACE = listOf(PLACE_1, PLACE_2, PLACE_3)
    }
}

/** The six music loops of docs/design/sound.md ("Music"), as files in `assets/audio/`. */
enum class Track(val file: String) {
    /** The map and everything over it. */
    MAP("music_map"),

    /** Boards of World 1, the tutorial included, and every two-player game. */
    NOTEBOOK("music_notebook"),

    /** Boards of World 2. */
    GARDEN("music_garden"),

    /** Boards of World 3. */
    NIGHT_SKY("music_night_sky"),

    /** Boards of World 4. */
    SEA("music_sea"),

    /** Boss levels, in any world. */
    BOSS("music_boss"),
    ;

    val asset: String get() = "audio/$file.ogg"

    companion object {
        /** The boards' track of [world]: World 1 has the notebook, World 4 the sea; a world after those has the notebook. */
        fun ofWorld(world: Int): Track = when (world) {
            2 -> GARDEN
            3 -> NIGHT_SKY
            4 -> SEA
            else -> NOTEBOOK
        }
    }
}
