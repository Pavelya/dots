# Illustrations

Pinfold's characters live around the board, never on it (investigation, section 3): the
opponent's face above the board, the boss faces, and the pictures on the world map. This page
gives the prompts for the computer, the four bosses and the map pages of all four worlds, made
with ChatGPT image generation, and describes each world's board touches. The
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

**The dark version.** The Notebook theme's dark version uses lighter inks on a dark page. Because
each picture keeps to the four colours, a script swaps them for the dark version's inks (`#FF9384`,
`#6B90E8`, `#5E3834`, `#2F3F66`) and keeps white, so no second set of prompts is needed. Claude
makes the dark versions when the pictures come in.

**Other themes.** A theme draws the game screen's pictures with the same swap, from its own inks.
The [Star chart theme](theme-star-chart.md#the-faces) and the
[Neon arcade theme](theme-neon-arcade.md#the-faces) list their inks and file names; neither needs
new prompts. Neon arcade's dark version swaps the blue inks differently, so the white eyes stay
clear on a bright cyan. The map pages keep the Notebook inks in every theme.

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

**The script.** [`illustrations/make_pictures.py`](illustrations/make_pictures.py) does the cutting.
Its first lines say how to run it. It numbers the drawings on a sheet, and Claude names each
picture in the script's list of sheets. Then it sets every pixel to one of the four inks (or white,
on sheets with eyes), turns the paper transparent, makes the dark version, scales each sheet's
pictures to 4 pixels per dp of their size on screen and writes them to
`app/src/main/res/drawable-nodpi/` as `<sheet>_<name>.png` and `<sheet>_<name>_dark.png`. It also
adds the sheet's row to the asset register. For the faces, which the game screen shows, it also
writes each theme's versions (`<sheet>_<name>_star_chart.png`, `..._star_chart_dark.png` and the
same for `neon_arcade`) with a register row per theme; `make_pictures.py themes` makes them again
from the pictures in the app. `make_pictures.py check` checks the pictures in the app, and
`make_pictures.py self-test` runs every step on two sheets drawn in code.

A sheet cuts cleanly when its drawings stand apart, with clear paper between them, as the prompts
ask. Pale paper shut inside a drawing becomes transparent, so a pond or a window shows the page
behind it. Only a patch big enough to be an eyeball becomes white, on the sheets with faces.

## The computer

The opponent in every campaign level that has no boss. It is drawn the same in every world; its
face shows how strong it plays and how the game is going.

- **Its moods** come with the moments of the game: happy as the level opens, smug when the
  player's dots are in danger or it captures, upset when the player captures, sulky when the
  player wins, and smug again when the player loses. Worried and surprised are on the sheet but no
  moment shows them yet.
- **Its strength** is its plain look, between those moments, and its face on the level card:
  sleepy for the beginner and easy steps, calm for steady and normal, sharp for hard and expert.
- **A boss without its own faces** (Worlds 2 to 4 for now) keeps the computer's.

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
computer, and its face sits above the board on its level. It is smug on its level card and
through the game, attacks while it places the two dots of its special turn, and is beaten when
the player captures or wins.

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

## The bosses of Worlds 2 to 4

Each world's boss is drawn like the Ink Bottle (prompt 3): one creature in blue ink, three times,
smug, attacking and beaten, with white eyeballs and thick angry eyebrows. Each wears something red
on its head like a crown, as the Ink Bottle wears its cork; it slips off when the boss is beaten,
as the flag falls on the map. Each attacks with two of something, for the two dots of its special
turn. Each is about as wide as it is tall, so it fills the square photo above the board as the
Ink Bottle does. The faces go where the Ink Bottle's go: above the board on the boss level and on
its level card.

**Prompt 12: `boss-world-2`, the mole**

