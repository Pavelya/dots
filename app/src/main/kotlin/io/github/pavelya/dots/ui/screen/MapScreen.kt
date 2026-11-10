package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dots.campaign.CampaignMap
import dots.campaign.Level
import dots.campaign.LevelState
import dots.campaign.LevelStop
import dots.campaign.WorldStop
import io.github.pavelya.dots.R
import kotlin.math.sin

/** Where the map's entries lead (docs/design/flows.md, "Navigation in short"). */
class MapActions(
    val onLevel: (Level) -> Unit,
    val onDaily: () -> Unit,
    val onPlay: () -> Unit,
    val onRules: () -> Unit,
    val onStats: () -> Unit,
    val onShop: () -> Unit,
    val onSettings: () -> Unit,
)

/** The scrolling path of the map, for tests. */
const val MAP_PATH_TAG = "map-path"

/**
 * Home: the world map, laid out as the canvas's map frame (docs/design/canvas, "World map"). One
 * path through every world scrolls from World 1 at the bottom upwards. Over it stay the world in
 * view with its stars (top left), Settings (top right), Today's puzzle as a taped note, Shop, Stats
 * and Rules in the left margin, and Play a game at the bottom right. The map opens with the next
 * level low on the screen, near the thumb. A level opens its Level card; a locked level or world
 * says what opens it. There are no Play buttons on levels.
 *
 * [map] is null while the progress is read. The inks are the plain POC ones ([MapStyle.plain]);
 * the Notebook theme (V-02, V-03) brings the paper, fonts and hand-drawn icons, V-11 the landscape,
 * A-04 the chest and the player's title, A-07 the streak on the puzzle note.
 */
@Composable
fun MapScreen(map: CampaignMap?, actions: MapActions) {
    val colors = MaterialTheme.colorScheme
    val style = remember(colors) { MapStyle.plain(colors, dark = colors.background.luminance() < 0.5f) }
    var lock by remember { mutableStateOf<CampaignMap.Lock?>(null) }
    Box(Modifier.fillMaxSize().background(style.paper)) {
        if (map != null) {
            val rows = remember(map) { rows(map) }
            // The list is drawn from the bottom up. One level below the start keeps it off the
            // screen's edge; under a world's first level, that is the boss just passed, below the
            // world's banner. The place the player scrolled to is kept until the start changes, for
            // example when a level is passed and the next one is further up. (A key, not an input of
            // rememberSaveable: a saved place is restored whatever the inputs say.)
            val start = startRow(rows)
            val first = (if (rows.getOrNull(start - 1) is MapRow.Banner) start - 2 else start - 1).coerceAtLeast(0)
            val state = key(first) { rememberLazyListState(initialFirstVisibleItemIndex = first) }
            MapPath(
                rows = rows,
                state = state,
                style = style,
                onLevel = { stop -> map.lock(stop.level)?.let { lock = it } ?: actions.onLevel(stop.level) },
                onLockedWorld = { lock = CampaignMap.Lock.WorldClosed(it) },
            )
            // The header names the world of the next level while it is on the screen, otherwise the
            // world in the middle of the screen; never a locked one, whose banner says what opens it.
            val inView by remember(map, rows, state) {
                derivedStateOf {
                    val visible = state.layoutInfo.visibleItemsInfo
                    val next = visible.firstOrNull { (rows[it.index] as? MapRow.Stop)?.isNext == true }
                    var world = worldOf(rows, next?.index ?: visible.getOrNull(visible.size / 2)?.index ?: first)
                    while (world > 1 && !map.worlds[world - 1].isOpen) world--
                    world
                }
            }
            Header(map.worlds[inView - 1], style)
        }
        TopIcon(
            Doodles.SETTINGS, stringResource(R.string.home_settings), style, actions.onSettings,
            Modifier.align(Alignment.TopEnd).padding(top = 26.dp, end = 6.dp),
        )
        DailyNote(style, actions.onDaily, Modifier.align(Alignment.TopEnd).padding(top = 120.dp, end = 12.dp))
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 1.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MarginIcon(Doodles.SHOP, stringResource(R.string.map_shop), style, actions.onShop)
            MarginIcon(Doodles.STATS, stringResource(R.string.map_stats), style, actions.onStats)
            MarginIcon(Doodles.HELP, stringResource(R.string.map_rules_icon), style, actions.onRules)
        }
        PlayAGame(style, actions.onPlay, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 26.dp))
    }
    lock?.let { LockNote(it, onClose = { lock = null }) }
}

