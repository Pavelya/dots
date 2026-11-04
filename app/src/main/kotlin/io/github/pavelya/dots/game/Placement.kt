package io.github.pavelya.dots.game

import dots.engine.Pos

/**
 * Placing a dot takes two taps, so a finger that lands on the wrong point costs nothing: the first
 * tap on a playable point shows a preview, a tap on the same point places it, a tap on another
 * playable point moves the preview, and a tap anywhere else clears it.
 */
object Placement {
    data class Result(val preview: Pos?, val placed: Pos?)

    fun onTap(preview: Pos?, tapped: Pos?, isPlayable: (Pos) -> Boolean): Result = when {
        tapped == null || !isPlayable(tapped) -> Result(preview = null, placed = null)
        tapped == preview -> Result(preview = null, placed = tapped)
        else -> Result(preview = tapped, placed = null)
    }
}
