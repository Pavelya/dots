# POC test protocol

## Testers

5 to 10 people with Android phones (Android 7 or newer): at least 2 who played Dots on paper,
at least 2 who never heard of it. Nobody who worked on the game.

## The week

| Day | What happens |
|---|---|
| 0 | Tester installs the APK from the GitHub Release link. One 15-minute call: they read the rules screen and play their first game against Easy while thinking aloud. We note what confuses them; we do not explain unless they are stuck for over a minute. |
| 1 to 6 | They play whenever they feel like it. No reminders, no requests to play. Whether they come back on their own is the main thing being measured. |
| 7 | They tap "Export log" and send the file, then fill in the survey (5 minutes). |

## Survey (Google Forms, free)

1. On how many days did you play? (from the log too)
2. "I want to keep playing this game." 1 (not at all) to 5 (very much)
3. In your own words: when are dots captured?
4. The length of one game was: too short / about right / too long
5. Which difficulty felt best? Was it too easy, fair, too hard?
6. Did you turn on the threat highlight? Did it help? (yes / a bit / no / didn't try)
7. What annoyed you most? (free text)
8. What did you enjoy most? (free text)
9. Would you recommend it to a friend who likes board games? (yes / maybe / no)

## From the logs

A script in `cli` reads all logs and reports, per tester and overall: days played, games, game
length, share of games with a winner, win rate per difficulty, undos per game, captures per
game with the threat highlight on and off. It replays every game with the engine to check the
logs are consistent.

## Report and decision

A one-page report in `docs/poc/results.md`: each hypothesis from the charter with its numbers
and pass or fail, the top 5 annoyances, the top 5 things enjoyed. Then the go / change and retest
/ stop decision, recorded in `docs/decisions/`.
