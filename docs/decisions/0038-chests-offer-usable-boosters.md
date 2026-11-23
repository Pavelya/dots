# 0038: A chest offers only boosters the next levels allow

**Date:** 2026-11-23 · **Status:** accepted · **Replaces:** the first point of [0027](0027-chests-own-boosters-and-title.md)

## Context

Decision 0027 let a chest offer two kinds that "later levels" allow. A player's own boosters show
only on levels that allow their kind, so a kind allowed many levels later looks lost. A tester
opened the chest after 1-10 in v0.2.0, picked Bomb, and could not find it on 1-11: no level allows
Bomb before 3-1, 25 levels on. On 2026-11-23 the owner set the rule: never offer a booster that
can't be used.

From the level files, the levels between the chest after 1-10 and the next chest (1-11 to 2-5)
allow only Wall, and so do those after the chest after 2-5 (2-6 to 2-15).

## Decision

- **Each choice of a chest is a kind that at least one level between that chest and the next one
  allows.** For the last chest, the levels to the end of the campaign count.
- **Where those levels allow one kind, the chest holds one reward,** and the player keeps it with
  no pick: one button, **Keep it**. The chests after 1-10 and 2-5 give Wall ×2.
- The other five chests keep their two choices, which already pass the rule.
- A chest of one reward has no ad offer: the offer keeps both of two boosters (decision 0030).
- Undo is never in a chest, two of each kind as before, and the rest of 0027 stands.

## Consequences

- `Chest.CHOICES` (engine, `dots.campaign`) holds one or two kinds per chest. A test reads every
  level file and fails if a chest offers a kind that no level up to the next chest allows, so a
  change to the levels can't break the rule unnoticed.
- Players who already opened a chest keep what they picked. Saved progress needs no change.
- C-05 (cosmetics in chests) may give a one-reward chest a cosmetic as a second choice.
