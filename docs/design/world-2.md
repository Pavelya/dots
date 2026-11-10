# World 2: Garden

World 2 has 20 levels. Its new ideas are two missions and a booster: **survive** (lose no dot
while the computer places its next few dots), **rescue** (keep one marked dot from being
captured until a straight line of dots joins it to the edge) and the **wall** booster, which
blocks one point for the rest of the game. The level files are in
[`engine/src/main/resources/levels/`](../../engine/src/main/resources/levels/); the format is in
the [README](../../README.md#level-files).

## The shape of the world

| Levels | What the player meets |
| --- | --- |
| 1 and 2 | The two new missions at their simplest: fill the last gap (survive), step to the edge (rescue). |
| 3 to 5 | A longer survive, a short target, a rescue near a corner. |
| 6 | The first whole game of the world, with two walls. |
| 7 to 11 | Survive, capture, rescue and target puzzles, each a little longer. |
| 12 | A longer whole game, one extra computer dot in the middle. |
| 13 to 17 | Puzzles again: a target whose answer is a step away, a stepping stone, the longest survive. |
| 18 | A whole game with fewer dots than the computer. |
| 19 | A short survive. |
| 20 | The Mole, World 2's boss: a double move every fourth turn and two more dots than you. |

The difficulty is a saw-tooth: levels 3, 6, 9, 12, 15 and 18 are the hard ones, and the two
after each are easier. Levels 6, 12 and 18 are whole games and 20 is the boss; levels 3, 9 and
15 are puzzles whose answers take three or four moves.

## How the levels are built

- **Every puzzle has an answer that works against any defence,** and only one or two first
  moves pass it. `cli check-levels` proves it, and CI runs it.
- **Puzzles give two spare moves,** as in World 1: a puzzle whose shortest answer takes 3 moves
  gives 5, and `stars: moves 1 2` asks for both spares for three stars.
- **Survive levels give two boosters,** and their stars count the boosters left
  (`stars: boosters 1 2`): three stars without a booster, one with both used. Every survive level
  can be passed without them; the solver never uses a booster.
- **The computer is the steady step** for every level but the last boss of World 4, so the
  difficulty comes from the boards, the missions and the dot budgets.
- **Every level file has a one-line `note`,** repeated in italics below.
- **Most puzzles started from the computer's own games or from random small positions,** and were
  kept only when the solver proved them and found one or two winning first moves. Their names
  and notes were written by hand.
- **Boosters:** survive levels give two walls; the other puzzles give one. Whole games give two walls.

## How hard it is

`cli bench levels 80 <stand-in>`: a stand-in plays red on each level, and the table shows how many
games passed the level's mission. The EASY step stands for a weak player, the NORMAL step for a
strong one, border-only for someone who only plays on the border. The README explains the bench
([How strong is it, and how hard are the levels?](../../README.md#how-strong-is-it-and-how-hard-are-the-levels)).

```
                                             passed of 80   passed of 80     passed of 40
level                 mission                EASY stand-in  NORMAL stand-in  border-only
2-1   Weeds           survive 2 moves         80             80               0
2-2   Path Home       rescue b4               80             80               4
2-3   Snail           survive 4 moves         10             80               0
2-4   Ladybug         target e4                5              4               0
2-5   Fence Line      rescue c3               72             80               0
2-6   Flower Beds     win                     33             59               0
2-7   Bramble         survive 3 moves          0             80               0
2-8   Seedling        capture 1 in 4 moves    11             11               0
2-9   Side Step       rescue d5               43             80               0
2-10  Rose Bush       target d4                0              0               0
2-11  Watering Can    survive 3 moves          1             80               0
2-12  Hedge           win                     24             52               0
2-13  Greenhouse      target g6                9              6               0
2-14  Stepping Stone  rescue c4               43             80               0
2-15  Thorns          survive 4 moves          8             80               0
2-16  Tulips          target e6               74             33               0
2-17  Garden Gate     rescue d3                0             80               0
2-18  Few Seeds       win with 18 dots        21             28               0
2-19  Trellis         survive 3 moves          0             79               0
2-20  Mole            win with 20 dots        17             27               0
```

- **Whole games get harder through the world** for a weak player: 33, 24, 21 and 17 passes of 80
  for levels 6, 12, 18 and the Mole. The first is a little easier than World 1's mini-boss, 1-15 (29
  of 80), so the world opens with a dip.
- **The puzzles look harder here than they are,** as in World 1: the stand-ins play for captures
  and do not read the mission. That every puzzle can be passed is what `check-levels` proves; how
  hard each feels needs people.
- **Border-only play passes no level of the world but 2-2** (4 of 40), whose answer is itself a
  border point. Before its first dot, the computer looks at the whole board, so a danger already
  on the board is attacked even when the player opens far from it.
- These are stand-ins, not people, and nobody has played these levels yet: treat the numbers as a
  first draft.

## Trying the levels

Every level plays in the terminal game; you are red and move first:

```sh
./gradlew :cli:installDist
cli/build/install/cli/bin/cli play 2-1     # any level, 2-1 to 2-20
cli/build/install/cli/bin/cli check-levels # the puzzle answers
```

## The levels

Boards as in the level files: `R` the player's dots, `B` the computer's, `.` an empty point.
Columns are letters from the left, rows numbers from the top. Puzzle answers show the
computer's replies in brackets, as `check-levels` prints them.

### 2-1 Weeds

`survive 2 moves` · computer: steady · 2 dots each · boosters: `wall 2` · stars: `boosters 1 2`

*Survive: lose no dot. One dot fills the last gap; a wall there works too, which is what the wall booster is for.*

```
    a b c d e f g
  1 . . . . . . .
  2 . . . . . . .
  3 . . . B . . .
  4 . . B R B . .
  5 . . . . . . .
  6 . . . . . . .
  7 . . . . . . .
```

`check-levels`: passes in 2 moves: d5 (c3) c5 (e3); 1 first move passes: d5.

### 2-2 Path Home

`rescue b4` · computer: steady · 3 dots each · boosters: `wall 1` · stars: `moves 1 2`

*Rescue: a dot joined to the edge by a straight line of dots can never be captured.*

```
    a b c d e f g
  1 . . . . . . .
  2 . . . . . . .
  3 . B . . . . .
  4 . R B . . . .
  5 . B . . . . .
  6 . . . . . . .
  7 . . . . . . .
```

`check-levels`: passes in 1 move: a4; 1 first move passes: a4.

### 2-3 Snail

`survive 4 moves` · computer: steady · 4 dots each · boosters: `wall 2` · stars: `boosters 1 2`

*Hard: survive four moves. Close the gap under your lone dot at e6 first, then answer each new attack on the right.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . . . . .
  3 . . . . . . . . .
  4 . . . . B . . . .
  5 . . R R B R . . .
  6 . . . B R B . . .
  7 . . . . . . . . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 4 moves: e7 (g5) f4 (f3) g4 (g3) h4 (e8); 1 first move passes: e7.

### 2-4 Ladybug

`target e4` · computer: steady · 4 dots each · boosters: `wall 1` · stars: `moves 1 2`

*A short target after a hard level: close the corner above the pair, and when it runs up, one more dot closes round both.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . . . . .
  3 . . . R . . . . .
  4 . . B R B B R . .
  5 . . R B R R B . .
  6 . . . B R . . . .
  7 . . . . . . . . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 2 moves: f3 (e3) e2; 1 first move passes: f3.

### 2-5 Fence Line

`rescue c3` · computer: steady · 5 dots each · boosters: `wall 1` · stars: `moves 1 2`

*Rescue near a corner: each time the computer blocks the way to the edge, step along and threaten the next way out.*

```
    a b c d e f g
  1 . . . . . . .
  2 . . B . . . .
  3 . . R B . . .
  4 . . B . . . .
  5 . . . . . . .
  6 . . . . . . .
  7 . . . . . . .
```

`check-levels`: passes in 3 moves: b3 (a3) b2 (a2) b1; 1 first move passes: b3.

### 2-6 Flower Beds

`win` · computer: steady · 18 dots each · boosters: `wall 2` · stars: `margin 2 4`

*World 2's first whole game: four crosses on 11x11, the steady computer, two walls to block its rings.*

```
    a b c d e f g h i j k
  1 . . . . . . . . . . .
  2 . . . . . . . . . . .
  3 . . R B . . . B R . .
  4 . . B R . . . R B . .
  5 . . . . . . . . . . .
  6 . . . . . . . . . . .
  7 . . . . . . . . . . .
  8 . . B R . . . R B . .
  9 . . R B . . . B R . .
 10 . . . . . . . . . . .
 11 . . . . . . . . . . .
```

### 2-7 Bramble

`survive 3 moves` · computer: steady · 3 dots each · boosters: `wall 2` · stars: `boosters 1 2`

*Your dot at e6 is one move from capture: block its last gap, or capture the attacker next to it first.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . . B . .
  3 . . . . . R B R .
  4 . . . . . B R . .
  5 . . . . . R . . .
  6 . . . B R B . . .
  7 . . . . B R . . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 3 moves: e5 (e3) f2 (h4) g5 (e4); 2 first moves pass: e5 g6.

### 2-8 Seedling

`capture 1 in 4 moves` · computer: steady · 4 dots each · boosters: `wall 1` · stars: `moves 1 2`

*Capture review: one dot that threatens two rings at once, and the computer can block only one.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . . . . .
  3 . . . . . R R . .
  4 . . . B B R B . .
  5 . . . . R B . . .
  6 . . . . R . . . .
  7 . . . . . . . . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 2 moves: g5 (h4) f6; 1 first move passes: g5.

### 2-9 Side Step

`rescue d5` · computer: steady · 5 dots each · boosters: `wall 1` · stars: `moves 1 2`

*Hard: the way down is one move too slow, so the dot steps sideways first, next to its friend, and runs from there.*

```
    a b c d e f g
  1 . . . . . . .
  2 . . . . . . .
  3 . . . . . . .
  4 . . . B . . .
  5 . . . R B . .
  6 . . R B . . .
  7 . . . . . . .
```

`check-levels`: passes in 3 moves: c5 (c7) b6 (b7) a6; 1 first move passes: c5.

### 2-10 Rose Bush

`target d4` · computer: steady · 5 dots each · boosters: `wall 1` · stars: `moves 1 2`

*Target: close above the marked dot, then follow its run to the right and up.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . . . . .
  3 . . . . . . . . .
  4 . . R B . B R R .
  5 . . B R R R B R .
  6 . . . . . . B B .
  7 . . . . . . . . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 3 moves: d3 (e4) f3 (e3) e2; 1 first move passes: d3.

### 2-11 Watering Can

`survive 3 moves` · computer: steady · 3 dots each · boosters: `wall 2` · stars: `boosters 1 2`

*Survive: your dot at f3 has three attackers; fill its last gap first, then keep blocking.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . . . . .
  3 . . . . B R B . .
  4 . . . . R B B . .
  5 . . . R R B R R .
  6 . . . B B R B . .
  7 . . . . . R . . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 3 moves: f2 (h4) d4 (h6) i5 (e7); 1 first move passes: f2.

### 2-12 Hedge

`win` · computer: steady · 20 dots each · boosters: `wall 2` · stars: `margin 2 4`

*A longer whole game: four crosses on 13x13 and one extra computer dot in the middle.*

```
    a b c d e f g h i j k l m
  1 . . . . . . . . . . . . .
  2 . . . . . . . . . . . . .
  3 . . R B . . . . B R . . .
  4 . . B R . . . . R B . . .
  5 . . . . . . . . . . . . .
  6 . . . . . . . . . . . . .
  7 . . . . . . B . . . . . .
  8 . . . . . . . . . . . . .
  9 . . B R . . . . R B . . .
 10 . . R B . . . . B R . . .
 11 . . . . . . . . . . . . .
 12 . . . . . . . . . . . . .
 13 . . . . . . . . . . . . .
```

### 2-13 Greenhouse

`target g6` · computer: steady · 5 dots each · boosters: `wall 1` · stars: `moves 1 2`

*Target: the block right next to the marked dot fails; the answer is a point further out that covers both ways it can run.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . . . . .
  3 . . . . . . . . .
  4 . . . . R B B . .
  5 . . . R R B R R .
  6 . . . B B R B . .
  7 . . . . . R . . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 3 moves: h7 (h6) i6 (g7) g8; 1 first move passes: h7.

### 2-14 Stepping Stone

`rescue c4` · computer: steady · 5 dots each · boosters: `wall 1` · stars: `moves 1 2`

*Rescue with a stepping stone: run towards the dot you already have near the edge.*

```
    a b c d e f g
  1 . . . . . . .
  2 . . . . . . .
  3 . . B . . . .
  4 . B R B . . .
  5 . R . . . . .
  6 . . . . . . .
  7 . . . . . . .
```

`check-levels`: passes in 3 moves: c5 (a5) b6 (a6) b7; 1 first move passes: c5.

### 2-15 Thorns

`survive 4 moves` · computer: steady · 4 dots each · boosters: `wall 2` · stars: `boosters 1 2`

*Hard: survive four moves. Close the gap above e5 first; the attacks that follow come from both sides.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . . . . .
  3 . . . . . . . . .
  4 . . . . . . . . .
  5 . . . B R B . . .
  6 . . . R B R . . .
  7 . . . R B . . . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

`check-levels`: passes in 4 moves: e4 (f7) g6 (d8) c7; 1 first move passes: e4.

### 2-16 Tulips

`target e6` · computer: steady · 5 dots each · boosters: `wall 1` · stars: `moves 1 2`

*Target: close above the marked dot, then follow it as it runs to the right and down.*

```
    a b c d e f g h i j
  1 . . . . . . . . . .
  2 . . . . . . . . . .
  3 . . . . . . B R . .
  4 . . . . . . R B B .
  5 . . . . . . B R R .
  6 . . . R B . B R . .
  7 . . . B R . R B . .
  8 . . . . . . . . . .
  9 . . . . . . . . . .
 10 . . . . . . . . . .
```

`check-levels`: passes in 3 moves: e5 (f6) f4 (f7) f8; 1 first move passes: e5.

### 2-17 Garden Gate

`rescue d3` · computer: steady · 5 dots each · boosters: `wall 1` · stars: `moves 1 2`

*Rescue: the top edge is closer than it looks, because your dot at f1 is already on it.*

```
    a b c d e f g
  1 . . . . . R .
  2 . . . . . . .
  3 . . B R B . .
  4 . . . B . . .
  5 . . . . . . .
  6 . . . . . . .
  7 . . . . . . .
```

`check-levels`: passes in 3 moves: d2 (d1) e2 (e1) f2; 1 first move passes: d2.

### 2-18 Few Seeds

`win with 18 dots` · computer: steady · 20 dots each · boosters: `wall 2` · stars: `margin 2 4`

*Hard: a whole game with 18 dots against the computer's 20, so every dot must count.*

```
    a b c d e f g h i j k l m
  1 . . . . . . . . . . . . .
  2 . . . . . . . . . . . . .
  3 . . . R B . . . . . . . .
  4 . . . B R . . . B R . . .
  5 . . . . . . . . R B . . .
  6 . . . . . . . . . . . . .
  7 . . . . . . . . . . . . .
  8 . . . . . . . . . . . . .
  9 . . B R . . . R B . . . .
 10 . . R B . . . B R . . . .
 11 . . . . . . . . . . . . .
 12 . . . . . . . . . . . . .
 13 . . . . . . . . . . . . .
```

### 2-19 Trellis

`survive 3 moves` · computer: steady · 3 dots each · boosters: `wall 2` · stars: `boosters 1 2`

*Survive after a hard level: the gap under d7 comes first, then answer the attacks at the top and on the right.*

```
    a b c d e f g h i j
  1 . . . . . . . . . .
  2 . . . . . . . . . .
  3 . . R B . . . . . .
  4 . B B R . . . . . .
  5 . . R R R R B . . .
  6 . . R B B B R . . .
  7 . . B R B . . . . .
  8 . . . . . . . . . .
  9 . . . . . . . . . .
 10 . . . . . . . . . .
```

`check-levels`: passes in 3 moves: d8 (b3) c2 (h6) g7 (f7); 1 first move passes: d8.

### 2-20 Mole

`win with 20 dots` · computer: steady · 22 dots each · boosters: `wall 2` · rule: double move every 4 turns · stars: `margin 2 5`

*World 2's boss: every fourth turn it places two dots, and it has 22 dots to your 20.*

```
    a b c d e f g h i j k l m n
  1 . . . . . . . . . . . . . .
  2 . . . . . . . . . . . . . .
  3 . . . . . . . . . B R . . .
  4 . . R B . . . . . R B . . .
  5 . . B R . . . . . . . . . .
  6 . . . . . . . . . . . . . .
  7 . . . . . . B . . . . . . .
  8 . . . . . . . B . . . . . .
  9 . . . . . . . . . . . . . .
 10 . . . B R . . . . . R B . .
 11 . . . R B . . . . . B R . .
 12 . . . . . . . . . . . . . .
 13 . . . . . . . . . . . . . .
 14 . . . . . . . . . . . . . .
```
