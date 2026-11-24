# 0041: Rewarded ads: Google's consent form, no age question, no IDs for ads

**Date:** 2026-11-24 · **Status:** accepted

## Context

Decision 0030 puts rewarded ads in two places. Building them (C-03) left three questions open:

- **Age.** `money.md` says players under the age the rating allows see no ads. The game has no
  sign-in and no age question. Its target audience on Google Play is teens and adults, not
  "Designed for Families" (R-04).
- **The advertising ID.** Decision 0033 removed the permission to read it, for analytics. AdMob
  uses the ID to cap how often an ad repeats, to catch fraud and to choose ads; without it ads
  still show, but fewer fill and they pay a little less.
- **A win with the ad's booster.** G-29 caps such a game at 1 star on most levels. Result would
  still say "Two stars need …", a target the player can't reach with the ad's booster.

## Decision

- **Google's consent form (UMP)** for the EU, the EEA and the UK, each time the app opens and
  again from Settings > Privacy > Ad choices. Ads are asked for only once Google allows it. Google
  Analytics collects nothing until the form has its answer, or Google has said none is needed.
- **No age question.** The target audience stays teens and adults (R-04), and every ad is limited
  to content rated PG, so no player sees an ad meant for adults.
- **No IDs for ads.** The app keeps no permission for the advertising ID, nor for Android's ad
  services (their ad ID, attribution and topics). Decision 0033 holds for ads too.
- **No star target after a capped win.** When a passed game used the ad's booster and so earned
  1 star, Result shows its stars and its count, with no "stars need" line.

## Consequences

- The Data safety form (R-03) lists AdMob's data with no advertising ID, and the privacy policy
  names AdMob and the consent form. R-04 declares the ads and keeps the audience at teens and
  adults; a younger audience would need a new decision.
- AdMob's own settings match: maximum ad content rating PG, a published European regulations
  message (`docs/release.md`, Part 9).
- If ads earn too little without the advertising ID, a new decision can bring it back; the Data
  safety form then changes with it.
