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
cells out of play as `,`, blocked points as `#`, bonus points as `+`, and holes as blanks.

Benchmarks (computer against computer; see "How strong is it" below):

```sh
cli/build/install/cli/bin/cli bench steps 160          # each strength step against the next: games [board size] [dots each]
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
- **Board shapes and special points (campaign).** Holes, blocked points and bonus points; see
  [Board shapes and special points](#board-shapes-and-special-points).
- **Not implemented:** the *kropki na dwie kratki* variant (lines of length two and knight-move
  links), more than two players, and restricting early moves to the centre.

## Campaign

Levels against the computer, with a dot budget each, grouped in worlds. Difficulty comes from the
computer's tier, its head start (dots already on the board), the board size and the mission.

- **A level is passed when its mission is** (`Level.isPassed`). In `win`, a draw is not enough.
- **Every level starts the human with dots in the middle.** Otherwise placing every dot on the
  border would be a safe way never to lose, which is exactly the forced draw the article warns
  about. A border-only stand-in passes no level (`bench levels 20 border`).

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
holes: none
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
| `computer` | The computer's strength, weakest first: `beginner`, `easy`, `steady`, `normal` or `hard` (see [Strength steps](#strength-steps)), or `expert`. |
| `dots` | Dots each side may place. |
| `mission` | What passes the level: `win`, `win by 3`, `win with 8 dots`, `capture 5 in 8 moves`, `survive 6 moves`, `rescue e5` or `target e5`. See [Missions](#missions). |
| `solutions` | `few` or `open`: how many first moves may pass a puzzle. `few` allows at most 3; `open` any number, for teaching levels where any sensible move should work. See [Checking puzzles](#checking-puzzles). |
| `boosters` | `none`, or the boosters the player may use and how many of each: `wall 2, hint 1, bomb 1, double-dot 1`. See [Boosters](#boosters). |
| `rule` | A rule the computer bends on a boss level: `none`, `double move every 5 turns`, `fortress of 12 dots` or `two groups at once`. See [Boss rules](#boss-rules). |
| `stars` | `none`, or the scores for two and for three stars: `3 6`. What is counted depends on the mission. |
| `holes` | Whether the edge of the map's holes counts as border: `border` or `open`, or `none` for a map without holes. See [Board shapes and special points](#board-shapes-and-special-points). |
| `map` | The board, one row per line from the top. `.` empty point, `R` your dot, `B` a computer dot, `-` a hole (no point; shapes the board), `#` a blocked point, `+` a bonus point. The `R` and `B` dots are the opening; extra `B` dots are the computer's head start. |

A file that cannot be read gives an error with the line, for example
`1-03.level, line 4: unknown computer strength 'medium' (beginner, easy, steady, normal, hard, expert)`. The files
are kept in the exact form `LevelFormat.write` gives, and a test checks that each one is written
back unchanged.

### Missions

A move is one dot placed; a pass is not a move. After every move, `Level.status` says where the
mission stands (playing, passed or failed), what decided it, and the numbers a player follows:
dots captured, dots lost and moves left. The app turns these into words. A dot captured on a
bonus point counts 2 here as everywhere: `capture 5` asks for a score of 5.

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

A dot is **safe** when a chain joins it to the border, stepping only up, down, left or right,
through its side's dots and points out of play: holes, blocked points and the inside of earlier
captures (`Grounding.safe`). Such a chain cannot be crossed, since nobody can ever place a dot on
it. An enemy ring can cross a diagonal step, so a dot joined to the border only diagonally is not
safe.

A level is refused if its mission is decided before the first move, or asks for more moves than
that side has dots. The computer plays for captures only, not against the mission.

### Boosters

Boosters exist only in the campaign, and only the player has them (decision 0006). A level's
`boosters` line says which ones the player may use and how many times in that level.

| Booster | What it does |
|---------|--------------|
| `wall` | Blocks one empty point that is in play. Nobody may place a dot there for the rest of the game. A ring passes over a wall as over an empty point, so a wall on the point that would close a ring stops that ring there, but a wider ring round the wall still captures. |
| `hint` | Shows the player the computer's best move for them (`Hints.hint`, the hard computer without its random choice, within 1.5 seconds). The board does not change. |
| `bomb` | Removes the computer's dots on a point and the four points next to it, up, down, left and right. Points off the board and points out of play (inside a capture) are not touched, so a bomb never undoes a capture. It needs at least one dot to hit. The points become empty and playable; scores and dots left do not change. |
| `double-dot` | The player places two dots this turn. It needs 2 dots left. No pass or stop between the two; if the board fills up after the first, the turn ends there. |

