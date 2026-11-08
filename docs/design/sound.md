# Sound

This page names every sound in Pinfold before it is made: 38 sound effects and 6 music tracks.
For each one it gives when it plays, how long it is, its mood, and how it is made, with the exact
file name to save and the exact prompt, recording or recipe. The effects are made in V-08, the
music in V-09, and V-10 makes the app play them.

The times come from the [motion storyboard](motion.md): a sound starts at the moment its animation
step starts, and its "When it plays" names that step.

## Rules for all sound

1. **The game works on mute.** Every moment is carried by the animation and the vibration. Sound
   adds to them and never carries information alone.
2. **Sound and vibration start together.** A sound that goes with a vibration (motion rule 6)
   starts at the same millisecond: the dot at 0 ms, the closing ring at 550 ms, the big capture at
   650 ms, the bomb's blast at 150 ms, each star as it lands.
3. **Pen and paper.** Sounds of things happening on the board are recordings of a pen, a pencil
   and a notebook. Sounds of reward and warning are soft notes, like felt mallets on small metal
   bars, made in code.
4. **Notes that fit every track.** Notes made in code use only D, E, G and A, in any octave. These
   four notes belong to the key of every music track, so a sound never clashes with the music.
5. **Little repeats.** The dot is the only sound heard every turn. It has three takes, played in
   turn at slightly different pitches (from 95% to 105% speed), so it never sounds mechanical.
6. **A tap stops the sound with the animation.** A tap that finishes an animation (motion rule 1)
   also fades its sounds out in 50 ms.
7. **Same loudness in every file.** Every effect is normalised to the same loudness (the V-08
   check: within 2 dB). The app sets each sound's volume from the "Volume" column: full, or half
   for small, frequent sounds.
8. **Two players on one phone.** Each side's dot plays its own ink's sound. Every capture plays the
   player's capture sounds; the sound for the computer capturing the player's dots is not used.

## Where each sound comes from

