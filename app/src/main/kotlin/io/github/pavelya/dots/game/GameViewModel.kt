package io.github.pavelya.dots.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dots.ai.Ai
import dots.ai.Danger
import dots.ai.Hints
import dots.campaign.CampaignMap
import dots.campaign.Level
import dots.campaign.LevelRecord
import dots.campaign.MissionStatus
import dots.campaign.Tutorial
import dots.campaign.Outcome as MissionOutcome
import dots.engine.Action
import dots.engine.Booster
import dots.engine.Game
import dots.engine.GameRecord
import dots.engine.GameState
import dots.engine.MoveResult
import dots.engine.Player
import dots.engine.Pos
import dots.log.GameLogEntry
import dots.log.Outcome
import io.github.pavelya.dots.data.CampaignJournal
import io.github.pavelya.dots.data.GameJournal
import io.github.pavelya.dots.data.LevelEnd
import io.github.pavelya.dots.data.PlacingMethod
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Everything the game screen draws, as one immutable value. */
data class GameUiState(
    val state: GameState,
    val preview: Pos? = null,
    val lastMove: Pos? = null,
    /** True while the computer is choosing its move; taps are ignored meanwhile. */
    val thinking: Boolean = false,
    val canUndo: Boolean = false,
    val vsComputer: Boolean = false,
    /** Groups close to capture, for both players; empty unless the threat highlight is on. */
    val dangers: List<Danger> = emptyList(),
    /** The computer's face and speech bubble; null between two people. */
    val opponent: OpponentLook? = null,
    /** True once the game is over: no move is left or, in a campaign level, its mission is decided. */
    val isOver: Boolean = state.isOver,
    /** The campaign level being played; null in free play and between two people. */
    val level: Level? = null,
    /** Where the level's mission stands; null outside the campaign. */
    val mission: MissionStatus? = null,
    /** A finished level's end, once its stars are saved; Result opens then. Null until then, and outside the campaign. */
    val levelEnd: LevelEnd? = null,
    /** True before the game's first action: a level then opens by showing its mission. */
    val fresh: Boolean = false,
    /** Counts the games played on this screen, so that each new one opens afresh. */
    val round: Int = 0,
    /**
     * The tutorial's guided step (A-06): what to do now and the point to tap. Null outside the
     * tutorial's levels, once the player skipped it, and at the end. On the computer's turn the
     * last step stays, without a point.
     */
    val guide: Tutorial.Step? = null,
    /**
     * True for the whole of a tutorial level whose steps are not skipped: the board is laid out for
     * the step note, whether or not a step shows at the moment.
     */
    val guided: Boolean = false,
    /** True for a practice game, such as a replay of the tutorial: its end changes no campaign progress. */
    val practice: Boolean = false,
    /** True for a free game with no dot limit: it ends when a player stops and the other is done (decision 0025). */
    val openEnded: Boolean = false,
    /** True when the side to move may stop now: an open-ended game, a person's turn, before anyone stopped. */
    val canStop: Boolean = false,
    /**
     * True when a person collects after the other side stopped: they place as many dots as they
     * like, then end the game with done.
     */
    val collecting: Boolean = false,
    /**
     * The campaign's boosters the player may use now (A-05): on their turn, while the computer is
     * not thinking and no booster preview is open. Empty outside the campaign.
     */
    val usable: Set<Booster> = emptySet(),
    /** The booster preview, while one is open over the board; null otherwise. */
    val aim: BoosterAim? = null,
    /** The point a Hint showed, circled until the player's dot lands; null otherwise. */
    val hint: Pos? = null,
    /** After a Double dot, the dots the player still places this turn: 2, then 1. Null otherwise. */
    val doubleDots: Int? = null,
) {
    /** True when Result can open: the game is over and, in a level, its end is saved. */
    val showsResult: Boolean get() = isOver && (level == null || levelEnd != null)
}

