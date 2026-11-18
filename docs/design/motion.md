# Motion

This page describes every animation in Pinfold before it is built: what moves, in which order,
for how long, and what the player can do meanwhile. The animations are built in V-05 (the board
and the screens), V-06 (the booster effects), A-14 (the computer's last move) and V-13 (the board
themes). They follow the times on this page.

Every time is in milliseconds (ms), counted from the moment's start, which each section names.
Colours and sizes are those of the [Notebook theme](theme-notebook.md) and
[Piece shapes](pieces.md). The [canvas](canvas/README.md) shows the still frames these
animations start and end on. The board themes move at the same times; their sections,
[Star chart](#star-chart) and [Neon arcade](#neon-arcade), list only what they change.

Each moment has a strip of key frames and a GIF made from the canvas mock-ups. The GIFs have a dark
band above the frame that names the step and shows the time since it started. The band and the
grey thumb are part of the mock-up, not the game. Each GIF also exists in the dark version.

## Rules for all motion

1. **The board never waits.** It takes every tap at once. A tap during an animation finishes that
   animation (everything jumps to its end state), and the tap still counts. No animation holds
   the next tap back.
2. **The computer waits for the player.** The computer's dot lands at least 500 ms after the
   player's dot, and not before the player's capture has finished. The player sees their own move
   settle first, and has time to lift their finger.
3. **The board stays solid.** On the board things grow, shrink, are drawn like a pen stroke, or
   blend from one theme colour to another. Nothing on the board turns see-through, as on the theme
   page. Notes and the dimmed background over the map may fade.
4. **Movement stops on its own.** Nothing moves for longer than 5 seconds. The threat pulse stops
   after three beats. This follows the WCAG rule that moving content must stop or be stoppable.
5. **With animations removed.** When the phone's **Remove animations** setting is on (Android
   Settings, Accessibility), every moment shows its end state at once. The information stays: the
   marks on the computer's move, the threat rings, the counter's colour, "Last dot!", the stars. Only
   the ripples, pulses, shakes, bursts and flights go.
6. **Touch.** A light vibration when a dot lands, a medium one when a ring closes, a strong one for
   a big capture and a bomb. The vibration setting turns them off. Sounds, when they come, start
   at the same moments.

**Easing.** How a movement speeds up and slows down:

| Name on this page | What it does | In Compose |
| --- | --- | --- |
| ease out | starts fast, settles gently; things arriving | `LinearOutSlowInEasing` |
| ease in | starts gently, leaves fast; things leaving | `FastOutLinearInEasing` |
| ease in-out | gentle at both ends; things moving across | `FastOutSlowInEasing` |
| linear | the same speed throughout; handwriting, counting | `LinearEasing` |

Times are exact, so the build uses timed animations (`tween` and `keyframes`), not springs.

## Timing table

"Next tap waits" is how long the player has to wait before a tap counts. "A tap" says what a tap
does while the animation runs.

