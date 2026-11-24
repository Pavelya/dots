package io.github.pavelya.dots.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dots.campaign.Holes
import dots.campaign.Level
import dots.campaign.Mission
import dots.campaign.Stars
import dots.engine.Booster
import io.github.pavelya.dots.R
import io.github.pavelya.dots.game.Mood
import io.github.pavelya.dots.ui.board.BoardStyle
import io.github.pavelya.dots.ui.board.BoardView
import io.github.pavelya.dots.ui.motion.NoteMotion
import io.github.pavelya.dots.ui.motion.dim
import io.github.pavelya.dots.ui.motion.rememberNoteMotion
import io.github.pavelya.dots.ui.motion.slideUp
import io.github.pavelya.dots.ui.theme.FaceInks
import io.github.pavelya.dots.ui.theme.Palette

/** The hand-cut shape of a booster chip on the card. */
private val CHIP_SHAPE = RoundedCornerShape(24.dp)

/**
 * Up to this many booster chips sit in one row beside the board picture. More share one row of their
 * own under the picture and the computer, like the game's tool bar (canvas frame `level-card-five`, D-33).
 */
private const val CHIPS_BESIDE = 2

/**
 * The Level card, over the map (canvas frame `level-card`; the wireframes' Level card and Boss
 * card): a page torn from the notebook with the level's name, its mission in one line, a small
 * picture of the board at the start, the computer's face for its strength and its head start, the
 * dots and boosters the player has (a booster opens [onBooster]: Boosters), on a board with holes
 * whether their edge is safe or open, and the star targets.
 * The [boosters] are what a new game of the level has ([dots.campaign.Progress.boostersFor]): the
 * level's own left after earlier attempts (decision 0040) plus the player's own of the kinds it
 * allows (decision 0027), with ×0 for a kind used up. A boss
 * level adds the boss's rule, and the boss's name and face in red. One button: Start, or Resume
 * when this level's game is [saved]. When the campaign's saved game is of another level, [replaces]
 * names it, and Start first asks before that game ends (flows.md, "Saved games"); Keep it, or
 * Back, returns to the card. Close, or a tap on the map around the card, closes it.
 *
 * The card is drawn over the whole screen, anchored at the bottom; on a small phone it scrolls.
 * It slides up as the map behind it dims, and down as it leaves ([motion]).
 */
@Composable
fun LevelCard(
    level: Level,
    saved: Boolean,
    onStart: () -> Unit,
    onBooster: (Booster) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    boosters: Map<Booster, Int> = level.boosters,
    replaces: String? = null,
    motion: NoteMotion = rememberNoteMotion(),
) {
    val style = Palette.current
    var asking by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = asking) { asking = false }
    val boss = level.rule != null
    val title = stringResource(R.string.level_title, level.world, level.number)
    BoxWithConstraints(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .fillMaxSize()
            .dim(style.scrim) { motion.up }
            .semantics { paneTitle = "$title, ${level.name}" },
    ) {
        // A small phone (canvas frames `small-level-card-*`): a smaller picture and face, and less room between the parts.
        val small = maxHeight < 700.dp
        val picture = if (small) 96.dp else 120.dp
        val gap = if (small) 10.dp else 12.dp
        val shared = boosters.size > CHIPS_BESIDE
        // A tap on the map around the card closes it; the card above takes its own taps.
        Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onClose() } })
        Column(
            verticalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier
                .slideUp { motion.up }
                .padding(start = 10.dp, end = 10.dp, bottom = 16.dp, top = 24.dp)
                .rotate(-0.8f)
                .heightIn(max = maxHeight - 40.dp)
                .shadow(6.dp, RectangleShape, clip = false)
                .background(style.card)
                .tornTop(style.card)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = if (small) 10.dp else 14.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
                    Text(title, style = NoteText.label, color = style.graphite)
                    Text(level.name, style = NoteText.display, color = if (boss) style.red else style.blue)
                }
                CloseButton(style, onClose)
            }
            level.rule?.let { rule ->
                val label = stringResource(R.string.level_boss)
                val ruleLabel = stringResource(R.string.level_boss_rule)
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, style.red, TOOL_SHAPE)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(label.uppercase(), style = NoteText.small, fontWeight = FontWeight.ExtraBold, color = style.red)
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(ruleLabel) }
                            append(' ')
                            append(ruleText(rule))
                        },
                        style = NoteText.body,
                        color = style.pencil,
                    )
                }
            }
            MissionLine(level, style)
            // Three or more chips share a row under the picture; the column beside it then ends with
            // "Boosters:" just above them, so it sits at the picture's foot.
            Column(verticalArrangement = Arrangement.spacedBy(if (small) 6.dp else 8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (small) 12.dp else 14.dp),
                    verticalAlignment = if (shared) Alignment.Bottom else Alignment.CenterVertically,
                ) {
                    BoardPicture(level, Modifier.width(picture))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Opponent(level, style, small)
                        Allowance(level, style)
                        if (!shared) BoosterChips(boosters, style, onBooster)
                    }
                }
                if (shared) BoosterRow(boosters, style, small, onBooster)
            }
            HolesLine(level, style)
            StarTargets(level, style)
            if (asking && replaces != null) {
                ReplaceQuestion(
                    style,
                    detail = stringResource(R.string.level_replace_detail, replaces),
                    replace = stringResource(R.string.level_start),
                    onReplace = onStart,
                    onKeep = { asking = false },
                )
            } else {
                val start = if (!saved && replaces != null) ({ asking = true }) else onStart
                BigButton(stringResource(if (saved) R.string.level_resume else R.string.level_start), style, start, Modifier.fillMaxWidth())
            }
        }
    }
}