/** The map's rows, from the bottom: each world's banner, then its levels; at the top, the end. */
private sealed interface MapRow {
    val key: String

    data class Banner(val world: WorldStop) : MapRow {
        override val key get() = "world-${world.world.number}"
    }

    /**
     * A level and its place across the screen, [x], as a share of the width. The path goes on to
     * [nextX], [nextHeight] and [nextOpen] (the next level's place, row height and whether it is
     * open); [nextX] is null after a world's last level.
     */
    data class Stop(
        val stop: LevelStop,
        val x: Float,
        val isNext: Boolean,
        val nextX: Float?,
        val nextHeight: Dp,
        val nextOpen: Boolean,
    ) : MapRow {
        override val key get() = stop.level.code
        val height: Dp get() = if (stop.isBoss) BOSS_HEIGHT else STOP_HEIGHT
    }

    data object End : MapRow {
        override val key get() = "end"
    }
}

private val STOP_HEIGHT = 72.dp
private val BOSS_HEIGHT = 128.dp
private val STOP_SIZE = 44.dp
private val TOUCH_SIZE = 56.dp
private val CASTLE_WIDTH = 68.dp
private val CASTLE_HEIGHT = 96.dp

/**
 * Where a level sits across the screen, as a share of its width: the path wanders between the
 * margin icons on the left and the right edge, as on the canvas (x from about 112 to 300 of 360 dp).
 */
private fun pathX(index: Int): Float = 0.57f + 0.25f * sin(index * 1.1f + 3.6f)

private fun rows(map: CampaignMap): List<MapRow> {
    val next = map.next
    val rows = ArrayList<MapRow>()
    for (world in map.worlds) {
        rows += MapRow.Banner(world)
        world.levels.forEachIndexed { i, stop ->
            val after = world.levels.getOrNull(i + 1)
            rows += MapRow.Stop(
                stop = stop,
                x = pathX(i),
                isNext = stop.level == next,
                nextX = after?.let { pathX(i + 1) },
                nextHeight = if (after?.isBoss == true) BOSS_HEIGHT else STOP_HEIGHT,
                nextOpen = after != null && after.state != LevelState.LOCKED,
            )
        }
    }
    rows += MapRow.End
    return rows
}

/**
 * The row to open at: the next level; with none, the first locked world (it needs stars), or the
 * end of the path.
 */
private fun startRow(rows: List<MapRow>): Int {
    val next = rows.indexOfFirst { it is MapRow.Stop && it.isNext }
    if (next >= 0) return next
    val locked = rows.indexOfFirst { it is MapRow.Banner && !it.world.isOpen }
    return if (locked >= 0) locked else rows.lastIndex
}

/** The number of the world a row belongs to. */
private fun worldOf(rows: List<MapRow>, index: Int): Int {
    for (i in index.coerceIn(0, rows.lastIndex) downTo 0) {
        when (val row = rows[i]) {
            is MapRow.Banner -> return row.world.world.number
            is MapRow.Stop -> return row.stop.level.world
            MapRow.End -> Unit
        }
    }
    return 1
}

@Composable
private fun MapPath(
    rows: List<MapRow>,
    state: LazyListState,
    style: MapStyle,
    onLevel: (LevelStop) -> Unit,
    onLockedWorld: (WorldStop) -> Unit,
) {
    LazyColumn(
        state = state,
        reverseLayout = true,
        // Room for the header and the puzzle note above, and Play a game below.
        contentPadding = PaddingValues(top = 184.dp, bottom = 84.dp),
        modifier = Modifier.fillMaxSize().testTag(MAP_PATH_TAG),
    ) {
        items(rows.size, key = { rows[it].key }, contentType = { rows[it]::class }) { i ->
            when (val row = rows[i]) {
                is MapRow.Banner -> Banner(row.world, style, onLocked = { onLockedWorld(row.world) })
                is MapRow.Stop -> Stop(row, style, onOpen = { onLevel(row.stop) })
                MapRow.End -> Text(
                    stringResource(R.string.map_more_coming),
                    style = MaterialTheme.typography.bodyLarge,
                    color = style.graphite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 72.dp),
                )
            }
        }
    }
}

