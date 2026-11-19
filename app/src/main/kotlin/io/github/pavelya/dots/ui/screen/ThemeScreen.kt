package io.github.pavelya.dots.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.github.pavelya.dots.ui.theme.Palette

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
    val style = Palette.current
    val audio = LocalAudio.current
    BoxWithConstraints {
        // The wireframe's 14 dp between cards on a tall phone, 8 dp on a small one (320 x 568).
        val gap = if (maxHeight >= 700.dp) 14.dp else 8.dp
        ThemePage(choices, inUse, onUse = { id ->
            audio.play(Effect.PLACE_1, file = GameTheme.of(id).sound(Effect.PLACE_1))
            onUse(id)
        }, debugNote, onShop, onBack, gap, style)
    }
}

@Composable
private fun ThemePage(
    choices: List<ThemeId>,
    inUse: ThemeId,
    onUse: (ThemeId) -> Unit,
    debugNote: Boolean,
    onShop: (() -> Unit)?,
    onBack: () -> Unit,
    gap: Dp,
    style: Palette,
) {
    PaperPage(stringResource(R.string.settings_theme), onBack, stringResource(R.string.back_to_settings)) {
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            if (debugNote) DebugNote(stringResource(R.string.theme_debug_note), style)
            val group = stringResource(R.string.settings_theme)
            Column(
                verticalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier.selectableGroup().semantics { contentDescription = group },
            ) {
                choices.forEachIndexed { i, id ->
                    ThemeCard(id, selected = id == inUse, turn = CARD_TURNS[i % CARD_TURNS.size], style = style) {
                        if (id != inUse) onUse(id)
                    }
                }
            }
            Text(stringResource(R.string.theme_light_or_dark), style = CARD_LINE.copy(lineHeight = 18.sp), color = style.graphite)
            if (onShop != null && choices.size < ThemeId.entries.size) {
                Text(
                    stringResource(R.string.theme_more_in_shop),
                    style = ROW_TEXT.copy(textDecoration = TextDecoration.Underline),
                    color = style.blue,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button, onClick = onShop)
                        .wrapContentHeight(),
                )
            }
        }
    }
}

/**
 * One theme's card: its sample board, drawn by the theme, its name and line, and "In use" with a
 * tick when [selected]. TalkBack reads the name and the line, then selected or not; the board and
 * "In use" are not read.
 */
@Composable
private fun ThemeCard(id: ThemeId, selected: Boolean, turn: Float, style: Palette, onClick: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .rotate(turn)
            .shadow(2.dp, shape, clip = false)
            .background(style.card, shape)
            .border(2.dp, if (selected) style.blue else Color.Transparent, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(8.dp),
    ) {
        Box(Modifier.clearAndSetSemantics {}) {
            DotsTheme(GameTheme.of(id), dark = GameTheme.isDark, highContrast = GameTheme.highContrast) {
                BoardView(THEME_SAMPLE, BoardStyle.current, description = "", modifier = Modifier.size(width = 112.dp, height = 80.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(themeName(id)), style = CARD_NAME, color = style.pencil)
            Text(stringResource(themeLine(id)), style = CARD_LINE, color = style.pencil)
            if (selected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clearAndSetSemantics {},
                ) {
                    Icon(remember(style) { Doodles.icon(Doodles.TICK, style.red, width = 3f) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.theme_in_use), style = CARD_LINE.copy(fontWeight = FontWeight.ExtraBold), color = style.red)
                }
            }
        }
    }
}

/** A note for testers in a dashed frame: what a player's build shows instead. */
@Composable
private fun DebugNote(text: String, style: Palette) {
    Text(
        text,
        style = CARD_LINE.copy(lineHeight = 18.sp),
        color = style.graphite,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val line = 1.5.dp.toPx()
                drawRoundRect(
                    style.graphite,
                    topLeft = Offset(line / 2, line / 2),
                    size = Size(size.width - line, size.height - line),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(line, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                )
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** How far each card is turned, in degrees, in the wireframe's order. */
private val CARD_TURNS = listOf(-0.6f, 0.5f, -0.3f)

/** A theme's name on its card: Caveat, 26 sp. */
private val CARD_NAME: TextStyle
    @Composable @ReadOnlyComposable
    get() = NoteText.display.copy(fontSize = 26.sp, lineHeight = 26.sp)

/** A card's line, and the small text under the cards: 13 sp. */
private val CARD_LINE: TextStyle
    @Composable @ReadOnlyComposable
    get() = NoteText.body.copy(fontSize = 13.sp, lineHeight = 17.sp)