/** The card's top edge, torn from the notebook: a zigzag in [ink] just above it (draw.py, `level_card_frame`). */
private fun Modifier.tornTop(ink: Color): Modifier = drawBehind {
    val u = size.width / 340f
    val path = Path().apply {
        moveTo(0f, 1f)
        lineTo(0f, -5f * u)
        var x = 10
        while (x <= 340) {
            lineTo(x * u, (if ((x / 10) % 2 == 1) -2f else -7f) * u)
            x += 10
        }
        lineTo(size.width, 1f)
        close()
    }
    drawPath(path, ink)
}

/** The note's Close icon, 48 dp, in the top right corner. */
@Composable
internal fun CloseButton(style: Palette, onClose: () -> Unit) {
    val label = stringResource(R.string.close)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClose)
            .semantics { contentDescription = label },
    ) {
        Icon(remember(style) { Doodles.icon(Doodles.CLOSE, style.pencil, width = 2f) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(28.dp))
    }
}

/** The board at the start, small, with its grid, the dots on it and any dot the mission is about. */
@Composable
internal fun BoardPicture(level: Level, modifier: Modifier) {
    val boardStyle = BoardStyle.current
    val start = remember(level) { level.newGame() }
    val marked = when (val mission = level.mission) {
        is Mission.Rescue -> mission.dot
        is Mission.Target -> mission.dot
        else -> null
    }
    BoardView(
        state = start,
        style = boardStyle,
        description = stringResource(R.string.level_board_picture),
        modifier = modifier.border(1.dp, boardStyle.grid),
        showLinks = false,
        marked = marked,
    )
}

/**
 * On a level whose board has holes, one line saying whether their edge is safe, like the board's
 * edge, or open, so a ring can go round them (decision 0034). The board draws both alike, so the
 * player learns it here before the first move. A level without holes has no line.
 */
@Composable
private fun HolesLine(level: Level, style: Palette) {
    val (label, text) = when (level.holes) {
        Holes.NONE -> return
        Holes.BORDER -> stringResource(R.string.level_holes_border_label) to stringResource(R.string.level_holes_border)
        Holes.OPEN -> stringResource(R.string.level_holes_open_label) to stringResource(R.string.level_holes_open)
    }
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(label) }
            append(' ')
            append(text)
        },
        style = NoteText.body,
        color = style.pencil,
    )
}

/** The computer's face for its strength, its name, how it plays and its head start; a boss's name in red. */
@Composable
private fun Opponent(level: Level, style: Palette, small: Boolean) {
    val boss = level.rule != null
    val name = if (boss) level.name else stringResource(R.string.player_computer)
    // A boss is sure of itself on its card; the computer shows how strong it plays.
    val face = Faces.of(if (boss) Mood.SMUG else Mood.DETERMINED, level.strength, Faces.boss(level))
    val strength = strengthText(level.strength)
    val headStart = headStartText(level)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (small) 6.dp else 8.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$name, $strength, $headStart" },
    ) {
        // The level card is a note on the map, which stays Notebook's in every theme (decision 0028).
        // On a small phone a 40 dp face leaves the head start room for one line.
        Face(face, if (small) 40.dp else 48.dp, inks = FaceInks.NOTEBOOK)
        Column {
            Text(name, style = NoteText.label, color = if (boss) style.red else style.blue)
            Text(strength, style = NoteText.small, color = style.graphite)
            Text(headStart, style = NoteText.small, color = style.graphite)
        }
    }
}

