package dots.campaign

/**
 * A world of the campaign: its [levels], in order, and the [name] players see on the map.
 *
 * A world after the first opens when the player has passed the previous world's last level, its
 * boss, and has [starsToOpen] stars in total (decision 0022).
 */
data class World(val number: Int, val name: String, val starsToOpen: Int, val levels: List<Level>) {
    init {
        require(levels.isNotEmpty()) { "world $number has no levels" }
        require(levels.all { it.world == number }) { "world $number holds a level of another world" }
    }

    /** The most stars the world's levels give. */
    val maxStars: Int get() = levels.size * Stars.MAX

    companion object {
        /**
         * Each world's name and the stars it needs, in world order: about 4 stars for every 3
         * levels before it, so passing every level with one star is not enough, and replaying a
         * few for more stars is.
         */
        internal val NAMES_AND_STARS = listOf(
            "Notebook" to 0,
            "Garden" to 20,
            "Night sky" to 45,
            "Sea" to 75,
        )

        /** Groups [levels] into worlds, named and gated by [NAMES_AND_STARS]. */
        internal fun of(levels: List<Level>): List<World> =
            levels.groupBy { it.world }.map { (number, inWorld) ->
                val (name, stars) = NAMES_AND_STARS.getOrNull(number - 1)
                    ?: throw IllegalStateException("world $number has no name and no stars to open it (World.NAMES_AND_STARS)")
                World(number, name, stars, inWorld)
            }
    }
}
