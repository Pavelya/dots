# POC tasks

Same card format as the full plan: goal, depends on, done when, verified by. "Pulls forward"
names the full-plan tasks this one covers in part, so their work is not done twice later.
One task = one pull request with green CI. Status is kept in the table at the end.

### P-01 Approve the charter

- **Goal:** owner agrees with the question, hypotheses, pass marks, scope and decisions D-POC-1 to 3.
- **Depends on:** nothing.
- **Done when:** `docs/poc/README.md` says "approved" with the date, and the three decisions are recorded.
- **Verified by:** Human (owner).

### P-02 Engine additions for the POC

- **Goal:** the engine offers what the app needs: a game record (seed, settings, moves) that replays exactly, undo, and a computer that answers within a time limit.
- **Pulls forward:** E-01, E-05 (parts).
- **Depends on:** P-01.
- **Done when:** tests show a record replays to the same final position for 500 random games; undo restores the previous state; with a 300 ms limit the computer answers within 400 ms on 15x15.
- **Verified by:** Agent (CI).

### P-03 App skeleton and CI

- **Goal:** an `app` module that builds in CI and starts on a phone, structured as in [architecture.md](architecture.md).
- **Pulls forward:** A-01, A-02, S-04 (parts), Q-01 (parts).
- **Depends on:** P-01.
- **Done when:** CI builds the debug APK and runs app unit tests; the app opens to a home screen with "Play the computer", "Two players", "Rules", "Settings".
- **Verified by:** Agent (CI) and Human (installs it).

### P-04 Board view and placement

- **Goal:** a crisp 15x15 board (also 12x12, 20x20) with tap-to-preview, tap-to-confirm placement.
- **Pulls forward:** A-03, A-04.
- **Depends on:** P-03.
- **Done when:** unit tests map touches to the right cell at all three sizes; screenshot tests of three fixed positions pass; a person places 20 intended dots on 15x15 without a mistake.
- **Verified by:** Agent and Human.

### P-05 Playing a game

- **Goal:** full games against the computer (Easy, Normal, Hard) and between two people, ending in a result screen with "play again".
- **Pulls forward:** A-05, A-06.
- **Depends on:** P-02, P-04.
- **Done when:** a UI test plays a scripted game to the result screen; the screen never freezes while the computer thinks (StrictMode clean); undo works against the computer.
- **Verified by:** Agent and Human.

### P-06 Threat highlight

- **Goal:** groups one or two dots from capture are marked, for both sides, when the setting is on.
- **Pulls forward:** E-04, V-05 (a plain version).
- **Depends on:** P-05.
- **Done when:** known positions show the expected marks in screenshot tests; turning the setting off removes them.
- **Verified by:** Agent.

### P-07 Rules screen and settings

- **Goal:** one rules screen a newcomer can learn from, and settings for board size, dots, difficulty and threat highlight.
- **Pulls forward:** M-05 (parts), D-04 (screen list).
- **Depends on:** P-03.
- **Done when:** settings survive a restart (test); one person who does not know the game reads the rules and explains captures correctly.
- **Verified by:** Agent and Human.

### P-08 Resume and game log

- **Goal:** closing the app never loses a game, and every finished game is written to the log that testers can export.
- **Pulls forward:** A-07, E-02 (parts).
- **Depends on:** P-05.
- **Done when:** a test kills the app mid-game and gets the same position back; the log format test passes; `cli` replays an exported log and reports the same results.
- **Verified by:** Agent.

### P-09 Test build and analysis script

- **Goal:** testers can install the app from one link, and their logs can be analysed in one command.
- **Depends on:** P-05 to P-08.
- **Done when:** tagging `poc-v0.1` puts a signed debug APK on a GitHub Release; `cli poc-report <logs>` prints every metric in the test protocol.
- **Verified by:** Agent (build) and Human (installs from the link on a second phone).

### P-10 Test week and decision

- **Goal:** answer the POC question.
- **Depends on:** P-09, testers found (D-POC-3).
- **Done when:** `docs/poc/results.md` scores every hypothesis, and the go / change / stop decision is recorded in `docs/decisions/`.
- **Verified by:** Human (owner).

## Order

```
P-01 ─┬─ P-02 ───────────────┐
      └─ P-03 ─┬─ P-04 ─ P-05 ─┬─ P-06 ─┐
               └─ P-07 ────────┼────────┼─ P-09 ─ P-10
                               └─ P-08 ─┘
```

## Status

| Task | Status |
|---|---|
| P-01 Approve the charter | Done 2026-11-04 |
| P-02 Engine additions | Done 2026-11-04 |
| P-03 App skeleton and CI | Done 2026-11-05 (owner installed it on a phone) |
| P-04 Board view and placement | Done in CI 2026-11-04; waiting for the owner's 20-dot placement check on a phone |
| P-05 Playing a game | Done 2026-11-05 (owner played both modes on a phone) |
| P-06 Threat highlight | Done in CI 2026-11-05 |
| P-07 Rules screen and settings | Done in CI 2026-11-04; waiting for a newcomer to read the rules and explain captures |
| P-08 Resume and game log | Done in CI 2026-11-05 |
| P-09 Test build and analysis script | Not started |
| P-10 Test week and decision | Not started |
