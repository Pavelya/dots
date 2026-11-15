package dots.daily

import java.util.TimeZone

/**
 * The daily puzzle's calendar: a new puzzle at each midnight of the phone's own time zone. Takes
 * the time ([millis] since 1970, UTC) and the zone as they are now, so it follows the phone when
 * the player travels or the clocks change.
 */
object DailyClock {
    const val DAY_MILLIS = 86_400_000L

    /** The day it is at [millis] in [zone]. */
    fun today(millis: Long, zone: TimeZone): Day =
        Day.ofEpochDay(Math.floorDiv(millis + zone.getOffset(millis), DAY_MILLIS))

    /**
     * Milliseconds from [millis] to the next midnight in [zone], when the next puzzle comes; 1 or
     * more. A day that the clocks change on is an hour longer or shorter.
     */
    fun toNextPuzzle(millis: Long, zone: TimeZone): Long {
        val tomorrow = today(millis, zone).epochDay + 1
        // Midnight in UTC of tomorrow's date, moved by the zone's offset at that moment. The first
        // guess uses the offset now; the second, the offset at the first guess.
        val guess = tomorrow * DAY_MILLIS - zone.getOffset(millis)
        val midnight = tomorrow * DAY_MILLIS - zone.getOffset(guess)
        return (midnight - millis).coerceAtLeast(1)
    }
}
