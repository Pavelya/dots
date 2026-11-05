# Dots — Plan to the Android app

> **Copy of a Claude Doc.** The working copy is the [plan doc](https://claude.ai/artifact/9GaGRbXg9qwJsYMswwE7ZF); this file is a snapshot of it, exported on 2026-11-05 and refreshed whenever the doc changes. Status values appear as plain text here.

Nov 5, 2026 · @Pavel

The plan takes the POC to a free-to-play Android game in 10 phases and 83 tasks, rewritten on 2026-11-05 after the [design investigation](https://claude.ai/artifact/DyiSkrrx3hwARfRpyxNGRP), whose recommendations the owner accepted in full. It replaces the parked 56-task plan.

**What changed from the old plan:**

- **Done in the POC:** project setup, the app skeleton, board, placement, playing a game, threat highlight, resume, and the game log (P-01 to P-08). Their full-plan tasks are marked done or folded into the POC record.
- **New phases:** name and brand (the app cannot be called "Dots": that is another company's game trademark), game design for missions, boosters and bosses, and money (cosmetics and rewarded ads; nothing that buys an advantage).
- **New in existing phases:** campaign worlds with stars and titles, a daily puzzle with streaks and sharing, an interactive tutorial, the Notebook launch theme, recorded sound effects, Suno music, the asset register, and the Play closed test.
- **Online play** stays after launch: friends turn by turn first, ranked only once there are enough players.

**The game in one paragraph:** the classic paper game on a 15x15 board with four starting crosses. Solo play is a campaign of worlds; each world teaches one new idea (a mission type, a board shape, a booster) and ends with an announced boss. Difficulty comes from the levels, not a picker. Two people can play on one phone, and later online. It looks like ink on squared paper at launch, with other themes as cosmetics. It earns from cosmetics and optional rewarded ads.

**Order of work:** finish the POC test week; meanwhile pick a name and make the design references and canvas. The design gate (owner approves the look) comes before any visual coding. Engine work for missions and boosters can start right after the POC, in parallel with design.

## How to use this plan

Every task is a card that an AI agent or a person can pick up alone. Each card has:

- **ID:** phase letter and number. P POC, B brand, D design, G game design and engine, A app, V visuals and sound, C money, O online, Q quality, R release.
- **Goal:** the outcome in one sentence.
- **Depends on:** tasks that must be Done first.
- **Steps:** the work, in short.
- **Checks:** tests, measurements or reviews to run.
- **Done when:** the result anyone can verify, with numbers where possible.
- **Verified by:** *Agent* (checkable from the repository, CI or a script), *Human* (needs eyes, a phone or taste), or both.

Working rules:

1. One task is one pull request when it changes code; CI must be green before merge.
2. Design files go under `docs/design/`, decisions under `docs/decisions/` (one short file per decision), the asset register at `docs/assets.md`.
3. A task is Done only when every "Done when" line holds. Status lives in the tracker near the end of this doc.
4. Numbers in "Done when" are starting targets. Change them here, not silently in code.
5. Every image, sound or font enters the repository with a row in the asset register (V-01), or it does not enter.
6. Engine code stays free of Android code, so rules run the same in the app, the CLI and a future server.

## Decisions already made

These are settled; tasks build on them. Changing one means a new decision record in `docs/decisions/`.

| ID | Decision | Made |
| --- | --- | --- |
| D-POC-1 | App ID `io.github.pavelya.dots` for the POC; the store app gets its final ID with the new name (B-03) | 2026-11-04 |
| D-POC-2 | Test builds go out as an APK on a GitHub Release | 2026-11-04 |
| D-POC-3 | 5 to 10 testers, at least 2 who knew the paper game and 2 who did not | 2026-11-04 |
| 0003 | Difficulty comes from level progression, not a difficulty picker | 2026-11-05 |
| 0004 | Free to play: cosmetic purchases and optional rewarded ads; nothing that buys an advantage | 2026-11-05 |
| 0005 | Audience: casual adults; tone playful, not childish | 2026-11-05 |
| 0006 | Boosters only in the solo campaign; earned, never sold; two-player, online and ranked games use pure rules | 2026-11-05 |
| 0007 | Languages: English first, then Russian, Ukrainian and Polish | 2026-11-05 |
| 0008 | Budget: free tools, plus the $25 Play account and one month of a paid Suno plan | 2026-11-05 |
| 0009 | Ownership: being copied is acceptable; every asset has a clean, recorded source so no one can claim we used theirs | 2026-11-05 |
| 0010 | Our own name; "Dots" only as a descriptive word | 2026-11-05 |
| 0011 | Android first; assets made ready for iOS | 2026-11-05 |
| 0012 | Launch theme Notebook (ink on squared paper); first boosters Wall, Hint, Bomb, Double dot | 2026-11-05 |
| 0013 | Music from one Suno Pro month; sound effects recorded or generated by us | 2026-11-05 |
| 0014 | Multiplayer order: same phone, then online with friends turn by turn, ranked only later | 2026-11-05 |
| 0015 | Launch with about 75 levels in 4 worlds, plus a daily puzzle | 2026-11-05 |
| 0016 | The game is called Pinfold; the store app ID will be io.github.pavelya.pinfold (POC test builds keep theirs) | 2026-11-05 |
| 0017 | The repository is public; secrets live only in Actions secrets; the old committed test key is burned | 2026-11-05 |

Decisions 0004 to 0015 come from the [design investigation](https://claude.ai/artifact/DyiSkrrx3hwARfRpyxNGRP); their record files are added to `docs/decisions/` in the repository together with this plan.

## Where we are

The code is in [Pavelya/dots](https://github.com/Pavelya/dots), branch `claude/gifted-tesla-dmmchd`; CI builds the debug APK on every push.

| Part | State |
| --- | --- |
| `engine` (pure Kotlin) | Rules with captures, grounding and dot budgets; openings incl. four random crosses; game record that replays exactly; undo; computer opponent (Easy, Normal, Hard) with a time limit and cancelling; threat finder and the public hints API; game log format; 8 sample campaign levels; 76 tests |
| `cli` | Terminal game, benchmarks (tiers, levels, duel, variants), `log-check` for tester logs; 3 tests |
| `app` | Home, board with tap-to-preview placement (12, 15, 20 points), games against the computer and two players on one phone, undo, result screen, threat highlight, rules screen with pictures, settings, resume after the app is closed, game log shared from Settings; 38 tests incl. screenshot tests; lint with warnings as errors |
| `docs/poc/` | Charter (approved), architecture, tasks P-01 to P-10, test protocol |
| Owner checks | App installed and both modes played on a phone (2026-11-05) |

**Still missing:** a real name, the designed look, campaign and meta, sound, money, online play, the store release. The rest of this plan covers them.

## Tools, accounts and configuration

Everything is free except the one-time Play account ($25) and one month of Suno Pro (about $10, approximate). Ownership notes per tool are in the design investigation, section 8.

### Tools

| Tool | Used for | Cost | First needed in |
| --- | --- | --- | --- |
| Android Studio (stable) with its JDK, SDK and emulator | Build, run, debug, profile | Free | A-01 |
| Git, GitHub Actions | Version control, CI, APK builds | Free | Exists |
| A real Android phone, plus an old cheap one | Touch, feel, speed on low-end hardware | Own devices | Exists / Q-02 |
| ChatGPT image generation | Mood boards, concept art, backgrounds, portraits | Owner's Pro plan | D-02 |
| Figma (free plan); Penpot as a fallback | Wireframes, canvas mock-ups, clickable prototype | Free | D-06 |
| Krita, Inkscape, GIMP | Clean-up, piece shapes as SVG, icons, textures | Free | D-04 |
| Google Fonts (OFL), Material Symbols (Apache 2.0) | Type and interface icons | Free | D-03 |
| WebAIM Contrast Checker, Coblis, Android "Simulate color space" | Contrast and colour-blindness checks | Free | D-04 |
| Google Lens, TinEye | Reverse image search for key art | Free | V-01 |
| Audacity, a phone recorder, jsfxr | Recording and editing sound effects | Free | V-08 |
| Kenney packs, Freesound (CC0 only) | Extra sound effects | Free | V-08 |
| Suno Pro (one month) | Music for menu, worlds and boss | About $10 once | V-09 |
| A song-recognition app (for example Shazam) | Check AI music does not match a known song | Free | V-09 |
| Robolectric, Compose UI test, Roborazzi | Unit, UI and screenshot tests (in use) | Free | Exists |
| Macrobenchmark, Baseline Profiles, Profiler | Frame time, start-up, AI speed | Free | Q-02 |
| Accessibility Scanner (Play app) | Accessibility audit on the phone | Free | Q-03 |
| Firebase Emulator Suite | Testing online rules locally | Free | O-03 |
| Shotcut or DaVinci Resolve | Store video | Free | R-05 |
| Bitwarden (or another password manager) | Account and signing-key passwords | Free | B-05 |

### Accounts

| Account | Why | Cost | When |
| --- | --- | --- | --- |
| GitHub (exists) | Code, CI, releases | Free | Now |
| A Google account just for the game | Owns Play Console, Firebase, AdMob, Play Games | Free | B-05 |
| A support email address on that account | Shown in the store, answers players | Free | B-05 |
| Figma | Design files | Free | D-06 |
| Freesound | Downloading CC0 sounds | Free | V-08 |
| Suno (Pro for one month, then cancel) | Music with commercial rights | About $10 | V-09 |
| Google Play Console, personal, with identity check | Publishing; Play Games Services; in-app purchases | $25 once | R-01 |
| Google payments profile (merchant) | Selling cosmetics | Free; Google keeps a share of each sale | C-02 |
| AdMob | Rewarded ads | Free | C-03 |
| Firebase project (Spark plan first) | Crash reports, analytics, online play | Free; Blaze only if server checks need it | Q-05 / O-01 |

### Configuration

- **Repository:** `docs/design/` for design files, `docs/decisions/` for decisions, `docs/assets.md` for the asset register, a pull-request template.
- **Build:** the existing version catalog, `minSdk` 24, `targetSdk` the newest Play requires; a release build type with code shrinking.
- **CI (in use):** engine and CLI tests, app and screenshot tests, lint, debug APK on every push; later a release build on tags.
- **Secrets, only at release:** the upload keystore and passwords as GitHub Actions secrets, with an offline backup; Play App Signing keeps the real signing key.
- **Web pages:** a privacy policy and a small game page on GitHub Pages (needs a public repository on the free plan) or Google Sites.

## Phase 0: finish the POC

The POC answers whether people enjoy the core game enough to come back. Its tasks live in [`docs/poc/tasks.md`](https://github.com/Pavelya/dots/blob/claude/gifted-tesla-dmmchd/docs/poc/tasks.md); two remain. Phases 1 and 2 need no code and run in parallel.

### P-09 Test build and analysis script

- **Goal:** testers install from one link with a stable signing key, and their logs are analysed in one command.
- **Depends on:** P-05 to P-08 (done).
- **Steps:** a fixed debug signing key stored as a CI secret, so updates install over each other; a workflow that builds on tag `poc-v0.1` and attaches the APK to a GitHub Release; `cli poc-report <logs>` printing every metric of the test protocol.
- **Checks:** install `poc-v0.1`, then `poc-v0.2`, over it on one phone; run `poc-report` on a synthetic log with known answers.
- **Done when:** the release page has the APK, an update installs without uninstalling, and `poc-report` prints days played, games, game length, share with a winner, win rate per difficulty, undos per game and captures with the threat highlight on and off.
- **Verified by:** Agent (build, script) and Human (installs from the link on a second phone).

### P-10 Test week and decision

- **Goal:** answer the POC question with numbers.
- **Depends on:** P-09; 5 to 10 testers (D-POC-3).
- **Steps:** run the week as in the test protocol; collect logs and survey answers; write `docs/poc/results.md`; record go, change or stop.
- **Checks:** every hypothesis H1 to H6 scored with its numbers; `cli log-check` passes on every log.
- **Done when:** the results file and a decision record exist. A "change" result names which campaign or design tasks below move first.
- **Verified by:** Human (owner).

**Note:** the testers of P-10 can also be the 12 testers Play requires for the closed test (R-06), so recruit with that in mind.

## Phase 1: name and brand

Phase 1 gives the game a name nobody else owns, its permanent app ID, and its logo. "Dots" is a registered game trademark of another company, so it can only describe the game, not name it.

### B-01 Name candidates

- **Goal:** about 20 name candidates worth checking.
- **Depends on:** nothing.
- **Steps:** brainstorm along three lines: enclosing (encircle, corral, lasso), the paper origin (ink, margin, notebook), and invented soft-sounding words; for each, how it sounds in English, Russian, Ukrainian and Polish.
- **Checks:** no candidate is "Dots" plus a common word; none means something rude or silly in the four languages.
- **Done when:** `docs/design/names.md` lists 20 candidates with a one-line reason each.
- **Verified by:** Agent (list) and Human (owner reads it).

### B-02 Name checks

- **Goal:** know which candidates are free to use.
- **Depends on:** B-01.
- **Steps:** for each candidate, search Google Play, the USPTO, EUIPO and WIPO Global Brand Database (classes 9, 28 and 41), a domain registrar (.com, .app, .game), and social handles.
- **Checks:** each result recorded with the date and a link to the search.
- **Done when:** a table in `names.md` marks each candidate clear, risky or taken, with at least 5 clear ones.
- **Verified by:** Agent (searches) and Human (spot-checks two).

### B-03 Pick the name and the app ID

- **Goal:** the final name and the permanent application ID.
- **Depends on:** B-02, and ideally D-02 (the name should fit the look).
- **Steps:** the owner picks from the clear candidates; the ID becomes `io.github.pavelya.<name>` (or a domain-based one if a domain is bought); a store subtitle uses "dots" descriptively.
- **Checks:** a last trademark and store search on the day of the decision.
- **Done when:** decision record written; the ID is set in the app before the first Play upload (it can never change after).
- **Verified by:** Human (owner).

### B-04 Logo and app icon

- **Goal:** a logo and an adaptive app icon that are clean and ours.
- **Depends on:** B-03, D-03.
- **Steps:** drafts with ChatGPT under the prompt rules (V-01); a person redraws the chosen one as a vector in Inkscape; adaptive icon layers (foreground, background, monochrome) and the 512 px store icon.
- **Checks:** reverse image search of the drafts and the final; the icon read at 48 px on a home screen and in the Play listing; legible in the monochrome themed-icon mode.
- **Done when:** SVG sources and exports in `docs/design/brand/`, rows in the asset register, icon live in the app.
- **Verified by:** Agent (files, register) and Human (looks right on a phone).

### B-05 Accounts and handles

- **Goal:** the game's own accounts exist before anything is published.
- **Depends on:** B-03.
- **Steps:** a Google account for the game, a support email, the domain if wanted, social handles if wanted; passwords in the password manager, with two-factor sign-in.
- **Checks:** sign in to each from a second device.
- **Done when:** a private list of accounts (not in the repository) and two-factor on every one.
- **Verified by:** Human (owner).

## Phase 2: design references and canvas

Phase 2 ends with the owner seeing how the game will look and feel, as still pictures and a clickable prototype, before any visual code is written. Everything lands in `docs/design/`.

### D-01 References

- **Goal:** a shared picture of what "good" looks like.
- **Depends on:** nothing.
- **Steps:** collect 20 to 30 screenshots of well-made puzzle and board games on mobile (board clarity, level maps, capture or match moments, result screens), plus photos of the paper game; one line each on what to learn. Screenshots are kept as private references only, never as AI input or assets.
- **Checks:** every reference has its source noted.
- **Done when:** `docs/design/references.md` with links and notes, grouped by board, map, moment, menus.
- **Verified by:** Human (owner reviews).

### D-02 Mood boards, three directions

- **Goal:** the owner picks the look from three concrete options.
- **Depends on:** D-01, V-01 (prompt rules).
- **Steps:** with ChatGPT, one board of 6 to 9 images each for Notebook (recommended), Night sky and Neon arcade: the board, a capture, a level map, a background; colours and type samples.
- **Checks:** prompt rules followed (no artist, game or brand names); every image logged in the asset register as "reference".
- **Done when:** three boards in `docs/design/moodboards/` and the owner's pick recorded (expected: Notebook, decision 0012).
- **Verified by:** Human (owner).

### D-03 Notebook theme specification

- **Goal:** exact rules for the launch look.
- **Depends on:** D-02.
- **Steps:** paper colour and grid, ink colours for the two sides, line weights, the ring stroke style, shading of captured areas, fonts (Google Fonts), light and dark versions.
- **Checks:** text contrast at least 4.5:1, dots against paper at least 3:1 (WebAIM).
- **Done when:** `docs/design/theme-notebook.md` with every colour as a hex value and every size in dp.
- **Verified by:** Agent (contrast numbers) and Human (taste).

### D-04 Piece shapes and colour-blind safety

- **Goal:** the two sides differ by shape as well as colour.
- **Depends on:** D-03.
- **Steps:** pairs such as round and diamond, or filled and ring; a captured-dot look; a last-move marker; SVG drawings at 1x and 4x.
- **Checks:** Coblis or Android's colour-space simulation for the three common types of colour blindness; a 15x15 board at phone size.
- **Done when:** the pair is distinguishable in greyscale and in all three simulations at 4 mm per point.
- **Verified by:** Agent (simulation images) and Human (on a phone).

### D-05 Screens and flows

- **Goal:** the full list of screens and how a player moves between them.
- **Depends on:** nothing.
- **Steps:** home, world map, level card, game, result, boosters, daily puzzle, two players, online lobby (later), shop, settings, rules and tutorial, stats; first launch and returning player flows.
- **Checks:** every feature in this plan has a screen; nothing needs more than 2 taps from home except settings details.
- **Done when:** `docs/design/flows.md` with a flow diagram and one line per screen.
- **Verified by:** Human.

### D-06 Wireframes

- **Goal:** the layout of every screen, without styling.
- **Depends on:** D-05.
- **Steps:** grey boxes in Figma at 360 x 800 dp and a small phone (320 dp wide); thumb reach for the main buttons.
- **Checks:** touch targets at least 48 dp; the board as large as possible on the game screen.
- **Done when:** one frame per screen, linked from `flows.md`.
- **Verified by:** Human.

### D-07 Canvas: the game in the chosen look

- **Goal:** still pictures of how the game will really look.
- **Depends on:** D-03, D-04, D-06.
- **Steps:** full mock-ups of the game screen (start, mid-game with a threat, right after a big capture), the world map, a level card, a result with stars, the home screen; light and dark.
- **Checks:** a 15x15 board shown at real phone size; text at real size.
- **Done when:** the mock-ups are exported to `docs/design/canvas/` and shared with the owner.
- **Verified by:** Human (owner).

### D-08 Motion storyboard

- **Goal:** every animation described before it is built.
- **Depends on:** D-07.
- **Steps:** frames and timings for placing, chain lines, the capture (ring traced, fill, captured dots, +N to the score, big-capture version), threat pulse, each booster, end of game, star reveal, level unlock.
- **Checks:** no animation blocks the next tap for more than 0.3 s; a big capture under 1.5 s and skippable.
- **Done when:** `docs/design/motion.md` with a timing table, plus a short video or GIF made from the mock-ups.
- **Verified by:** Human.

### D-09 Sound list

- **Goal:** every sound named before it is made.
- **Depends on:** D-08.
- **Steps:** board sounds, booster sounds, moments, interface sounds and music cues, each with length, mood and source plan (recorded, generated, CC0, Suno).
- **Checks:** every animation moment in D-08 has a sound or a deliberate silence.
- **Done when:** `docs/design/sound.md` with about 35 effects and 6 music pieces listed.
- **Verified by:** Human.

### D-10 Clickable prototype

- **Goal:** the owner taps through the game before it is built.
- **Depends on:** D-07.
- **Steps:** Figma prototype linking home, world map, level card, game (a few fixed frames of one level), result and back.
- **Checks:** opened on a phone in the Figma app.
- **Done when:** the prototype link is in `flows.md` and works on a phone.
- **Verified by:** Human.

### D-11 Five-second tests

- **Goal:** strangers understand the screens at a glance.
- **Depends on:** D-07.
- **Steps:** show 5 people each key screen for 5 seconds; ask what the game is, whose turn it is, what just happened.
- **Checks:** answers written down word for word.
- **Done when:** at least 4 of 5 people answer each question right; fixes made where they did not.
- **Verified by:** Human.

### D-12 Design gate

- **Goal:** the owner approves the look, so visual coding can start.
- **Depends on:** D-08 to D-11, B-03.
- **Steps:** review canvas, motion, sound list and test results together.
- **Checks:** none open from D-11.
- **Done when:** a decision record "design approved" in `docs/decisions/`. V tasks that draw anything start only after it.
- **Verified by:** Human (owner).

## Phase 3: game design and engine

Phase 3 turns the paper game into a campaign: levels as data, six mission types, four boosters, boss rules and special board points, all in the engine with tests, plus the 75 launch levels and a daily puzzle. It needs no visuals, so it starts right after the POC.

### G-01 Level format

- **Goal:** a level is a data file the engine, the app and the CLI all read.
- **Depends on:** P-10 (go).
- **Steps:** one text format per level: board size and shape, blocked and bonus points, opening, the computer's head start, its strength, dots per side, mission, allowed boosters, special rule, star thresholds, world and number. Replace the 8 hard-coded sample levels.
- **Checks:** every field parsed and written back identically; a bad file gives a clear error with the line.
- **Done when:** levels load from `engine/src/main/resources/levels/`; round-trip test on all levels; the CLI plays any level by file.
- **Verified by:** Agent.

### G-02 Mission types

- **Goal:** six ways to win a level besides "more captures".
- **Depends on:** G-01.
- **Steps:** capture N dots in M moves; survive M moves without losing a dot; rescue a marked group; capture one marked target dot; win by a margin; win with fewer dots than the computer. Each reports progress ("3 of 5 captured, 2 moves left") and success or failure as early as it is certain.
- **Checks:** unit tests per mission on hand-made positions, including the first move that decides it.
- **Done when:** all six pass their tests and are playable in the CLI.
- **Verified by:** Agent.

### G-03 Puzzle checker

- **Goal:** no puzzle ships unsolvable or trivially easy.
- **Depends on:** G-02.
- **Steps:** a solver that searches the player's moves against the computer's best replies for short missions (up to about 5 moves); reports whether a solution exists, how many first moves work, and the shortest solution.
- **Checks:** known solvable and unsolvable test puzzles; a time cap per puzzle.
- **Done when:** `cli check-levels` runs the solver on every mission level and fails if any is unsolvable or has more than 3 winning first moves (unless marked "open").
- **Verified by:** Agent.

### G-04 Boosters in the engine

- **Goal:** Wall, Hint, Bomb and Double dot as exact rules.
- **Depends on:** G-01.
- **Steps:** Wall blocks one empty point; Hint returns the computer's best move for the player; Bomb removes the opponent's uncaptured dots in a plus shape of 5 points, never inside captured areas; Double dot lets the player place 2 dots in one turn. At most one booster per turn; counts per level from the level file. Game records include booster actions so games still replay exactly.
- **Checks:** unit tests for each, including edge cases (bomb at the border, double dot that closes two rings, wall on a closing point).
- **Done when:** all boosters pass, records with boosters replay identically, and the computer reacts sensibly after each (no crash, legal move).
- **Verified by:** Agent.

### G-05 Boss rules

- **Goal:** announced boss levels where the computer bends a rule.
- **Depends on:** G-01.
- **Steps:** "double move every Nth turn", "starts with a fortress" (a large head start), "plays two groups at once"; the engine exposes the next special turn so the app can show a countdown.
- **Checks:** tests that the double move happens exactly on schedule and never otherwise.
- **Done when:** each rule is selectable in a level file and passes its tests.
- **Verified by:** Agent.

### G-06 Board shapes and special points

- **Goal:** levels look and play differently.
- **Depends on:** G-01.
- **Steps:** holes and odd outlines (points out of play), blocked points, bonus points worth 2 when captured; capture and grounding rules defined for each (a hole's edge counts as border or not, chosen per level).
- **Checks:** capture tests around holes and bonus points; replay tests.
- **Done when:** the rules are written in the README and covered by tests.
- **Verified by:** Agent.

### G-07 Computer that errs like a person

- **Goal:** weaker levels feel like a weaker player, not a random one.
- **Depends on:** nothing (engine only).
- **Steps:** replace random mistakes with human-like ones: miss threats farther than K points away, overlook a ring that needs 2 more dots, prefer attack over defence; tune per strength step.
- **Checks:** `cli bench` shows at least 5 distinct strength steps with significant gaps (sign test p < 0.05 between neighbours).
- **Done when:** the strength steps are documented in the README with their bench results.
- **Verified by:** Agent.

### G-08 Stars

- **Goal:** every level can be passed with 1, 2 or 3 stars.
- **Depends on:** G-02.
- **Steps:** per mission, a star rule: margin of victory, moves to spare, dots saved, boosters unused; thresholds stored in the level file.
- **Checks:** for each level, the bench stand-ins show 3 stars are reachable but rarer than 1.
- **Done when:** star results are part of the game result and covered by tests.
- **Verified by:** Agent.

### G-09 World 1 levels

- **Goal:** 15 levels that teach the game from zero.
- **Depends on:** G-02, G-03, G-08.
- **Steps:** design on paper first: levels 1 to 3 teach placing, chains, the first capture; 4 to 10 introduce capture missions; 11 to 15 full small games; a mini-boss at 15. Then write the files.
- **Checks:** `check-levels` passes; a newcomer plays 1 to 5 without help.
- **Done when:** 15 level files merged, each with a one-line design note.
- **Verified by:** Agent (checks) and Human (newcomer test).

### G-10 Worlds 2 to 4

- **Goal:** 60 more levels, each world teaching one new idea.
- **Depends on:** G-04, G-05, G-06, G-09.
- **Steps:** World 2 Garden: survive and rescue, the Wall booster; World 3 Night sky: board shapes, Bomb and Double dot; World 4 Sea: blocked and bonus points, the first real boss. A saw-tooth curve: a hard level followed by two easier ones.
- **Checks:** `check-levels` passes; the bench curve (G-11) rises with the planned dips.
- **Done when:** 75 level files in total, with design notes.
- **Verified by:** Agent and Human.

### G-11 Difficulty curve

- **Goal:** know how hard each level is before people play it.
- **Depends on:** G-07, G-10.
- **Steps:** extend `cli bench levels` to all levels with three stand-ins (weak, average, strong); chart pass rate and stars per level.
- **Checks:** no level that the average stand-in passes less than 20% of the time without a deliberate "wall" note; no world with two such walls in a row.
- **Done when:** the chart and the per-level numbers are in `docs/design/difficulty.md`, and outliers are fixed or explained.
- **Verified by:** Agent.

### G-12 Daily puzzle generator

- **Goal:** a fresh, checked puzzle every day, the same for everyone.
- **Depends on:** G-03.
- **Steps:** generate candidate positions from the date as the seed; keep those the solver rates as one clear solution of 2 to 4 moves; produce a year of puzzles ahead; a spoiler-free share text (emoji grid of moves used).
- **Checks:** 365 generated puzzles all pass the solver; the same date gives the same puzzle on two machines.
- **Done when:** the generator and a year of puzzles are in the repository, with tests.
- **Verified by:** Agent.

### G-13 Capture details for animation

- **Goal:** the app gets everything it needs to animate a capture.
- **Depends on:** nothing.
- **Steps:** for each capture, the ring's points in drawing order, the enclosed area's points, the captured dots, and the score change; the same for booster effects.
- **Checks:** tests that the ring path is closed, follows only the capturer's dots, and surrounds exactly the area.
- **Done when:** `MoveResult` carries the path and is covered by tests.
- **Verified by:** Agent.

## Phase 4: app: campaign and meta

Phase 4 builds the screens around the board on top of the POC app: world map, level card, results, boosters, tutorial, daily puzzle, progress. It uses the plain POC look until the V tasks restyle it, so it can start before the design gate.

### A-01 Navigation for the full game

- **Goal:** the app's screens and navigation match the D-05 flows.
- **Depends on:** D-05, B-03 (final app ID).
- **Steps:** routes for every screen; the POC's difficulty picker and test-only settings move behind a hidden developer menu; the final app ID and name.
- **Checks:** a navigation test visits every screen and returns home; back always does what the flow says.
- **Done when:** the navigation test passes and the POC features still work.
- **Verified by:** Agent.

### A-02 World map

- **Goal:** the player sees their path through the campaign.
- **Depends on:** A-01, G-01.
- **Steps:** a scrolling map per world with level nodes, stars earned, the next level highlighted, locked worlds with the stars needed.
- **Checks:** screenshot tests for a new player, mid-world and a completed world; 75 nodes scroll smoothly.
- **Done when:** tests pass and the map opens the right level card.
- **Verified by:** Agent and Human.

### A-03 Level card and result

- **Goal:** a player knows the goal before playing and what they earned after.
- **Depends on:** A-02, G-02, G-08.
- **Steps:** the card shows the mission in one line, a small board picture, the computer as a face for its strength, allowed boosters, star rules; the result shows stars, score, "next" and "retry"; a hint or booster offer after 3 failures.
- **Checks:** UI tests for pass, fail and 3-failure paths; mission text for every mission type.
- **Done when:** a level can be chosen, played, passed or failed, and retried in under 2 taps.
- **Verified by:** Agent.

### A-04 Progress and unlocks

- **Goal:** progress is saved and never lost.
- **Depends on:** A-03.
- **Steps:** stars per level, unlocked worlds, player title (Novice to Grandmaster), booster counts, cosmetics owned; saved on the phone like the POC's game store; backup through Android's backup (now allowed for progress, never for logs).
- **Checks:** a restart and an app update keep everything; a reinstall with backup restores progress.
- **Done when:** tests for save, load, upgrade from an older format, and corrupted data.
- **Verified by:** Agent.

### A-05 Boosters in the app

- **Goal:** boosters are easy to see, choose and cancel.
- **Depends on:** A-03, G-04.
- **Steps:** a booster bar with counts; choose, see where it will act (preview), confirm or cancel; one per turn; first use of each shows a one-line explanation.
- **Checks:** UI tests per booster, including cancel and "none left".
- **Done when:** all four boosters work in a level and in the CLI-replayed record.
- **Verified by:** Agent and Human.

### A-06 Tutorial

- **Goal:** a newcomer learns by playing, without reading the rules screen.
- **Depends on:** G-09.
- **Steps:** World 1 levels 1 to 3 run with guided steps: "tap here", "close the ring", one sentence each; can be replayed from settings; the rules screen stays as a reference.
- **Checks:** 3 people who never played finish levels 1 to 5 alone and explain captures correctly.
- **Done when:** the guided levels pass UI tests and the newcomer check.
- **Verified by:** Agent and Human.

### A-07 Daily puzzle and streak

- **Goal:** a reason to open the game every day.
- **Depends on:** G-12, A-04.
- **Steps:** the daily puzzle on home; a streak counter with one free "freeze" per week; share the result as text through the share sheet; works offline.
- **Checks:** tests with a fake clock across midnight, time-zone changes and a missed day.
- **Done when:** the streak behaves correctly in all clock tests and the share text has no spoilers.
- **Verified by:** Agent.

### A-08 Free play and two players

- **Goal:** the POC's free games become a tidy mode.
- **Depends on:** A-01.
- **Steps:** free play against the computer at the strength the player has reached in the campaign (decision 0003), with board size and length to choose; two players on one phone with pure rules; resume kept.
- **Checks:** the existing POC tests still pass.
- **Done when:** both modes reachable from home and their games resume.
- **Verified by:** Agent.

### A-09 Settings

- **Goal:** the player controls sound, feel and comfort.
- **Depends on:** A-01.
- **Steps:** music and effects volume, vibration on or off, high-contrast pieces, threat highlight, language, replay tutorial, privacy choices (ads consent), credits and licences.
- **Checks:** every setting survives a restart; the licences screen lists every font and CC0 source.
- **Done when:** tests pass for each setting.
- **Verified by:** Agent.

### A-10 Languages

- **Goal:** the game in English, Russian, Ukrainian and Polish.
- **Depends on:** A-09, all screens in place.
- **Steps:** all text in resources (already so); translations by a fluent speaker per language (machine drafts allowed, a person checks); plural forms; longer words checked in layouts.
- **Checks:** screenshot tests of key screens in each language; lint for missing translations.
- **Done when:** no missing string in any language and no clipped text in screenshots.
- **Verified by:** Agent and Human (one speaker per language).

### A-11 Stats

- **Goal:** players see their own progress.
- **Depends on:** A-04.
- **Steps:** games played, won, captures, best capture, longest streak, stars per world.
- **Checks:** numbers match a scripted history in tests.
- **Done when:** the stats screen passes its tests.
- **Verified by:** Agent.

### A-12 Rate and feedback prompts

- **Goal:** happy players leave reviews; unhappy ones tell us first.
- **Depends on:** A-03.
- **Steps:** Google's in-app review prompt after a 3-star level, at most once a month; a "send feedback" item in settings that opens an email.
- **Checks:** the prompt is never shown after a loss.
- **Done when:** tests for the trigger rules.
- **Verified by:** Agent.

## Phase 5: visuals, animation and sound

Phase 5 makes the game look and feel the way the canvas promised. V-01 starts at once (it guards every asset); the drawing tasks start after the design gate (D-12).

### V-01 Asset register and prompt rules

- **Goal:** every asset can be traced to a clean source.
- **Depends on:** nothing.
- **Steps:** `docs/assets.md` with a row per asset: file, what it is, tool and plan, date, prompt or source link, licence, author, edits. Prompt rules: no artist, studio, game or brand names; no uploaded images we do not own; reject outputs with logos, letters, signatures or known characters; reverse image search for key art. A CI check that every file under the app's art and sound folders has a register row.
- **Checks:** the CI check fails on an unregistered test file.
- **Done when:** the register, the rules and the CI check are merged.
- **Verified by:** Agent.

### V-02 Theme system

- **Goal:** a theme is data, so new themes need no new code.
- **Depends on:** D-12.
- **Steps:** grow the POC's `BoardStyle` into a theme: board paper, grid, piece drawings per side, ring stroke, area fill, background per world, fonts, sounds; light and dark.
- **Checks:** screenshot tests render the same position in two themes.
- **Done when:** the board, map and result screens read everything from the theme.
- **Verified by:** Agent.

### V-03 Notebook theme

- **Goal:** the launch look, as specified in D-03.
- **Depends on:** V-02, D-03, D-04.
- **Steps:** squared paper, ink pieces, pen-stroke rings, captured-area hatching, typography, all screens restyled.
- **Checks:** screenshot tests against the canvas (D-07) side by side; contrast numbers from D-03.
- **Done when:** the owner signs off the screenshots as matching the canvas.
- **Verified by:** Agent and Human.

### V-04 Pieces and accessibility

- **Goal:** the D-04 shapes in the game, with a high-contrast option.
- **Depends on:** V-03.
- **Steps:** both piece shapes as vectors; captured and last-move looks; high-contrast setting.
- **Checks:** the simulations from D-04 on real screenshots.
- **Done when:** sides are told apart in greyscale screenshots.
- **Verified by:** Agent and Human.

### V-05 Animations

- **Goal:** the D-08 moments feel alive without slowing play.
- **Depends on:** V-03, D-08, G-13.
- **Steps:** placing, chain lines, the capture (ring traced along the G-13 path, area fill, captured dots, +N), big-capture version, threat pulse, end-of-game sequence, star reveal, level unlock; all reduced when the phone's "remove animations" setting is on.
- **Checks:** Compose tests that step the animation clock and screenshot key frames; Macrobenchmark frame timing during a capture on a mid-range phone; one game with animations off.
- **Done when:** timings within 10% of D-08, under 5% slow frames during a capture, the game correct with animations off.
- **Verified by:** Agent and Human (feel).

### V-06 Booster effects

- **Goal:** each booster has its own short, clear effect.
- **Depends on:** V-05, A-05.
- **Steps:** wall placed, hint glow, bomb blast, double-dot sparkle, as in D-08.
- **Checks:** key-frame screenshot tests per booster.
- **Done when:** all four effects pass and stay under 1.5 s.
- **Verified by:** Agent and Human.

### V-07 Haptics

- **Goal:** placing and capturing can be felt.
- **Depends on:** V-05.
- **Steps:** a light tick on placing, a stronger pattern on capture, a distinct one for a big capture; respect the setting and the phone's own haptics setting.
- **Checks:** tested on two phones from different makers.
- **Done when:** the owner confirms the feel; off means off.
- **Verified by:** Human.

### V-08 Sound effects

- **Goal:** about 35 effects from D-09, all ours or CC0.
- **Depends on:** D-09, V-01.
- **Steps:** record pen taps, strokes and paper sounds with a phone; edit in Audacity; jsfxr for interface sounds; Kenney or CC0 Freesound for gaps; normalise loudness; short formats (OGG).
- **Checks:** every file in the register with its source; loudness within 2 dB across the set.
- **Done when:** all D-09 effects exist and play in the right moments.
- **Verified by:** Agent (register, loudness) and Human (listening).

### V-09 Music

- **Goal:** a menu theme, four world loops and a boss theme with commercial rights.
- **Depends on:** D-09, V-01.
- **Steps:** subscribe to Suno Pro for one month; generate instrumental, calm pieces; keep every prompt and output; cut seamless loops in Audacity; cancel the subscription.
- **Checks:** each track run through a song-recognition app (no match); listened to for familiar melodies; the subscription dates saved as proof of the plan used.
- **Done when:** 6 tracks in the app, each in the register with its prompt and the subscription proof.
- **Verified by:** Human (owner) and Agent (register).

### V-10 Audio in the app

- **Goal:** sound behaves well on every phone.
- **Depends on:** V-08, V-09.
- **Steps:** low-latency effects, music that fades between screens, separate volumes, silent mode and calls respected, music pauses when the app leaves the screen.
- **Checks:** tests of the audio state rules; a manual run with a phone call and with headphones.
- **Done when:** no sound plays in silent mode or in the background.
- **Verified by:** Agent and Human.

### V-11 World backgrounds

- **Goal:** each world has its own place: Notebook, Garden, Night sky, Sea.
- **Depends on:** V-03.
- **Steps:** backgrounds for the map and around the board, calm under the board area; made with ChatGPT under the rules, cleaned up in Krita.
- **Checks:** board readability with each background (5-second test); register rows.
- **Done when:** four backgrounds in the app, light and dark.
- **Verified by:** Agent and Human.

### V-12 Faces of the computer

- **Goal:** the opponent has a face: one per strength step, one per boss.
- **Depends on:** V-03.
- **Steps:** a small set of portraits in one style, shown on the level card and beside the score; boss faces with an expression for their turn.
- **Checks:** same style across all faces; register rows; reverse image search.
- **Done when:** faces for every strength step and boss in the app.
- **Verified by:** Human.

### V-13 Two cosmetic themes

- **Goal:** Night sky and Neon as unlockable or purchasable looks.
- **Depends on:** V-02, V-05.
- **Steps:** each as a theme file with its own pieces, rings, fills and sounds.
- **Checks:** all screenshot tests run in each theme; colour-blind checks per theme.
- **Done when:** both themes selectable and passing every check.
- **Verified by:** Agent and Human.

## Phase 6: money

Phase 6 adds the agreed model (decision 0004): cosmetics for sale and optional rewarded ads; nothing that buys an advantage, no loot boxes, no forced ads.

### C-01 Money design

- **Goal:** what is sold, for how much, and where ads may appear.
- **Depends on:** V-13 (something to sell), A-04.
- **Steps:** the cosmetic catalogue (themes, piece styles, ring effects) with prices per country tier; rewarded-ad places (one extra booster after a failed level, double daily reward) with a daily cap; earnable alternatives for every reward.
- **Checks:** no item changes the rules or the odds; every ad is opt-in; no random paid items.
- **Done when:** `docs/design/money.md` approved by the owner as a decision record.
- **Verified by:** Human (owner).

### C-02 Shop and Play Billing

- **Goal:** players can buy and use cosmetics safely.
- **Depends on:** C-01, R-01 (Play account), B-05.
- **Steps:** Google Play Billing for one-time products; a shop screen with previews; purchases acknowledged, restored on a new phone, refunds handled.
- **Checks:** Play's license testers buy, refund and restore each product; tests with the billing library's test responses.
- **Done when:** buy, restore and refund all work on the internal test track.
- **Verified by:** Agent and Human.

### C-03 Rewarded ads and consent

- **Goal:** optional ads that respect players and the law.
- **Depends on:** C-01, R-01.
- **Steps:** AdMob rewarded ads only; Google's consent message (UMP) for the EU and the UK; no ads for players under the age the rating allows; ads paused for 24 hours after a purchase.
- **Checks:** AdMob test ads only during development; the consent form shown in an EU test region; the reward granted only after the ad completes.
- **Done when:** test ads, consent and caps work; the Data safety form (R-03) lists AdMob.
- **Verified by:** Agent and Human.

### C-04 Purchase and ad safety checks

- **Goal:** nothing about money can break the game or annoy players.
- **Depends on:** C-02, C-03.
- **Steps:** offline behaviour (owned items still work), slow network, failed payment, ad that fails to load (the offer disappears instead of an error).
- **Checks:** a written test list run on two phones.
- **Done when:** every case in the list passes.
- **Verified by:** Human.

## Phase 7: online play

Phase 7 follows decision 0014: leaderboards first (O-02 can ship with the launch), then friends turn by turn after launch, ranked only when enough people play. Online games use pure rules, no boosters.

### O-01 Choose the online service

- **Goal:** a service that is free at our size and safe from surprise bills.
- **Depends on:** R-07 (launched).
- **Steps:** compare Firebase (Firestore, Cloud Functions, Cloud Messaging) with one alternative on free limits, server-side move checks, notifications and lock-in; a budget alert at $1.
- **Checks:** the free limits written down from the providers' own pricing pages, with the date.
- **Done when:** a decision record with the choice and the expected monthly cost at 1,000 and 10,000 daily players.
- **Verified by:** Human (owner).

### O-02 Play Games sign-in and leaderboards

- **Goal:** competition without needing an opponent online.
- **Depends on:** R-01, A-07.
- **Steps:** Play Games Services sign-in (optional for the player); leaderboards for the daily puzzle and total stars; a few achievements; cloud save of progress.
- **Checks:** tested with test accounts; the game works fully when the player declines sign-in.
- **Done when:** leaderboards update and progress restores on a second phone.
- **Verified by:** Agent and Human.

### O-03 Friends, turn by turn

- **Goal:** play a friend at your own pace.
- **Depends on:** O-01.
- **Steps:** invite by link (share sheet); games stored online; each side moves when they like; up to 10 games at once; resign and rematch.
- **Checks:** two phones play a whole game; one goes offline mid-game and catches up.
- **Done when:** 20 test games between two accounts finish with the same final position on both phones.
- **Verified by:** Agent and Human.

### O-04 Server move checks

- **Goal:** a modified app cannot make illegal moves online.
- **Depends on:** O-03.
- **Steps:** the engine's rules run on the server for every online move; the server keeps the true game record.
- **Checks:** tests that send illegal moves, moves out of turn and replays; the Emulator Suite in CI.
- **Done when:** every illegal move is rejected and logged.
- **Verified by:** Agent.

### O-05 Notifications

- **Goal:** players know when it is their turn, without spam.
- **Depends on:** O-03.
- **Steps:** "your turn" and "game finished" messages; at most one reminder a day; Android's notification permission asked only when the first online game starts.
- **Checks:** tested with the app closed and the phone in power saving.
- **Done when:** notifications arrive within a minute and can be turned off in settings.
- **Verified by:** Human.

### O-06 Safety and privacy online

- **Goal:** online play is safe and declared correctly.
- **Depends on:** O-03.
- **Steps:** player names from a filtered list or Play Games names, no free chat; block and report; delete-my-data in settings; privacy policy and Data safety updated.
- **Checks:** the policy matches what is collected; deleting data removes the player's online games.
- **Done when:** the updated policy is live and the delete flow is tested.
- **Verified by:** Agent and Human.

### O-07 Ranked matchmaking (gated)

- **Goal:** fair games against strangers, once there are enough strangers.
- **Depends on:** O-04; a gate of at least about 1,000 daily players (to be confirmed by data).
- **Steps:** Glicko-2 rating; matchmaking by rating with a widening window; seasons; label any bot opponent as a bot.
- **Checks:** simulated players to check ratings converge; median wait under 60 seconds in a load test.
- **Done when:** the gate is met and a week of beta ranked play shows median waits under 60 seconds.
- **Verified by:** Agent and Human.

## Phase 8: quality

Phase 8 runs alongside phases 3 to 7 and must be green before the closed test.

### Q-01 Test coverage

- **Goal:** the rules, the money and the progress can never silently break.
- **Depends on:** nothing (ongoing).
- **Steps:** keep the POC's tests; add tests for every G, A and C task; a nightly emulator run of the main flows.
- **Checks:** engine line coverage report in CI.
- **Done when:** engine coverage at least 85%; every screen has at least one UI or screenshot test; the nightly run is green for a week.
- **Verified by:** Agent.

### Q-02 Speed on a cheap phone

- **Goal:** smooth on the phones most target players own.
- **Depends on:** V-05.
- **Steps:** Baseline Profiles; Macrobenchmark for start-up, a capture animation and the computer's move; test on an old low-end phone (2 GB memory class).
- **Checks:** benchmark numbers recorded per release.
- **Done when:** cold start under 2 s, under 5% slow frames in play, the computer answers within its 1.5 s limit on the low-end phone.
- **Verified by:** Agent and Human.

### Q-03 Accessibility

- **Goal:** playable with large text, a screen reader and colour blindness.
- **Depends on:** V-04, A-09.
- **Steps:** Accessibility Scanner on every screen; TalkBack reads the board state and the last move; layouts at 200% font size; animations reduced with the system setting.
- **Checks:** scanner report; one full level played with TalkBack.
- **Done when:** no scanner errors and a TalkBack level completed.
- **Verified by:** Agent and Human.

### Q-04 Play-tests of the campaign

- **Goal:** real people confirm the difficulty curve.
- **Depends on:** G-10, A-03.
- **Steps:** 5 to 8 testers play worlds 1 and 2 with the game log on; compare their pass rates with the bench curve (G-11); fix outliers.
- **Checks:** per-level pass rate and retries from the logs.
- **Done when:** no level where most testers fail more than 5 times without a planned "wall" note; testers rate the campaign at least 4 of 5 on "I want to continue".
- **Verified by:** Human (and Agent for the log numbers).

### Q-05 Crash reports

- **Goal:** we see crashes before reviews mention them.
- **Depends on:** R-01.
- **Steps:** Play Console's Android vitals, plus Firebase Crashlytics if needed; declared in Data safety.
- **Checks:** a forced test crash appears in the dashboard.
- **Done when:** crash-free sessions at least 99.5% in the closed test.
- **Verified by:** Agent.

### Q-06 Analytics

- **Goal:** know where players get stuck or leave, without tracking people.
- **Depends on:** A-03.
- **Steps:** a small set of anonymous events (level start, pass, fail, booster used, daily puzzle done); Firebase Analytics or our own log format; consent where needed; declared in Data safety.
- **Checks:** events seen in the debug view; nothing personal sent.
- **Done when:** a funnel from install to level 10 is visible.
- **Verified by:** Agent.

### Q-07 Offline and interruptions

- **Goal:** the game never loses state, online or not.
- **Depends on:** A-04, C-02.
- **Steps:** airplane mode, a phone call mid-capture, low battery mode, rotating or folding the phone, Android killing the app.
- **Checks:** a written list run on two phones.
- **Done when:** every case resumes exactly where it stopped.
- **Verified by:** Human.

## Phase 9: release and after

Phase 9 gets the game onto Google Play and keeps it alive.

### R-01 Play Console account

- **Goal:** the developer account exists and is verified.
- **Depends on:** B-05.
- **Steps:** personal developer account on the game's Google account; $25 fee; identity verification; developer page details.
- **Checks:** verification complete in Play Console.
- **Done when:** an internal test track accepts an upload.
- **Verified by:** Human.

### R-02 Release build and signing

- **Goal:** signed release builds come from CI on a tag.
- **Depends on:** R-01, B-03.
- **Steps:** upload key in CI secrets with an offline backup; Play App Signing; release build with shrinking; version numbers from the tag; an Android App Bundle uploaded to internal testing.
- **Checks:** a test tag produces a bundle that installs from the internal track.
- **Done when:** tag to internal track works end to end.
- **Verified by:** Agent.

### R-03 Privacy policy and Data safety

- **Goal:** what the app collects is declared truthfully.
- **Depends on:** C-03, Q-05, Q-06.
- **Steps:** a privacy policy page (GitHub Pages or Google Sites); the Data safety form listing ads, analytics, crash reports and purchases; account deletion path if accounts exist (O-06).
- **Checks:** the policy matches the SDKs actually in the build (list them from the dependencies).
- **Done when:** both are accepted in Play Console.
- **Verified by:** Agent and Human.

### R-04 Age rating and ads declaration

- **Goal:** the right rating and audience.
- **Depends on:** C-03.
- **Steps:** IARC questionnaire; target audience teens and adults (not "Designed for Families"); "contains ads" and "in-app purchases" declared.
- **Checks:** the rating certificate in Play Console.
- **Done when:** ratings received for all regions.
- **Verified by:** Human.

### R-05 Store listing

- **Goal:** a listing that shows the game's best moment in one look.
- **Depends on:** V-05, A-10, B-04.
- **Steps:** icon, feature graphic, 6 to 8 phone screenshots (the first shows a capture), a 30-second video, short and full description using "dots" descriptively; all four languages.
- **Checks:** the listing previewed on a phone; every image in the asset register.
- **Done when:** the listing is complete in all four languages.
- **Verified by:** Human.

### R-06 Closed test

- **Goal:** meet Play's closed-test requirement and catch last problems.
- **Depends on:** R-02 to R-05, Q-01 to Q-07.
- **Steps:** a closed track with at least the number of testers Play requires for new personal accounts (12 for 14 days at the last rule known; check Play Console); collect feedback; fix blockers.
- **Checks:** Play Console shows the requirement met.
- **Done when:** the requirement is met, no open blocker, crash-free sessions at least 99.5%.
- **Verified by:** Human.

### R-07 Launch

- **Goal:** the game is public on Google Play.
- **Depends on:** R-06.
- **Steps:** apply for production access; staged rollout 10%, 50%, 100% over about a week, watching vitals and reviews; announce to testers and friends.
- **Checks:** vitals within Play's thresholds at each stage.
- **Done when:** 100% rollout with no bad-behaviour warning.
- **Verified by:** Human.

### R-08 After launch

- **Goal:** keep players coming back and the game growing.
- **Depends on:** R-07.
- **Steps:** answer reviews within 3 days; a new world every 1 to 2 months; seasonal weekly challenges; read the analytics funnel monthly; plan the online phase (O-01) once retention looks healthy.
- **Checks:** monthly review of day-1, day-7 and day-30 return rates.
- **Done when:** ongoing; reviewed every month in a short note under `docs/`.
- **Verified by:** Human.

## Dependencies and tracker

Naming and design start now, beside the POC test week; engine work waits for the POC's "go", and visual coding waits for the design gate.

![Phase dependencies: 10 phases, 2 gates](images/plan-phase-dependencies.png)

Money, the app and quality all feed the release; online play follows the launch, except leaderboards (O-02), which can ship with it.

### Tracker: phases 0 to 3

| Task | Phase | Depends on | Status |
| --- | --- | --- | --- |
| P-09 Test build and analysis script | 0 POC | P-05 to P-08 (done) | Done |
| P-10 Test week and decision | 0 POC | P-09 | Not started |
| B-01 Name candidates | 1 Brand | none | Done |
| B-02 Name checks | 1 Brand | B-01 | Done |
| B-03 Pick the name and the app ID | 1 Brand | B-02, D-02 | Waiting for a human check |
| B-04 Logo and app icon | 1 Brand | B-03, D-03 | Not started |
| B-05 Accounts and handles | 1 Brand | B-03 | Not started |
| D-01 References | 2 Design | none | Not started |
| D-02 Mood boards, three directions | 2 Design | D-01, V-01 | Not started |
| D-03 Notebook theme specification | 2 Design | D-02 | Not started |
| D-04 Piece shapes and colour-blind safety | 2 Design | D-03 | Not started |
| D-05 Screens and flows | 2 Design | none | Not started |
| D-06 Wireframes | 2 Design | D-05 | Not started |
| D-07 Canvas: the game in the chosen look | 2 Design | D-03, D-04, D-06 | Not started |
| D-08 Motion storyboard | 2 Design | D-07 | Not started |
| D-09 Sound list | 2 Design | D-08 | Not started |
| D-10 Clickable prototype | 2 Design | D-07 | Not started |
| D-11 Five-second tests | 2 Design | D-07 | Not started |
| D-12 Design gate | 2 Design | D-08 to D-11, B-03 | Not started |
| G-01 Level format | 3 Game design | P-10 | Not started |
| G-02 Mission types | 3 Game design | G-01 | Not started |
| G-03 Puzzle checker | 3 Game design | G-02 | Not started |
| G-04 Boosters in the engine | 3 Game design | G-01 | Not started |
| G-05 Boss rules | 3 Game design | G-01 | Not started |
| G-06 Board shapes and special points | 3 Game design | G-01 | Not started |
| G-07 Computer that errs like a person | 3 Game design | none | Not started |
| G-08 Stars | 3 Game design | G-02 | Not started |
| G-09 World 1 levels | 3 Game design | G-02, G-03, G-08 | Not started |
| G-10 Worlds 2 to 4 | 3 Game design | G-04, G-05, G-06, G-09 | Not started |
| G-11 Difficulty curve | 3 Game design | G-07, G-10 | Not started |
| G-12 Daily puzzle generator | 3 Game design | G-03 | Not started |
| G-13 Capture details for animation | 3 Game design | none | Not started |

### Tracker: phases 4 to 9

| Task | Phase | Depends on | Status |
| --- | --- | --- | --- |
| A-01 Navigation for the full game | 4 App | D-05, B-03 | Not started |
| A-02 World map | 4 App | A-01, G-01 | Not started |
| A-03 Level card and result | 4 App | A-02, G-02, G-08 | Not started |
| A-04 Progress and unlocks | 4 App | A-03 | Not started |
| A-05 Boosters in the app | 4 App | A-03, G-04 | Not started |
| A-06 Tutorial | 4 App | G-09 | Not started |
| A-07 Daily puzzle and streak | 4 App | G-12, A-04 | Not started |
| A-08 Free play and two players | 4 App | A-01 | Not started |
| A-09 Settings | 4 App | A-01 | Not started |
| A-10 Languages | 4 App | A-09 | Not started |
| A-11 Stats | 4 App | A-04 | Not started |
| A-12 Rate and feedback prompts | 4 App | A-03 | Not started |
| V-01 Asset register and prompt rules | 5 Visuals | none | Not started |
| V-02 Theme system | 5 Visuals | D-12 | Not started |
| V-03 Notebook theme | 5 Visuals | V-02, D-03, D-04 | Not started |
| V-04 Pieces and accessibility | 5 Visuals | V-03 | Not started |
| V-05 Animations | 5 Visuals | V-03, D-08, G-13 | Not started |
| V-06 Booster effects | 5 Visuals | V-05, A-05 | Not started |
| V-07 Haptics | 5 Visuals | V-05 | Not started |
| V-08 Sound effects | 5 Visuals | D-09, V-01 | Not started |
| V-09 Music | 5 Visuals | D-09, V-01 | Not started |
| V-10 Audio in the app | 5 Visuals | V-08, V-09 | Not started |
| V-11 World backgrounds | 5 Visuals | V-03 | Not started |
| V-12 Faces of the computer | 5 Visuals | V-03 | Not started |
| V-13 Two cosmetic themes | 5 Visuals | V-02, V-05 | Not started |
| C-01 Money design | 6 Money | V-13, A-04 | Not started |
| C-02 Shop and Play Billing | 6 Money | C-01, R-01, B-05 | Not started |
| C-03 Rewarded ads and consent | 6 Money | C-01, R-01 | Not started |
| C-04 Purchase and ad safety checks | 6 Money | C-02, C-03 | Not started |
| O-01 Choose the online service | 7 Online | R-07 | Not started |
| O-02 Play Games sign-in and leaderboards | 7 Online | R-01, A-07 | Not started |
| O-03 Friends, turn by turn | 7 Online | O-01 | Not started |
| O-04 Server move checks | 7 Online | O-03 | Not started |
| O-05 Notifications | 7 Online | O-03 | Not started |
| O-06 Safety and privacy online | 7 Online | O-03 | Not started |
| O-07 Ranked matchmaking (gated) | 7 Online | O-04, player numbers | Not started |
| Q-01 Test coverage | 8 Quality | none | Not started |
| Q-02 Speed on a cheap phone | 8 Quality | V-05 | Not started |
| Q-03 Accessibility | 8 Quality | V-04, A-09 | Not started |
| Q-04 Play-tests of the campaign | 8 Quality | G-10, A-03 | Not started |
| Q-05 Crash reports | 8 Quality | R-01 | Not started |
| Q-06 Analytics | 8 Quality | A-03 | Not started |
| Q-07 Offline and interruptions | 8 Quality | A-04, C-02 | Not started |
| R-01 Play Console account | 9 Release | B-05 | Not started |
| R-02 Release build and signing | 9 Release | R-01, B-03 | Not started |
| R-03 Privacy policy and Data safety | 9 Release | C-03, Q-05, Q-06 | Not started |
| R-04 Age rating and ads declaration | 9 Release | C-03 | Not started |
| R-05 Store listing | 9 Release | V-05, A-10, B-04 | Not started |
| R-06 Closed test | 9 Release | R-02 to R-05, Q-01 to Q-07 | Not started |
| R-07 Launch | 9 Release | R-06 | Not started |
| R-08 After launch | 9 Release | R-07 | Not started |

## Open decisions

These are still open; each gets a record in `docs/decisions/` when made.

| Decision | Options | Recommendation | Needed by |
| --- | --- | --- | --- |
| The name | The clear candidates from B-02 | Pick after seeing the mood boards, so name and look fit | B-03 |
| Buy a domain | Yes (about $10 to $20 a year); no, use github.io | Yes once the name is final: a stable support and privacy address | B-05 |
| Prices and ad caps | From C-01 | Low prices per country tier; at most 3 rewarded ads a day | C-01 |
| Hole edges in odd boards | Count as border (safe); count as open (capturable) | Per level, stated on the level card | G-06 |
| One dot closing several chains | Capture every enclosed area (current); only the smallest | Keep the current rule; revisit after play-tests | Q-04 |
| Free play ending | Dot budget (current); open-ended with "stop" as on paper | Budget by default, open-ended as an option | A-08 |
| Online service | Firebase; one alternative | Decide from the O-01 comparison | O-01 |
| Ranked gate | About 1,000 daily players; another threshold | Confirm with real numbers after launch | O-07 |
| Register our own trademark | Now; after traction; never | After traction: a few hundred euros or dollars per country and class | After R-07 |
