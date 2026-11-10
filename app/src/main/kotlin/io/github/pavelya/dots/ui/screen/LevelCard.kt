package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import dots.campaign.Level
import dots.campaign.Mission
import dots.campaign.Stars
import dots.campaign.Strength
import dots.engine.Booster
import io.github.pavelya.dots.R
import io.github.pavelya.dots.game.Mood
import io.github.pavelya.dots.ui.board.BoardStyle
import io.github.pavelya.dots.ui.board.BoardView

/** The hand-cut shape of a booster chip on the card. */
private val CHIP_SHAPE = RoundedCornerShape(24.dp)

/**
 * The Level card, over the map (canvas frame `level-card`; the wireframes' Level card and Boss
 * card): a page torn from the notebook with the level's name, its mission in one line, a small
 * picture of the board at the start, the computer's face for its strength and its head start, the
 * dots and boosters the player has (a booster opens [onBooster]: Boosters), and the star targets.
 * A boss level adds the boss's rule, and the boss's name and face in red. One button: Start, or
 * Resume when this level's game is [saved]. Close, or a tap on the map around the card, closes it.
 *
 * The card is drawn over the whole screen, anchored at the bottom; on a small phone it scrolls.
 */
@Composable
fun LevelCard(
    level: Level,
    saved: Boolean,
    onStart: () -> Unit,
    onBooster: (Booster) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = rememberGameStyle()
    val boss = level.rule != null
    val title = stringResource(R.string.level_title, level.world, level.number)
    BoxWithConstraints(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .fillMaxSize()
            .semantics { paneTitle = "$title, ${level.name}" },
    ) {
        // A tap on the map around the card closes it; the card above takes its own taps.
        Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onClose() } })
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(start = 10.dp, end = 10.dp, bottom = 16.dp, top = 24.dp)
                .rotate(-0.8f)
                .heightIn(max = maxHeight - 40.dp)
                .shadow(6.dp, RectangleShape, clip = false)
                .background(style.card)
                .tornTop(style.card)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
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
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                BoardPicture(level, Modifier.width(if (maxHeight < 700.dp) 96.dp else 120.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Opponent(level, style)
                    Allowance(level, style)
                    BoosterChips(level.boosters, style, onBooster)
                }
            }
            StarTargets(level, style)
            BigButton(stringResource(if (saved) R.string.level_resume else R.string.level_start), style, onStart)
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

@Composable
private fun CloseButton(style: GameStyle, onClose: () -> Unit) {
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
private fun BoardPicture(level: Level, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val boardStyle = remember(colors) { BoardStyle.plain(colors, colors.background.luminance() < 0.5f) }
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
        marked = marked,
    )
}

/** The computer's face for its strength, its name, how it plays and its head start; a boss's name in red. */
@Composable
private fun Opponent(level: Level, style: GameStyle) {
    val boss = level.rule != null
    val name = if (boss) level.name else stringResource(R.string.player_computer)
    val mood = when {
        boss -> Mood.SMUG
        level.strength <= Strength.EASY -> Mood.HAPPY
        level.strength <= Strength.NORMAL -> Mood.DETERMINED
        else -> Mood.SMUG
    }
    val strength = strengthText(level.strength)
    val headStart = headStartText(level)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$name, $strength, $headStart" },
    ) {
        Image(
            remember(mood, style) { Doodles.face(mood, style.blue, style.bluePale, style.paper) },
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        Column {
            Text(name, style = NoteText.label, color = if (boss) style.red else style.blue)
            Text(strength, style = NoteText.small, color = style.graphite)
            Text(headStart, style = NoteText.small, color = style.graphite)
        }
    }
}

/** The dots the player may place, and "Boosters:" when the level allows any. */
@Composable
private fun Allowance(level: Level, style: GameStyle) {
    val dots = pluralStringResource(R.plurals.level_your_dots, level.humanDots, level.humanDots)
    val text = if (level.boosters.isEmpty()) dots else dots + " " + stringResource(R.string.level_boosters)
    Text(text, style = NoteText.small, color = style.graphite)
}

/** The boosters the level allows, as dashed chips with a count; each opens Boosters. */
@Composable
private fun BoosterChips(boosters: Map<Booster, Int>, style: GameStyle, onBooster: (Booster) -> Unit) {
    if (boosters.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in boosters.entries.sortedBy { it.key }.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((booster, count) in row) BoosterChip(booster, count, style) { onBooster(booster) }
            }
        }
    }
}

@Composable
private fun BoosterChip(booster: Booster, count: Int, style: GameStyle, onClick: () -> Unit) {
    val label = pluralStringResource(R.plurals.booster_chip, count, boosterName(booster), count)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .height(48.dp)
            .clip(CHIP_SHAPE)
            .toolOutline(style.blue, dashed = true, CHIP_SHAPE)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = label }
            .padding(horizontal = 12.dp),
    ) {
        BoosterIcon(booster, style.blue, Modifier.size(20.dp))
        Text("×$count", style = NoteText.label, color = style.blue)
    }
}

/** A booster's icon: the canvas's line icons, and a dot beside a diamond for the double dot. */
@Composable
internal fun BoosterIcon(booster: Booster, ink: Color, modifier: Modifier) {
    val icon = remember(booster, ink) {
        when (booster) {
            Booster.WALL -> Doodles.icon(Doodles.WALL, ink, width = 2f)
            Booster.HINT -> Doodles.icon(Doodles.HINT, ink, width = 2f)
            Booster.BOMB -> Doodles.icon(Doodles.BOMB, ink, width = 2f)
            Booster.DOUBLE_DOT -> Doodles.doubleDot(ink)
        }
    }
    Icon(icon, contentDescription = null, tint = Color.Unspecified, modifier = modifier)
}

/** What earns one, two and three stars: a row of stars and a line each. A level without targets has one row. */
@Composable
private fun StarTargets(level: Level, style: GameStyle) {
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
