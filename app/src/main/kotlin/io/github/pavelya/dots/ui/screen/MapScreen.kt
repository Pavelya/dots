package io.github.pavelya.dots.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dots.campaign.CampaignMap
import dots.campaign.Level
import dots.campaign.LevelState
import dots.campaign.LevelStop
import dots.campaign.WorldStop
import io.github.pavelya.dots.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
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

/** The scrolling list of the map, for tests. */
const val MAP_PATH_TAG = "map-path"

/**
 * Home: the world map. One path through every world, from World 1 at the bottom upwards, with a
 * section per world. It opens with the next level low on the screen, near the thumb. A level opens
 * its Level card; a locked level or world says what opens it. There are no Play buttons.
 *
 * [map] is null while the progress is read. The look is the plain POC one until the Notebook
 * theme (V-02, V-03) draws the landscape.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapScreen(map: CampaignMap?, actions: MapActions) {
    var lock by remember { mutableStateOf<CampaignMap.Lock?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            if (map != null) TotalStars(map.totalStars)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        ) {
            Entry(R.string.map_daily, actions.onDaily)
            Entry(R.string.map_play, actions.onPlay)
            Entry(R.string.map_rules, actions.onRules)
            Entry(R.string.map_stats, actions.onStats)
            Entry(R.string.map_shop, actions.onShop)
            Entry(R.string.home_settings, actions.onSettings)
        }
        if (map != null) {
            MapPath(
                map = map,
                onLevel = { stop -> map.lock(stop.level)?.let { lock = it } ?: actions.onLevel(stop.level) },
                onLockedWorld = { lock = CampaignMap.Lock.WorldClosed(it) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }
    lock?.let { LockNote(it, onClose = { lock = null }) }
}

/** The map's rows, from the bottom: each world's banner, then its levels; at the top, the end. */
private sealed interface MapRow {
    val key: String

    data class Banner(val world: WorldStop) : MapRow {
        override val key get() = "world-${world.world.number}"
    }

    /** A level and its place across the screen, [x]; [nextX] is where the path goes next, null after a world's last level. */
    data class Stop(val stop: LevelStop, val x: Float, val nextX: Float?, val isNext: Boolean) : MapRow {
        override val key get() = stop.level.code
    }

    data object End : MapRow {
        override val key get() = "end"
    }
}

/** Where a level sits across the screen, as a share of its width: the path swings left and right. */
private fun pathX(index: Int): Float = 0.5f + 0.3f * sin(index * 0.9f + 0.4f)

