package io.github.pavelya.dots.ui.screen

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.pavelya.dots.R

/**
 * A picture cut from one of the owner's sheets by docs/design/illustrations/make_pictures.py, in
 * its [light] and [dark] version (drawable-nodpi). Pictures of the places on the path are drawn at
 * [PLACE_PX_PER_DP] pixels per dp, all at one scale, so their lines stay equally thick.
 */
@Immutable
data class Picture(val light: Int, val dark: Int) {
    fun of(dark: Boolean): Int = if (dark) this.dark else light
}

/**
 * The places' pixels per dp on the map. The script cuts the places sheet at 4 pixels per dp for
 * 108 dp, its largest picture's size at the scale of the Chest note's chest; on the map that
 * picture, the beaten castle, is 68 dp wide.
 */
const val PLACE_PX_PER_DP = 4f * 108f / 68f

/**
 * A doodle beside the path: a [picture] [width] wide, its centre at [x] of the screen's width,
 * its top [top] below the top of the row of [level] (0 is the start of the path). A doodle hangs
 * from a row and may reach down into the rows below, never up, so it comes on screen whole: a row
 * is drawn only while it is on screen, and rows leave at the top under the map's header.
 */
@Immutable
data class Doodle(val level: Int, val picture: Picture, val x: Float, val top: Dp, val width: Dp)

/**
 * A world's map page (decision 0028; docs/design/illustrations.md, "Where each picture goes"): its
 * boss stop before and after the boss is beaten, the [start] at the foot of its path, and the
 * [doodles] beside the path. Drawn the Notebook way whatever theme the player picked; the light or
 * dark version follows the phone.
 */
@Immutable
data class MapPage(
    val boss: Picture,
    val bossBeaten: Picture,
    val start: Picture,
    val doodles: List<Doodle>,
) {
    /** The doodles hanging from the row of [level]. */
    fun doodlesAt(level: Int): List<Doodle> = doodles.filter { it.level == level }
}

/** Each world's map page, and the pictures every world shares. */
object MapPages {
    /** The chest on the path, closed until it is opened; the same in every world (prompt 5). */
    val CHEST = Picture(R.drawable.map_world_1_places_chest, R.drawable.map_world_1_places_chest_dark)
    val CHEST_OPEN = Picture(R.drawable.map_world_1_places_chest_open, R.drawable.map_world_1_places_chest_open_dark)

    private val PINES = Picture(R.drawable.map_world_1_nature_pines, R.drawable.map_world_1_nature_pines_dark)
    private val TREE = Picture(R.drawable.map_world_1_nature_tree, R.drawable.map_world_1_nature_tree_dark)
    private val POND = Picture(R.drawable.map_world_1_nature_pond, R.drawable.map_world_1_nature_pond_dark)
    private val HILL = Picture(R.drawable.map_world_1_nature_hill, R.drawable.map_world_1_nature_hill_dark)
    private val MOUNTAINS = Picture(R.drawable.map_world_1_nature_mountains, R.drawable.map_world_1_nature_mountains_dark)
    private val CLOUD = Picture(R.drawable.map_world_1_nature_cloud, R.drawable.map_world_1_nature_cloud_dark)
    private val ROCK = Picture(R.drawable.map_world_1_nature_rock, R.drawable.map_world_1_nature_rock_dark)
    private val FENCE = Picture(R.drawable.map_world_1_nature_fence, R.drawable.map_world_1_nature_fence_dark)
    private val GRASS_SHORT = Picture(R.drawable.map_world_1_nature_grass_short, R.drawable.map_world_1_nature_grass_short_dark)
    private val GRASS_TALL = Picture(R.drawable.map_world_1_nature_grass_tall, R.drawable.map_world_1_nature_grass_tall_dark)
    private val GRASS_SMALL = Picture(R.drawable.map_world_1_nature_grass_small, R.drawable.map_world_1_nature_grass_small_dark)
    private val COTTAGE = Picture(R.drawable.map_world_1_places_cottage, R.drawable.map_world_1_places_cottage_dark)