- A booster does not use up the turn: the player still places their dot afterwards.
- At most one booster per turn, and none after a stop.
- A "target" mission cannot allow the bomb, which would remove the target instead of capturing
  it.
- A double dot's two dots both count as moves, and both come out of the dots left.

A level game is saved as a `LevelRecord`: the level and every action, boosters included, on one
line. It replays to exactly the same position against the level as its file is now.

```
level/1 2-5 | e5 f6 wall:g7 g6 f8 double-dot d4 e4 c3 hint:b2 b2 c2 bomb:d3 d3 e2
```

### Boss rules

A boss level bends one rule in the computer's favour, and the player is told before the first
move. Only the computer has the rule.

| Rule | What the computer does | The level must have |
|------|------------------------|---------------------|
| `double move every 4 turns` | Places two dots on its turns 4, 8, 12 and so on, counting its own turns from 1. | Enough dots for the first double move: 5 here. |
| `fortress of 12 dots` | Starts with a fortress: a group of 12 of its dots on the map, joined up, down, left, right or diagonally. A large head start in one piece. | A group of exactly that many computer dots on the map, bigger than any other, and at least 8. |
| `two groups at once` | Places two dots on every turn, one in the left half of the board and one in the right half, in either order. | A board of even width, so the halves are equal. |

- **Two dots in one turn** follow the double dot booster: both come out of the computer's dots
  left, so it runs out sooner, and if no point is left for the second dot the turn ends after the
  first. With only one dot left, a double move is a single dot.
- **A pass or a stop** may come at the start of a two-dot turn, and uses it up. None between
  the two dots.
- **The countdown.** `GameState.doubleMoveIn(computer)` says how many of the computer's turns
  until its next double move: 0 while it places it, 1 if its next turn is one, and so on. It is
  null when no double move can come any more: no rule, the game is over or in collecting, or the
  computer will have fewer than 2 dots left by then. With `two groups at once`, every turn is a
  double move. `Level.fortress` lists the fortress's dots, for the app to draw.
- **Not checked as puzzles.** The puzzle checker counts one computer dot per turn, so it leaves
  out levels with `double move` or `two groups at once` (`check-levels` says "not checked").

### Board shapes and special points

A level's map can shape the board and mark special points. The computer, the puzzle checker and
replays all play them by the same rules.

| Map | Point | Rules |
|-----|-------|-------|
| `-` | Hole | No point here, so never a dot. Holes shape the board: along the map's edge they give it an odd outline. What a hole's edge is depends on the level's `holes` line, below. |
| `#` | Blocked point | Nobody may place a dot here. A ring passes over it as over an empty point and may enclose it, as with the wall booster. A dot next to it can be captured. |
| `+` | Bonus point | Either side may place a dot here. A dot captured on it counts 2 instead of 1: in the result, the missions and the stars. An empty bonus point inside a capture adds nothing and is out of play. |

**Hole edges.** A level with holes says on its `holes` line whether their edge is border:

- `holes: border`. The edge of every hole is part of the border. A dot next to a hole, up, down,
  left or right, can never be captured, like a dot on the edge of the board, and no ring can go
  round a hole.
- `holes: open`. A hole is like a blocked point: a ring may go round it, and the hole is then
  inside the captured area. A dot next to a hole can be captured.

Either way, a hole that reaches the edge of the map, directly or through other holes, is outside
the board, since no ring can go round it. So an odd outline plays the same with `border` and
`open`; the choice matters for holes inside the board. A dot that touches a hole only diagonally
can be captured either way: a ring may cut across the hole's corner.

A map without holes says `holes: none`, and a map with holes must say `border` or `open`.

`GameState.isBorder` says which points are on the border: the outer edge of the grid, and the
holes outside the board and the points next to them. The computer counts a dot on a bonus point
twice when it weighs a capture.

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

