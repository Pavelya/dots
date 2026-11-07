# Asset register

Every image, sound, music track, font and video in this repository has a row here. The row says
where the file came from and who may use it, so any claim that we used someone else's work can be
answered in minutes. The rules come from decision 0009
([`docs/decisions/0009-asset-ownership.md`](decisions/0009-asset-ownership.md)).

## Clean sources

An asset may come only from:

- **us:** drawn, recorded, written or coded by the owner or by Claude for the owner;
- **AI tools under the prompt rules below:** ChatGPT for images, Suno Pro (while subscribed) for
  music;
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
| `app/src/main/res/mipmap-*/ic_launcher.png` | POC launcher icon, five sizes: four red dots joined in a ring around a blue dot, on paper colour | Claude Code (owner's Claude plan), drawn in code | 2026-11-04 | Simple shapes drawn in code during P-03; no image tool or prompt | Ours | Claude for the owner | none |
| `docs/images/campaign-loop.png` | Diagram of the campaign loop in the design investigation | Claude Docs diagram (owner's Claude plan), exported as PNG | 2026-11-05 | Drawn from the investigation's text; no image tool or prompt | Ours | Claude for the owner | none |
| `docs/images/plan-phase-dependencies.png` | Diagram of the phase dependencies in the plan | Claude Docs diagram (owner's Claude plan), exported as PNG | 2026-11-05 | Drawn from the plan's text; no image tool or prompt | Ours | Claude for the owner | none |