| Source | Who makes it | Files to save | Used for |
| --- | --- | --- | --- |
| Recorded | The owner, with a phone | `rec_*` ([recording list](#the-recordings)) | Board, booster and interface sounds |
| Made in code | Claude, in V-08, with a script kept in the repository | none; the script makes `sfx_*` | Notes for rewards, warnings and results |
| Suno Pro | The owner, during the one paid month | `music_*.wav` ([music](#music)) | The six music tracks |
| CC0 (Kenney, Freesound) | Claude or the owner | the sound's page link, saved with it | Only if a recording does not work after two tries |

Claude cuts, cleans, mixes and normalises every effect and saves it in the app as
`sfx_<name>.ogg`. Music is saved in the app as `music_<name>.ogg`. File names use only lowercase
letters, digits and underscores, as Android needs.

## Board sounds

| Sound | When it plays | Length | Mood | Volume | Made from |
| --- | --- | --- | --- | --- | --- |
| `sfx_preview` | The preview ring appears, if A-13 chooses a placing method with a preview | 60 ms | barely there | half | `rec_pencil_touch` |
| `sfx_place_red` (3 takes) | A red dot lands, at 0 ms | 100 to 150 ms | crisp, a full stop | full | `rec_pen_tap` |
| `sfx_place_blue` (3 takes) | A blue dot lands, at 0 ms; the computer's dot | 100 to 150 ms | soft and a little lower than red, so the computer's move is heard | full | `rec_marker_dab` |
| `sfx_ring_draw` | The capture ring is drawn, 150 to 550 ms. For a big capture (150 to 650 ms) the app plays it at 80% speed, which also lowers it | 400 ms | quick, sure stroke | full | `rec_pen_circle` |
| `sfx_ring_close` | The ring closes, at 550 ms (the medium vibration). Also any capture in a two-player game | 400 ms | bright, satisfying | full | code |
| `sfx_captured` | Instead of `sfx_ring_close`, when the computer captures the player's dots | 500 ms | "oh no", gentle, never mocking | full | code |
| `sfx_cut` | Each captured dot's cut, from 600 ms, 40 ms apart (30 ms in a big capture) | 60 ms | tiny tick | half | `rec_pen_flick` |
| `sfx_score` | The tally and the score take the points, at 1100 ms (1250 ms in a big capture) | 150 ms | a point lands | half | code |
| `sfx_capture_big` | A big capture's burst, at 650 ms (the strong vibration and the shake) | 700 ms | a thump and a chord: big, warm | full | `rec_palm_slap` and code |
| `sfx_threat_danger` | Threat rings draw in around one of the player's groups, at 0 ms of the threat pulse | 300 ms | a low double knock: "watch out" | full | code |
| `sfx_threat_chance` | Threat rings draw in around an opponent's group, at 0 ms of the threat pulse | 300 ms | a bright single note: "you could take this" | half | code |
| `sfx_dots_low` | A side's dots-left count reaches 5 and turns Highlighter | 200 ms | a soft low tick | half | code |
| `sfx_note_nudge` | The mission note swings at 3 dots left, mission not done | 300 ms | paper flutter | half | `rec_page_flutter` |
| `sfx_last_dot` | "Last dot!" writes in | 300 ms | quick handwriting | full | `rec_scribble` |
| `sfx_blocked` | A tap on a point where a dot cannot go | 80 ms | dull, soft, not a buzzer | half | `rec_eraser_tap` |
| `sfx_boss_turn` | The boss's countdown reaches its special turn (G-05) | 800 ms | cheeky, a little sneaky | full | code |
| `sfx_level_start` | The level opens: the mission note shows big, at 0 ms | 150 ms | a pen cap pulled off: "let's start" | full | `rec_cap_off` |

## Booster sounds

Each starts at 0 ms, when the player taps Confirm in the booster preview.

| Sound | When it plays | Length | Mood | Volume | Made from |
| --- | --- | --- | --- | --- | --- |
| `sfx_wall` | The wall: square drawn, then hatched | 400 ms | pencil, firm and tidy | full | `rec_pencil_square` |
| `sfx_circle_twice` | The hint: the point circled twice. Also the next level being circled on the map | 500 ms | pencil, light, pointing | full | `rec_pencil_circle_twice` |
| `sfx_bomb` | The bomb: it drops at 0 ms, the blast at 150 ms falls inside the file | 450 ms | a thud and a scrunch: big but friendly | full | `rec_book_drop` and `rec_paper_crumple` |
| `sfx_double_dot` | The "×2" chip grows in | 300 ms | two notes up: "again!" | full | code |
| `sfx_spark` | Each of the double dot's two dots lands, with the place sound | 250 ms | a tiny sparkle | half | code |

## Moments

| Sound | When it plays | Length | Mood | Volume | Made from |
| --- | --- | --- | --- | --- | --- |
| `sfx_area_light` | End of game: each captured area lights up. Each area plays one note higher (D, E, G, A, then on up) | 300 ms | a counting-up recap | half | code |
| `sfx_win` | "You won!" writes in on the result note; also the daily puzzle solved | 1.5 s | happy, short, not brassy | full | code |
| `sfx_lose` | The failure headline writes in | 1.2 s | kind, "nearly, try again" | full | code |
| `sfx_draw` | The draw headline writes in | 800 ms | even, neither up nor down | full | code |
| `sfx_star_1` | The first star is stamped (with its light vibration) | 400 ms | a stamp and a bell | full | `rec_stamp` and code |
| `sfx_star_2` | The second star is stamped | 400 ms | the same, one note higher | full | `rec_stamp` and code |
| `sfx_star_3` | The third star is stamped | 400 ms | the same, higher again, a little longer bell | full | `rec_stamp` and code |
| `sfx_path_draw` | Level unlock: the path to the next level inks in, 800 to 1200 ms | 400 ms | marker dashes | half | `rec_marker_dashes` |
| `sfx_page_turn` | A world opens: the new world's page slides in, 600 ms | 600 ms | a notebook page turned | full | `rec_page_turn` |
| `sfx_world_open` | A world opens, under the page turn, from 300 ms | 2 s | a rising run and a held chord: the biggest reward sound | full | code |
| `sfx_chest_open` | The chest opens over the map | 600 ms | a box lid lifted, then a sparkle | full | `rec_box_lid` and code |
| `sfx_reward` | A reward is picked in the chest; an item bought or earned in the Shop | 600 ms | sparkle and chord | full | code |
| `sfx_streak` | The daily streak goes up by one | 600 ms | three quick notes up | full | code |

## Interface sounds

| Sound | When it plays | Length | Mood | Volume | Made from |
| --- | --- | --- | --- | --- | --- |
| `sfx_button` | A button or switch is tapped (Start, Next, Retry, Confirm, Settings switches) | 60 ms | a ballpoint click | half | `rec_pen_click` |
| `sfx_note_open` | A note or card slides up: Level card, Pause, Result, Today's puzzle, Play a game, booster preview | 300 ms | paper slid across a table | half | `rec_paper_slide_in` |
| `sfx_note_close` | A note or card slides down; Back from one | 200 ms | a shorter slide away | half | `rec_paper_slide_out` |

## Deliberate silences

These moments have no sound on purpose:

| Moment | Why silent |
| --- | --- |
| Chain lines growing | They grow with every dot; the dot's sound covers them, and a second sound every turn would tire. |
| The computer's ripples and edge marks | The blue dot's own sound announces the move. |
| The pen stroke under the name of the side to move | It moves every turn. |
| +N flying to the tally | The ring, the cuts and the score note already make the capture heard. |
| The opponent's face changing | Faces have no voices. |
| The threat pulse's three beats, and faint rings | One sound as the rings draw in is enough; a repeating warning would nag. |
| Each dot after the count reaches 5 | The dot's own sound plays; the count's swell is silent. |
| The booster count dropping | The booster's own sound covers it. |
| The tool bar and pause icon leaving at the end of a game | The areas lighting up start right after. |
| A star not earned | No sound for what was missed. |
| Stars flying to the map, the level's stop filling, "next!" writing in | The path and the circling already play; more would crowd 2 seconds. |
| The loading screen | The app often opens in public places. |
| Scrolling the map | It happens all the time. |
| A streak lost | No sound for a loss the player did not watch happen. |
| Google Play screens (sign-in, achievements, purchase, ads) | Not ours; they bring their own. |

## Check against the motion storyboard

Every moment on the [motion page](motion.md) has a sound or a deliberate silence:

| Motion moment | Sound |
| --- | --- |
| Placing a dot | `sfx_place_red` or `sfx_place_blue` at 0 ms; `sfx_preview` before it if A-13 uses a preview |
| Chain lines | silence |
| The computer's move | `sfx_place_blue` at 0 ms; ripples and edge marks silent |
| Capture | `sfx_ring_draw` 150 ms, `sfx_ring_close` or `sfx_captured` 550 ms, `sfx_cut` from 600 ms, `sfx_score` 1100 ms |
| Big capture | `sfx_ring_draw` at 80% speed 150 ms, `sfx_capture_big` 650 ms, `sfx_cut` from 700 ms, `sfx_score` 1250 ms |
| Threat pulse | `sfx_threat_danger` or `sfx_threat_chance` at 0 ms; the pulses silent |
| Dots running out | `sfx_dots_low` at 5 left, `sfx_note_nudge` at 3 left, `sfx_last_dot` at 1 left |
| Wall | `sfx_wall` |
| Hint | `sfx_circle_twice` |
| Bomb | `sfx_bomb` |
| Double dot | `sfx_double_dot`, then `sfx_spark` with each dot |
| End of game | `sfx_area_light` for each area; the result note slides up with `sfx_note_open` |
| Star reveal | `sfx_win`, `sfx_lose` or `sfx_draw` at 0 ms; `sfx_star_1` to `sfx_star_3` as each star lands |
| Level unlock | `sfx_path_draw` 800 ms, `sfx_circle_twice` 1200 ms, `sfx_note_open` 1800 ms; when a world opens, `sfx_page_turn` and `sfx_world_open` first |
| Level opening | `sfx_level_start` at 0 ms |
| Notes and cards | `sfx_note_open` in, `sfx_note_close` out |

If X-11 makes Undo a booster, or G-06 adds special points with their own animation, they need a
row here too.

## The recordings

The owner records these with a phone. One session of about an hour covers them all.

**Set-up**

- A quiet room: windows shut, fans and music off. The phone in flight mode, so it does not buzz.
- A school notebook, open, lying on a wooden table. Use the same notebook and pens throughout.
- The phone lying on a folded cloth, 20 to 30 cm from the spot on the page, with its bottom edge
  (the microphone) towards it.
- The recorder app at its best quality (lossless or WAV if it offers it). Any format the phone
  saves is fine; only the name before the dot matters.

**For each recording**

1. Start recording and stay silent for 2 seconds (Claude uses that to remove the room's hiss).
2. Make the sound 5 times, about 1 second apart.
3. Stop, and name the file exactly as below, for example `rec_pen_tap.m4a`.
4. If the level meter touches the top, move the phone a little farther and record again.

Claude cuts the takes, picks the cleanest, and keeps three takes of each dot sound. The owner
listens in the V-08 pull request and may name a different take.

| File to save | What to record | Becomes |
| --- | --- | --- |
| `rec_pencil_touch` | A sharp pencil set down lightly on the page, without moving | `sfx_preview` |
| `rec_pen_tap` | A ballpoint pen tip tapped once, firmly, on the page, like a full stop | `sfx_place_red` |
| `rec_marker_dab` | A felt-tip marker pressed once on the page, a little softer than the pen | `sfx_place_blue` |
| `rec_pen_circle` | A small circle, 2 cm across, drawn fast in one ballpoint stroke, in under half a second | `sfx_ring_draw` |
| `rec_pen_flick` | A short, fast ballpoint slash, 1 cm long | `sfx_cut` |
| `rec_palm_slap` | A flat palm patted once on the closed notebook | `sfx_capture_big` |
| `rec_page_flutter` | A page corner lifted 3 cm and let fall | `sfx_note_nudge` |
| `rec_scribble` | The word "last" written fast in joined-up handwriting | `sfx_last_dot` |
| `rec_eraser_tap` | The rubber end of a pencil tapped once on the page | `sfx_blocked` |
| `rec_cap_off` | A pen cap pulled off briskly | `sfx_level_start` |
| `rec_pencil_square` | A small square drawn in pencil, then five quick hatch strokes inside it | `sfx_wall` |
| `rec_pencil_circle_twice` | A point circled twice in pencil, quickly, as if marking an answer | `sfx_circle_twice` |
| `rec_book_drop` | A closed paperback dropped flat on the table from 10 cm | `sfx_bomb` |
| `rec_paper_crumple` | A loose sheet of paper scrunched once, fast | `sfx_bomb` |
| `rec_stamp` | A rubber stamp pressed on the page; without one, the flat end of a marker pressed hard | `sfx_star_1` to `sfx_star_3` |
| `rec_marker_dashes` | Four short marker dashes in a row | `sfx_path_draw` |
| `rec_page_turn` | A notebook page turned briskly | `sfx_page_turn` |
| `rec_box_lid` | The lid of a small cardboard box lifted off | `sfx_chest_open` |
| `rec_pen_click` | A click-top ballpoint clicked once (the press only) | `sfx_button` |
| `rec_paper_slide_in` | A sheet of paper slid 20 cm across the table towards the phone | `sfx_note_open` |
| `rec_paper_slide_out` | The same sheet slid away from the phone, quicker | `sfx_note_close` |

## Sounds made in code

Claude makes these in V-08 with a script kept in the repository; the script is their prompt. The
sounds are:

- **mallet:** a soft felt mallet on a small metal bar: a pure tone with faint upper overtones,
  a 5 ms start and a smooth fade;
- **pluck:** a soft nylon string, warmer and shorter than the mallet;
- **wood:** a short, hollow knock with a little pitch.

Notes are named with their octave: D5 is the D above middle C (C4).

| Sound | Recipe | Length |
| --- | --- | --- |
| `sfx_ring_close` | Mallet D5, then A5 60 ms later | 400 ms |
| `sfx_captured` | Mallet A4, then E4 80 ms later, softer | 500 ms |
| `sfx_score` | Mallet A5, short | 150 ms |
| `sfx_capture_big` | `rec_palm_slap`, with mallet and pluck chord D4, A4, E5 on it | 700 ms |
| `sfx_threat_danger` | Wood G3 twice, 120 ms apart | 300 ms |
| `sfx_threat_chance` | Mallet E6, swelling in over 50 ms | 300 ms |
| `sfx_dots_low` | Wood D4, soft | 200 ms |
| `sfx_boss_turn` | Pluck D3 and G3 together, then a slow wobble in pitch | 800 ms |
| `sfx_double_dot` | Mallet D5, then D6 80 ms later | 300 ms |
| `sfx_spark` | Mallet A6, D7, E7 within 60 ms, quiet | 250 ms |
| `sfx_area_light` | Mallet D5; the app plays it faster for each later area, one note of D, E, G, A higher | 300 ms |
| `sfx_win` | Mallet D5, G5, A5, D6, 120 ms apart, the last held, over a pluck chord D4, A4 | 1.5 s |
| `sfx_lose` | Mallet A4, G4, E4, 180 ms apart, the last soft | 1.2 s |
| `sfx_draw` | Mallet D5, then A4 | 800 ms |
| `sfx_star_1` | `rec_stamp`, with mallet D6 | 400 ms |
| `sfx_star_2` | `rec_stamp`, with mallet E6 | 400 ms |
| `sfx_star_3` | `rec_stamp`, with mallet A6, held longer | 400 ms |
| `sfx_world_open` | Mallet D5, E5, G5, A5, D6, E6, 80 ms apart, then a pluck and mallet chord D4, A4, E5, held | 2 s |
| `sfx_chest_open` | `rec_box_lid`, then `sfx_spark` | 600 ms |
| `sfx_reward` | Mallet A6, D7, E7, A7 within 150 ms, over a pluck chord D4, A4 | 600 ms |
| `sfx_streak` | Mallet E5, G5, A5, 90 ms apart | 600 ms |

## Music

All six tracks are instrumental and calm, because players are thinking. They share one family of
instruments (felt piano, soft percussion, warm bass) and nearby keys, so the music can fade from
the map into a level without a jolt.

| File to save | Plays on | Loop length | Key, speed | Mood |
| --- | --- | --- | --- | --- |
| `music_map.wav` | The map and everything over it: Level card, chest, Today's puzzle, Play a game, Rules, Stats, Shop, Settings | 1.5 to 2 min | C major, 90 BPM | warm, playful, inviting |
| `music_notebook.wav` | Boards of World 1, the tutorial included | 2 to 3 min | F major, 80 BPM | calm focus, a little playful |
| `music_garden.wav` | Boards of World 2 | 2 to 3 min | G major, 84 BPM | gentle, sunny |
| `music_night_sky.wav` | Boards of World 3 | 2 to 3 min | D major, 70 BPM | dreamy, peaceful, awake |
| `music_sea.wav` | Boards of World 4 | 2 to 3 min | A dorian, 76 BPM, 6/8 | breezy, flowing |
| `music_boss.wav` | Boss levels, in any world | 1 to 1.5 min | D minor, 100 BPM | mischievous tension, never scary |

The daily puzzle and free play use the track of the world the player is in. Two-player games use
`music_notebook.wav`. Under `sfx_win`, `sfx_lose`, `sfx_draw` and `sfx_world_open` the music drops
to half its volume and comes back after.

### Before the first track

1. **Subscribe to Suno Pro first.** Songs made on the free plan carry no commercial rights, and
   upgrading later does not change that.
2. On the same day, before generating, save Suno's terms and its "Does Suno own the music I make?"
   help page as PDF, named `suno-terms-YYYY-MM-DD.pdf` and `suno-ownership-YYYY-MM-DD.pdf` with
   that day's date. They go in the licence copies folder (V-14).
3. Keep the receipt or a screenshot of the plan and its dates privately. It shows personal details,
   so it never enters the repository, which is public.

### Settings for every track

- **Custom** mode, **Instrumental** on, the newest model Suno offers that day.
- **Style:** the track's prompt below, pasted exactly.
- **Exclude** (the same for every track):

  ```
  vocals, singing, choir, humming, spoken word, EDM drop, heavy drums, distorted guitar, trap, dubstep, fade out
  ```

- **Weirdness** about 35% and **Style influence** about 65%, if the sliders show. These are a
  starting point, not a rule.
- **Title:** as given below. The title does not change the music.
- Never use Upload, Cover or Persona with anything that is not ours.

### The prompts

**`music_map.wav`**, title `Pinfold Map`

```
Warm, playful instrumental for a puzzle game's home screen. Felt piano melody, pizzicato strings, soft glockenspiel accents, upright bass, light brushed snare. Curious and inviting, like doodling in a notebook margin on a sunny afternoon. 90 BPM, C major, steady groove throughout, simple repeating phrases, gentle and uncluttered, no build-up, no ending.
```

**`music_notebook.wav`**, title `Pinfold Notebook`

```
Calm, focused instrumental for thinking. Muted felt piano, warm electric piano chords, soft clarinet countermelody, quiet shaker and soft wood block like a pencil tapping. Cozy study room, quiet concentration, a little playful. 80 BPM, F major, steady and even, sparse arrangement with lots of space, same energy from start to end, no ending.
```

**`music_garden.wav`**, title `Pinfold Garden`

```
Gentle, sunny instrumental. Nylon-string acoustic guitar picking, soft marimba, airy flute melody, light hand percussion and shaker, warm upright bass. Morning in a garden, birdsong mood without birds, relaxed and bright but calm enough to think. 84 BPM, G major, steady groove throughout, simple repeating melody, no build-up, no ending.
```

**`music_night_sky.wav`**, title `Pinfold Night Sky`

```
Dreamy, calm instrumental. Celesta and music box melody, soft warm synth pad, slow low strings, felt piano, very light brushed percussion. Starry night, quiet wonder, peaceful but awake, not sleepy. 70 BPM, D major with a lifted, floating feel, slow and even, spacious, same energy from start to end, no ending.
```

**`music_sea.wav`**, title `Pinfold Sea`

```
Breezy, flowing instrumental in a gentle 6/8 sway. Harp arpeggios, soft vibraphone, warm string pads swelling like waves, upright bass, light brushed drums. Calm seaside, open horizon, soft sunlight on water, relaxed and steady. 76 BPM, A dorian, rolling rhythm throughout, simple repeating phrases, no build-up, no ending.
```

**`music_boss.wav`**, title `Pinfold Boss`

```
Playful, mischievous tension for a friendly boss battle in a puzzle game. Low pizzicato strings, staccato bassoon, ticking wood block, soft timpani hits, repeating piano pattern. Cheeky villain sneaking around, focused and a bit urgent but never scary or loud. 100 BPM, D minor, tight steady groove throughout, short repeating phrases, no ending.
```

### Choosing and saving

1. Generate 2 to 4 versions of each track. Keep the one with the steadiest middle: V-09 cuts the
   loop from the middle, so the start and the ending do not matter.
2. Reject any version with a voice, or a melody that sounds familiar.
3. Run the kept version through a song-recognition app (for example Shazam). A match means: reject
   it and generate again.
4. Download the kept version as WAV and name it as in the table above.
5. Add its entry to `music_log.txt`, in the same folder. If a prompt was changed, the log holds the
   prompt actually used, and that is the one that goes in the asset register.

```
music_map.wav
  date: YYYY-MM-DD
  song link: https://suno.com/song/...
  model: (the model name Suno showed)
  prompt changes: none
  Shazam: no match
```

## Handing the files over

The files are made outside the repository, in one folder that the owner backs up and keeps:

```
Pinfold-audio/
  recordings/   rec_*.m4a (or .wav), as recorded
  music/        music_*.wav, full length, and music_log.txt
  licences/     suno-terms-YYYY-MM-DD.pdf, suno-ownership-YYYY-MM-DD.pdf
```

This folder is the master copy. Only the finished, compressed files enter the repository: the
full-length WAVs are large, and git would keep every version for ever.

To bring them in, the owner starts a session with, for example, "Work on V-09, files in
~/Pinfold-audio". In that session Claude cuts and converts the files, puts them in the app, adds
their rows to the [asset register](../assets.md) and the licence copies, and opens the pull
request. The owner listens and merges. V-08 does the same for the recordings, and V-10 makes the
app play them.
