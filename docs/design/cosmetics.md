# Piece styles and ring effects

The Shop's small cosmetics at launch ([decision 0030](../decisions/0030-money-design.md),
[`money.md`](money.md)): two piece styles, **Hearts and clubs** and **Flame and frost**, and two
ring effects, **Lasso** and **Rubber stamp**. This page gives every value the app needs to draw
them. A piece style changes only the pieces. A ring effect changes only how a ring and its fill are
drawn, and how a capture moves. Both work in every theme, drawn with that theme's inks, in its light
and dark versions.

| Name | Kind | What it is | Price | Earned at | In the code |
| --- | --- | --- | --- | --- | --- |
| Hearts and clubs | piece style | Red's pieces are hearts, Blue's are clubs | $0.99 | 40 stars | `hearts-and-clubs` |
| Lasso | ring effect | The ring is a twisted rope, thrown loose round the area and pulled tight | $0.99 | 70 stars | `lasso` |
| Flame and frost | piece style | Red's pieces are flames, Blue's are snowflakes | $0.99 | 100 stars | `flame-and-frost` |
| Rubber stamp | ring effect | The ring has a thin second line inside it, and the area is stamped down at once | $0.99 | 125 stars | `rubber-stamp` |

The prices and star counts are those of [`money.md`](money.md). No world or theme uses these
names ([decision 0028](../decisions/0028-worlds-and-themes.md)). "Red" and "Blue" are the sides as
the code names them: gold and ice in Star chart, magenta and cyan in Neon arcade.

## Rules

- **The theme stays in charge.** A piece style and a ring effect use the theme's colours and
  nothing else. They never add a colour, a texture under the points or a see-through layer.
- **The two sides differ by shape,** as on every theme ([Piece shapes](pieces.md)). In greyscale
  and in each kind of colour blindness, the shapes alone tell the sides apart.
- **Every mark stays:** the last-move pinhole, the cut on a captured piece, the preview, the
  high-contrast band, the threat rings and the closing point, at the sizes of
  [Piece shapes](pieces.md).
- **A capture stays as easy to read as in the theme.** The ring stays in the captor's ink and at
  least as wide as the theme's, and the area keeps its wash and hatching, leaning the theme's way.
- **The same times.** A capture still closes at 550 ms (650 ms for a big one), with the theme's
  sound and vibration, and still takes 1250 ms (1400 ms). There are no new sounds: board sounds
  belong to the theme ([`sound.md`](sound.md)).
- **With animations removed,** a ring effect falls back to the theme's own ring and fill
  ([`money.md`](money.md)). A piece style is the same with or without animations.
- **Defaults.** The theme's own pieces and ring are the default. A theme, a piece style and a ring
  effect are chosen separately.

## Where they show

Wherever the theme's pieces and rings show: the game board, the pieces beside the names in the
score rows, the two players' turn banner, and the Shop's small boards. Like the theme, they do not
reach the map pages, the Level card's small board or How to play, which stay Notebook's
([`theme-star-chart.md`](theme-star-chart.md), Outside the game screen). The Double dot booster's
icon keeps Notebook's circle and diamond, as in every theme.

## Piece styles

Sizes are in **dot sizes**, the dot size of [Piece shapes](pieces.md) (half the spacing, 9 to
20 dp; 12 dp at 24 dp spacing). Every shape is centred on its point, and is solid: overlapping parts
of one shape are one area of ink.

<img src="pieces/cosmetics-pieces-cards.svg" alt="Hearts and clubs in each theme, light and dark: heart, club, each with the last-move pinhole, each captured with a cut, and each as a preview" width="768">

<img src="pieces/cosmetics-pieces-fire.svg" alt="Flame and frost in each theme, light and dark: flame, snowflake, each with the last-move pinhole, each captured with a cut, and each as a preview" width="768">

From left to right: red piece, blue piece, red and blue last move, a captured red piece (in a blue
area), a captured blue piece (in a red area), red and blue preview. Rows: Notebook light and dark,
Star chart light and dark, Neon arcade light and dark. Each cell is one 24 dp grid square.

### Hearts and clubs

| Element | Shape | In dot sizes |
| --- | --- | --- |
| Red piece: heart | The curve x = 16 sin³ t, y = −(13 cos t − 5 cos 2t − 2 cos 3t − cos 4t) for t from 0 to 2π, y pointing down, scaled by 0.0376 and moved down by 0.031, so its centre of mass sits on the point. Point down. | 1.20 wide, 1.09 high; area 0.80; its point 0.67 below the point |
| Blue piece: club | Three round lobes, radius 0.27, their centres 0.30 from the point, straight up, lower left and lower right (at −90°, 150° and 30°); a round hub, radius 0.20, on the point; and a foot, a trapezoid from (±0.07, 0.15) to (±0.22, 0.58). | 1.06 wide, 1.15 high; area 0.77; reaches 0.62 |

### Flame and frost

