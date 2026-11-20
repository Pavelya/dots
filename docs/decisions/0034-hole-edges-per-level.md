# 0034: Hole edges are chosen per level and stated on the level card

**Date:** 2026-11-20 · **Status:** accepted

## Context

Campaign boards can have holes: places with no point, drawn as `-` in a level's map. A hole along
the map's edge gives the board an odd outline. A hole inside the board raises a question the
paper game never asks: is its edge part of the border?

- **Count as border (safe).** A dot next to a hole, up, down, left or right, can never be
  captured, like a dot on the edge of the board, and no ring can go round the hole. A hole becomes
  a lake whose shore is a shelter for both sides.
- **Count as open (capturable).** A ring may go round the hole, and the hole is then inside the
  captured area. A hole shelters nobody; it only changes the shape of the fight.

Each gives different puzzles. The lake teaches "take the shore first"; the open hole takes that
easy answer away. World 3 is built on both (`world-3.md`), and World 4's Reef uses open holes. A
single rule for the whole game would throw one set of puzzles away.

The two rules look the same on the board, so a player cannot tell from the picture which one a
level uses.

## Decision

- **Per level.** Each level with holes chooses: its `holes` line says `border` or `open`. A level
  without holes says `none`. A map with holes that does not choose is an error when the level
  loads. The rules are in the README, "Board shapes and special points".
- **Stated on the level card.** The level card of a level with holes says in one line which rule
  it uses, so the player knows before the first move. A level without holes says nothing about
  holes.
- **Odd outlines play the same either way.** A hole that reaches the edge of the map, directly or
  through other holes, is outside the board, since no ring can go round it. The choice only
  matters for holes inside the board.
- Holes belong to campaign levels. The daily puzzle has none (its `holes` line is always `none`).

## Consequences

- Level designers pick the rule that makes the puzzle. The engine, `check-levels` and
  `bench levels` play each level by its own rule.
- The level card gets one more line on levels with holes, and the board draws the holes; both
  are A-15. The line's wording and look are set there, from the level card's design.
- If a later mode wants holes, it chooses the rule the same way and shows it before the game.
