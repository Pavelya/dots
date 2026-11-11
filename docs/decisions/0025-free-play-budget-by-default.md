# 0025: Free play has a dot budget by default; an open-ended game is an option

**Date:** 2026-11-11 · **Status:** accepted

## Context

A free play game needs a way to end. Two ways exist in the engine:

- **Dot budget.** Each player has a fixed number of dots. The game ends when both have placed
  theirs. The POC used it: Game length is Short (15% of the board's points per player) or Long
  (30%). Games are short and always end.
- **Open-ended, as on paper.** No dot limit. A player may **stop**, declaring the dots they want to
  keep grounded; the opponent then captures what is left and ends the game with **done**. The game
  also ends when neither player can place a dot, or both pass in a row. The computer stops on its
  own once it cannot lose. Games can run long.

A-08 builds free play and needs to know which one to use.

## Decision

- **A dot budget is the default.** A new free play game uses the budget from Game length, Short
  unless the player chose otherwise, as in the POC.
- **Open-ended is an option** in Game length, next to Short and Long. The choice is remembered for
  the next game, like board size.
- In an open-ended game the player can stop and, after the computer stops, end the game with done.
- Two players on one phone share the same Game length choice, so they can play either way too.
- The campaign and the daily puzzle keep their own fixed budgets. This decision does not change
  them.

## Consequences

- The app adds the open-ended choice to Game length, and Stop and Done buttons on the free play
  and two-player boards when a game is open-ended (A-08). These buttons need a design first.
- The engine needs no change: it already plays both ways.
- The game log records the game's settings, so budget and open-ended games can be told apart
  later.