/**
 * An open booster preview (docs/design/flows.md, "Booster preview"): the [booster], the [point]
 * chosen for it, and what the board shows of it. Wall and Bomb need a point; the others have none.
 */
data class BoosterAim(
    val booster: Booster,
    /** The chosen point, or the point under the finger while it drags; null until one is chosen. */
    val point: Pos? = null,
    /** The points it may be aimed at; empty for a booster without a point. */
    val targets: List<Pos> = emptyList(),
    /** The bomb's plus of 5 points around [point], on the board. */
    val plus: List<Pos> = emptyList(),
    /** The pieces it would take away: the bomb's hits, or the turn Undo takes back. */
    val gone: List<Pos> = emptyList(),
) {
    /** True once Confirm can apply it: it has its point, or needs none. */
    val ready: Boolean get() = point != null || !needsPoint(booster)

    companion object {
        fun needsPoint(booster: Booster): Boolean = booster == Booster.WALL || booster == Booster.BOMB
    }
}

/**
 * Holds one game and turns screen events into game actions. The screen sends events ([onTap],
 * [aim], [release], [undo], [stop], [done], [playAgain], and for boosters [chooseBooster],
 * [confirmBooster] and [cancelBooster]) and draws [ui]; all rules are the engine's.
 *
 * A game is described by a [GameLogEntry]: a new one, or the saved one when the app was closed
 * mid-game (its moves are replayed). Every change is reported to the [journal], which saves it
 * and logs the game once it is over. A campaign level's game ends as soon as its mission is
 * decided; its stars and failures then go to the [campaign], and Result opens once they are saved.
 *
 * Against the computer the human is red and moves first. The computer thinks on
 * [computerDispatcher], never on the main thread, and its thinking is cancelled by undo, a new
 * game, or leaving the screen. It answers after at least [replyDelayMillis], so the player sees
 * their own dot land before the reply.
 *
 * @param start the game to play: new, or resumed
 * @param nextGame the game "play again" starts (a fresh seed every time; the same level again, or
 *   a free game with the same setup)
 * @param computerFor the computer opponent of a game, or null for two players on one phone
 * @param clock the time now in milliseconds, for the log
 * @param placing how dots are placed now, for the log: the game log records the method with the
 *   touches and dots it took, so that `cli placing-report` can compare methods (A-13)
 * @param hintFor the Hint booster's point for a position, found on [computerDispatcher]
 */
