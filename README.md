# Dots

A prototype of the paper-and-pencil game *Dots* (Kropki / Точки / Židi) for a future Android app:
two players alternately place dots on a grid and win points by surrounding the other side's dots.

This repository holds the platform-independent game logic, a computer opponent, a sample
campaign, and a terminal front end to play it. The Android UI is not started yet.

## Layout

| Module   | What it is |
|----------|------------|
| `engine` | Pure Kotlin/JVM library (JVM 17 bytecode, stdlib only): rules, AI, campaign data. Meant to be imported by an Android module. |
| `cli`    | Terminal game and AI benchmark, built on `engine`. |

Packages in `engine`: `dots.engine` (rules), `dots.ai` (computer opponent), `dots.campaign` (levels).

## Run it

```sh
./gradlew test                      # engine tests
./gradlew :cli:installDist          # build the terminal game
cli/build/install/cli/bin/cli levels
cli/build/install/cli/bin/cli play 3          # campaign level 3, you are red and move first
cli/build/install/cli/bin/cli pvp 12 12 20    # two players, one keyboard
cli/build/install/cli/bin/cli bench tiers 10  # do harder AI tiers really win?
cli/build/install/cli/bin/cli bench levels 10 # how does a stand-in human fare on each level?
```

Moves are a column letter and a row number (`e5`); `pass` and `quit` also work.

## Rules as implemented

1. Players alternate placing one dot on an empty intersection. Red moves first.
2. A player's dots connect orthogonally **and** diagonally. When they form a closed ring around
   at least one live enemy dot, every enemy dot inside is captured and scores one point each.
   The ring owner's own dots inside count for nothing.
3. Everything inside a capturing ring is out of play: no more moves there, and captured dots stop
   being part of any chain. A bigger ring can still capture the rim of an earlier ring.
4. A ring around only empty cells (or only its owner's dots) captures nothing, and the inside
   stays playable. **An enemy dot placed inside such a ring is captured immediately.**
5. Dots on the board's edge can never be enclosed.
6. Optional dot budget per player (`dotsPerPlayer`): the game ends when both have placed all of
   theirs. Without a budget it ends when the board is full or both players pass in a row.
7. A level may start with dots already on the board (the computer's head start). They don't count
   against the budget, and a start position may not already contain a capture.

### Open questions

I could not open the Wikipedia article from the sandbox these were built in, so the rules above
come from search excerpts of it plus general knowledge of Kropki/Points. Two points are worth
checking against the article and the tournament rules before anything is final:

- Rule 4 (a dot dropped into an empty enemy ring is lost). Some rule sets leave it alive and
  playable; the change is local to `GameState.applyMove`.
- Endgame: tournament Points uses a "grounding" declaration to end the game. Here the dot budget
  (or two passes) ends it, which suits short mobile sessions.

## Computer opponent

`SearchAi` is an alpha-beta search over the captured-dot difference. Each ply only considers cells
near existing dots and expands the best few moves by immediate gain and a neighbourhood
heuristic. Difficulty is `AiConfig`: search depth, breadth, and a mistake rate for the easy tier.
Campaign difficulty also comes from the head-start dots on the map and the dot budget.

Leaf positions are scored by captured dots plus a small "pressure" bonus for dots placed around
enemy dots; without it the search was blind until a capture was one move away.

### How strong is it? (`cli bench tiers 10`, 10x10, 16 dots each, W-L-D for the first-named tier)

```
EASY   vs NORMAL   0-2-8     EASY   vs HARD    0-6-4     EASY   vs EXPERT  0-4-6
NORMAL vs HARD     0-1-9     NORMAL vs EXPERT  0-0-10    HARD   vs EXPERT  0-1-9
```

Read this cautiously. A stronger tier never lost to a weaker one, and HARD beats EASY clearly,
but most games are draws: with 16 dots on a 10x10 board neither computer is forced into a
capture. So NORMAL/HARD/EXPERT are **not yet shown to be different from each other**. Ways to
separate them: smaller boards or bigger budgets in the benchmark, a stronger evaluation, and
above all playing against real people. Treat the tiers as placeholders until then.

## Next steps

- Android UI (Jetpack Compose canvas or a game engine) on top of `engine`: board, zoom, a
  precise-placement aid for fingers, capture animations (`MoveResult.captures` carries the dots
  and the region for exactly that).
- Interactive tutorial; themes and piece shapes as cosmetics.
- Tune the campaign by playing it; add objectives beyond "more captures" and a daily puzzle.
- Async online play, then ranked.
- Profile the deepest AI tier on a real phone; the capture check is a full-board flood fill and
  can be made incremental if needed.
