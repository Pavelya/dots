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

    /** The map page of [world], or null for a world whose pictures have not come in yet (V-20). */
    fun of(world: Int): MapPage? = when (world) {
        1 -> COUNTRYSIDE
        else -> null
    }
}
