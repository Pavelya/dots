package dots.daily

/**
 * The player's streak: how many days' puzzles they have solved in a row, as it stands on [today].
 *
 * - A day counts once its puzzle is solved, whenever the attempt ended ([DailyRecord]).
 * - Today does not break the streak while it lasts: it may still be solved.
 * - A missed day is covered by a **freeze**, one free each week (Monday to Sunday): the first
 *   missed day of a week is frozen, and it neither adds to the streak nor ends it. A second missed
 *   day in the same week ends the streak. Freezes do not save up: an unused one is gone when its
 *   week ends.
 * - A day solved after [today] (the phone's clock was set back, or the player flew west) counts as
 *   solved; the days between it and [today] are not missed yet.
 *
 * @property days the days solved in the streak, 0 if there is none
 * @property freezeLeft true while this week's freeze has not been used
 * @property frozen the missed days the streak's freezes covered, oldest first
 */
class Streak private constructor(val days: Int, val freezeLeft: Boolean, val frozen: List<Day>) {

    override fun toString(): String = "Streak(days=$days, freezeLeft=$freezeLeft, frozen=$frozen)"

    companion object {
        /** The streak on [today], given every day whose puzzle was [solved]. */
        fun of(solved: Collection<Day>, today: Day): Streak {
            val walk = Walk(solved, today)
            return Streak(walk.run, freezeLeft = week(today) !in walk.weeksUsed, frozen = walk.frozen.toList())
        }

        /**
         * The longest streak ever, in days solved, given every day whose puzzle was [solved]: the
         * most the streak has been on any day, by the same rules as [of]. 0 if none is solved.
         */
        fun longest(solved: Collection<Day>): Int {
            if (solved.isEmpty()) return 0
            return Walk(solved, solved.max()).longest
        }

        /** The week of [day], counted in weeks from Monday 29 December 1969; weeks run Monday to Sunday. */
        fun week(day: Day): Long = Math.floorDiv(day.epochDay + 3, 7L)
    }
}

/** Goes through the days from the first solved one to [today], as [Streak.of] describes. */
private class Walk(solved: Collection<Day>, today: Day) {
    /** The streak's days solved at the end of the walk. */
    var run = 0

    /** The most [run] has been. */
    var longest = 0
    val frozen = ArrayList<Day>()
    val weeksUsed = HashSet<Long>()

    init {
        val days = solved.toSortedSet()
        if (days.isNotEmpty()) {
            val last = maxOf(days.last(), today)
            var day = days.first()
            while (day <= last) {
                when {
                    day in days -> {
                        run++
                        longest = maxOf(longest, run)
                    }
                    // Today, or a day after it, may still be solved.
                    day >= today -> Unit
                    Streak.week(day) !in weeksUsed -> {
                        weeksUsed += Streak.week(day)
                        frozen += day
                    }
                    else -> {
                        run = 0
                        frozen.clear()
                    }
                }
                // Before the next solve, a broken streak has nothing to freeze.
                if (run == 0) {
                    day = days.tailSet(day + 1).firstOrNull() ?: break
                    continue
                }
                day += 1
            }
        }
    }
}