| Element | Shape | In dot sizes |
| --- | --- | --- |
| Red piece: flame | A flame, point up and leaning a little right, with a smaller tongue on its left. Six cubic curves; each row is a start point and two control points, and ends at the next row's start: (0.08, −0.68) (0.34, −0.42) (0.50, −0.16); (0.48, 0.12) (0.47, 0.37) (0.26, 0.52); (0.00, 0.52) (−0.27, 0.52) (−0.48, 0.36); (−0.48, 0.10) (−0.48, −0.12) (−0.43, −0.30); (−0.34, −0.46) (−0.28, −0.30) (−0.20, −0.20); (−0.12, −0.16) (−0.13, −0.40) (−0.05, −0.55). | 0.96 wide, 1.20 high; area 0.80; its tip 0.68 above the point |
| Blue piece: snowflake | Six arms 0.22 wide with round tips, 0.62 from the point, one straight up; a round hub, radius 0.24; and on each arm, 0.36 from the point, two twigs 0.15 wide and 0.20 long with round ends, turned 50° out to either side. | 1.10 wide, 1.24 high; area 0.74; reaches 0.62 |

### Marks

As [Piece shapes](pieces.md), for every piece style:

- **Last move:** the pinhole, 0.3 dot sizes across, in Paper, on the point. Each shape is solid
  there: the heart's middle, the club's hub, the flame's body, the snowflake's hub.
- **Captured:** the cut, a band 1/6 of a dot size wide and 1.5 long, through the point along the
  captor's hatching, in the captor's wash. Neon arcade's captured pieces lose their glow, as the
  theme's own do.
- **Preview:** the moving side's shape in its pale colour inside the preview ring.
- **Glow (Neon arcade only):** the 1.5 dp band round each live piece's outline, with round
  corners, as the theme's own pieces have.
- **High contrast:** the dot size becomes 0.6 of the spacing, and each piece sits in a 1.5 dp band
  of the Paper or wash under it, following its outline with round corners. At 24 dp spacing the
  flame's tip then reaches 9.8 dp from the point and the heart's 9.7 dp, a little into the line of
  a threat ring round them (its inside edge is at 9.1 dp), as Neon arcade's triangle does. The
  piece is drawn on top, and the ring still reads as a ring.

### Why these shapes

- **Each pair differs most in outline at 9 dp.** A heart has two lobes over one point; a club has
  three round lobes on a foot. A flame is one smooth body with a tip; a snowflake is six arms and
  open space between them. None is hollow, so none looks like a threat ring or the preview ring.
- **The colours fit.** Red hearts and blue clubs are a card player's suits. A warm flame and a cold
  snowflake suit red and blue, gold and ice, magenta and cyan.
- **Equal weight.** The heart, club and flame cover 0.77 to 0.80 square dot sizes, about
  Notebook's round dot (0.785). The snowflake covers 0.74, because its arms spread wider and look
  heavier than their area.
- **Twigs on the snowflake.** With straight arms only it read as an asterisk. The twigs make it a
  snowflake and still show at 9 dp.
- **Points up and down, never along a diagonal.** The flame's tip, the heart's point and the
  snowflake's arms keep clear of the cut, which runs on a diagonal, so the cut always crosses ink.

## Ring effects

<img src="pieces/cosmetics-rings.svg" alt="A red capture of two blue pieces in each theme, light and dark: the theme's own ring, Lasso and Rubber stamp" width="720">

Columns: the theme's own ring, Lasso, Rubber stamp. Rows as the piece sheets. "The ring's width"
is the theme's: 1/12 of the spacing, 1.5 to 3 dp (2 dp at 24 dp spacing). Each ring still runs
through the centres of the capturing pieces, along the theme's path: Notebook's bows like a pen
line, Star chart's and Neon arcade's are straight.

### Lasso

