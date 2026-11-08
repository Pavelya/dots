# Illustrations

Pinfold's characters live around the board, never on it (investigation, section 3): the
opponent's face above the board, the boss faces, and the pictures on the world map. This page
gives the prompts for World 1's illustrations, made with ChatGPT image generation. The
[canvas](canvas/README.md) shows where each one goes; its ink doodles mark the places and sizes.

## The look of every picture

- **Ballpoint ink on paper.** Clean pen lines of even weight with round ends, and shading by
  straight parallel hatching, as a person draws in a school notebook margin.
- **Four colours only:** red ink `#C0392B`, blue ink `#1F4E9C`, pale red `#ECBEB7` and pale blue
  `#BCCBEA` (the theme's Red ink, Blue ink, Red pale and Blue pale), plus white for the whites of
  eyes. No other colours, no gradients, no glow.
- **Transparent background, no paper.** The game draws the paper and the grid itself.
- **No letters, numbers, signatures or logos** anywhere in the picture.
- **The computer is blue.** It plays the blue diamonds, so its faces are blue ink. Red is for
  accents: a flag, a roof, a tree.

**The dark version.** The game's dark theme uses lighter inks on a dark page. Because each
picture keeps to the four colours, a script swaps them for the dark-theme inks (`#FF9384`,
`#6B90E8`, `#5E3834`, `#2F3F66`), so no second set of prompts is needed. Claude makes the dark
versions when the pictures come in.

## Making the pictures

1. Start a new ChatGPT chat for each prompt and paste the prompt exactly as written. Don't upload
   any image, and don't add names of artists, games, studios or brands
   ([prompt rules](../assets.md#prompt-rules)).
2. Reject any picture with letters, numbers, a logo, a signature, a watermark or a character
   someone would recognise, and ask again (up to two more tries). If a prompt keeps failing, say
   so: the prompt changes on this page, not the rules.
3. Save the picture you keep as a PNG named after its prompt, for example `opponent-moods.png`,
   in one folder outside the repository, and note the date.
4. Give Claude the folder. Claude cuts each sheet into single pictures, makes the dark versions,
   puts them on the canvas in place of the doodles, adds them to the asset register
   ([`docs/assets.md`](../assets.md)) and opens the pull request.

## The computer

The opponent in every campaign level that has no boss. It is drawn the same in every world; its
face shows how strong it plays and how the game is going.

**Prompt 1: `opponent-moods`, six moods**

> A character sheet of one small cartoon creature, drawn six times in a grid of three columns
> and two rows, each one separate with space around it. The creature is a round blot of blue
> ink, slightly lumpy at the edges, with one short drip at the lower right and two tiny ink
> droplets flying off beside it. It has two large round eyes with white eyeballs and dark pupils,
> eyebrows drawn as short pale lines on its body, and a mouth drawn as a single pale line. No
> arms, no legs. A small flat oval shadow of pale blue hatching lies under it. The six faces:
> happy with a wide smile; smug with heavy half-closed eyelids and a lopsided grin; worried with
> raised eyebrows and a wavy mouth; upset with a frown and one teardrop; surprised with round
> eyes and a small round open mouth; sulking with eyes looking aside and a flat wavy mouth.
> Drawn as clean ballpoint pen lines with straight parallel hatching for shading. Use only these
> colours: blue ink #1F4E9C for the body, pale blue #BCCBEA for the eyebrows, mouth, shadow and
> hatching, and white for the eyeballs. Transparent background. No paper texture, no letters, no
> numbers, no signature.

**Prompt 2: `opponent-strengths`, how strong it plays**

> A character sheet of one small cartoon creature, drawn three times side by side, each one
> separate with space around it. The creature is a round blot of blue ink, slightly lumpy at the
> edges, with one short drip at the lower right, two large round eyes with white eyeballs and
> dark pupils, and a mouth drawn as a single pale line. No arms, no legs. A small flat oval
> shadow of pale blue hatching lies under each one. Left: relaxed and sleepy, eyes half closed, a
> soft smile, a small droopy sleeping cap on its head. Middle: calm and attentive, eyes wide
> open, a small confident smile. Right: sharp and focused, small round reading glasses on its
> face, eyebrows pulled down, mouth a straight determined line, a pencil tucked behind it like
> behind an ear. Drawn as clean ballpoint pen lines with straight parallel hatching for shading.
> Use only these colours: blue ink #1F4E9C, pale blue #BCCBEA, red ink #C0392B for the sleeping
> cap and the pencil, and white for the eyeballs. Transparent background. No paper texture, no
> letters, no numbers, no signature.

## The World 1 boss

The boss of World 1 waits at the castle at the end of the path. It plays blue, like the
computer, and its face sits above the board on its level.

**Prompt 3: `boss-world-1`, the ink bottle**

> A character sheet of one cartoon creature, drawn three times side by side, each one separate
> with space around it. The creature is a squat old glass ink bottle full of blue ink, with a
> cork in its neck worn like a small crown with three points, a face on the front of the bottle,
> and a puddle of spilled blue ink at its base. Its face has two large round eyes with white
> eyeballs and dark pupils and thick angry eyebrows. Left: smug, eyes half closed, a wide toothy
> grin. Middle: attacking, leaning forward, eyes wide, mouth open in a shout, two ink drops
> flying from its neck. Right: beaten, tilted to one side, eyes closed in two small curved lines,
> the cork crown slipping off. Drawn as clean ballpoint pen lines with straight parallel hatching
> for shading and for the glass highlights. Use only these colours: blue ink #1F4E9C, pale blue
> #BCCBEA, red ink #C0392B and pale red #ECBEB7 for the cork crown, and white for the eyeballs.
> Transparent background. No paper texture, no letters, no numbers, no label on the bottle, no
> signature.

## The World 1 map

The map's path and level circles are drawn by the game. The pictures sit beside the path, each one
placed on its own, so they come as sheets of separate stickers.

**Prompt 4: `map-world-1-nature`, the landscape**

> A sheet of separate small landscape doodles, laid out in a loose grid with plenty of empty
> space between them, nothing touching or overlapping: a group of three pine trees of different
> heights; a single round leafy tree with a short trunk; a small pond with ripples and a few
> reeds at one side; a low rounded hill; a range of two mountain peaks; a small fluffy cloud; a
> small rock; three small grass tufts; a short wooden fence. Drawn as clean ballpoint pen lines of
> even weight with round ends, shaded with straight parallel hatching, like doodles in the margin
> of a school notebook. Pine trees, pond, mountains, cloud, rock, grass and fence in blue ink
> #1F4E9C with pale blue #BCCBEA hatching. The round tree and the hill in red ink #C0392B with
> pale red #ECBEB7 hatching. Use only these four colours. Flat colours, no gradients.
> Transparent background. No paper texture, no letters, no numbers, no signature.

**Prompt 5: `map-world-1-places`, the places on the path**

> A sheet of separate small doodles of places, laid out in a loose grid with plenty of empty
> space between them, nothing touching or overlapping: a small castle with three towers,
> battlements and an arched gate, with a red pennant flag on the middle tower; the same castle
> with its gate open and the flag fallen; a small wooden treasure chest, closed; the same chest
> open, with a soft burst of short lines above it; a small cottage with a pitched roof, a
> chimney and one window; a small flag on a pole stuck in a mound of earth. Drawn as clean
> ballpoint pen lines of even weight with round ends, shaded with straight parallel hatching,
> like doodles in the margin of a school notebook. Castles, cottage walls and the flag pole in
> blue ink #1F4E9C with pale blue #BCCBEA hatching. Flags, the cottage roof and the chests in red
> ink #C0392B with pale red #ECBEB7 hatching. Use only these four colours. Flat colours, no
> gradients. Transparent background. No paper texture, no letters, no numbers, no signature.

## Where each picture goes

| Picture | Where | Size on screen |
| --- | --- | --- |
| The computer's moods | Above the board in every game; the face changes with the moment | 108 dp |
| The computer's strengths | On the level card, next to the opponent's name | 48 dp |
| The World 1 boss | Above the board on the boss level; on the boss's level card | 108 dp; 48 dp |
| Landscape doodles | Beside the path on the World 1 map | 30 to 140 dp wide |
| Castle, chest, cottage, flag | On the path: the boss level, the chest after level 10, the start | 48 to 68 dp |

At 48 dp a face is small: check that the eyes and the mouth still read at that size before
keeping a picture.
