# 0003: Difficulty comes from level progression, not a picker

**Date:** 2026-11-05 · **Status:** accepted

## Context

The POC lets testers choose Easy, Normal or Hard in Settings, so the test can find which
strength people enjoy (hypothesis H4). The owner does not want players to face many choices.

## Decision

- **In the POC:** Easy / Normal / Hard stays, as a test setting.
- **In the full version:** there is no difficulty picker. The computer gets stronger as the player
  progresses through levels (the campaign: tier, head start and board per level), and the POC's
  H4 results decide where each tier sits on that ladder.

## Consequences

- The full plan's settings screen loses the difficulty choice; free play against the computer
  uses the strength the player has reached in the campaign.
- Keep other player-facing choices to a minimum for the same reason.
