# POC architecture and reuse

The POC is built as the first slice of the real app, not as a throwaway: game logic, app
structure, input and logging are written to production standards; only the look is
temporary. If the POC leads to "go", the full plan continues in the same modules.

## Modules

```
dots/
  engine/   pure Kotlin, no Android: rules, AI, openings, game record   (exists; reused 100%)
  app/      Android app: Jetpack Compose, Material 3                    (new in the POC)
  cli/      terminal game and benchmarks                                (exists; dev tool)
```

The engine stays free of Android code so it is tested fast on any machine, used by the CLI
benchmarks, and could later run on a server for online play.

## Inside `app`

Layers, each depending only on the ones below it:

| Layer | Package | What it holds | Kept in the full version? |
|---|---|---|---|
| Screens | `ui.screen` | Compose screens: the map, game, settings, rules, the developer menu | Layout replaced by the designed screens; routes follow [`docs/design/flows.md`](../design/flows.md) |
| Board view | `ui.board` | Draws the board on a Canvas; turns a touch into a grid cell and a placing gesture (two taps or drag, decision 0024) into game events; reads inks, sizes and piece shapes from a `BoardStyle`, which comes from the theme | Kept |
| Theme | `ui.theme` | A `GameTheme` is a look as data: inks in a light and a dark version, board sizes, pieces, text styles, sounds and world backgrounds. `DotsTheme` hands the theme to every screen | Kept; each new look is a new `GameTheme` value, with no new drawing code |
| Game control | `game` | One `GameViewModel` per game: holds the `GameState`, runs the computer on a background thread with a time limit, exposes one immutable `GameUiState` | Kept |
| Data | `data` | Settings and campaign progress (DataStore), saved game, game log file | Kept; the log feeds future analytics |

Rules the code follows (common practice for mobile games built with Compose):

1. **One direction of data:** the screen sends events (`CellTapped`, `Undo`, `Rematch`) to the
   ViewModel; the ViewModel updates one immutable state; the screen only draws that state.
2. **Logic is separate from drawing:** the engine decides everything about the game; the view
   never computes rules. This is what makes themes and animations a drawing-only change later.
3. **Nothing heavy on the main thread:** the computer thinks on a background coroutine with a
   time limit and is cancelled when the player leaves the game.
4. **Deterministic games:** every game has a seed and a move record, so any game in a tester's
   log can be replayed exactly in the CLI for analysis.
5. **Survives being killed:** the game is saved after every move; Android may close the app at
   any time.
6. **Accessible defaults:** touch targets at least 48 dp, content descriptions, system font
   scaling respected.

## Saved game and game log

Both use one format, `GameLogEntry` in the engine (`dots.log`), one line of JSON per game:

```
{"app":"0.1-poc","started":1762335720000,"updated":1762336100000,"play_ms":380000,"opponent":"normal",
 "threats":true,"undos":1,"outcome":"finished","red":5,"blue":3,"placing":"two-taps","touches":70,"placed":34,"practice":false,
 "record":"dots/1 15x15 dots=33 ... | h8 g7 ..."}
```

- `record` is the game record (settings, seed, every move), so the game replays exactly.
- `started` / `updated` are milliseconds since 1970 (UTC); `play_ms` is time spent playing, where a
  pause of over 2 minutes between moves counts as 2 minutes (the player put the phone down).
- `opponent` is `human` (two players on one phone) or the computer's strength (`steady`): a
  level's, or in free play the one the player has reached in the campaign. POC logs name the test
  difficulty (`easy`, `normal`, `hard`), strengths of the same names.
- `outcome` is `playing` (the saved game), `finished`, or `abandoned` (another game was started).
- `placing` is how dots were placed (`two-taps` or `drag`, from Settings); `touches` counts the
  players' touches on the board, and `placed` the dots they placed by touch, undone ones included.
  `cli placing-report` turns them into undos per game and taps per dot for each method. Logs from
  before these fields were added lack them.
- `practice` is true for a campaign level played as practice, such as a replay of the tutorial:
  its end changes no campaign progress. Logs from before it was added lack it, and read as false.

Files, in the app's private folder (`files/games/`, excluded from backups):

- `saved-campaign.json`, `saved-free-play.json`, `saved-two-players.json`: each mode's unfinished
  game, rewritten after every move and undo (to a temporary file first, then renamed, so a kill
  mid-write keeps the previous version). A saved game in one mode never replaces another mode's.
  A new game is written here *before* its screen opens; the game screen plays the saved game of
  its mode, so after Android kills and restarts the app it comes back to the same position, and
  its card (the Level card, or Play a game) opens it too. A damaged file is ignored rather than
  crashing the app. The one `current-game.json` of older versions becomes the saved game of its
  mode.
- `game-log.txt`: one line per finished or abandoned game (abandoned only if it had moves).

`GameStore` writes both on one background thread, in order, so the main thread never waits for
the disk. Testers share the log from the developer menu, "Share the game log" (Android share sheet, through
a `FileProvider` that exposes only this folder). No network access and no identifiers: the
tester's name is known from who sends the file.

`cli log-check <files>` replays every game in shared logs and flags any whose logged result the
replay does not give.

## Build, test, CI

- Gradle version catalog (`gradle/libs.versions.toml`) for all versions, kept current by Dependabot PRs; `minSdk` 24 (Android 7); `compileSdk` and `targetSdk` 37, the newest Android.
- CI (GitHub Actions) on every pull request and push to `main`: engine and CLI tests, app unit tests (ViewModel, input
  mapping, saved game and log), screenshot tests of the board (Robolectric + Roborazzi), lint, debug APK build.
- A tagged commit (`poc-v0.1`, ...) builds the test APK and attaches it to a GitHub Release.
- App tests run on Robolectric, pinned to the Android 36 runtime in
  `app/src/test/resources/robolectric.properties` until Robolectric supports Android 37 (the
  app's target). Where Maven Central is rate-limited, `ROBOLECTRIC_REPO_URL` points Robolectric's
  own download at a mirror; CI does not need it.
- The build runs on JDK 21: Robolectric needs it for the Android 35+ runtimes. Code is still
  compiled to Java 17 bytecode.
- Lint treats warnings as errors, so a new warning fails the build instead of piling up.

## What is deliberately temporary

Grey visuals, the one-screen rules card, the settings layout. Nothing in the temporary parts is
depended on by the kept parts.
