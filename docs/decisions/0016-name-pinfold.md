# 0016: The game is called Pinfold

**Date:** 2026-11-05 · **Status:** accepted

## Context

The game needs its own name (decision 0010). Task B-01 listed 20 candidates in
`docs/design/names.md`; the owner chose Pinfold, an old English word for an enclosure where stray
animals were penned: closing the other side's dots into a ring.

## Decision

- The game's name is **Pinfold**. Store titles may add a descriptive part that uses "dots" as an
  ordinary word, for example "Pinfold: Rings of Dots".
- The store app's application ID will be `io.github.pavelya.pinfold`, set in task A-01 before the
  first Play upload. POC test builds keep `io.github.pavelya.dots` (D-POC-1), so testers' updates
  keep working during the test week.

## Consequences

- Checks so far (`docs/design/names.md`): no game and no trademark named Pinfold was found; an iOS
  travel app and an animal-related web service use the name, and pinfold.com and pinfold.app are
  taken; pinfold.game and pinfold.io looked free.
- Before buying a domain or starting logo work, the owner searches the official trademark
  registers once (USPTO, EUIPO, WIPO) for "pinfold" in classes 9, 28 and 41. A conflicting
  registration there reopens this decision.
- A future iOS version needs a longer title, since the App Store already has an app called Pinfold.
