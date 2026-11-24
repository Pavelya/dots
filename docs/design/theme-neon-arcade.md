# Neon arcade theme

Neon arcade is a cosmetic theme (decision 0028,
[`docs/decisions/0028-worlds-and-themes.md`](../decisions/0028-worlds-and-themes.md)). One side
places hot magenta discs, the other electric cyan triangles. Rings are neon tubes with a bright
core, and every live piece and every tube sits in a band of glow. This page gives every value the
app needs to draw it, in a light and a dark version. The
[Neon arcade mood board](moodboards/neon-arcade.md) shows the feel. This page gives the exact
values. It follows the [Notebook theme](theme-notebook.md) and [Piece shapes](pieces.md) section
by section. Where it says "as Notebook", the value on those pages holds unchanged.

<img src="theme-neon-arcade.svg" alt="A board sample in the light and the dark version: magenta discs and cyan triangles, two captured areas with tube rings, washes and hatching, a threat, a closing point and a preview" width="720">

The sample above is drawn from the values on this page, at 2 px per dp. It shows a magenta
capture around two triangles, a cyan capture around one disc, a triangle one move from capture
(solid ring) and one two moves away (faint ring), the point that would close the ring (small
magenta circle), the last move (the disc with the pinhole), and the triangle Cyan is about to
place (bottom left).

## Rules of the theme

- **Neon, drawn in solid colours.** The glow is a band of solid colour round each piece and each
  tube, not a blur. A blur is see-through, so it would change the contrast of everything under it,
  and on a crowded 15x15 board (mood board picture 3) blurs run together and hide the pieces. A
  1.5 dp band reads as glow at phone size and keeps every edge sharp.
- **Magenta is Red's side, Cyan is Blue's.** Wherever the game and the code say "red" and "blue",
  Neon arcade draws magenta and cyan. The player is magenta in the campaign; the computer is cyan.
- **The board stays calm.** The grid is a dim violet and the washes are quiet, as in Notebook.
  The light comes from the pieces and the tubes, not from the board.
- **Three weights,** as Notebook: thin grid lines, tubes twice as thick, solid pieces.
- **Never colour alone.** Discs and triangles differ by shape. Magenta areas are hatched one way and
  cyan areas the other. A captured piece loses its glow as well as getting a cut.
