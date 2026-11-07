# Mood boards

Three looks for Pinfold, each shown as one board of pictures. The owner compares them and picks the
launch look. Decision 0012
([`docs/decisions/0012-launch-theme-and-first-boosters.md`](../../decisions/0012-launch-theme-and-first-boosters.md))
already chose Notebook. The boards confirm that choice with pictures, or show it should change.

| Board | Pieces | Feel |
| --- | --- | --- |
| [Notebook](notebook.md) | Red and blue ink dots on squared paper; rings drawn like a pen stroke | Warm, nostalgic, close to the paper game |
| [Night sky](night-sky.md) | Gold stars against ice-blue comets; rings drawn as constellation lines | Calm, elegant |
| [Neon arcade](neon-arcade.md) | Glowing magenta and cyan discs; rings as bright tubes | Energetic, bold |

Each board has the same eight pictures, so the three can be compared side by side:

| # | Picture | What to judge |
| --- | --- | --- |
| 1 | The board in play | Are the dots, the grid and the capture outlines easy to tell apart? (B02, B05) |
| 2 | A capture | Does the outline stand out more than the fill? Do the captured dots stay visible? (B07, M01) |
| 3 | A crowded late game | Is a full board still readable at phone size? (B03, F02, F03) |
| 4 | The world map | Is the next level obvious? Does the world have its own landscape? (P01, P06) |
| 5 | A world background | Does it frame the screen without fighting the board? |
| 6 | A won level | Can you still see the final board under the celebration? (M04, M05) |
| 7 | The home screen | Are three ways in clear without words? (U05) |
| 8 | Colours and type | A palette swatch drawn by us, and two Google Fonts to look at |

The codes in brackets are the references in [`references.md`](../references.md).

Pictures 1 to 7 are generated with ChatGPT from the prompts on each board page. Picture 8 is an
SVG drawn in code, because the prompt rules reject AI images that contain letters. The palettes
and fonts are first proposals. D-03 sets the final colours, with contrast checks.

## Making the pictures

1. Use ChatGPT image generation. Start a new chat for each picture, so earlier pictures do not
   steer it, and paste the prompt exactly as written. Don't add names of artists, games, studios or
   brands, and don't upload any image ([prompt rules](../../assets.md#prompt-rules)).
2. Reject any output with letters, numbers, a logo, a signature, a watermark or a recognisable
   character, and try again. If a prompt keeps failing, say so: the prompt changes, not the rules.
   A changed prompt is written back onto the board page, so the page always holds the prompt that
   made the picture.
3. Save the picture you keep as `<board>-<number>.png`, for example `notebook-1.png`, in one folder
   outside the repository. Note the date.
4. When all 21 are done, or as many as you want to send, give Claude the folder. Claude shrinks
   each one to a JPEG of about 1080 px on its long side, adds it to the board page and to the asset
   register ([`docs/assets.md`](../../assets.md)) as "Reference:", and opens the pull request.

These pictures are references only. None of them ships in the app.

## The pick

The owner's pick goes here once the boards are complete, with the reasons in a few lines.
