# Star chart theme

Star chart is a cosmetic theme (decision 0028,
[`docs/decisions/0028-worlds-and-themes.md`](../decisions/0028-worlds-and-themes.md)). One side
places gold stars, the other ice-blue comets. Rings are straight constellation lines, and
captured areas are a faint haze, hatched the way old star atlases engrave the Milky Way. This page
gives every value the app needs to draw it, in a light and a dark version. The
[Star chart mood board](moodboards/star-chart.md) shows the feel. This page gives the exact values.
It follows the [Notebook theme](theme-notebook.md) and [Piece shapes](pieces.md) section by
section. Where it says "as Notebook", the value on those pages holds unchanged.

<img src="theme-star-chart.svg" alt="A board sample in the light and the dark version: gold stars and ice-blue comets, two captured areas with straight rings, hazes and hatching, a threat, a closing point and a preview" width="720">

The sample above is drawn from the values on this page, at 2 px per dp. It shows a gold capture
around two comets, an ice capture around one star, a comet one move from capture (solid ring)
and one two moves away (faint ring), the point that would close the ring (small gold circle), the
last move (the star with the pinhole), and the comet Ice is about to place (bottom left).

## Rules of the theme

- **A printed chart, not a photo of the sky.** Lines are straight and crisp, every colour is solid,
  and nothing glows. The mood board's glows look soft in one picture, but on a crowded 15x15 board
  (mood board picture 3) they merge and blur the pieces. A glow is also see-through, which
  changes the contrast of everything under it. The night comes from the colours: light inks on a
  deep sky.
- **Gold is Red's side, Ice is Blue's.** Wherever the game and the code say "red" and "blue", Star
  chart draws gold and ice. The player is gold in the campaign; the computer is ice.
- **The board stays calm.** The grid and the hazes are much paler than the inks, as in Notebook.
- **Three weights,** as Notebook: thin grid lines, rings twice as thick, solid pieces.
- **Never colour alone.** Stars and comets differ by shape. Gold areas are hatched one way and ice
  areas the other.
- **Light or dark follows the phone,** as Notebook. The light version is a printed star atlas:
  pale chart paper, brass-gold stars and deep-blue comets. The dark version is the night sky of the
  mood board.
- **No texture on the board.** No background stars, nebulae or pictures under the points. The
  chart points at the crossings are part of the grid.

## Colours

| Name | Light | Dark | In the code | Used for |
| --- | --- | --- | --- | --- |
| Paper | `#F3F5FA` | `#0F1733` | `paper` | The board and the game screen's background |
| Card | `#FFFFFF` | `#1A2448` | `card` | Notes, the opponent's photo and the speech bubble on the game screen |
| Grid | `#D2DAEA` | `#26315C` | `grid` | Grid lines on the board |
| Chart point | `#8F9CBF` | `#4A5A8E` | new | The small point at each grid crossing |
| Gold ink | `#9A5B00` | `#F6CB6E` | `red` | Gold stars and rings; the gold side's name and score; booster counts |
| Ice ink | `#0E5C85` | `#6DBAE6` | `blue` | Ice comets and rings; the ice side's name and score; main buttons |
| Gold haze | `#F1E7D9` | `#2C2A3F` | `redWash` | Fill of an area gold has captured |
| Ice haze | `#DCE7F0` | `#152A4C` | `blueWash` | Fill of an area ice has captured |
| Gold pale | `#E2CDAD` | `#6B5B45` | `redPale` | Hatching in gold areas; links between stars; the gold preview; tape |
| Ice pale | `#B5CDE0` | `#2D5678` | `bluePale` | Hatching in ice areas; links between comets; the ice preview |
| Nova | `#B8326E` | `#FF8DB5` | `highlighter` | Ring around a group one move from capture; error text |
| Nova faint | `#E6B3C9` | `#6A3559` | `highlighterFaint` | Ring around a group two moves from capture |
| Starlight | `#1A2240` | `#E6EAF7` | `pencil` | Main text and icons |
| Dust | `#525C78` | `#9CA8CA` | `graphite` | Secondary text, hints, outlines of secondary buttons |

Every colour is solid, as in Notebook. The mood board proposed Sky `#0F1733` and the hazes
`#3B3546` and `#1F3550`; the dark Paper keeps its sky, and the hazes are paler here so the pieces
inside them keep 3:1.

