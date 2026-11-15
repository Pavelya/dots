# 0027: Chests give boosters; the player's own boosters add to a level's; the title comes from stars

**Date:** 2026-11-15 · **Status:** accepted

## Context

A-04 saves the player's progress and adds the chest and the player's title to the map. The design
showed where they go (canvas `map`, the wireframes' Chest and Stats) but left four rules open:

- The Chest wireframe offers a booster or a cosmetic. The app has no cosmetics yet, so a cosmetic
  from a chest could not be shown or used.
- Until now a level gave a fixed number of each booster it allows, and the player owned nothing
  between levels. A booster from a chest needs a way into a level.
- The design investigation suggested a title "from points earned in every mode", but no points
  rule exists, and the daily puzzle and Stats are not built yet.
- "A chest after every 10th level" can count across the campaign or within each world. World 1
  has 15 levels and Worlds 2 to 4 have 20 each.

## Decision

- **A chest offers two boosters,** two of each, and the player keeps one. Each chest offers two
  kinds that later levels allow, never Undo, which stays one per level (decision 0023). The pairs
  are in `Chest.CHOICES` (engine, `dots.campaign`). Cosmetics join chests once they exist; the
  saved progress already keeps a list of cosmetics owned.
- **The player's own boosters add to a level's own,** only for the kinds that level allows. The
  Level card, Boosters and the tool bar show the sum. A game uses the player's own boosters first,
  and like the free Hint after three failures, they do not count against a level's star targets.
  Each one used leaves the player's boosters at once; an undo gives it back.
- **The title comes from the stars earned in the campaign:** Novice from 0, Apprentice from 10,
  Strategist from 45, Master from 100 and Grandmaster from 180 of the 225 stars. Stars never go
  down, so neither does the title. The numbers are in `Title` (engine, `dots.campaign`).
- **A chest follows every 10th level of the campaign,** counted across worlds: after 1-10, 2-5,
  2-15, 3-5, 3-15, 4-5 and 4-15.

## Consequences

- A level record lists the player's own boosters apart from the free Hint (`*wall` beside
  `+hint`), so `cli log-check` replays it and the app knows how many of them a game used.
- A new world needs a pair in `Chest.CHOICES` for each new chest; the engine tests fail without
  one.
- Stats (A-11) and the daily puzzle (A-07) can add to the title later only through a new decision.
