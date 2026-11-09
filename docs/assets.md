# Asset register

Every image, sound, music track, font and video in this repository has a row here. The row says
where the file came from and who may use it, so any claim that we used someone else's work can be
answered in minutes. The rules come from decision 0009
([`docs/decisions/0009-asset-ownership.md`](decisions/0009-asset-ownership.md)).

## Clean sources

An asset may come only from:

- **us:** drawn, recorded, written or coded by the owner or by Claude for the owner;
- **AI tools under the prompt rules below:** ChatGPT for images, Suno Pro (while subscribed) for
  music and a few sound effects (decision 0020);
- **CC0:** Kenney packs, Freesound sounds marked CC0;
- **a licence that allows commercial use in an app:** Google Fonts (Open Font License, keep the
  licence file next to the font), Material Symbols (Apache 2.0).

Never use images from search results or Pinterest, assets from other games, music from YouTube or
streaming services, fonts from "free font" sites without a clear licence, or anything marked
"personal use only" or "non-commercial". Screenshots of other games stay private references
(D-01). They never enter the repository and are never uploaded to an image or music generator.
AI assistants may read them to collect and describe references.

## Prompt rules

For every AI-generated image or track:

1. **No names.** A prompt never names an artist, studio, game, brand or known character, and never
   says "in the style of". Describe the look in plain words: colours, materials, mood, shapes.
2. **No uploads we do not own.** Only images we made, or earlier outputs of our own prompts, go in
   as references.
3. **Reject tainted outputs.** Throw away any output that shows a logo, letters, a signature, a
   watermark or a character someone would recognise.
4. **Search key art.** Before the logo, the app icon or a main background is used, run a reverse
   image search with Google Lens and TinEye on the draft and on the final. Note the result in the
   row's "Edits" column. Run a song-recognition app (for example Shazam) on each AI music track
   in the same way.
5. **A person finishes key art.** The logo and the app icon are redrawn by a person, as vectors.
6. **Keep the prompt.** Copy the exact prompt into the row, or into a file next to the asset that
   the row links to.

## Adding an asset

1. Add the file and a row below in the same pull request. The row's first cell is the file's path
   from the repository root, in backticks. A `*` matches any characters except `/`, so one row can
   cover the sizes of one image (`app/src/main/res/mipmap-*/ic_launcher.png`).
2. Fill every column. "Prompt or source" holds the prompt, a link to the source page, or how the
   file was made. "Edits" says what a person changed, or "none". Write a `|` inside a cell as
   `\|`.
3. Mood-board images and other design references start "Reference:" in "What it is". They are never
   shipped in the app.
4. When a file is removed, remove its row too.

**The check.** `./gradlew :cli:test` (part of the CI command) fails when:

- an asset file has no row. Asset files are images, sound, music, fonts and video anywhere in the
  repository (by file type), and every file in the app's `drawable*`, `mipmap*` and `font*`
  resource folders, whatever its type (vector images and font descriptions are XML);
- a row matches no file;
- a row has an empty cell.

Screenshot-test references in `app/src/test/screenshots/` are left out: the app's own code draws
them.

## Register

