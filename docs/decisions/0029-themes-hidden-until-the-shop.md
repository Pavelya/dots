# 0029: Star chart and Neon arcade stay hidden until the Shop

**Date:** 2026-11-18 · **Status:** accepted

## Context

V-13 makes two themes, Star chart and Neon arcade (decision 0028). Buying them comes later:
C-01 sets prices and C-02 builds the Shop and Play Billing. Between the two, the themes exist in
the app with no way to own them.

Decision 0004 makes themes something the game sells, and says every reward from an ad can also
be earned by playing. Decision 0027 says chests give boosters until cosmetics exist.

Three options were considered:

- **A. Free until the Shop.** Anyone could pick either theme in Settings and keep it once the
  Shop opens. Testers could judge the themes during the POC test week, but the game would give
  away what C-01 has to price.
- **B. Hidden until the Shop.** Chosen.
- **C. Earned only.** The themes unlock through play, such as finishing a world. This sets an
  unlock rule before C-01 has decided how earning and buying fit together.

## Decision

- **Before the Shop exists, players can use only Notebook.** Release builds never show Star
  chart or Neon arcade: not in Settings, not in chests, not anywhere else.
- **Debug builds show both themes,** so the owner and testers on a debug build can pick them and
  check them on a phone.
- **The themes are in the code and tested like any other part of the app.** Hiding them only
  hides the choice; the themes themselves are finished work.
- **Players first see the themes in the Shop.** How they can be bought and earned there is for
  C-01 and C-02, within decision 0004.

## Consequences

- The app needs one switch that says whether a theme can be chosen. Before C-02 it allows every
  theme on a debug build and only Notebook on a release build. C-02 replaces it with "the player
  owns this theme".
- D-25 and A-23 design and build choosing a theme for a debug build now. The same screen must
  work later with the Shop's owned and not-owned themes.
- Screenshot tests may draw Star chart and Neon arcade, since they test the code, not what a
  release build offers.
- Testers on release builds do not judge the themes during the POC test week.
- Chests keep giving boosters only (decision 0027) until the Shop exists.