Tiers are `AiConfig`: search depth and breadth, how far it looks for threats, which part of the
board it looks at, and a variety margin. Five tiers are the [strength steps](#strength-steps),
from BEGINNER to HARD; EXPERT is kept for experiments.

**Variety.** So that a position does not always get the same answer, every candidate move gets a
random bonus of 0 to a small margin before the best is picked. A move that scores just below the
best is then played now and then, one a full margin below never. A captured dot is worth 10,000
points and touching one more dot 2 or 3, so the margins never trade away a capture; they only
choose between quiet moves.

| Tier     | Margin |
|----------|-------:|
| BEGINNER | 10     |
| EASY     | 10     |
| STEADY   | 10     |
| NORMAL   | 10     |
| HARD     | 5      |
| EXPERT   | 5      |

The steps below HARD get the wider margin: a weaker player picks among moves that look alike
more loosely. The margins stay small because, once a capture is more than
a few moves away, the only thing steering the computer towards it is that "touching one more dot"
score. A larger margin drowns it out: with a margin of 10, HARD on level 7 failed to capture a
player who only plays on the border in half its games.

`cli bench variety <tier> [margin]` answers 20 fixed mid-game positions 10 times each and counts
the different moves. HARD plays at least two different moves in 14 of the 20 positions (2 of 20
without a margin), BEGINNER in 19, EASY in 16, STEADY in 15, NORMAL in 17, EXPERT in 13. Over 80 paired games
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

| Tier     | Looks for attacks | Looks for danger | Attack weights (1, 2, 3, 4+ dots away) | Danger weights      |
|----------|------------------:|-----------------:|----------------------------------------|---------------------|
| BEGINNER | none              | none             | (not used)                             | (not used)          |
| EASY     | 2 dots            | 1 dot            | 3000, 600, 120, 30                     | 1500, 300, 60, 15   |
| STEADY   | 2 dots            | 1 dot            | 3000, 600, 120, 30                     | 1500, 300, 60, 15   |
| NORMAL   | 2 dots            | 1 dot            | 3000, 600, 120, 30                     | 1500, 300, 60, 15   |
| HARD     | 3 dots            | 2 dots           | 3000, 600, 120, 30                     | the same            |
| EXPERT   | 3 dots            | 3 dots           | 3000, 600, 120, 30                     | the same            |

HARD and EXPERT weigh defence and attack alike (`AiConfig.BALANCED`). The steps below HARD lean
to attack on purpose, as a weaker player does (`AiConfig.ATTACK_FIRST`, half the attack weights).

`cli bench balance 40` measures what each tier does. Each tier plays 40 games against a casual
stand-in: a computer that thinks two moves ahead, looks at the whole board and plays a random
nearby move a quarter of the time. The games use the
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
tier      attack  defend  both  neither      (moves)      of those   en prise  lost after attack  lost
BEGINNER   14.8%   10.3%  0.2%   74.6%        35.2%        19.1%      0.45      0.88               6.70
EASY       38.0%   15.0%  0.9%   46.1%        15.5%        56.4%      0.10      1.60               4.95
STEADY     18.2%   11.5%  0.2%   70.1%        22.9%        29.8%      0.15      1.00               3.10
NORMAL     19.3%    7.6%  0.1%   73.0%         2.7%        45.7%      0.05      0.43               1.03
HARD       37.1%    8.3%  0.4%   54.2%         5.9%        59.0%      0.08      0.75               1.78
EXPERT     31.4%    8.6%  0.5%   59.4%        10.2%        37.8%      0.13      0.68               1.50
```

What this shows:

- **The weights did not cause HARD's lean.** With lower danger weights (2500, 400, 80, 20) its
  shares move by less than a point: 37.4% attack, 8.3% defence. Even danger weights two to three
  times the attack weights (6000, 1800, 360, 90) leave HARD at 37% attack and 9% defence.
- **HARD attacks often because there is often something to attack.** It rarely needs to defend:
  one of its own groups is within two dots of capture on only 6% of its moves (EXPERT 10%). It
  almost never leaves a group one dot from capture to attack elsewhere (0.1 times per game or
  less).
- **The weaker steps show their blind spots.** BEGINNER, which sees no threats, has a group within
  two dots of capture on a third of its moves and loses almost 7 dots a game. EASY attacks as
  often as HARD but does not check the answer: it loses the most dots right after attacking (1.6
  a game). STEADY checks the answer but weighs few moves, and loses 3 dots a game. NORMAL looks
  only near the last dots, so it attacks less than HARD; against this stand-in it also loses
  fewer dots, yet it loses to HARD (see [Strength steps](#strength-steps)).
- **Looking as far for danger as for attacks made HARD passive.** With danger 3 dots out, HARD
  failed to capture players who only defend. Over 60 games per level, against a player who only
  plays on the border, it won level 7 in 27 games instead of 44. Against a stand-in that thinks
  two moves ahead it won level 8 in 23 games instead of 42. Against itself it was no stronger
  (81-68-11 over 160 games, p=0.33). So HARD looks for danger one dot less far than for attacks.

Equal weights cost HARD no strength. Over 80 paired games (`cli bench duel`, weights given as
`dangerWeightsB=2500,400,80,20` and so on), HARD with each set of danger weights against HARD
with 2500, 400, 80, 20:

```
danger weights               15×15, four crosses, 33 dots    10×10, a cross, 24 dots
3000, 600, 120, 30 (chosen)  40-34- 6  p=0.56                 11-11-58  p=1.00
3600, 900, 180, 45           35-37- 8  p=0.91
6000, 1800, 360, 90          37-38- 5  p=1.00
```

Endgame behaviour follows the grounding rule:

- **Stopping.** In a game without a dot budget the computer declares its dots grounded only when
  the game is already decided: its lead exceeds the opponent's score plus every dot of its own
  that is still exposed.
- **Collecting** (after the human stops). It builds the cheapest wall around each exposed enemy
  group, using the same cut computed exactly by `Cleanup`, until nothing is left to take. Known
  gap: a group already inside the computer's own closed chain would need a dividing wall, which
  the planner doesn't try.

### Strength steps

A weaker step plays like a weaker person: someone with less strategy who does not plan far ahead.
It does not play like a strong player who now and then plays at random. Every step below HARD
has the same three blind spots:

- **It looks only where the game just happened** (`focus` 2): it considers only points at most 2
  steps from the last dot each side placed. A ring closing elsewhere on the board goes unseen,
  whether it is the computer's chance or its danger. The blind spot holds while it thinks ahead,
  too: it expects its opponent to answer near the same dots.
- **It overlooks a ring that needs 2 more dots** around its own groups (`dangerReach` 1). It
  notices a group in danger only when one more dot would capture it.
- **It would rather attack than defend** (`dangerWeights` 1500, 300, 60, 15, half the attack
  weights). Between building its own ring and blocking one, it leans to building.

What sets the steps apart is how far ahead they think and how many moves they weigh:

| Step | Tier     | Thinks ahead               | Moves it weighs, at most (answers to each) | Looks for rings to close   |
|-----:|----------|----------------------------|--------------------------------------------|----------------------------|
| 1    | BEGINNER | its own move only          | 24                                | only one it can close now  |
| 2    | EASY     | its own move only          | 24                                | up to 2 dots from closing  |
| 3    | STEADY   | its move and the answer    | 4 (2)                             | up to 2 dots from closing  |
| 4    | NORMAL   | its move and the answer    | 20 (10)                           | up to 2 dots from closing  |
| 5    | HARD     | three moves                | 16 (8)                            | up to 3 dots from closing  |

HARD has none of the blind spots: it looks at the whole board, sees danger 2 dots away and weighs
attack and defence alike. BEGINNER sees no threats at all, its own or the opponent's: it plays
next to other dots and closes a ring when one more dot closes it. No step plays random moves
(`mistakeRate` 0); only the benchmarks' stand-in players use that.

**Each step beats the one before it.** `cli bench steps 160`: each step plays the next one, 160
games per pair in the testers' format (15×15, four random crosses, 33 dots each). The games are
played in pairs with the same random seeds and the sides swapped, so luck cancels out. W-L-D is
for the weaker step; *p* is a two-sided sign test on the decisive games (below 0.05 the
difference is unlikely to be chance).

```
                         15×15, four crosses, 33 dots     10×10, four crosses, 24 dots
                         (bench steps 160)                (bench steps 160 10 24)
1 BEGINNER vs 2 EASY      20-129-11  p<0.001               37-108-15  p<0.001
2 EASY     vs 3 STEADY    28-109-23  p<0.001               21-115-24  p<0.001
3 STEADY   vs 4 NORMAL    25- 96-39  p<0.001               20- 87-53  p<0.001
4 NORMAL   vs 5 HARD      44- 80-36  p=0.002               30- 93-37  p<0.001
```

With 80 games per pair (`cli bench steps 80`) every gap holds too; the closest is NORMAL against
HARD, 20-44-16 (p=0.004). The weaker step still wins some games: from about 1 in 8 to about 1 in 4.

Some weaknesses that sound human made no measurable difference, so no step relies on them alone
(80 paired games each, `cli bench duel`, in the format above):

- **Shorter sight for a computer that thinks two moves ahead.** Looking for attacks only one dot
  from closing and not looking for danger at all won 24 and lost 25 against looking 2 and 1 dots
  out: the search sees those captures anyway.
- **A focus alone.** Two moves ahead, looking 2 points around the last dots won 27 and lost 32
  against looking at the whole board; 3 points, 31 and 30. Only 1 point made a difference
  (16-32, p=0.03). But the blind spots add up: against HARD, two-move NORMAL with the whole board
  and equal weights won 32 and lost 42 (p=0.30), with its blind spots 20 and 44 (p=0.004).
- **Caring less about its own losses than its gains** (tried, not kept as a setting). Counting a
  dot it may lose at half, a quarter, a tenth or 2% of a dot it may win changed nothing: any loss
  still outweighs a quiet move. Ignoring losses completely made a two-move computer play like a
  one-move one.

### How strong is it, and how hard are the levels?

**Tiers.** `cli bench tiers 40 10 24`: every tier against every other, 40 paired games on 10×10
from a single cross, 24 dots each. W-L-D is for the first-named tier.

```
BEGINNER vs EASY     11-25- 4  p=0.029      EASY   vs NORMAL   0-36- 4  p<0.001
BEGINNER vs STEADY    4-34- 2  p<0.001      EASY   vs HARD     0-38- 2  p<0.001
BEGINNER vs NORMAL    1-30- 9  p<0.001      EASY   vs EXPERT   0-38- 2  p<0.001
BEGINNER vs HARD      0-37- 3  p<0.001      STEADY vs NORMAL   2-11-27  p=0.022
BEGINNER vs EXPERT    1-34- 5  p<0.001      STEADY vs HARD     1-19-20  p<0.001
EASY     vs STEADY    7-28- 5  p=0.001      STEADY vs EXPERT   2-16-22  p=0.001
NORMAL   vs HARD      0- 7-33  p=0.016      NORMAL vs EXPERT   4-10-26  p=0.180
HARD     vs EXPERT    2- 9-29  p=0.065
```

From a single cross the steps keep their order, but most games between the stronger tiers are
draws: equals rarely force a capture, so this format separates them less well than four crosses.
**EXPERT is no stronger than HARD** and thinks about three times as long, so the campaign does
not use it; it is kept for experiments with deeper search. Two identical configurations come out
exactly even by construction.

**Levels.** `cli bench levels 20 <stand-in>`: a stand-in plays red on each level, 20 games;
W-L-D in captures for the stand-in, and how many games passed the level's mission (for `win`,
the same as the wins). Each game stops when the mission is decided. The EASY step stands for a
weak player, the NORMAL step for a strong one, border for someone who only plays on the border.
Levels 1 to 3 have an easy computer, 4 and 5 a normal one, 6 to 8 a hard one.

```
                     EASY stand-in    NORMAL stand-in    border-only
1 Warm-up           15- 2- 3          16- 0- 4           0-20- 0
2 Open Field        17- 2- 1          18- 0- 2           0-20- 0
3 Scout              9- 7- 4          13- 3- 4           0-20- 0
4 Outpost            0-13- 7           1- 0-19           0-18- 2
5 Rescue             0-14- 6           1- 1-18           0- 0-20
6 Pincer             0-17- 3           2- 7-11           0-20- 0
7 Siege              1-14- 5           1- 5-14           0-15- 5
8 Grandmaster        0-20- 0           3-13- 4           0-20- 0
```

Difficulty rises for both stand-ins, but not smoothly for a weak player: level 4 is a wall,
because there the computer jumps from step 2 to step 4 and starts checking the answer to its
moves. Against a player who only plays on the border, the normal computer of level 5 draws every
game: with its focus on the last dots it does not go after the human's starting dots. A draw
does not pass a `win` level, so border play still passes none. The stand-ins are computers, not
people, and nobody has played the levels yet, so treat the curve as a first draft.

**Speed.** On a laptop, a HARD move on a 14×14 board takes about 40 ms (EXPERT about 130 ms).
Not yet measured on a phone.

### Which game format has the most action?

`cli bench variants 20`: each format played 20 times between two equal stand-ins, a *casual*
one (a computer that thinks two moves ahead, looks at the whole board and plays a random nearby
move 25% of the time) and a *strong* one (HARD). It
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
