package dots.campaign

/**
 * The player's title, shown beside the stars on the map and on Stats. It comes from the stars
 * earned in the campaign (decision 0027): a title is reached at [stars] stars in total. Stars never
 * go down, so neither does the title.
 */
enum class Title(val stars: Int) {
    NOVICE(0),
    APPRENTICE(10),
    STRATEGIST(45),
    MASTER(100),
    GRANDMASTER(180),
    ;

    /** The title after this one, or null for the last. */
    val next: Title? get() = entries.getOrNull(ordinal + 1)

    companion object {
        /** The title for [stars] stars in total. */
        fun of(stars: Int): Title = entries.last { stars >= it.stars }
    }
}