- **Light or dark follows the phone,** as Notebook. The dark version is the mood board: light on a
  near-black screen. The light version is a neon sign by day: coloured glass tubes on a pale wall,
  with the glow as a soft tint round them (see [The light version](#the-light-version)).
- **No texture on the board.** No perspective grid, horizon or shapes (mood board pictures 5 and
  6) under or around the points.

## Colours

| Name | Light | Dark | In the code | Used for |
| --- | --- | --- | --- | --- |
| Paper | `#F6F4FB` | `#0A0A12` | `paper` | The board and the game screen's background |
| Card | `#FFFFFF` | `#1A1730` | `card` | Notes, the opponent's photo and the speech bubble on the game screen |
| Grid | `#DCD6EB` | `#2A2546` | `grid` | Grid lines on the board |
| Magenta ink | `#C8106E` | `#FF3D8B` | `red` | Magenta discs and tubes; the magenta side's name and score; booster counts; error text |
| Cyan ink | `#00778E` | `#26E5FF` | `blue` | Cyan triangles and tubes; the cyan side's name and score; main buttons |
| Magenta wash | `#FADDEB` | `#3A1630` | `redWash` | Fill of an area magenta has captured |
| Cyan wash | `#D8F1F5` | `#0D3440` | `blueWash` | Fill of an area cyan has captured |
| Magenta glow | `#F5B9D7` | `#6A1D4A` | `redPale` | Glow round magenta pieces and tubes; hatching in magenta areas; links between discs; the magenta preview; tape |
| Cyan glow | `#A3DDE7` | `#0A4450` | `bluePale` | Glow round cyan pieces and tubes; hatching in cyan areas; links between triangles; the cyan preview |
| Magenta core | `#F77DB7` | `#FFC2DA` | new | The bright line along the middle of a magenta tube |
| Cyan core | `#5FC9DB` | `#C4F8FF` | new | The bright line along the middle of a cyan tube |
| Volt | `#9C5E00` | `#F5F23A` | `highlighter` | Ring around a group one move from capture; the count of dots left when it runs low |
| Volt faint | `#EBCB8E` | `#5C5A1E` | `highlighterFaint` | Ring around a group two moves from capture |
| Main text | `#1B1730` | `#F3F3FF` | `pencil` | Main text and icons |
| Dim text | `#5A5574` | `#A8A4CA` | `graphite` | Secondary text, hints, outlines of secondary buttons |

Every colour is solid, as in Notebook. The dark version keeps the mood board's screen
`#0A0A12`, grid `#23203A` made a little lighter, both inks `#FF3D8B` and `#26E5FF`, and its
captured-area colours `#3A1630` and `#0E3540` as the washes (the cyan one nudged to `#0D3440`).
The mood board's white light `#F3F3FF` is the main text.

**Why Volt.** Notebook's threat colour, Highlighter, is an amber that means "look here, act soon".
Volt keeps that meaning in the theme's own colour: the mood board's accent yellow in the dark
version, and an amber in the light, where yellow can't reach 3:1 on a pale screen. It belongs to
neither side, so a threat ring is never mistaken for a ring of one side.

**Errors** take Magenta ink, as they take Red ink in Notebook, so an error always has an icon or
words too, never colour alone.

**Notebook colours this theme doesn't use.** Margin: the game screen has no margin line, and the
menu screens stay Notebook's (see [Outside the game screen](#outside-the-game-screen)); the code
can set it to Grid. The tape is Magenta glow. The scrim behind Pause is Notebook's: black at 40% in
the light version, 55% in the dark.

### The light version

Decision 0028 gives every theme a light and a dark version, and Neon arcade is light on a dark
screen, so its light version had to be found rather than inverted. It is a **neon sign by day**:
the tubes are the same coloured glass, but the wall behind them is pale and the glow is only a
tint round them.

- **What stays:** magenta against cyan, discs against triangles, tubes with a bright core, a glow
  band round every live piece and tube, the violet grid, Volt for threats.
- **What changes:** the screen is a pale lilac `#F6F4FB` instead of near-black; the inks are
  deeper (`#C8106E`, `#00778E`) so they reach 4.5:1 as text on it; the glows are pale tints; Volt
  is an amber.
- **What is lost:** on a pale screen nothing really shines, so the light version is the calmer of
  the two. That is what a player who keeps the phone in its light theme asks for.

It meets every contrast target (see [Contrast](#contrast)), so no new decision is needed.

## The board

Sizes depend on the **spacing**, as Notebook: 24 dp on a 360 dp wide phone for a 15-point-wide
board.

| Element | Size at 24 dp spacing | Other spacings | Colour |
| --- | --- | --- | --- |
| Grid line | 1 dp | always 1 dp | Grid |
| Board edge (outermost grid lines) | 1.5 dp | always 1.5 dp | Grid |
| Dot size (a disc across) | 12 dp | half the spacing, at least 9 dp, at most 20 dp | (sets the pieces' sizes) |
| Magenta disc | 12 dp across | the dot size | Magenta ink |
| Cyan triangle | corners 7.9 dp from its centre; sides 13.7 dp | corners 0.66 dot sizes from the centre | Cyan ink |
| Glow round a live piece | 1.5 dp band round its outline | always 1.5 dp | Magenta glow or Cyan glow |
| Ring: the tube | 2 dp | 1/12 of the spacing, at least 1.5 dp, at most 3 dp | Ink of the side that captured |
| Ring: the tube's glow | 1.5 dp to either side of the tube (5 dp in all) | always 1.5 dp to either side | Glow of the side that captured |
| Ring: the tube's core | 0.67 dp, along the middle of the tube | a third of the tube | Core of the side that captured |
| Hatch line | 1 dp, one line every 6 dp | always the same | Magenta glow or Cyan glow |
| Link between two pieces of one side (only when shown) | 1.5 dp | always 1.5 dp | Magenta glow or Cyan glow |
| Threat ring, one move from capture | 20 dp across, 2 dp line | 0.84 of the spacing across | Volt |
| Threat ring, two moves from capture | 20 dp across, 1.5 dp line | 0.84 of the spacing across | Volt faint |
| Closing point | 8 dp across, 1.5 dp line | a third of the spacing across | Ink of the attacker |
| Preview | the moving side's piece inside a 22 dp ring, 2 dp line | piece as above; ring 0.9 of the spacing across | Piece: Magenta glow or Cyan glow; ring: ink of the side to move |
| Drag's guides | 1.5 dp line, dashes 4 dp long with 3 dp gaps | always the same | Ink of the side to move |

Paper fills the board and half a spacing around its outermost points, and the screen around it,
as Notebook.

**Drawing order**, from the back: paper, washes, hatching, grid lines, links, drag's guides, the
glows round live pieces (or, with high-contrast pieces, the bands round all pieces), the tubes'
glows, the tubes, the tubes' cores, threat rings, pieces, last-move pinhole and captured cuts,
closing points, preview. It is Notebook's order with the glows drawn where the high-contrast bands
go, and each ring drawn in three passes. Every ring's glow is drawn before any tube, so a glow
never lies over another ring's tube.

**The glow round a piece** follows its outline, with round corners, so a triangle's glow has soft
corners. Only live pieces glow: a captured piece has no glow (see [The pieces](#the-pieces)). The
preview's pale piece has none either; it is an unlit piece.

**The tubes and the pieces.** A tube runs through the centres of the capturing pieces, so the
pieces lie on top of it and cover its glow round them. At 24 dp spacing the nearest point a tube
passes without touching is 17 dp away, and a piece and a tube's glow reach 7.9 and 2.5 dp from
their centres, so a tube's glow never touches a piece it doesn't run through.

**Links**, **drag's guides** and **captured pieces** work as Notebook: links only where Notebook
draws them, guides as four dashed arms from the preview ring to the board's edge, and captured
pieces in full ink with a cut.

## The pieces

<img src="pieces/neon-arcade-pieces-4x.svg" alt="The pieces in the light and the dark version: magenta disc, cyan triangle, each with the last-move pinhole, each captured with a cut, and each as a preview" width="768">

From left to right: magenta piece, cyan piece, magenta and cyan last move, a captured disc (in a
cyan area), a captured triangle (in a magenta area), magenta and cyan preview. Top row light,
bottom row dark. Each cell is one 24 dp grid square. The same sheet at 1 px per dp is
[`pieces/neon-arcade-pieces-1x.svg`](pieces/neon-arcade-pieces-1x.svg).

Sizes follow the **dot size** (12 dp at 24 dp spacing).

| Element | Shape | Size at 24 dp spacing | In dot sizes | Colour |
| --- | --- | --- | --- | --- |
| Magenta piece | Round disc, centred on the point | 12 dp across | 1 | Magenta ink |
| Cyan piece | Equilateral triangle, one corner straight up, its centre (where its three middle lines meet) on the point, sharp corners | corners 7.9 dp from the centre: 7.9 dp above the point, base 4 dp below it; sides 13.7 dp | corners 0.66 from the centre | Cyan ink |
| Glow | A band round the piece's outline, round corners; live pieces only | 1.5 dp | always 1.5 dp | Magenta glow or Cyan glow |
| Last move | Pinhole in the middle of the newest piece (the triangle's centre) | 3.6 dp across | 0.3 | Paper |
| Captured piece | Cut: a straight band through the middle of the piece; no glow | 2 dp wide, 18 dp long | 1/6 wide, 1.5 long | Wash of the side that captured it |
| Preview | The moving side's shape, pale, inside the preview ring; no glow | as above | as above | Magenta glow or Cyan glow; ring in the ink of the side to move |

**The cut runs along the captor's hatching,** as Notebook. A captured disc lies in a cyan area,
hatched falling (`\`), so its cut falls. A captured triangle lies in a magenta area, hatched rising
(`/`), so its cut rises.

**Why these shapes.**

- **A disc and a triangle, both solid.** The mood board's pieces are glowing discs, and the
  player's side keeps the disc, as Red keeps the round dot in Notebook. The other side needs a
  different outline at 9 dp, and of the simple shapes a triangle differs most from a circle: three
  sharp corners against none. It is also the arcade's own shape, the little ship. A hollow piece
  would look like a threat ring or the preview ring, as in Notebook.
- **Not a square or a diamond.** A square at 9 dp is close to a disc, and its sides would lie along
  the grid lines. A diamond is Notebook's blue piece; each theme keeps its own pair.
- **Equal weight.** The triangle covers 0.57 square dot sizes and the disc 0.79. A triangle with
  corners at 0.72 (0.67 square dot sizes, about the area of Notebook's diamond) looked clearly
  heavier than the disc on the check boards, because its corners reach further, and its top
  corner touched a threat ring's line. At 0.66 the two sides look equally heavy, and the top
  corner stays inside the line.
- **Point up, centre on the point.** The triangle stands on its base like a ship. Its centre, not
  the middle of its height, sits on the point, so it looks centred on the crossing and the pinhole
  sits where the triangle is widest.
- **A captured piece goes dark.** It keeps its full ink, as in every theme, but loses its glow:
  the light is out. With the cut, a captured piece differs from a live one in two ways that don't
  rely on colour.
- **A pinhole for the last move, not a ring of light.** The mood board marks the last move with a
  pulsing ring, but a ring means a threat on this board, as in Notebook. The last move moves as
  Notebook's ([Motion, Neon arcade](motion.md#placing-a-dot-in-neon-arcade)).

### High contrast

As Notebook ([Piece shapes](pieces.md#high-contrast)): the dot size becomes 0.6 of the spacing (at
least 9 dp, at most 24 dp), so the disc is 14.4 dp across and the triangle's corners 9.5 dp from
its centre at 24 dp spacing. Each piece sits in a 1.5 dp band of the Paper or wash under it,
following its outline with round corners. **The band takes the place of the glow**, so with
high-contrast pieces no piece glows; the tubes keep theirs. The preview's pale piece is outlined
in the ink of the side to move, 1.5 dp, and drag's guides become 2 dp dashes on a 4 dp strip of
the surface under them.

At 24 dp spacing two triangles side by side are then 7.5 dp apart, and 4.5 dp once their bands are
drawn, so the bands never join. A triangle's top corner reaches 0.4 dp into the line of a threat
ring round it; the triangle is drawn on top, and the ring still reads as a ring.

With the bands, a piece only meets Paper or a wash. Its weakest pair is Cyan ink on Magenta wash in
the light version (4.12) and Magenta ink on Cyan wash in the dark (3.97).

## The ring

The ring is a neon tube: it runs through the centres of the capturing pieces, with round ends and
round corners. It is **straight** and of **even width**, as glass tubing is: no bow and no change
of thickness. It is drawn in three layers, all in the colours of the side that captured:

1. **Glow:** a line as wide as the tube plus 1.5 dp to either side, in the side's glow.
2. **Tube:** the ring width, in the side's ink.
3. **Core:** a third of the ring width along the middle, in the side's core colour.

The core is what makes the line read as a lit tube rather than a pen line. It is lighter than the
ink in both versions (1.32 to 2.69 against it), never darker, so the tube keeps its colour.

## Captured areas

Two layers inside the ring, as Notebook:

1. **Wash:** a flat fill in Magenta wash or Cyan wash.
2. **Hatching:** straight lines in Magenta glow or Cyan glow, 1 dp thick, one every 6 dp, clipped
   to the ring. Magenta areas are hatched rising to the right (`/`, 45°), cyan areas falling to the
   right (`\`, 135°).

The hatching is as sparse as Star chart's (6 dp), so an area reads as a faint glow, not as stripes.

**On a crowded 15x15 board** (mood board picture 3) the areas stay readable because the washes are
quiet (1.08 to 1.48 against Paper), the two hatchings lean opposite ways, and every ring is a crisp
tube in full ink with its core. The glows never blur: they are solid bands with sharp edges, and
the pieces are drawn over the tubes' glows. The check boards below show a mid-game board at phone
size.

## What follows the theme on the game screen

Every part of the game screen takes this theme's colours by the same roles as Notebook: what is
Paper, Pencil, Red ink or Blue ink there is Paper, Main text, Magenta ink or Cyan ink here. **The
shapes stay Notebook's:** notes are still slightly turned, with tape and a soft shadow; the tool
bar keeps its hand-drawn corners; the fonts stay. Only the board itself changes shape. That keeps
the game one notebook, as decision 0028 says, with the neon board as the page in it. Nothing off
the board glows.

| Part | In Neon arcade |
| --- | --- |
| The screen behind the board | Paper, with no grid outside the board. |
| Top bar: pause icon, level label | Main text and Dim text on Paper. |
| The score rows | Each side's name, tally and score in its ink, with its shape beside the name: a disc for magenta, a triangle for cyan, at the dot size used there, without glow. The pen stroke under the side to move is in its ink. "Dots left" in Main text and Dim text, and in Volt when it runs low, as Notebook's turns Highlighter. |
| The tool bar | Booster buttons as Notebook: icons in Cyan ink, counts in Paper on Magenta ink, empty boosters dashed in Dim text. |
| The notes: mission, level opening, Pause, booster preview, Undo, tutorial | Card, with Magenta glow tape. Ticks in the mission note in Magenta ink. Main buttons Paper on Cyan ink. |
| The result note and the star reveal, the daily result | Card. Headlines in Magenta ink where Notebook uses Red ink (a win), in Cyan ink where it uses Blue ink (a loss). Earned stars in Magenta ink, stars not earned in Dim text outline. |
| Two players' turn banner | The side's ink. |
| The opponent's photo and speech bubble | Card, with Magenta glow tape; the bubble's outline and words in Cyan ink. |
| The computer's faces and the bosses' | Recoloured by a script, not redrawn: see below. |
| Board touches | Drawn in code in the theme's pale inks, as on every theme: Magenta glow and Cyan glow. They sit beside the opponent, never next to the board, so no doodle is taken for a piece. |

**Earned stars are magenta, not yellow.** The mood board's result card (picture 6) shows yellow
stars, but yellow is Volt here, and Volt means "look here, act soon". A star in the side's ink is
praise, as in Notebook. The mood board's dark card with a glowing magenta border is also left out:
notes keep Notebook's shapes.

### The faces

The faces are Notebook ink pictures in four inks and white (see
[Illustrations](illustrations.md)). Neon arcade uses the same pictures with their inks swapped,
the way `make_pictures.py` makes the dark version. **No new picture is needed.** The computer is
the same character in every theme; only its inks follow the board.

<img src="pieces/neon-arcade-faces.png" alt="Four of the computer's pictures recoloured: happy, upset, sharp and the World 1 boss, in the light version above and the dark version below. In the dark version the body is a deep cyan with white eyes, and the eyebrows, mouth and shadow are bright cyan." width="720">

| Ink in the picture | Light version | Dark version |
| --- | --- | --- |
| Red ink `#C0392B` | Magenta ink `#C8106E` | Magenta ink `#FF3D8B` |
| Blue ink `#1F4E9C` | Cyan ink `#00778E` | **`#00778E`** (the light version's Cyan ink) |
| Red pale `#ECBEB7` | Magenta glow `#F5B9D7` | Magenta glow `#6A1D4A` |
| Blue pale `#BCCBEA` | Cyan glow `#A3DDE7` | **Cyan ink `#26E5FF`** |
| White | White | White |

**The dark version is not a plain swap.** The body is drawn in Blue ink and carries the white
eyes, whose pupils are Blue ink too. The dark Cyan ink is so light that white eyes on it are only
1.50:1, and the pupils vanish. So in the dark version the body takes the deeper cyan of the light
version, and the pale lines (eyebrows, mouth, shadow, hatching) take the bright cyan: the face is
drawn in light, like the tubes.

| Faces | Light | Dark |
| --- | --- | --- |
| White eyes on the body | 5.21 (7.99) | 5.21 (3.09) |
| Lines on the body (eyebrows, mouth) | 3.48 (4.89) | 3.40 (3.35) |
| The body on paper | 4.77 (7.53) | 3.78 (5.55) |

Notebook's figures are in brackets. Every figure in the dark version is at least Notebook's dark
figure except the body on paper, which is still 3.78.

Each recoloured picture is saved beside the Notebook one as `<sheet>_<name>_neon_arcade.png` and
`<sheet>_<name>_neon_arcade_dark.png` (for example `opponent_moods_happy_neon_arcade.png`), for
every face the game screen shows: the computer's moods and strengths and the four bosses' faces
(`opponent_moods_*`, `opponent_strengths_*`, `boss_world_*`). [`make_pictures.py`](illustrations/make_pictures.py) makes
them (`themes`, and `cut` for a new sheet), and the app picks them by the theme's `faceInks`.

### Outside the game screen

The map pages stay Notebook's in every theme (decision 0028). So do the notes laid over the map
(the Level card with its small board, the Chest, Today's puzzle, Play a game) and the menu
screens, How to play among them. They are not the board and the screen behind it, which is what a
theme sets.

### Left to other tasks

The capture's burst of sparks and how the pieces and tubes light up are on the motion page
([Motion, Neon arcade](motion.md#neon-arcade)). The board sounds are on the
[sound page](sound.md#neon-arcade). The canvas frames are D-22.

## Type

Neon arcade keeps Notebook's fonts and text styles: Caveat for big headings and Nunito for
everything else ([Notebook, Type](theme-notebook.md#type)).

- Decision 0028 doesn't list fonts as part of a theme, and the game stays one notebook in every
  theme.
- The mood board proposed Orbitron for headings and Exo 2 for everything else. Each would bring a
  new font for a job Caveat or Nunito already does.

## Interface colours

How the colours fill the Material colour roles, as Notebook:

| Material role | Colour |
| --- | --- |
| `background`, `surface` | Paper |
| `surfaceContainer` (and its lower and higher steps) | Card |
| `onBackground`, `onSurface` | Main text |
| `onSurfaceVariant` | Dim text |
| `primary` | Cyan ink |
| `onPrimary` | Paper |
| `secondary` | Magenta ink |
| `onSecondary` | Paper |
| `outline` | Dim text |
| `outlineVariant` | Grid |
| `error` | Magenta ink |
| `onError` | Paper |

## New in the code

What the app's theme classes need for this theme that Notebook and Star chart don't have:

- **A triangle** among the piece shapes: equilateral, corner up, centre on the point, corners
  0.66 dot sizes from it.
- **A glow round live pieces:** a band like the high-contrast band, 1.5 dp, in the side's glow,
  skipped for captured pieces and the preview.
- **A tube for the ring:** a glow width (1.5 dp to either side) and a core (a third of the ring
  width) in a new core colour per side, `redCore` and `blueCore` in the palette.

## Contrast

Ratios use the WCAG 2 formula, cut (not rounded) to two decimals, as on the Notebook page.
Targets: text at least 4.5:1; pieces, rings and marks at least 3:1. "Quiet" pairs are meant to be
low, and have no target.

| Kind | Pair | Target | Light | Dark |
| --- | --- | --- | --- | --- |
| Text | Main text on paper | 4.5 | 15.88 | 17.91 |
| Text | Dim text on paper | 4.5 | 6.44 | 8.28 |
| Text | Main text on card | 4.5 | 17.32 | 15.77 |
| Text | Dim text on card | 4.5 | 7.03 | 7.30 |
| Text | Magenta ink on paper | 4.5 | 5.12 | 5.90 |
| Text | Cyan ink on paper | 4.5 | 4.77 | 12.89 |
| Text | Magenta ink on card | 4.5 | 5.59 | 5.20 |
| Text | Cyan ink on card | 4.5 | 5.21 | 11.35 |
| Text | Paper on cyan ink (main button) | 4.5 | 4.77 | 12.89 |
| Text | Paper on magenta ink (booster count, error) | 4.5 | 5.12 | 5.90 |
| Text | Volt on paper (dots left, running out) | 4.5 | 4.79 | 16.56 |
| Piece | Magenta ink on paper | 3 | 5.12 | 5.90 |
| Piece | Cyan ink on paper | 3 | 4.77 | 12.89 |
| Piece | Magenta ink on grid | 3 | 3.95 | 4.33 |
| Piece | Cyan ink on grid | 3 | 3.68 | 9.46 |
| Piece | Magenta ink on magenta wash | 3 | 4.42 | 4.70 |
| Piece | Cyan ink on magenta wash | 3 | 4.12 | 10.26 |
| Piece | Magenta ink on cyan wash | 3 | 4.74 | 3.97 |
| Piece | Cyan ink on cyan wash | 3 | 4.42 | 8.67 |
| Piece | Magenta ink on magenta glow (its glow; hatch line) | 3 | 3.40 | 3.33 |
| Piece | Cyan ink on magenta glow (hatch line) | 3 | 3.17 | 7.27 |
| Piece | Magenta ink on cyan glow (hatch line) | 3 | 3.74 | 3.21 |
| Piece | Cyan ink on cyan glow (its glow; hatch line) | 3 | 3.48 | 7.02 |
| Mark | Volt on paper | 3 | 4.79 | 16.56 |
| Mark | Volt on magenta wash | 3 | 4.13 | 13.19 |
| Mark | Volt on cyan wash | 3 | 4.43 | 11.15 |
| Mark | Pinhole: paper on magenta ink | 3 | 5.12 | 5.90 |
| Mark | Pinhole: paper on cyan ink | 3 | 4.77 | 12.89 |
| Mark | Cut: cyan wash on magenta ink | 3 | 4.74 | 3.97 |
| Mark | Cut: magenta wash on cyan ink | 3 | 4.12 | 10.26 |
| Quiet | Grid on paper | – | 1.29 | 1.36 |
| Quiet | Magenta wash on paper | – | 1.15 | 1.25 |
| Quiet | Cyan wash on paper | – | 1.08 | 1.48 |
| Quiet | Magenta glow on paper | – | 1.50 | 1.77 |
| Quiet | Cyan glow on paper | – | 1.36 | 1.83 |
| Quiet | Magenta glow on magenta wash | – | 1.29 | 1.41 |
| Quiet | Cyan glow on cyan wash | – | 1.26 | 1.23 |
| Quiet | Magenta core on magenta ink | – | 2.28 | 2.22 |
| Quiet | Cyan core on cyan ink | – | 2.69 | 1.32 |
| Quiet | Volt faint on paper | – | 1.42 | 2.75 |
| Quiet | Magenta ink on cyan ink | – | 1.07 | 2.18 |

Every pair with a target meets it, for the usual and the high-contrast board. The piece rows cover
every surface a piece can sit on: paper, card, a grid line, either wash, either hatch line, and its
own glow, which is the same colour as its side's hatching. With high-contrast pieces only the paper
and wash rows apply. Drag's guides use the two inks, so their figures are the piece rows. The
tubes use the inks too; their cores are lighter than the ink and lie inside it, so they need no
row of their own.

**The two inks have almost the same lightness in the light version** (last row: 1.07), and differ
more in the dark (2.18, cyan the lighter). In greyscale the light version's two sides are the same
grey. The shapes are what separate the sides, as on every theme.

## Colour-blindness check

The same check as [Piece shapes](pieces.md#colour-blindness-check), on the same mid-game 15x15
board at **4 mm per point** (25.2 dp spacing on a 420 dpi phone, 992 px wide), with Notebook's red
as magenta and blue as cyan. Each board is shown as seen, in greyscale, and with protanopia,
deuteranopia and tritanopia (Machado, Oliveira and Fernandes, 2009, at full strength).

| | As seen | Greyscale | Protanopia | Deuteranopia | Tritanopia |
| --- | --- | --- | --- | --- | --- |
| Usual, light | <img src="pieces/neon-arcade-board-light-usual-seen.png" width="140" alt="Light board as seen"> | <img src="pieces/neon-arcade-board-light-usual-greyscale.png" width="140" alt="Light board in greyscale"> | <img src="pieces/neon-arcade-board-light-usual-protanopia.png" width="140" alt="Light board with protanopia"> | <img src="pieces/neon-arcade-board-light-usual-deuteranopia.png" width="140" alt="Light board with deuteranopia"> | <img src="pieces/neon-arcade-board-light-usual-tritanopia.png" width="140" alt="Light board with tritanopia"> |
| Usual, dark | <img src="pieces/neon-arcade-board-dark-usual-seen.png" width="140" alt="Dark board as seen"> | <img src="pieces/neon-arcade-board-dark-usual-greyscale.png" width="140" alt="Dark board in greyscale"> | <img src="pieces/neon-arcade-board-dark-usual-protanopia.png" width="140" alt="Dark board with protanopia"> | <img src="pieces/neon-arcade-board-dark-usual-deuteranopia.png" width="140" alt="Dark board with deuteranopia"> | <img src="pieces/neon-arcade-board-dark-usual-tritanopia.png" width="140" alt="Dark board with tritanopia"> |
| High contrast, light | <img src="pieces/neon-arcade-board-light-contrast-seen.png" width="140" alt="Light board with high-contrast pieces as seen"> | <img src="pieces/neon-arcade-board-light-contrast-greyscale.png" width="140" alt="Light board with high-contrast pieces in greyscale"> | <img src="pieces/neon-arcade-board-light-contrast-protanopia.png" width="140" alt="Light board with high-contrast pieces with protanopia"> | <img src="pieces/neon-arcade-board-light-contrast-deuteranopia.png" width="140" alt="Light board with high-contrast pieces with deuteranopia"> | <img src="pieces/neon-arcade-board-light-contrast-tritanopia.png" width="140" alt="Light board with high-contrast pieces with tritanopia"> |
| High contrast, dark | <img src="pieces/neon-arcade-board-dark-contrast-seen.png" width="140" alt="Dark board with high-contrast pieces as seen"> | <img src="pieces/neon-arcade-board-dark-contrast-greyscale.png" width="140" alt="Dark board with high-contrast pieces in greyscale"> | <img src="pieces/neon-arcade-board-dark-contrast-protanopia.png" width="140" alt="Dark board with high-contrast pieces with protanopia"> | <img src="pieces/neon-arcade-board-dark-contrast-deuteranopia.png" width="140" alt="Dark board with high-contrast pieces with deuteranopia"> | <img src="pieces/neon-arcade-board-dark-contrast-tritanopia.png" width="140" alt="Dark board with high-contrast pieces with tritanopia"> |

Open an image to see it at full size.

**Colour alone is not enough.** The contrast between the two inks in each view:

| View | Light | Dark |
| --- | --- | --- |
| As seen | 1.07 | 2.18 |
| Greyscale | 1.07 | 2.18 |
| Protanopia | 1.63 | 3.37 |
| Deuteranopia | 1.18 | 1.69 |
| Tritanopia | 1.07 | 2.69 |

In the colour-blind views magenta turns slate grey with protanopia, khaki with deuteranopia and
red with tritanopia; cyan turns a pale blue-grey with the first two and teal with tritanopia. With
protanopia in the light version both inks are blue-grey and differ only a little in lightness.
In every image the discs and triangles still separate the sides at a glance, and the pinhole,
the cuts, the threat rings and the closing point all show.

**On a phone.** Open each board image on the phone, fitted to the screen width, so a 15x15 board
has about 4 mm between points. Check that discs and triangles, the pinhole and the cuts are easy to
tell apart at arm's length, in every image.

**Redrawing.** [`pieces/neon_arcade.py`](pieces/neon_arcade.py) holds this page's values. It draws
the sample at the top, the piece sheets, the check boards and the faces picture, and
`python3 docs/design/pieces/neon_arcade.py contrast` prints the contrast tables above. Run it from
the repository root; it needs Python 3 and `rsvg-convert` (from librsvg).
