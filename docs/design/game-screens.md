# Game screens: the brief

Pinfold is a game, so its screens are designed around the board and the map, not around menus.
This page gives the principles the wireframes (D-06) follow and lists the frames they draw. Each
point names the research it comes from: the [references](references.md) (B, P, M, U, F), the
[design investigation](investigation.md) and the [Notebook mood board](moodboards/notebook.md)
(pictures 1 to 7).

## Principles

1. **The board and the map are the stage.** Cards, Pause and results are notes laid over them, never
   pages that replace them. The player always sees where they are. (U01, M04, M05, M06; mood
   board 6)
2. **The map is home.** The game opens on the world map, with the next level near the thumb. There
   is no tab bar. Everything else is one small icon away. (P01, P06; mood boards 4 and 7)
3. **Two sides face each other.** The opponent sits above the board with a face; the player sits
   below it, near the thumb. Scores are ink tallies. Whose turn it is shows on the side, not in a
   status line. (B01; mood board 1; investigation section 3: characters live around the board)
4. **Design the moments.** The game screen is drawn in each moment that matters: the level
   opening, a threat, the three steps of a capture, a boss turn, a booster, the end. A capture is
   the reward the whole game is built on. (M01, M03, M07, M08, M10; investigation section 3)
5. **Few controls, one clear action.** One slim bar of hand-drawn icons under the board, with a
   count on each booster. Each screen has one main action; the rest are small icons. (B04, M09;
   mood board 1)
6. **Show progress happening.** Stars fly to the map, a new world opens, a chest arrives every 10
   levels, the player's title grows. These are the reasons to come back. (P06, P07, U04;
   investigation section 4)
7. **Teach on the board.** The tutorial is a real level with a few dots placed and one sentence at
   a time. (B10, B11)
8. **Plain where plain is right.** Settings, the shop list and the developer menu stay short and
   quiet, on the same paper. (U02)

The layout rules from D-06 still hold: touch targets at least 48 dp, the largest board that fits
(24 dp spacing on a 360 dp phone), and main actions below the top third of the screen.

## Fidelity

The wireframes are sketches in the Notebook look: squared paper, red and blue ink, hand-drawn
icons, Caveat for headings. Faces, the map's landscape and world pictures are simple doodles that
mark where art goes; the art itself comes later (D-07 and phase 5).

## The frames

### The map and the way into a level

| Frame | What it shows | From |
| --- | --- | --- |
| Map (home) | The world's path through a doodled landscape. Finished stops with stars, the next stop circled twice with Play beside it, near the thumb. The boss fortress, a chest every 10 levels. Today's puzzle as a sticky note. Small icons: play a game, How to play, Stats, Shop, Settings. The player's title and stars. | P01, P06, P07, P03; mood boards 4 and 7 |
| Map: a world opens | After the last star needed: the next world's page turns and the path continues. | P02, P06 |
| Level card | A page torn from the notebook, over the map: the mission in one line, the board picture, the opponent's face and head start, the boosters allowed, the star targets. Start. | U01, P04 |
| Boss card | The level card with the boss's name, face and rule as a banner. | U01, M07 |
| Chest | Every 10 levels: pick one of two rewards. | U04 |
| Play a game | A small sheet from the map: against the computer, or with a friend on one phone. One tap starts with the defaults. | U05, U07, U09 |
| Today's puzzle | The puzzle's note: date, mission, streak, time to the next one. Play. | P03, M11 |

### The play

| Frame | What it shows | From |
| --- | --- | --- |
| Game board: the parts | Opponent above the board (face, tally, boss counter), the board edge to edge, the player below (tally, the tool bar). The mission as a note in a corner. | B01, M08, M09; mood board 1 |
| Level opening | The mission note over the board, target dots marked on the board, the boss rule if any. The first tap starts. | B10, M07 |
| Threat | Highlighter rings around a group in danger, the closing point; the opponent's face reacts. | Investigation section 3; D-03 |
| Placing | The preview ring, then the dot with its last-move pinhole. | B01, B09; D-04 |
| Capture 1: the ring closes | The pen stroke runs around the area. | M01; mood board 2 |
| Capture 2: ink spreads | The wash soaks in, captured dots get their cut. | M01, M03; mood board 2 |
| Capture 3: the score | +N flies to the tally, the mission note ticks. | M01, M02 |
| Big capture | A stronger version: a short shake, the opponent's face falls. | Investigation section 3 |
| Boss turn | The counter reaches zero; the board shows where the boss will place two dots. | M07, M08 |
| Booster aimed | One line on what it does, the points it can be used on, Confirm. | M10 |
| Last moves | Few dots left: the tally and the mission note get the player's eye. | M08 |
| Win | The captured areas light up in turn and the score counts up; then a taped note with stars, Next and Retry over the final board. | M04, M05, M06; mood board 6 |
| Loss | "Missed by one capture" on the note; Retry is the big action. After a third try, a hint or booster. | M06; investigation section 4 |
| Tutorial level | A few dots placed, one sentence, a hand pointing at the point to tap. | B11 |
| Two players | Both names in their inks; a turn banner at each change. | U09 |
| Pause | A note over the board: Resume, Rules, Settings, Restart, Save and leave. | U04 |

### Around the game

| Frame | What it shows | From |
| --- | --- | --- |
| Daily result | Moves against the best possible, streak, time to the next puzzle, Share. | U06, M11, M12 |
| Stats | The player's title, totals, stars per world; leaderboards and achievements. | P07 |
| Shop | Themes, piece styles, ring effects, each on a small board. | U03 |
| Settings | Sound, vibration, high-contrast pieces, threat highlights, help, privacy. | U02 |
| After launch | Online lobby, invite, online game, ranked, weekly challenge: drawn the same way, on paper. | — |

## Navigation

The screens and flows page ([`flows.md`](flows.md), updated in D-13) follows this brief: the map is
home, there is no tab bar, and cards, Pause and results sit over the map or the board.