/** The dots the player may place, and "Boosters:" when the level allows any. */
@Composable
private fun Allowance(level: Level, style: Palette) {
    val dots = pluralStringResource(R.plurals.level_your_dots, level.humanDots, level.humanDots)
    val text = if (level.boosters.isEmpty()) dots else dots + " " + stringResource(R.string.level_boosters)
    Text(text, style = NoteText.small, color = style.graphite)
}

/**
 * One or two boosters the level allows, as dashed chips with a count beside the picture; each opens
 * Boosters. A kind used up on earlier attempts shows ×0 in grey, as Boosters and the tool bar show
 * none left.
 */
@Composable
private fun BoosterChips(boosters: Map<Booster, Int>, style: Palette, onBooster: (Booster) -> Unit) {
    if (boosters.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in boosters.entries.sortedBy { it.key }.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((booster, count) in row) BoosterChip(booster, count, style) { onBooster(booster) }
            }
        }
    }
}

/**
 * Three to five boosters, as the same chips in one row under the picture, in the tool bar's order:
 * each takes an equal share of the card's width, about 55 dp on a 360 dp phone and 49 dp on a 320 dp
 * one, with its icon and count in the middle.
 */
@Composable
private fun BoosterRow(boosters: Map<Booster, Int>, style: Palette, small: Boolean, onBooster: (Booster) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(if (small) 6.dp else 8.dp), modifier = Modifier.fillMaxWidth()) {
        for ((booster, count) in boosters.entries.sortedBy { it.key }) {
            BoosterChip(booster, count, style, Modifier.weight(1f), shared = true, small = small) { onBooster(booster) }
        }
    }
}

@Composable
private fun BoosterChip(
    booster: Booster,
    count: Int,
    style: Palette,
    modifier: Modifier = Modifier,
    shared: Boolean = false,
    small: Boolean = false,
    onClick: () -> Unit,
) {
    val label = pluralStringResource(R.plurals.booster_chip, count, boosterName(booster), count)
    val ink = if (count > 0) style.blue else style.graphite
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (shared) Arrangement.spacedBy(if (small) 3.dp else 4.dp, Alignment.CenterHorizontally) else Arrangement.spacedBy(4.dp),
        modifier = modifier
            .height(48.dp)
            .clip(CHIP_SHAPE)
            .toolOutline(ink, dashed = true, CHIP_SHAPE)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = label }
            .padding(horizontal = if (shared) 0.dp else 12.dp),
    ) {
        BoosterIcon(booster, ink, Modifier.size(20.dp))
        Text("×$count", style = NoteText.label, color = ink, maxLines = 1, softWrap = false)
    }
}

/** A booster's icon: the canvas's line icons, a dot beside a diamond for the double dot, and Undo's dot and loop (D-14). */
@Composable
internal fun BoosterIcon(booster: Booster, ink: Color, modifier: Modifier) {
    val icon = remember(booster, ink) {
        when (booster) {
            Booster.WALL -> Doodles.icon(Doodles.WALL, ink, width = 2f)
            Booster.HINT -> Doodles.icon(Doodles.HINT, ink, width = 2f)
            Booster.BOMB -> Doodles.icon(Doodles.BOMB, ink, width = 2f)
            Booster.DOUBLE_DOT -> Doodles.doubleDot(ink)
            Booster.UNDO -> Doodles.undoBooster(ink, width = 2f)
        }
    }
    Icon(icon, contentDescription = null, tint = Color.Unspecified, modifier = modifier)
}

/** What earns one, two and three stars: a row of stars and a line each. A level without targets has one row. */
@Composable
private fun StarTargets(level: Level, style: Palette) {
    val rows = if (level.stars == null) 1 else Stars.MAX
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (stars in 1..rows) {
            val text = starTargetText(level, stars)
            val description = stringResource(R.string.stars_row, stars, text)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
            ) {
                Row {
                    for (i in 1..Stars.MAX) {
                        val filled = i <= stars
                        Icon(
                            remember(filled, style) { Doodles.star(if (filled) style.red else style.graphite, filled) },
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Text(text, style = NoteText.body, color = style.pencil)
            }
        }
    }
}
