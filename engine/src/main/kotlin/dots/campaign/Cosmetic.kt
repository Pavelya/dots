package dots.campaign

/**
 * A cosmetic the player can earn by playing (decision 0030): it is earned at [stars] campaign
 * stars in total, the same count the [Title] uses, or earlier if a chest gives it
 * ([Progress.cosmetics]). The star counts are those of docs/design/money.md, "The catalogue at
 * launch". Stars never go down, so an earned cosmetic is never lost.
 *
 * [id] is the name [Progress] stores and the app uses for the item. Notebook, and the theme's own
 * pieces and ring (Standard), are not here: they are always owned. The piece styles and ring
 * effects are those of docs/design/cosmetics.md.
 */
enum class Cosmetic(val id: String, val kind: Kind, val stars: Int) {
    HEARTS_AND_CLUBS("hearts-and-clubs", Kind.PIECE_STYLE, 40),
    LASSO("lasso", Kind.RING_EFFECT, 70),
    FLAME_AND_FROST("flame-and-frost", Kind.PIECE_STYLE, 100),
    RUBBER_STAMP("rubber-stamp", Kind.RING_EFFECT, 125),
    STAR_CHART("star-chart", Kind.THEME, 150),
    NEON_ARCADE("neon-arcade", Kind.THEME, 200),
    ;

    /** What a cosmetic changes (money.md): the whole look, only the pieces, or only the rings. */
    enum class Kind { THEME, PIECE_STYLE, RING_EFFECT }

    companion object {
        /** The cosmetic named [id], or null if there is none. */
        fun of(id: String): Cosmetic? = entries.firstOrNull { it.id == id }

        /** The cosmetics earned with [stars] campaign stars, not counting chests. */
        fun earnedAt(stars: Int): Set<Cosmetic> = entries.filterTo(mutableSetOf()) { stars >= it.stars }
    }
}
