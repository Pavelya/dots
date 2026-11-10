package dots.daily

/**
 * A calendar day (the Gregorian calendar, no time zone), written `2027-01-31`. The engine keeps
 * its own small date type because `java.time` is missing on the oldest phones the app supports.
 */
data class Day(val year: Int, val month: Int, val day: Int) : Comparable<Day> {
    init {
        require(year in 1..9999) { "year must be 1 to 9999" }
        require(month in 1..12) { "month must be 1 to 12" }
        require(day in 1..daysIn(year, month)) { "$year-$month has no day $day" }
    }

    /** Days since 1970-01-01, as `java.time.LocalDate.toEpochDay` counts them. */
    val epochDay: Long
        get() {
            // Howard Hinnant's days_from_civil: years start on 1 March, so the leap day comes last.
            val y = (if (month <= 2) year - 1 else year).toLong()
            val era = Math.floorDiv(y, 400L)
            val yearOfEra = y - era * 400
            val dayOfYear = (153 * ((month + 9) % 12) + 2) / 5 + day - 1
            val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
            return era * 146_097 + dayOfEra - 719_468
        }

    /** The day [days] days later (or earlier, for a negative number). */
    operator fun plus(days: Int): Day = ofEpochDay(epochDay + days)

    override fun compareTo(other: Day): Int = epochDay.compareTo(other.epochDay)

    // Not String.format, whose digits follow the phone's language.
    override fun toString(): String = "${pad(year, 4)}-${pad(month, 2)}-${pad(day, 2)}"

    private fun pad(n: Int, width: Int) = n.toString().padStart(width, '0')

    companion object {
        /** The day [epochDay] days after 1970-01-01. */
        fun ofEpochDay(epochDay: Long): Day {
            // Howard Hinnant's civil_from_days.
            val z = epochDay + 719_468
            val era = Math.floorDiv(z, 146_097L)
            val dayOfEra = z - era * 146_097
            val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
            val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
            val mp = (5 * dayOfYear + 2) / 153
            val day = (dayOfYear - (153 * mp + 2) / 5 + 1).toInt()
            val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
            val year = (yearOfEra + era * 400 + if (month <= 2) 1 else 0).toInt()
            return Day(year, month, day)
        }

        /** Reads `2027-01-31`; null if [text] is not a real day written that way. */
        fun parse(text: String): Day? {
            val match = Regex("(\\d{4})-(\\d{2})-(\\d{2})").matchEntire(text) ?: return null
            val (year, month, day) = match.destructured
            val y = year.toInt()
            val m = month.toInt()
            val d = day.toInt()
            if (y < 1 || m !in 1..12 || d !in 1..daysIn(y, m)) return null
            return Day(y, m, d)
        }

        /** Every day of [year], from 1 January to 31 December. */
        fun allOf(year: Int): List<Day> {
            val first = Day(year, 1, 1)
            return (0 until (if (isLeap(year)) 366 else 365)).map { first + it }
        }

        fun isLeap(year: Int): Boolean = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

        private fun daysIn(year: Int, month: Int): Int = when (month) {
            2 -> if (isLeap(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }
}
