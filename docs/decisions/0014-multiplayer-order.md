# 0014: Multiplayer order: same phone, then friends online, ranked later

**Date:** 2026-11-05 · **Status:** accepted

## Context

A new, niche game rarely has enough players online at once for instant matches. Background: the design investigation (https://claude.ai/artifact/DyiSkrrx3hwARfRpyxNGRP).

## Decision

- Launch with two players on one phone and leaderboards.
- After launch, online games with friends, turn by turn.
- Ranked matchmaking only once enough people play (a gate in plan task O-07).

## Consequences

- Online games are checked on a server with the same engine rules.
