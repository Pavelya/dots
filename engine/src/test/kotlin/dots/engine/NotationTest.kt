package dots.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotationTest {
    @Test
    fun `names points like a spreadsheet`() {
        assertEquals("a1", Notation.name(Pos(0, 0)))
        assertEquals("z1", Notation.name(Pos(25, 0)))
        assertEquals("aa3", Notation.name(Pos(26, 2)))
        assertEquals("am32", Notation.name(Pos(38, 31)))
    }

    @Test
    fun `every name reads back as its point`() {
        for (x in 0 until 99) for (y in 0 until 99) {
            assertEquals(Pos(x, y), Notation.parse(Notation.name(Pos(x, y))))
        }
    }

    @Test
    fun `text that is not a point gives null`() {
        for (text in listOf("", "5e", "a0", "abc1", "e", "12", "e-5")) assertNull(Notation.parse(text), text)
    }
}
