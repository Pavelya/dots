package dots.campaign

import dots.engine.Booster

/**
 * The level ladder, read from the level files in the engine's resources (`levels/`). A level's
 * file is named after its world and number: `1-03.level` is level 3 of world 1. Worlds and levels
 * are numbered from 1 without gaps; the first missing file ends a world, and a world without a
 * level 1 ends the campaign.
 *
 * Every level starts the human with dots in the middle of the board. Dots on the border can never
 * be captured, so without that a player could place every dot on the border and never lose.
 */
object Campaign {
    const val FOLDER = "levels"

    val levels: List<Level> by lazy { load(::resource) }

    /** The [levels] grouped into worlds, with each world's name and the stars that open it. */
    val worlds: List<World> by lazy { World.of(levels) }

    /** The chests on the path, one after every 10th level, in order. */
    val chests: List<Chest> by lazy { Chest.of(levels) }

    /** The chest on the path right after [level], if there is one. */
    fun chestAfter(level: Level): Chest? = chests.firstOrNull { it.after.code == level.code }

    /** The level after [level] in the campaign, across worlds; null after the last. */
    fun after(level: Level): Level? = indexOf(level)?.let { levels.getOrNull(it + 1) }

    /**
     * The first level from [from] on, [from] itself included, that allows [booster], in campaign
     * order; null if none does. A player's own boosters of a kind wait for that level (decision 0027).
     */
    fun firstAllowing(booster: Booster, from: Level): Level? =
        indexOf(from)?.let { i -> levels.drop(i).firstOrNull { booster in it.boosters } }

    /** The first level after [chest] that allows [booster], one of its rewards: where the reward can first be used. */
    fun firstUse(chest: Chest, booster: Booster): Level? = after(chest.after)?.let { firstAllowing(booster, it) }

    /** Where [level] stands in [levels], found by its code; null for a level that is not in the campaign. */
    private fun indexOf(level: Level): Int? = levels.indexOfFirst { it.code == level.code }.takeIf { it >= 0 }

    /** What the world map shows for [progress]. */
    fun map(progress: Progress): CampaignMap = CampaignMap.of(worlds, progress, chests)

    /**
     * The strength free play uses against the computer (decision 0003: no difficulty picker): the
     * strongest computer among the levels passed in [progress], or the first level's before any.
     */
    fun strengthReached(progress: Progress): Strength =
        levels.filter { progress.isPassed(it) }.maxOfOrNull { it.strength } ?: levels.first().strength

    fun level(world: Int, number: Int): Level = levels.firstOrNull { it.world == world && it.number == number }
        ?: throw NoSuchElementException("No level $world-$number (see the files in $FOLDER/)")

    fun fileName(world: Int, number: Int): String = "%d-%02d.level".format(world, number)

    /** Loads the campaign with [read], which gives a file's text by name, or null if there is none. */
    internal fun load(read: (String) -> String?): List<Level> {
        val levels = ArrayList<Level>()
        var world = 1
        while (true) {
            var number = 1
            while (true) {
                val name = fileName(world, number)
                val text = read(name) ?: break
                val level = LevelFormat.parse(text, name)
                if (level.world != world || level.number != number) {
                    throw LevelFormatException(
                        name, null,
                        "the file says level ${level.code}, but its name says $world-$number",
                    )
                }
                levels += level
                number++
            }
            if (number == 1) break
            world++
        }
        return levels
    }

    private fun resource(name: String): String? =
        Campaign::class.java.getResourceAsStream("/$FOLDER/$name")?.use { it.readBytes().toString(Charsets.UTF_8) }
}
