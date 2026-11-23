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
import io.github.pavelya.dots.data.ThemeId
import io.github.pavelya.dots.ui.board.BoardStyle
import io.github.pavelya.dots.ui.board.BoardView
import io.github.pavelya.dots.ui.theme.DotsTheme
import io.github.pavelya.dots.ui.theme.GameTheme

/** The theme's name, as Settings and the Theme page show it (flows.md, "Choosing a theme"). */
@StringRes
fun themeName(id: ThemeId): Int = when (id) {
    ThemeId.NOTEBOOK -> R.string.theme_notebook
    ThemeId.STAR_CHART -> R.string.theme_star_chart
    ThemeId.NEON_ARCADE -> R.string.theme_neon_arcade
}

/** The theme's one line on its card. */
@StringRes
private fun themeLine(id: ThemeId): Int = when (id) {
    ThemeId.NOTEBOOK -> R.string.theme_notebook_line
    ThemeId.STAR_CHART -> R.string.theme_star_chart_line
    ThemeId.NEON_ARCADE -> R.string.theme_neon_arcade_line
}

/**
 * The sample board on every theme's card (flows.md, "Choosing a theme"): the same small position in
 * each, a capture by Red around one blue piece and a few loose pieces of each side, as the
 * wireframes' Theme frame places them.
 */
internal val THEME_SAMPLE: GameState = GameState.fromAscii(
    """
    .......
    ...R.B.
    ..RBR..
    ......B
    .R...B.
    """,
).play(Pos(3, 3)).state

/**
 * The Theme page (flows.md, "Choosing a theme"; the wireframes' frame "Theme"): one card for each
 * theme the player can choose ([choices]), each with a sample board drawn by that theme, in the
 * light or dark version the phone picks. The page itself is on Notebook paper, like Settings. The
 * theme [inUse] has a tick and "In use". Tapping another card plays that theme's first placing
 * sound and puts it in use at once ([onUse]); tapping the theme in use does nothing. A debug build
 * says so at the top ([debugNote]). [onShop], once the Shop exists, adds a link to it under the
 * cards when the player lacks a theme.
 */
@Composable
fun ThemeScreen(
    choices: List<ThemeId>,
    inUse: ThemeId,
    onUse: (ThemeId) -> Unit,
    debugNote: Boolean,
    onShop: (() -> Unit)?,
    onBack: () -> Unit,
) {
    val audio = LocalAudio.current
    ChoicePage(
        title = stringResource(R.string.settings_theme),
        choices = choices,
        inUse = inUse,
        onUse = { id ->
            audio.play(Effect.PLACE_1, file = GameTheme.of(id).sound(Effect.PLACE_1))
            onUse(id)
        },
        name = ::themeName,
        line = ::themeLine,
        debugNote = if (debugNote) stringResource(R.string.theme_debug_note) else null,
        under = stringResource(R.string.theme_light_or_dark),
        shopLink = if (lacksOne(choices, ThemeId.entries, onShop)) stringResource(R.string.theme_more_in_shop) else null,
        onShop = { onShop?.invoke() },
        onBack = onBack,
    ) { id, _ ->
        DotsTheme(GameTheme.of(id), dark = GameTheme.isDark, highContrast = GameTheme.highContrast) {
            BoardView(THEME_SAMPLE, BoardStyle.current, description = "", modifier = Modifier.size(width = 112.dp, height = 80.dp))
        }
    }
}
