# 0033: Analytics: Google Analytics for Firebase, a few game events, no IDs for ads

**Date:** 2026-11-20 · **Status:** accepted

## Context

We want to see where players get stuck or leave: how many players who install the game reach
level 10, which levels they fail, which boosters they use and how many finish the daily puzzle.
We don't want to know who they are.

There are two ways. The game already writes a game log (`game-log.txt`), but it stays on the
phone; testers send it by hand. Collecting it from every player would need a server, which we
don't have (decision 0019: no domain). Google Analytics for Firebase is free, uses the Firebase
project that Crashlytics already uses (decision 0032), and draws funnels from its events.

Firebase Analytics counts installs, sessions and time in the game by itself. By default it also
reads the phone's advertising ID and can share data with Google's ads products. Each installation
gets a random app instance ID, which lets Analytics follow one installation through a funnel; it
is not linked to a name, an account or the phone.

In the EU, the EEA, the UK and Switzerland, storing an ID on the phone for analytics needs the
player's consent. The game gets a consent form for these places anyway, for ads (C-03): Google's
User Messaging Platform (UMP). UMP passes the player's answer to Firebase Analytics by itself
(Google's "consent mode"), including whether analytics may store its ID.

## Decision

- **Google Analytics for Firebase**, in the Firebase project `pinfold`, store builds only. Debug
  builds (CI and POC test builds) and the tests contain no Firebase; they write each event to the
  phone's log instead.
- **Four events of our own**, the whole list (`Events` in the app):
  - `level_start`: a new game of a campaign level (`level_name` such as `1-4`, `level_number`
    counting from the first level, `world`).
  - `level_end`: the level ended, with `success` (1 or 0) and `stars`.
  - `booster_used`: the `booster` and the level.
  - `daily_puzzle_done`: an attempt at a daily puzzle ended, with `puzzle_number` and `success`.

  Practice games, free play and games between two people send none of them. Firebase's own events
  (`first_open`, sessions, engagement, app updates and removals) stay on. The funnel runs from
  `first_open` to `level_end` of level 10 with `success` 1.
- **No IDs for ads.** The app removes the advertising ID permission that Firebase adds, and turns
  off the collection of the advertising ID and of the Android ID. Screen views are off: the game is
  one screen to Android. No user ID and no user properties are ever set.
- **Consent mode defaults: nothing for ads.** Ad storage, ad user data and ad personalization are
  denied in every country. Analytics storage is allowed by default; where the law needs consent,
  C-03's consent form sets it from the player's answer. No outside player gets a store build
  before C-03: the closed test (R-06) needs the Data safety form (R-03), which needs C-03. Until
  then only the internal testers have store builds.
- **In Google Analytics:** data kept 2 months (the shortest), Google signals off, granular
  location and device data off, data sharing with Google off, no link to Google Ads or AdMob.

## Consequences

- C-03 connects the consent form to Analytics, and makes sure nothing leaves the phone in those
  places before the player answers. Firebase logs `first_open` at the first launch, before the
  form can know where the player is, so C-03 holds collection back until then (for example: off in
  the manifest, switched on once the form has its answer) and checks it in DebugView from an EU
  test region. When AdMob comes, C-03 also decides about the advertising ID permission for ads;
  Analytics keeps its own collection of the advertising ID off.
- The Data safety form (R-03) adds, on top of decision 0032: app interactions (app activity) and
  device or other IDs (the app instance ID), collected for analytics, not shared, encrypted in
  transit. The privacy policy names Google Analytics for Firebase.
- Crashlytics reports now also list the game's last few events before a crash, which helps to see
  what the player was doing.
- How to turn Analytics on, check the events and build the funnel: [`docs/release.md`](../release.md#analytics).
  What exists in Firebase and Google Analytics is listed in `docs/release.md`, "What is set up".
- Funnels and other explorations look back at most 2 months. Standard reports keep their totals.
