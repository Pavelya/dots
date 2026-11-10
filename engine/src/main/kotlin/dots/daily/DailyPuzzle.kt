package dots.daily

import dots.ai.AiConfig
import dots.campaign.Level
import dots.campaign.LevelMap
import dots.campaign.LevelResult
import dots.campaign.Mission
import dots.campaign.MissionStatus
import dots.campaign.Strength
import dots.engine.GameState
import dots.engine.Player

/**
 * The puzzle of one [day], the same for every player: a small board, a mission and a computer
 * that defends. The player (red) moves first. The mission is "capture N in M moves" or
 * "survive M moves", and [best] moves is the fewest that pass it whatever the computer does.
 * [DailyGenerator] makes the puzzles and [Daily] gives the one for a day.
 */
class DailyPuzzle internal constructor(
    val day: Day,
    val map: LevelMap,
    val mission: Mission,
    val strength: Strength = Daily.STRENGTH,
) {
    init {
        require(mission is Mission.Capture || mission is Mission.Survive) { "a daily puzzle is 'capture' or 'survive', not '${mission.text}'" }
    }

    /** Players see it as "Puzzle #12": puzzle 1 is that of [Daily.FIRST_DAY]. */
    val number: Int get() = Daily.number(day)

    /** The fewest moves of the player's that pass the mission whatever the computer does. */
    val best: Int
        get() = when (mission) {
            is Mission.Capture -> mission.moves
            is Mission.Survive -> mission.moves
            else -> error("not a daily mission")
        }

    /** Dots each side may place: the mission's moves, so the game cannot run on past it. */
    val dots: Int get() = best

    val human: Player get() = level.human
    val computer: Player get() = level.computer
    val ai: AiConfig get() = strength.config

    /**
     * The puzzle as a level, for the solver and the rules. Its world and number mean nothing, so
     * it stays inside the engine: a record of a daily game names the day, not a level.
     */
    internal val level: Level = Level(1, 1, "Puzzle ${day}", strength, best, map, mission)

    fun newGame(): GameState = level.newGame()

    /** Where the mission stands in [state], a position of this puzzle's game. */
    fun status(state: GameState): MissionStatus = level.status(state)

    /** The puzzle ends when its mission is decided (or, in the rare case first, its game is over). */
    fun isOver(state: GameState): Boolean = level.isOver(state)

    fun isPassed(end: GameState): Boolean = level.isPassed(end)

    /** The mission's status at [end]; a passed puzzle earns 1 star, a failed one none. */
    fun result(end: GameState): LevelResult = level.result(end)

    override fun equals(other: Any?): Boolean =
        other is DailyPuzzle && day == other.day && map == other.map && mission == other.mission && strength == other.strength

    override fun hashCode(): Int = listOf(day, map, mission, strength).hashCode()

    override fun toString(): String = DailyFormat.write(listOf(this)).trimEnd()
}
