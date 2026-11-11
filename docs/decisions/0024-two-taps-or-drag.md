# 0024: Dots are placed by two taps or by drag; the player chooses

**Date:** 2026-11-11 · **Status:** accepted

## Context

On a 15x15 board a phone 360 to 410 dp wide gives each point 22 to 25 dp; Android's guideline
for a touch target is 48 dp. In the POC a dot took two taps: one to preview, one to place. POC
testers said the points felt too small for large fingers and that two taps per dot felt like many
taps, though fine once known ([POC results](../poc/results.md), points 7 and 8). H5, "placing dots
by finger works", failed.

Decision 0023 makes Undo a campaign booster, so a misplaced dot can no longer always be taken
back: the way a dot is placed has to guard against mis-taps.

A-13 built five methods and testers tried each on a phone:

| Method | How it works | Taps per dot |
| --- | --- | --- |
| Two taps | Tap a point to see the preview, tap it again to place. | 2 |
| Zoom | The first tap zooms the board in to 48 dp a point; the next tap places. | 2 |
| Magnifier | While the finger is down, a magnifier above it shows the point; lifting places. | 1 |
| Drag | Press, drag to the point and lift. The point's row and column show around the finger. | 1 |
| Long press | Hold a point for 400 ms to place. | 1 |

The testers liked two taps and drag, and did not take to the other three.

## Decision

- **Two ways of placing a dot, chosen in Settings** under "Placing a dot": **two taps** and
  **drag**. Both guard against mis-taps: two taps by its confirming tap, drag because nothing is
  placed until the finger lifts, and lifting it off the board places nothing.
- **Two taps is the default.** Every tester already knew it. The tutorial (A-06) teaches it and
  says where to switch to drag.
- **Zoom, magnifier and long press are removed.**
- The game log keeps recording the method, the touches and the dots placed, so `cli
  placing-report` can compare the two later.

## Consequences

- The app's Settings has a "Placing a dot" choice (`flows.md`, Settings). A-09 keeps it when it
  redesigns Settings.
- The design draws drag's row and column guides in the Notebook theme (D-15). Two taps uses the
  preview already in `pieces.md`.
- 0 ms of "Placing a dot" in `motion.md` is the second tap, or the finger lifting in drag.
