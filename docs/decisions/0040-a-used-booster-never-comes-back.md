# 0040: A level's own booster, once used, never comes back

**Date:** 2026-11-23 · **Status:** accepted

## Context

Each level gives the player some boosters (`boosters: wall 2, undo 1`). Until now every new game of
a level started from that full allowance, so Retry, Restart, Next and a replay all gave the
boosters back. A tester on v0.2.0 used a booster, lost the level, and had it back on the next
attempt. The player's own boosters from chests already leave for good when used (decision 0027).

Decision 0023 says "one Undo per level". It meant one per game of a level.

## Decision

- **A level hands out its own boosters once.** One used is gone for every later attempt at that
  level: Retry, Restart, a return from the map, and a replay for more stars after passing it.
  Unused ones stay for later attempts. This changes decision 0023's "one per level" from one per
  game to one per level, for good.
- **Undo still takes back the player's last turn** and gives back a booster used in that turn: the
  turn never happened. Undo itself stays used.
- **Every attempt is judged from scratch.** It starts without the boosters used before, but those
  never cost stars. On levels whose stars count the boosters (`stars: boosters 2 3`), the count is
  the level's full allowance less the level's own boosters used in this attempt. An attempt that
  uses none earns the full count, however many earlier attempts used.
- **Unchanged:** the free Hint after three failures and an ad's booster are for one attempt
  (decision 0030); the ad's booster still costs the same as one of the level's own on those
  levels; the player's own boosters (decision 0027); free play's Undo, which has no count; a
  practice game, such as a replay of the tutorial, which spends nothing and has the full
  allowance.

## Consequences

- The saved progress keeps each level's own boosters used (`Progress.spent`), and one function,
  `Progress.game`, starts every new game of a level with what is left plus the player's own.
- A game spends a level's own booster the moment it is used, in the same write as the player's
  own, and an undo gives it back. A game resumed after the app closed spends nothing twice.
- A level record says what its game started without (`-wall`), so it still replays; records
  without it started with the full allowance.
- The Level card, Boosters and the tool bar show what is left for the level, ×0 for a kind used up.