**Why Nova.** Notebook's threat colour, Highlighter, is an amber that would read as gold here. Nova
is a rose that belongs to neither side, so a threat ring is never mistaken for a ring of one side.
Errors take Nova too: in Notebook they share Red ink, but a gold error looks like praise.

**Notebook colours this theme doesn't use.** Margin: the game screen has no margin line, and the
menu screens stay Notebook's (see [Outside the game screen](#outside-the-game-screen)); the code
can set it to Grid. The tape is Gold pale. The scrim behind Pause is Notebook's: black at 40% in
the light version, 55% in the dark.

## The board

Sizes depend on the **spacing**, as Notebook: 24 dp on a 360 dp wide phone for a 15-point-wide
board.

| Element | Size at 24 dp spacing | Other spacings | Colour |
| --- | --- | --- | --- |
| Grid line | 1 dp | always 1 dp | Grid |
| Board edge (outermost grid lines) | 1.5 dp | always 1.5 dp | Grid |
| Chart point (at every crossing) | 2 dp across | always 2 dp | Chart point |
| Dot size | 12 dp | half the spacing, at least 9 dp, at most 20 dp | (sets the pieces' sizes) |
| Gold star | 16.8 dp across its points | 1.4 dot sizes | Gold ink |
| Ice comet | head 9 dp across; tail to 16.8 dp from the head's centre | see [The pieces](#the-pieces) | Ice ink |
| Ring (constellation line) | 2 dp | 1/12 of the spacing, at least 1.5 dp, at most 3 dp | Ink of the side that captured |
| Hatch line | 1 dp, one line every 6 dp | always the same | Gold pale or Ice pale |
| Link between two pieces of one side (only when shown) | 1.5 dp | always 1.5 dp | Gold pale or Ice pale |
| Threat ring, one move from capture | 20 dp across, 2 dp line | 0.84 of the spacing across | Nova |
| Threat ring, two moves from capture | 20 dp across, 1.5 dp line | 0.84 of the spacing across | Nova faint |
| Closing point | 8 dp across, 1.5 dp line | a third of the spacing across | Ink of the attacker |
| Preview | the moving side's piece inside a 22 dp ring, 2 dp line | piece as above; ring 0.9 of the spacing across | Piece: Gold pale or Ice pale; ring: ink of the side to move |
| Drag's guides | 1.5 dp line, dashes 4 dp long with 3 dp gaps | always the same | Ink of the side to move |

Paper fills the board and half a spacing around its outermost points, and the screen around it,
as Notebook.

**Chart points** make the grid read as a star chart and show the empty points to aim at. They
are 2.5:1 against Paper, darker than the grid lines but far paler than the pieces, and at 2 dp
they are a sixth of a piece across, so they are never taken for one. A piece covers the point it
stands on.

**Drawing order**, from the back: paper, hazes, hatching, grid lines, chart points, links, drag's
guides, the bands round the pieces (high contrast only), rings, threat rings, pieces, last-move
pinhole and captured cuts, closing points, preview. It is Notebook's order with the chart points
added after the grid.

**Links**, **drag's guides** and **captured pieces** work as Notebook: links only where Notebook
draws them, guides as four dashed arms from the preview ring to the board's edge, and captured
pieces in full ink with a cut.

**The comet's tail and the rings.** At 24 dp spacing the tail reaches 16.8 dp from the point, past
the threat ring (10.1 dp) and the preview ring (10.8 dp). The tail lies on top of a threat ring,
since pieces are drawn after threat rings, and the pale tail of the preview pokes out of its ring.
Both were checked on the sample and the check boards: the ring still reads as a ring round the
head.

## The pieces

<img src="pieces/star-chart-pieces-4x.svg" alt="The pieces in the light and the dark version: gold star, ice comet, each with the last-move pinhole, each captured with a cut, and each as a preview" width="768">

From left to right: gold piece, ice piece, gold and ice last move, a captured star (in an ice
area), a captured comet (in a gold area), gold and ice preview. Top row light, bottom row dark.
Each cell is one 24 dp grid square. The same sheet at 1 px per dp is
[`pieces/star-chart-pieces-1x.svg`](pieces/star-chart-pieces-1x.svg).

Sizes follow the **dot size** (12 dp at 24 dp spacing). "Along" is along the comet's tail from the
head's centre; "across" is to either side of the tail's line.

| Element | Shape | Size at 24 dp spacing | In dot sizes | Colour |
| --- | --- | --- | --- | --- |
| Gold piece | Five-pointed star, one point straight up, centred on the point, sharp points | 16.8 dp across its points; inner corners 4.6 dp from the centre | 1.4 across; inner corners at 0.55 of the points' radius | Gold ink |
| Ice piece: head | A round dot centred on the point | 9 dp across | 0.75 | Ice ink |
| Ice piece: tail | Three streaks running up and to the left, at 45°, one shape with the head: it leaves the head at 70° either side of its line, then runs to a side streak's tip, a notch, the middle streak's tip, a notch and the other side streak's tip | middle tip 16.8 dp along; side tips 12.6 dp along and 3.6 dp across; notches 7.8 dp along and 1.3 dp across | middle tip 1.4 along; side tips 1.05 along, 0.3 across; notches 0.65 along, 0.11 across | Ice ink |
| Last move | Pinhole in the middle of the newest piece (the comet's head) | 3.6 dp across | 0.3 | Paper |
| Captured piece | Cut: a straight band through the middle of the piece | 2 dp wide, 18 dp long | 1/6 wide, 1.5 long | Haze of the side that captured it |
| Preview | The moving side's shape, pale, inside the preview ring | as above | as above | Gold pale or Ice pale; ring in the ink of the side to move |

**The cut runs along the captor's hatching,** as Notebook. A captured star lies in an ice area,
hatched falling (`\`), so its cut falls. A captured comet lies in a gold area, hatched rising
(`/`), so its cut rises and crosses the tail instead of running along it.

**Why these shapes.**

- **A star and a comet, both solid.** They are the pieces the mood board was built on, and they
  differ in outline at any size: five points against a round head with a tail. A hollow piece
  would look like a threat ring or the preview ring, as in Notebook.
- **Equal weight.** The star covers 0.79 square dot sizes and the comet 0.84, about the area of
  Notebook's round dot (0.785), so neither side looks heavier. A star as wide as the dot covers
  half as much, so the star is 1.4 dot sizes across.
- **A fat star.** Its inner corners sit at 0.55 of the points' radius (a classic star uses 0.38),
  so the arms stay solid at 9 dp and the pinhole fits inside.
- **Three streaks, not a smooth tail.** A smooth tapering tail made the comet look like a
  raindrop or a fin on the test boards. The notched tail reads as a comet at 9 dp too.
- **The tail points up and to the left, always.** On a diagonal it never lies along a grid line,
  a guide or a straight side of a ring. Up and to the left, not up and to the right, so that a
  captured comet's cut crosses the tail.
- **A pinhole for the last move, not a ring of light.** The mood board marks the last move with a
  ring, but a ring means a threat on this board, as in Notebook.

### High contrast

As Notebook ([Piece shapes](pieces.md#high-contrast)): the dot size becomes 0.6 of the spacing (at
least 9 dp, at most 24 dp), so the star is 20.2 dp across and the comet's head 10.8 dp at 24 dp
spacing. Each piece sits in a 1.5 dp band of the Paper or haze under it, following its outline
with round corners. The preview's pale piece is outlined in the ink of the side to move, 1.5 dp,
and drag's guides become 2 dp dashes on a 4 dp strip of the surface under them.

At 24 dp spacing two stars side by side are then 4.8 dp apart, and 1.8 dp once their bands are
drawn, so the bands never join.

With the bands, a piece only meets Paper or a haze. Its weakest pair is Gold ink on Ice haze in the
light version (4.32) and Ice ink on Gold haze in the dark (6.50).

## The ring

The ring is a constellation line: it runs through the centres of the capturing pieces, in the ink
of the side that captured, with round ends and round corners. Unlike Notebook's pen stroke, it is
**straight** and of **even width**: no bow and no change of thickness. A printed chart draws its
lines with a ruler.

## Captured areas

Two layers inside the ring, as Notebook:

1. **Haze:** a flat fill in Gold haze or Ice haze.
2. **Hatching:** straight lines in Gold pale or Ice pale, 1 dp thick, one every 6 dp, clipped to the
   ring. Gold areas are hatched rising to the right (`/`, 45°), ice areas falling to the right
   (`\`, 135°).

The hatching is a little sparser than Notebook's (6 dp, not 5 dp), so a haze reads as a soft
patch, not as a striped one.

**On a crowded 15x15 board** (mood board picture 3) the areas stay readable because the hazes are
quiet (1.12 to 1.26 against Paper), the two hazes lean opposite ways, and every ring is a crisp
line in full ink. The check boards below show a mid-game board at phone size.

## What follows the theme on the game screen

Every part of the game screen takes this theme's colours by the same roles as Notebook: what is
Paper, Pencil, Red ink or Blue ink there is Paper, Starlight, Gold ink or Ice ink here. **The
shapes stay Notebook's:** notes are still slightly turned, with tape and a soft shadow; the tool
bar keeps its hand-drawn corners; the fonts stay. Only the board itself changes shape. That keeps
the game one notebook, as decision 0028 says, with the chart as the page in it.

| Part | In Star chart |
| --- | --- |
| The screen behind the board | Paper, with no grid outside the board. |
| Top bar: pause icon, level label | Starlight and Dust on Paper. |
| The score rows | Each side's name, tally and score in its ink, with its shape beside the name: a star for gold, a comet for ice, at the dot size used there. The pen stroke under the side to move is in its ink. "Dots left" in Starlight and Dust. |
| The tool bar | Booster buttons as Notebook: icons in Ice ink, counts in Paper on Gold ink, empty boosters dashed in Dust. |
| The notes: mission, level opening, Pause, booster preview, Undo, tutorial | Card, with Gold pale tape. Ticks in the mission note in Gold ink. Main buttons Paper on Ice ink. |
| The result note and the star reveal, the daily result | Card. Headlines in Gold ink where Notebook uses Red ink (a win), in Ice ink where it uses Blue ink (a loss). Earned stars in Gold ink, stars not earned in Dust outline. |
| Two players' turn banner | The side's ink. |
| The opponent's photo and speech bubble | Card, with Gold pale tape; the bubble's outline and words in Ice ink. |
| The computer's faces and the World 1 boss | Recoloured by a script, not redrawn: see below. |
| Board touches | Drawn in code in the theme's pale inks, so Gold pale and Ice pale, as on every theme. World 3's star and comet doodles are outlines, beside the opponent, never next to the board, so they are not taken for pieces. |

### The faces

The faces are Notebook ink pictures in four inks and white (see
[Illustrations](illustrations.md)). Star chart uses the same pictures with their inks swapped, the
way `make_pictures.py` makes the dark version. **No new picture is needed.** The computer is the
same character in every theme; only its inks follow the board, so it sits on the chart without
looking pasted in.

| Ink in the picture | Light version | Dark version |
| --- | --- | --- |
| Red ink `#C0392B` | Gold ink `#9A5B00` | Gold ink `#F6CB6E` |
| Blue ink `#1F4E9C` | Ice ink `#0E5C85` | Ice ink `#6DBAE6` |
| Red pale `#ECBEB7` | Gold pale `#E2CDAD` | Gold pale `#6B5B45` |
| Blue pale `#BCCBEA` | Ice pale `#B5CDE0` | Ice pale `#2D5678` |
| White | White | White |

Each recoloured picture is saved beside the Notebook one as `<sheet>_<name>_star_chart.png` and
`<sheet>_<name>_star_chart_dark.png` (for example `opponent_moods_happy_star_chart.png`), for every
picture shown on the game screen: the six moods, the three strengths and the three World 1 boss
faces. The faces of later bosses get the same two versions when they are made.

The lines inside a face (eyebrows, mouth, hatching) stay as clear as in Notebook: Ice pale on Ice
ink is 4.42 light and 3.61 dark, against Notebook's 4.89 and 3.35.

### Outside the game screen

The map pages stay Notebook's in every theme (decision 0028). So do the notes laid over the map
(the Level card with its small board, the Chest, Today's puzzle, Play a game) and the menu
screens, How to play among them. They are not the board and the screen behind it, which is what a
theme sets.

## Type

Star chart keeps Notebook's fonts and text styles: Caveat for big headings and Nunito for
everything else ([Notebook, Type](theme-notebook.md#type)).

- Decision 0028 doesn't list fonts as part of a theme, and the game stays one notebook in every
  theme.
- The mood board proposed Fraunces for headings. Fraunces has no Cyrillic letters, so it can't
  show Russian or Ukrainian (decision 0007).
- Inter would do for body text, but it brings a new font and nothing Nunito lacks.

## Interface colours

How the colours fill the Material colour roles, as Notebook:

| Material role | Colour |
| --- | --- |
| `background`, `surface` | Paper |
| `surfaceContainer` (and its lower and higher steps) | Card |
| `onBackground`, `onSurface` | Starlight |
| `onSurfaceVariant` | Dust |
| `primary` | Ice ink |
| `onPrimary` | Paper |
| `secondary` | Gold ink |
| `onSecondary` | Paper |
| `outline` | Dust |
| `outlineVariant` | Grid |
| `error` | Nova |
| `onError` | Paper |

## Contrast

Ratios use the WCAG 2 formula, cut (not rounded) to two decimals, as on the Notebook page.
Targets: text at least 4.5:1; pieces, rings and marks at least 3:1. "Quiet" pairs are meant to be
low, and have no target.

| Kind | Pair | Target | Light | Dark |
| --- | --- | --- | --- | --- |
| Text | Starlight on paper | 4.5 | 14.29 | 14.67 |
| Text | Dust on paper | 4.5 | 6.09 | 7.44 |
| Text | Starlight on card | 4.5 | 15.59 | 12.57 |
| Text | Dust on card | 4.5 | 6.64 | 6.38 |
| Text | Gold ink on paper | 4.5 | 4.97 | 11.49 |
| Text | Ice ink on paper | 4.5 | 6.66 | 8.24 |
| Text | Gold ink on card | 4.5 | 5.42 | 9.84 |
| Text | Ice ink on card | 4.5 | 7.27 | 7.06 |
| Text | Paper on ice ink (main button) | 4.5 | 6.66 | 8.24 |
| Text | Paper on gold ink (booster count) | 4.5 | 4.97 | 11.49 |
| Text | Nova on paper (error) | 4.5 | 5.17 | 8.17 |
| Text | Nova on card (error) | 4.5 | 5.64 | 7.00 |
| Text | Paper on nova | 4.5 | 5.17 | 8.17 |
| Piece | Gold ink on paper | 3 | 4.97 | 11.49 |
| Piece | Ice ink on paper | 3 | 6.66 | 8.24 |
| Piece | Gold ink on grid | 3 | 3.86 | 8.16 |
| Piece | Ice ink on grid | 3 | 5.17 | 5.85 |
| Piece | Gold ink on gold haze | 3 | 4.43 | 9.06 |
| Piece | Ice ink on gold haze | 3 | 5.94 | 6.50 |
| Piece | Gold ink on ice haze | 3 | 4.32 | 9.32 |
| Piece | Ice ink on ice haze | 3 | 5.79 | 6.68 |
| Piece | Gold ink on gold pale (hatch line) | 3 | 3.50 | 4.26 |
| Piece | Ice ink on gold pale (hatch line) | 3 | 4.69 | 3.06 |
| Piece | Gold ink on ice pale (hatch line) | 3 | 3.30 | 5.04 |
| Piece | Ice ink on ice pale (hatch line) | 3 | 4.42 | 3.61 |
| Mark | Nova on paper | 3 | 5.17 | 8.17 |
| Mark | Nova on gold haze | 3 | 4.61 | 6.44 |
| Mark | Nova on ice haze | 3 | 4.49 | 6.63 |
| Mark | Pinhole: paper on gold ink | 3 | 4.97 | 11.49 |
| Mark | Pinhole: paper on ice ink | 3 | 6.66 | 8.24 |
| Mark | Cut: ice haze on gold ink | 3 | 4.32 | 9.32 |
| Mark | Cut: gold haze on ice ink | 3 | 5.94 | 6.50 |
| Quiet | Grid on paper | – | 1.28 | 1.40 |
| Quiet | Chart point on paper | – | 2.50 | 2.64 |
| Quiet | Gold haze on paper | – | 1.12 | 1.26 |
| Quiet | Ice haze on paper | – | 1.15 | 1.23 |
| Quiet | Gold pale on gold haze | – | 1.26 | 2.12 |
| Quiet | Ice pale on ice haze | – | 1.30 | 1.84 |
| Quiet | Nova faint on paper | – | 1.65 | 1.90 |
| Quiet | Gold ink on ice ink | – | 1.34 | 1.39 |

Every pair with a target meets it, for the usual and the high-contrast board. The piece rows cover
every surface a piece can sit on: paper, card, a grid line, either haze and either hatch line. With
high-contrast pieces only the paper and haze rows apply. Drag's guides use the two inks, so their
figures are the piece rows. Chart points need no row: a piece covers its own.

As in Notebook, the two inks differ only a little in lightness (last row; gold is the lighter one
in both versions), and the shapes are what separates the sides.

## Colour-blindness check

The same check as [Piece shapes](pieces.md#colour-blindness-check), on the same mid-game 15x15
board at **4 mm per point** (25.2 dp spacing on a 420 dpi phone, 992 px wide), with Notebook's red
as gold and blue as ice. Each board is shown as seen, in greyscale, and with protanopia,
deuteranopia and tritanopia (Machado, Oliveira and Fernandes, 2009, at full strength).

| | As seen | Greyscale | Protanopia | Deuteranopia | Tritanopia |
| --- | --- | --- | --- | --- | --- |
| Usual, light | <img src="pieces/star-chart-board-light-usual-seen.png" width="140" alt="Light board as seen"> | <img src="pieces/star-chart-board-light-usual-greyscale.png" width="140" alt="Light board in greyscale"> | <img src="pieces/star-chart-board-light-usual-protanopia.png" width="140" alt="Light board with protanopia"> | <img src="pieces/star-chart-board-light-usual-deuteranopia.png" width="140" alt="Light board with deuteranopia"> | <img src="pieces/star-chart-board-light-usual-tritanopia.png" width="140" alt="Light board with tritanopia"> |
| Usual, dark | <img src="pieces/star-chart-board-dark-usual-seen.png" width="140" alt="Dark board as seen"> | <img src="pieces/star-chart-board-dark-usual-greyscale.png" width="140" alt="Dark board in greyscale"> | <img src="pieces/star-chart-board-dark-usual-protanopia.png" width="140" alt="Dark board with protanopia"> | <img src="pieces/star-chart-board-dark-usual-deuteranopia.png" width="140" alt="Dark board with deuteranopia"> | <img src="pieces/star-chart-board-dark-usual-tritanopia.png" width="140" alt="Dark board with tritanopia"> |
| High contrast, light | <img src="pieces/star-chart-board-light-contrast-seen.png" width="140" alt="Light board with high-contrast pieces as seen"> | <img src="pieces/star-chart-board-light-contrast-greyscale.png" width="140" alt="Light board with high-contrast pieces in greyscale"> | <img src="pieces/star-chart-board-light-contrast-protanopia.png" width="140" alt="Light board with high-contrast pieces with protanopia"> | <img src="pieces/star-chart-board-light-contrast-deuteranopia.png" width="140" alt="Light board with high-contrast pieces with deuteranopia"> | <img src="pieces/star-chart-board-light-contrast-tritanopia.png" width="140" alt="Light board with high-contrast pieces with tritanopia"> |
| High contrast, dark | <img src="pieces/star-chart-board-dark-contrast-seen.png" width="140" alt="Dark board with high-contrast pieces as seen"> | <img src="pieces/star-chart-board-dark-contrast-greyscale.png" width="140" alt="Dark board with high-contrast pieces in greyscale"> | <img src="pieces/star-chart-board-dark-contrast-protanopia.png" width="140" alt="Dark board with high-contrast pieces with protanopia"> | <img src="pieces/star-chart-board-dark-contrast-deuteranopia.png" width="140" alt="Dark board with high-contrast pieces with deuteranopia"> | <img src="pieces/star-chart-board-dark-contrast-tritanopia.png" width="140" alt="Dark board with high-contrast pieces with tritanopia"> |

Open an image to see it at full size.

**Colour alone is not enough.** The contrast between the two inks in each view:

| View | Light | Dark |
| --- | --- | --- |
| As seen | 1.34 | 1.39 |
| Greyscale | 1.34 | 1.39 |
| Protanopia | 1.06 | 1.19 |
| Deuteranopia | 1.54 | 1.53 |
| Tritanopia | 1.22 | 1.32 |

In the three colour-blind views the inks keep different hues (gold turns olive in protanopia and
deuteranopia, red-pink in tritanopia). In greyscale only the small lightness difference is left,
and in protanopia the two inks have nearly the same lightness. In every image the stars and
comets still separate the sides at a glance, and the pinhole, the cuts, the threat rings and the
closing point all show.

**On a phone.** Open each board image on the phone, fitted to the screen width, so a 15x15 board
has about 4 mm between points. Check that stars and comets, the pinhole and the cuts are easy to
tell apart at arm's length, in every image.

**Redrawing.** [`pieces/star_chart.py`](pieces/star_chart.py) holds this page's values. It draws
the sample at the top, the piece sheets and the check boards, and
`python3 docs/design/pieces/star_chart.py contrast` prints the contrast tables above. Run it from
the repository root; it needs Python 3 and `rsvg-convert` (from librsvg).
