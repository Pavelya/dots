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
  or three away is a smaller liability. Before this, deeper search made the computer *worse*
  (depth 5 lost 0-8 to depth 3), because a position where a group is slowly being strangled looked
  the same as a safe one.
- **Collecting** after the opponent stops (below).

Tiers are `AiConfig`: search depth and breadth, how far ahead it looks for threats, and a mistake
rate. EASY is deliberately blind to threats and plays a random nearby move a third of the time.
Campaign difficulty also comes from the head-start dots and the dot budget.

Endgame behaviour follows the grounding rule:

- **Stopping.** In a game without a dot budget the computer declares its dots grounded only when
  the game is already decided: its lead exceeds the opponent's score plus every dot of its own
  that is still exposed.
- **Collecting** (after the human stops). It builds the cheapest wall around each exposed enemy
  group, using the same cut computed exactly by `Cleanup`, until nothing is left to take. Known
  gap: a group already inside the computer's own closed chain would need a dividing wall, which
  the planner doesn't try.

### How strong is it, and how hard are the levels?

`cli bench tiers 20 10 24` (20 games per pair, 10×10 from a cross, 24 dots each; W-L-D for the
first-named tier; a HARD move takes tens of milliseconds on a laptop; not yet measured on a phone):

```
EASY   vs NORMAL   0-19- 1     EASY   vs HARD    1-19- 0     EASY   vs EXPERT  0-20- 0
NORMAL vs HARD     0- 5-15     NORMAL vs EXPERT  0- 4-16     HARD   vs EXPERT  2- 3-15
```

`cli bench levels 20 <tier>` (a stand-in player of that tier is red on each level; W-L-D for the
stand-in, 20 games):

```
                     EASY stand-in    NORMAL stand-in
1 Warm-up            9- 4- 7          14- 0- 6
2 Open Field        11- 5- 4          16- 0- 4
3 Scout              3- 5-12           5- 1-14
4 Outpost            3-10- 7           5- 0-15
5 Rescue             0-19- 1           1- 3-16
6 Pincer             0-19- 1           0- 4-16
7 Siege              0-17- 3           0- 3-17
8 Grandmaster        0-20- 0           2-15- 3
```

What this does and does not show:

- There are three real strength levels, **EASY ≪ NORMAL < HARD**, and the campaign now gets steadily
  harder for both a weak and a strong stand-in. (The first version of the AI drew almost every
  game 0-0, so its levels could not be told apart.)
- **EXPERT is not measurably stronger than HARD** (2-3-15) and takes about three times as long to
  think, so the campaign does not use it. It is kept only for experiments with deeper search.
- Strong players still draw a lot against the stronger computers: games between equals rarely
  force a capture.
- The stand-ins are computers, not people. Levels have not been played by anyone yet, so treat the
  difficulty curve as a first draft.

## Next steps

- Android UI (Jetpack Compose canvas or a game engine) on top of `engine`: board, zoom, a
  precise-placement aid for fingers, capture animations (`MoveResult.captures` carries the dots
  and the region for exactly that), highlighting grounded dots (`Grounding`).
- Interactive tutorial; themes and piece shapes as cosmetics.
- Tune the campaign by playing it; add objectives beyond "more captures" and a daily puzzle.
- Async online play, then ranked.
- Find out whether deeper search can be made to pay off (it does not yet beyond depth 3), and
  profile HARD on a real phone; the capture check is a flood fill and can be made incremental
  if needed.
- Score levels by margin (stars) rather than just win/lose, since equal play often ends in a draw.