/** The world in view, its name and stars, over the top of the map. */
@Composable
private fun Header(world: WorldStop, style: MapStyle) {
    val most = world.world.maxStars
    val stars = pluralStringResource(R.plurals.map_world_stars, most, world.stars, most)
    val number = stringResource(R.string.map_world_number, world.world.number)
    val description = "$number, ${world.world.name}, $stars"
    // Paper behind the header, so the path scrolls away under it.
    Box(
        Modifier
            .fillMaxWidth()
            .height(124.dp)
            .background(Brush.verticalGradient(0f to style.paper, 0.85f to style.paper, 1f to style.paper.copy(alpha = 0f))),
    )
    Column(
        Modifier
            .padding(start = 70.dp, top = 26.dp)
            .clearAndSetSemantics {
                contentDescription = description
                heading()
            },
    ) {
        Text(number, style = MaterialTheme.typography.labelLarge, color = style.graphite)
        Text(world.world.name, style = MaterialTheme.typography.displaySmall, color = style.done, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Doodles.star(style.next, filled = true), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
            Text(
                stringResource(R.string.map_world_stars_short, world.stars, most),
                style = MaterialTheme.typography.labelLarge,
                color = style.pencil,
            )
        }
    }
}

@Composable
private fun TopIcon(path: String, label: String, style: MapStyle, onClick: () -> Unit, modifier: Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Icon(remember(path, style) { Doodles.icon(path, style.pencil) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(26.dp))
    }
}

/** Today's puzzle: a note taped to the page. A-07 adds the streak. */
@Composable
private fun DailyNote(style: MapStyle, onClick: () -> Unit, modifier: Modifier) {
    Box(modifier.rotate(3f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .size(width = 158.dp, height = 56.dp)
                .shadow(4.dp, RectangleShape, clip = false)
                .background(style.card)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 8.dp),
        ) {
            Icon(remember(style) { Doodles.icon(Doodles.CALENDAR, style.next) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
            Text(stringResource(R.string.map_daily), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = style.pencil)
        }
        // The strip of tape.
        Box(Modifier.offset(x = 40.dp, y = (-8).dp).rotate(-7f).size(width = 44.dp, height = 16.dp).background(style.tape))
    }
}

@Composable
private fun MarginIcon(path: String, label: String, style: MapStyle, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        modifier = Modifier
            .size(width = 56.dp, height = 52.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Icon(remember(path, style) { Doodles.icon(path, style.done) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = style.graphite)
    }
}

/** The canvas's hand-cut button shape. */
private val BUTTON_SHAPE = RoundedCornerShape(topStart = 16.dp, topEnd = 20.dp, bottomEnd = 14.dp, bottomStart = 22.dp)

@Composable
private fun PlayAGame(style: MapStyle, onClick: () -> Unit, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .height(52.dp)
            .clip(BUTTON_SHAPE)
            .background(style.card)
            .border(BorderStroke(2.dp, style.graphite), BUTTON_SHAPE)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Icon(remember(style) { Doodles.people(style.next, style.done) }, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(width = 28.dp, height = 22.dp))
        Text(stringResource(R.string.map_play), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = style.pencil)
    }
}

/** Between worlds: the next world's name, and what opens it while it is locked. */
@Composable
private fun Banner(world: WorldStop, style: MapStyle, onLocked: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp, end = 16.dp, top = 24.dp, bottom = 24.dp)
            .then(if (world.isOpen) Modifier.semantics(mergeDescendants = true) {} else Modifier.clickable(role = Role.Button, onClick = onLocked)),
    ) {
        Text(stringResource(R.string.map_world_number, world.world.number), style = MaterialTheme.typography.labelLarge, color = style.graphite)
        Text(
            world.world.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (world.isOpen) style.done else style.graphite,
        )
        if (!world.isOpen) {
            for (line in lockLines(world)) {
                Text(line, style = MaterialTheme.typography.bodyMedium, color = style.pencil, textAlign = TextAlign.Center)
            }
        }
    }
}

