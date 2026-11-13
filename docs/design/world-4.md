# World 4: Sea

World 4 has 20 levels. Its new ideas are two kinds of special point: **rocks** (`#`, blocked
points), where nobody may place a dot but a ring may pass over, and **pearls** (`+`, bonus
points), where a captured dot counts 2. It ends with the **Kraken**, the first real boss: the
normal computer, placing two dots every turn. All five boosters are available. The level files
are in [`engine/src/main/resources/levels/`](../../engine/src/main/resources/levels/); the rules
for special points are in the
[README](../../README.md#board-shapes-and-special-points).

## The shape of the world

| Levels | What the player meets |
| --- | --- |
| 1 and 2 | Rocks block a way out; a ring passes over a rock, and a dot caught on a pearl counts 2. |
| 3 to 5 | A survive between rocks, a target that closes beyond a rock, a capture round a rock. |
| 6 | Rocky Shore, the first whole game with rocks, and every booster. |
| 7 to 11 | Survive and capture puzzles on boards with rocks and pearls. |
| 12 | Pearls: a whole game with eight pearls in the middle. |
| 13 to 17 | A dot run onto a pearl, a survive along the bottom, the longest capture, a run to the edge, a target that turns twice. |
| 18 | Reef: open holes, rocks and pearls, with fewer dots than the computer. |
| 19 | A capture that closes beyond a rock. |
| 20 | The Kraken: the normal computer, and two dots every turn, one in each half of the board. |

The difficulty is a saw-tooth: levels 6, 12 and 18 are whole games, harder than the puzzles
around them, and 20 is the boss. World 4 has the most hard puzzles: 4-4, 4-5, 4-15, 4-17 and
4-19. Each answer is three moves deep, and its first move is not the one that looks natural: a
gap next to the dot that does not hold, or a fight elsewhere that draws the eye. The
[difficulty curve](difficulty.md) measures them.

## How the levels are built

- **Every puzzle has an answer that works against any defence,** and only one or two first
  moves pass it. `cli check-levels` proves it, and CI runs it.
- **Puzzles give two spare moves,** as in World 1: a puzzle whose shortest answer takes 3 moves
  gives 5, and `stars: moves 1 2` asks for both spares for three stars.
- **Survive levels give two boosters,** plus the Undo every level has, and their stars count
  the boosters left (`stars: boosters 2 3`): three stars without a booster, two with one used,
  one with two or more used. Every survive level
  can be passed without them; the solver never uses a booster.
- **The computer is the steady step** for every level but the last boss of World 4, so the
  difficulty comes from the boards, the missions and the dot budgets.
- **Every level file has a one-line `note`,** repeated in italics below.
- **Most puzzles started from the computer's own games or from random small positions,** and were
  kept only when the solver proved them and found one or two winning first moves. Their names
  and notes were written by hand.
- **Boosters:** survive levels give a wall and a bomb, captures a hint and a double dot, targets a wall and a hint. Whole games give all four. Every level also gives one Undo.
- **Pearls in puzzles.** A level map cannot start a dot on a pearl, so a pearl counts in a puzzle when the computer runs onto one and is captured there (4-2, 4-13).

## How hard it is

`cli bench levels 80 <stand-in>`: a stand-in plays red on each level, and the table shows how many
games passed the level's mission. The EASY step stands for a weak player, the NORMAL step for a
strong one, border-only for someone who only plays on the border. The README explains the bench
([How strong is it, and how hard are the levels?](../../README.md#how-strong-is-it-and-how-hard-are-the-levels)).

```
                                             passed of 80   passed of 80     passed of 40
level                 mission                EASY stand-in  NORMAL stand-in  border-only
4-1   Rocks           capture 1 in 4 moves    18              9               0
4-2   Oyster          capture 3 in 4 moves     1              2               0
4-3   Tide Pool       survive 4 moves          0             80               0
4-4   Anchor          target c5               30             40               0
4-5   Rock Pool       capture 2 in 4 moves    18             12               0
4-6   Rocky Shore     win                     22             45               0
4-7   Seagull         survive 4 moves          0             80               0
4-8   Coral           capture 2 in 4 moves    24             20               0
4-9   Shipwreck       capture 2 in 5 moves    18             80               0
4-10  Harbour         capture 1 in 4 moves    71             61               0
4-11  Buoy            survive 4 moves         77             80               0
4-12  Pearls          win                     16             37               0
4-13  Starfish        capture 1 in 4 moves    34             26               0
4-14  Lighthouse      survive 4 moves         58             80               0
4-15  Whirlpool       capture 1 in 5 moves     0             80               0
4-16  Sandbar         survive 4 moves         80             80               0
4-17  Driftwood       target e6                5             80               0
4-18  Reef            win with 19 dots        21             24               0
4-19  Cove            capture 1 in 5 moves     2             80               0
4-20  Kraken          win                      8             26               0
```

- **Whole games get harder through the world** for a weak player: 22, 16, 21 and 8 passes of 80
  for Rocky Shore, Pearls, Reef and the Kraken. Rocky Shore is easier than the Comet, so the
  world opens with a dip. Pearls and Reef are equal within the noise of 80 games.
- **The puzzles look harder here than they are,** as in World 1: the stand-ins play for captures
  and do not read the mission. That every puzzle can be passed is what `check-levels` proves; how
  hard each feels needs people.
- **Border-only play passes no level of the world.** Before its first dot, the computer looks at
  the whole board, so a danger already on the board is attacked even when the player opens far
  from it.
- These are stand-ins, not people, and nobody has played these levels yet: treat the numbers as a
  first draft.

## Trying the levels

Every level plays in the terminal game; you are red and move first:

```sh
./gradlew :cli:installDist
cli/build/install/cli/bin/cli play 4-1     # any level, 4-1 to 4-20
cli/build/install/cli/bin/cli check-levels # the puzzle answers
```

## The levels

Boards as in the level files: `R` the player's dots, `B` the computer's, `.` an empty point, `-` a hole, `#` a rock (blocked point), `+` a pearl (bonus point).
Columns are letters from the left, rows numbers from the top. Puzzle answers show the
computer's replies in brackets, as `check-levels` prints them.

### 4-1 Rocks

`capture 1 in 4 moves` · computer: steady · 4 dots each · boosters: `hint 1, double-dot 1, undo 1` · stars: `moves 1 2`

*Rocks: nobody can place a dot on #. The rock at d6 blocks the way out, so d5 closes one side and d3 then shuts d4 in.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . . . . .
  3 . . # . R . . . .
  4 . . R B R # . . .
  5 . R B . B R . . .
  6 . . . # . . . . .
  7 . . B . R . # . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 2 moves: d5 (c6) d3; 1 first move passes: d5.

### 4-2 Oyster

`capture 3 in 4 moves` · computer: steady · 4 dots each · boosters: `hint 1, double-dot 1, undo 1` · stars: `moves 1 2`

*A ring passes over a rock, and a dot on a pearl counts 2. Close above over the rock; if the computer runs onto the pearl, close below it.*

```
    a b c d e f g h i j
  1 . . . . . . . . . .
  2 . . . . . . . . . .
  3 . . . # . . + . . .
  4 . . . . . . . . . .
  5 . . + . . . . # . .
  6 . R # R . . . + . .
  7 R B B B R . . . . .
  8 . R R + R . # . . .
  9 . . . . . . . . . .
 10 . . . . . . . . . .
```

`check-levels`: passes in 2 moves: c5 (d8) d9; 1 first move passes: c5.

### 4-3 Tide Pool

`survive 4 moves` · computer: steady · 4 dots each · boosters: `wall 1, bomb 1, undo 1` · stars: `boosters 2 3`

*Hard: survive four moves. Fill the last gap beside your dot at e5, then answer the attacks on the right.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . . . . .
  3 . . # . . . . . .
  4 . . . . B # . . .
  5 . . . B R . . B .
  6 . . . # B R . R .
  7 . . . . . . # . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 4 moves: f5 (g6) i6 (f7) g4; 1 first move passes: f5.

### 4-4 Anchor

`target c5` · computer: steady · 5 dots each · boosters: `wall 1, hint 1, undo 1` · stars: `moves 1 2`

*Target: the ring may pass over the rock at d6, so close the right side beyond it, not the gap at d4; then follow the runs left and down.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . R . . . . .
  3 . . # . . . . . .
  4 . . R . . # . . .
  5 . R B R . . . . .
  6 . . B # . . . . .
  7 . R . R . . # . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 3 moves: e6 (b6) a6 (c7) c8; 1 first move passes: e6.

### 4-5 Rock Pool

`capture 2 in 5 moves` · computer: steady · 5 dots each · boosters: `hint 1, double-dot 1, undo 1` · stars: `moves 1 2`

*Hard: the pair has three ways out; close the left one first, then follow it right, and close again with a ring over the rock.*

```
    a b c d e f g h i j
  1 . . . . . . . . . .
  2 . . . . . . . . . .
  3 . . . # R . + . . .
  4 . . . . B R . . . .
  5 . . + . B . . # . .
  6 . . # R R R . + . .
  7 . . . . . . . . . .
  8 . . . + . . # B . .
  9 . . . . . . . . . .
 10 . . . . . . . . . .
```

`check-levels`: passes in 3 moves: d4 (f5) g5 (d5) c5; 1 first move passes: d4.

### 4-6 Rocky Shore

`win` · computer: steady · 26 dots each · boosters: `wall 1, hint 1, bomb 1, double-dot 1, undo 1` · stars: `margin 2 4`

*World 4's first whole game: four rocks near the edges and two in the middle, four crosses, and every booster.*

```
    a b c d e f g h i j k l m n
  1 . . . . . . . . . . . . . .
  2 . . . . . . # . . . . . . .
  3 . . . . . . . . . . . . . .
  4 . . . R B . . . . B R . . .
  5 . . . B R . . . . R B . . .
  6 . . . . . . . . . . . . . .
  7 . . . . . . . . . . . . # .
  8 . # . . . . . . . . . . . .
  9 . . . . . . . . . . . . . .
 10 . . . B R . . . . R B . . .
 11 . . . R B . . . . B R . . .
 12 . . . . . . . . . . . . . .
 13 . . . . . . . # . . . . . .
 14 . . . . . . . . . . . . . .
```

### 4-7 Seagull

`survive 4 moves` · computer: steady · 4 dots each · boosters: `wall 1, bomb 1, undo 1` · stars: `boosters 2 3`

*One gap left above your dot: fill it, then keep the next ring from closing.*

```
    a b c d e f g h i j
  1 . . . . . . . . . .
  2 . . . . . . . . . .
  3 . . . # . . + . . .
  4 . . . . . . . . . .
  5 . . + . . . . # . .
  6 . . # . B R B + . .
  7 . . B . . B . . . .
  8 . . . + . . # . . .
  9 . . . . . . . . . .
 10 . . . . . . . . . .
```

`check-levels`: passes in 4 moves: f5 (e5) g5; 1 first move passes: f5.

### 4-8 Coral

`capture 2 in 4 moves` · computer: steady · 4 dots each · boosters: `hint 1, double-dot 1, undo 1` · stars: `moves 1 2`

*Close below the pair; when it runs left, one more dot closes round both.*

```
    a b c d e f g h i j
  1 . . . . . . . . . .
  2 . . . . . R R . . .
  3 . . . # . R B R . .
  4 . . . . . . B R . .
  5 . . + . . R . # . .
  6 . . # . B . . + . .
  7 . . . . . . . . . .
  8 . . . + . . # . . .
  9 . . . . . . . . . .
 10 . . . . . . . . . .
```

`check-levels`: passes in 2 moves: g5 (f4) e4; 1 first move passes: g5.

### 4-9 Shipwreck

`capture 2 in 5 moves` · computer: steady · 5 dots each · boosters: `hint 1, double-dot 1, undo 1` · stars: `moves 1 2`

*Hard: close the right side against the rock; the pair runs up, and you follow it twice.*

```
    a b c d e f g h i j
  1 . . . . . . . . . .
  2 . . . . . . . . . .
  3 . . . # . . + . . .
  4 . . . . R . . . . .
  5 . . + . . . . # . .
  6 . . # R . . R + . .
  7 . . . R B B . . . .
  8 . . . + R R # . . .
  9 . . . R . . . . . .
 10 . . . . . . . . . .
```

`check-levels`: passes in 3 moves: g7 (e6) e5 (f6) f5; 1 first move passes: g7.

### 4-10 Harbour

`capture 1 in 4 moves` · computer: steady · 4 dots each · boosters: `hint 1, double-dot 1, undo 1` · stars: `moves 1 2`

*One dot threatens both computer dots, and the rock means the top one has nowhere to run.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . B . . .
  3 R . # . . . . . .
  4 R B . R . # . . .
  5 R R B R . . . . .
  6 . . . # . . . . .
  7 . . . . . . # . .
  8 . . . R . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 2 moves: c4 (c6) b3; 1 first move passes: c4.

### 4-11 Buoy

`survive 4 moves` · computer: steady · 4 dots each · boosters: `wall 1, bomb 1, undo 1` · stars: `boosters 2 3`

*A lone dot with the computer on three sides: run down between the rocks.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . . . . .
  3 . . # . . . . . .
  4 . . . . B # . . .
  5 . . . B R B . . .
  6 . . . # . . . . .
  7 . . . . . . # . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 4 moves: e6 (f6) e7; 1 first move passes: e6.

### 4-12 Pearls

`win` · computer: steady · 26 dots each · boosters: `wall 1, hint 1, bomb 1, double-dot 1, undo 1` · stars: `margin 3 5`

*A whole game with eight pearls in the middle: a dot captured on a pearl counts 2, for both sides.*

```
    a b c d e f g h i j k l m n
  1 . . . . . . . . . . . . . .
  2 . . . . . . . . . . . . . .
  3 . . . . . . . . . . . . . .
  4 . . . R B . . . . B R . . .
  5 . . . B R . . + . R B . . .
  6 . . . . . . . . . . . . . .
  7 . . . . + . + + . . . . . .
  8 . . . . . . + + . + . . . .
  9 . . . . . . . . . . . . . .
 10 . . . B R . + . . R B . . .
 11 . . . R B . . . . B R . . .
 12 . . . . . . . . . . . . . .
 13 . . . . . . . . . . . . . .
 14 . . . . . . . . . . . . . .
```

### 4-13 Starfish

`capture 1 in 4 moves` · computer: steady · 4 dots each · boosters: `hint 1, double-dot 1, undo 1` · stars: `moves 1 2`

*Close the left side; the dot runs onto the pearl, and when the ring closes below it, it counts 2.*

```
    a b c d e f g h i j
  1 . . . . . . . . . .
  2 . . . . . . . . . .
  3 . . . # . . + . . .
  4 . . . . . . . . . .
  5 . . + . . . . # . .
  6 . . # R . . . + . .
  7 . . . B R . . . . .
  8 . . R + R . # . . .
  9 . B . . . . . . . .
 10 . . . . . . . . . .
```

`check-levels`: passes in 2 moves: c7 (d8) d9; 1 first move passes: c7.

### 4-14 Lighthouse

`survive 4 moves` · computer: steady · 4 dots each · boosters: `wall 1, bomb 1, undo 1` · stars: `boosters 2 3`

*Fill the gap under your dot, then run along the bottom while the computer follows.*

```
    a b c d e f g h i j
  1 . . . . . . . . . .
  2 . . . . . . . . . .
  3 . . . # . . + . . .
  4 . . . . . . . . . .
  5 . . + . . . . # . .
  6 . . # . . B . + . .
  7 . . . . B R B . . .
  8 . . . + B . # . . .
  9 . . . . . . . . . .
 10 . . . . . . B . . .
```

`check-levels`: passes in 4 moves: f8 (f9) h8 (h7) h9; 1 first move passes: f8.

### 4-15 Whirlpool

`capture 1 in 5 moves` · computer: steady · 5 dots each · boosters: `hint 1, double-dot 1, undo 1` · stars: `moves 1 2`

*Hard: close below the left dot first, not above it; when it runs up, follow it round.*

```
    a b c d e f g h i j
  1 . . . . . . . . . .
  2 . . . . . . . . . .
  3 . . . # . . + . . .
  4 . . . . . . R . . .
  5 . . + . R . . # . .
  6 . . # . . . R + . R
  7 . . . R B R B B . .
  8 . . . + . . # R . .
  9 . . . . . . R . . .
 10 . . . . . . . . . .
```

`check-levels`: passes in 3 moves: e8 (e6) d6 (f6) f5; 1 first move passes: e8.

### 4-16 Sandbar

`survive 4 moves` · computer: steady · 4 dots each · boosters: `wall 1, bomb 1, undo 1` · stars: `boosters 2 3`

*Your dot at e3 has one way out: d3, then up towards the top edge.*

```
    a b c d e f g h i
  1 . . . . . . . R .
  2 . . . . B B . . .
  3 . . # . R B . . .
  4 . . . . B # . . .
  5 . . . . . . . . .
  6 . . . # . . . . .
  7 . . . . . . # . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 4 moves: d3 (g2) d2; 1 first move passes: d3.

### 4-17 Driftwood

`target e6` · computer: steady · 5 dots each · boosters: `wall 1, hint 1, undo 1` · stars: `moves 1 2`

*Target: leave the fight at the top alone and close the left; the dot runs down, then up, and you follow both times.*

```
    a b c d e f g h i j
  1 . . . . . . . . . .
  2 . . . . . R B . . .
  3 . . . # . B + . . .
  4 . . . . . . . . . .
  5 . . + R . R . # . .
  6 . . # . B R . + . .
  7 . . . R . R . . . .
  8 . . . + . . # . . .
  9 . B . . . . . . . .
 10 . . . . . . . . . .
```

`check-levels`: passes in 3 moves: d6 (e7) e8 (e5) e4; 1 first move passes: d6.

### 4-18 Reef

`win with 19 dots` · computer: steady · 28 dots each · boosters: `wall 1, hint 1, bomb 1, double-dot 1, undo 1` · stars: `margin 2 4` · holes: `open`

*Hard: open holes, rocks and pearls, three computer dots ahead, and 19 dots against its 28.*

```
    a b c d e f g h i j k l m n
  1 . . . . . . . . . . . . . .
  2 . . . . . . . . . . . . . .
  3 . . . . . . # . . . . - . .
  4 . . . R B . . B . B R . . .
  5 . . . B R . . . . R B . . .
  6 . . . . . + . . + . . . . .
  7 . . . . . . - - . . B # . .
  8 . . # . . . - - . . . . . .
  9 . . . . . + . . + . . . . .
 10 . . . B R . . . . R B . . .
 11 . . . R B . B . . B R . . .
 12 . . - . . . . # . . . . . .
 13 . . . . . . . . . . . . . .
 14 . . . . . . . . . . . . . .
```

### 4-19 Cove

`capture 1 in 5 moves` · computer: steady · 5 dots each · boosters: `hint 1, double-dot 1, undo 1` · stars: `moves 1 2`

*The ring may pass over the rock: b6 closes the left beyond it, while e6 by the dot does not hold; then follow the dot right and up.*

```
    a b c d e f g h i j
  1 . . . . . . . . . .
  2 . . . . . . . . . .
  3 . . R # . . + . . .
  4 . . . . . R R . . .
  5 . . R . R . . # . .
  6 . . # B . . . + . .
  7 . . R R R . R . . .
  8 . . . + . . # . . .
  9 . . . . . . . . . .
 10 . . . . . . . . . .
```

`check-levels`: passes in 3 moves: b6 (e6) f6 (d5) d4; 1 first move passes: b6.

### 4-20 Kraken

`win` · computer: normal · 30 dots each · boosters: `wall 1, hint 1, bomb 1, double-dot 1, undo 1` · rule: two groups at once · stars: `margin 3 8`

*The first real boss: the normal computer, and every turn it places two dots, one in each half of the board.*

```
    a b c d e f g h i j k l m n
  1 . . . . . . . . . . . . . .
  2 . . . . . . . . . . . . . .
  3 . . . . . . . . . . . . . .
  4 . . R B . . . . B R . . . .
  5 . . B R . . . . R B . . . .
  6 . . . . . . B . . . . . . .
  7 . . . . . . # + . . . . . .
  8 . . . . . . + # . . . . . .
  9 . . . . . . . B . . . . . .
 10 . . . . B R . . . . R B . .
 11 . . . . R B . . . . B R . .
 12 . . . . . . . . . . . . . .
 13 . . . . . . . . . . . . . .
 14 . . . . . . . . . . . . . .
```
