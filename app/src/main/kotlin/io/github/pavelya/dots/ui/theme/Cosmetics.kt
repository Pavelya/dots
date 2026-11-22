package io.github.pavelya.dots.ui.theme

import io.github.pavelya.dots.data.PieceStyleId
import io.github.pavelya.dots.data.RingEffectId

/**
 * This board with a piece style and a ring effect in place of the theme's own (docs/design/
 * cosmetics.md): [pieces] changes only the two sides' pieces, [rings] only how rings are drawn and
 * captures move. Everything else, the inks and every mark among them, stays the theme's. Standard
 * leaves the board as it is.
 */
fun BoardLook.dressed(pieces: PieceStyleId, rings: RingEffectId): BoardLook {
    val (red, blue) = when (pieces) {
        PieceStyleId.STANDARD -> red to blue
        PieceStyleId.HEARTS_AND_CLUBS -> Piece(PieceShape.HEART) to Piece(PieceShape.CLUB)
        PieceStyleId.FLAME_AND_FROST -> Piece(PieceShape.FLAME) to Piece(PieceShape.SNOWFLAKE)
    }
    val effect = when (rings) {
        RingEffectId.STANDARD -> RingEffect.THEME
        RingEffectId.LASSO -> RingEffect.LASSO
        RingEffectId.RUBBER_STAMP -> RingEffect.STAMP
    }
    if (red == this.red && blue == this.blue && effect == ringEffect) return this
    return copy(red = red, blue = blue, ringEffect = effect)
}
