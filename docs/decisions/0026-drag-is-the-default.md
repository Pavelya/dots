# 0026: Drag is the default way to place a dot

**Date:** 2026-11-15 · **Status:** accepted

## Context

Decision 0024 gave the player two ways to place a dot, two taps and drag, chosen in Settings
under "Placing a dot". It made two taps the default because every POC tester already knew it.

Drag takes one touch per dot instead of two. Its guides (D-15, V-03) draw the point's row and
column around the finger, so the point is easy to aim at even on a 15x15 board. Like two taps, it
guards against mis-taps: nothing is placed until the finger lifts, and lifting it off the board
places nothing. The owner wants a new player to start with it.

The app keeps the player's settings on the phone. Whenever any setting changes (a volume, a
switch, the board size on Play a game, Skip in the tutorial), the app saves all of them, the
placing method included. So a saved "two taps" does not show that the player chose it: most
players who have it only changed something else. Players who never changed any setting have
nothing saved.

The app is not in a store yet. Only the owner and a few testers have it.

## Decision

- **Drag is the default.** A new player places dots by drag. Settings lists drag first, marked as
  the default, and two taps second.
- **The tutorial (A-06) teaches drag** on level 1 and says that two taps can be chosen in
  Settings.
- **Players who already have the app switch to drag, once.** The app saves the placing method
  under a new name and ignores the old one, so every existing player starts on drag, whatever
  was saved before. A player who wants two taps back chooses it in Settings; from then on it
  stays.
- Everything else in decision 0024 stands: the two methods, the choice in Settings, and the game
  log recording the method, the touches and the dots placed, so `cli placing-report` can compare
  them.

## Consequences

- A tester who used two taps finds drag after the update. A single tap no longer shows a preview:
  it places a dot where the finger lifts. Replay the tutorial shows how drag works and where to
  switch back.
- More games are played by drag, which gives `cli placing-report` more drag games to compare.
- `flows.md` (Settings, First launch), `world-1.md` and the design pages name drag as the default.