| File | What it is | Tool and plan | Date | Prompt or source | Licence | Author | Edits |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `docs/images/campaign-loop.png` | Diagram of the campaign loop in the design investigation | Claude Docs diagram (owner's Claude plan), exported as PNG | 2026-11-05 | Drawn from the investigation's text; no image tool or prompt | Ours | Claude for the owner | none |
| `docs/images/plan-phase-dependencies.png` | Diagram of the phase dependencies in the plan | Claude Docs diagram (owner's Claude plan), exported as PNG | 2026-11-05 | Drawn from the plan's text; no image tool or prompt | Ours | Claude for the owner | none |
| `docs/design/moodboards/notebook-8.svg` | Reference: Notebook mood board, palette chips and a small board with one capture | Claude Code (owner's Claude plan), SVG written in code | 2026-11-07 | Drawn in code from the palette on `docs/design/moodboards/notebook.md`; no image tool or prompt | Ours | Claude for the owner | none |
| `docs/design/moodboards/night-sky-8.svg` | Reference: Night sky mood board, palette chips and a small board with one capture | Claude Code (owner's Claude plan), SVG written in code | 2026-11-07 | Drawn in code from the palette on `docs/design/moodboards/night-sky.md`; no image tool or prompt | Ours | Claude for the owner | none |
| `docs/design/moodboards/neon-arcade-8.svg` | Reference: Neon arcade mood board, palette chips and a small board with one capture | Claude Code (owner's Claude plan), SVG written in code | 2026-11-07 | Drawn in code from the palette on `docs/design/moodboards/neon-arcade.md`; no image tool or prompt | Ours | Claude for the owner | none |
| `docs/design/moodboards/notebook-1.jpg` | Reference: Notebook mood board, picture 1, the board in play | ChatGPT image generation (owner's Pro plan) | 2026-11-07 | Prompt 1 on `docs/design/moodboards/notebook.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/notebook-2.jpg` | Reference: Notebook mood board, picture 2, a capture | ChatGPT image generation (owner's Pro plan) | 2026-11-07 | Prompt 2 on `docs/design/moodboards/notebook.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/notebook-3.jpg` | Reference: Notebook mood board, picture 3, a crowded late game | ChatGPT image generation (owner's Pro plan) | 2026-11-07 | Prompt 3 on `docs/design/moodboards/notebook.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/notebook-4.jpg` | Reference: Notebook mood board, picture 4, the world map | ChatGPT image generation (owner's Pro plan) | 2026-11-07 | Prompt 4 on `docs/design/moodboards/notebook.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/notebook-5.jpg` | Reference: Notebook mood board, picture 5, a world background | ChatGPT image generation (owner's Pro plan) | 2026-11-07 | Prompt 5 on `docs/design/moodboards/notebook.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/notebook-6.jpg` | Reference: Notebook mood board, picture 6, a won level | ChatGPT image generation (owner's Pro plan) | 2026-11-07 | Prompt 6 on `docs/design/moodboards/notebook.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/notebook-7.jpg` | Reference: Notebook mood board, picture 7, the home screen | ChatGPT image generation (owner's Pro plan) | 2026-11-07 | Prompt 7 on `docs/design/moodboards/notebook.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/night-sky-1.jpg` | Reference: Night sky mood board, picture 1, the board in play | ChatGPT image generation (owner's Pro plan) | 2026-11-07 | Prompt 1 on `docs/design/moodboards/night-sky.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/night-sky-2.jpg` | Reference: Night sky mood board, picture 2, a capture | ChatGPT image generation (owner's Pro plan) | 2026-11-07 | Prompt 2 on `docs/design/moodboards/night-sky.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/night-sky-3.jpg` | Reference: Night sky mood board, picture 3, a crowded late game | ChatGPT image generation (owner's Pro plan) | 2026-11-07 | Prompt 3 on `docs/design/moodboards/night-sky.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/night-sky-4.jpg` | Reference: Night sky mood board, picture 4, the world map | ChatGPT image generation (owner's Pro plan) | 2026-11-08 | Prompt 4 on `docs/design/moodboards/night-sky.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/night-sky-5.jpg` | Reference: Night sky mood board, picture 5, a world background | ChatGPT image generation (owner's Pro plan) | 2026-11-08 | Prompt 5 on `docs/design/moodboards/night-sky.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/night-sky-6.jpg` | Reference: Night sky mood board, picture 6, a won level | ChatGPT image generation (owner's Pro plan) | 2026-11-08 | Prompt 6 on `docs/design/moodboards/night-sky.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/night-sky-7.jpg` | Reference: Night sky mood board, picture 7, the home screen | ChatGPT image generation (owner's Pro plan) | 2026-11-08 | Prompt 7 on `docs/design/moodboards/night-sky.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/neon-arcade-1.jpg` | Reference: Neon arcade mood board, picture 1, the board in play | ChatGPT image generation (owner's Pro plan) | 2026-11-08 | Prompt 1 on `docs/design/moodboards/neon-arcade.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/neon-arcade-2.jpg` | Reference: Neon arcade mood board, picture 2, a capture | ChatGPT image generation (owner's Pro plan) | 2026-11-08 | Prompt 2 on `docs/design/moodboards/neon-arcade.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/neon-arcade-3.jpg` | Reference: Neon arcade mood board, picture 3, a crowded late game | ChatGPT image generation (owner's Pro plan) | 2026-11-08 | Prompt 3 on `docs/design/moodboards/neon-arcade.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/neon-arcade-4.jpg` | Reference: Neon arcade mood board, picture 4, the world map | ChatGPT image generation (owner's Pro plan) | 2026-11-08 | Prompt 4 on `docs/design/moodboards/neon-arcade.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/neon-arcade-5.jpg` | Reference: Neon arcade mood board, picture 5, a world background | ChatGPT image generation (owner's Pro plan) | 2026-11-08 | Prompt 5 on `docs/design/moodboards/neon-arcade.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/neon-arcade-6.jpg` | Reference: Neon arcade mood board, picture 6, a won level | ChatGPT image generation (owner's Pro plan) | 2026-11-08 | Prompt 6 on `docs/design/moodboards/neon-arcade.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/moodboards/neon-arcade-7.jpg` | Reference: Neon arcade mood board, picture 7, the home screen | ChatGPT image generation (owner's Pro plan) | 2026-11-08 | Prompt 7 on `docs/design/moodboards/neon-arcade.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1080 px on the long side; checked: no letters, logos, signatures or known characters |
| `docs/design/theme-notebook.svg` | Reference: Notebook theme, a board sample in the light and the dark version | Claude Code (owner's Claude plan), SVG written in code | 2026-11-08 | Drawn by a script from the colours and sizes on `docs/design/theme-notebook.md`; no image tool or prompt | Ours | Claude for the owner | Blue pieces redrawn as diamonds and captured pieces given cuts, to match `docs/design/pieces.md` |
| `docs/design/pieces/pieces-*.svg` | Reference: the piece sheet at 1 and 4 px per dp, light and dark: red dot, blue diamond, last-move pinhole, captured cut, preview | Claude Code (owner's Claude plan), SVG written by `docs/design/pieces/draw.py` | 2026-11-08 | Drawn from the values on `docs/design/pieces.md`; no image tool or prompt | Ours | Claude for the owner | none |
| `docs/design/pieces/board-*.png` | Reference: colour-blindness check, a 15x15 board in the light and the dark version, as seen, in greyscale and with protanopia, deuteranopia and tritanopia | Claude Code (owner's Claude plan), SVG written by `docs/design/pieces/draw.py`, rendered with rsvg-convert | 2026-11-08 | Drawn from the values on `docs/design/pieces.md`; simulation matrices from Machado, Oliveira and Fernandes (2009); no image tool or prompt | Ours | Claude for the owner | none |
| `app/src/main/res/drawable/ic_launcher_*.xml` | App icon: the adaptive icon's background, foreground and monochrome layers, a red ring of four dots around a blue dot on a folded notebook page | Claude Code (owner's Claude plan), SVG written in code | 2026-11-08 | Drawn from the shapes in `docs/design/brand/pinfold-icon.svg` | Ours | Claude for the owner | none |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml` | App icon: the adaptive icon, square and round, joining the three layers | Claude Code (owner's Claude plan), XML written in code | 2026-11-08 | Points to the layers in `app/src/main/res/drawable/` | Ours | Claude for the owner | none |
| `app/src/main/res/mipmap-*/ic_launcher*.png` | App icon for Android 7: square and round, five sizes each | Claude Code (owner's Claude plan), rendered from SVG with rsvg-convert | 2026-11-08 | Rendered from `docs/design/brand/pinfold-icon.svg` | Ours | Claude for the owner | none |
| `docs/design/brand/pinfold-icon.svg` | The app icon's source, with background, foreground, monochrome and guide layers | Claude Code (owner's Claude plan), SVG written in code | 2026-11-08 | Redrawn in code from draft 4 (the owner's pick); see `docs/design/brand/README.md` | Ours | Claude for the owner | Redrawn as clean vector shapes in the Notebook theme colours; reverse image search: pending |
| `docs/design/brand/pinfold-logo.svg` | The logo: the icon tile beside the word Pinfold | Claude Code (owner's Claude plan), SVG written in code; letters from the font Caveat, turned into outlines | 2026-11-08 | The icon tile from `docs/design/brand/pinfold-icon.svg`; the word set in [Caveat](https://fonts.google.com/specimen/Caveat) Bold | Ours; the letter shapes are Caveat, SIL Open Font License 1.1 | Claude for the owner | none |
| `docs/design/brand/pinfold-store-512.png` | The Google Play icon, 512 x 512 px | Claude Code (owner's Claude plan), rendered from SVG with rsvg-convert | 2026-11-08 | Rendered from `docs/design/brand/pinfold-icon.svg` | Ours | Claude for the owner | none |
| `docs/design/brand/drafts/draft-1.jpg` | Reference: icon draft 1, diamond on the grid | ChatGPT image generation (owner's Pro plan) | 2026-11-08 | Prompt 1 on `docs/design/brand/README.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1024 px; checked: no letters, logos, signatures or known characters; reverse image search: pending |
| `docs/design/brand/drafts/draft-2.jpg` | Reference: icon draft 2, ring of eight | ChatGPT image generation (owner's Pro plan) | 2026-11-08 | Prompt 2 on `docs/design/brand/README.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1024 px; checked: no letters, logos, signatures or known characters; reverse image search: pending |
| `docs/design/brand/drafts/draft-3.jpg` | Reference: icon draft 3, one closing stroke | ChatGPT image generation (owner's Pro plan) | 2026-11-08 | Prompt 3 on `docs/design/brand/README.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1024 px; checked: no letters, logos, signatures or known characters; reverse image search: pending |
| `docs/design/brand/drafts/draft-4.jpg` | Reference: icon draft 4, the folded page, the owner's pick | ChatGPT image generation (owner's Pro plan) | 2026-11-08 | Prompt 4 on `docs/design/brand/README.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1024 px; checked: no letters, logos, signatures or known characters; reverse image search: pending |
| `docs/design/brand/drafts/draft-5.jpg` | Reference: icon draft 5, paper badge on blue | ChatGPT image generation (owner's Pro plan) | 2026-11-08 | Prompt 5 on `docs/design/brand/README.md` | Ours; OpenAI's terms assign output rights to the user | the owner, with ChatGPT | PNG converted to JPEG, 1024 px; checked: no letters, logos, signatures or known characters; reverse image search: pending |
| `docs/design/canvas/*.png` | Reference: the canvas mock-ups, six frames of the game in the Notebook look, light and dark, 1080 x 2400 px | Claude Code (owner's Claude plan), HTML and SVG written by `docs/design/canvas/draw.py`, rendered with Google Chrome | 2026-11-08 | Drawn from the values on `docs/design/theme-notebook.md` and `docs/design/pieces.md`; text set in the Google Fonts Caveat and Nunito; no image tool or prompt | Ours; the letter shapes are Caveat and Nunito, SIL Open Font License 1.1 | Claude for the owner | none |
| `docs/design/motion/*.gif` | Reference: the motion mock-ups, each animated moment of the game in the Notebook look, light and dark, 25 frames a second | Claude Code (owner's Claude plan), HTML and SVG written by `docs/design/motion/draw.py`, rendered with Google Chrome, made into GIFs with ffmpeg | 2026-11-08 | Drawn from the times on `docs/design/motion.md` and the canvas drawing `docs/design/canvas/draw.py`; text set in the Google Fonts Caveat and Nunito; no image tool or prompt | Ours; the letter shapes are Caveat and Nunito, SIL Open Font License 1.1 | Claude for the owner | none |
| `docs/design/motion/strip-*.png` | Reference: key frames of each animated moment, light version | Claude Code (owner's Claude plan), HTML and SVG written by `docs/design/motion/draw.py`, rendered with Google Chrome | 2026-11-08 | Drawn from the times on `docs/design/motion.md` and the canvas drawing `docs/design/canvas/draw.py`; text set in the Google Fonts Caveat and Nunito; no image tool or prompt | Ours; the letter shapes are Caveat and Nunito, SIL Open Font License 1.1 | Claude for the owner | none |
