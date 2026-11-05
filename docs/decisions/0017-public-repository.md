# 0017: The repository is public

**Date:** 2026-11-05 · **Status:** accepted

## Context

The plan left one question open: should the repository be public or private? GitHub's free plan
treats the two differently. Public repositories get branch protection, unlimited Actions minutes
and GitHub Pages. Private ones get none of the first and third, and a monthly minutes limit. The
only cost of going public is that others can read and copy the code and the art, and decision 0009
already accepts being copied.

## Decision

- The repository is public.
- Nothing secret is committed. Signing keys and passwords live in GitHub Actions secrets; the POC
  test key is the secrets `POC_KEYSTORE_BASE64` and `POC_KEYSTORE_PASSWORD`. `.gitignore` blocks
  key files and local secret files.
- The test key that was once committed (`app/poc-test.keystore`, certificate SHA-256
  `12:C7:8C:2D:E6:DA:A3:1C:1B:4D:95:24:26:09:F2:29:06:6F:62:78:74:16:38:ED:B9:46:43:8E:B1:EE:0E:97`)
  stays in the history and is burned: it never signs a build again. The release workflow refuses
  an APK signed with it.
- Commits use GitHub noreply addresses (see `CLAUDE.md`). CI fails a pull request with any other
  author or committer email.
- Security problems are reported privately, as `SECURITY.md` describes.

## Consequences

- Main can be protected, so changes reach it only through pull requests with green CI.
- The privacy policy and support pages can be served from GitHub Pages (R-03).
- Workflows get the smallest permissions they need. Test releases run only from `main` or a
  `poc-v*` tag on main, so a branch build can't reach testers.
- Testers who installed a build signed with the old key uninstall it once before installing a
  build signed with the new one.
