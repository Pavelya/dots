# 0019: No domain of our own

**Date:** 2026-11-06 · **Status:** accepted

## Context

Google Play needs a privacy policy address and a support email for the store listing. A domain of
our own (for example pinfold.game) would give both a stable address, for about $10 to $20 a year.
But a domain only pays off with a website behind it, and building and keeping a landing page is
work that a pet project does not need.

## Decision

- We do not buy a domain.
- The privacy policy, and any other page the store needs, is served from GitHub Pages under the
  owner's `github.io` address. (Google Sites is the fallback.)
- The support email is an address on the game's own Google account (task B-05).
- The store app ID stays `io.github.pavelya.pinfold` (decision 0016), which needs no domain.

## Consequences

- No yearly cost, and no website to build or maintain.
- B-05 Accounts and handles skips the domain step.
- The privacy policy address depends on the GitHub account and repository names. If either
  changes, the address in the Play listing must be updated the same day.
- Someone else may register pinfold.game or a similar domain. Buying one later means a new
  decision record.
