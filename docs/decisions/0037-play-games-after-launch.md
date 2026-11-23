# 0037: Leaderboards and Play Games after launch

**Date:** 2026-11-23 · **Status:** accepted · **Replaces:** the first point of [0014](0014-multiplayer-order.md)

## Context

Decision 0014 planned to launch with two players on one phone and leaderboards. Leaderboards,
achievements and the Play Games backup all come from Google Play Games (O-02), which needs its own
setup in Play Console, sign-in in the app, and tests on a phone. On 2026-11-22 the owner chose to
keep the launch smaller and move all of it after the launch.

## Decision

- Pinfold launches with two players on one phone, without Google Play Games.
- Leaderboards, achievements and the Play Games backup come after the launch, with O-02.
- Until then, the app shows no way to them: Stats has no Leaderboards or Achievements links, and
  Settings has no Play Games backup row.
- The rest of decision 0014 stands: games with friends online after the launch, ranked games only
  once enough people play.

## Consequences

- The launch build needs no sign-in, so no player is asked to sign in to anything.
- The app keeps the routes O-02 will fill: the Leaderboards screen and the Play Games backup page.
- Decision 0030 gives the daily puzzle's and the stars' leaderboards as reasons for some money
  rules (no ad to save a streak; an ad's booster never raises the stars). Those rules stay at
  launch, so nothing changes when the leaderboards arrive.
- Android's own backup still carries the campaign progress to a new phone.
