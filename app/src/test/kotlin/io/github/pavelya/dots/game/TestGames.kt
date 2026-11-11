package io.github.pavelya.dots.game

import dots.ai.Ai
import dots.engine.GameSettings
import dots.log.GameLogEntry
import io.github.pavelya.dots.data.GameJournal
import io.github.pavelya.dots.data.PlacingMethod
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** A [GameViewModel] for tests: every game in [newSettings], against [newComputer] if given. */
internal fun gameViewModel(
    newSettings: () -> GameSettings,
    newComputer: (() -> Ai)? = null,
    computerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    replyDelayMillis: Long = 400,
    showThreats: Boolean = false,
    journal: GameJournal? = null,
    clock: () -> Long = { 0L },
    placing: () -> PlacingMethod = { PlacingMethod.TWO_TAPS },
): GameViewModel {
    val opponent = if (newComputer != null) "test" else GameLogEntry.HUMAN
    fun next() = GameLogEntry.start(newSettings(), opponent, showThreats, clock())
    return GameViewModel(
        start = next(),
        nextGame = ::next,
        computerFor = { newComputer?.invoke() },
        journal = journal,
        computerDispatcher = computerDispatcher,
        replyDelayMillis = replyDelayMillis,
        clock = clock,
        placing = placing,
    )
}