/** What a locked world waits for: the previous boss, the stars, or both. */
@Composable
private fun lockLines(world: WorldStop): List<String> = buildList {
    world.bossNeeded?.let { add(stringResource(R.string.map_locked_boss, it.code, it.name)) }
    if (world.starsNeeded > 0) add(stringResource(R.string.map_locked_stars, world.world.starsToOpen, world.world.starsToOpen - world.starsNeeded))
}

@Composable
private fun Stop(row: MapRow.Stop, style: MapStyle, onOpen: () -> Unit) {
    val stop = row.stop
    val description = stopDescription(row)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(row.height)
            .drawBehind { row.nextX?.let { drawPathTo(row, it, style) } },
    ) {
        val centerX = maxWidth * row.x
        val centerY = row.height / 2
        val width = if (stop.isBoss) CASTLE_WIDTH else TOUCH_SIZE
        val height = if (stop.isBoss) CASTLE_HEIGHT else TOUCH_SIZE
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(x = centerX - width / 2, y = centerY - height / 2)
                .size(width, height)
                .clickable(onClick = onOpen)
                .clearAndSetSemantics {
                    contentDescription = description
                    role = Role.Button
                    onClick { onOpen(); true }
                },
        ) {
            if (stop.isBoss) Castle(row, style) else Node(row, style)
        }
        // The stars sit above a passed level, as on the canvas.
        if (stop.state == LevelState.PASSED) {
            val top = if (stop.isBoss) centerY - height / 2 - 14.dp else centerY - STOP_SIZE / 2 - 16.dp
            Row(Modifier.offset(x = centerX - 20.dp, y = top).clearAndSetSemantics { }) {
                repeat(3) {
                    val filled = it < stop.stars
                    Icon(
                        Doodles.star(if (filled) style.next else style.graphite, filled),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
        if (row.isNext) NextLabel(row, style, centerX, centerY)
    }
}

/** A level's stop: blue when passed, ringed twice in red when next, dashed while locked. */
@Composable
private fun Node(row: MapRow.Stop, style: MapStyle) {
    val stop = row.stop
    val ink = when {
        stop.state == LevelState.PASSED -> style.onDone
        row.isNext || stop.state == LevelState.OPEN -> style.next
        else -> style.graphite
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(TOUCH_SIZE)) {
        Canvas(Modifier.matchParentSize()) {
            val r = STOP_SIZE.toPx() / 2
            when {
                stop.state == LevelState.PASSED -> drawCircle(style.done, r)
                row.isNext -> {
                    val big = TOUCH_SIZE.toPx() / 2
                    drawCircle(style.card, big)
                    drawCircle(style.next, big - 1.5.dp.toPx(), style = Stroke(3.dp.toPx()))
                    drawCircle(style.paper, big + 2.dp.toPx(), style = Stroke(4.dp.toPx()))
                    drawCircle(style.next, big + 5.25.dp.toPx(), style = Stroke(2.5.dp.toPx()))
                }
                stop.state == LevelState.OPEN -> {
                    drawCircle(style.card, r)
                    drawCircle(style.next, r - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
                }
                else -> {
                    drawCircle(style.paper, r)
                    drawCircle(
                        style.graphite,
                        r - 1.dp.toPx(),
                        style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                    )
                }
            }
        }
        Text(
            "${stop.level.number}",
            color = ink,
            fontSize = if (row.isNext) 20.sp else 16.sp,
            fontWeight = if (row.isNext || stop.state == LevelState.PASSED) FontWeight.ExtraBold else FontWeight.Bold,
        )
    }
}

/** A boss level: the castle, in the ink of its state, and "Boss 15" under it. */
@Composable
private fun Castle(row: MapRow.Stop, style: MapStyle) {
    val ink = when {
        row.stop.state == LevelState.PASSED -> style.done
        row.isNext -> style.next
        else -> style.pencil
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            remember(ink, style) { Doodles.castle(ink, style.paper, style.next) },
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(width = 68.dp, height = 76.dp),
        )
        Text(stringResource(R.string.map_boss, row.stop.level.number), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = style.pencil)
    }
}

/** "next!" in red beside the next level, with an arrow to it, on the side with more room. */
@Composable
private fun NextLabel(row: MapRow.Stop, style: MapStyle, centerX: Dp, centerY: Dp) {
    val onLeft = row.x > 0.5f
    val reach = (if (row.stop.isBoss) CASTLE_WIDTH / 2 else TOUCH_SIZE / 2 + 6.dp) + 4.dp
    val width = 108.dp
    val arrow = remember(style) { Doodles.arrow(style.next) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (onLeft) Arrangement.End else Arrangement.Start,
        modifier = Modifier
            .offset(x = if (onLeft) centerX - reach - width else centerX + reach, y = centerY - 16.dp)
            .width(width)
            .clearAndSetSemantics { },
    ) {
        // The arrow points left as drawn: towards a level on the left of the label. Mirrored, it
        // points right.
        val arrowIcon = @Composable {
            Icon(
                arrow,
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(width = 30.dp, height = 18.dp).graphicsLayer { scaleX = if (onLeft) -1f else 1f },
            )
        }
        if (!onLeft) arrowIcon()
        Text(
            stringResource(R.string.map_next_hand),
            color = style.next,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
        )
        if (onLeft) arrowIcon()
    }
}

@Composable
private fun stopDescription(row: MapRow.Stop): String {
    val stop = row.stop
    val parts = mutableListOf(stringResource(R.string.map_level, stop.level.code))
    if (stop.isBoss) parts += stop.level.name
    when {
        row.isNext -> parts += stringResource(R.string.map_next)
        stop.state == LevelState.LOCKED -> parts += stringResource(R.string.map_locked)
        stop.state == LevelState.PASSED -> parts += pluralStringResource(R.plurals.map_stars, stop.stars, stop.stars)
        else -> Unit
    }
    return parts.joinToString(", ")
}

/**
 * The stretch of path from this level up to the next one: inked and dashed up to an open level,
 * dotted in graphite beyond it. The stops are drawn over its ends.
 */
private fun DrawScope.drawPathTo(row: MapRow.Stop, nextX: Float, style: MapStyle) {
    val from = Offset(size.width * row.x, size.height / 2)
    // A boss's path ends under its castle's label, not through it.
    val end = if (row.nextHeight == BOSS_HEIGHT) -row.nextHeight.toPx() / 2 + CASTLE_HEIGHT.toPx() / 2 else -row.nextHeight.toPx() / 2
    val to = Offset(size.width * nextX, end)
    val bend = (from.y - to.y) / 2
    val path = Path().apply {
        moveTo(from.x, from.y)
        cubicTo(from.x, from.y - bend, to.x, to.y + bend, to.x, to.y)
    }
    val inked = row.nextOpen
    drawPath(
        path,
        color = if (inked) style.done else style.graphite,
        style = Stroke(
            width = 2.2.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = if (inked) {
                PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 6.dp.toPx()))
            } else {
                PathEffect.dashPathEffect(floatArrayOf(0.01f, 7.dp.toPx()))
            },
        ),
    )
}

/** A note over the map on why a level or world is locked. */
@Composable
private fun LockNote(lock: CampaignMap.Lock, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Note {
            when (lock) {
                is CampaignMap.Lock.PassFirst -> {
                    Text(stringResource(R.string.map_locked_level_title), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.map_pass_first, lock.level.code), style = MaterialTheme.typography.bodyLarge)
                }
                is CampaignMap.Lock.WorldClosed -> {
                    Text(stringResource(R.string.map_world, lock.world.world.number, lock.world.world.name), style = MaterialTheme.typography.headlineSmall)
                    for (line in lockLines(lock.world)) Text(line, style = MaterialTheme.typography.bodyLarge)
                }
            }
            TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
        }
    }
}
