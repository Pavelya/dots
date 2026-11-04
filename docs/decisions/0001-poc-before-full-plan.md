# 0001: Build a proof of concept before the full plan

**Date:** 2026-11-04 · **Status:** accepted

## Context

The full plan has 56 tasks (themes, figures, animations, campaign, online play, store release).
Nobody has played the game on a phone yet. Computer simulation shows a 15x15 board starting
from four random crosses produces lively games, but whether people enjoy the game is unknown,
and the audience that knows the game is small.

## Decision

Park the full plan. Build a plain, playable Android proof of concept that answers one
question: do people enjoy the core game enough to keep playing? Scope, hypotheses and pass marks
are in `docs/poc/README.md`.

## Consequences

- The POC is built as the first slice of the real app (see `docs/poc/architecture.md`), so
  the engine, app structure, board input and logging are kept if the answer is "go".
- Full-plan tasks the POC covers are marked "pulls forward" in `docs/poc/tasks.md`; the rest
  stay parked until the POC decision.
- If the answer is "stop", the cost was the POC's build and one test week, not the visual work.
