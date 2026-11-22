package io.github.pavelya.dots.game

import dots.ai.Ai
import dots.campaign.Strength
import dots.engine.Action
import dots.engine.Game
import dots.engine.GameRecord
import dots.engine.GameSettings
import dots.engine.GameState
import dots.engine.Opening
import dots.engine.Player
import dots.engine.Pos
import dots.log.GameLogEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * The canvas's open-ended game (docs/design/canvas/draw.py, `FREE`, D-16) on a 15 x 15 board,
 * played by the rules from an empty board. Red leads 4 to 1: its rings run to the top and right
 * edges, a chain runs in from the left edge, and its group of three at (5, 11), (6, 11) and
 * (6, 12) is joined to no edge, so only those three can still be captured after a stop.
 */
internal object FreeGame {
    /** Blue first, Blue's last dot at (7, 11); Red to move. */
    private const val FREE = "6,4,2,4,10,9,6,3,11,9,7,4,10,10,6,5,2,3,6,2,3,4,6,1,2,5,6,0,0,4,9,9,4,11,10,8,6,13,11,8,8,14,12,9," +
        "8,13,11,10,9,12,10,11,9,5,13,9,10,4,14,9,11,3,0,7,12,3,1,7,13,2,2,7,14,2,3,7,3,9,5,11,4,6,6,11,8,7,6,12,7,8,5,4,1,4,9,10,7,11"

    /**
     * The same game with the sides swapped, mirrored left to right and one dot fewer for the
     * computer (draw.py, `FREE_COMPUTER`): Red first, Blue passes once, Red's last dot at (7, 11).
     * Blue, the computer, leads 4 to 1 with two open dots, so it stops.
     */
    private const val COMPUTER = "8,4,12,4,4,9,8,3,3,9,7,4,4,10,8,5,12,3,8,2,11,4,8,1,12,5,8,0,14,4,5,9,10,11,4,8,8,13,3,8,6,14," +
        "2,9,6,13,3,10,5,12,4,11,5,5,1,9,4,4,0,9,3,3,14,7,2,3,13,7,1,2,12,7,0,2,11,7,11,9,9,11,10,6,8,11,6,7,9,4,7,8,5,10,13,4,pass,7,11"

    private fun actions(text: String): List<Action> {
        val tokens = text.split(",")
        val out = ArrayList<Action>()
        var i = 0
        while (i < tokens.size) {
            if (tokens[i] == "pass") {
                out += Action.Pass
                i++
            } else {
                out += Action.Place(Pos(tokens[i].toInt(), tokens[i + 1].toInt()))
                i += 2
            }
        }
        return out
    }

    /** Red's open group: the dots Stop's note circles. */
    val openRed = listOf(Pos(5, 11), Pos(6, 11), Pos(6, 12))

    /** The computer's two open dots after it stopped. */
    val openBlue = listOf(Pos(8, 11), Pos(9, 11))

    private fun record(text: String, first: Player, dotsPerPlayer: Int?, then: List<Action> = emptyList(), last: Pos? = null): GameRecord {
        var moves = actions(text)
        // Another of Blue's dots played last: the two swap places, which changes no capture.
        if (last != null) {
            val end = moves.last()
            moves = moves.map { if (it == Action.Place(last)) end else if (it == end) Action.Place(last) else it }
        }
        return GameRecord(GameSettings(width = 15, height = 15, dotsPerPlayer = dotsPerPlayer, opening = Opening.EMPTY, first = first), moves + then)
    }

    private fun entry(record: GameRecord, vsComputer: Boolean): GameLogEntry {
        val state = Game.from(record).state
        val opponent = if (vsComputer) Strength.HARD.text else GameLogEntry.HUMAN
        return GameLogEntry.start(record, opponent, threatHighlight = false, now = 0)
            .copy(record = record, red = state.score(Player.RED), blue = state.score(Player.BLUE))
    }

    /** Free play, open-ended: Red, the player, to move. */
    val open: GameLogEntry get() = entry(record(FREE, Player.BLUE, dotsPerPlayer = null), vsComputer = true)

    /** The same position with a dot budget of 33 each (Short): Red has 9 dots left. The game Play a game asks to replace. */
    val saved: GameLogEntry get() = entry(record(FREE, Player.BLUE, dotsPerPlayer = 33), vsComputer = true)

    /** Two players: Blue's last dot was (8, 7), then Red stopped. Blue collects. */
    val redStopped: GameLogEntry
        get() = entry(record(FREE, Player.BLUE, dotsPerPlayer = null, then = listOf(Action.Stop), last = Pos(8, 7)), vsComputer = false)

    /** Free play: the computer stopped after the player's dot at (7, 11). The player collects. */
    val computerStopped: GameLogEntry get() = entry(record(COMPUTER, Player.RED, dotsPerPlayer = null, then = listOf(Action.Stop)), vsComputer = true)

    /** Free play, just before the computer stops: the player has played (7, 11), and the computer is to move. */
    val computerToStop: GameLogEntry get() = entry(record(COMPUTER, Player.RED, dotsPerPlayer = null), vsComputer = true)

    /**
     * A view model playing [entry]. Against the computer, [ai] plays it, by default one that takes
     * the first free point, thinking on [dispatcher].
     */
    fun viewModel(entry: GameLogEntry, ai: Ai = FirstFree, dispatcher: CoroutineDispatcher = Dispatchers.Unconfined): GameViewModel = GameViewModel(
        start = entry,
        nextGame = { entry },
        computerFor = { if (it.opponent == GameLogEntry.HUMAN) null else ai },
        computerDispatcher = dispatcher,
        replyDelayMillis = 0,
    )

    private object FirstFree : Ai {
        override fun choose(state: GameState): Action = Action.Place(state.legalMoves().first())
    }
}