    /**
     * World 1, Countryside: the canvas's `map` frame moved onto the app's path. Each doodle sits
     * on the side away from the path, clear of the stops, their stars and the "next!" label at 320
     * to 412 dp wide: the cottage and a hill by the start, a rock, the pond, the round tree, the
     * pines, the fence, and the mountains and a cloud beside the castle, with grass tufts between.
     */
    private val COUNTRYSIDE = MapPage(
        boss = Picture(R.drawable.map_world_1_places_castle, R.drawable.map_world_1_places_castle_dark),
        bossBeaten = Picture(R.drawable.map_world_1_places_castle_beaten, R.drawable.map_world_1_places_castle_beaten_dark),
        start = Picture(R.drawable.map_world_1_places_flag, R.drawable.map_world_1_places_flag_dark),
        doodles = listOf(
            Doodle(0, COTTAGE, 0.88f, 8.dp, 54.dp),
            Doodle(0, HILL, 0.36f, 34.dp, 84.dp),
            Doodle(1, GRASS_SHORT, 0.25f, 40.dp, 20.dp),
            Doodle(2, ROCK, 0.82f, 14.dp, 50.dp),
            Doodle(3, GRASS_SMALL, 0.9f, 44.dp, 17.dp),
            Doodle(5, POND, 0.31f, 14.dp, 72.dp),
            Doodle(6, GRASS_TALL, 0.24f, 54.dp, 22.dp),
            Doodle(7, GRASS_SMALL, 0.9f, 52.dp, 17.dp),
            Doodle(8, TREE, 0.83f, 0.dp, 60.dp),
            Doodle(9, GRASS_TALL, 0.88f, 6.dp, 22.dp),
            Doodle(10, GRASS_SHORT, 0.42f, 54.dp, 20.dp),
            Doodle(11, PINES, 0.3f, 4.dp, 66.dp),
            Doodle(12, GRASS_SHORT, 0.88f, 40.dp, 20.dp),
            Doodle(14, FENCE, 0.8f, 54.dp, 70.dp),
            Doodle(14, GRASS_SMALL, 0.24f, 50.dp, 17.dp),
            Doodle(15, MOUNTAINS, 0.85f, 54.dp, 84.dp),
            Doodle(15, CLOUD, 0.31f, 18.dp, 46.dp),
        ),
    )

    private val W2_WHEELBARROW = Picture(R.drawable.map_world_2_nature_wheelbarrow, R.drawable.map_world_2_nature_wheelbarrow_dark)
    private val W2_HEDGE = Picture(R.drawable.map_world_2_nature_hedge, R.drawable.map_world_2_nature_hedge_dark)
    private val W2_DAISIES = Picture(R.drawable.map_world_2_nature_daisies, R.drawable.map_world_2_nature_daisies_dark)
    private val W2_FENCE = Picture(R.drawable.map_world_2_nature_fence, R.drawable.map_world_2_nature_fence_dark)
    private val W2_SEEDS = Picture(R.drawable.map_world_2_nature_seeds, R.drawable.map_world_2_nature_seeds_dark)
    private val W2_WATERING_CAN = Picture(R.drawable.map_world_2_nature_watering_can, R.drawable.map_world_2_nature_watering_can_dark)
    private val W2_TULIPS = Picture(R.drawable.map_world_2_nature_tulips, R.drawable.map_world_2_nature_tulips_dark)
    private val W2_BUTTERFLY = Picture(R.drawable.map_world_2_nature_butterfly, R.drawable.map_world_2_nature_butterfly_dark)
    private val W2_SUNFLOWERS = Picture(R.drawable.map_world_2_nature_sunflowers, R.drawable.map_world_2_nature_sunflowers_dark)

    /**
     * World 2, Garden: its canvas frame `map-world-2` moved onto the app's path, as for World 1:
     * the wheelbarrow and the hedge by the gate, a picket fence, the seed rows, the watering can,
     * the tulips, a butterfly, and the sunflowers beside the shed, with daisies between.
     */
    private val GARDEN = MapPage(
        boss = Picture(R.drawable.map_world_2_places_shed, R.drawable.map_world_2_places_shed_dark),
        bossBeaten = Picture(R.drawable.map_world_2_places_shed_beaten, R.drawable.map_world_2_places_shed_beaten_dark),
        start = Picture(R.drawable.map_world_2_places_gate, R.drawable.map_world_2_places_gate_dark),
        doodles = listOf(
            Doodle(0, W2_WHEELBARROW, 0.9f, 12.dp, 44.dp),
            Doodle(0, W2_HEDGE, 0.36f, 30.dp, 80.dp),
            Doodle(1, W2_DAISIES, 0.25f, 38.dp, 26.dp),
            Doodle(2, W2_FENCE, 0.82f, 14.dp, 52.dp),
            Doodle(3, W2_DAISIES, 0.9f, 44.dp, 22.dp),
            Doodle(5, W2_SEEDS, 0.31f, 14.dp, 66.dp),
            Doodle(6, W2_DAISIES, 0.22f, 50.dp, 20.dp),
            Doodle(7, W2_DAISIES, 0.9f, 48.dp, 22.dp),
            Doodle(8, W2_WATERING_CAN, 0.83f, 0.dp, 56.dp),
            Doodle(9, W2_DAISIES, 0.88f, 6.dp, 24.dp),
            Doodle(10, W2_DAISIES, 0.42f, 54.dp, 22.dp),
            Doodle(11, W2_TULIPS, 0.3f, 4.dp, 60.dp),
            Doodle(12, W2_DAISIES, 0.88f, 40.dp, 24.dp),
            Doodle(14, W2_DAISIES, 0.24f, 50.dp, 22.dp),
            Doodle(17, W2_BUTTERFLY, 0.27f, 28.dp, 32.dp),
            Doodle(18, W2_DAISIES, 0.93f, 44.dp, 22.dp),
            Doodle(20, W2_SUNFLOWERS, 0.81f, 0.dp, 58.dp),
            Doodle(20, W2_DAISIES, 0.24f, 70.dp, 22.dp),
        ),
    )

