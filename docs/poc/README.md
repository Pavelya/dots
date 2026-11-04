# Dots POC: charter

**Status:** draft, waiting for the owner's approval (task P-01).
**One question:** do people enjoy the core game on a phone, enough to keep playing without being asked?

Everything else (themes, figures, animations, sound, campaign, online play, store release) is
parked in the [full plan](https://claude.ai/artifact/9GaGRbXg9qwJsYMswwE7ZF) until this question
has an answer. The POC builds only what is needed to answer it, in a way the full version can
reuse.

## What we already know (computer simulation only)

From `cli bench variants` (see the main README):

- On an empty board, two careful players almost always draw 0-0.
- A 15x15 board starting from four random crosses (2 red + 2 blue dots each, placed fairly) gives
  captures from the first moves, a winner in about 95% of games, and comebacks in about a third.
- 33 dots each makes a game of 66 moves, about 5 to 8 minutes.

None of this has been tried by a person yet.

## Hypotheses and how we judge them

Measured during one test week with 5 to 10 players (see [test protocol](test-protocol.md)).

| # | Hypothesis | Measured by | Passes when |
|---|---|---|---|
| H1 | People like the game enough to come back | Game logs, survey | At least half the testers play on 3 or more different days, and at least half answer 4 or 5 to "I want to keep playing" (1 to 5) |
| H2 | The rules are learnable from one screen | Survey, first-game logs | At least 4 of 5 testers can explain when dots are captured after their first 2 games |
| H3 | The format is right: 15x15, four crosses, 33 dots | Game logs, survey | Median game 4 to 10 minutes; at least 80% of games have a winner; most testers call the length "about right" |
| H4 | Playing the computer is fun at some difficulty | Game logs, survey | Every tester finds a difficulty they beat sometimes but not always (win rate 20% to 70%) |
| H5 | Placing dots by finger works | Game logs, survey | Under 1 undone misplaced move per game on average; no tester names placement as a top annoyance |
| H6 | Showing threats helps (on/off setting) | Survey, logs | Testers who used it say it helped, and capture counts are not lower with it on |

**Decision after the test week** (recorded in `docs/decisions/`):

- **Go:** H1 and H3 pass, and no other hypothesis fails badly. Un-park the full plan, re-scored with what we learned.
- **Change and retest:** H1 is borderline, or a fixable hypothesis (H2, H4, H5, H6) fails. Fix it, retest for one more week.
- **Stop:** H1 clearly fails (most testers stop after one or two days) even though the game is easy to learn and the format works. The core game is the problem, and visuals will not fix it.

## Scope

**In the POC:**

- Android app, plain grey look, Material defaults.
- Game: 15x15 board, four random crosses, 33 dots each. Settings also allow 12x12 / 20x20 and 67 dots, for comparison.
- Opponents: computer (Easy, Normal, Hard) and two players on one phone.
- Accurate placement: tap to preview, tap again to confirm; undo of the last own move against the computer.
- Threat highlight, switchable in settings.
- One rules screen, with three small pictures.
- Result screen with "play again".
- Resume an unfinished game after closing the app.
- A game log on the phone, shared by the tester with one button (no tracking, no internet).

**Not in the POC (parked in the full plan):** themes, piece figures, animations beyond simple
feedback, sound, campaign and levels, tutorial, online play, achievements, languages other than
English, Play Store release.

## Constraints

- Free tools only (see the full plan's tool list). No money is spent on the POC.
- The test build is shared as an APK file, not through the Play Store (decision D-POC-2 below).
- No personal data leaves the phone unless the tester shares their log file themselves.

## Decisions needed from the owner before building

| ID | Decision | Proposal |
|---|---|---|
| D-POC-1 | App ID (permanent if the app is ever published) | `io.github.pavelya.dots` |
| D-POC-2 | How testers install it | APK attached to a GitHub Release; Android asks testers to allow the install once |
| D-POC-3 | Who the testers are | 5 to 10 people: at least 2 who knew Dots on paper, at least 2 who never heard of it |

## Documents

- [Architecture and reuse](architecture.md): how the POC code is structured so the full version keeps it.
- [Tasks](tasks.md): the POC work, in order, each with its goal and how it is verified.
- [Test protocol](test-protocol.md): how the test week runs and what is measured.
- Decision records: `docs/decisions/`.
