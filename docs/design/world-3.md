# World 3: Night sky

World 3 has 20 levels on boards with odd shapes: holes in the board, drawn as `-`. Its ideas
are **the lake**, a hole whose edge counts as border (`holes: border`), so a dot touching it can
never be captured, for the player or the computer; **the open hole** (`holes: open`), which a
ring can go round, so it shelters nobody; and **round boards** whose corners are holes. The
**bomb** and the **double dot** boosters arrive here. The level files are in
[`engine/src/main/resources/levels/`](../../engine/src/main/resources/levels/); the rules for
holes are in the [README](../../README.md#board-shapes-and-special-points).

## The shape of the world

| Levels | What the player meets |
| --- | --- |
| 1 and 2 | The lake: your dot by the shore is safe; then the computer's would be too, so take the shore first. |
| 3 to 5 | A longer survive by the lake, a capture and a run on a round board. |
| 6 | Full Moon, the first whole game on a round board, with a bomb and a double dot. |
| 7 to 11 | The lake again, a target, an open hole that takes away the easy answer, a capture and a run to the edge on round boards. |
| 12 | Crater: a whole game round an open hole, with fewer dots than the computer. |
| 13 to 17 | Shut the way to the lake, an open hole that is no shelter, join a dot to the shore. |
| 18 | Twin Stars: two round halves joined in the middle. |
| 19 | The dot by the lake cannot be captured, so capture its neighbour. |
| 20 | The Comet, World 3's boss: a double move every third turn, on a round board. |

The difficulty is a saw-tooth: levels 3, 6, 9, 12, 15 and 18 are the hard ones, and the two
after each are easier. Levels 6, 12 and 18 are whole games and 20 is the boss; levels 3, 9 and
15 are puzzles whose answers take three or four moves.

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
- **Boosters:** survive levels give a bomb and a double dot; captures and targets a double dot (a target never allows the bomb, which would remove the target). Whole games give a bomb and a double dot. Every level also gives one Undo.
- **Holes:** the lake levels say `holes: border`, the open-hole levels `holes: open`. Round boards cut their corners with holes; a hole that reaches the edge of the map is outside the board either way, so they say `holes: border`.

## How hard it is

`cli bench levels 80 <stand-in>`: a stand-in plays red on each level, and the table shows how many
games passed the level's mission. The EASY step stands for a weak player, the NORMAL step for a
strong one, border-only for someone who only plays on the border. The README explains the bench
([How strong is it, and how hard are the levels?](../../README.md#how-strong-is-it-and-how-hard-are-the-levels)).

```
                                             passed of 80   passed of 80     passed of 40
level                 mission                EASY stand-in  NORMAL stand-in  border-only
3-1   Lake Shore      survive 3 moves          5             80               0
3-2   Shoreline       target e3               31             80               0
3-3   Night Swim      survive 4 moves         13             79               0
3-4   Crescent        capture 2 in 4 moves    20             20               0
3-5   Lantern         survive 4 moves         66             80               0
3-6   Full Moon       win                     24             54               0
3-7   Fireflies       survive 3 moves          4             80               0
3-8   Owl             target f6                4              6               0
3-9   Crater Rim      capture 2 in 5 moves    18              9               0
3-10  Telescope       capture 1 in 4 moves     7              4               0
3-11  Shooting Star   survive 4 moves         67             80               0
3-12  Crater          win with 20 dots        18             31               0
3-13  Reflection      target f3                0              0               0
3-14  Pole Star       capture 1 in 4 moves     1              7               0
3-15  Black Pool      survive 4 moves         18             80               0
3-16  Moon Bay        survive 4 moves         75             80               0
3-17  Dusk            target d4               79             49               0
3-18  Twin Stars      win with 19 dots        17             39               0
3-19  Stargazer       capture 1 in 4 moves    28             27               0
3-20  Comet           win with 23 dots        18             18               0
```

- **Whole games get harder after the first** for a weak player: 24, 18, 17 and 18 passes of 80
  for Full Moon, Crater, Twin Stars and the Comet; the last three are equal within the noise of
  80 games. Full Moon is easier than the Mole, so the world opens with a dip. For the average
  player of the [difficulty curve](difficulty.md), the Comet is the hardest level of the world.
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
cli/build/install/cli/bin/cli play 3-1     # any level, 3-1 to 3-20
cli/build/install/cli/bin/cli check-levels # the puzzle answers
```

## The levels

Boards as in the level files: `R` the player's dots, `B` the computer's, `.` an empty point, `-` a hole.
Columns are letters from the left, rows numbers from the top. Puzzle answers show the
computer's replies in brackets, as `check-levels` prints them.

### 3-1 Lake Shore

`survive 3 moves` · computer: steady · 3 dots each · boosters: `bomb 1, double-dot 1, undo 1` · stars: `boosters 2 3` · holes: `border`

*A lake's edge is border: your dot at e4 touches the lake and can never be captured, so only d3 needs its gap filled.*

```
    a b c d e f g h i
  1 . . . . B R . . .
  2 . . . B B R . . .
  3 . . . R B R . . .
  4 . . . B R . . . .
  5 . . . . - . . . .
  6 . . . . B R . . .
  7 . . . . R B . . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 3 moves: c3 (g6) f5 (d7) e8 (d5); 1 first move passes: c3.

### 3-2 Shoreline

`target e3` · computer: steady · 5 dots each · boosters: `double-dot 1, undo 1` · stars: `moves 1 2` · holes: `border`

*The lake guards the computer too: take e4 by the shore before the marked dot gets there, then follow its run.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . R . R . . .
  3 . . R . B R . . .
  4 . . . B . R . . .
  5 . . R R - . . . .
  6 . . . . . . . . .
  7 . . . . . R . . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 3 moves: e4 (d3) e2 (c4) b4; 1 first move passes: e4.

### 3-3 Night Swim

`survive 4 moves` · computer: steady · 4 dots each · boosters: `bomb 1, double-dot 1, undo 1` · stars: `boosters 2 3` · holes: `border`

*Hard: the dot by the lake is safe, the one at f3 is not. Close its gap, then answer the attacks on both sides for four moves.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . . . . .
  3 . . . . B R B . .
  4 . . . . . B R . .
  5 . . . R - . . . .
  6 . . B R . . . . .
  7 . R R B . . . . .
  8 . . B . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 4 moves: f2 (e2) h4 (b6) f1 (b8) a7 (g5); 1 first move passes: f2.

### 3-4 Crescent

`capture 2 in 4 moves` · computer: steady · 4 dots each · boosters: `double-dot 1, undo 1` · stars: `moves 1 2` · holes: `border`

*A round board, its corners cut off. Close below the pair; when it runs right, one more dot closes round both.*

```
    a b c d e f g h i
  1 - - . . . . . - -
  2 - . R . . . . . -
  3 . . . . R R . . .
  4 . . . R B . . . .
  5 . . . R B R . . .
  6 . . . B . . . . .
  7 . . . . . . . . .
  8 - . . . . . . . -
  9 - - . . . . . - -
```

`check-levels`: passes in 2 moves: e6 (f4) g4; 1 first move passes: e6.

### 3-5 Lantern

`survive 4 moves` · computer: steady · 4 dots each · boosters: `bomb 1, double-dot 1, undo 1` · stars: `boosters 2 3` · holes: `border`

*A lone dot with the computer on three sides: run up, and keep stepping away from the ring.*

```
    a b c d e f g h i
  1 - - . . . . . - -
  2 - . . . . . . . -
  3 . . . . . . . . .
  4 . . . . . . . . .
  5 . . . B R B . . .
  6 . B . . B . . . .
  7 . . . . . . . . .
  8 - . . . . . . . -
  9 - - . . . . . - -
```

`check-levels`: passes in 4 moves: e4 (e3) f4 (d4) f3; 1 first move passes: e4.

### 3-6 Full Moon

`win` · computer: steady · 22 dots each · boosters: `bomb 1, double-dot 1, undo 1` · stars: `margin 2 5` · holes: `border`

*World 3's first whole game: a round board, four crosses, and a bomb and a double dot to use.*

```
    a b c d e f g h i j k l m
  1 - - - . . . . . . . - - -
  2 - - . . . . . . . . . - -
  3 - . . . . . . . . . . . -
  4 . . . R B . . . B R . . .
  5 . . . B R . . . R B . . .
  6 . . . . . . . . . . . . .
  7 . . . . . . . . . . . . .
  8 . . . . . . . . . . . . .
  9 . . . B R . . . R B . . .
 10 . . . R B . . . B R . . .
 11 - . . . . . . . . . . . -
 12 - - . . . . . . . . . - -
 13 - - - . . . . . . . - - -
```

### 3-7 Fireflies

`survive 3 moves` · computer: steady · 3 dots each · boosters: `bomb 1, double-dot 1, undo 1` · stars: `boosters 2 3` · holes: `border`

*Again the lake guards one dot; the other, at d7, needs its last gap filled first.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . R . . . .
  3 . . . . R B B . .
  4 . . . . B R R . .
  5 . . . R - . . . .
  6 . . R B B . . . .
  7 . . B R B . . . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 3 moves: d8 (f5) c5 (g5) h4 (f2); 1 first move passes: d8.

### 3-8 Owl

`target f6` · computer: steady · 5 dots each · boosters: `double-dot 1, undo 1` · stars: `moves 1 2` · holes: `border`

*Target: close above the marked dot, then follow it left and up.*

```
    a b c d e f g h i
  1 - - . . . . . - -
  2 - . . . . . . . -
  3 . . . . . . . . .
  4 . . B R . . . . .
  5 . . R B . . . . .
  6 . . . . . B R R .
  7 . . . . R R B R .
  8 - . . . . . B B -
  9 - - . . . . . - -
```

`check-levels`: passes in 3 moves: f5 (e6) d6 (e5) e4; 1 first move passes: f5.

### 3-9 Crater Rim

`capture 2 in 5 moves` · computer: steady · 5 dots each · boosters: `double-dot 1, undo 1` · stars: `moves 1 2` · holes: `open`

*Hard: an open hole is not border. It takes away the easy point at e5; close d5, then follow the run down the left side.*

```
    a b c d e f g h i j
  1 . . . . . . R . . .
  2 . . . . . . . . . .
  3 B . R R . . . . . .
  4 . R R B R . . . . .
  5 . R B . - - . . . .
  6 R . . R - - . . . .
  7 . . . . . . . . . .
  8 . . . . . . . . . .
  9 . . . . . . . . . .
 10 . . . . . . . . . .
```

`check-levels`: passes in 3 moves: d5 (c6) c7 (b6) b7; 1 first move passes: d5.

### 3-10 Telescope

`capture 1 in 4 moves` · computer: steady · 4 dots each · boosters: `double-dot 1, undo 1` · stars: `moves 1 2` · holes: `border`

*Close the top; the dot runs down into your pocket, and one more dot shuts it.*

```
    a b c d e f g h i
  1 - - . . . . . - -
  2 - . . . . . . . -
  3 . . . . . . . . .
  4 . . . . . . . . .
  5 . B . . . . . . .
  6 . . R B R . . . .
  7 . . R . R . . . .
  8 - . . . . . . . -
  9 - - . . . . . - -
```

`check-levels`: passes in 2 moves: d5 (d7) d8; 1 first move passes: d5.

### 3-11 Shooting Star

`survive 4 moves` · computer: steady · 4 dots each · boosters: `bomb 1, double-dot 1, undo 1` · stars: `boosters 2 3` · holes: `border`

*A lone dot near the top: run for the edge, stepping sideways when the computer blocks.*

```
    a b c d e f g h i
  1 - - . . . . . - -
  2 - . . . . . . . -
  3 . . . B R B . . .
  4 . . . . B . . . .
  5 . . . . . . . . .
  6 . . . . . . . . .
  7 . . . . . . . . .
  8 - . . . . . . . -
  9 - - . . . . . - -
```

`check-levels`: passes in 4 moves: e2 (e1) f2 (d2) f1; 1 first move passes: e2.

### 3-12 Crater

`win with 20 dots` · computer: steady · 24 dots each · boosters: `bomb 1, double-dot 1, undo 1` · stars: `margin 2 4` · holes: `open`

*A whole game round an open crater, with 20 dots against the computer's 24.*

```
    a b c d e f g h i j k l m
  1 . . . . . . . . . . . . .
  2 . . . . . . . . . . . . .
  3 . . R B . . . . . B R . .
  4 . . B R . . B . . R B . .
  5 . . . . . . . . . . . . .
  6 . . . . . - - - . . . . .
  7 . . . . . - - - . . . . .
  8 . . . . . - - - . . . . .
  9 . . . . . . . . . . . . .
 10 . . B R . . B . . R B . .
 11 . . R B . . . . . B R . .
 12 . . . . . . . . . . . . .
 13 . . . . . . . . . . . . .
```

### 3-13 Reflection

`target f3` · computer: steady · 5 dots each · boosters: `double-dot 1, undo 1` · stars: `moves 1 2` · holes: `border`

*Wall: the only first move is a quiet one, three moves before the ring closes; shut the way to the lake first, then answer the runs.*

```
    a b c d e f g h i
  1 . . . B . . . . .
  2 . . . . R R R . .
  3 . . . . . B R . .
  4 . . . . . . R . R
  5 . . . . - . . . .
  6 . . . R . . . . .
  7 . . . . . . . . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 3 moves: e4 (e3) d3 (f4) f5; 1 first move passes: e4.

### 3-14 Pole Star

`capture 1 in 4 moves` · computer: steady · 4 dots each · boosters: `double-dot 1, undo 1` · stars: `moves 1 2` · holes: `border`

*Close one side; when the dot runs, follow it and close the far end.*

```
    a b c d e f g h i
  1 - - . . . . . - -
  2 - B R R . . . . -
  3 . . B . . . . . .
  4 . . R R . R . . .
  5 . . . . . B . . .
  6 . . . B . . . . .
  7 . . . . . . . . .
  8 - . . . . . . . -
  9 - - . . . . . - -
```

`check-levels`: passes in 2 moves: b3 (d3) e3; 1 first move passes: b3.

### 3-15 Black Pool

`survive 4 moves` · computer: steady · 4 dots each · boosters: `bomb 1, double-dot 1, undo 1` · stars: `boosters 2 3` · holes: `open`

*Hard: an open hole is no shelter, since a ring can go round it. Your dot at e8 needs d8 at once, and the attacks keep coming.*

```
    a b c d e f g h i j
  1 . . . . . . . . . .
  2 . . . . . . . . . .
  3 . . . . . . . . . .
  4 . . . . . . . . . .
  5 . . . . - - . . R .
  6 . . . . - - B . . .
  7 . . . B B R . . . .
  8 . . . . R B B . . .
  9 . . . . B . . . . .
 10 . . . . . . . . . .
```

`check-levels`: passes in 4 moves: d8 (c8) d9 (d10) c9 (c10) b9 (g7); 1 first move passes: d8.

### 3-16 Moon Bay

`survive 4 moves` · computer: steady · 4 dots each · boosters: `bomb 1, double-dot 1, undo 1` · stars: `boosters 2 3` · holes: `border`

*Your dot at c6 is almost closed in. d6 joins it to the dot by the lake, and a straight line to the shore can never be cut.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . . . . .
  3 . . . . . . . . .
  4 . . . . . . . . .
  5 . . B . - . . . .
  6 . B R . R . . . .
  7 . . B B . . . . .
  8 . . . . . R . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 4 moves: d6 (e8) f7; 1 first move passes: d6.

### 3-17 Dusk

`target d4` · computer: steady · 4 dots each · boosters: `double-dot 1, undo 1` · stars: `moves 1 2` · holes: `border`

*Close the right side; the dot runs down, and one more dot shuts it in.*

```
    a b c d e f g h i
  1 - - . . . . . - -
  2 - . . . . . . . -
  3 . . B R . . . . .
  4 . . R B . . . . .
  5 . . R . . . . . .
  6 . B B R . . . . .
  7 . . R B B . . . .
  8 - . . R . . . . -
  9 - - . . . . . - -
```

`check-levels`: passes in 2 moves: e4 (d5) e5; 1 first move passes: e4.

### 3-18 Twin Stars

`win with 19 dots` · computer: steady · 24 dots each · boosters: `bomb 1, double-dot 1, undo 1` · stars: `margin 2 4` · holes: `border`

*Hard: two round halves joined in the middle, three computer dots in the neck, and 19 dots against its 24.*

```
    a b c d e f g h i j k l m n o
  1 - - - . . . - - - . . . - - -
  2 - - . . . . - - - . . . . - -
  3 - . . . . . - - - . . . . . -
  4 . . R B . . - - - . . B R . .
  5 . . B R . . . . . . . R B . .
  6 . . . . . . . B . . . . . . .
  7 . . . . . . . B . . . . . . .
  8 . . . . . . . B . . . . . . .
  9 . . . B R . . . . . R B . . .
 10 . . . R B . - - - . B R . . .
 11 - . . . . . - - - . . . . . -
 12 - - . . . . - - - . . . . - -
 13 - - - . . . - - - . . . - - -
```

### 3-19 Stargazer

`capture 1 in 4 moves` · computer: steady · 4 dots each · boosters: `double-dot 1, undo 1` · stars: `moves 1 2` · holes: `border`

*The dot by the lake can never be captured, so go for its neighbour: close one side, then follow.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 B . R R . . . . .
  3 . . . B . . . . .
  4 . . R R B R . . .
  5 . . . . - R . . .
  6 . . . . . . . . .
  7 . . . . . . . . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 2 moves: e3 (c3) b3; 1 first move passes: e3.

### 3-20 Comet

`win with 23 dots` · computer: steady · 26 dots each · boosters: `bomb 1, double-dot 1, undo 1` · rule: double move every 3 turns · stars: `margin 2 5` · holes: `border`

*World 3's boss: every third turn it places two dots, and it has 26 dots to your 23, on a round board.*

```
    a b c d e f g h i j k l m n
  1 - - - . . . . . . . . - - -
  2 - - . . . . . . . . . . - -
  3 - . . . . . . . . . . . . -
  4 . . . R B . . . . B R . . .
  5 . . . B R . . . . R B . . .
  6 . . . . . . . . . . . . . .
  7 . . . . . . B . . . . . . .
  8 . . . . . . . B . . . . . .
  9 . . . . . . . . . . . . . .
 10 . . . B R . . . . R B . . .
 11 . . . R B . . . . B R . . .
 12 - . . . . . . . . . . . . -
 13 - - . . . . . . . . . . - -
 14 - - - . . . . . . . . - - -
```