> A character sheet of one cartoon creature, drawn three times side by side, each one separate
> with space around it. The creature is a plump round mole poking out of a small heap of earth up
> to its waist, with a small flowerpot worn upside down on its head like a crown, a round nose,
> two big front teeth, and two wide digging paws with short claws. Its face has two large round
> eyes with white eyeballs and dark pupils and thick angry eyebrows. Left: smug, eyes half
> closed, a wide grin showing its two front teeth, paws folded on the heap. Middle: attacking,
> leaning forward, eyes wide, mouth open in a shout, both digging paws raised high, two clods of
> earth flying up beside it. Right: beaten, sunk deeper into the heap and tilted to one side,
> eyes closed in two small curved lines, the flowerpot slipping off its head. Drawn as clean
> ballpoint pen lines with straight parallel hatching for shading. Use only these colours: blue
> ink #1F4E9C for the mole, pale blue #BCCBEA for the heap of earth, the clods and the hatching,
> red ink #C0392B and pale red #ECBEB7 for the flowerpot and the nose, and white for the eyeballs
> and the teeth. Transparent background. No paper texture, no letters, no numbers, no signature.

**Prompt 13: `boss-world-3`, the comet**

> A character sheet of one cartoon creature, drawn three times side by side, each one separate
> with space around it. The creature is a round comet: a ball of blue ink with a short, thick tail
> of curved streaks sweeping up and back behind it, so the whole comet is about as wide as it is
> tall, with a small five-pointed star worn on its head like a crown. Its face has two large round
> eyes with white eyeballs and dark pupils and thick angry eyebrows. A small flat oval shadow of
> pale blue hatching lies under it. Left: smug, eyes half closed, a wide toothy grin, the tail
> curled lazily. Middle: attacking, rushing forward and down, eyes wide, mouth open in a shout,
> the tail streaming straight behind it, two small sparks flying ahead of it. Right: beaten,
> tilted to one side, eyes closed in two small curved lines, the tail drooping into a few short
> limp wisps, the star crown slipping off. Each comet stands alone on the empty background, with
> no dark sky and no other stars behind it. Drawn as clean ballpoint pen lines with straight
> parallel hatching for shading. Use only these colours: blue ink #1F4E9C for the comet, pale blue
> #BCCBEA for the tail's streaks, the shadow and the hatching, red ink #C0392B and pale red
> #ECBEB7 for the star crown and the two sparks, and white for the eyeballs and the teeth.
> Transparent background. No paper texture, no letters, no numbers, no signature.

**Prompt 14: `boss-world-4`, the kraken**

> A character sheet of one cartoon creature, drawn three times side by side, each one separate
> with space around it. The creature is a squat round sea monster with a big dome-shaped head and
> four short curling tentacles with round suckers, rising from a small ring of waves, with a
> small branch of coral worn on its head like a crown with three points. Its face has two large
> round eyes with white eyeballs and dark pupils and thick angry eyebrows. Left: smug, eyes half
> closed, a wide toothy grin, two tentacles folded in front of it. Middle: attacking, leaning
> forward, eyes wide, mouth open in a shout, two tentacles raised high on either side of its head,
> two splashes of water flying up. Right: beaten, sinking into the waves and tilted to one side,
> eyes closed in two small curved lines, the tentacles hanging limp, the coral crown slipping off.
> Drawn as clean ballpoint pen lines with straight parallel hatching for shading. Use only these
> colours: blue ink #1F4E9C for the sea monster, pale blue #BCCBEA for the suckers, the waves, the
> splashes and the hatching, red ink #C0392B and pale red #ECBEB7 for the coral crown, and white
> for the eyeballs and the teeth. Transparent background. No paper texture, no letters, no
> numbers, no signature.

## The World 1 map page

World 1 is Countryside. The map's path and level circles are drawn by the game. The pictures sit
beside the path, each one placed on its own, so they come as sheets of separate stickers. Every
world's map page is drawn this way, whatever theme the player picks
([decision 0028](../decisions/0028-worlds-and-themes.md)).

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

## The World 2 map page

World 2 is Garden. Its doodles come from its levels: flower beds (2-6), a hedge (2-12) and a few
seeds (2-18). Its boss, the Mole, waits at a garden shed with a molehill at the door. The chest is
the same in every world (prompt 5).

**Prompt 6: `map-world-2-nature`, the garden**

> A sheet of separate small garden doodles, laid out in a loose grid with plenty of empty space
> between them, nothing touching or overlapping: a row of three sunflowers of different heights; a
> group of three tulips; a watering can; three short rows of planted seeds with small sprouts; a
> wheelbarrow; a butterfly; a short wooden picket fence; a low rounded hedge; three small daisies.
> Drawn as clean ballpoint pen lines of even weight with round ends, shaded with straight parallel
> hatching, like doodles in the margin of a school notebook. The watering can, the seed rows, the
> hedge, the tulip stems and the daisies in blue ink #1F4E9C with pale blue #BCCBEA hatching. The
> sunflowers, the tulip flowers, the wheelbarrow, the butterfly and the fence in red ink #C0392B
> with pale red #ECBEB7 hatching. Use only these four colours. Flat colours, no gradients.
> Transparent background. No paper texture, no letters, no numbers, no signature.

