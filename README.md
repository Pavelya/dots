# Pinfold

**Pinfold** (decision 0016) is a future Android game built on the paper-and-pencil game *Dots*
(Kropki / Точки / Židi): two players alternately place dots on a grid and score by surrounding
the other side's dots.

This repository holds the platform-independent game logic, a computer opponent, a sample
campaign, a terminal front end, and the Android app. The app is a proof of concept (POC): a plain
version that finds out whether people enjoy the game, before any visual work.

- [`docs/poc/`](docs/poc/README.md): the POC's question, scope and test plan.
- [`docs/plan.md`](docs/plan.md): the road from the POC to the store, based on the design
  investigation ([`docs/design/investigation.md`](docs/design/investigation.md)).
- [`docs/decisions/`](docs/decisions/): settled choices, one file each.
- [GitHub Issues](https://github.com/Pavelya/dots/issues): every task and its status, one pull
  request per issue. How a task is picked up and finished: [`CLAUDE.md`](CLAUDE.md).

## Layout

| Module   | What it is |
|----------|------------|
| `engine` | Pure Kotlin/JVM library (JVM 17 bytecode, stdlib only): rules, AI, campaign data, game log. No Android code, so the app, the CLI and a future server share it. |
| `cli`    | Terminal game and AI benchmarks, built on `engine`. |
| `app`    | Android app (Jetpack Compose, Material 3, Android 7+), built on `engine`. Structure: [`docs/poc/architecture.md`](docs/poc/architecture.md). |

Packages in `engine`: `dots.engine` (rules, openings, grounding, game records), `dots.ai`
(computer opponent), `dots.campaign` (levels), `dots.log` (the game log format). All versions
live in `gradle/libs.versions.toml`. Keeping them up to date is Dependabot's job (`.github/dependabot.yml`): it opens update PRs every
week, minor and patch updates grouped into one PR per ecosystem. Lint's "a newer version is
available" checks are switched off so that an upstream release cannot fail the build.

## Run it

Needs a JDK 21 (Android Studio bundles one; the engine and CLI also run on 17). The Gradle
wrapper downloads everything else.

```sh
./gradlew :engine:test :cli:test    # engine and CLI tests
./gradlew :cli:installDist          # build the terminal game (on Windows: gradlew.bat, cli.bat)
cli/build/install/cli/bin/cli levels                 # list the campaign levels
cli/build/install/cli/bin/cli play 1-3               # level 3 of world 1, you are red and move first
cli/build/install/cli/bin/cli play my.level          # any level file
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
cli/build/install/cli/bin/cli bench variety hard        # how often a tier answers the same position differently
cli/build/install/cli/bin/cli bench balance 40          # how often each tier attacks or defends
```

Puzzle levels (see [Checking puzzles](#checking-puzzles)):

```sh
cli/build/install/cli/bin/cli check-levels              # solve every puzzle level of the campaign
cli/build/install/cli/bin/cli check-levels my.level     # or the given files; --seconds 60 sets the time cap
```

Game logs shared by testers from the app (Settings, "Share the game log"; format in
[`docs/poc/architecture.md`](docs/poc/architecture.md)):

```sh
cli/build/install/cli/bin/cli log-check game-log.txt    # replays every game, flags results that don't match
cli/build/install/cli/bin/cli poc-report ann.txt bob.txt # the POC test week's numbers, one log per tester
```

Test builds for testers: on GitHub, Actions > "poc release" > Run workflow on `main`, with a
version such as `0.2` (or push a tag `poc-v0.2` on a commit of `main`). It runs all checks and
publishes the APK as a GitHub Release with install notes ([`docs/poc/install.md`](docs/poc/install.md)).
CI signs its builds with one test key, so a new test build installs over the previous one.
The key lives in the Actions secrets `POC_KEYSTORE_BASE64` and `POC_KEYSTORE_PASSWORD`, never in
the repository, and is never used for the store. A build without the key (a local build, or a
Dependabot pull request, which gets no secrets) uses the standard Android debug key.

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

Levels against the computer, with a dot budget each, grouped in worlds. Difficulty comes from the
computer's tier, its head start (dots already on the board), the board size and the mission.

- **A level is passed when its mission is** (`Level.isPassed`). In `win`, a draw is not enough.
- **Every level starts the human with dots in the middle.** Otherwise placing every dot on the
  border would be a safe way never to lose, which is exactly the forced draw the article warns
  about. A border-only stand-in now loses every level (`bench levels 20 border`).

### Level files

Each level is a text file in [`engine/src/main/resources/levels/`](engine/src/main/resources/levels/),
named after its world and number: `1-03.level` is level 3 of world 1. The engine, the app and the
CLI all read the same files (`Campaign`, `LevelFormat`). Worlds and levels are numbered from 1
without gaps.

```
world: 1
level: 3
name: Scout
computer: easy
dots: 14
mission: win
solutions: open
boosters: none
rule: none
stars: none
map:
..........
..........
..........
......B...
....RB....
....BR....
...B......
..........
..........
..........
```

| Field | What it holds |
|-------|---------------|
| `world`, `level` | The world (1 or more) and the level's number in it (1 to 99). They must match the file name. |
| `name` | The name players see. |
| `computer` | The computer's strength: `easy`, `normal`, `hard` or `expert`. |
| `dots` | Dots each side may place. |
| `mission` | What passes the level: `win`, `win by 3`, `win with 8 dots`, `capture 5 in 8 moves`, `survive 6 moves`, `rescue e5` or `target e5`. See [Missions](#missions). |
| `solutions` | `few` or `open`: how many first moves may pass a puzzle. `few` allows at most 3; `open` any number, for teaching levels where any sensible move should work. See [Checking puzzles](#checking-puzzles). |
| `boosters` | `none`, or the boosters allowed and how many of each: `wall 2, hint 1, bomb 1, double-dot 1`. |
| `rule` | A rule the computer bends: `none`, or `double move every 5 turns`. |
| `stars` | `none`, or the scores for two and for three stars: `3 6`. What is counted depends on the mission. |
| `map` | The board, one row per line from the top. `.` empty point, `R` your dot, `B` a computer dot, `-` a hole (no point; shapes the board), `#` a blocked point, `+` a bonus point. The `R` and `B` dots are the opening; extra `B` dots are the computer's head start. |

The rules play every mission, but no special rule and no map with holes, blocked or bonus
points. A level that uses those can be written and read, but refuses to start.

A file that cannot be read gives an error with the line, for example
`1-03.level, line 4: unknown computer strength 'medium' (easy, normal, hard, expert)`. The files
are kept in the exact form `LevelFormat.write` gives, and a test checks that each one is written
back unchanged.

### Missions

A move is one dot placed; a pass is not a move. After every move, `Level.status` says where the
mission stands (playing, passed or failed), what decided it, and the numbers a player follows:
dots captured, dots lost and moves left. The app turns these into words.

| Mission | Passes | Fails |
|---------|--------|-------|
| `win` | At the end of the game, with more captures than the computer. | At the end otherwise, or as soon as capturing every computer dot that is not safe would not be enough. |
| `win by 3` | As `win`, but 3 or more captures ahead. | As `win`. |
| `win with 8 dots` | As `win`. The player has 8 dots to place; the computer has the level's `dots`, which must be more. | As `win`. |
| `capture 5 in 8 moves` | On the move that captures the 5th dot, within the player's first 8 moves. | After the 8th move, or as soon as too few computer dots are left to capture. |
| `survive 6 moves` | Once the computer has placed 6 dots without a capture, or can capture nothing more. | On the move that captures one of the player's dots. |
| `rescue e5` | Once the player's dot at e5 is safe, or at the end of the game if it is still there. | On the move that captures it. |
| `target e5` | On the move that captures the computer's dot at e5. | Once that dot is safe, or at the end of the game. |

Capture, survive, rescue and target end the level the moment they are decided. The three win
missions play on to the end even once won, because the margin counts for stars.

A dot is **safe** when a chain of its side's dots joins it to the border, stepping only up, down,
left or right (`Grounding.safe`). Such a chain cannot be crossed. An enemy ring can cross a diagonal
step, so a dot joined to the border only diagonally is not safe.

A level is refused if its mission is decided before the first move, or asks for more moves than
that side has dots. The computer plays for captures only, not against the mission.

### Checking puzzles

A level whose mission is short is a **puzzle**: `capture` and `survive` with 5 moves or fewer,
and `rescue` and `target`. `cli check-levels` solves each puzzle with `Solver` and fails if:

- **it is unsolvable**: the computer can stop the player whatever the player does;
- **it is too easy**: more than 3 first moves pass it, and its file says `solutions: few`;
- **it cannot be decided within the time cap** (20 seconds): make the board or the mission
  smaller.

A passing puzzle shows its shortest answer, the computer's replies in brackets:

```
1-9   Fork         capture 1 in 2 moves   ok      passes in 2 moves: d3 (c4) e4; 1 first move passes: d3  (0.0 s, 66 positions)
```

The win missions, and capture or survive with more than 5 moves, are whole games, not puzzles,
and are not checked; `bench levels` covers them. A rescue or target that neither side can force
within 5 of the player's moves is not checked either: it plays as a longer game. A test runs the
check on every campaign level, so CI stops a bad puzzle.

**How the solver works.** It searches every move of the player's against every reply of the
computer's, a pass included. These are the replies of a perfect opponent, who plays better than
the computer a player meets, so a puzzle the solver passes can be passed in the game. To keep
this fast:

- Points within two of a live dot are tried one by one. One point further away stands for all
  the others, which are too far to take part in a capture within a few moves.
- Lower bounds cut lines short. A dot needs at least as many more dots to wall in as the
  smallest set of empty points between it and the border (a minimum cut), and at least as many
  to make safe as the empty points on its shortest straight chain to the border. When that is
  more than the moves left, the line cannot work.
- Positions reached by different move orders are solved once, and the points that settled
  earlier positions are tried first.
- Counting stops at 4 passing first moves, which is already too many.

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

Tiers are `AiConfig`: search depth and breadth, how far ahead it looks for threats, a mistake
rate and a variety margin. EASY is deliberately blind to threats and plays a random nearby move a
third of the time.

**Variety.** So that a position does not always get the same answer, every candidate move gets a
random bonus of 0 to a small margin before the best is picked. A move that scores just below the
best is then played now and then, one a full margin below never. A captured dot is worth 10,000
points and touching one more dot 2 or 3, so the margins never trade away a capture; they only
choose between quiet moves.

| Tier   | Margin |
|--------|-------:|
| EASY   | 0      |
| NORMAL | 10     |
| HARD   | 5      |
| EXPERT | 5      |

EASY needs no margin: its random mistakes already vary every position. NORMAL, the weaker
searching tier, gets the wider margin. The margins stay small because, once a capture is more than
a few moves away, the only thing steering the computer towards it is that "touching one more dot"
score. A larger margin drowns it out: with a margin of 10, HARD on level 7 failed to capture a
player who only plays on the border in half its games.

`cli bench variety <tier> [margin]` answers 20 fixed mid-game positions 10 times each and counts
the different moves. HARD plays at least two different moves in 14 of the 20 positions (2 of 20
without a margin), NORMAL in 16, EXPERT in 13, EASY in all 20. Over 80 paired games
(`cli bench duel 80 10 24 3 8 3 2 3 8 3 2 5 0`), HARD with its margin against HARD without won
9, lost 11 and drew 60 (sign test p=0.82): the variety costs no strength. With a seed fixed, as in
the tests, the computer still plays the same game every time.

**Attack and defence.** Testers felt the computer "always prefers to attack". Two things set the
balance in a tier:

- **How far it looks.** `attackReach`: enemy groups at most this many dots from capture count as
  chances to attack. `dangerReach`: its own groups at most this many dots from capture count as
  in danger.
- **How much it wants each.** A cell that walls in an enemy group gets a bonus
  (`attackWeights`), and so does a cell that stops the opponent walling in one of its own groups
  (`dangerWeights`). The closer the group is to capture, the bigger the bonus.

| Tier   | Looks for attacks | Looks for danger | Attack weights (1, 2, 3, 4+ dots away) | Danger weights |
|--------|------------------:|-----------------:|----------------------------------------|----------------|
| EASY   | none              | none             | (not used)                             | (not used)     |
| NORMAL | 2 dots            | 1 dot            | 3000, 600, 120, 30                     | the same       |
| HARD   | 3 dots            | 2 dots           | 3000, 600, 120, 30                     | the same       |
| EXPERT | 3 dots            | 3 dots           | 3000, 600, 120, 30                     | the same       |

Defence and attack now have the same weights at every tier (`AiConfig.BALANCED`). Before, the
danger weights were lower: 2500, 400, 80, 20.

`cli bench balance 40` measures what each tier does. Each tier plays 40 games against a casual
stand-in: NORMAL that plays a random nearby move a quarter of the time. The games use the
testers' format: 15×15, four crosses, 33 dots each. Every move of the tier is sorted by its effect
on groups at most three dots from capture:

- *attack*: it captures, or brings an enemy group closer to capture;
- *defend*: it moves one of its own groups further from capture;
- *both*, or *neither*.

The table also shows three more things:

- *threatened*: the share of moves made while one of the tier's own groups was at most two dots
  from capture, and how many of those moves defended.
- *en prise*: attacks made while one of the tier's own groups was one dot from capture, without
  defending it.
- *lost after attack*: dots lost on the opponent's next move after an attack that did not also
  defend. *Lost* counts all the dots the tier lost.

```
                  share of the tier's moves          threatened   defended   per game:
tier    weights   attack  defend  both  neither      (moves)      of those   en prise  lost after attack  lost
EASY    old/new    12.7%    7.0%  0.2%   80.1%        47.1%        10.3%      0.70      0.80               6.88
NORMAL  old        23.9%    8.5%  0.3%   67.3%         6.6%        47.1%      0.10      0.68               1.70
        new        23.9%    8.5%  0.3%   67.3%         6.6%        47.1%      0.10      0.68               1.70
HARD    old        37.4%    8.3%  0.3%   54.0%         6.6%        51.7%      0.08      0.75               1.75
        new        37.1%    8.3%  0.4%   54.2%         5.9%        59.0%      0.08      0.75               1.78
EXPERT  old        32.5%    8.5%  0.4%   58.6%        10.7%        37.6%      0.13      0.70               1.53
        new        31.4%    8.6%  0.5%   59.4%        10.2%        37.8%      0.13      0.68               1.50
```

What this shows:

- **The weights did not cause the lean.** With equal weights NORMAL plays exactly the same games,
  and HARD's shares move by less than a point. Even danger weights two to three times the attack
  weights (6000, 1800, 360, 90) leave HARD at 37% attack and 9% defence.
- **The computer attacks often because there is often something to attack.** It rarely needs to
  defend: one of its own groups is within two dots of capture on only 6% to 10% of its moves
  (EASY, which ignores threats: almost half). It almost never leaves a group one dot from capture
  to attack elsewhere (0.1 times per game or less).
- **Looking as far for danger as for attacks made HARD passive.** With danger 3 dots out, HARD
  failed to capture players who only defend. Over 60 games per level, against a player who only
  plays on the border, it won level 7 in 27 games instead of 44. Against the NORMAL stand-in it
  won level 8 in 23 games instead of 42. Against itself it was no stronger (81-68-11 over 160
  games, p=0.33). So HARD looks for danger one dot less far than for attacks.
- **NORMAL sees danger only one dot away on purpose.** It stands for a weaker player, and a
  weaker player notices too late that a group of theirs is being walled in.

Equal weights cost HARD no strength. Over 80 paired games (`cli bench duel`, weights given as
`dangerWeightsB=2500,400,80,20` and so on), HARD with each set of new danger weights against
HARD with the old ones:

```
danger weights               15×15, four crosses, 33 dots    10×10, a cross, 24 dots
3000, 600, 120, 30 (chosen)  40-34- 6  p=0.56                 11-11-58  p=1.00
3600, 900, 180, 45           35-37- 8  p=0.91
6000, 1800, 360, 90          37-38- 5  p=1.00
```

Nothing else in this README changed with the new weights: the tier and level tables and the
variety numbers come out the same.

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
EASY   vs NORMAL   2-34- 4  p=0.000      NORMAL vs HARD     1-17-22  p=0.000
EASY   vs HARD     0-40- 0  p=0.000      NORMAL vs EXPERT   1-14-25  p=0.001
EASY   vs EXPERT   0-39- 1  p=0.000      HARD   vs EXPERT   2- 9-29  p=0.065
```

So there are three strength levels, **EASY ≪ NORMAL < HARD**. **EXPERT is no stronger than HARD**
and thinks about three times as long, so the campaign does not use it; it is kept for experiments
with deeper search. Most games between the stronger tiers are still draws: equals rarely force a
capture. Two identical configurations come out exactly even by construction.

**Levels.** `cli bench levels 20 <stand-in>`: a stand-in plays red on each level, 20 games;
W-L-D in captures for the stand-in, and how many games passed the level's mission (for `win`,
the same as the wins). Each game stops when the mission is decided. EASY stands for a weak player,
NORMAL for a strong one, border for someone who only plays on the border.

```
                     EASY stand-in    NORMAL stand-in    border-only
1 Warm-up            9- 4- 7          18- 0- 2           0-19- 1
2 Open Field        11- 5- 4          19- 0- 1           0-20- 0
3 Scout              8- 8- 4          14- 0- 6           0-19- 1
4 Outpost            0-20- 0           3- 2-15           0-20- 0
5 Rescue             0-20- 0           2- 5-13           0-20- 0
6 Pincer             0-20- 0           1-10- 9           0-20- 0
7 Siege              0-17- 3           2- 4-14           0-15- 5
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
15x15 empty    (33)            80%     0.3       0.1           5%        never          25%
15x15 1 cross  (33)            70%     0.8       0.1          10%        never          50%
15x15 4 crosses (33)            5%     5.5       0.8          55%        12             58%
15x15 4 crosses (67)           10%     7.7       1.0          55%        12             50%
9x9   4 crosses (12)           50%     2.2       0.1           5%         5             80%
casual vs casual
15x15 4 crosses (33)           15%     4.2       0.1           5%         8             59%
12x12 4 crosses (21)            0%     2.7       0.3          25%         5             65%
```

- **The opening matters most.** From an empty board equal players draw most games; from four
  random crosses there is contact from the first moves, a capture by about move 10, and between
  strong players comebacks in half of the games.
- **15x15 with four crosses and about 33 dots each** (66 moves, roughly 5 to 8 minutes for people)
  is the best candidate default; 67 dots each makes a longer, even livelier game.
- **Small boards favour the first player** (9x9 with four crosses: 64% to 83% of decisive games) and still draw often.
- 20 games per row, so differences of a few points are noise; people play differently from
  these stand-ins, so this picks formats to play-test rather than replacing the test.

## Checks

GitHub Actions runs on every pull request and push to `main` (`.github/workflows/test.yml`):
engine and CLI tests, app tests and screenshot tests, lint, and the debug APK. Locally, the same:

```sh
./gradlew --continue :engine:test :cli:test :cli:installDist :app:verifyRoborazziDebug :app:lintDebug :app:assembleDebug
```
