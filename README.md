# Pinfold

**Pinfold** (decision 0016) is a future Android game built on the paper-and-pencil game *Dots*
(Kropki / Точки / Židi). This repository is its prototype:
two players alternately place dots on a grid and score by surrounding the other side's dots.

This repository holds the platform-independent game logic, a computer opponent, a sample
campaign, a terminal front end, and the start of the Android app.

**Current focus: a proof of concept.** A plain Android version to find out whether people
enjoy the game, before any visual work. Goals, scope, tasks and test plan: [`docs/poc/`](docs/poc/README.md).
The road from the POC to the store is the full plan ([`docs/plan.md`](docs/plan.md), a copy of the
[plan doc](https://claude.ai/artifact/9GaGRbXg9qwJsYMswwE7ZF)), based on the design investigation
([`docs/design/investigation.md`](docs/design/investigation.md)); settled choices are in
[`docs/decisions/`](docs/decisions/).

## Layout

| Module   | What it is |
|----------|------------|
| `engine` | Pure Kotlin/JVM library (JVM 17 bytecode, stdlib only): rules, AI, campaign data. Meant to be imported by an Android module. |
| `cli`    | Terminal game and AI benchmarks, built on `engine`. |
| `app`    | Android app (Jetpack Compose, Material 3, Android 7+), built on `engine`. Structure: [`docs/poc/architecture.md`](docs/poc/architecture.md). |

Packages in `engine`: `dots.engine` (rules, openings, grounding, game records), `dots.ai`
(computer opponent), `dots.campaign` (levels). All versions live in `gradle/libs.versions.toml`.
Keeping them up to date is Dependabot's job (`.github/dependabot.yml`): it opens update PRs every
week, minor and patch updates grouped into one PR per ecosystem. Lint's "a newer version is
available" checks are switched off so that an upstream release cannot fail the build.

## Run it

Needs a JDK 21 (Android Studio bundles one; the engine and CLI also run on 17). The Gradle
wrapper downloads everything else.

```sh
./gradlew :engine:test :cli:test    # engine and CLI tests
./gradlew :cli:installDist          # build the terminal game (on Windows: gradlew.bat, cli.bat)
cli/build/install/cli/bin/cli levels                 # list the campaign levels
cli/build/install/cli/bin/cli play 3                 # campaign level 3, you are red and move first
cli/build/install/cli/bin/cli vs normal 16 14 cross  # classic open-ended game against the computer
cli/build/install/cli/bin/cli pvp 39 32              # two players, one keyboard, traditional size
cli/build/install/cli/bin/cli watch hard normal      # two computers play each other
```

Run the game from a real terminal: `./gradlew :cli:run` does not pass keyboard input through.

The Android app needs the Android SDK as well (Android Studio installs it, or set `ANDROID_HOME`):

```sh
./gradlew :app:assembleDebug                    # APK in app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest :app:lintDebug # app tests (Robolectric, no phone needed) and lint
```

Every pull request and every push to `main` also builds the APK on GitHub Actions: open the run
and download the `dots-debug-apk` artifact.

Moves are a column and a row: `e5`, or `ab12` on boards wider than 26 columns (after `z` come
`aa`, `ab`, ...). Other commands: `stop` (declare your dots grounded, see the rules), `done`
(finish collecting after your opponent stopped), `pass`, `quit`. Captured dots show as `r`/`b`,
cells out of play as `,`.

Benchmarks (computer against computer; see "How strong is it" below):

```sh
cli/build/install/cli/bin/cli bench tiers 40 10 24      # every tier against every other: games, board size, dots each
cli/build/install/cli/bin/cli bench levels 20 normal    # a stand-in plays each level: a tier, or "border"
cli/build/install/cli/bin/cli bench duel                # two custom configurations; prints its arguments
```

Game logs shared by testers from the app (Settings, "Share the game log"; format in
[`docs/poc/architecture.md`](docs/poc/architecture.md)):

```sh
cli/build/install/cli/bin/cli log-check game-log.txt    # replays every game, flags results that don't match
cli/build/install/cli/bin/cli poc-report ann.txt bob.txt # the POC test week's numbers, one log per tester
```

Test builds for testers: on GitHub, Actions > "poc release" > Run workflow, with a version such as
`0.2` (or push a tag `poc-v0.2`). It runs all checks and publishes the APK as a GitHub Release with
install notes ([`docs/poc/install.md`](docs/poc/install.md)).
Every build is signed with the same test key (`app/poc-test.keystore`, deliberately not secret and
never used for the store), so a new test build installs over the previous one.

## Rules

Implemented from the Wikipedia article
[Dots (paper-and-pencil game)](https://en.wikipedia.org/wiki/Dots_(paper-and-pencil_game)):

1. **Field.** A grid of any size (traditionally 39×32). Players alternately place a dot of their
   colour on an empty intersection. Red moves first.
2. **Capture.** If a newly placed dot completes a closed chain of the player's dots that encloses
   at least one enemy dot, the area inside is surrounded. Chain links run orthogonally or
   diagonally. The enemy dots inside score for the surrounding player (their own dots inside do
   not), and every enclosed dot and empty intersection is excluded from further play.
3. **No empty captures.** A chain around no enemy dot captures nothing, and the inside stays
   playable. An enemy dot dropped into such an area is not lost on the spot: it lives until the
   owner closes a *new* chain around it with a newly placed dot. Meanwhile it can be used to
   complete the intruder's own chain.
4. **Grounding.** A group of dots touching the border can never be surrounded. On their turn, a
   player may **stop**, saying "all the dots I want to keep are grounded". Their opponent then
   moves as often as they like to capture what is left, and ends the game with **done**. Whoever
   has captured more dots wins.
5. **Openings.** Because a player who only plays on the border can never be captured, games
   normally start from a few dots in the middle: a *cross* (a 2×2 block, colours alternating),
   a *double cross*, or four crosses placed randomly (`Openings`).

### Where this deviates from the article

- **Dot budget (mobile variant).** A game, and every campaign level, may give each player a fixed
  number of dots to place; it ends when both have used theirs. This keeps sessions short and
  makes puzzle levels possible. With a budget, `stop` is still allowed but the computer never
  uses it. Dots placed up front (the computer's *head start*) don't count against the budget.
- **One dot, several chains.** The article lets players choose which chain to draw (and says
  programs usually take the smallest area). This engine captures every newly enclosed area that
  holds an enemy dot, which is what a player drawing freely on paper would pick.
- **Start positions** may not already enclose an enemy dot.
- **Not implemented:** the *kropki na dwie kratki* variant (lines of length two and knight-move
  links), more than two players, and restricting early moves to the centre.

## Campaign

Eight levels (`Campaign`) against the computer, with a dot budget each. Difficulty comes from the
computer's tier, its head start (dots already on the board) and the board size.

- **Only a win passes a level** (`Level.isPassed`); a draw is not enough.
- **Every level starts the human with dots in the middle.** Otherwise placing every dot on the
  border would be a safe way never to lose, which is exactly the forced draw the article warns
  about. A border-only stand-in now loses every level (`bench levels 20 border`).

## Computer opponent

`SearchAi` is an alpha-beta search over captured dots. What makes it play purposefully is a
measure of **how many more dots until a group is captured**: the minimum vertex cut between the
group and the border, found by `ThreatFinder` (a small max-flow on the grid, capped at a few dots
so it is fast enough to run inside the search). It is used three ways:

- **Which moves to try.** At every ply the cells that wall in a nearby enemy group, and the cells
  that would stop the opponent doing the same to the computer's own groups, are searched first,
  together with the cells next to the most dots. This is how it builds toward a capture several
  moves ahead instead of waiting for one to appear.
- **How to score where the search stops.** Every group of either side is valued by its distance
  from capture and by whose turn it is: one dot away is nearly lost if its enemy is to move; two
  or three away is a smaller liability. Without this a slowly strangled group looks the same as a
  safe one, and deeper search played worse, not better.
- **Collecting** after the opponent stops (below).

Tiers are `AiConfig`: search depth and breadth, how far ahead it looks for threats, and a mistake
rate. EASY is deliberately blind to threats and plays a random nearby move a third of the time.

Endgame behaviour follows the grounding rule:

- **Stopping.** In a game without a dot budget the computer declares its dots grounded only when
  the game is already decided: its lead exceeds the opponent's score plus every dot of its own
  that is still exposed.
- **Collecting** (after the human stops). It builds the cheapest wall around each exposed enemy
  group, using the same cut computed exactly by `Cleanup`, until nothing is left to take. Known
  gap: a group already inside the computer's own closed chain would need a dividing wall, which
  the planner doesn't try.

### How strong is it, and how hard are the levels?

**Tiers.** `cli bench tiers 40 10 24`: 40 games per pairing on 10×10 from a cross, 24 dots each.
Games are played in pairs with the same random seeds and the sides swapped, so luck cancels out.
W-L-D is for the first-named tier; *p* is a two-sided sign test on the decisive games (below
about 0.05 the difference is unlikely to be chance).

```
EASY   vs NORMAL   0-36- 4  p=0.000      NORMAL vs HARD     1- 8-31  p=0.039
EASY   vs HARD     1-39- 0  p=0.000      NORMAL vs EXPERT   0- 8-32  p=0.008
EASY   vs EXPERT   0-40- 0  p=0.000      HARD   vs EXPERT   4- 5-31  p=1.000
```

So there are three strength levels, **EASY ≪ NORMAL < HARD**. **EXPERT is no stronger than HARD**
and thinks about three times as long, so the campaign does not use it; it is kept for experiments
with deeper search. Most games between the stronger tiers are still draws: equals rarely force a
capture. Two identical configurations come out exactly even by construction.

**Levels.** `cli bench levels 20 <stand-in>`: a stand-in plays red on each level, 20 games;
W-L-D for the stand-in. EASY stands for a weak player, NORMAL for a strong one, border for
someone who only plays on the border.

```
                     EASY stand-in    NORMAL stand-in    border-only
1 Warm-up            9- 4- 7          14- 0- 6           0-19- 1
2 Open Field        11- 5- 4          16- 0- 4           0-20- 0
3 Scout              8- 8- 4           8- 1-11           0-19- 1
4 Outpost            0-20- 0           3- 2-15           0-20- 0
5 Rescue             0-19- 1           1- 3-16           0-20- 0
6 Pincer             0-20- 0           1- 9-10           0-19- 1
7 Siege              0-17- 3           0- 3-17           0-14- 6
8 Grandmaster        0-20- 0           2-15- 3           0-20- 0
```

Difficulty rises for both stand-ins, but not smoothly for a weak player: level 4 is a wall,
because that is where the computer starts looking at threats. The stand-ins are computers, not
people, and nobody has played the levels yet, so treat the curve as a first draft.

**Speed.** On a laptop, a HARD move on a 14×14 board takes about 40 ms (EXPERT about 130 ms).
Not yet measured on a phone.

### Which game format has the most action?

`cli bench variants 20`: each format played 20 times between two equal stand-ins, a *casual*
one (NORMAL that plays a random nearby move 25% of the time) and a *strong* one (HARD). It
measures whether things happen in a game, not whether people enjoy it. Selected rows:

```
                              draws   captures  lead changes  won from   first capture  first player's
format (dots each)                    per game  per game      behind     (median move)  share of wins
strong vs strong
15x15 empty    (33)            95%     0.1       0.0           0%        never          -
15x15 1 cross  (33)            55%     0.8       0.1           5%        56             44%
15x15 4 crosses (33)            5%     4.4       0.5          35%        11             42%
15x15 4 crosses (67)            5%     6.3       0.7          45%        11             21%
9x9   4 crosses (12)           45%     2.0       0.1           5%         5             82%
casual vs casual
15x15 4 crosses (33)            0%     3.9       0.3          25%         6             40%
12x12 4 crosses (21)           35%     2.5       0.1           5%         5             69%
```

- **The opening matters most.** From an empty board equal players draw almost every game; from
  four random crosses there is contact from the first moves, a capture by about move 10, and
  comebacks in a third of games.
- **15x15 with four crosses and about 33 dots each** (66 moves, roughly 5 to 8 minutes for people)
  is the best candidate default; 67 dots each makes a longer, even livelier game.
- **Small boards favour the first player** (9x9: 82% to 91% of decisive games) and still draw often.
- 20 games per row, so differences of a few points are noise; people play differently from
  these stand-ins, so this picks formats to play-test rather than replacing the test.

## Status and checks

- GitHub Actions runs on every pull request and push to `main` (`.github/workflows/test.yml`): engine and
  CLI tests, app tests and screenshot tests, lint, and the debug APK. Locally, the same:
  `./gradlew --continue :engine:test :cli:test :cli:installDist :app:verifyRoborazziDebug :app:lintDebug :app:assembleDebug`
- Not yet verified: speed of the computer on a real phone.
- POC progress: [`docs/poc/tasks.md`](docs/poc/tasks.md).

## Next steps

- Android UI (Jetpack Compose canvas or a game engine) on top of `engine`: board, zoom, a
  precise-placement aid for fingers, capture animations (`MoveResult.captures` carries the dots
  and the region for exactly that), highlighting grounded dots (`Grounding`).
- Interactive tutorial; themes and piece shapes as cosmetics.
- Play-test the campaign with people, smooth the step at level 4, add objectives beyond "more
  captures" and a daily puzzle.
- Async online play, then ranked.
- Find out whether deeper search can be made to pay off (it does not yet beyond depth 3), and
  profile HARD on a real phone; the capture check is a flood fill and can be made incremental
  if needed.
- Score levels by margin (stars) rather than just win/lose, since equal play often ends in a draw.