**Prompt 7: `map-world-2-places`, the places on the path**

> A sheet of separate small doodles of places, laid out in a loose grid with plenty of empty space
> between them, nothing touching or overlapping: a small garden shed with a pitched roof, a door
> and one window, a red pennant flag on the roof, and a fresh molehill of earth in front of the
> door; the same shed with its door open, the flag fallen and the molehill flattened; a small
> garden gate in a short stretch of fence, closed. Drawn as clean ballpoint pen lines of even weight
> with round ends, shaded with straight parallel hatching, like doodles in the margin of a school
> notebook. The shed walls, the molehills and the gate in blue ink #1F4E9C with pale blue #BCCBEA
> hatching. The flags, the shed roofs and the fence in red ink #C0392B with pale red #ECBEB7
> hatching. Use only these four colours. Flat colours, no gradients. Transparent background. No
> paper texture, no letters, no numbers, no signature.

## The World 3 map page

World 3 is Night sky, still drawn on the notebook's paper: the moon, stars and a comet as doodles,
never a dark sky. Its doodles come from its levels: the lake (3-1), Full Moon (3-6), Crater (3-12)
and Twin Stars (3-18). Its boss, the Comet, waits at an observatory.

**Prompt 8: `map-world-3-nature`, the night sky**

> A sheet of separate small night-sky doodles, laid out in a loose grid with plenty of empty space
> between them, nothing touching or overlapping: a crescent moon; a round full moon with a few
> craters; two five-pointed stars side by side; a small comet with a short straight tail; a ringed
> planet; a telescope on a three-legged stand; a small lake with the reflection of a crescent moon
> and a few ripples; a shallow crater in the ground; a range of low rounded hills; four small
> twinkling stars drawn as short crosses. Each doodle stands alone on the empty background, with no
> dark sky behind it. Drawn as clean ballpoint pen lines of even weight with round ends, shaded with
> straight parallel hatching, like doodles in the margin of a school notebook. The moons, the
> telescope, the lake, the hills and the twinkling stars in blue ink #1F4E9C with pale blue #BCCBEA
> hatching. The two stars, the comet, the planet and the crater in red ink #C0392B with pale red
> #ECBEB7 hatching. Use only these four colours. Flat colours, no gradients. Transparent
> background. No paper texture, no letters, no numbers, no signature.

**Prompt 9: `map-world-3-places`, the places on the path**

> A sheet of separate small doodles of places, laid out in a loose grid with plenty of empty space
> between them, nothing touching or overlapping: a small round observatory with a domed roof, a
> door, and a telescope poking out of a slit in the dome, a red pennant flag on the dome; the same
> observatory with the telescope pointing down, the slit shut and the flag fallen; a small tent
> beside a little campfire. Each doodle stands alone on the empty background, with no dark sky
> behind it. Drawn as clean ballpoint pen lines of even weight with round ends, shaded with straight
> parallel hatching, like doodles in the margin of a school notebook. The observatories, the
> telescopes and the tent in blue ink #1F4E9C with pale blue #BCCBEA hatching. The flags, the
> campfire's flames and the tent's door flap in red ink #C0392B with pale red #ECBEB7 hatching.
> Use only these four colours. Flat colours, no gradients. Transparent background. No paper
> texture, no letters, no numbers, no signature.

## The World 4 map page

World 4 is Sea. Its doodles come from its levels: Rocky Shore (4-6), Pearls (4-12) and Reef
(4-18). Its boss, the Kraken, waits at a stone tower on a rock, with its tentacles round it.

**Prompt 10: `map-world-4-nature`, the sea**