private fun rows(map: CampaignMap): List<MapRow> {
    val next = map.next
    val rows = ArrayList<MapRow>()
    for (world in map.worlds) {
        rows += MapRow.Banner(world)
        world.levels.forEachIndexed { i, stop ->
            val nextX = if (i < world.levels.lastIndex) pathX(i + 1) else null
            rows += MapRow.Stop(stop, pathX(i), nextX, stop.level == next)
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

private val STOP_HEIGHT = 88.dp
private val NODE_SIZE = 56.dp

@Composable
private fun MapPath(map: CampaignMap, onLevel: (LevelStop) -> Unit, onLockedWorld: (WorldStop) -> Unit, modifier: Modifier) {
    val rows = remember(map) { rows(map) }
    // The list is drawn from the bottom up. One level below the start keeps it off the screen's
    // edge; under a world's first level, that is the boss just passed, below the world's banner.
    // The place the player scrolled to is kept until the start changes, for example when a level
    // is passed and the next one is further up.
    val start = startRow(rows)
    val first = (if (rows.getOrNull(start - 1) is MapRow.Banner) start - 2 else start - 1).coerceAtLeast(0)
    // (A key, not an input of rememberSaveable: a saved place is restored whatever the inputs say.)
    val state: LazyListState = key(first) { rememberLazyListState(initialFirstVisibleItemIndex = first) }
    LazyColumn(
        state = state,
        reverseLayout = true,
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
        modifier = modifier.testTag(MAP_PATH_TAG),
    ) {
        items(rows.size, key = { rows[it].key }, contentType = { rows[it]::class }) { i ->
            when (val row = rows[i]) {
                is MapRow.Banner -> Banner(row.world, onLocked = { onLockedWorld(row.world) })
                is MapRow.Stop -> Stop(row, onOpen = { onLevel(row.stop) })
                MapRow.End -> Text(
                    stringResource(R.string.map_more_coming),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun Banner(world: WorldStop, onLocked: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val title = stringResource(R.string.map_world, world.world.number, world.world.name)
    Surface(
        color = if (world.isOpen) colors.secondaryContainer else colors.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .then(if (world.isOpen) Modifier else Modifier.clickable(onClickLabel = title, onClick = onLocked)),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (world.isOpen) {
                    val most = world.world.maxStars
                    val description = pluralStringResource(R.plurals.map_world_stars, most, world.stars, most)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clearAndSetSemantics { contentDescription = description }) {
                        Star(filled = true, size = 18.dp)
                        Text(" ${world.stars} / $most", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            if (!world.isOpen) {
                for (line in lockLines(world)) Text(line, style = MaterialTheme.typography.bodyMedium)
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
private fun Stop(row: MapRow.Stop, onOpen: () -> Unit) {
    val stop = row.stop
    val colors = MaterialTheme.colorScheme
    val passed = stop.state == LevelState.PASSED
    val locked = stop.state == LevelState.LOCKED
    val ink = if (passed) colors.primary else colors.outline
    val description = stopDescription(row)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(STOP_HEIGHT)
            .drawBehind { row.nextX?.let { drawPathTo(row.x, it, ink, dashed = !passed) } },
    ) {
        val nodeLeft = maxWidth * row.x - NODE_SIZE / 2
        val shape: Shape = if (stop.isBoss) RoundedCornerShape(10.dp) else CircleShape
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset(x = nodeLeft, y = (STOP_HEIGHT - NODE_SIZE) / 2)
                .width(NODE_SIZE)
                .clickable(onClick = onOpen)
                .clearAndSetSemantics {
                    contentDescription = description
                    role = Role.Button
                    onClick { onOpen(); true }
                },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(NODE_SIZE)
                    .then(if (row.isNext) Modifier.border(BorderStroke(3.dp, colors.primary), shape).padding(6.dp) else Modifier)
                    .background(
                        when {
                            passed -> colors.primary
                            locked -> colors.surfaceVariant
                            else -> colors.surface
                        },
                        shape,
                    )
                    .then(if (!passed && !locked) Modifier.border(BorderStroke(2.dp, colors.primary), shape) else Modifier),
            ) {
                Text(
                    "${stop.level.number}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (row.isNext) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        passed -> colors.onPrimary
                        locked -> colors.onSurfaceVariant.copy(alpha = 0.6f)
                        else -> colors.primary
                    },
                )
            }
            if (passed) Stars(stop.stars)
        }
        // A label beside the node: "next" on the next level, the boss's name on a boss level.
        val label = when {
            row.isNext -> stringResource(R.string.map_next)
            stop.isBoss -> stop.level.name
            else -> null
        }
        if (label != null) {
            val onLeft = row.x > 0.5f
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (row.isNext) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = if (onLeft) nodeLeft - 96.dp else nodeLeft + NODE_SIZE + 8.dp)
                    .width(88.dp)
                    .clearAndSetSemantics { },
                textAlign = if (onLeft) TextAlign.End else TextAlign.Start,
            )
        }
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

/** The stretch of path from this level's node up to the next one's, stopping short of both. */
private fun DrawScope.drawPathTo(x: Float, nextX: Float, ink: Color, dashed: Boolean) {
    val from = Offset(size.width * x, size.height / 2)
    val to = Offset(size.width * nextX, -size.height / 2)
    val length = hypot(to.x - from.x, to.y - from.y)
    val gap = (NODE_SIZE / 2 + 6.dp).toPx()
    if (length <= 2 * gap) return
    val dx = (to.x - from.x) / length
    val dy = (to.y - from.y) / length
    drawLine(
        color = ink,
        start = Offset(from.x + dx * gap, from.y + dy * gap),
        end = Offset(to.x - dx * gap, to.y - dy * gap),
        strokeWidth = 3.dp.toPx(),
        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())) else null,
    )
}

/** Up to three stars under a passed level. */
@Composable
private fun Stars(earned: Int) {
    Row(Modifier.padding(top = 2.dp)) {
        repeat(3) { Star(filled = it < earned, size = 14.dp) }
    }
}

@Composable
private fun TotalStars(total: Int) {
    val description = pluralStringResource(R.plurals.map_stars, total, total)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clearAndSetSemantics { contentDescription = description }) {
        Star(filled = true, size = 22.dp)
        Spacer(Modifier.width(4.dp))
        Text("$total", style = MaterialTheme.typography.titleLarge)
    }
}

/** A five-pointed star, drawn here until the hand-drawn icons arrive (V-03). */
@Composable
private fun Star(filled: Boolean, size: Dp) {
    val colors = MaterialTheme.colorScheme
    val fill = if (filled) colors.tertiary else colors.outlineVariant
    Canvas(Modifier.size(size)) {
        val outer = this.size.minDimension / 2
        val inner = outer * 0.45f
        val star = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val angle = -PI / 2 + i * PI / 5
            val x = center.x + r * cos(angle).toFloat()
            val y = center.y + r * sin(angle).toFloat()
            if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        drawPath(star, fill)
    }
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

@Composable
private fun Entry(@StringRes label: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(stringResource(label)) }
}