class GameViewModel(
    start: GameLogEntry,
    private val nextGame: () -> GameLogEntry,
    private val computerFor: (GameLogEntry) -> Ai?,
    private val journal: GameJournal? = null,
    private val campaign: CampaignJournal? = null,
    private val computerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val replyDelayMillis: Long = 400,
    private val clock: () -> Long = System::currentTimeMillis,
    private val placing: () -> PlacingMethod = { PlacingMethod.TWO_TAPS },
    private val hintFor: (GameState) -> Action.Hint? = { Hints.hint(it) },
) : ViewModel() {
    val human: Player = Player.RED

    private var entry = start
    private var game = Game.from(start.record)
    private var computer: Ai? = computerFor(start)
    private var thinking: Job? = null
    private var levelEnd: LevelEnd? = null
    private var round = 0

    /** When the game last changed (or was opened), to add up the time spent playing. */
    private var lastActive = clock()

    /**
     * The point last chosen in the booster preview. While a finger drags, the preview follows it;
     * lifting it off a target comes back here (A-05).
     */
    private var chosen: Pos? = null

    /** True once the player skipped the tutorial's guided steps on this screen. */
    private var skipped = false

    /**
     * The last guided step for the position itself, with no dot shown: on the computer's turn it
     * stays, without its point.
     */
    private var shownGuide: Tutorial.Step? = null

    /** How often the human took a move back in this game. */
    val undoCount: Int get() = entry.undos

    /** The campaign level being played, or null. */
    private val level: Level? get() = (entry.record as? LevelRecord)?.level

    private val _ui = MutableStateFlow(snapshot(lastMove = lastPlaced()))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    init {
        // A resumed level may have been closed between its last move and the saving of its stars.
        if (game.isOver) levelOver()
        letComputerMove() // a resumed game may have been closed while the computer was to move
    }

    /**
     * A tap on board point [point], or off the board when null, placing by two taps ([Placement]):
     * the first shows the preview, a second on the same point places the dot.
     */
    fun onTap(point: Pos?) {
        if (!humanToMove()) return
        if (_ui.value.aim != null) return aimAt(point, lifted = true)
        touched()
        val result = Placement.onTap(_ui.value.preview, point) { game.state.isLegal(it) }
        val placed = result.placed
        if (placed == null) {
            showPreview(result.preview)
        } else {
            put(placed)
        }
    }

    /**
     * A finger is down over [point], or off the board when null: the preview follows it, on a
     * playable point only. Placing methods that place on release send this while the finger moves;
     * it is not a touch of its own.
     */
    fun aim(point: Pos?) {
        if (!humanToMove()) return
        if (_ui.value.aim != null) return aimAt(point, lifted = false)
        val preview = point?.takeIf { game.state.isLegal(it) }
        if (preview != _ui.value.preview) showPreview(preview)
    }

    /**
     * A touch that ends asking for a dot at [point]: it is placed if the point is playable. Null,
     * or an unplayable point, places nothing and clears the preview (the finger was lifted off the
     * board, or too early).
     */
    fun release(point: Pos?) {
        if (!humanToMove()) return
        if (_ui.value.aim != null) return aimAt(point, lifted = true)
        touched()
        if (point == null || !game.state.isLegal(point)) {
            showPreview(null)
        } else {
            put(point)
        }
    }

    private fun showPreview(preview: Pos?) {
        _ui.value = _ui.value.copy(preview = preview, guide = guideFor(game.state, preview))
    }

    /** True when a touch on the board may place a dot: not while the computer thinks or after the end. */
    private fun humanToMove(): Boolean =
        !_ui.value.thinking && !game.isOver && (computer == null || game.state.toMove == human)

    /** Counts a touch on the board for the log; it is saved with the next move. */
    private fun touched() {
        entry = entry.copy(touches = entry.touches + 1)
    }

    private fun put(point: Pos) {
        val move = game.apply(Action.Place(point))
        entry = entry.copy(placed = entry.placed + 1)
        changed()
        _ui.value = snapshot(lastMove = point, captured = move)
        letComputerMove()
    }

    /**
     * Opens [booster]'s preview over the board, if the player may use it now. Nothing is spent
     * until [confirmBooster].
     */
    fun chooseBooster(booster: Booster) {
        if (booster !in _ui.value.usable) return
        val state = game.state
        val targets = when (booster) {
            Booster.WALL -> state.points().filter { state.isLegal(Action.Wall(it)) }
            Booster.BOMB -> state.points().filter { state.isLegal(Action.Bomb(it)) }
            else -> emptyList()
        }
        chosen = null
        showAim(BoosterAim(booster, targets = targets))
    }

    /**
     * In the booster preview, a touch at [point] chooses it the way the player places a dot: a tap
     * (two taps), or a finger [lifted] there (drag), while a moving finger only shows it. A touch
     * off the targets keeps the point already chosen.
     */
    private fun aimAt(point: Pos?, lifted: Boolean) {
        val aim = _ui.value.aim ?: return
        val target = point?.takeIf { it in aim.targets }
        if (lifted && target != null) chosen = target
        val shown = target ?: chosen
        if (shown != aim.point) showAim(aim.copy(point = shown))
    }

    /** Shows [aim] with what it would do at its point, and the board as it would look. */
    private fun showAim(aim: BoosterAim) {
        val state = game.state
        val point = aim.point
        val shown = when {
            aim.booster == Booster.BOMB && point != null -> aim.copy(plus = plusAround(state, point), gone = state.bombHits(point))
            aim.booster == Booster.UNDO -> {
                val before = state.useBooster(Action.Undo)
                aim.copy(gone = state.points().filter { state.ownerAt(it) != null && before.ownerAt(it) == null })
            }
            else -> aim.copy(plus = emptyList(), gone = emptyList())
        }
        _ui.value = snapshot(lastMove = lastPlaced(), aim = shown)
    }

    /** Applies the booster of the open preview: Wall and Bomb at its point, Hint once it is found. */
    fun confirmBooster() {
        val aim = _ui.value.aim ?: return
        if (!humanToMove() || !aim.ready) return
        val action = when (aim.booster) {
            Booster.WALL -> Action.Wall(aim.point!!)
            Booster.BOMB -> Action.Bomb(aim.point!!)
            Booster.DOUBLE_DOT -> Action.DoubleDot
            Booster.UNDO -> Action.Undo
            Booster.HINT -> return findHint()
        }
        if (!game.state.isLegal(action)) return cancelBooster()
        use(action)
    }

    /** Closes the booster preview; nothing is spent. */
    fun cancelBooster() {
        if (_ui.value.aim == null) return
        chosen = null
        _ui.value = snapshot(lastMove = lastPlaced())
    }

    /** The Hint looks for the player's best move as the computer would, off the main thread. */
    private fun findHint() {
        val position = game.state
        chosen = null
        _ui.value = snapshot(lastMove = lastPlaced()).copy(thinking = true, usable = emptySet())
        thinking = viewModelScope.launch {
            val hint = withContext(computerDispatcher) { hintFor(position) }
            ensureActive()
            if (game.state !== position) return@launch // the game changed meanwhile
            if (hint == null) {
                _ui.value = snapshot(lastMove = lastPlaced())
            } else {
                use(hint)
            }
        }
    }

    private fun use(action: Action.UseBooster) {
        chosen = null
        game.apply(action)
        if (action == Action.Undo) entry = entry.copy(undos = entry.undos + 1)
        changed()
        _ui.value = snapshot(lastMove = lastPlaced())
        letComputerMove()
    }

    /**
     * In an open-ended game, the side to move declares the dots they keep grounded. The other side
     * then collects what is left: the computer by itself, a person until they are [done].
     */
    fun stop() {
        if (!humanToMove() || !_ui.value.canStop) return
        game.apply(Action.Stop)
        changed()
        _ui.value = snapshot(lastMove = lastPlaced())
        letComputerMove()
    }

    /** A person who collects after the other side stopped ends the game. */
    fun done() {
        if (!humanToMove() || !_ui.value.collecting) return
        game.apply(Action.Finish)
        changed()
        _ui.value = snapshot(lastMove = lastPlaced())
    }

    /** Takes back the human's last move and the computer's reply. */
    fun undo() {
        if (computer == null || game.isOver) return
        thinking?.cancel()
        if (game.undo(human)) {
            entry = entry.copy(undos = entry.undos + 1)
            changed()
        }
        _ui.value = snapshot(lastMove = lastPlaced())
    }

    /**
     * Starts a new game: free play with fresh settings (a new seed, so new crosses), or the same
     * level again, as practice if this game is. An unfinished game is logged as abandoned.
     */
    fun playAgain() {
        val level = level
        begin(if (entry.practice && level != null) levelGame(level, entry.threatHighlight, clock(), practice = true) else nextGame())
    }

    /** In a practice game of the tutorial, plays its next level, also as practice; else does nothing. */
    fun practiceNext() {
        val next = level?.takeIf { entry.practice }?.let(Tutorial::next) ?: return
        begin(levelGame(next, entry.threatHighlight, clock(), practice = true))
    }

    /** Hides the tutorial's guided steps on this screen, also in the levels played after this one. */
    fun skipGuides() {
        skipped = true
        shownGuide = null
        _ui.value = _ui.value.copy(guide = null, guided = false)
    }

    /**
     * Plays the same level again with [booster] given on top of the level's own: the help Result
     * offers after the third failure in a row. Outside the campaign it is [playAgain].
     */
    fun retryWith(booster: Booster) {
        val level = level ?: return playAgain()
        begin(levelGame(level, entry.threatHighlight, clock(), gift = mapOf(booster to 1), practice = entry.practice))
    }

    private fun begin(next: GameLogEntry) {
        thinking?.cancel()
        entry = next
        game = Game.from(entry.record)
        computer = computerFor(entry)
        levelEnd = null
        shownGuide = null
        chosen = null
        round++
        lastActive = clock()
        journal?.start(entry)
        _ui.value = snapshot(lastMove = null)
    }

    private fun letComputerMove() {
        val ai = computer ?: return
        val position = game.state
        if (game.isOver || position.toMove == human) return
        _ui.value = _ui.value.copy(thinking = true)
        thinking = viewModelScope.launch {
            delay(replyDelayMillis)
            val action = withContext(computerDispatcher) {
                val context = coroutineContext
                ai.choose(position) { !context.isActive }
            }
            ensureActive()
            if (game.state !== position) return@launch // the position changed meanwhile (undo)
            val result = game.apply(action)
            changed()
            _ui.value = snapshot(lastMove = (action as? Action.Place)?.pos ?: lastPlaced(), captured = result)
            letComputerMove() // the engine skips the human's turn if they have no dots left
        }
    }

    /** Brings [entry] up to date with the game and hands it to the journal. */
    private fun changed() {
        val now = clock()
        val state = game.state
        val over = game.isOver
        entry = entry.copy(
            updatedAt = now,
            playMillis = entry.playMillis + (now - lastActive).coerceIn(0, GameLogEntry.IDLE_CAP_MILLIS),
            outcome = if (over) Outcome.FINISHED else Outcome.PLAYING,
            red = state.score(Player.RED),
            blue = state.score(Player.BLUE),
            record = game.record,
            placing = placing().key,
        )
        lastActive = now
        if (over) {
            journal?.finish(entry)
            levelOver()
        } else {
            journal?.save(entry)
        }
    }

    /**
     * Saves a finished level's stars and failures, then lets Result open. A practice game saves
     * nothing; in the tutorial, Next then plays its next level.
     */
    private fun levelOver() {
        val level = level ?: return
        val result = level.result(game.state)
        val thisRound = round
        val practice = entry.practice
        viewModelScope.launch {
            val end = when {
                practice -> LevelEnd(result, CampaignMap.Opened.NOTHING, failed = 0, next = Tutorial.next(level))
                else -> campaign?.finished(level, result) ?: LevelEnd(result, CampaignMap.Opened.NOTHING, failed = 0, next = null)
            }
            if (round != thisRound) return@launch // another game has started meanwhile
            levelEnd = end
            _ui.value = _ui.value.copy(levelEnd = end)
        }
    }

    /** The guided step for [state] with a dot shown at [preview]; see [GameUiState.guide]. */
    private fun guideFor(state: GameState, preview: Pos?): Tutorial.Step? {
        val level = level
        if (level == null || skipped || game.isOver || Tutorial.lesson(level) == null) {
            shownGuide = null
            return null
        }
        val step = Tutorial.step(level, state, preview)
        if (preview == null || step == null) shownGuide = step ?: shownGuide?.copy(point = null)
        return step ?: shownGuide
    }

    /** The last dot placed that is still on the board: after an Undo, the dot before the turn it took back. */
    private fun lastPlaced(): Pos? {
        val state = game.state
        return when (val last = game.record.actions.lastOrNull { it is Action.Place || it == Action.Undo }) {
            is Action.Place -> last.pos
            Action.Undo -> state.lastPlaced(state.toMove.opponent)
            else -> null
        }
    }

    /** After a Double dot this turn, the dots the player still places: 2, then 1; else null. */
    private fun doubleDotsLeft(state: GameState): Int? {
        if (state.toMove != human || !state.boosterUsed || game.isOver) return null
        val actions = game.record.actions
        val used = actions.lastIndexOf(Action.DoubleDot)
        if (used < 0) return null
        val placed = actions.subList(used + 1, actions.size)
        if (placed.any { it !is Action.Place } || placed.size >= 2) return null
        return 2 - placed.size
    }

    /** What the screen draws now; [captured] is the move just played, if any, for the computer's face. */
    private fun snapshot(lastMove: Pos?, captured: MoveResult? = null, aim: BoosterAim? = null): GameUiState {
        val state = game.state
        val level = level
        val mission = level?.status(state)
        val over = game.isOver
        val openEnded = level == null && (entry.record as? GameRecord)?.settings?.dotsPerPlayer == null
        val peopleToMove = computer == null || state.toMove == human
        // Undo's preview shows the board as it was before the turn it takes back, threats included.
        val shownState = if (aim?.booster == Booster.UNDO) state.useBooster(Action.Undo) else state
        val dangers = if (entry.threatHighlight && !over) Player.entries.flatMap { Hints.dangers(shownState, it) } else emptyList()
        val opponent = computer?.let {
            val lastCapture = captured?.captures?.takeIf { it.isNotEmpty() }?.let { captures ->
                LastCapture(captures.first().by, captures.sumOf { c -> c.points })
            }
            val inDanger = if (entry.threatHighlight) {
                dangers.any { it.dotsNeeded <= 1 && state.ownerAt(it.group.first()) == human }
            } else {
                !over && Hints.dangers(state, human, maxDots = 1).isNotEmpty()
            }
            val ending = when {
                !over -> null
                mission == null -> OpponentLook.endingOf(state, human)
                mission.outcome == MissionOutcome.PASSED -> Ending.WON
                else -> Ending.LOST
            }
            if (aim?.booster == Booster.UNDO) OpponentLook(Mood.SMUG, Bubble.TAKE_IT_BACK) else OpponentLook.of(state, human, lastCapture, inDanger, ending)
        }
        val humanTurn = !over && (computer == null || state.toMove == human)
        val usable = if (level == null || !humanTurn || aim != null) emptySet() else Booster.entries.filter { game.canUse(it) }.toSet()
        return GameUiState(
            state = state,
            lastMove = lastMove,
            canUndo = computer != null && level == null && !over && game.canUndo(human),
            vsComputer = computer != null,
            dangers = dangers,
            opponent = opponent,
            isOver = over,
            level = level,
            mission = mission,
            levelEnd = levelEnd,
            fresh = game.record.actions.isEmpty(),
            round = round,
            guide = guideFor(state, preview = null),
            guided = level != null && Tutorial.lesson(level) != null && !skipped,
            practice = entry.practice,
            openEnded = openEnded,
            canStop = openEnded && peopleToMove && state.isLegal(Action.Stop),
            collecting = openEnded && peopleToMove && !over && state.isCleanup,
            usable = usable,
            aim = aim,
            hint = (game.record.actions.lastOrNull() as? Action.Hint)?.pos?.takeIf { humanTurn },
            doubleDots = doubleDotsLeft(state),
        )
    }
}

/** Every point of the board, row by row. */
private fun GameState.points(): List<Pos> = (0 until height).flatMap { y -> (0 until width).map { x -> Pos(x, y) } }

/** The bomb's plus of 5 points around [center]: those on the board. */
private fun plusAround(state: GameState, center: Pos): List<Pos> =
    listOf(center, Pos(center.x, center.y - 1), Pos(center.x - 1, center.y), Pos(center.x + 1, center.y), Pos(center.x, center.y + 1))
        .filter { state.inBounds(it.x, it.y) && !state.isHole(it) }
