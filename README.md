# Dots

A prototype of the paper-and-pencil game *Dots* (Kropki / Точки / Židi) for a future Android app:
two players alternately place dots on a grid and score by surrounding the other side's dots.

This repository holds the platform-independent game logic, a computer opponent, a sample
campaign, and a terminal front end to play it. The Android UI is not started yet.

## Layout

| Module   | What it is |
|----------|------------|
| `engine` | Pure Kotlin/JVM library (JVM 17 bytecode, stdlib only): rules, AI, campaign data. Meant to be imported by an Android module. |
| `cli`    | Terminal game and AI benchmark, built on `engine`. |

Packages in `engine`: `dots.engine` (rules, openings, grounding), `dots.ai` (computer opponent),
`dots.campaign` (levels).

## Run it

```sh
./gradlew test                      # engine tests
./gradlew :cli:installDist          # build the terminal game
cli/build/install/cli/bin/cli levels
cli/build/install/cli/bin/cli play 3                 # campaign level 3, you are red and move first
cli/build/install/cli/bin/cli vs normal 16 14 cross  # classic open-ended game against the computer
cli/build/install/cli/bin/cli pvp 20 16 double       # two players, one keyboard
cli/build/install/cli/bin/cli bench tiers 10         # do harder AI tiers really win?
cli/build/install/cli/bin/cli bench levels 10        # how does a stand-in human fare on each level?
```

Moves are a column letter and a row number (`e5`). `stop`, `done`, `pass` and `quit` also work.

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
   complete the intruder's own chain. (My first version captured it immediately; that was wrong.)
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

## Computer opponent

`SearchAi` is an alpha-beta search over captured dots, plus a small "pressure" bonus for dots
placed around enemy dots (without it the search is blind until a capture is one move away). Each
ply only considers cells near existing dots and expands the best few by immediate gain and a
neighbourhood heuristic. Difficulty is `AiConfig`: search depth, breadth, and a mistake rate for
the easy tier. Campaign difficulty also comes from the head-start dots and the dot budget.

Endgame behaviour follows the grounding rule:

- **Stopping.** In a game without a dot budget the computer declares its dots grounded only when
  the game is already decided: its lead exceeds the opponent's score plus every dot of its own
  that is still exposed.
- **Collecting** (after the human stops). It finds, for each exposed enemy group, the fewest
  cells that wall it in (a minimum vertex cut, via max-flow) and builds the cheapest one, until
  nothing is left to take. Known gap: a group already inside the computer's own closed chain
  would need a dividing wall, which the planner doesn't try.

### How strong is it, and how hard are the levels?

`cli bench tiers 10` (10×10, 16 dots each, W-L-D for the first-named tier):

```
EASY   vs NORMAL   0-2-8     EASY   vs HARD    0-6-4     EASY   vs EXPERT  0-4-6
NORMAL vs HARD     0-1-9     NORMAL vs EXPERT  0-0-10    HARD   vs EXPERT  0-1-9
```

`cli bench levels 6 normal` (a NORMAL stand-in plays red on each level; W-L-D for the human):

```
1 Warm-up 5-0-1    2 Open Field 4-0-2   3 Scout 0-0-6      4 Outpost 0-0-6
5 Rescue  0-1-5    6 Pincer     0-1-5   7 Siege 1-1-4      8 Grandmaster 2-0-4
```

Read this cautiously. A stronger tier never lost to a weaker one, and HARD beats EASY clearly, but
most games end 0-0: with this many dots on these boards neither side is forced into a capture. So
NORMAL, HARD and EXPERT are **not yet shown to differ from each other**, and campaign levels 3-7
don't yet produce decisive games against a stand-in player. Treat both the tiers and the level
curve as placeholders until real people have played them.

## Next steps

- Android UI (Jetpack Compose canvas or a game engine) on top of `engine`: board, zoom, a
  precise-placement aid for fingers, capture animations (`MoveResult.captures` carries the dots
  and the region for exactly that), highlighting grounded dots (`Grounding`).
- Interactive tutorial; themes and piece shapes as cosmetics.
- Tune the campaign by playing it; add objectives beyond "more captures" and a daily puzzle.
- Async online play, then ranked.
- A stronger AI evaluation: the cut size used for collecting is also the natural "how many dots
  until this group is captured" measure for a search heuristic.
- Profile the deepest AI tier on a real phone; the capture check is a flood fill and can be made
  incremental if needed.