| Moment | Starts when | Length | Next tap waits | A tap | With animations removed |
| --- | --- | --- | --- | --- | --- |
| [Drag's guides](#placing-by-drag) | the finger goes down, placing by drag | 100 ms, then they follow the finger at once | 0 | – (the finger is down) | the guides at once |
| [Placing a dot](#placing-a-dot-and-chain-lines) | a dot is confirmed | 200 ms | 0 | counts as usual | the dot at once |
| [Chain lines](#placing-a-dot-and-chain-lines) | with placing | 80 to 230 ms | 0 | counts as usual | the links at once |
| [The computer's move](#the-computers-move) | 500 ms or more after the player's dot | 900 ms; the edge marks stay | 0 | counts as usual | dot and edge marks at once, no ripple |
| [Capture](#capture) | the closing dot lands | 1250 ms | 0 | finishes it | the captured area and score at once |
| [Big capture](#big-capture), 5 dots or more | the closing dot lands | 1400 ms | 0 | finishes it | as a capture, no shake or burst |
| [Threat pulse](#threat-pulse) | 200 ms after the dot that makes the threat | 5050 ms, then still | 0 | counts as usual | still rings at once |
| [Dots running out](#dots-running-out) | 5 dots left | 300 ms per dot | 0 | counts as usual | colour, row and "Last dot!" at once |
| [Wall](#boosters) | Confirm | 400 ms | 0 | counts as usual | the wall at once |
| [Hint](#boosters) | Confirm | 500 ms | 0 | counts as usual | the circles at once |
| [Bomb](#boosters) | Confirm | 450 ms | 0 | finishes it | the dots gone at once |
| [Double dot](#boosters) | Confirm | 300 ms, then 250 ms per dot | 0 | counts as usual | the chip at once |
| [Undo](#boosters) | Confirm | 600 ms | 0 | finishes it | the turn gone at once |
| [End of game](#end-of-game) | the game is over and the last move has played out | 1400 ms to the result note | the game is over | jumps to the note | the note at once |
| [Star reveal](#star-reveal) | the result note has settled | 1100 ms | 0: the buttons work at once | finishes it | the stars at once |
| [Level unlock](#level-unlock) | the map shows after Next | 2100 ms | 0 | jumps to the Level card | the new state and the card at once |
| [Level opening](#level-opening-notes-and-cards) | the board shows | 1500 ms | 0 | finishes it and counts | the corner note at once |
| [Notes and cards](#level-opening-notes-and-cards) | a note is opened or closed | 300 ms in, 200 ms out | 0: the buttons work at once | counts as usual | the note at once |

**The same times in every theme.** Star chart and Neon arcade change how a step looks, never when
it starts or how long the moment lasts. So the sounds and vibrations stay in step in every theme
([Sound](sound.md), rule 3): a capture's ring closes at 550 ms and a big capture's at 650 ms,
whatever the board looks like.

## Placing by drag

A player who places dots by drag (the default, decision 0026) presses the board, slides to the
point and lifts. While the finger is down, the preview sits on the point under it, and the
guides run from the preview's ring along its row and column to the board's edges (theme page,
[Drag's guides](theme-notebook.md#drags-guides)). The finger hides the point, so the guides show
around it. 0 ms is the moment the finger goes down.

<img src="motion/strip-drag.png" width="720" alt="Key frames: a thumb goes down and dashed red guides grow out from under it; the thumb slides off the board and the guides are gone; it goes down again, passes a taken point with no guides, stops on a free point with guides, lifts, and the red dot lands">

<img src="motion/drag-light.gif" width="360" alt="Animation: the thumb slides off the bottom of the board and lifts, placing nothing; then it slides up past a taken point to a free one and lifts, and the dot lands">

[Same in the dark version](motion/drag-dark.gif). The thumb is drawn a little see-through, so the
preview under it shows.

| Time | What moves | Easing |
| --- | --- | --- |
| 0 | The preview appears on the point under the finger: the pale piece in its ring, as with two taps. | |
| 0 to 100 | The four guides are drawn out from the ring to the edges, in the ink of the side to move. | ease out |
| The finger moves onto another point | The preview and the guides jump to it at once. They never glide: a guide that trailed the finger would point at the wrong row. | |
| The finger is over a taken point, or off the board | The preview and the guides go at once. Back on a free point, they come back at once, without being drawn out again. | |
| The finger lifts on a free point | The guides and the preview ring go at once. This is 0 ms of [Placing a dot](#placing-a-dot-and-chain-lines). | |
| The finger lifts anywhere else | The preview and the guides go at once. Nothing is placed: no vibration, no sound, and the dots-left count stays. | |

- **Nothing vibrates while the finger moves.** The light vibration comes when the dot lands, as for
  two taps.
- **With animations removed,** the guides appear at full length at once. Everything else here
  already happens at once.
- During the computer's turn a finger on the board shows no preview and no guides.

## Placing a dot and chain lines

0 ms is the moment the dot is placed: the second tap, or the finger lifting when placing by drag
(decision 0024). Links grow only where the board shows them: when "Links between dots" is on in
Settings, and in the tutorial's linking level ([theme page](theme-notebook.md#the-board)).

<img src="motion/strip-placing.png" width="720" alt="Key frames: the preview, then the dot growing past its size, settling, its link growing up to its neighbour, and the pinhole appearing">

| Time | What moves | Easing |
| --- | --- | --- |
| 0 | The preview ring goes. The dot starts to grow from nothing. The dots-left count goes down by one. Light vibration. | |
| 0 to 120 | The dot grows to 115% of its size. | ease out |
| 120 to 200 | It settles to 100%. | ease in-out |
| 0 to 200 | The pen stroke under the name moves to the side to move: it is drawn left to right under the new name. | ease out |
| 80 to 230 | A link grows from each neighbouring dot of the same side to the new dot. Several links grow together. | ease out |
| 200 | The last-move pinhole moves to the new dot. | |

## The computer's move

The P-10 testers often missed the computer's dot, because their finger hid it. The dot therefore
announces itself around the finger and away from it:

- **Two ripples** in the computer's ink grow to 96 dp across. A fingertip covers about 10 mm, about
  60 dp, so the ripple shows around it.
- **Edge marks**: a short stroke on the top edge of the board above the dot's column, and one on the
  left edge beside its row. A hand coming from below rarely covers the top edge. The marks stay
  until the player's next dot lands, so a player who looked away can still find the move.

0 ms is the moment the computer's dot lands. A-14 settles the still mark for the last move; this is
its motion.

<img src="motion/strip-computer-move.png" width="720" alt="Key frames: a thumb rests on the board; the blue dot lands next to it and two blue rings spread out from it, wider than the thumb">

<img src="motion/turn-light.gif" width="360" alt="Animation: the player places a dot with the thumb, which stays on the board; the computer's dot lands beside it with two spreading rings and marks on the top and left edges">

[Same in the dark version](motion/turn-dark.gif)

| Time | What moves | Easing |
| --- | --- | --- |
| 0 to 200 | The dot lands, as in placing. | as placing |
| 80 to 230 | Its links grow. | ease out |
| 0 to 700 | First ripple: a ring in the computer's ink grows from 22 dp to 96 dp across. Its line thins from 3 dp to 0.5 dp, and then it is gone. | ease out |
| 200 to 900 | Second ripple, the same. | ease out |
| 0 to 150 | The edge marks grow in from the edges: 8 dp long, 2.5 dp thick, in the computer's ink, in the half spacing of paper outside the grid. | ease out |
| 200 | The pinhole moves to the new dot. | |
| Player's next dot, 0 to 150 | The edge marks shrink back into the edges. | ease in |

The ripples are drawn under the pieces, so they never hide a dot. In a two-player game every dot
lands this way, in its side's ink: the player who looks next did not see it placed.

## Capture

For captures of 1 to 4 dots. 0 ms is the moment the closing dot lands. Several areas closed by one
dot play at the same time, and their dots add up to one +N.

<img src="motion/strip-capture.png" width="720" alt="Key frames: the ring being drawn around two blue dots, the wash spreading, the cuts, +2 at the area, then the +2 gone and the score and mission note showing 2">

<img src="motion/capture-light.gif" width="270" alt="Animation: a capture of two dots">

[Same in the dark version](motion/capture-dark.gif)

| Time | What moves | Easing |
| --- | --- | --- |
| 0 to 200 | The closing dot lands, as in placing. | as placing |
| 150 to 550 | The ring is drawn like a pen stroke: from the closing dot, around the area, back to it. It is the final ring, with the same bow and thickness. | ease in-out |
| 550 | Medium vibration. | |
| 500 to 800 | The wash and the hatching spread from the closing dot as a growing circle, kept inside the ring. | ease out |
| 600 onwards | Each captured dot's cut is drawn across it in 100 ms, the nearest dot first, 40 ms apart. | linear |
| 700 to 850 | "+N" grows in over the area, in the Score style and the captor's ink. | ease out |
| 800 | The opponent's face changes: upset when the player captured, smug when the computer did. | |
| 850 to 1100 | +N flies along a curve to the captor's tally and shrinks to 55%. | ease in |
| 1100 | The tally and the score take the N points, and the mission note ticks its boxes. | |
| 1100 to 1250 | The score swells to 130% and back; the mission note to 106% and back. | |

## Big capture

For 5 dots or more in one move. It is the capture with more weight, and under 1.5 s. 0 ms is the
moment the closing dot lands.

<img src="motion/strip-big-capture.png" width="720" alt="Key frames: the ring drawn around six dots, ink strokes bursting out, +6 and Six at once! below the area, the face falling, the score at 8 and the mission done">

<img src="motion/big-capture-light.gif" width="270" alt="Animation: a capture of six dots, played in full, then again with a tap at 500 ms that finishes it at once">

[Same in the dark version](motion/big-capture-dark.gif). The GIF plays the capture twice: in
full, then with a tap at 500 ms, which shows the end state at once.

| Time | What moves | Easing |
| --- | --- | --- |
| 0 to 200 | The closing dot lands. | as placing |
| 150 to 650 | The ring is drawn, as in a capture. | ease in-out |
| 650 | Strong vibration. | |
| 650 to 830 | The board shakes sideways: 2 dp, three swings, each smaller. | |
| 650 to 900 | A burst: 14 short strokes in the captor's ink around the area move outwards, grow and shrink away. None crosses a dot. | ease out |
| 650 to 950 | The wash and the hatching spread. | ease out |
| 700 onwards | The cuts, 30 ms apart. | linear |
| 850 to 1000 | "+N" grows in below the area at 40 sp, with "Six at once!" under it (Caveat, the Headline size). The number of dots is a word there: Caveat is never used for figures. | ease out |
| 900 | The opponent's face falls. | |
| 1000 to 1250 | +N flies to the tally. "Six at once!" stays. | ease in |
| 1250 to 1400 | The tally, score and mission note take it and swell as in a capture. "Six at once!" shrinks away. | ease in |

## Threat pulse

A group one move from capture gets a Highlighter ring, and the point that would close it a small
circle. 0 ms is 200 ms after the dot that makes the threat lands, when that dot has settled.

<img src="motion/strip-threat.png" width="720" alt="Key frames: the threat rings drawing in, swelling at the top of a pulse, and resting">

<img src="motion/threat-light.gif" width="360" alt="Animation: the computer's dot lands, the threat rings appear and pulse three times, then stay still">

[Same in the dark version](motion/threat-dark.gif)

| Time | What moves | Easing |
| --- | --- | --- |
| 0 to 250 | The rings draw in: each shrinks from 130% to its size while its colour blends from Paper to Highlighter. The closing points grow from nothing. | ease out |
| 250 to 5050 | Three pulses of 1600 ms. The ring's line goes from 2 dp to 3.5 dp and back, and the ring from 100% to 108% of its size and back. The closing point goes from 100% to 135% and back, in step. | ease in-out |
| 5050 | Everything is still. | |

- A faint ring (two moves from capture) draws in the same way but never pulses.
- A new threat pulses only its own group. The others stay still.
- When the player's group is in danger, the opponent's face turns smug.

## Dots running out

The P-10 testers asked to see when their dots are running out. From 5 dots left, the count turns
Highlighter, the colour that means "look here, act soon", and the last dots show as a row. Each side's
count does this for its own dots. 0 ms is the moment a dot is placed.

<img src="motion/strip-running-out.png" width="720" alt="The count row at 6, 5, 4, 3, 2 and 1 dots left: from 5 the number is Highlighter with a row of small dots, at 1 it says Last dot!">

<img src="motion/running-out-light.gif" width="270" alt="Animation: five turns, from 6 dots left to Last dot!">

[Same in the dark version](motion/running-out-dark.gif)

| When | What moves | Easing |
| --- | --- | --- |
| The count reaches 5 | The number blends from Pencil to Highlighter in 200 ms. A row of five small pieces (8 dp, the side's shape and ink) writes in beside it, 50 ms apart, 150 ms each. | linear; ease out |
| Each dot after that | The number swells to 130% and back in 300 ms. The last small piece in the row shrinks away in 150 ms. | ease in |
| 3 left, mission not done | The mission note swings once, 3° each way, in 400 ms. | |
| 1 left | "Last dot!" writes in, left to right, in 300 ms, in place of the number: Caveat 24 sp, Highlighter. | linear |

Both the number (24 sp, extra bold) and "Last dot!" (24 sp) are large text, so their target is 3:1.
Highlighter on paper is 4.48 in light and 9.02 in dark.

## Boosters

0 ms is the moment the player taps Confirm in the booster preview (A-05). In every case the
booster's count drops at 0 ms and swells to 135% and back in 200 ms. At 0 left, the button turns
dashed and grey.

<img src="motion/strip-boosters.png" width="720" alt="Key frames of the five boosters: the wall drawn and hatched, the hint circled twice, the bomb removing four blue dots, the double dot placing two dots with sparkles, and Undo's preview with two pale dots in dashed rings, rubbed out by grey strokes, the computer's first">

<img src="motion/boosters-light.gif" width="360" alt="Animation: Wall, Hint, Bomb, Double dot and Undo, one after another, with the five boosters in the tool bar">

[Same in the dark version](motion/boosters-dark.gif)

**Wall** blocks one empty point.

| Time | What moves | Easing |
| --- | --- | --- |
| 0 to 200 | A square, 0.7 of the spacing (17 dp), is drawn round the point in one Pencil stroke, 1.8 dp. | ease in-out |
| 200 to 400 | Five hatch lines in Graphite fill it, one after another. It stays. | linear |

**Hint** shows the player's best move.

| Time | What moves | Easing |
| --- | --- | --- |
| 0 to 500 | The point is circled twice in the player's ink, 250 ms per loop: the same mark as the next level on the map. | linear |
| Player's dot lands, 0 to 150 | The circles shrink away. | ease in |

**Bomb** removes the opponent's dots in a plus of 5 points.

| Time | What moves | Easing |
| --- | --- | --- |
| 0 to 150 | The bomb drops on the centre point: it grows from 60% to 100%. | ease out |
| 150 | The blast. Strong vibration. The bomb is gone, and the links of the hit dots go with it. | |
| 150 to 300 | The board shakes: 2 dp, two swings, each smaller. | |
| 150 to 220 | The hit dots swell to 130%. | ease out |
| 220 to 350 | They shrink to nothing. | ease in |
| 150 to 450 | Twelve specks in the opponent's ink fly out from the centre, growing and then shrinking away. | ease out |

**Double dot** lets the player place two dots in one turn.

| Time | What moves | Easing |
| --- | --- | --- |
| 0 to 300 | A small "×2" chip in the player's ink grows in beside their name. | ease out |
| First dot lands, 0 to 250 | The dot lands as usual, and four short strokes spark out from it. The chip turns to "×1" and swells to 125% and back in 200 ms. | ease out |
| Second dot lands, 0 to 250 | The same sparks. The chip shrinks away in 150 ms, and the turn passes. | ease in |

**Undo** takes back the player's last turn: their dot (both after a double dot), any booster used
in that turn, and the computer's answer (decision 0023). It has no point to aim at, so its preview
shows the turn it takes back: each of those pieces in its pale colour (Red pale, Blue pale) inside
a dashed Graphite ring, and the threat marks that turn made are gone. The note over the player's
line and the tool bar says "Undo takes back the pale dots." with Cancel and Take back, the canvas
frame "Game: Undo's preview". The turn is rubbed out newest first, like a pencil eraser.

| Time | What moves | Easing |
| --- | --- | --- |
| 0 to 250 | The computer's answer is rubbed out: three short Graphite strokes scrub back and forth across it while the piece shrinks to nothing and its links pull back. A capture it made goes with it: the ring undraws, the wash blends back to paper, the cuts go, and its score counts back. | ease in |
| 200 to 450 | The player's dot is rubbed out the same way, both dots together after a double dot, and any capture it made. A booster used in that turn flies back to its button from where it was used (or from the player's dot), from 200 to 400 ms; its count goes up as it lands and swells to 135% and back in 200 ms. Dots a bomb took in that turn grow back in, from nothing to their size, and their links grow back from their neighbours. | ease in; the flight ease in-out; growing back ease out |
| 450 to 600 | The scrub strokes shrink away. The dots-left count goes up, the pen stroke sits under "You", and the opponent's face goes back to how it looked before its answer. | ease out |

Undo's button waits until there is a turn to take back: before the player's first move it shows
its icon in Graphite on paper, with its count still red, as on the canvas frame "Game: the level
opens". A tap on it then does nothing. Each level allows one Undo, so after it the button turns
dashed and grey with 0, like any booster.

## End of game

0 ms is the moment the game is over and the last move has played out. In a mission level that is
when the capture that finishes the mission ends.

<img src="motion/strip-end.png" width="720" alt="Key frames: the tool bar and pause icon leaving, each captured area lighting up in turn, the result note sliding up">

<img src="motion/end-light.gif" width="270" alt="Animation: the end of the game, the areas lighting up in turn, the result note and its stars">

[Same in the dark version](motion/end-dark.gif)

| Time | What moves | Easing |
| --- | --- | --- |
| 0 to 250 | The pause icon slides up off the screen and the tool bar slides down off it. | ease in |
| 300 onwards | Each captured area lights up in the order it was captured, for 300 ms each, 150 ms apart: its ring thickens to 190% and back, and its wash blends towards the pale colour and back. Its side's score swells to 130% and back. | ease in-out |
| | If there are many areas, the gap shrinks so that all of them fit in 1500 ms. | |
| last area + 200 | The result note slides up from below in 300 ms. | ease out |
| last area + 200 to + 500 | It tips to −2.5° and settles at its −1.5° slant in the last 100 ms. The opponent's face changes. | ease in-out |

With the three areas of Level 7, the note starts at 1100 ms and has settled at 1400 ms.

## Star reveal

0 ms is the moment the result note has settled.

<img src="motion/strip-stars.png" width="720" alt="Key frames: You won! written in, the first star stamped in large and settling, the second star, the empty third star drawn in outline">

| Time | What moves | Easing |
| --- | --- | --- |
| 0 to 300 | The headline writes in, left to right. | linear |
| 300 + 300 per star | An earned star is stamped: it shrinks from 180% to its size and turns from −15° to straight, in 200 ms. A tick and a light vibration as it lands. | ease in |
| | A star not earned is drawn in outline in 200 ms. | linear |
| 1100 | Done, with three stars. | |

The score lines and the buttons are there as soon as the note appears, and the buttons work at once.
A tap elsewhere on the note finishes the reveal.

## Level unlock

After Next, the map shows the finished level and the way on. 0 ms is the moment the map shows.

<img src="motion/strip-unlock.png" width="720" alt="Key frames on the map: stars flying to level 7, level 7 filling in blue, the path inking towards level 8, level 8 circled with next!, the Level card of level 8 coming up">

<img src="motion/unlock-light.gif" width="270" alt="Animation: the level unlock on the map">

[Same in the dark version](motion/unlock-dark.gif)

| Time | What moves | Easing |
| --- | --- | --- |
| 0 to 500 | The new stars fly, 100 ms apart and 400 ms each, from where the result note showed them to the level's stop, shrinking to the map's star size. | ease in-out |
| 500 to 800 | The stop fills: a blue disc grows from its centre. Its red "next" rings shrink away in the first 150 ms. | ease out; ease in |
| 800 to 1200 | The path to the next level inks in as a blue dashed line. | ease in-out |
| 1200 to 1600 | The next level is circled twice in red, 200 ms per loop. | linear |
| 1300 to 1600 | "next!" writes in beside it. | linear |
| 1800 to 2100 | The next level's card slides up and the map dims behind it. | ease out |

**A world opens.** When these stars open the next world, the page turns before the path inks in:
the new world's page slides in from the right in 600 ms (ease in-out). Everything after it moves
600 ms later.

A tap at any time shows the end state with the card up.

## Level opening, notes and cards

These have no GIF. The canvas frame "Game: the level opens" shows the opening.

- **Level opening.** The mission note sits big over the board for 1200 ms, then shrinks into its
  corner in 300 ms (ease in-out). A tap ends it at once and counts as a tap.
- **Notes and cards** over the map or the board (Level card, Pause, Result, Chest, Today's
  puzzle, Play a game, the booster preview): they slide up from below in 300 ms (ease out) while
  the background dims; they leave by sliding down in 200 ms (ease in). Their buttons work at once.

## Star chart

The [Star chart theme](theme-star-chart.md) is a printed star chart: straight lines, solid
colours, nothing glows. Its board moves as Notebook's, at the same times, except in a capture. There
Notebook's pen stroke becomes a constellation line traced from star to star, and the wash becomes a
haze that blends in. This section lists only those changes.

Every other moment on this page plays as written, in the theme's colours: Gold ink where the page
says the red side's ink, Ice ink for the blue side's, Nova for Highlighter, Starlight for Pencil and
Dust for Graphite. Placing a dot, the computer's move, the threat pulse, the boosters and the end of
the game have no section here, because nothing about them changes. The canvas frames are the
[Star chart frames](canvas/README.md#star-chart).

### Capture in Star chart

0 ms is the moment the closing star lands, as in [Capture](#capture). The GIFs below show the same
capture of two comets as Notebook's.

<img src="motion/strip-capture-star-chart.png" width="720" alt="Key frames: the gold line traced from the closing star up the left side, over the top and down the right, each star swelling as the line reaches it; the haze and hatching blending in; the comets cut">

<img src="motion/capture-star-chart-light.gif" width="270" alt="Animation: a capture of two comets in Star chart">

[Same in the dark version](motion/capture-star-chart-dark.gif)

| Time | What changes from Notebook | Easing |
| --- | --- | --- |
| 150 to 550 | The ring is a constellation line. It is traced from the closing star, round the area from star to star, and back to the closing star, at an even speed, as if drawn with a ruler. It is straight and of even width from the first moment. | linear |
| As the line reaches each star | That star twinkles: it swells to 125% and back in 120 ms. The closing star twinkles last, at 550 ms, as the line comes back to it, with the medium vibration. | ease in-out |
| 500 to 800 | The haze and its hatching blend in from Paper over the whole area at once. They do not spread from the closing star. | ease out |

The cuts, +N, the opponent's face and the score are Notebook's.

### Big capture in Star chart

<img src="motion/strip-big-capture-star-chart.png" width="720" alt="Key frames: the line traced round the six comets star by star, the closing star twinkling, the burst of gold strokes and the haze blending in, then the comets cut">

<img src="motion/big-capture-star-chart-light.gif" width="270" alt="Animation: a capture of six comets in Star chart, played in full, then again with a tap at 500 ms that finishes it at once">

[Same in the dark version](motion/big-capture-star-chart-dark.gif)

| Time | What changes from Notebook | Easing |
| --- | --- | --- |
| 150 to 650 | The line is traced star to star, as in a capture. With ten stars on the ring, a star twinkles about every 50 ms. | linear |
| 650 | The closing star twinkles as the line comes back to it, with the strong vibration, the shake and the burst. | ease in-out |
| 650 to 950 | The haze and hatching blend in over the whole area. | ease out |

The shake and the burst of 14 short strokes are Notebook's, in Gold ink, as on the canvas frame
"Game: right after a big capture".

**Why these changes.**

- **An even speed.** Notebook's ring speeds up and slows down like a hand with a pen. A chart's
  lines are ruled, so the line runs at one speed. It still takes the same 400 ms (500 ms for a big
  capture), so the ring's sound fits it.
- **A twinkle, not a glow.** The theme page rules out glows. A star that swells as the line
  reaches it gives the line of light from the mood board (picture 2) in solid colours.
- **A haze that blends in.** A nebula appears where it is; it does not flow in from one side. The
  haze blends from Paper to its colour, so nothing on the board turns see-through (rule 3).

**With animations removed,** the line, the haze and the cuts show at once, as in Notebook. No
star twinkles.

## Neon arcade

The [Neon arcade theme](theme-neon-arcade.md) is light on a dark screen: every live piece and
every tube sits in a band of glow. Its board moves as Notebook's, at the same times, with the light
added. A piece lights up just after it lands. A tube lights up as it closes, with a burst of sparks.
A captured piece goes dark. Every glow is a solid band that grows or switches on, never a blur that
fades in. This section lists only those changes.

Every other moment on this page plays as written, in the theme's colours: Magenta ink where the
page says the red side's ink, Cyan ink for the blue side's, Volt for Highlighter, Main text for
Pencil and Dim text for Graphite. The canvas frames are the
[Neon arcade frames](canvas/README.md#neon-arcade).

**A glow follows its piece.** Whenever a piece grows, shrinks or swells (placing, a bomb, Undo's
rub-out), its glow band grows, shrinks and swells with it. When an area lights up at the end of the
game, the whole tube thickens to 190%: glow, tube and core together.

### Placing a dot in Neon arcade

0 ms is the moment the dot is placed, as in
[Placing a dot](#placing-a-dot-and-chain-lines). The computer's dot lands the same way, with its
ripples in Cyan ink.

<img src="motion/strip-placing-neon-arcade.png" width="720" alt="Key frames: the pale disc in its ring; the magenta disc growing without glow; at 120 ms its pale band starting to grow; at 200 ms the band full and the pinhole in the disc">

<img src="motion/turn-neon-arcade-light.gif" width="360" alt="Animation: the player places a magenta disc, which lights up; the computer's cyan triangle lands beside the thumb with two spreading rings and lights up">

[Same in the dark version](motion/turn-neon-arcade-dark.gif)

| Time | What changes from Notebook | Easing |
| --- | --- | --- |
| 0 to 120 | The piece grows to 115%, as in Notebook, unlit: it has no glow yet. | ease out |
| 120 to 200 | While it settles to 100%, its glow grows from nothing to the 1.5 dp band. | ease out |

**The last move.** The theme page leaves open how the last move moves. It moves as Notebook's: the
pinhole jumps to the new piece at 200 ms and stays still. The mood board's pulsing ring is left out:
a ring means a threat on this board, and nothing moves for longer than 5 seconds (rule 4).

### Capture in Neon arcade

0 ms is the moment the closing disc lands, as in [Capture](#capture). The GIFs show the same
capture of two triangles as Notebook's.

<img src="motion/strip-capture-neon-arcade.png" width="720" alt="Key frames: the magenta tube drawn round the two triangles without glow; at 560 ms it lights and sparks fly from the closing disc; at 600 ms it is dark again; at 640 ms lit for good; the triangles cut and without glow">

<img src="motion/capture-neon-arcade-light.gif" width="270" alt="Animation: a capture of two triangles in Neon arcade">

[Same in the dark version](motion/capture-neon-arcade-dark.gif)

| Time | What changes from Notebook | Easing |
| --- | --- | --- |
| 150 to 550 | The tube is drawn from the closing disc, round the area and back, as Notebook's ring is, but unlit: in the side's ink only, without its glow and core. It is straight and of even width. | ease in-out |
| 550 | The tube lights up: its glow and core switch on, with the medium vibration. | |
| 590 to 630 | One flicker: the glow and core go out for 40 ms and come back for good, as a neon tube does when it starts. | |
| 550 to 800 | Sparks: up to ten short streaks fly out from the closing disc, away from the area, growing and then shrinking away. Each is a small tube, 2.2 dp of the side's ink with its core along the middle. None crosses a piece. | ease out |
| 600 onwards | Each captured piece's glow goes out at once when its cut starts: the light is out. | |

The wash spreads from the closing disc as Notebook's does. The cuts, +N, the face and the score are
Notebook's.

### Big capture in Neon arcade

<img src="motion/strip-big-capture-neon-arcade.png" width="720" alt="Key frames: the tube drawn round the six triangles without glow; at 660 ms it lights, sparks fly from the closing disc and the burst starts; at 700 ms the flicker; the wash spreading; the triangles cut, without glow">

<img src="motion/big-capture-neon-arcade-light.gif" width="270" alt="Animation: a capture of six triangles in Neon arcade, played in full, then again with a tap at 500 ms that finishes it at once">

[Same in the dark version](motion/big-capture-neon-arcade-dark.gif)

| Time | What changes from Notebook | Easing |
| --- | --- | --- |
| 150 to 650 | The tube is drawn unlit. | ease in-out |
| 650 | It lights up, with the strong vibration and the shake. | |
| 690 to 730 | The flicker. | |
| 650 to 900 | The sparks from the closing disc, at the same time as the burst. | ease out |
| 700 onwards | Each captured piece goes dark as its cut starts, 30 ms apart. | |

The shake and the burst of 14 short strokes round the area are Notebook's, in Magenta ink, as on
the canvas frame "Game: right after a big capture".

**Why these changes.**

- **Lit after it lands.** A sign lights once it is up. The glow grows in while the piece settles,
  so placing still ends at 200 ms.
- **The tube lights when it closes.** A tube lights only when the circuit is complete. The light
  comes on at the moment of the closing sound and vibration, so the three arrive together.
- **One flicker.** It makes the light read as a neon tube starting. It is a single 40 ms dip on a
  5 dp band, far below the WCAG limit of three flashes in any one second, and it is only on a
  capture, not on every dot.
- **Sparks where it closes,** as in the mood board (picture 2). They fly away from the area, so
  they never cover the pieces just captured.
- **Dark when captured.** The theme page gives a captured piece no glow. It goes out with its cut,
  so the two signs that don't rely on colour arrive together.

**With animations removed,** a placed piece shows with its glow at once, and a capture shows its
lit tube, wash, cuts and dark pieces at once. There is no flicker and no sparks.

## Checks

- **No animation holds the next tap for more than 0.3 s.** The board takes every tap at once, and a
  tap finishes whatever is running. Notes and cards take taps while they slide in. The longest wait
  for a tap is 0 ms.
- **A big capture is under 1.5 s and can be skipped.** It takes 1400 ms, and a tap shows its end at
  once (second half of the big-capture GIF).
- **Booster effects are under 1.5 s** (the V-06 check): Wall 400 ms, Hint 500 ms, Bomb 450 ms,
  Double dot 300 ms and then 250 ms per dot, Undo 600 ms.
- **The board themes keep these limits.** Star chart and Neon arcade use the same times, so a
  capture still takes 1250 ms and a big capture 1400 ms. A tap finishes their twinkles, flicker
  and sparks with the rest (second half of each theme's big-capture GIF).

## New in this storyboard

These looks are new here. They are proposals for the owner to accept or change:

- The computer's move: two ripples and the edge marks on the top and left edges.
- Placing by drag: the guides drawn out from the ring in 100 ms, then jumping with the finger.
- Dots running out: the Highlighter count, the row of small pieces, the mission-note nudge and
  "Last dot!".
- The wall: a square in pencil, hatched.
- The hint: the point circled twice, like the next level on the map.
- The double dot: the "×2" chip beside the player's name.
- A big capture's +N and words below the area, where the canvas frame shows them.
- Star chart: the constellation line traced at an even speed, each star twinkling as the line
  reaches it, and the haze blending in over the whole area.
- Neon arcade: a piece's glow growing in after it lands; the tube drawn unlit and lighting up as it
  closes, with one flicker; sparks from the closing piece; a captured piece going dark with its cut;
  the last move marked by the pinhole alone.

## The mock-ups

[`motion/draw.py`](motion/draw.py) draws every GIF and strip on this page from the times above,
using the canvas drawing ([`canvas/draw.py`](canvas/draw.py)). It draws 25 frames a second;
Google Chrome renders them and ffmpeg makes the GIFs.

```sh
python3 docs/design/motion/draw.py               # every GIF and strip (several minutes)
python3 docs/design/motion/draw.py capture       # only the ones named
python3 docs/design/motion/draw.py star-chart    # only one board theme's: star-chart or neon-arcade
```

A board theme's GIFs are named `<moment>-<theme>-<light|dark>.gif` and its strips
`strip-<moment>-<theme>.png`. Its board is drawn from the theme page's values, as on the canvas
frames, with links off.

It needs Python 3, Google Chrome, ffmpeg and a network connection (the fonts come from Google
Fonts). Change a time on this page, in the script and in the app together: the app reads every
time from `app/src/main/kotlin/io/github/pavelya/dots/ui/motion/Times.kt`, and `MotionTimesTest`
checks them against this page.

**On a phone.** Every GIF frame is 360 dp wide, drawn at 1.5 px per dp. Open a GIF on the phone
and fit it to the screen width. On a phone about 360 dp wide it then shows at the size the game
will have, so the ripple can be checked against a real thumb.
