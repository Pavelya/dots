package dots.campaign

import dots.engine.Action
import dots.engine.Booster
import dots.engine.GameState
import dots.engine.PlayRecord

/**
 * A level game as the level plus the actions taken, boosters included, which replays to exactly
 * the same position. A record names its level by world and number, so it replays against the
 * level as its file is now. [spent] are the level's own boosters used on earlier attempts, which
 * this game starts without (decision 0040). Boosters come on top of the level's own in three ways:
 * [gift] holds those given for this game, such as the free Hint offered after three failures,
 * [owned] those the player brought from their own (decision 0027), and [ad] the one a rewarded ad
 * gave for this retry (decision 0030). Its text form is one line, with the same action words as a
 * [dots.engine.GameRecord], a `-` word for each booster spent before, a `+` word for each booster
 * given, a `*` word for each booster brought and a `!` word for the ad's booster:
 *
 *     level/2 2-5 -undo +hint *wall !wall | e5 f6 wall:g7 g6 f8 double-dot d4 e4 c3
 *
 * Records written before decision 0040 have no `-` words: their games started with the level's
 * full allowance, and they replay so.
 *
 * Records written before decision 0039 start with `level/1`. They replay under the rule of their
 * time, where both dots of a two-dot turn came out of the budget ([extraSecondDot] false), and are
 * written back the same way.
 */
