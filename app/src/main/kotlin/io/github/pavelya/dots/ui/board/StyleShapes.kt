package io.github.pavelya.dots.ui.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import io.github.pavelya.dots.ui.theme.PieceShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The piece styles' shapes (docs/design/cosmetics.md, "Piece styles"), in dot sizes, centred on
 * the point, y pointing down. Each is the same outline as docs/design/canvas/draw.py draws
 * (`heart_shape`, `club_shape`, `flame_shape`, `frost_shape`): a few parts, each a closed run of
 * points, which overlap into one area of ink.
 */
internal object StyleShapes {
    /** The parts of [shape], or null for a theme's own shape. */
    fun parts(shape: PieceShape): List<List<Offset>>? = when (shape) {
        PieceShape.HEART -> HEART
        PieceShape.CLUB -> CLUB
        PieceShape.FLAME -> FLAME
        PieceShape.SNOWFLAKE -> SNOWFLAKE
        else -> null
    }

    /** [shape]'s outline as one path, its parts joined, so a stroke follows only the outside. */
    fun outline(shape: PieceShape): Path? = when (shape) {
        PieceShape.HEART -> heartPath
        PieceShape.CLUB -> clubPath
        PieceShape.FLAME -> flamePath
        PieceShape.SNOWFLAKE -> snowflakePath
        else -> null
    }

