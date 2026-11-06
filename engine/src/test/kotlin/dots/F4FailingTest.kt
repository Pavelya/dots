package dots

import kotlin.test.Test
import kotlin.test.fail

// Throwaway: proves a PR with a failing test can't be merged (#4). Never merged.
class F4FailingTest {
    @Test
    fun fails() = fail("This PR must not be mergeable")
}
