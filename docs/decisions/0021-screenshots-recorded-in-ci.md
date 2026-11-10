# 0021: Reference screenshots are recorded in CI, on Linux

**Date:** 2026-11-10 · **Status:** accepted

## Context

The app's screenshot tests compare what the app draws with reference images committed in
`app/src/test/screenshots/`. CI checks them on every pull request, but it can only compare. New
images had to be recorded on a machine with the Android SDK, and the owner's Mac has none.
Installing it (F-11) takes 8 to 16 GB on a nearly full disk. The images also depend on the machine:
the committed ones were recorded on Linux, and a Mac may draw text and edges slightly differently,
which would make CI fail.

## Decision

- Reference screenshots are recorded by the GitHub workflow `record screenshots`
  (`.github/workflows/screenshots.yml`), on GitHub's Linux machines, the same type that checks them.
- The workflow records them on a chosen branch and uploads them as an artifact. It does not push.
  The person or agent working on the branch downloads them, looks at every changed image and
  commits them. How: `CLAUDE.md`, Commands.
- A local `recordRoborazziDebug` is only for a first look. Its images are not committed.

## Consequences

- App tasks that change what the app draws no longer need the Android SDK on the Mac. They depend
  on F-12, not F-11. F-11 stays for work that needs a phone connected by USB or an emulator, such
  as benchmarks.
- Every committed screenshot comes from the same kind of machine, so CI does not fail because of
  where an image was made.
- Recording costs a push and a run of a few minutes. Phone checks use the APK that every CI run
  uploads (`dots-debug-apk`), signed with the POC test key.
