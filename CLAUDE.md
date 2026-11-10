# Pinfold

Pinfold is an Android game built on the paper-and-pencil game *Dots*: two players take turns
placing dots on a grid and score points by surrounding the other side's dots. This repository
holds the game rules, a computer opponent, a terminal version and the Android app. The README
explains the rules and how the computer opponent works.

## Layout

| Path | What it is |
|------|------------|
| `engine/` | Pure Kotlin/JVM library: rules, computer opponent (`dots.ai`), campaign, daily puzzle, game log. No Android code. |
| `cli/` | Terminal game, benchmarks, tester-log tools. Built on `engine`. |
| `app/` | Android app (Jetpack Compose). Built on `engine`. Structure: `docs/poc/architecture.md`. |
| `gradle/libs.versions.toml` | Every dependency version. Dependabot updates it; don't bump versions by hand unless the task asks for it. |
| `docs/plan.md` | The plan: the game, phases, gates, tools and accounts. Task cards are issues. |
| `docs/poc/` | The proof of concept: charter, architecture, test protocol. |
| `docs/design/` | Design work: names, investigation, references, mood boards, the Notebook theme, the brand, and the screens and flows (`flows.md`). |
| `docs/decisions/` | One file per settled decision, `NNNN-short-title.md`. |

## Commands

Needs JDK 21 and, for `app`, the Android SDK. The Gradle wrapper fetches everything else.

```sh
./gradlew :engine:test :cli:test          # engine and CLI tests (fast, no Android SDK needed)
./gradlew :app:testDebugUnitTest          # app tests (Robolectric, no phone needed)
./gradlew :app:recordRoborazziDebug       # rewrite the reference screenshots in app/src/test/screenshots/
./gradlew :app:verifyRoborazziDebug       # fail if the app draws something different from them
./gradlew :cli:installDist                # then: cli/build/install/cli/bin/cli levels
```

**The CI command** (same as `.github/workflows/test.yml`; run it before every push):

```sh
./gradlew --continue :engine:test :cli:test :cli:installDist :app:verifyRoborazziDebug :app:lintDebug :app:assembleDebug
```

Lint treats warnings as errors. If a change alters what the app draws, record new screenshots,
look at them, and commit them with the change.

If the machine has no JDK or Android SDK, push anyway and rely on CI, and say in the PR that the
command was not run locally.

## Session protocol

One session works on one issue. Given "Work on B-02" (a task ID):

1. **Read the issue.** Find its number with `gh issue list --state all --search "B-02 in:title"`,
   then `gh issue view <number> --comments`. Comments can change the task. If it
   depends on an open issue, or is labelled `blocked`, stop and say so.
2. **Branch** `task/<ID>-<slug>` from the latest `main`, e.g. `task/F-3-safe-to-publish`
   (`git fetch origin && git switch -c task/F-3-safe-to-publish origin/main`).
3. **Stay within the issue.** Anything else you notice (a bug, a stale doc, a better idea) becomes
   a new issue (`gh issue create`, using the task template, titled with a new ID), not an extra
   change in this PR.
4. **Run the CI command** before pushing.
5. **Open a PR** with `gh pr create`, following `.github/pull_request_template.md`: `Closes #<number>`,
   what changed, and the issue's "Done when" checklist copied with each item ticked or explained.
   Items that only a person can check stay unticked, with a note on what they should do.
6. **The owner merges.** Never run `gh pr merge`, never push to `main`, never force-push.
   After review comments, add commits to the same branch.

Labels: `agent` (checkable from the repository, CI or a script), `human` (needs the owner's eyes,
a phone or taste), `blocked`, `decision` (closes with a record in `docs/decisions/`), `draft`
(card to be revised after the POC test week).

## Task IDs

Every task has one name: its **ID**, a letter and a number, e.g. B-02. The letter is the phase:
F foundation, P POC, B brand, D design, G game design and engine, A app, V visuals and sound,
C money, O online, Q quality, R release, X open decisions. A new issue takes the next free number
of its letter, and its title starts with the ID (`B-06 Store subtitle`).

Refer to tasks only by ID, in chat, docs, code comments, commits and PRs. Issue numbers appear
only where GitHub needs them: `Closes #<number>` and `gh` commands. Decision records keep their
own four-digit numbers (decision 0016); those are documents, not tasks.

## Rules

- **Status lives only in GitHub Issues.** Never write task status ("done", "in progress",
  "not started", counts of finished tasks) into docs or code comments.
- **The engine stays free of Android code**, so the rules run the same in the app, the CLI and a
  future server.
- **Every image, sound or font** enters the repository with a row in the asset register
  (`docs/assets.md`): source, tool, licence, author. Without a row, it does not enter, and
  `:cli:test` fails.
- **Decisions** go in `docs/decisions/`, one short file each (Context, Decision, Consequences).
  Changing a settled decision means a new record, not an edit of the old one.
- **Numbers in "Done when"** are targets. If one turns out wrong, change it in the issue and say
  so in the PR; don't change it silently in code.
- **Writing:** plain English, short sentences, no jargon a newcomer would not know. Docs describe
  how things are, not the history of how they got there.
- **Commits** use the GitHub noreply identity `Pavelya <3656025+Pavelya@users.noreply.github.com>`
  (set in the repository's git config; set it there if missing). Merges into `main` are squash-only.
