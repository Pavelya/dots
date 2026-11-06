# 0019: POC "go" without the full test week

**Date:** 2026-11-06 · **Status:** accepted

## Context

The POC charter ([`docs/poc/README.md`](../poc/README.md)) planned a one-week test with logs and a
survey, and a go / change / stop decision based on hypotheses H1 to H6. Instead, the owner and a
few friends played the POC informally, without logs. Their feedback
([`docs/poc/results.md`](../poc/results.md)) is about teaching, the computer's play and placing
dots. None of it questions the core game. H1, whether people come back on their own, was not
measured. H5, placing dots by finger, failed: testers named placement as an annoyance.

## Decision

- **Go.** The game design phase starts, using the POC feedback.
- H1 is measured in the closed test (R-06) instead.
- The other POC findings become tasks, not a retest. Placement goes to A-13 and the computer's
  behaviour to G-15, G-16 and G-17. The full list is in the results file.

## Consequences

- Retention is unknown until the closed test. If R-06 shows that most testers stop after one or
  two days, the plan is reviewed before launch.
- Decision 0003 said H4 results place the computer's tiers on the level ladder. With H4 unmeasured,
  benchmarks (G-11) and the campaign play-tests (Q-04) place them instead.
- Cards labelled `draft` are revised with what the POC taught before work on them starts.