> A sheet of separate small seaside doodles, laid out in a loose grid with plenty of empty space
> between them, nothing touching or overlapping: a small sandy island with one palm tree; a small
> sailing boat with a triangular sail; two rounded rocks in the water; a scallop shell; a round
> pearl; a jumping fish with a small splash; a seagull in flight drawn as one curved line; a band of
> rolling waves; four small single waves. Drawn as clean ballpoint pen lines of even weight with
> round ends, shaded with straight parallel hatching, like doodles in the margin of a school
> notebook. The boat's hull, the rocks, the pearl, the fish, the gull and all the waves in blue ink
> #1F4E9C with pale blue #BCCBEA hatching. The island, the palm tree, the sail and the scallop shell
> in red ink #C0392B with pale red #ECBEB7 hatching. Use only these four colours. Flat colours, no
> gradients. Transparent background. No paper texture, no letters, no numbers, no signature.

**Prompt 11: `map-world-4-places`, the places on the path**

> A sheet of separate small doodles of places, laid out in a loose grid with plenty of empty space
> between them, nothing touching or overlapping: a small round stone tower with battlements on a
> rock in the sea, a red pennant flag on top, and two curling tentacles rising from the water on
> either side of it; the same tower with the tentacles gone, a few ripples in their place and the
> flag fallen; a small wooden pier with a rowing boat tied to it. Drawn as clean ballpoint pen
> lines of even weight with round ends, shaded with straight parallel hatching, like doodles in the
> margin of a school notebook. The towers, the rocks, the tentacles, the water, the pier and the
> boat in blue ink #1F4E9C with pale blue #BCCBEA hatching. The flags and the boat's oars in red
> ink #C0392B with pale red #ECBEB7 hatching. Use only these four colours. Flat colours, no
> gradients. Transparent background. No paper texture, no letters, no numbers, no signature.

## Board touches

Each world adds a few small doodles to the game screen, beside the opponent (decision 0028). They
are drawn in code, not made with ChatGPT, so they follow the chosen theme's pale inks and stay
crisp at any size. The canvas frames `game-world-2` to `game-world-4` show them; their paths are
`TOUCHES` in [`canvas/draw.py`](canvas/draw.py).

- **Where:** in the empty paper beside the opponent's photo: left of it, and right of it under the
  speech bubble, above the computer's name. Never on or next to the board, the tool bar, the
  mission note or the bubble.
- **How:** three or four line doodles, 20 to 30 dp each, in Red pale and Blue pale, with
  1.6 dp lines and no fill, like light pencil marks.
- **Which:** World 1 a wave, a star, a spiral and a paper plane; World 2 a flower, a leaf, a
  butterfly and a tulip; World 3 a crescent moon, a star, a comet and a twinkle; World 4 a fish, a
  shell, a boat and a wave.
- **When:** in every campaign level of the world, light and dark, while the opponent's photo shows.
  On a small phone the photo makes way for the board, and the touches go with it. Free play, two
  players and the daily puzzle belong to no world and have none. The app's `BoardTouches` holds the
  canvas's paths.
- **No paper tint.** The board's paper is the theme's in every world. A tint would change the
  contrast that the theme's colours were checked for, so every world would need those checks again.

## Where each picture goes

| Picture | Where | Size on screen |
| --- | --- | --- |
| The computer's moods | Above the board in every game; the face changes with the moment | 108 dp |
| The computer's strengths | On the level card, next to the opponent's name; above the board between moods | 48 dp; 108 dp |
| The World 1 boss | Above the board on the boss level; on the boss's level card | 108 dp; 48 dp |
| Landscape doodles | Beside the path on the World 1 map page; `MapPages` in the app lists where each one sits | 17 to 84 dp wide |
| Castle, chest, flag | On the path: the boss level (the castle beaten once it is passed), the chest after level 10 (open once it is opened), the start at the foot of World 1's path | 48 to 68 dp |
| Cottage | Beside the start of World 1's path | 54 dp |
| The open chest | On the Chest note, after every 10th level (the same picture as on the map) | 96 dp tall; 64 dp on a small phone |
| Garden, night-sky and seaside doodles | Beside the path on the World 2, 3 and 4 map pages; `MapPages` in the app lists where each one sits | 12 to 84 dp wide |
| Shed, observatory, tower; gate, tent, pier | On the path: the boss level of Worlds 2, 3 and 4 (beaten once it is passed); the start at the foot of each world's path | 50 to 68 dp |
| Board touches (drawn in code) | Beside the opponent in every game of the world | 20 to 30 dp |

At 48 dp a face is small: check that the eyes and the mouth still read at that size before
keeping a picture.
