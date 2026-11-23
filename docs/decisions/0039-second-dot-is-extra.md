# 0039: The second dot of a two-dot turn is extra

**Date:** 2026-11-23 · **Status:** accepted

## Context

A tester on v0.2.0 noticed that on 1-15 the boss ran out of dots first, and they then placed their
last three dots with no reply. The engine took both dots of a two-dot turn from the budget, so a
boss with a double move ran out turns early: 3 turns early on 1-15, and 15 turns early on 4-20,
whose boss places two dots every turn. The Double dot booster worked the same way, so after using
it the player ran out one turn early and the computer placed its last dot with no reply. No design
or decision chose this; `world-1.md` says only "18 dots each" and "every fifth turn it places two
dots". On 2026-11-23 the owner chose to treat the booster like the bosses, and the 4-20 boss
("two groups at once") like the others.

## Decision

- **A budget counts turns.** Each turn takes one from the player's dots left, on its first dot.
- **The second dot of a turn of two is extra**, and takes nothing from the budget. This holds for
  every turn of two: a boss's `double move every N turns`, the 4-20 boss's `two groups at once`,
  and the Double dot booster.
- **The Double dot booster works with 1 dot left.** A player who uses it places one dot more
  than the budget.
- **A turn of two is one move** in missions that count moves ("capture 2 in 4 moves").
- **With equal budgets both sides place their last dot on the same turn.** Where the computer has
  more dots ("win with 20 dots" against 22), it places the extra ones at the end with no reply, as
  on every "win with fewer dots" level.

## Consequences

- The bosses get stronger: they place 21 dots on 1-15 (was 18), 27 on 2-20 (22), 34 on 3-20 (26)
  and 60 on 4-20 (30). Their star targets were re-checked with `cli bench stars`.
- `GameState.dotsLeft` is the budget still to use. `GameState.dotsToPlace` adds the extra dots
  still to come, for the computer's search, the hint and the mission checks.
  `GameState.movesMade` counts a turn of two as one move.
- Level records are now written as `level/2`. A `level/1` record, from before this decision,
  replays under the old rule, so saved games and tester logs still replay as they were played.
- The app's dots-left count drops by one on a Double dot turn, not two, and the Double dot tool
  is enabled with 1 dot left.
