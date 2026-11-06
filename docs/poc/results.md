# POC results

## How the test ran

The owner and a few friends played the POC build informally. This was not the week described
in the [test protocol](test-protocol.md): there were no game logs, no survey and no daily
schedule. The results below are therefore notes from play, not numbers. The decision based on
them is [decision 0019](../decisions/0019-poc-go-without-the-test-week.md).

## Hypotheses

The hypotheses and pass marks are in the [charter](README.md#hypotheses-and-how-we-judge-them).

| # | Hypothesis | Result | What we know |
|---|---|---|---|
| H1 | People come back | Not measured | No logs and no week. The closed test (R-06) measures it instead. |
| H2 | Rules learnable from one screen | Not measured | Testers asked for more teaching: show dots close to capture in the first levels, and explain the two-tap placement (points 1 and 8). |
| H3 | Format: 15x15, four crosses, 33 dots | Not measured | Nobody named the game length or board size as a problem. |
| H4 | The computer is fun at some difficulty | Not measured | Testers noticed that the computer always plays the same move in the same position and seems to attack too much (points 3 to 5). |
| H5 | Placing dots by finger works | Fails | The pass mark says no tester names placement as a top annoyance. Testers named it twice: cells too small for large fingers, and two taps per dot (points 7 and 8). |
| H6 | Showing threats helps | Not measured | The threat highlight is off by default, so few saw it. Testers asked for something like it in the early levels (point 1). |

## Feedback

| # | What testers said | What the code shows | Where it went |
|---|---|---|---|
| 1 | In the first levels, show which dots are close to being captured, so players learn to avoid the computer's traps. | The threat highlight exists but is off by default. | A-06 Tutorial |
| 2 | Undo should be a limited booster, usable X times per game. | Undo against the computer is unlimited. Decision 0012 fixed the first four boosters. | X-11 Decision: Undo as a booster; then G-04 and A-05 |
| 3 | The computer's strategy should grow with the levels: little strategy early on, sacrifices for bigger captures later, and play that suits every kind of puzzle, not only the plain game. | Four fixed tiers; the weak ones make random mistakes. Nothing plays toward a level's mission. | G-07 (early levels), G-14 (sacrifices), G-17 (missions), G-11 (the whole curve) |
| 4 | The computer always plays the same move in the same position. | Hard and Expert choose at random only between moves with exactly the same score, which is rare. | G-15 Varied computer moves |
| 5 | The computer seems to always attack. | The bonus for attacking is higher than the bonus for defending at every distance. | G-16 Attack and defence balance |
| 6 | A timed mode: X minutes per player for the whole game, or X seconds per move. | Not in the plan. | G-18 Timed games |
| 7 | Cells feel too small for large fingers. | 15x15 gives about 22 to 25 dp per point; Android's guideline for a touch target is 48 dp. | A-13 Touch size and placement; Q-03 |
| 8 | Two taps per dot feels like many taps, though it is fine once known. Try alternatives, or explain it better. | Tap to preview, tap again to confirm. | A-13 Touch size and placement; A-06 |
| 9 | Show when a player's dots are running out: a pulse or a colour. | Only the status text counts the dots left. | V-05 Animations; D-08 |
| 10 | Make the computer's last dot easy to see; a finger often hides it. | A thin grey ring in the same colour as the grid. | A-14 Clear mark on the computer's last move; V-05; D-08 |

## What we enjoyed and what annoyed us

No survey was run, so there are no top-5 lists. Nothing in the feedback questions the core game.
All ten points are about teaching, the computer's play and placing dots.