data class LevelRecord(
    val level: Level,
    override val actions: List<Action>,
    val gift: Map<Booster, Int> = emptyMap(),
    val owned: Map<Booster, Int> = emptyMap(),
    /**
     * The one booster a rewarded ad gave for this game, usually the level's [Level.adBooster]: a
     * kind the level allows, never Undo. It does not join the player's own boosters, and it never
     * adds a star ([result]).
     */
    val ad: Booster? = null,
    /**
     * True for the rule of decision 0039: the second dot of a two-dot turn is extra. False for a
     * `level/1` record, which replays under the old rule.
     */
    val extraSecondDot: Boolean = true,
    /**
     * The level's own boosters used on earlier attempts at the level, by kind: the game starts
     * without them ([Progress.spent]). A count above what the level now allows leaves none.
     */
    val spent: Map<Booster, Int> = emptyMap(),
) : PlayRecord {
    init {
        require(spent.values.all { it >= 1 }) { "a booster spent before is kept with a count of 1 or more" }
        require(gift.values.all { it >= 1 }) { "a gift has 1 or more of each booster" }
        require(owned.values.all { it >= 1 }) { "a player brings 1 or more of each booster" }
        require(ad == null || (ad != Booster.UNDO && ad in level.boosters)) {
            "an ad gives a booster the level allows, never undo; level ${level.code} allows ${level.boosters.keys.map { it.text }}, not ${ad?.text}"
        }
    }

    /** The boosters this game has on top of the level's own, by kind. */
    private val extra: Map<Booster, Int>
        get() = (gift.keys + owned.keys + listOfNotNull(ad)).associateWith { (gift[it] ?: 0) + (owned[it] ?: 0) + (if (it == ad) 1 else 0) }

    override fun start(): GameState = level.newGame(extra, extraSecondDot, spent)

    /** The level's own boosters this game started with, by kind: the level's allowance less [spent]. */
    val levelBoosters: Map<Booster, Int> get() = level.boostersLeft(spent)

    /** How many of [booster] the game has used by [state]: what it started with less what is left. */
    private fun used(state: GameState, booster: Booster): Int =
        (levelBoosters[booster] ?: 0) + (extra[booster] ?: 0) - state.boostersLeft(level.human, booster)

    /**
     * How many of the player's [owned] boosters the game has used by [state], by kind: they are
     * used before the gift, the level's own and the [ad]'s. An undo that gives a booster back
     * lowers the count.
     */
    fun ownedUsed(state: GameState): Map<Booster, Int> = owned.mapValues { (booster, brought) ->
        used(state, booster).coerceIn(0, brought)
    }

    /**
     * How many of the level's own boosters the game has used by [state], by kind, leaving out
     * kinds with none used: they are used after the player's [owned] ones and the [gift], and
     * before the [ad]'s. These are gone for every later attempt at the level ([Progress.spendOn]);
     * an undo that gives one back lowers the count, and Undo itself stays used.
     */
    fun levelUsed(state: GameState): Map<Booster, Int> = levelBoosters.mapValues { (booster, had) ->
        (used(state, booster) - (owned[booster] ?: 0) - (gift[booster] ?: 0)).coerceIn(0, had)
    }.filterValues { it > 0 }

    /**
     * The result of this game, ended in [end]: [Level.result] with the [ad]'s booster, so a game
     * with it never earns more stars than the same game without. On levels whose stars count the
     * boosters left, it counts as one of the level's own; on the others, a game that used it earns
     * at most 1 star ([Level.adBoosterUsed]). The boosters [spent] before cost no stars.
     */
    fun result(end: GameState): LevelResult = level.result(end, ad, spent)

    /** The level ends when its game is over or its mission is decided. */
    override fun isOver(state: GameState): Boolean = level.isOver(state)

    override fun withActions(actions: List<Action>): LevelRecord = copy(actions = actions)

    override fun toText(): String = buildString {
        append("${if (extraSecondDot) FORMAT else OLD_FORMAT} ${level.code}")
        for ((booster, count) in spent.entries.sortedBy { it.key }) repeat(count) { append(" -").append(booster.text) }
        for ((booster, count) in gift.entries.sortedBy { it.key }) repeat(count) { append(" +").append(booster.text) }
        for ((booster, count) in owned.entries.sortedBy { it.key }) repeat(count) { append(" *").append(booster.text) }
        ad?.let { append(" !").append(it.text) }
        append(" |")
        for (action in actions) append(' ').append(Action.word(action))
    }

    companion object {
        const val FORMAT = "level/2"

        /** Records from before decision 0039, replayed under the old rule for two-dot turns. */
        const val OLD_FORMAT = "level/1"

        /** True if [text] starts like a level record of either format. */
        fun isRecord(text: String): Boolean = text.trimStart().let { it.startsWith("$FORMAT ") || it.startsWith("$OLD_FORMAT ") }

        /** Reads a record written by [toText]; [find] gives the level for a world and number. */
        fun parse(text: String, find: (world: Int, number: Int) -> Level = Campaign::level): LevelRecord {
            val (head, body) = text.split('|', limit = 2).let { it[0].trim() to it.getOrElse(1) { "" }.trim() }
            val words = head.split(Regex("\\s+"))
            require(words.size >= 2 && (words[0] == FORMAT || words[0] == OLD_FORMAT)) { "Not a $FORMAT record: $text" }
            val code = words[1].split('-').map { it.toIntOrNull() }
            require(code.size == 2 && code.all { it != null }) { "Bad level '${words[1]}' in: $text" }
            val extras = words.drop(2).map { word ->
                require(word.isNotEmpty() && word[0] in "-+*!") { "Bad booster '$word' in: $text" }
                val booster = Booster.entries.firstOrNull { it.text == word.drop(1) } ?: throw IllegalArgumentException("Bad booster '$word' in: $text")
                word[0] to booster
            }
            val gift = extras.filter { it.first == '+' }.groupingBy { it.second }.eachCount()
            val owned = extras.filter { it.first == '*' }.groupingBy { it.second }.eachCount()
            val spent = extras.filter { it.first == '-' }.groupingBy { it.second }.eachCount()
            val ads = extras.filter { it.first == '!' }.map { it.second }
            require(ads.size <= 1) { "An ad gives one booster, not ${ads.size}, in: $text" }
            val actions = if (body.isEmpty()) emptyList() else body.split(Regex("\\s+")).map(Action::parse)
            return LevelRecord(find(code[0]!!, code[1]!!), actions, gift, owned, ads.firstOrNull(), extraSecondDot = words[0] == FORMAT, spent = spent)
        }
    }
}
