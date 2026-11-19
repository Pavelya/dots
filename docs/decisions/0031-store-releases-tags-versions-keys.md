# 0031: Store releases: tags, version numbers and keys

**Date:** 2026-11-19 · **Status:** accepted

## Context

Google Play takes an app only as an Android App Bundle signed with the developer's upload key.
Every new upload needs a higher version code than any before it, and a version code can never be
used again. The test builds so far are debug builds signed with the POC test key, with the version
code 100 + the number of commits. The store app needs its own rule, one that stays the same for as
long as the app is on Google Play.

## Decision

- **A store release starts from a tag** `v<major>.<minor>.<patch>` on a commit of `main`, for
  example `v0.1.0`. The workflow `store release` (`.github/workflows/release.yml`) tests, builds,
  signs and uploads it to the internal testing track. Moving a release on to the closed test or to
  production is done by hand in Play Console.
- **The tag sets the version.** The version name is the tag without the `v`. The version code is
  major × 1,000,000 + minor × 1,000 + patch, so `v0.1.0` is 1000 and `v1.2.3` is 1002003. Each
  part is 0 to 999.
- **The release build shrinks the code** with R8 and drops unused resources.
- **Two keys.** Google keeps the app signing key (Play App Signing). The owner makes the upload key
  once; it lives in the Actions secrets `UPLOAD_KEYSTORE_BASE64` and `UPLOAD_KEYSTORE_PASSWORD`,
  in the password manager, and on an offline copy. If it is lost, Play support can register a new
  one; the app signing key is never at risk.
- **Uploads use a service account** with release rights for this app only, its key in the Actions
  secret `PLAY_SERVICE_ACCOUNT_JSON`. A script in the repository (`.github/scripts/play-upload.sh`)
  talks to the Play Developer API, so no third-party action ever holds the key.

## Consequences

- A version number is used once. A broken build is fixed with the next patch number, never by
  moving a tag.
- Store builds are signed with a different key from test builds, so a phone with a test build
  uninstalls it once before installing Pinfold from Google Play.
- The first upload is made by hand in Play Console: the Play Developer API knows an app only after
  its first bundle. While the app has never been published, Play keeps uploaded releases as drafts;
  the workflow says so, and the owner rolls them out in Play Console.
- How to release, and the one-time setup: [`docs/release.md`](../release.md).