    private val W3_CRATER = Picture(R.drawable.map_world_3_nature_crater, R.drawable.map_world_3_nature_crater_dark)
    private val W3_HILLS = Picture(R.drawable.map_world_3_nature_hills, R.drawable.map_world_3_nature_hills_dark)
    private val W3_TWINKLE = Picture(R.drawable.map_world_3_nature_twinkle, R.drawable.map_world_3_nature_twinkle_dark)
    private val W3_FULL_MOON = Picture(R.drawable.map_world_3_nature_full_moon, R.drawable.map_world_3_nature_full_moon_dark)
    private val W3_TWINKLE_SMALL = Picture(R.drawable.map_world_3_nature_twinkle_small, R.drawable.map_world_3_nature_twinkle_small_dark)
    private val W3_LAKE = Picture(R.drawable.map_world_3_nature_lake, R.drawable.map_world_3_nature_lake_dark)
    private val W3_TWINKLES = Picture(R.drawable.map_world_3_nature_twinkles, R.drawable.map_world_3_nature_twinkles_dark)
    private val W3_PLANET = Picture(R.drawable.map_world_3_nature_planet, R.drawable.map_world_3_nature_planet_dark)
    private val W3_TELESCOPE = Picture(R.drawable.map_world_3_nature_telescope, R.drawable.map_world_3_nature_telescope_dark)
    private val W3_COMET = Picture(R.drawable.map_world_3_nature_comet, R.drawable.map_world_3_nature_comet_dark)
    private val W3_STARS = Picture(R.drawable.map_world_3_nature_stars, R.drawable.map_world_3_nature_stars_dark)
    private val W3_CRESCENT = Picture(R.drawable.map_world_3_nature_crescent, R.drawable.map_world_3_nature_crescent_dark)

    /**
     * World 3, Night sky: its canvas frame `map-world-3` moved onto the app's path, as for World 1:
     * the crater and the hills by the tent, the full moon, the lake, the planet, the telescope, a
     * comet, and the twin stars and the crescent moon beside the observatory, with twinkles
     * between.
     */
    private val NIGHT_SKY = MapPage(
        boss = Picture(R.drawable.map_world_3_places_observatory, R.drawable.map_world_3_places_observatory_dark),
        bossBeaten = Picture(R.drawable.map_world_3_places_observatory_beaten, R.drawable.map_world_3_places_observatory_beaten_dark),
        start = Picture(R.drawable.map_world_3_places_tent, R.drawable.map_world_3_places_tent_dark),
        doodles = listOf(
            Doodle(0, W3_CRATER, 0.9f, 26.dp, 44.dp),
            Doodle(0, W3_HILLS, 0.36f, 34.dp, 84.dp),
            Doodle(1, W3_TWINKLE, 0.25f, 40.dp, 16.dp),
            Doodle(2, W3_FULL_MOON, 0.82f, 10.dp, 44.dp),
            Doodle(3, W3_TWINKLE_SMALL, 0.9f, 44.dp, 12.dp),
            Doodle(5, W3_LAKE, 0.31f, 16.dp, 72.dp),
            Doodle(6, W3_TWINKLE, 0.22f, 54.dp, 16.dp),
            Doodle(7, W3_TWINKLES, 0.88f, 44.dp, 28.dp),
            Doodle(8, W3_PLANET, 0.83f, 8.dp, 60.dp),
            Doodle(9, W3_TWINKLE, 0.88f, 6.dp, 16.dp),
            Doodle(10, W3_TWINKLE_SMALL, 0.42f, 56.dp, 12.dp),
            Doodle(11, W3_TELESCOPE, 0.3f, 4.dp, 52.dp),
            Doodle(12, W3_TWINKLES, 0.88f, 38.dp, 28.dp),
            Doodle(14, W3_TWINKLE, 0.24f, 50.dp, 16.dp),
            Doodle(17, W3_COMET, 0.27f, 30.dp, 46.dp),
            Doodle(18, W3_TWINKLE, 0.93f, 44.dp, 16.dp),
            Doodle(20, W3_STARS, 0.66f, 4.dp, 44.dp),
            Doodle(20, W3_CRESCENT, 0.87f, 0.dp, 36.dp),
            Doodle(20, W3_TWINKLE_SMALL, 0.24f, 70.dp, 12.dp),
        ),
    )

