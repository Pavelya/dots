# 0042: A bomb that hits a ring breaks the capture

**Date:** 2026-11-28 · **Status:** accepted

## Context

A bomb removes the computer's dots in a plus of 5 points. Points inside a captured area are out of
play, so a bomb never touches them, but the dots of the ring round the area are in play and can be
hit. The engine removed such a dot and left the capture standing: the dots inside stayed captured,
the score stayed, and the app still drew the ring, now through an empty point that could even be
played. In play this looked like a bomb with no effect. The owner found it: the computer had a
ring round two of their dots, and a bomb on one of the ring's dots did nothing they could see.

There were two ways out: a bomb that breaks the ring, or no bomb on a ring at all. The owner
chose the first.

## Decision

- **A bomb breaks every capture one of its hit dots held closed.** A dot holds a capture closed
  when it is next to a point of the captured area: up, down, left or right. A dot of the ring that
  touches the area only at a corner does not hold it, so removing it breaks nothing.
- **A broken capture is undone.** The dots it took are alive again, the captor's score drops by
  the points it gave (2 for a dot on a bonus point), and its points go back into play.
- **Earlier captures stay.** A capture made before inside the broken area keeps its dots, its
  points and its area.
- **The bomb captures nothing itself.** The captor may close the ring again with a new dot, by the
  normal rules.
- **Unchanged:** a bomb still never removes a captured dot or touches a point inside a captured
  area, and still needs a dot to hit. Undo after the bomb gives the capture back with the rest of
  the turn.

## Consequences

- The game state keeps each standing capture (its area, the dots it took, its points), so a bomb
  can undo one exactly. `GameState.bombBreaks` says what a bomb would break, and the bomb's
  `BoosterEffect.broken` says what it broke.
- A bomb is the only booster that can change the score. It only ever lowers the computer's score:
  only the player has boosters.
- Missions are unaffected: a mission is decided the moment a dot is captured or a count is reached,
  so no level goes on after the capture a bomb could break.
- The app marks the areas a bomb would break in its preview, and after the blast takes them back
  the way Undo does (`motion.md`, Boosters: Bomb).
- An old game record with a bomb on a ring's dot replays to a different position. The levels' own
  checks never use boosters, so no level changes.
