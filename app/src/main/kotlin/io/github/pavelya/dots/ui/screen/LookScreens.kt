package io.github.pavelya.dots.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dots.engine.GameState
import dots.engine.Pos
import io.github.pavelya.dots.R
import io.github.pavelya.dots.audio.Effect
import io.github.pavelya.dots.audio.LocalAudio
import io.github.pavelya.dots.data.PieceStyleId
import io.github.pavelya.dots.data.RingEffectId
import io.github.pavelya.dots.data.ThemeId
import io.github.pavelya.dots.ui.board.BoardStyle
import io.github.pavelya.dots.ui.board.BoardView
import io.github.pavelya.dots.ui.theme.DotsTheme
import io.github.pavelya.dots.ui.theme.GameTheme

/** A piece style's name, as the Shop and Settings call it (cosmetics.md); the theme's own is Standard. */
@StringRes
fun pieceStyleName(style: PieceStyleId): Int = when (style) {
    PieceStyleId.STANDARD -> R.string.look_standard
    PieceStyleId.HEARTS_AND_CLUBS -> R.string.pieces_hearts_and_clubs
    PieceStyleId.FLAME_AND_FROST -> R.string.pieces_flame_and_frost
}

/** A ring effect's name, as the Shop and Settings call it (cosmetics.md); the theme's own is Standard. */
@StringRes
fun ringEffectName(effect: RingEffectId): Int = when (effect) {
    RingEffectId.STANDARD -> R.string.look_standard
    RingEffectId.LASSO -> R.string.rings_lasso
    RingEffectId.RUBBER_STAMP -> R.string.rings_rubber_stamp
}

/** A piece style's one line on its card (flows.md, "Choosing pieces and rings"). */
@StringRes
internal fun pieceStyleLine(style: PieceStyleId): Int = when (style) {
    PieceStyleId.STANDARD -> R.string.pieces_standard_line
    PieceStyleId.HEARTS_AND_CLUBS -> R.string.pieces_hearts_and_clubs_line
    PieceStyleId.FLAME_AND_FROST -> R.string.pieces_flame_and_frost_line
}

/** A ring effect's one line on its card. */
@StringRes
internal fun ringEffectLine(effect: RingEffectId): Int = when (effect) {
    RingEffectId.STANDARD -> R.string.rings_standard_line
    RingEffectId.LASSO -> R.string.rings_lasso_line
    RingEffectId.RUBBER_STAMP -> R.string.rings_rubber_stamp_line
}

/**
 * The Shop's small board (cosmetics.md, "The Shop's small board"), on every card of the Pieces and
 * Rings pages: 5 x 5 points, a red ring round one blue piece, a red piece and three blue pieces
 * loose, and the last move ([LOOK_SAMPLE_LAST]) marked.
 */
internal val LOOK_SAMPLE: GameState = GameState.fromAscii(
    """
    B....
    ..R.B
    .RBR.
    .....
    R....
    """,
).play(Pos(2, 3)).state.play(Pos(3, 4)).state

/** The last move on [LOOK_SAMPLE]: Blue's piece at the bottom. */
internal val LOOK_SAMPLE_LAST = Pos(3, 4)

/**
 * The Pieces page (flows.md, "Choosing pieces and rings"; the wireframes' frame "Pieces"): one
 * card for each piece style the player can choose ([choices]), Standard first, each on the Shop's
 * small board drawn by the [theme] in use with the theme's own ring. Tapping a card plays the
 * theme's first placing sound and puts the style in use at once ([onUse]). Otherwise as the Theme
 * page: [debugNote] on a debug build, and a link to the Shop ([onShop], once it exists) when the
 * player lacks a style.
 */
@Composable
fun PiecesScreen(
    theme: ThemeId,
    choices: List<PieceStyleId>,
    inUse: PieceStyleId,
    onUse: (PieceStyleId) -> Unit,
    debugNote: Boolean,
    onShop: (() -> Unit)?,
    onBack: () -> Unit,
) {
    val audio = LocalAudio.current
    ChoicePage(
        title = stringResource(R.string.settings_pieces),
        choices = choices,
        inUse = inUse,
        onUse = { style ->
            audio.play(Effect.PLACE_1, file = GameTheme.of(theme).sound(Effect.PLACE_1))
            onUse(style)
        },
        name = ::pieceStyleName,
        line = ::pieceStyleLine,
        debugNote = if (debugNote) stringResource(R.string.pieces_debug_note) else null,
        under = stringResource(R.string.looks_shown_in_your_theme),
        shopLink = if (lacksOne(choices, PieceStyleId.entries, onShop)) stringResource(R.string.pieces_more_in_shop) else null,
        onShop = { onShop?.invoke() },
        onBack = onBack,
    ) { style, tall ->
        LookSample(theme, tall, pieces = style)
    }
}

/**
 * The Rings page (flows.md, "Choosing pieces and rings"; the wireframes' frame "Rings"): the Pieces
 * page for ring effects. Each card's board has the theme's own pieces and the finished ring, still;
 * tapping a card plays the theme's ring-closing sound.
 */
@Composable
fun RingsScreen(
    theme: ThemeId,
    choices: List<RingEffectId>,
    inUse: RingEffectId,
    onUse: (RingEffectId) -> Unit,
    debugNote: Boolean,
    onShop: (() -> Unit)?,
    onBack: () -> Unit,
) {
    val audio = LocalAudio.current
    ChoicePage(
        title = stringResource(R.string.settings_rings),
        choices = choices,
        inUse = inUse,
        onUse = { effect ->
            audio.play(Effect.RING_CLOSE, file = GameTheme.of(theme).sound(Effect.RING_CLOSE))
            onUse(effect)
        },
        name = ::ringEffectName,
        line = ::ringEffectLine,
        debugNote = if (debugNote) stringResource(R.string.rings_debug_note) else null,
        under = stringResource(R.string.looks_shown_in_your_theme),
        shopLink = if (lacksOne(choices, RingEffectId.entries, onShop)) stringResource(R.string.rings_more_in_shop) else null,
        onShop = { onShop?.invoke() },
        onBack = onBack,
    ) { effect, tall ->
        LookSample(theme, tall, rings = effect)
    }
}

/**
 * The Shop's small board in [theme], in the light or dark version the phone picks, with only
 * [pieces] or [rings] changed: 104 dp on a tall phone, 80 dp on a small one, as the wireframes.
 */
@Composable
private fun LookSample(
    theme: ThemeId,
    tall: Boolean,
    pieces: PieceStyleId = PieceStyleId.STANDARD,
    rings: RingEffectId = RingEffectId.STANDARD,
) {
    LookBoard(LOOK_SAMPLE, LOOK_SAMPLE_LAST, theme, Modifier.size(if (tall) 104.dp else 80.dp), pieces, rings)
}

/**
 * A small still board, [state] with its [lastMove] marked, drawn by [theme] in the light or dark
 * version the phone picks, with the [pieces] and [rings] given: the Settings cards, the Shop's
 * tiles and its preview. TalkBack reads [description], if any.
 */
@Composable
internal fun LookBoard(
    state: GameState,
    lastMove: Pos?,
    theme: ThemeId,
    modifier: Modifier,
    pieces: PieceStyleId = PieceStyleId.STANDARD,
    rings: RingEffectId = RingEffectId.STANDARD,
    description: String = "",
) {
    DotsTheme(GameTheme.of(theme), dark = GameTheme.isDark, highContrast = GameTheme.highContrast, pieces = pieces, rings = rings) {
        BoardView(state, BoardStyle.current, description = description, lastMove = lastMove, modifier = modifier)
    }
}
