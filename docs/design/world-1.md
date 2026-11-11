# World 1: Notebook

World 1 has 15 levels that teach Pinfold to someone who has never played it. Each level teaches
one thing, and each builds on the one before. The level files are in
[`engine/src/main/resources/levels/`](../../engine/src/main/resources/levels/); the format is in
the [README](../../README.md#level-files).

All 15 boards, drawn in the Notebook look with the puzzle answers marked, are on the
[World 1 level sheet](https://claude.ai/artifact/MNrs71yoNgFasLXGyriTFH). It opens only for
people the owner has shared it with.

## The shape of the world

| Levels | Part | Computer | What the player learns |
| --- | --- | --- | --- |
| 1 to 3 | Tutorial | beginner | Placing a dot, chains (a dot can join two rings), the first ring of their own. |
| 4 to 10 | Puzzles | easy | Capture missions: shut a gap, keep the computer off the edge, capture a marked dot, chase, cut a diagonal link, close the far end first. |
| 11 to 14 | Whole games | easy, then steady | A real game against the computer: one cross, two crosses, a head start, four crosses. |
| 15 | Mini-boss | steady | The Ink Bottle: a head start of two blots and a double move every fifth turn. |

Levels 1 to 3 are the levels the tutorial runs with guided steps (A-06). They work without the
guidance too: their computer, the beginner, does not see threats, so it does not hurry to
block a ring.

## The tutorial's steps

Each tutorial level teaches one lesson: 1-1 placing, 1-2 linking, 1-3 capturing. On the
player's turn, a note above the board says what to do in one sentence, and the board circles the
point to tap, with a hand pointing at it. Threat highlights are on in these levels. The steps are
in `dots.campaign.Tutorial`, worked out from the position, so they follow whatever the computer
does:

1. A dot shown but not placed yet: place it (tap it again, or lift the finger).
2. Level 1: place the dot that closes the ring.
3. A ring that closes with one dot: close it.
4. A ring the computer closes next turn around the player's dots: block it.
5. Level 2: the dot that leaves two rings one dot from closing, so the computer can block only one.
6. Otherwise: start a ring, at the dot after which a second one captures.

Level 1 also says that drag can be chosen in Settings, when the player places dots by two taps.
Skip hides the steps in the tutorial's levels until the tutorial is replayed. Play the tutorial
(How to play) and Replay the tutorial (Settings) play levels 1 to 3 as practice: Next goes from
one to the next, and nothing is saved to the campaign.

Following the steps passes levels 1 and 2 in every game against their own computer, and level 3
in 497 of 500 (the engine's `TutorialTest` plays 20); in the other three the computer blocks
every ring by chance, and Retry plays it again.

## How the levels are built

- **Every puzzle has an answer that works against any defence.** `cli check-levels` proves it
  (see [Checking puzzles](../../README.md#checking-puzzles)), and CI runs it. Puzzles 4 to 10
  have one to three first moves that work (`solutions: few`). Levels 1 and 2 accept more
  (`solutions: open`), because they teach.
- **Puzzles give two spare moves.** A puzzle whose shortest answer takes 3 moves gives 5. The
  stars count the moves left (`stars: moves 1 2`): three stars for the shortest answer, two for
  one move more, one for two more. In a target mission the player's dots are the moves.
- **The computer never gets weaker.** Beginner for levels 1 to 3, easy for 4 to 13, steady for
  14 and 15. The easy computer in the puzzles is weaker than the perfect defender that
  `check-levels` plays, so a puzzle is easier in the game than on paper.
- **Whole games grow.** From 9x9 with 10 dots each to 12x12 with 18. Their stars count the
  margin of victory (`stars: margin 2 4`; `margin 2 5` for the mini-boss).
- **The mini-boss's rule is a whole-game rule.** `check-levels` does not play double moves, and
  a `win` level is not a puzzle anyway.
- **Every level starts with the player's dots in the middle,** so playing only on the border
  passes nothing.
- **Every level file has a one-line `note`** saying what it teaches. It is repeated in italics
  below.

## How hard it is

The [README](../../README.md#how-strong-is-it-and-how-hard-are-the-levels) has the numbers from
`cli bench levels` and `cli bench stars`. In short: a weak computer player passes the whole games
11 to 15 in 28, 17, 18, 16 and 14 of 40 games, so they get harder step by step (17 and 18 are
equal within the noise) and the mini-boss is the hardest. Computer players do not read missions, so the puzzles need people to judge how
hard they feel.

## Trying the levels

Every level plays in the terminal game; you are red and move first:

```sh
./gradlew :cli:installDist
cli/build/install/cli/bin/cli play 1-1     # any level, 1-1 to 1-15
cli/build/install/cli/bin/cli check-levels # the puzzle answers
```

A newcomer check: someone who has never played starts at 1-1 and plays 1 to 5 with no help
other than the mission line. Note where they get stuck and whether they can say why a ring
captured.

## The levels

Boards as in the level files: `R` the player's dots, `B` the computer's, `.` an empty point.
Columns are letters from the left, rows numbers from the top. Puzzle answers show the
computer's replies in brackets, as `check-levels` prints them.

### 1-1 First Dot

`capture 1 in 3 moves` · computer: beginner · 3 dots each · stars: `moves 1 2`

*Placing: one tap closes a ring that is almost done, so the very first move captures.*

```
    a b c d e f g
  1 . . . . . . .
  2 . . . . . . .
  3 . . . R . . .
  4 . . R B R . .
  5 . . . . . . .
  6 . . . . . . .
  7 . . . . . . .
```

Blue d4 has red on three sides. **Answer:** d5 closes the diamond. A tap anywhere else gives the computer a turn to block d5; if it does not, d5 still captures on the second move, for two stars.

`check-levels`: passes in 1 move: d5; 1 first move passes: d5.

### 1-2 Two Rings

`capture 1 in 4 moves` · computer: beginner · 4 dots each · stars: `moves 1 2`

*Chains: the dot between two open rings joins both, so one of them closes whatever the computer blocks.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . . . . .
  3 . . . R . R . . .
  4 . . R B . B R . .
  5 . . . . . . . . .
  6 . . . . . . . . .
  7 . . . . . . . . .
```

Two blue dots, each with red on two sides. **Answer:** e4 is next to both; blue can block only one of d5 and f5, and red closes the other. d5 or f5 first also passes with the spare moves, a little slower.

`check-levels`: passes in 2 moves: e4 (d5) f5; 3 first moves pass: e4 d5 f5.

### 1-3 First Capture

`capture 1 in 8 moves` · computer: beginner · 8 dots each · stars: `moves 4 6`

*The first ring of your own, from a plain cross, against a computer that does not see threats.*

```
    a b c d e f g h
  1 . . . . . . . .
  2 . . . . . . . .
  3 . . . . . . . .
  4 . . . R B . . .
  5 . . . B R . . .
  6 . . . . . . . .
  7 . . . . . . . .
  8 . . . . . . . .
```

A plain cross on 8x8. The beginner does not see threats, so two dots around one blue dot of the cross (for example e3 and f4 around e4) are usually enough. Eight moves leave room to try again.

### 1-4 Door

`capture 2 in 4 moves` · computer: easy · 4 dots each · stars: `moves 1 2`

*Shut the door: close the gap at the top; if the computer slips out sideways, the ring only grows.*

```
    a b c d e f g
  1 . . . . . . .
  2 . . . . . . .
  3 . . R . B R .
  4 . . . R B R .
  5 . . . . R . .
  6 . . . . . . .
  7 . . . . . . .
```

Blue e3 and e4 sit in a red pocket with gaps at e2 and d3. **Answer:** e2. If blue runs to d3, d2 closes a bigger ring round all three; if it does not, d3 takes the two.

`check-levels`: passes in 2 moves: e2 (d3) d2; 1 first move passes: e2.

### 1-5 Edge

`capture 2 in 5 moves` · computer: easy · 5 dots each · stars: `moves 1 2`

*A dot on the border can never be captured, so take the edge point before the computer reaches it.*

```
    a b c d e f g
  1 . . . . . . .
  2 . . . . . . .
  3 . . . . R . .
  4 . . R . . . .
  5 . . R B R . .
  6 . . R B R . .
  7 . . . . . . .
```

Blue d5 and d6 are one step from the bottom edge. A dot on the border can never be captured, so if blue reaches d7 the level is lost. **Answer:** d7, then follow the run: blue d4, red d3; blue e4, red f4 closes round four dots.

`check-levels`: passes in 3 moves: d7 (d4) d3 (e4) f4; 1 first move passes: d7.

### 1-6 Marked Dot

`target d4` · computer: easy · 4 dots each · stars: `moves 1 2`

*The first target: capture one marked dot, which means ringing the whole group it belongs to.*

```
    a b c d e f g h
  1 . . . . . . . .
  2 . . R . . . . .
  3 . R B B R . . .
  4 . . R B R . . .
  5 . . . . . R . .
  6 . . . R R . . .
  7 . . . . . . . .
  8 . . . . . . . .
```

The marked dot d4 belongs to a group of three (c3, d3, d4). **Answer:** d2 closes the top; when blue runs to d5, c5 closes the ring round the whole group.

`check-levels`: passes in 2 moves: d2 (d5) c5; 1 first move passes: d2.

### 1-7 Chase

`capture 2 in 5 moves` · computer: easy · 5 dots each · stars: `moves 1 2`

*Chase: close the bottom, let the computer run up into your wall, then close behind it.*

```
    a b c d e f g h
  1 . . . . . . . .
  2 . . . . . . . .
  3 . . . . R R . .
  4 . . . . . . . .
  5 . . . R B B R .
  6 . . . . R . . .
  7 . . . . . . . .
  8 . . . . . . . .
```

Blue e5 and f5, with a red wall above. **Answer:** f6 closes the bottom. Blue runs up to f4 (red g4) and e4 (red d4), into the wall, and the ring takes four dots.

`check-levels`: passes in 3 moves: f6 (f4) g4 (e4) d4; 1 first move passes: f6.

### 1-8 Cut

`target d5` · computer: easy · 5 dots each · stars: `moves 1 2`

*Cut the diagonal: a dot across the link separates the target from its group, then the ring closes.*

```
    a b c d e f g h
  1 . . . . . . . .
  2 . . . . . . . .
  3 . . . . R . . .
  4 . B B . . . . .
  5 . . R B . . . .
  6 . . . R R . . .
  7 . . . . . . . .
  8 . . . . . . . .
```

The marked dot d5 hangs on a diagonal link to c4, and that group can reach the left edge. **Answer:** d4. Red c5 and d4 cross the blue link c4-d5, so d5 is cut off. Then blue e5, red f5; blue e4, red f4.

`check-levels`: passes in 3 moves: d4 (e5) f5 (e4) f4; 1 first move passes: d4.

### 1-9 Three in a Row

`capture 3 in 5 moves` · computer: easy · 5 dots each · stars: `moves 1 2`

*A wider net: close the far end first, then answer each run until the ring takes all of them.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . . . . .
  3 . . . . R R . . .
  4 . . . . . . R . .
  5 . . . R B B B . .
  6 . . . . . R R . .
  7 . . . . R . . . .
  8 . . . . . . . . .
```

Three blue dots in a row inside a loose net. **Answer:** h5 closes the far end. Blue runs to e4 (red d4) and to e6 (red d6), and the ring takes all of them.

`check-levels`: passes in 3 moves: h5 (e4) d4 (e6) d6; 1 first move passes: h5.

### 1-10 Long Chase

`target f4` · computer: easy · 6 dots each · stars: `moves 1 2`

*The hardest puzzle of the world: four moves, each one answering where the computer runs.*

```
    a b c d e f g h
  1 . . . . . . . .
  2 . . . . R R . .
  3 . . . R . B . .
  4 . . . R B B R .
  5 . . . . . . R .
  6 . . . R . . . .
  7 . . . . . . . .
  8 . . . . . . . .
```

The marked dot f4 in a group of three, with an open side to the right and below. **Answers:** g2, g3 or h3. In the shortest line: g3, then blue e5, red e6; blue f5, red f6; blue d5, red c5. Four moves, each one answering the last run.

`check-levels`: passes in 4 moves: g3 (e5) e6 (f5) f6 (d5) c5; 3 first moves pass: g2 g3 h3.

### 1-11 First Game

`win` · computer: easy · 10 dots each · stars: `margin 2 4`

*The first whole game: a small board, one cross, a computer that makes mistakes.*

```
    a b c d e f g h i
  1 . . . . . . . . .
  2 . . . . . . . . .
  3 . . . . . . . . .
  4 . . . R B . . . .
  5 . . . B R . . . .
  6 . . . . . . . . .
  7 . . . . . . . . .
  8 . . . . . . . . .
  9 . . . . . . . . .
```

One cross on 9x9, 10 dots each, against the easy computer. Nothing is set up: the player plays a whole game for the first time.

### 1-12 Two Crosses

`win` · computer: easy · 12 dots each · stars: `margin 2 4`

*Two crosses: two places to fight, so the player learns to look at the whole board.*

```
    a b c d e f g h i j
  1 . . . . . . . . . .
  2 . . . . . . . . . .
  3 . . R B . . . . . .
  4 . . B R . . . . . .
  5 . . . . . . . . . .
  6 . . . . . . . . . .
  7 . . . . . . R B . .
  8 . . . . . . B R . .
  9 . . . . . . . . . .
 10 . . . . . . . . . .
```

Two crosses on 10x10, 12 dots each. A fight in one corner can be lost while the player looks at the other.

### 1-13 Head Start

`win` · computer: easy · 12 dots each · stars: `margin 2 4`

*The first head start: two extra computer dots stretch its diagonal through the cross to four dots.*

```
    a b c d e f g h i j
  1 . . . . . . . . . .
  2 . . . . . . . . . .
  3 . . . . . . . . . .
  4 . . . . . . B . . .
  5 . . . . R B . . . .
  6 . . . . B R . . . .
  7 . . . B . . . . . .
  8 . . . . . . . . . .
  9 . . . . . . . . . .
 10 . . . . . . . . . .
```

One cross on 10x10, plus blue g4 and d7, which join the cross's blue dots into a diagonal of four. The head start is shown on the level card.

### 1-14 Four Crosses

`win` · computer: steady · 16 dots each · stars: `margin 2 4`

*Four places to fight, and the first computer that checks the answer to its own moves.*

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

Four crosses on 11x11, 16 dots each, against the steady computer: it thinks about the answer to its own moves and makes fewer mistakes.

### 1-15 Ink Bottle

`win` · computer: steady · 18 dots each · stars: `margin 2 5` · rule: double move every 5 turns

*The mini-boss: two ink blots to start, and every fifth turn it places two dots, with a countdown.*

```
    a b c d e f g h i j k l
  1 . . . . . . . . . . . .
  2 . . . . . . . . . . . .
  3 . . R B . . . . B R . .
  4 . . B R . . . . R B . .
  5 . . . . . . . . . . . .
  6 . . . . . B . . . . . .
  7 . . . . . . B . . . . .
  8 . . . . . . . . . . . .
  9 . . B R . . . . R B . .
 10 . . R B . . . . B R . .
 11 . . . . . . . . . . . .
 12 . . . . . . . . . . . .
```

Four crosses on 12x12, two blue blots in the middle, 18 dots each. Every fifth computer turn it places two dots (turns 5, 10 and 15); the countdown shows when.
