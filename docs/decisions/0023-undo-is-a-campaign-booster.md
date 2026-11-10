# 0023: Undo is a campaign booster; free play keeps unlimited undo

**Date:** 2026-11-10 · **Status:** accepted

## Context

In the POC, a player could take back their last move against the computer as often as they
liked. Testers suggested that undo should be allowed only a few times per game, as a booster
([POC results](../poc/results.md), point 2).

Most campaign levels are short puzzles with star targets. With unlimited undo, a player can try
a move, see the computer's answer, take it back and try another, until the puzzle is solved by
trial and error. Retry already restarts a level.

Decision 0012 set the first four boosters: Wall, Hint, Bomb and Double dot. Decision 0006 says
boosters exist only in the solo campaign, are earned and never sold, and every level can be
passed without them.

## Decision

- **Undo is a fifth campaign booster,** next to Wall, Hint, Bomb and Double dot. It follows
  decision 0006: earned, never sold, never used by the computer, and never needed to pass a
  level.
- **What it does.** It takes back the player's last turn: their dot (both dots after a double
  dot), any booster used in that turn, which the player gets back, and the computer's reply. The
  player then plays that turn again. It can be used only on the player's turn, after their first
  move, and not after the game ends. It does not count as the turn's one booster.
- **One per level, in every world.** Every campaign level allows Undo 1, World 1 included, so
  Undo is the first booster a player meets.
- **It counts like any booster.** On survive levels, whose stars count the boosters left, using
  Undo costs a star. Their star targets change so that three stars still means no booster used.
- **Free play against the computer** keeps unlimited undo, as in the POC. There it is a plain
  Undo button with no count, not a booster.
- **No undo** in two players on one phone, the daily puzzle, online and ranked games. They use
  the pure rules. A failed daily puzzle can be retried.
- Mis-taps are guarded by the way a dot is placed (A-13), not by undo.

## Consequences

- The engine adds Undo as a booster, gives every level Undo 1, records its use so a level game
  still replays exactly, and moves the survive star targets from `boosters 1 2` to `boosters 2 3`
  (G-23).
- The design adds Undo's icon, a tool bar with five boosters on the canvas, and Undo's rows in
  `motion.md` and `sound.md` (D-14).
- The app shows five boosters in the campaign tool bar (A-05), an Undo button in free play and
  none in two players (A-08), and no undo in the daily puzzle (A-07). Undo gets its own effect
  (V-06).
