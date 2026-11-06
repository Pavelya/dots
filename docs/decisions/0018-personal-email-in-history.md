# 0018: Four early commits keep the owner's personal email

**Date:** 2026-11-06 · **Status:** accepted

## Context

Decision 0017 requires GitHub noreply addresses on every commit. Four squash merges made in the
GitHub web interface took the owner's personal email from the account settings instead:
`38fc1c5` (#7), `6d0f4e9` (#13), `d729c1c` (#15) and `9a6d893` (#16). Replacing it means
rewriting `main` and force-pushing, which changes every commit hash after the first of them.

## Decision

- The four commits stay as they are. The owner accepts that the address is public once the
  repository is.
- `main` is not rewritten.
- The owner's GitHub account keeps "Keep my email addresses private" and "Block command line
  pushes that expose my email" on, so web merges and pushes use the noreply address from now on.

## Consequences

- These four commits are the only exception to decision 0017. Any other commit with a personal
  email fails CI (`commit-identity` in `.github/workflows/test.yml`).
- The `commit-identity` run for the push of `9a6d893` to `main` failed. It is the last expected
  failure: the check looks only at the commits each push adds.
