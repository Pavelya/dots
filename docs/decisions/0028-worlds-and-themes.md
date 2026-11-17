# 0028: Worlds are chapters of one notebook; themes are what the player picks for the board

**Date:** 2026-11-17 · **Status:** accepted

## Context

Decision 0012 makes Night sky a cosmetic theme. Decision 0015 makes Night sky World 3. World 1
and the launch theme are both called Notebook. The docs also use "theme", "look", "world", "style"
and "background" loosely. So a reader can't tell whether World 3 changes the board, or whether a
theme changes the map.

Giving each world a whole look of its own was considered and dropped. Every world would need its
own board, about 10 board sounds and its own accessibility checks, and the Shop would have no
themes left to sell (decision 0004).

This record makes 0012 and 0015 clearer. Both stay as they are.

## Decision

- **Worlds are chapters of one notebook.** A world is a part of the campaign: its levels, its
  new idea (a mission type, a board shape, a booster), its boss, its map page and its music. Every
  map page is drawn the Notebook way, as ink doodles on squared paper, whatever theme the player
  picked. A world may add small board touches. It never changes the pieces, the rings, the fills
  or the sounds.
- **Themes are what the player picks for the board.** A theme sets how the board and the screen
  behind it are drawn (paper, grid, pieces, rings, fills) and the sounds made on the board. It is
  the same in every world and every mode. A theme does not change the map pages or the music.
  Notebook is the theme at launch and the default. Other themes come later as cosmetics.
- **No world and no theme share a name.** World 1 becomes **Countryside**, after the trees, pond,
  hills, cottage and castle on its map page. "Notebook" names the theme, and the notebook that
  every world is a chapter of. The cosmetic theme that 0012 calls Night sky becomes **Star
  chart**. Its mood board already calls it "a calm star chart": the grid becomes a chart and the
  rings become constellation lines. A large astronomy app on Google Play is also called Star
  Chart, but our Star chart is only the name of an item in the Shop. It is never used in Pinfold's
  store title, subtitle or keywords.

The worlds at launch: 1 Countryside, 2 Garden, 3 Night sky, 4 Sea. The themes: Notebook at
launch, then Star chart and Neon arcade.

### Words

Every doc and task card uses these words this way.

| Word | Means | Not to be confused with |
| --- | --- | --- |
| **world** | A chapter of the campaign: its levels, new idea, boss, map page and music. | A theme. A world never changes the pieces, rings, fills or sounds. |
| **theme** | What the player picks for the board: paper, grid, pieces, rings, fills and board sounds. The same in every world and mode. | A world. "Light" and "dark" are not themes. |
| **map page** | A world's part of the map: its stretch of the path and the doodles beside it, drawn the Notebook way. | A background. A world has no background picture behind the board. |
| **board touches** | A world's small additions around the board, inside the chosen theme, such as margin doodles or a slight paper tint. Never under the points. | A theme. Board touches never change the pieces, rings, fills or sounds. |
| **light and dark version** | The two versions of every theme. The phone's dark theme setting picks one. | A theme. Say "the dark version"; "dark theme" means only the phone's setting. |
| **piece style, ring effect** | Smaller cosmetics that change only the pieces or only the rings, inside the chosen theme (decision 0004). | A theme. |

Words to avoid as names for these ideas: **look**, **skin**, **background** and **style** on its
own. In plain description they are fine ("the look of every picture", "the screen background is
Paper", "text styles").

## Consequences

- World 1's name changes in the engine (`World.kt`), on the map's header and in its tests, and on
  the canvas `map` frame. That is a code change with new screenshots, so it is its own task. The
  music files `music_notebook` and `music_night_sky` keep their file names. The asset register
  says which world each one plays in.
- V-11 makes map pages and board touches, not world backgrounds. The pictures belong to the
  world, so `GameTheme.worldBackgrounds` moves out of the theme when V-11 draws them.
- V-13 makes Star chart and Neon arcade. Each brings its own board, board sounds and
  colour-blind checks.
- A new world needs a map page, music and a name that no theme uses. A new theme needs a board,
  board sounds and a name that no world uses.
