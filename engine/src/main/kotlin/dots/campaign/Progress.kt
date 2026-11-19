package dots.campaign

import dots.engine.Booster

/**
 * The player's campaign progress, everything that is saved and backed up:
 *
 * - [stars]: the most stars earned on each passed level, by level code (`1-3`). A level that is not
 *   in it has not been passed. Stars only ever grow.
 * - [boosters]: the boosters the player owns, from chests, on top of what each level gives
 *   (decision 0027). A level game takes the ones its level allows ([owned]).
 * - [chests]: the chests opened, by the code of the level each one follows.
 * - [cosmetics]: the cosmetics chests gave, by [Cosmetic.id] (C-05). Never purchases: those come
 *   from Google Play (C-02). A cosmetic is earned by chest or by stars ([earned]).
 */
data class Progress(
    val stars: Map<String, Int> = emptyMap(),
    val boosters: Map<Booster, Int> = emptyMap(),
    val chests: Set<String> = emptySet(),
    val cosmetics: Set<String> = emptySet(),
) {
    init {
        require(stars.values.all { it in 1..Stars.MAX }) { "a passed level has 1 to ${Stars.MAX} stars" }
        require(boosters.values.all { it >= 1 }) { "an owned booster is kept with a count of 1 or more" }
    }

    /** All stars earned. */
    val total: Int get() = stars.values.sum()

    /** The player's title, from [total]. */
    val title: Title get() = Title.of(total)

    /**
     * The cosmetics earned by playing: those [total] stars reach, and those a chest gave. A name
     * in [cosmetics] that is no [Cosmetic] is left out.
     */
    val earned: Set<Cosmetic> get() = Cosmetic.earnedAt(total) + cosmetics.mapNotNull(Cosmetic::of)

    /** True once [cosmetic] is earned by stars or from a chest. */
    fun hasEarned(cosmetic: Cosmetic): Boolean = total >= cosmetic.stars || cosmetic.id in cosmetics

    fun stars(level: Level): Int = stars[level.code] ?: 0

    fun isPassed(level: Level): Boolean = level.code in stars

    /** This progress after [level] was passed with [earned] stars; a replay keeps the best. */
    fun record(level: Level, earned: Int): Progress =
        if (earned <= stars(level)) this else copy(stars = stars + (level.code to earned))

    /** The owned boosters a game of [level] takes: those of the kinds the level allows. */
    fun owned(level: Level): Map<Booster, Int> = boosters.filterKeys { it in level.boosters }

    /** True once [chest] has been opened. */
    fun isOpened(chest: Chest): Boolean = chest.after.code in chests

    /** True when [chest] can be opened: the level before it is passed, and it is not opened yet. */
    fun isReady(chest: Chest): Boolean = isPassed(chest.after) && !isOpened(chest)

    /**
     * This progress after [chest] was opened and [reward], one of its choices, kept. A chest opens
     * once: opening it again changes nothing.
     */
    fun open(chest: Chest, reward: Reward): Progress {
        require(reward in chest.choices) { "$reward is not in the chest after ${chest.after.code}" }
        if (isOpened(chest)) return this
        return copy(
            boosters = boosters + (reward.booster to (boosters[reward.booster] ?: 0) + reward.count),
            chests = chests + chest.after.code,
        )
    }

    /**
     * This progress with [used] taken from the owned boosters; a negative count gives some back
     * (an undo). A count never goes below 0.
     */
    fun spend(used: Map<Booster, Int>): Progress {
        if (used.values.all { it == 0 }) return this
        val left = (boosters.keys + used.keys).associateWith { ((boosters[it] ?: 0) - (used[it] ?: 0)).coerceAtLeast(0) }
        return copy(boosters = left.filterValues { it > 0 })
    }

    /** The [stars] as one line for storage, read back by [parse]: `1-1:3 1-2:2`, in level order. */
    val text: String
        get() = stars.entries
            .sortedWith(compareBy({ order(it.key)?.first }, { order(it.key)?.second }))
            .joinToString(" ") { "${it.key}:${it.value}" }

    /** The owned [boosters] as one line: `wall:2 bomb:1`. */
    val boostersText: String get() = boosterText(boosters)

    /** The opened [chests] as one line: `1-10 2-5`, in level order. */
    val chestsText: String
        get() = chests.sortedWith(compareBy({ order(it)?.first }, { order(it)?.second })).joinToString(" ")

    /** The [cosmetics] chests gave as one line, sorted. */
    val cosmeticsText: String get() = cosmetics.sorted().joinToString(" ")

    companion object {
        val NONE = Progress()

        /**
         * Reads the lines written by [text], [boostersText], [chestsText] and [cosmeticsText]. An
         * entry that cannot be read is left out, so damaged storage costs the player that one entry,
         * not the whole campaign. A line saved before its part existed is empty.
         */
        fun parse(stars: String, boosters: String = "", chests: String = "", cosmetics: String = ""): Progress = Progress(
            stars = entries(stars).mapNotNull { entry ->
                val code = entry.substringBefore(':', "")
                val count = entry.substringAfter(':', "").toIntOrNull()
                if (order(code) != null && count != null && count in 1..Stars.MAX) code to count else null
            }.toMap(),
            boosters = parseBoosters(boosters),
            chests = entries(chests).filter { order(it) != null }.toSet(),
            cosmetics = entries(cosmetics).filter { COSMETIC.matches(it) }.toSet(),
        )

        /** Boosters by kind as one line, `wall:2 bomb:1`, read back by [parseBoosters]. */
        fun boosterText(boosters: Map<Booster, Int>): String =
            boosters.entries.filter { it.value > 0 }.sortedBy { it.key }.joinToString(" ") { "${it.key.text}:${it.value}" }

        /** Reads [boosterText]; an entry that cannot be read, or a count below 1, is left out. */
        fun parseBoosters(text: String): Map<Booster, Int> = entries(text).mapNotNull { entry ->
            val booster = Booster.entries.firstOrNull { it.text == entry.substringBefore(':', "") }
            val count = entry.substringAfter(':', "").toIntOrNull()
            if (booster != null && count != null && count >= 1) booster to count else null
        }.toMap()

        private val COSMETIC = Regex("[a-z0-9][a-z0-9-]*")

        private fun entries(text: String): List<String> = text.split(' ').filter { it.isNotEmpty() }

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

/** Where a chest on the path stands for the player. */
enum class ChestState {
    /** The level before it is not passed yet. */
    LOCKED,

    /** The level before it is passed: the player can open it. */
    READY,

    /** Opened, its reward kept. */
    OPENED,
}

/** A chest on the map and where it stands. */
data class ChestStop(val chest: Chest, val state: ChestState)

/**
 * What the world map shows for a player's [Progress]: each world open or locked, each level
 * locked, open or passed, the [chests] on the path, the player's [title], and the [next] level to
 * play.
 *
 * World 1 is always open; a later world opens when the previous world's last level is passed and
 * the player has its [World.starsToOpen] stars (decision 0022). In an open world, the first level
 * is open, and each other level opens when the one before it is passed.
 */
class CampaignMap(val worlds: List<WorldStop>, val totalStars: Int, val chests: List<ChestStop> = emptyList()) {
    /** The player's title, from [totalStars]. */
    val title: Title get() = Title.of(totalStars)

    /** The chest on the path right after [level], if there is one. */
    fun chestAfter(level: Level): ChestStop? = chests.firstOrNull { it.chest.after.code == level.code }

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
        fun of(worlds: List<World>, progress: Progress, chests: List<Chest> = emptyList()): CampaignMap {
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
            val chestStops = chests.map { chest ->
                val state = when {
                    progress.isOpened(chest) -> ChestState.OPENED
                    progress.isPassed(chest.after) -> ChestState.READY
                    else -> ChestState.LOCKED
                }
                ChestStop(chest, state)
            }
            return CampaignMap(stops, total, chestStops)
        }
    }
}
