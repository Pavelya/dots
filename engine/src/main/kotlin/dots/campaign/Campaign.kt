package dots.campaign

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
