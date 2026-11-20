# 0035: One dot closing several chains captures every area it closes

**Date:** 2026-11-20 · **Status:** accepted

## Context

A single dot can close more than one chain at once: for example, a dot placed between two
almost-finished rings closes both. The Wikipedia article the rules follow lets the player choose
which chain to draw, and says programs usually take the smallest area. On a phone there is no pen,
so the game has to pick for the player.

- **Every enclosed area (the current rule).** The dot captures each newly enclosed area that holds
  an enemy dot. This is what a player drawing freely on paper would pick, since more captured dots
  is always better.
- **Only the smallest area.** The dot captures one area; the others stay open and may be closed
  later, or broken by the opponent.

The engine, the computer opponent, the campaign levels and their bench numbers are all built on
the first rule. Neither rule has been seen with real players yet.

## Decision

- **Keep the current rule.** One dot captures every newly enclosed area that holds an enemy dot.
  The rule is in the README, "Where this deviates from the article".
- **Revisit after the play-tests (Q-04).** If testers are surprised by captures they did not
  expect, or find the rule unfair, a new decision record settles it again.

## Consequences

- Nothing changes in the engine, the computer opponent, the levels or the app.
- Changing the rule later would mean a change to captures in the engine, a fresh `bench levels`
  run, and checking every level whose answer closes two rings with one dot.
