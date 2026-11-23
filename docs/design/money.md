# Money

How Pinfold earns money: what the Shop sells, for how much, how each item can also be earned,
and where rewarded ads may appear. The rules come from
[decision 0004](../decisions/0004-free-to-play-cosmetics-and-rewarded-ads.md) and
[decision 0030](../decisions/0030-money-design.md). The screens are in [`flows.md`](flows.md),
"Shop and rewarded ads".

Numbers on this page are targets. A task that finds one wrong changes it here and says so in its
pull request.

## Principles

- **Nothing for sale helps you win.** The Shop sells cosmetics only: themes, piece styles and ring
  effects. Boosters are never sold (decision 0006).
- **Every cosmetic can also be earned by playing.** Money buys time, never something a player
  can't reach. Nothing is earnable only for a limited time.
- **Prices in real money.** No coins, gems or other game currency. Each item shows its price in
  the player's own currency, as Google Play gives it.
- **No random paid items.** A purchase is always the item the player chose.
- **No pressure.** No countdowns, no "last chance" offers, no pop-ups that advertise the Shop. The
  Shop is a screen the player opens from the map.
- **Ads are always the player's choice.** Each ad names its reward first, and declining costs
  nothing.

## The catalogue at launch

| Item | Kind | What it changes | Price (base) | Earned at |
| --- | --- | --- | --- | --- |
| Notebook | theme | the default | free | always owned |
| Star chart | theme | the board, pieces, rings and board sounds ([`theme-star-chart.md`](theme-star-chart.md)) | $2.99 | 150 stars |
| Neon arcade | theme | the board, pieces, rings and board sounds ([`theme-neon-arcade.md`](theme-neon-arcade.md)) | $2.99 | 200 stars |
| Hearts and clubs | piece style | only the pieces ([`cosmetics.md`](cosmetics.md)) | $0.99 | 40 stars |
| Lasso | ring effect | only how a ring is drawn ([`cosmetics.md`](cosmetics.md)) | $0.99 | 70 stars |
| Flame and frost | piece style | only the pieces ([`cosmetics.md`](cosmetics.md)) | $0.99 | 100 stars |
| Rubber stamp | ring effect | only how a ring is drawn ([`cosmetics.md`](cosmetics.md)) | $0.99 | 125 stars |

The campaign has 225 stars. "Earned at" counts the stars earned in the campaign, the same count
the player's title uses (decision 0027). Stars never go down, so an earned item is never lost. A
chest may give a piece style or ring effect earlier than its star count (C-05). The engine keeps
the same counts in `Cosmetic` (`dots.campaign`); a count changed here changes there too.

The piece styles and ring effects are drawn on [Piece styles and ring effects](cosmetics.md). Their
names follow decision 0028: no world or theme uses them.

If the piece styles and ring effects are not ready at launch, the Shop opens with the two themes.
Nothing else on this page changes.

### Piece styles and ring effects

A piece style changes only the pieces, and a ring effect only how a ring is drawn. Both work in
every theme, drawn with that theme's inks, and in its light and dark versions.

- **A piece style keeps the two sides different in shape**, as the default pieces do: round
  against diamond, or another pair that passes the same colour-blind checks
  ([`pieces.md`](pieces.md)). It keeps the last-move pinhole, the cut on a captured piece, the
  preview and the high-contrast version.
- **A ring effect keeps a capture easy to read.** The ring, the fill and the captured pieces stay
  as clear as in the theme. With animations removed, the effect keeps its look and drops only its
  motion.
- A theme, a piece style and a ring effect are chosen separately. The default piece style and
  ring effect are the theme's own.

### New cosmetics after launch

Each new cosmetic is sold at the price of its kind, and has a way to earn it that stays open: a
star count, a chest or the weekly challenge (R-08). A weekly challenge's cosmetic comes back in a
later challenge, so a player who misses one week can still earn it.

## Owning an item

- **An item is owned if it was bought or earned.** Google Play says what was bought; the saved
  progress says what was earned (stars and chests). The game never writes a purchase into the
  saved progress.
- **A refund** takes away only the purchase. If the player has also earned the item, they keep it.
  An item they no longer own goes back to the default (`flows.md`).
- **Restore purchases** in the Shop asks Google Play again. Purchases also come back by
  themselves on a new phone signed in to the same Google account.
- **Owned items work offline.** The game remembers the last purchases Google Play confirmed.
- **Before the Shop exists**, release builds offer only Notebook, even to a player who already has
  the stars (decision 0029). Earned items appear with the Shop.

## Prices by country

Google Play turns one base price into local prices by the exchange rate, not by what people can
afford. Pinfold sets lower prices by hand in two groups of countries. Each price is rounded the
way prices usually end in that country, and never goes below Google Play's minimum.

