package dots.engine

import kotlin.random.Random

enum class Opening { EMPTY, CROSS, DOUBLE_CROSS, CROSSES }

/**
 * Everything needed to set up a game. The [seed] places the random crosses, so the same settings
 * always give the same start and a game can be replayed from its record.
 */
data class GameSettings(
    val width: Int = 15,
    val height: Int = 15,
    val dotsPerPlayer: Int? = 33,
    val opening: Opening = Opening.CROSSES,
    val seed: Long = 0,
    val first: Player = Player.RED,
) {
    fun startDots(): Map<Pos, Player> = when (opening) {
        Opening.EMPTY -> emptyMap()
        Opening.CROSS -> Openings.cross(width, height)
        Opening.DOUBLE_CROSS -> Openings.doubleCross(width, height)
        Opening.CROSSES -> Openings.randomCrosses(width, height, 4, Random(seed))
    }

    fun newGame(): GameState = GameState.start(width, height, dotsPerPlayer, first, startDots())

    companion object {
        /** The format the POC tests: 15x15, four random crosses, 33 dots each (see docs/poc). */
        val POC = GameSettings()
    }
}
