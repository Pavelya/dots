# Logo and app icon

Pinfold's mark is the game's key moment: a ring of red ink closing around a blue dot. A pinfold is
an old pen for stray animals, so the mark is the name too. This page gives the brief, the prompts
for the drafts, and the sizes Android and Google Play need.

## The brief

- **One idea.** A red pen loop closed around one blue dot, in the Notebook colours
  ([`theme-notebook.md`](../theme-notebook.md)): Red ink `#C0392B`, Blue ink `#1F4E9C`,
  Paper `#FBF8F1`.
- **Reads at 48 px.** A phone shows the icon about 48 dp wide. Few shapes, bold lines, no fine
  detail. Hatching, paper grain and thin grid lines disappear at that size, so they are optional
  extras, never the idea itself.
- **No letters in the icon.** The icon is only the mark. The logo is the mark beside the word
  "Pinfold", set in Caveat 700 (the theme's heading font, Open Font License) in Pencil `#2E2E2E`,
  with the letters turned into outlines in the SVG.
- **Ours.** No shape that copies another game's or app's icon. Every draft and the final are
  reverse-image-searched (prompt rule 4 in [`docs/assets.md`](../../assets.md)).
- **Works in one colour.** Android 13 and later can show every icon in one colour picked from the
  wallpaper (themed icons). The mark must still read as a ring around a dot when it is a single
  flat shape.

The home screen picture on the Notebook mood board
([`moodboards/notebook.md`](../moodboards/notebook.md), picture 7) already shows an early form of
this mark: eight red dots joined in a ring around one blue dot.

## Drafts

Each prompt below goes into a **new** ChatGPT chat, pasted exactly as written. They follow the
prompt rules in [`docs/assets.md`](../../assets.md): no names, no uploads, and the picture is
thrown away if it shows letters, a logo, a signature or a known character. If a picture breaks a
rule, ask ChatGPT for a new one (up to two more tries) and keep the best clean one.

Save each kept picture as PNG and name it after its prompt: `draft-1.png` to `draft-5.png`. Run a
reverse image search on each with [Google Lens](https://lens.google.com) and
[TinEye](https://tineye.com), and note anything that looks close to an existing icon.

### 1. Diamond on the grid

The smallest capture in the game: four dots around one, on the crossings of the grid.

```text
Square image. A mobile app icon, seen straight on, flat and simple: one bold emblem on a plain background, filling the whole square edge to edge. No rounded corners, no frame, no drop shadow, no 3D, no gloss, no gradients. The look is ink on school paper: a warm off-white paper background, warm red ballpoint ink and deep blue ballpoint ink, lines slightly uneven like a pen but bold and clean. Few shapes, thick enough to read when the icon is shown very small on a phone home screen. The emblem sits in the middle, with a wide empty margin of paper around it on every side. The emblem: three large squares of a faint pale-blue grid, and on its crossings four red ink dots on the corners of a diamond, joined by a thick hand-drawn red pen loop, around one blue ink dot in the centre. The inside of the loop is lightly shaded in pale red. No text, letters, numbers, logos or signatures anywhere in the image.
```

### 2. Ring of eight

The mood board's emblem, made bolder and simpler.

```text
Square image. A mobile app icon, seen straight on, flat and simple: one bold emblem on a plain background, filling the whole square edge to edge. No rounded corners, no frame, no drop shadow, no 3D, no gloss, no gradients. The look is ink on school paper: a warm off-white paper background, warm red ballpoint ink and deep blue ballpoint ink, lines slightly uneven like a pen but bold and clean. Few shapes, thick enough to read when the icon is shown very small on a phone home screen. The emblem sits in the middle, with a wide empty margin of paper around it on every side. The emblem: eight red ink dots set evenly in a ring, joined by a thick hand-drawn red pen line into a closed loop, around one larger blue ink dot in the centre. The inside of the loop is lightly shaded in pale red with a few fine diagonal hatch lines. No text, letters, numbers, logos or signatures anywhere in the image.
```

### 3. One closing stroke

The pen gesture alone: the moment the ring closes.

```text
Square image. A mobile app icon, seen straight on, flat and simple: one bold emblem on a plain background, filling the whole square edge to edge. No rounded corners, no frame, no drop shadow, no 3D, no gloss, no gradients. The look is ink on school paper: a warm off-white paper background, warm red ballpoint ink and deep blue ballpoint ink, lines slightly uneven like a pen but bold and clean. Few shapes, thick enough to read when the icon is shown very small on a phone home screen. The emblem sits in the middle, with a wide empty margin of paper around it on every side. The emblem: one continuous, thick red pen stroke that curves all the way around a single large blue ink dot, its end just meeting its start, with a solid red ink dot where the two ends meet. The loop is round but a little uneven, like it was drawn in one quick confident movement. No text, letters, numbers, logos or signatures anywhere in the image.
```

### 4. The folded page

The "fold" in the name: a notebook page with its corner turned over.

```text
Square image. A mobile app icon, seen straight on, flat and simple: one bold emblem on a plain background, filling the whole square edge to edge. No rounded corners, no frame, no drop shadow, no 3D, no gloss, no gradients. Background: flat deep blue ink colour. The emblem: a square sheet of off-white notebook paper with a faint pale-blue grid, its top right corner folded over towards the viewer. On the sheet, four red ink dots on the corners of a diamond, joined by a thick hand-drawn red pen loop, around one blue ink dot in the centre. Lines slightly uneven like a pen but bold and clean. Few shapes, thick enough to read when the icon is shown very small on a phone home screen. The sheet sits in the middle, with a wide empty margin of blue around it on every side. No text, letters, numbers, logos or signatures anywhere in the image.
```

### 5. Paper badge on blue

A light mark on a dark field, so it stands out among light icons.

```text
Square image. A mobile app icon, seen straight on, flat and simple: one bold emblem on a plain background, filling the whole square edge to edge. No rounded corners, no frame, no drop shadow, no 3D, no gloss, no gradients. Background: flat deep blue ink colour. The emblem: a round disc of warm off-white paper in the middle. On the disc, six red ink dots set in a ring, joined by a thick hand-drawn red pen loop, around one blue ink dot in the centre. Lines slightly uneven like a pen but bold and clean. Few shapes, thick enough to read when the icon is shown very small on a phone home screen. The disc sits in the middle, with a wide empty margin of blue around it on every side. No text, letters, numbers, logos or signatures anywhere in the image.
```

## From draft to icon

1. The owner picks one draft.
2. Claude redraws it as a clean SVG in the theme colours: simple circles and paths, no traced
   pixels.
3. The owner opens the SVG in [Inkscape](https://inkscape.org), adjusts it, and saves the final.
   A person finishes key art (prompt rule 5).
4. The final is reverse-image-searched like the drafts.
5. The SVG is turned into the Android icon layers and the store icon below.

## Sizes

**Adaptive icon (Android 8 and later).** Three layers, each on a 108 x 108 dp canvas:

| Layer | What it holds |
| --- | --- |
| Background | Flat colour, edge to edge |
| Foreground | The mark. Phones crop the canvas to the middle 72 dp, in a shape the phone picks (circle, squircle, rounded square). Everything that matters stays inside a circle 66 dp across in the middle. |
| Monochrome | The mark as one flat shape, same canvas and safe circle. The phone tints it for themed icons. |

**Older phones (Android 7).** The app supports Android 7 (API 24), which has no adaptive icons.
It gets PNG icons with the layers already combined: 48, 72, 96, 144 and 192 px (mdpi to xxxhdpi).

**Google Play.** A 512 x 512 px PNG, full square, no rounded corners or shadow (Play rounds the
corners itself), at most 1 MB.

## Files

| File | What it is |
| --- | --- |
| `docs/design/brand/drafts/draft-*.jpg` | The ChatGPT drafts. Reference only, never shipped. |
| `docs/design/brand/pinfold-mark.svg` | The mark: the icon's source |
| `docs/design/brand/pinfold-logo.svg` | The logo: the mark and the word Pinfold |
| `docs/design/brand/pinfold-store-512.png` | The Google Play icon |
| `app/src/main/res/drawable/ic_launcher_*.xml` | The adaptive icon layers |
| `app/src/main/res/mipmap-*/ic_launcher*` | The launcher icon for each screen density |