    /** The box round [shape], in dot sizes. */
    fun bounds(shape: PieceShape): Rect? = parts(shape)?.flatten()?.let { points ->
        Rect(points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x }, points.maxOf { it.y })
    }

    /** The heart's area, in square dot sizes, and how many points run round it. */
    private const val HEART_AREA = 0.80
    private const val HEART_N = 64

    /**
     * The heart x = 16 sin³ t, y = −(13 cos t − 5 cos 2t − 2 cos 3t − cos 4t), scaled to
     * [HEART_AREA] and moved so its centre of mass is on the point. Its area is 180π in its own units.
     */
    private val HEART: List<List<Offset>> by lazy {
        val k = sqrt(HEART_AREA / (180 * PI))
        val pts = List(HEART_N) { i ->
            val s = 2 * PI * i / HEART_N
            16 * sin(s).pow(3) to -(13 * cos(s) - 5 * cos(2 * s) - 2 * cos(3 * s) - cos(4 * s))
        }
        val pairs = pts.zip(pts.drop(1) + pts.first())
        val a = pairs.sumOf { (p, q) -> p.first * q.second - q.first * p.second } / 2
        val cy = pairs.sumOf { (p, q) -> (p.second + q.second) * (p.first * q.second - q.first * p.second) } / (6 * a)
        listOf(wound(pts.map { (x, y) -> Offset((x * k).toFloat(), ((y - cy) * k).toFloat()) }))
    }

    /** The club: lobes, the distance of their centres from the point, the hub, and the foot's corners. */
    private const val CLUB_LOBE = 0.27
    private const val CLUB_REACH = 0.30
    private const val CLUB_HUB = 0.20
    private val CLUB_STEM = listOf(Offset(-0.07f, 0.15f), Offset(0.07f, 0.15f), Offset(0.22f, 0.58f), Offset(-0.22f, 0.58f))

    /** Three round lobes, up, lower right and lower left, round a hub, on a foot. */
    private val CLUB: List<List<Offset>> by lazy {
        val lobes = listOf(-90.0, 30.0, 150.0).map { a ->
            val r = Math.toRadians(a)
            circle(CLUB_REACH * cos(r), CLUB_REACH * sin(r), CLUB_LOBE)
        }
        (lobes + listOf(circle(0.0, 0.0, CLUB_HUB), CLUB_STEM)).map(::wound)
    }

    /**
     * The flame: six cubic curves, from the main tip clockwise, each a start point and two control
     * points; it ends at the next one's start.
     */
    private val FLAME_SIDES = listOf(
        listOf(0.08, -0.68, 0.34, -0.42, 0.50, -0.16),
        listOf(0.48, 0.12, 0.47, 0.37, 0.26, 0.52),
        listOf(0.00, 0.52, -0.27, 0.52, -0.48, 0.36),
        listOf(-0.48, 0.10, -0.48, -0.12, -0.43, -0.30),
        listOf(-0.34, -0.46, -0.28, -0.30, -0.20, -0.20),
        listOf(-0.12, -0.16, -0.13, -0.40, -0.05, -0.55),
    )

    /** A flame, point up and leaning a little right, with a smaller tongue on its left. */
    private val FLAME: List<List<Offset>> by lazy {
        val points = FLAME_SIDES.flatMapIndexed { i, side ->
            val next = FLAME_SIDES[(i + 1) % FLAME_SIDES.size]
            val xs = listOf(side[0], side[2], side[4], next[0])
            val ys = listOf(side[1], side[3], side[5], next[1])
            List(12) { k ->
                val s = k / 12.0
                val w = listOf((1 - s).pow(3), 3 * (1 - s).pow(2) * s, 3 * (1 - s) * s * s, s.pow(3))
                Offset(w.indices.sumOf { w[it] * xs[it] }.toFloat(), w.indices.sumOf { w[it] * ys[it] }.toFloat())
            }
        }
        listOf(wound(points))
    }

    /** The snowflake: arm width, its tips from the point, the hub; the twigs' width, where on the arm, and length. */
    private const val FROST_ARM = 0.22
    private const val FROST_REACH = 0.62
    private const val FROST_HUB = 0.24
    private const val FROST_TWIG = 0.15
    private const val FROST_TWIG_AT = 0.36
    private const val FROST_TWIG_LONG = 0.20

    /** Six arms with round tips, one straight up, round a hub, each arm with two twigs turned 50° out. */
    private val SNOWFLAKE: List<List<Offset>> by lazy {
        val twigs = (-90 until 270 step 60).flatMap { a ->
            val r = Math.toRadians(a.toDouble())
            val base = Offset((FROST_TWIG_AT * cos(r)).toFloat(), (FROST_TWIG_AT * sin(r)).toFloat())
            listOf(-50, 50).map { turn -> capsule((a + turn).toDouble(), FROST_TWIG_LONG, FROST_TWIG, 6, start = base) }
        }
        val arms = listOf(90.0, 30.0, -30.0).map { capsule(it, FROST_REACH, FROST_ARM) }
        (arms + listOf(circle(0.0, 0.0, FROST_HUB)) + twigs).map(::wound)
    }

    private val heartPath by lazy { union(HEART) }
    private val clubPath by lazy { union(CLUB) }
    private val flamePath by lazy { union(FLAME) }
    private val snowflakePath by lazy { union(SNOWFLAKE) }

    private fun union(parts: List<List<Offset>>): Path = parts.map(::polygon).reduce { joined, part ->
        Path().apply { op(joined, part, PathOperation.Union) }
    }

    private fun polygon(points: List<Offset>): Path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (p in points.drop(1)) lineTo(p.x, p.y)
        close()
    }

    /** [points] turned one way round, so that overlapping parts of one piece add up. */
    private fun wound(points: List<Offset>): List<Offset> {
        val area2 = points.indices.sumOf { i ->
            val p = points[i]
            val q = points[(i + 1) % points.size]
            (p.x * q.y - q.x * p.y).toDouble()
        }
        return if (area2 > 0) points else points.reversed()
    }

    private fun circle(cx: Double, cy: Double, r: Double, n: Int = 32): List<Offset> = List(n) { k ->
        val a = 2 * PI * k / n
        Offset((cx + r * cos(a)).toFloat(), (cy + r * sin(a)).toFloat())
    }

    /**
     * A bar [width] wide with round ends, at [angle] degrees: through the centre, its tips [reach]
     * from it; or from [start], reaching [reach] from there.
     */
    private fun capsule(angle: Double, reach: Double, width: Double, n: Int = 10, start: Offset? = null): List<Offset> {
        val a = Math.toRadians(angle)
        val r = width / 2
        val ux = cos(a)
        val uy = sin(a)
        if (start != null) {
            val mx = start.x + ux * reach / 2
            val my = start.y + uy * reach / 2
            return capsule(angle, reach / 2, width, n).map { Offset((mx + it.x).toFloat(), (my + it.y).toFloat()) }
        }
        val nx = -uy
        val ny = ux
        return listOf(1, -1).flatMap { end ->
            val cx = ux * (reach - r) * end
            val cy = uy * (reach - r) * end
            List(n + 1) { k ->
                val b = PI * k / n
                Offset(
                    (cx + (nx * cos(b) + ux * sin(b)) * r * end).toFloat(),
                    (cy + (ny * cos(b) + uy * sin(b)) * r * end).toFloat(),
                )
            }
        }
    }
}
