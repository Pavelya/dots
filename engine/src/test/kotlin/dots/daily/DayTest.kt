package dots.daily

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DayTest {

    @Test
    fun `counts days as java time does`() {
        var date = LocalDate.of(1899, 12, 1)
        while (date.year < 2102) {
            val day = Day(date.year, date.monthValue, date.dayOfMonth)
            assertEquals(date.toEpochDay(), day.epochDay, "$date")
            assertEquals(day, Day.ofEpochDay(date.toEpochDay()), "$date")
            date = date.plusDays(1)
        }
    }

    @Test
    fun `writes and reads dates`() {
        assertEquals("2027-03-04", Day(2027, 3, 4).toString())
        assertEquals(Day(2027, 3, 4), Day.parse("2027-03-04"))
        assertEquals(Day(2028, 2, 29), Day.parse("2028-02-29"))
        assertNull(Day.parse("2027-02-29"))
        assertNull(Day.parse("2027-13-01"))
        assertNull(Day.parse("2027-3-4"))
        assertNull(Day.parse("27-03-04"))
        assertFailsWith<IllegalArgumentException> { Day(2027, 4, 31) }
    }

    @Test
    fun `steps over months and years`() {
        assertEquals(Day(2027, 3, 1), Day(2027, 2, 28) + 1)
        assertEquals(Day(2026, 12, 31), Day(2027, 1, 1) + -1)
        assertEquals(365, Day.allOf(2027).size)
        assertEquals(366, Day.allOf(2028).size)
        assertEquals(Day(2028, 12, 31), Day.allOf(2028).last())
        assertEquals(-1, Day(2027, 1, 31).compareTo(Day(2027, 2, 1)))
    }
}