    private val W4_FISH = Picture(R.drawable.map_world_4_nature_fish, R.drawable.map_world_4_nature_fish_dark)
    private val W4_WAVES = Picture(R.drawable.map_world_4_nature_waves, R.drawable.map_world_4_nature_waves_dark)
    private val W4_WAVE = Picture(R.drawable.map_world_4_nature_wave, R.drawable.map_world_4_nature_wave_dark)
    private val W4_WAVE_ROW = Picture(R.drawable.map_world_4_nature_wave_row, R.drawable.map_world_4_nature_wave_row_dark)
    private val W4_SHELL = Picture(R.drawable.map_world_4_nature_shell, R.drawable.map_world_4_nature_shell_dark)
    private val W4_PEARL = Picture(R.drawable.map_world_4_nature_pearl, R.drawable.map_world_4_nature_pearl_dark)
    private val W4_ROCKS = Picture(R.drawable.map_world_4_nature_rocks, R.drawable.map_world_4_nature_rocks_dark)
    private val W4_BOAT = Picture(R.drawable.map_world_4_nature_boat, R.drawable.map_world_4_nature_boat_dark)
    private val W4_GULL = Picture(R.drawable.map_world_4_nature_gull, R.drawable.map_world_4_nature_gull_dark)
    private val W4_ISLAND = Picture(R.drawable.map_world_4_nature_island, R.drawable.map_world_4_nature_island_dark)

    /**
     * World 4, Sea: its canvas frame `map-world-4` moved onto the app's path, as for World 1:
     * the fish and a band of waves by the pier, a row of waves, the shell and its pearl, the rocks,
     * the boat, a gull, and the island beside the tower, with small waves between.
     */
    private val SEA = MapPage(
        boss = Picture(R.drawable.map_world_4_places_tower, R.drawable.map_world_4_places_tower_dark),
        bossBeaten = Picture(R.drawable.map_world_4_places_tower_beaten, R.drawable.map_world_4_places_tower_beaten_dark),
        start = Picture(R.drawable.map_world_4_places_pier, R.drawable.map_world_4_places_pier_dark),
        doodles = listOf(
            Doodle(0, W4_FISH, 0.9f, 14.dp, 40.dp),
            Doodle(0, W4_WAVES, 0.36f, 34.dp, 84.dp),
            Doodle(1, W4_WAVE, 0.25f, 40.dp, 18.dp),
            Doodle(2, W4_WAVE_ROW, 0.82f, 24.dp, 46.dp),
            Doodle(3, W4_WAVE, 0.9f, 44.dp, 18.dp),
            Doodle(5, W4_SHELL, 0.25f, 10.dp, 36.dp),
            Doodle(5, W4_PEARL, 0.4f, 30.dp, 14.dp),
            Doodle(6, W4_WAVE, 0.22f, 54.dp, 18.dp),
            Doodle(7, W4_WAVE_ROW, 0.86f, 50.dp, 46.dp),
            Doodle(8, W4_ROCKS, 0.83f, 20.dp, 60.dp),
            Doodle(9, W4_WAVE, 0.88f, 6.dp, 18.dp),
            Doodle(10, W4_WAVE, 0.42f, 56.dp, 18.dp),
            Doodle(11, W4_BOAT, 0.3f, 0.dp, 54.dp),
            Doodle(12, W4_WAVE, 0.88f, 40.dp, 18.dp),
            Doodle(14, W4_WAVE, 0.24f, 50.dp, 18.dp),
            Doodle(17, W4_GULL, 0.27f, 34.dp, 40.dp),
            Doodle(18, W4_WAVE, 0.93f, 44.dp, 18.dp),
            Doodle(20, W4_ISLAND, 0.8f, 0.dp, 58.dp),
            Doodle(20, W4_WAVE, 0.24f, 70.dp, 18.dp),
        ),
    )

    /** The map page of [world], or null for a world with none. */
    fun of(world: Int): MapPage? = when (world) {
        1 -> COUNTRYSIDE
        2 -> GARDEN
        3 -> NIGHT_SKY
        4 -> SEA
        else -> null
    }
}