| Group | Countries | Share of the base | Theme | Piece style or ring effect |
| --- | --- | --- | --- | --- |
| Base | Every country not listed below | 100% | $2.99, converted by Google Play | $0.99, converted |
| Central Europe | Poland, Czechia, Slovakia, Hungary, Romania, Bulgaria, Croatia, Lithuania, Latvia, Estonia | about 70% | about 7.99 zł | about 2.99 zł |
| Lower prices | Ukraine, Kazakhstan, Moldova, Georgia, Armenia, Azerbaijan, Uzbekistan, Kyrgyzstan | about 40% | about 49 ₴ | about 19 ₴ |
| No sales | Russia, Belarus | — | earn only | earn only |

C-02 sets the exact prices in Play Console. Google Play shows them with tax where a country
requires it. The groups are reviewed with real sales once the game is out (R-08).

**Russia and Belarus.** Google Play does not take payments from players there, and AdMob does
not show ads to players in Russia. The game is the same for them in every other way. In the Shop,
an item they don't own shows only how to earn it, with no Buy button. An ad offer that can't load
is not shown, as anywhere else.

## Rewarded ads

Rewarded ads come from AdMob and only appear in two places. There are no banners, no ads between
screens and no ads when the app opens.

| Place | The offer | The reward | Earned without an ad |
| --- | --- | --- | --- |
| Result of a failed campaign level, below Retry | "Watch an ad for one [booster] on your retry" | One booster for the next attempt at this level only. A kind the level allows, never Undo. | Chests, the boosters each level gives, the free Hint after three failures |
| A chest whose two choices are both boosters | "Watch an ad to keep both" | The other choice as well | Later chests and levels give the same boosters |

**The booster for a retry** is not added to the player's own boosters. It is for the next attempt
only, and is lost if the player leaves the level. On levels whose stars count the boosters left,
using it costs the same as using one of the level's own boosters. On every other level, a game that
uses it earns 1 star, a pass, whatever the stars count. It counts as used once the game has used
every booster of its kind, the ad's one too; one given back by an undo is not used. So ads can help
a player pass a level, but never raise the stars that the total-star leaderboard counts once it
ships after the launch (O-02, decision 0037). The booster offered is the first kind the level
allows, in the order a level file lists them: wall, hint, bomb, double dot. Levels that allow no
booster but Undo show no offer.

**Limits**

- At most **3 rewarded ads a day**, counted together across both places. The day starts at local
  midnight, as the daily puzzle does. After the third, the offers are not shown until the next day.
- At most one ad per failed attempt and one per chest.
- **No ad offers in World 1**, where players learn the rules.
- **No ad offers for 24 hours after a purchase.**
- **Never** during a game, in the daily puzzle, in free play, in two players on one phone or in
  online games. These keep their own rules (decisions 0006 and 0023).
- **Show ad offers**, a switch in Settings, Privacy, on by default. Off hides every offer.

**How an offer behaves** (`flows.md`, "Shop and rewarded ads"): it names its reward before the
player chooses. The reward is given once, only after the ad has finished. Declining, closing the
ad early or an ad that fails to load never blocks play; an offer whose ad isn't ready is simply
not shown. Players in the EU and the UK first answer Google's consent form, and players under the
age the rating allows see no ads (C-03).

### Not used

- **A daily reward to double.** The game has no daily reward. The daily puzzle and its streak stay
  free of ads: the puzzle gets a leaderboard after the launch (O-02), and an ad to save a streak
  would pressure players.
- **Ads that give cosmetics.** Cosmetics are bought or earned by playing.
- **"Remove ads" for sale.** Every ad is already optional, and the switch in Settings hides them
  for free.
- **Bundles and sales** at launch. A later bundle must cost no more than its items bought one by
  one.

## Why

- **Fair play.** Paid advantages are a common complaint in reviews of puzzle and strategy games
  (investigation, section 2), and Google Play has extra rules for random paid items.
- **Players who can't pay.** The paper game is best known in Russia, where neither Google Play
  payments nor AdMob work. The game is in English only (decision 0036), but players there can
  still play it. Making every cosmetic earnable gives those players the whole game.
- **Real-money prices.** The EU consumer authorities' principles on in-game currencies (March 2025)
  ask for prices in real money and warn against currencies that hide them. Pinfold has no
  currency, so it has nothing to convert.
- **Few ads, chosen by the player.** AdMob requires that each rewarded ad is chosen by the player,
  names its reward first and gives it once finished. A small daily cap and no ads while learning
  keep the offers welcome.
- **Expectations.** Games that sell only cosmetics earn modestly, and the game's biggest audience
  can't pay. These rules protect fairness and reviews first.

## Sources

- [AdMob rewarded ads policy](https://support.google.com/admob/answer/7313578)
- [Google Play: billing for users in Russia and Belarus](https://support.google.com/googleplay/android-developer/answer/11950272)
- [Google: regional monetisation pause policies](https://support.google.com/publisherpolicies/answer/15766875)
- [Google Play: setting prices for apps and products](https://support.google.com/googleplay/android-developer/answer/6334373)
- [Google Play: service fees](https://support.google.com/googleplay/android-developer/answer/112622)
- [EU consumer protection network: key principles on in-game virtual currencies, a summary](https://www.reedsmith.com/articles/qas-on-the-eu-consumer-protection-authorities-joint-guidance-paper/)
