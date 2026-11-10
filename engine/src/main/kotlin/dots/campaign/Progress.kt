package dots.campaign

/**
 * The player's campaign progress: the most stars earned on each passed level, by level code
 * (`1-3`). A level that is not in [stars] has not been passed. Stars only ever grow.
 */
data class Progress(val stars: Map<String, Int> = emptyMap()) {
    init {
        require(stars.values.all { it in 1..Stars.MAX }) { "a passed level has 1 to ${Stars.MAX} stars" }
    }

    /** All stars earned. */
    val total: Int get() = stars.values.sum()

    fun stars(level: Level): Int = stars[level.code] ?: 0

    fun isPassed(level: Level): Boolean = level.code in stars

    /** This progress after [level] was passed with [earned] stars; a replay keeps the best. */
    fun record(level: Level, earned: Int): Progress =
        if (earned <= stars(level)) this else copy(stars = stars + (level.code to earned))

    /** One line for storage, read back by [parse]: `1-1:3 1-2:2`, in level order. */
    val text: String
        get() = stars.entries
            .sortedWith(compareBy({ order(it.key)?.first }, { order(it.key)?.second }))
            .joinToString(" ") { "${it.key}:${it.value}" }

    companion object {
        val NONE = Progress()

        /**
         * Reads [text], as [Progress.text] writes it. An entry that cannot be read is left out, so
         * damaged storage costs the player that level's stars, not the whole campaign.
         */
        fun parse(text: String): Progress = Progress(
            text.split(' ').mapNotNull { entry ->
                val code = entry.substringBefore(':', "")
                val stars = entry.substringAfter(':', "").toIntOrNull()
                if (order(code) != null && stars != null && stars in 1..Stars.MAX) code to stars else null
            }.toMap(),
        )

        /** World and number of a level [code], or null if it is not one. */
        private fun order(code: String): Pair<Int, Int>? {
            val parts = code.split('-')
            if (parts.size != 2) return null
            val world = parts[0].toIntOrNull() ?: return null
            val number = parts[1].toIntOrNull() ?: return null
            return world to number
        }
    }
}

/** Where a level stands for the player. */
enum class LevelState {
    /** Not playable yet: the level before it is not passed, or its world is not open. */
    LOCKED,

    /** Playable and not yet passed. */
    OPEN,

    /** Passed, with 1 to 3 stars; it can be replayed for more. */
    PASSED,
}

/** A level on the map: where it stands and the stars earned on it. */
data class LevelStop(val level: Level, val state: LevelState, val stars: Int) {
    /** A boss level: it bends a rule in the computer's favour. */
    val isBoss: Boolean get() = level.rule != null
}

/**
 * A world on the map. [starsNeeded] is how many more stars it needs before it opens (0 when the
 * stars are there); [bossNeeded] is the previous world's boss, while it is not passed.
 */
data class WorldStop(
    val world: World,
    val isOpen: Boolean,
    val stars: Int,
    val starsNeeded: Int,
    val bossNeeded: Level?,
    val levels: List<LevelStop>,
)

/**
 * What the world map shows for a player's [Progress]: each world open or locked, each level
 * locked, open or passed, and the [next] level to play.
 *
 * World 1 is always open; a later world opens when the previous world's last level is passed and
 * the player has its [World.starsToOpen] stars (decision 0022). In an open world, the first level
 * is open, and each other level opens when the one before it is passed.
 */
class CampaignMap(val worlds: List<WorldStop>, val totalStars: Int) {
    /**
     * The level to play next: the first open level. Null when every level is passed, or when the
     * next world still needs stars; then the player replays levels for more.
     */
    val next: Level? get() = worlds.firstNotNullOfOrNull { world -> world.levels.firstOrNull { it.state == LevelState.OPEN }?.level }

    /** The stop of [level]. */
    fun stop(level: Level): LevelStop = worlds[level.world - 1].levels[level.number - 1]

    /**
     * Why [level] is locked: the level to pass first, or the world that needs stars or its
     * previous boss. Null when the level is not locked.
     */
    fun lock(level: Level): Lock? {
        if (stop(level).state != LevelState.LOCKED) return null
        val world = worlds[level.world - 1]
        return if (world.isOpen) Lock.PassFirst(world.levels[level.number - 2].level) else Lock.WorldClosed(world)
    }

    sealed interface Lock {
        data class PassFirst(val level: Level) : Lock

        data class WorldClosed(val world: WorldStop) : Lock
    }

    /**
     * What this map opens that [before] kept locked: the worlds that opened and the levels that
     * became playable, in campaign order. Result shows them after a level is passed.
     */
    fun openedSince(before: CampaignMap): Opened {
        val worlds = this.worlds.filter { it.isOpen && before.worlds.getOrNull(it.world.number - 1)?.isOpen == false }
        val levels = this.worlds.flatMap { it.levels }
            .filter { it.state == LevelState.OPEN && before.stop(it.level).state == LevelState.LOCKED }
        return Opened(worlds.map { it.world }, levels.map { it.level })
    }

    /** Worlds and levels a pass has just opened (see [openedSince]). */
    data class Opened(val worlds: List<World>, val levels: List<Level>) {
        val isEmpty: Boolean get() = worlds.isEmpty() && levels.isEmpty()

        companion object {
            val NOTHING = Opened(emptyList(), emptyList())
        }
    }

    companion object {
        fun of(worlds: List<World>, progress: Progress): CampaignMap {
            val total = progress.total
            var previous: World? = null
            val stops = worlds.map { world ->
                val boss = previous?.levels?.last()?.takeUnless(progress::isPassed)
                val starsNeeded = (world.starsToOpen - total).coerceAtLeast(0)
                val open = previous == null || (boss == null && starsNeeded == 0)
                var before = open
                val levels = world.levels.map { level ->
                    val stars = progress.stars(level)
                    val state = when {
                        stars > 0 && open -> LevelState.PASSED
                        before -> LevelState.OPEN
                        else -> LevelState.LOCKED
                    }
                    before = state == LevelState.PASSED
                    LevelStop(level, state, if (state == LevelState.PASSED) stars else 0)
                }
                previous = world
                WorldStop(world, open, levels.sumOf { it.stars }, starsNeeded, boss, levels)
            }
            return CampaignMap(stops, total)
        }
    }
}
