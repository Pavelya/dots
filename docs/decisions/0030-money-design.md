# 0030: Every cosmetic can be earned; prices by country; rewarded ads in two places

**Date:** 2026-11-19 · **Status:** accepted

## Context

Decision 0004 makes the game free, with cosmetics for sale and optional rewarded ads, and says
every ad reward can also be earned by playing. It left open what is sold, for how much, and where
ads appear. Decision 0029 hides Star chart and Neon arcade until C-01 and C-02 settle this.

Three facts shaped the answer:

- Google Play does not take payments from players in Russia and Belarus, and AdMob does not show
  ads to players in Russia. Russian is one of the game's first languages (decision 0007).
- Google Play converts prices by exchange rate only, so prices for lower-income countries must
  be set by hand.
- The game has no daily reward, and the daily puzzle has a leaderboard. Stars have a leaderboard
  too, and the player's own boosters do not cost stars (decision 0027).

The full design, with prices and sources, is [`docs/design/money.md`](../design/money.md).

## Decision

- **Every cosmetic can be earned by playing,** not only the ad rewards. Each launch cosmetic has a
  campaign star count: Star chart at 150 stars, Neon arcade at 200, and the two piece styles and
  two ring effects at 40, 70, 100 and 125. A chest may give a piece style or ring effect earlier.
  Nothing is earnable only for a limited time.
- **The Shop sells in real money,** with no game currency, no random items, no countdowns and no
  bundles at launch. One-time purchases only.
- **Prices:** a theme $2.99, a piece style or ring effect $0.99, as base prices. About 70% of
  that in central Europe and about 40% in Ukraine and its neighbours; Russia and Belarus can only
  earn. The country lists are in `money.md`.
- **Owned means bought or earned.** Purchases come from Google Play; earned items from the saved
  progress. A refund removes only the purchase.
- **Rewarded ads appear in two places only:** on the Result of a failed campaign level, one
  booster for the retry; and on a chest of two boosters, keep both. At most 3 ads a day. None in
  World 1, none for 24 hours after a purchase, and never in a game, the daily puzzle, free play,
  two players or online. A Settings switch hides every offer.
- **The booster from an ad is for the retry only.** It does not join the player's own boosters,
  and on levels whose stars count boosters left it costs the same as the level's own. Ads can
  help a player pass, but never add stars.

## Consequences

- The engine needs the star counts that earn each cosmetic, beside `Title` (G-27), and a booster
  given for one attempt that counts against star targets (G-28).
- The wireframes gain the ad offers, the earn-only Shop item and the Settings switch (D-28).
- Two piece styles and two ring effects need a design (D-29) and a build (V-28) before they can be
  sold. If they are late, the Shop opens with the two themes.
- C-02 builds the Shop with these prices and the bought-or-earned rule; C-03 the two ad places,
  the cap and the switch; C-04 tests them; C-05 picks the chests that give cosmetics.
