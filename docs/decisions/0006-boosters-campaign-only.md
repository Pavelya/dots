# 0006: Boosters only in the solo campaign, earned and never sold

**Date:** 2026-11-05 · **Status:** accepted

## Context

Boosters make the campaign more varied, but they would turn games between people into a contest of items. Background: the design investigation ([`docs/design/investigation.md`](../design/investigation.md)).

## Decision

- Boosters exist only in the solo campaign.
- They are earned by playing (or by an optional rewarded ad), never sold.
- Two-player, online and ranked games use the pure rules.
- The computer never uses boosters; every level can be passed without them.

## Consequences

- Engine task G-04 defines the boosters exactly; game records include booster actions.
