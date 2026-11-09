package dots.engine

/**
 * Enemy dots won by [by] when a ring closed.
 *
 * [dots] are the newly captured dots, [region] is every cell inside the ring (empty cells and
 * dots alike); the region is out of play from now on. UI code can use both to animate the capture.
 * [points] is what the capture adds to [by]'s score: one per dot, two for a dot on a bonus point.
 */
class Capture(val by: Player, val dots: List<Pos>, val region: List<Pos>, val points: Int = dots.size)

/** The position after a move plus every capture that move triggered, in the order they were resolved. */
class MoveResult(val state: GameState, val captures: List<Capture>)