**How it looks.** The ring is a rope: a cord 1.6 times the ring's width (3.2 dp at 24 dp
spacing), in the captor's ink, with round ends. Slanted twists cross it, in the captor's pale
colour (the hatching's colour), each 0.3 of the ring's width thick and leaning 55° from the cord's
line, reaching 92% of the way across. Each side of the ring holds a whole number of twists, about
four per grid step (one every 6 dp at 24 dp spacing), spread evenly along it. The pieces on the
ring cover the twists nearest them, so about two show between two neighbouring pieces. Notebook's cord keeps the pen line's bow but not its change of thickness. In Neon arcade the
cord sits in the theme's glow, a band 1.5 dp wider than the cord on either side, and has no core.
The wash and hatching are the theme's.

**How the capture moves.** 0 ms is the moment the closing piece lands, as on the
[motion page](motion.md#capture).

| Time | What moves | Easing |
| --- | --- | --- |
| 0 to 200 | The closing piece lands, as in the theme. | as placing |
| 150 to 450 | The rope is thrown: it is drawn from the closing piece round the area and back, loose, at 115% of the ring's size about the area's centre. Pieces stay on top of it. | ease in-out |
| 450 to 550 | It pulls tight to the ring's place. | ease in |
| 550 | Tight: the theme's closing sound and the medium vibration. | |
| 500 onwards | The wash, the cuts, +N, the face and the score, as in the theme. | as the theme |

A big capture throws the rope from 150 to 550 ms and pulls it tight from 550 to 650 ms, closing at
650 ms with the strong vibration, the shake and the burst. In Star chart the closing star twinkles
as the rope pulls tight; the stars along the way don't, because the rope doesn't reach them until
then. In Neon arcade the rope is thrown unlit and lights at 550 ms (650 ms), with the theme's
flicker and sparks.

<img src="pieces/cosmetics-strip-lasso.png" width="720" alt="Key frames of a capture with Lasso, Notebook light and dark: the loose rope drawn round two blue pieces, the loop complete, pulling tight, tight, the wash spreading and the cuts, done">

### Rubber stamp

**How it looks.** The theme's own ring, and inside it a second line, as on the border of a rubber
stamp: half the ring's width, at least 1 dp, in the captor's ink, 2.5 dp inside the ring's inner
edge, with round corners. It follows the ring's path, bow and all. In Neon arcade the ring keeps
its glow and core; the inner line has neither, and clears the glow by 1 dp. The wash and hatching
are the theme's, and run under the inner line.

**How the capture moves.**

| Time | What moves | Easing |
| --- | --- | --- |
| 0 to 200 | The closing piece lands, as in the theme. | as placing |
| 250 to 550 | The stamp comes down: the ring, its inner line, the wash and the hatching show at once at 125% of the area's size about its centre, and shrink to their place while their colours blend from Paper to their own. Pieces stay on top. | ease in |
| 550 | Down: the theme's closing sound and the medium vibration. | |
| 600 onwards | The cuts, +N, the face and the score, as in the theme. The wash doesn't spread: it came down with the stamp. | as the theme |

A big capture brings the stamp down from 250 to 650 ms, with the strong vibration, the shake and
the burst at 650 ms. In Star chart the closing star twinkles as the stamp comes down. In Neon arcade
the ring comes down unlit and lights at 550 ms (650 ms), with the flicker and sparks.

<img src="pieces/cosmetics-strip-stamp.png" width="720" alt="Key frames of a capture with Rubber stamp, Notebook light and dark: the area coming down large and pale, shrinking and darkening, landing, the cuts, done">

Nothing turns see-through: the stamp blends from Paper, as the threat rings draw in
([motion page](motion.md#rules-for-all-motion), rule 3). A tap finishes either effect, as it
finishes the theme's capture.

### Why these effects

- **A lasso rounds up.** A pinfold is a pen for stray animals, and a capture rounds up the other
  side's pieces. The rope is thrown loose and pulled tight, so the moment it closes is the moment
  of the sound.
- **Twists, not two strands.** Two thin strands that crossed each other left gaps that read as a
  dotted line, and dots and dashes already mean "not placed yet" on the board (Undo's preview). A
  solid cord with pale twists stays one line.
- **A stamp lands at once.** It is the opposite of the pen: no line is drawn, the whole area arrives
  in one press. The double border is the stamp's mark, and it shows even on the smallest ring.
- **Wider, never thinner.** Lasso's cord is wider than the theme's ring, and Rubber stamp adds a
  line, so neither makes a capture harder to see.

## The Shop's small board

The Shop shows every item on a small board of 5 x 5 points: a red ring round one blue piece, a
red piece and three blue pieces loose, and the last move marked
([D-27 wireframes](flows.md#wireframes), Shop). A theme's tile is drawn by that theme. A piece
style's or ring effect's tile is drawn in the theme in use, in the version the phone picks, so the
player sees the item on their own board. The default tile, **Theme's own**, shows the theme's own
pieces or ring. At 140 dp the spacing is 28 dp, so pieces are 14 dp across.

| | Light | Dark |
| --- | --- | --- |
| **Shop: piece styles.** The Pieces tab, Notebook in use. | <img src="canvas/shop-pieces-light.png" width="180" alt="Light: the Shop's Pieces tab"> | <img src="canvas/shop-pieces-dark.png" width="180" alt="Dark: the Shop's Pieces tab"> |
| **Shop: ring effects.** The Rings tab, Notebook in use. | <img src="canvas/shop-rings-light.png" width="180" alt="Light: the Shop's Rings tab"> | <img src="canvas/shop-rings-dark.png" width="180" alt="Dark: the Shop's Rings tab"> |

## Canvas frames

Each item in Notebook and in one other theme, light and dark, on the
[Pinfold canvas](https://claude.ai/artifact/GpJSNGSjeJGhZQCCojXNNj) and in
[`canvas/`](canvas/README.md#piece-styles-and-ring-effects). A piece style is shown on the threat,
which has every piece and mark; a ring effect right after the big capture, which has three rings.

| Item | Notebook, light | Notebook, dark | Other theme, light | Other theme, dark |
| --- | --- | --- | --- | --- |
| Hearts and clubs (Neon arcade) | <img src="canvas/cosmetic-hearts-and-clubs-notebook-light.png" width="140" alt="Hearts and clubs, Notebook, light"> | <img src="canvas/cosmetic-hearts-and-clubs-notebook-dark.png" width="140" alt="Hearts and clubs, Notebook, dark"> | <img src="canvas/cosmetic-hearts-and-clubs-neon-arcade-light.png" width="140" alt="Hearts and clubs, Neon arcade, light"> | <img src="canvas/cosmetic-hearts-and-clubs-neon-arcade-dark.png" width="140" alt="Hearts and clubs, Neon arcade, dark"> |
| Flame and frost (Star chart) | <img src="canvas/cosmetic-flame-and-frost-notebook-light.png" width="140" alt="Flame and frost, Notebook, light"> | <img src="canvas/cosmetic-flame-and-frost-notebook-dark.png" width="140" alt="Flame and frost, Notebook, dark"> | <img src="canvas/cosmetic-flame-and-frost-star-chart-light.png" width="140" alt="Flame and frost, Star chart, light"> | <img src="canvas/cosmetic-flame-and-frost-star-chart-dark.png" width="140" alt="Flame and frost, Star chart, dark"> |
| Lasso (Star chart) | <img src="canvas/cosmetic-lasso-notebook-light.png" width="140" alt="Lasso, Notebook, light"> | <img src="canvas/cosmetic-lasso-notebook-dark.png" width="140" alt="Lasso, Notebook, dark"> | <img src="canvas/cosmetic-lasso-star-chart-light.png" width="140" alt="Lasso, Star chart, light"> | <img src="canvas/cosmetic-lasso-star-chart-dark.png" width="140" alt="Lasso, Star chart, dark"> |
| Rubber stamp (Neon arcade) | <img src="canvas/cosmetic-rubber-stamp-notebook-light.png" width="140" alt="Rubber stamp, Notebook, light"> | <img src="canvas/cosmetic-rubber-stamp-notebook-dark.png" width="140" alt="Rubber stamp, Notebook, dark"> | <img src="canvas/cosmetic-rubber-stamp-neon-arcade-light.png" width="140" alt="Rubber stamp, Neon arcade, light"> | <img src="canvas/cosmetic-rubber-stamp-neon-arcade-dark.png" width="140" alt="Rubber stamp, Neon arcade, dark"> |

## Contrast

A piece style and a ring effect use only the theme's colours, in the pairs the theme already
checks, so every figure on the theme pages holds:

- **Pieces and marks:** the piece, pinhole and cut rows of each theme's contrast table.
- **Lasso:** the cord's edge is the captor's ink on Paper, the area's wash or a hatch line, as the
  theme's ring. Its twists, pale on ink, are texture inside the cord and need no target.
- **Rubber stamp:** the inner line is the captor's ink on its own wash and hatching. Its weakest
  pair in each theme, against a hatch line: Notebook 3.26 light (Red ink on Red pale) and 3.35
  dark (Blue ink on Blue pale); Star chart 3.50 light (Gold ink on Gold pale) and 3.61 dark (Ice
  ink on Ice pale); Neon arcade 3.40 light and 3.33 dark (Magenta ink on Magenta glow). All meet
  3:1.

## Colour-blindness check

The same check as [Piece shapes](pieces.md#colour-blindness-check): the same mid-game 15x15 board at
**4 mm per point** (25.2 dp spacing on a 420 dpi phone, 992 px wide), in every theme, light and
dark, with the usual and with high-contrast pieces. Each board is shown as seen, in greyscale, and
with protanopia, deuteranopia and tritanopia (Machado, Oliveira and Fernandes, 2009, at full
strength). Each piece style is drawn with one ring effect, so every item is checked in every theme:
Hearts and clubs with Lasso, Flame and frost with Rubber stamp.

**Result.** In every image the two shapes separate the sides at a glance, and the pinhole, the
cuts, the threat rings, the closing point and the preview show. That includes the views where the
two inks are almost the same grey: Neon arcade light in greyscale and with tritanopia (1.07), and
Notebook light with protanopia (1.00). Lasso's rope and Rubber stamp's inner line show as plainly as
the theme's ring in every view.

**On a phone.** Open each board image on the phone, fitted to the screen width, so a 15x15 board
has about 4 mm between points. Check that the two shapes, the pinhole and the cuts are easy to tell
apart at arm's length, in every image.

**Hearts and clubs, with Lasso**

| | As seen | Greyscale | Protanopia | Deuteranopia | Tritanopia |
| --- | --- | --- | --- | --- | --- |
| Notebook, usual, light | <img src="pieces/cosmetics-board-cards-notebook-light-usual-seen.png" width="120" alt="Hearts and clubs in Notebook, light, usual, as seen"> | <img src="pieces/cosmetics-board-cards-notebook-light-usual-greyscale.png" width="120" alt="Hearts and clubs in Notebook, light, usual, in greyscale"> | <img src="pieces/cosmetics-board-cards-notebook-light-usual-protanopia.png" width="120" alt="Hearts and clubs in Notebook, light, usual, with protanopia"> | <img src="pieces/cosmetics-board-cards-notebook-light-usual-deuteranopia.png" width="120" alt="Hearts and clubs in Notebook, light, usual, with deuteranopia"> | <img src="pieces/cosmetics-board-cards-notebook-light-usual-tritanopia.png" width="120" alt="Hearts and clubs in Notebook, light, usual, with tritanopia"> |
| Notebook, usual, dark | <img src="pieces/cosmetics-board-cards-notebook-dark-usual-seen.png" width="120" alt="Hearts and clubs in Notebook, dark, usual, as seen"> | <img src="pieces/cosmetics-board-cards-notebook-dark-usual-greyscale.png" width="120" alt="Hearts and clubs in Notebook, dark, usual, in greyscale"> | <img src="pieces/cosmetics-board-cards-notebook-dark-usual-protanopia.png" width="120" alt="Hearts and clubs in Notebook, dark, usual, with protanopia"> | <img src="pieces/cosmetics-board-cards-notebook-dark-usual-deuteranopia.png" width="120" alt="Hearts and clubs in Notebook, dark, usual, with deuteranopia"> | <img src="pieces/cosmetics-board-cards-notebook-dark-usual-tritanopia.png" width="120" alt="Hearts and clubs in Notebook, dark, usual, with tritanopia"> |
| Notebook, high contrast, light | <img src="pieces/cosmetics-board-cards-notebook-light-contrast-seen.png" width="120" alt="Hearts and clubs in Notebook, light, high contrast, as seen"> | <img src="pieces/cosmetics-board-cards-notebook-light-contrast-greyscale.png" width="120" alt="Hearts and clubs in Notebook, light, high contrast, in greyscale"> | <img src="pieces/cosmetics-board-cards-notebook-light-contrast-protanopia.png" width="120" alt="Hearts and clubs in Notebook, light, high contrast, with protanopia"> | <img src="pieces/cosmetics-board-cards-notebook-light-contrast-deuteranopia.png" width="120" alt="Hearts and clubs in Notebook, light, high contrast, with deuteranopia"> | <img src="pieces/cosmetics-board-cards-notebook-light-contrast-tritanopia.png" width="120" alt="Hearts and clubs in Notebook, light, high contrast, with tritanopia"> |
| Notebook, high contrast, dark | <img src="pieces/cosmetics-board-cards-notebook-dark-contrast-seen.png" width="120" alt="Hearts and clubs in Notebook, dark, high contrast, as seen"> | <img src="pieces/cosmetics-board-cards-notebook-dark-contrast-greyscale.png" width="120" alt="Hearts and clubs in Notebook, dark, high contrast, in greyscale"> | <img src="pieces/cosmetics-board-cards-notebook-dark-contrast-protanopia.png" width="120" alt="Hearts and clubs in Notebook, dark, high contrast, with protanopia"> | <img src="pieces/cosmetics-board-cards-notebook-dark-contrast-deuteranopia.png" width="120" alt="Hearts and clubs in Notebook, dark, high contrast, with deuteranopia"> | <img src="pieces/cosmetics-board-cards-notebook-dark-contrast-tritanopia.png" width="120" alt="Hearts and clubs in Notebook, dark, high contrast, with tritanopia"> |
| Star chart, usual, light | <img src="pieces/cosmetics-board-cards-star-chart-light-usual-seen.png" width="120" alt="Hearts and clubs in Star chart, light, usual, as seen"> | <img src="pieces/cosmetics-board-cards-star-chart-light-usual-greyscale.png" width="120" alt="Hearts and clubs in Star chart, light, usual, in greyscale"> | <img src="pieces/cosmetics-board-cards-star-chart-light-usual-protanopia.png" width="120" alt="Hearts and clubs in Star chart, light, usual, with protanopia"> | <img src="pieces/cosmetics-board-cards-star-chart-light-usual-deuteranopia.png" width="120" alt="Hearts and clubs in Star chart, light, usual, with deuteranopia"> | <img src="pieces/cosmetics-board-cards-star-chart-light-usual-tritanopia.png" width="120" alt="Hearts and clubs in Star chart, light, usual, with tritanopia"> |
| Star chart, usual, dark | <img src="pieces/cosmetics-board-cards-star-chart-dark-usual-seen.png" width="120" alt="Hearts and clubs in Star chart, dark, usual, as seen"> | <img src="pieces/cosmetics-board-cards-star-chart-dark-usual-greyscale.png" width="120" alt="Hearts and clubs in Star chart, dark, usual, in greyscale"> | <img src="pieces/cosmetics-board-cards-star-chart-dark-usual-protanopia.png" width="120" alt="Hearts and clubs in Star chart, dark, usual, with protanopia"> | <img src="pieces/cosmetics-board-cards-star-chart-dark-usual-deuteranopia.png" width="120" alt="Hearts and clubs in Star chart, dark, usual, with deuteranopia"> | <img src="pieces/cosmetics-board-cards-star-chart-dark-usual-tritanopia.png" width="120" alt="Hearts and clubs in Star chart, dark, usual, with tritanopia"> |
| Star chart, high contrast, light | <img src="pieces/cosmetics-board-cards-star-chart-light-contrast-seen.png" width="120" alt="Hearts and clubs in Star chart, light, high contrast, as seen"> | <img src="pieces/cosmetics-board-cards-star-chart-light-contrast-greyscale.png" width="120" alt="Hearts and clubs in Star chart, light, high contrast, in greyscale"> | <img src="pieces/cosmetics-board-cards-star-chart-light-contrast-protanopia.png" width="120" alt="Hearts and clubs in Star chart, light, high contrast, with protanopia"> | <img src="pieces/cosmetics-board-cards-star-chart-light-contrast-deuteranopia.png" width="120" alt="Hearts and clubs in Star chart, light, high contrast, with deuteranopia"> | <img src="pieces/cosmetics-board-cards-star-chart-light-contrast-tritanopia.png" width="120" alt="Hearts and clubs in Star chart, light, high contrast, with tritanopia"> |
| Star chart, high contrast, dark | <img src="pieces/cosmetics-board-cards-star-chart-dark-contrast-seen.png" width="120" alt="Hearts and clubs in Star chart, dark, high contrast, as seen"> | <img src="pieces/cosmetics-board-cards-star-chart-dark-contrast-greyscale.png" width="120" alt="Hearts and clubs in Star chart, dark, high contrast, in greyscale"> | <img src="pieces/cosmetics-board-cards-star-chart-dark-contrast-protanopia.png" width="120" alt="Hearts and clubs in Star chart, dark, high contrast, with protanopia"> | <img src="pieces/cosmetics-board-cards-star-chart-dark-contrast-deuteranopia.png" width="120" alt="Hearts and clubs in Star chart, dark, high contrast, with deuteranopia"> | <img src="pieces/cosmetics-board-cards-star-chart-dark-contrast-tritanopia.png" width="120" alt="Hearts and clubs in Star chart, dark, high contrast, with tritanopia"> |
| Neon arcade, usual, light | <img src="pieces/cosmetics-board-cards-neon-arcade-light-usual-seen.png" width="120" alt="Hearts and clubs in Neon arcade, light, usual, as seen"> | <img src="pieces/cosmetics-board-cards-neon-arcade-light-usual-greyscale.png" width="120" alt="Hearts and clubs in Neon arcade, light, usual, in greyscale"> | <img src="pieces/cosmetics-board-cards-neon-arcade-light-usual-protanopia.png" width="120" alt="Hearts and clubs in Neon arcade, light, usual, with protanopia"> | <img src="pieces/cosmetics-board-cards-neon-arcade-light-usual-deuteranopia.png" width="120" alt="Hearts and clubs in Neon arcade, light, usual, with deuteranopia"> | <img src="pieces/cosmetics-board-cards-neon-arcade-light-usual-tritanopia.png" width="120" alt="Hearts and clubs in Neon arcade, light, usual, with tritanopia"> |
| Neon arcade, usual, dark | <img src="pieces/cosmetics-board-cards-neon-arcade-dark-usual-seen.png" width="120" alt="Hearts and clubs in Neon arcade, dark, usual, as seen"> | <img src="pieces/cosmetics-board-cards-neon-arcade-dark-usual-greyscale.png" width="120" alt="Hearts and clubs in Neon arcade, dark, usual, in greyscale"> | <img src="pieces/cosmetics-board-cards-neon-arcade-dark-usual-protanopia.png" width="120" alt="Hearts and clubs in Neon arcade, dark, usual, with protanopia"> | <img src="pieces/cosmetics-board-cards-neon-arcade-dark-usual-deuteranopia.png" width="120" alt="Hearts and clubs in Neon arcade, dark, usual, with deuteranopia"> | <img src="pieces/cosmetics-board-cards-neon-arcade-dark-usual-tritanopia.png" width="120" alt="Hearts and clubs in Neon arcade, dark, usual, with tritanopia"> |
| Neon arcade, high contrast, light | <img src="pieces/cosmetics-board-cards-neon-arcade-light-contrast-seen.png" width="120" alt="Hearts and clubs in Neon arcade, light, high contrast, as seen"> | <img src="pieces/cosmetics-board-cards-neon-arcade-light-contrast-greyscale.png" width="120" alt="Hearts and clubs in Neon arcade, light, high contrast, in greyscale"> | <img src="pieces/cosmetics-board-cards-neon-arcade-light-contrast-protanopia.png" width="120" alt="Hearts and clubs in Neon arcade, light, high contrast, with protanopia"> | <img src="pieces/cosmetics-board-cards-neon-arcade-light-contrast-deuteranopia.png" width="120" alt="Hearts and clubs in Neon arcade, light, high contrast, with deuteranopia"> | <img src="pieces/cosmetics-board-cards-neon-arcade-light-contrast-tritanopia.png" width="120" alt="Hearts and clubs in Neon arcade, light, high contrast, with tritanopia"> |
| Neon arcade, high contrast, dark | <img src="pieces/cosmetics-board-cards-neon-arcade-dark-contrast-seen.png" width="120" alt="Hearts and clubs in Neon arcade, dark, high contrast, as seen"> | <img src="pieces/cosmetics-board-cards-neon-arcade-dark-contrast-greyscale.png" width="120" alt="Hearts and clubs in Neon arcade, dark, high contrast, in greyscale"> | <img src="pieces/cosmetics-board-cards-neon-arcade-dark-contrast-protanopia.png" width="120" alt="Hearts and clubs in Neon arcade, dark, high contrast, with protanopia"> | <img src="pieces/cosmetics-board-cards-neon-arcade-dark-contrast-deuteranopia.png" width="120" alt="Hearts and clubs in Neon arcade, dark, high contrast, with deuteranopia"> | <img src="pieces/cosmetics-board-cards-neon-arcade-dark-contrast-tritanopia.png" width="120" alt="Hearts and clubs in Neon arcade, dark, high contrast, with tritanopia"> |

**Flame and frost, with Rubber stamp**

| | As seen | Greyscale | Protanopia | Deuteranopia | Tritanopia |
| --- | --- | --- | --- | --- | --- |
| Notebook, usual, light | <img src="pieces/cosmetics-board-fire-notebook-light-usual-seen.png" width="120" alt="Flame and frost in Notebook, light, usual, as seen"> | <img src="pieces/cosmetics-board-fire-notebook-light-usual-greyscale.png" width="120" alt="Flame and frost in Notebook, light, usual, in greyscale"> | <img src="pieces/cosmetics-board-fire-notebook-light-usual-protanopia.png" width="120" alt="Flame and frost in Notebook, light, usual, with protanopia"> | <img src="pieces/cosmetics-board-fire-notebook-light-usual-deuteranopia.png" width="120" alt="Flame and frost in Notebook, light, usual, with deuteranopia"> | <img src="pieces/cosmetics-board-fire-notebook-light-usual-tritanopia.png" width="120" alt="Flame and frost in Notebook, light, usual, with tritanopia"> |
| Notebook, usual, dark | <img src="pieces/cosmetics-board-fire-notebook-dark-usual-seen.png" width="120" alt="Flame and frost in Notebook, dark, usual, as seen"> | <img src="pieces/cosmetics-board-fire-notebook-dark-usual-greyscale.png" width="120" alt="Flame and frost in Notebook, dark, usual, in greyscale"> | <img src="pieces/cosmetics-board-fire-notebook-dark-usual-protanopia.png" width="120" alt="Flame and frost in Notebook, dark, usual, with protanopia"> | <img src="pieces/cosmetics-board-fire-notebook-dark-usual-deuteranopia.png" width="120" alt="Flame and frost in Notebook, dark, usual, with deuteranopia"> | <img src="pieces/cosmetics-board-fire-notebook-dark-usual-tritanopia.png" width="120" alt="Flame and frost in Notebook, dark, usual, with tritanopia"> |
| Notebook, high contrast, light | <img src="pieces/cosmetics-board-fire-notebook-light-contrast-seen.png" width="120" alt="Flame and frost in Notebook, light, high contrast, as seen"> | <img src="pieces/cosmetics-board-fire-notebook-light-contrast-greyscale.png" width="120" alt="Flame and frost in Notebook, light, high contrast, in greyscale"> | <img src="pieces/cosmetics-board-fire-notebook-light-contrast-protanopia.png" width="120" alt="Flame and frost in Notebook, light, high contrast, with protanopia"> | <img src="pieces/cosmetics-board-fire-notebook-light-contrast-deuteranopia.png" width="120" alt="Flame and frost in Notebook, light, high contrast, with deuteranopia"> | <img src="pieces/cosmetics-board-fire-notebook-light-contrast-tritanopia.png" width="120" alt="Flame and frost in Notebook, light, high contrast, with tritanopia"> |
| Notebook, high contrast, dark | <img src="pieces/cosmetics-board-fire-notebook-dark-contrast-seen.png" width="120" alt="Flame and frost in Notebook, dark, high contrast, as seen"> | <img src="pieces/cosmetics-board-fire-notebook-dark-contrast-greyscale.png" width="120" alt="Flame and frost in Notebook, dark, high contrast, in greyscale"> | <img src="pieces/cosmetics-board-fire-notebook-dark-contrast-protanopia.png" width="120" alt="Flame and frost in Notebook, dark, high contrast, with protanopia"> | <img src="pieces/cosmetics-board-fire-notebook-dark-contrast-deuteranopia.png" width="120" alt="Flame and frost in Notebook, dark, high contrast, with deuteranopia"> | <img src="pieces/cosmetics-board-fire-notebook-dark-contrast-tritanopia.png" width="120" alt="Flame and frost in Notebook, dark, high contrast, with tritanopia"> |
| Star chart, usual, light | <img src="pieces/cosmetics-board-fire-star-chart-light-usual-seen.png" width="120" alt="Flame and frost in Star chart, light, usual, as seen"> | <img src="pieces/cosmetics-board-fire-star-chart-light-usual-greyscale.png" width="120" alt="Flame and frost in Star chart, light, usual, in greyscale"> | <img src="pieces/cosmetics-board-fire-star-chart-light-usual-protanopia.png" width="120" alt="Flame and frost in Star chart, light, usual, with protanopia"> | <img src="pieces/cosmetics-board-fire-star-chart-light-usual-deuteranopia.png" width="120" alt="Flame and frost in Star chart, light, usual, with deuteranopia"> | <img src="pieces/cosmetics-board-fire-star-chart-light-usual-tritanopia.png" width="120" alt="Flame and frost in Star chart, light, usual, with tritanopia"> |
| Star chart, usual, dark | <img src="pieces/cosmetics-board-fire-star-chart-dark-usual-seen.png" width="120" alt="Flame and frost in Star chart, dark, usual, as seen"> | <img src="pieces/cosmetics-board-fire-star-chart-dark-usual-greyscale.png" width="120" alt="Flame and frost in Star chart, dark, usual, in greyscale"> | <img src="pieces/cosmetics-board-fire-star-chart-dark-usual-protanopia.png" width="120" alt="Flame and frost in Star chart, dark, usual, with protanopia"> | <img src="pieces/cosmetics-board-fire-star-chart-dark-usual-deuteranopia.png" width="120" alt="Flame and frost in Star chart, dark, usual, with deuteranopia"> | <img src="pieces/cosmetics-board-fire-star-chart-dark-usual-tritanopia.png" width="120" alt="Flame and frost in Star chart, dark, usual, with tritanopia"> |
| Star chart, high contrast, light | <img src="pieces/cosmetics-board-fire-star-chart-light-contrast-seen.png" width="120" alt="Flame and frost in Star chart, light, high contrast, as seen"> | <img src="pieces/cosmetics-board-fire-star-chart-light-contrast-greyscale.png" width="120" alt="Flame and frost in Star chart, light, high contrast, in greyscale"> | <img src="pieces/cosmetics-board-fire-star-chart-light-contrast-protanopia.png" width="120" alt="Flame and frost in Star chart, light, high contrast, with protanopia"> | <img src="pieces/cosmetics-board-fire-star-chart-light-contrast-deuteranopia.png" width="120" alt="Flame and frost in Star chart, light, high contrast, with deuteranopia"> | <img src="pieces/cosmetics-board-fire-star-chart-light-contrast-tritanopia.png" width="120" alt="Flame and frost in Star chart, light, high contrast, with tritanopia"> |
| Star chart, high contrast, dark | <img src="pieces/cosmetics-board-fire-star-chart-dark-contrast-seen.png" width="120" alt="Flame and frost in Star chart, dark, high contrast, as seen"> | <img src="pieces/cosmetics-board-fire-star-chart-dark-contrast-greyscale.png" width="120" alt="Flame and frost in Star chart, dark, high contrast, in greyscale"> | <img src="pieces/cosmetics-board-fire-star-chart-dark-contrast-protanopia.png" width="120" alt="Flame and frost in Star chart, dark, high contrast, with protanopia"> | <img src="pieces/cosmetics-board-fire-star-chart-dark-contrast-deuteranopia.png" width="120" alt="Flame and frost in Star chart, dark, high contrast, with deuteranopia"> | <img src="pieces/cosmetics-board-fire-star-chart-dark-contrast-tritanopia.png" width="120" alt="Flame and frost in Star chart, dark, high contrast, with tritanopia"> |
| Neon arcade, usual, light | <img src="pieces/cosmetics-board-fire-neon-arcade-light-usual-seen.png" width="120" alt="Flame and frost in Neon arcade, light, usual, as seen"> | <img src="pieces/cosmetics-board-fire-neon-arcade-light-usual-greyscale.png" width="120" alt="Flame and frost in Neon arcade, light, usual, in greyscale"> | <img src="pieces/cosmetics-board-fire-neon-arcade-light-usual-protanopia.png" width="120" alt="Flame and frost in Neon arcade, light, usual, with protanopia"> | <img src="pieces/cosmetics-board-fire-neon-arcade-light-usual-deuteranopia.png" width="120" alt="Flame and frost in Neon arcade, light, usual, with deuteranopia"> | <img src="pieces/cosmetics-board-fire-neon-arcade-light-usual-tritanopia.png" width="120" alt="Flame and frost in Neon arcade, light, usual, with tritanopia"> |
| Neon arcade, usual, dark | <img src="pieces/cosmetics-board-fire-neon-arcade-dark-usual-seen.png" width="120" alt="Flame and frost in Neon arcade, dark, usual, as seen"> | <img src="pieces/cosmetics-board-fire-neon-arcade-dark-usual-greyscale.png" width="120" alt="Flame and frost in Neon arcade, dark, usual, in greyscale"> | <img src="pieces/cosmetics-board-fire-neon-arcade-dark-usual-protanopia.png" width="120" alt="Flame and frost in Neon arcade, dark, usual, with protanopia"> | <img src="pieces/cosmetics-board-fire-neon-arcade-dark-usual-deuteranopia.png" width="120" alt="Flame and frost in Neon arcade, dark, usual, with deuteranopia"> | <img src="pieces/cosmetics-board-fire-neon-arcade-dark-usual-tritanopia.png" width="120" alt="Flame and frost in Neon arcade, dark, usual, with tritanopia"> |
| Neon arcade, high contrast, light | <img src="pieces/cosmetics-board-fire-neon-arcade-light-contrast-seen.png" width="120" alt="Flame and frost in Neon arcade, light, high contrast, as seen"> | <img src="pieces/cosmetics-board-fire-neon-arcade-light-contrast-greyscale.png" width="120" alt="Flame and frost in Neon arcade, light, high contrast, in greyscale"> | <img src="pieces/cosmetics-board-fire-neon-arcade-light-contrast-protanopia.png" width="120" alt="Flame and frost in Neon arcade, light, high contrast, with protanopia"> | <img src="pieces/cosmetics-board-fire-neon-arcade-light-contrast-deuteranopia.png" width="120" alt="Flame and frost in Neon arcade, light, high contrast, with deuteranopia"> | <img src="pieces/cosmetics-board-fire-neon-arcade-light-contrast-tritanopia.png" width="120" alt="Flame and frost in Neon arcade, light, high contrast, with tritanopia"> |
| Neon arcade, high contrast, dark | <img src="pieces/cosmetics-board-fire-neon-arcade-dark-contrast-seen.png" width="120" alt="Flame and frost in Neon arcade, dark, high contrast, as seen"> | <img src="pieces/cosmetics-board-fire-neon-arcade-dark-contrast-greyscale.png" width="120" alt="Flame and frost in Neon arcade, dark, high contrast, in greyscale"> | <img src="pieces/cosmetics-board-fire-neon-arcade-dark-contrast-protanopia.png" width="120" alt="Flame and frost in Neon arcade, dark, high contrast, with protanopia"> | <img src="pieces/cosmetics-board-fire-neon-arcade-dark-contrast-deuteranopia.png" width="120" alt="Flame and frost in Neon arcade, dark, high contrast, with deuteranopia"> | <img src="pieces/cosmetics-board-fire-neon-arcade-dark-contrast-tritanopia.png" width="120" alt="Flame and frost in Neon arcade, dark, high contrast, with tritanopia"> |

Open an image to see it at full size.

## Redrawing

[`canvas/draw.py`](canvas/draw.py) holds this page's shapes and ring effects (`PIECE_STYLES`,
`SHAPES`, `rope`, `STAMP_GAP`) and draws the canvas frames:

```sh
python3 docs/design/canvas/draw.py --only cosmetics     # the frames on this page
python3 docs/design/pieces/cosmetics.py                 # the sheets, strips and check boards
python3 docs/design/pieces/cosmetics.py measure         # each shape's size, area and reach
```

[`pieces/cosmetics.py`](pieces/cosmetics.py) draws the sheets, the key-frame strips and the check
boards from the same code. Run both from the repository root. The canvas script needs Google
Chrome and a network connection for the fonts; the other needs `rsvg-convert` (from librsvg) and
ffmpeg.
