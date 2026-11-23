package io.github.pavelya.dots.game

import dots.engine.GameState
import dots.engine.Player
import dots.engine.TurnRule

/**
 * What the counter above the board shows on a boss level whose boss places two dots every few turns
 * (game-screens.md, "Game board: the parts" and "Boss turn"): how many of the [boss]'s turns until
 * its next turn of two dots, 1 when its next turn is one, and 0 from the player's dot before it
 * until the boss's second dot (`GameState.doubleMoveIn`). The count drops as each boss turn begins.
 *
 * Null when there is no counter: on a level without such a boss (4-20's boss places two dots on
 * every turn, so it has none), at the end, and once the boss has too few dots left for another
 * turn of two.
 */
fun bossCountdown(state: GameState, boss: Player): Int? =
    if (state.turnRule(boss) is TurnRule.DoubleMove) state.doubleMoveIn(boss) else null
