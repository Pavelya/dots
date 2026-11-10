# 0022: A world opens after its previous boss, with enough stars

**Date:** 2026-11-10 · **Status:** accepted

## Context

The world map (A-02) shows locked worlds with the stars they need, but nothing said when a world
opens. The design investigation suggested stars alone ("40 stars for world 3"). Puzzle levels
give three stars easily, so with stars alone a player could reach World 2 after about seven levels
of World 1, before its whole games and its boss.

## Decision

- World 1 is open from the start. A later world opens when the player has **passed the previous
  world's last level** (its boss) **and has enough stars in total**: World 2 at 20, World 3 at 45,
  World 4 at 75. That is about 4 stars for every 3 levels before it, so passing every level with
  one star is not enough, and replaying a few for more stars is.
- In an open world, the first level is open, and each other level opens when the one before it
  is passed.
- Stars count the best result of each level; a replay never lowers them.

The numbers are in `World.NAMES_AND_STARS` (engine, `dots.campaign`).

## Consequences

- No world's teaching levels or boss can be skipped.
- A player who passes a boss with too few stars replays earlier levels; the map shows the stars
  needed and the stars they have.
- A new world needs a name and a star count in `World.NAMES_AND_STARS`; the engine tests fail
  without them.
