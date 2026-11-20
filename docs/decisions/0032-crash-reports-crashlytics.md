# 0032: Crash reports: Firebase Crashlytics beside Android vitals

**Date:** 2026-11-20 · **Status:** accepted

## Context

We want to see crashes before players mention them in reviews, and the closed test (R-06) must
reach at least 99.5% crash-free sessions.

Google Play's Android vitals needs no code. But it only counts players who allowed their phone to
share usage and diagnostics with Google, its data arrives about a day late, and it measures the
share of daily users who crashed, not crash-free sessions. With the 12 or so testers of the closed
test, it would show very little.

Firebase Crashlytics is a library in the app. It reports every crash within minutes, from every
player, with the stack trace and the phone model, and counts crash-free users and sessions. Since
version 18.6 it needs no Google Analytics to count them.

## Decision

- **Both.** Android vitals stays on, as it always is. Store builds also carry Firebase
  Crashlytics.
- **Its own Firebase project**, `pinfold`, separate from the Google Cloud project
  `pinfold-release` that uploads releases. Later Firebase work (analytics in Q-06, online play in
  O-01) uses the same project. Google Analytics is off in it; Q-06 decides about analytics.
- **Store builds only.** Crashlytics is a release-only dependency. Debug builds (CI test builds,
  POC test builds) and the tests don't contain it and send nothing.
- **Nothing beyond the defaults.** The app adds no custom keys, logs or user IDs, so a report holds
  only what Crashlytics collects by itself: the stack trace, the app's state and the phone's model
  and Android version at the moment of the crash, and a random ID for the installation.
- **The mapping file goes to Firebase** from the release workflow, for tagged builds only, so
  reports show the real code names.
- **The Firebase settings file is in the repository.** `app/google-services.json` holds the
  project's public settings, not secrets (Firebase's own advice). It is needed to build the app.

## Consequences

- The store app now asks for internet access (Crashlytics adds the permission); debug builds don't.
- The Data safety form (R-03) declares crash logs, diagnostics and device or other IDs: collected,
  not shared, encrypted in transit, for crash reporting. The privacy policy names Crashlytics.
- Every store release can be checked in the Crashlytics dashboard within minutes of reaching
  testers. How to read both dashboards and force a test crash: [`docs/release.md`](../release.md#crash-reports).
- What exists in Firebase is listed in `docs/release.md`, "What is set up".
