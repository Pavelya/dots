# Sound

This page names every sound in Pinfold before it is made: 17 sound effects and 6 music tracks.
For each one it gives when it plays, how long it is, its mood, and how it is made, with the exact
file name to save and the exact prompt or recipe. The effects are made in V-08, the music in V-09,
and V-10 makes the app play them.

The times come from the [motion storyboard](motion.md): a sound starts at the moment its animation
step starts, and its "When it plays" names that step.

## Rules for all sound

1. **The game works on mute.** Many players keep the sound off. Every moment is carried by the
   animation and the vibration; sound adds to them and never carries information alone.
2. **Few sounds in a level.** In a level, only what the player needs to notice makes a sound: a
   dot landing, a capture, the player's own group in danger, the dots running low, the boss's
   special turn and the boosters. Buttons, notes and cards are silent everywhere.
3. **Sound and vibration start together.** A sound that goes with a vibration (motion rule 6)
   starts at the same millisecond: the dot at 0 ms, the closing ring at 550 ms, the big capture at
   650 ms, the bomb's blast at 150 ms.
4. **Pen and paper, and soft notes.** The three sounds of things happening on the board are a pen
   tap, a pen drawing a circle and a soft thud. Sounds of reward and warning are soft notes, like
   felt mallets on small metal bars, made in code.
5. **Notes that fit every track.** Notes made in code use only D, E, G and A, in any octave. These
   four notes belong to the key of every music track, so a sound never clashes with the music.
6. **One sound, several uses.** The app plays a sound faster or slower to make it higher or lower,
   so one file serves several moments: the computer's dot, the big capture, the wall, the hint,
   undo, the stars and the areas lighting up. This keeps the set small.
7. **The dot never sounds mechanical.** It is heard every turn, so it has three takes, played in
   turn at slightly different speeds (95% to 105%), at half volume.
8. **A tap stops the sound with the animation.** A tap that finishes an animation (motion rule 1)
   also fades its sounds out in 50 ms.
9. **Same loudness in every file.** Every effect is normalised to the same loudness (the V-08
   check: within 2 dB). The app sets each sound's volume from the "Volume" column.
10. **Two players on one phone.** Red dots play the dot at normal speed, blue dots at 85%. Every
    capture plays `sfx_ring_close`; `sfx_captured` is not used.

## Where each sound comes from

| Source | Who makes it | Files to save | Sounds |
| --- | --- | --- | --- |
| Suno Sounds, during the Suno Pro month (decision 0020) | The owner | `fx_*.wav` ([the three source sounds](#the-three-source-sounds)) | The dot, the ring, the bomb |
| A phone recording, if Suno's version is not clean | The owner | `fx_*.m4a` or `.wav` | The same three, one by one as needed |
| Made in code | Claude, in V-08, with a script kept in the repository | none; the script makes `sfx_*` | The other 14 |
| Suno songs, during the Suno Pro month | The owner | `music_*.wav` ([music](#music)) | The six music tracks |
| CC0 (Kenney, Freesound) | Claude or the owner | the sound's page link, saved with it | Only if both Suno and the phone fail |

Claude trims, cleans and normalises every effect and saves it in the app as `sfx_<name>.ogg`.
Music is saved in the app as `music_<name>.ogg`. File names use only lowercase letters, digits and
underscores, as Android needs.

## Sound effects

**Volume:** "half" is for sounds heard often; "full" for the rest.

### In a level

| Sound | When it plays | Length | Mood | Volume | Made from |
| --- | --- | --- | --- | --- | --- |
| `sfx_place_1`, `_2`, `_3` | A dot lands, at 0 ms. Red dots at normal speed; blue dots, the computer's included, at 85%, so they sound a little lower and the computer's move is heard | 100 to 150 ms | crisp, a full stop | half | `fx_pen_tap` |
| `sfx_ring_draw` | The capture ring is drawn, 150 to 550 ms. A big capture's ring (150 to 650 ms) plays it at 80% speed, which also lowers it | 400 ms | quick, sure stroke | full | `fx_pen_circle` |
| `sfx_ring_close` | The player's ring closes, at 550 ms (the medium vibration); any capture in a two-player game | 400 ms | bright, satisfying | full | code |
| `sfx_captured` | Instead of `sfx_ring_close`, when the computer captures the player's dots | 500 ms | "oh no", gentle, never mocking | full | code |
| `sfx_capture_big` | A big capture's burst, at 650 ms (the strong vibration and the shake) | 700 ms | a soft thump and a warm chord | full | code |
| `sfx_threat` | Threat rings draw in around one of the player's own groups, at 0 ms of the threat pulse. Not for the opponent's groups | 300 ms | a low double knock: "watch out" | full | code |
| `sfx_dots_low` | The player's dots-left count reaches 5 and turns Highlighter. Once per game | 200 ms | a soft low tick | half | code |
| `sfx_boss_turn` | The boss's countdown reaches its special turn (G-05) | 800 ms | cheeky, a little sneaky | full | code |

### Boosters

Each starts at 0 ms, when the player taps Confirm in the booster preview.

| Booster | What plays | Length | Volume |
| --- | --- | --- | --- |
| Wall | `sfx_ring_draw` at normal speed: the square being drawn | 400 ms | full |
| Hint | `sfx_ring_draw` twice at 160% speed, 250 ms apart: the point circled twice, lighter and higher | 500 ms | full |
| Bomb | `sfx_bomb`, made from `fx_book_drop`: the bomb drops at 0 ms and the thud lands at 150 ms, with the blast | 450 ms | full |
| Double dot | `sfx_double_dot` (code), as the "×2" chip grows in: two notes up, "again!" | 300 ms | full |
| Undo | `sfx_ring_draw` at 75% speed: one slow, low scrub as the turn is rubbed out. The booster flying back is silent | 530 ms | full |

### After a level and on the map

| Sound | When it plays | Length | Mood | Volume | Made from |
| --- | --- | --- | --- | --- | --- |
| `sfx_area_light` | End of game: each captured area lights up. Each area plays one note higher (D, E, G, A, then on up) | 300 ms | a counting-up recap | half | code |
| `sfx_win` | "You won!" writes in on the result note; the daily puzzle solved | 1.5 s | happy, short, not brassy | full | code |
| `sfx_lose` | The failure headline writes in | 1.2 s | kind, "nearly, try again" | full | code |
| `sfx_draw` | The draw headline writes in | 800 ms | even, neither up nor down | full | code |
| `sfx_star` | Each earned star is stamped (with its light vibration). Star 1 at normal speed, star 2 at 112% (one note up), star 3 at 150% (two notes up) | 400 ms | a soft click and a bell | full | code |
| `sfx_world_open` | A world opens: from 300 ms of the page turn | 2 s | a rising run and a held chord: the biggest reward sound | full | code |
| `sfx_reward` | The chest opens; an item bought or earned in the Shop; the daily streak goes up | 600 ms | sparkle and chord | full | code |

## Deliberate silences

These moments have no sound on purpose:

| Moment | Why silent |
| --- | --- |
| The preview ring, and drag's row and column guides | They would play on almost every tap. |
| Chain lines growing | They grow with every dot; the dot's sound covers them. |
| The computer's ripples and edge marks | The blue dot's own sound announces the move. |
| The pen stroke under the name of the side to move | It moves every turn. |
| The cuts across captured dots, +N flying, the score swelling | The ring and its closing sound already make the capture heard. |
| The opponent's face changing | Faces have no voices. |
| Threat rings around the opponent's groups | Threats come often on a crowded board; only danger to the player's own dots is worth a sound. |
| The threat pulse's three beats, and faint rings | One sound as the rings draw in is enough. |
| The mission note's nudge at 3 dots left, and "Last dot!" | `sfx_dots_low` already warned once; a sound every turn near the end would nag. |
| A tap where a dot cannot go | The tap simply does nothing; a buzz would scold. |
| The level opening | The level's music starting is enough. |
| The booster count dropping | The booster's own sound covers it. |
| The tool bar and pause icon leaving at the end of a game | The areas lighting up start right after. |
| A star not earned | No sound for what was missed. |
| On the map: stars flying, the level's stop filling, the path inking in, the next level circled, "next!" | The map's music carries it; these play after every level. |
| Buttons, switches, and notes or cards sliding in and out | Interface sounds are what players find most annoying. |
| The loading screen | The app often opens in public places. |
| Scrolling the map | It happens all the time. |
| A streak lost | No sound for a loss the player did not watch happen. |
| Google Play screens (sign-in, achievements, purchase, ads) | Not ours; they bring their own. |

## Check against the motion storyboard

Every moment on the [motion page](motion.md) has a sound or a deliberate silence:

| Motion moment | Sound |
| --- | --- |
| Placing a dot | `sfx_place_*` at 0 ms; the preview silent |
| Chain lines | silence |
| The computer's move | `sfx_place_*` at 85% speed at 0 ms; ripples and edge marks silent |
| Capture | `sfx_ring_draw` 150 ms, `sfx_ring_close` or `sfx_captured` 550 ms; the cuts, +N and the score silent |
| Big capture | `sfx_ring_draw` at 80% speed 150 ms, `sfx_capture_big` 650 ms; the rest silent |
| Threat pulse | `sfx_threat` at 0 ms when the player's own group is in danger; otherwise, and the pulses, silent |
| Dots running out | `sfx_dots_low` at 5 left; the nudge and "Last dot!" silent |
| Wall | `sfx_ring_draw` |
| Hint | `sfx_ring_draw` twice at 160% speed |
| Bomb | `sfx_bomb` |
| Double dot | `sfx_double_dot`, then `sfx_place_*` with each dot |
| Undo | `sfx_ring_draw` at 75% speed; a booster flying back silent |
| End of game | `sfx_area_light` for each area; the result note silent |
| Star reveal | `sfx_win`, `sfx_lose` or `sfx_draw` at 0 ms; `sfx_star` as each earned star lands |
| Level unlock | silent; when a world opens, `sfx_world_open` with the page turn |
| Level opening | silent |
| Notes and cards | silent |

If G-06 adds special points with their own animation, they need a row here too.

## The three source sounds

Three effects start from a real-sounding source: a pen tap, a pen drawing a circle and a soft
thud. Try Suno Sounds first, during the Suno Pro month. If Suno's version of a sound is not clean
after three tries, record that one sound with a phone instead.

| File to save | Becomes | Suno Sounds prompt (One Shot) | Phone fallback: what to record |
| --- | --- | --- | --- |
| `fx_pen_tap_1.wav`, `_2`, `_3` (three different versions) | `sfx_place_1` to `_3` | `A single ballpoint pen tip tapping once on a paper notebook page on a wooden table. Dry, close, quiet room, no reverb, no music.` | A ballpoint pen tip tapped once, firmly, on the page, like a full stop |
| `fx_pen_circle.wav` | `sfx_ring_draw` | `A ballpoint pen quickly drawing one small circle on paper in a single stroke, about half a second long. Dry, close, no music.` | A small circle, 2 cm across, drawn fast in one ballpoint stroke, in under half a second |
| `fx_book_drop.wav` | `sfx_bomb` | `A closed paperback book dropped flat onto a wooden table from a short height. One soft thud, dry, no music.` | A closed paperback dropped flat on the table from 10 cm |

### With Suno Sounds

1. In Suno, open **Create**, then **Sounds** (a beta mode; its place in the menu may move), and
   choose **One Shot**.
2. Paste the prompt exactly. Each generation gives two versions.
3. Keep a version only if it is the sound alone: no hiss, no echo, no music, no voice, and a clean,
   quick end. Generate again if not, up to three times.
4. Download each kept version as WAV and name it as in the table. For the pen tap, keep three
   versions that sound slightly different.
5. Add an entry for each to `audio_log.txt` (see [Choosing and saving](#choosing-and-saving)).

### With a phone

- A quiet room, windows shut, fans and music off, the phone in flight mode.
- A school notebook open on a wooden table; the phone on a folded cloth 20 to 30 cm away, its
  bottom edge (the microphone) towards the page.
- The recorder app at its best quality (Voice Memos on iPhone: Settings, Apps, Voice Memos, Audio
  Quality, Lossless).
- Start recording, stay silent for 2 seconds (Claude uses that to remove the room's hiss), make
  the sound 5 times about 1 second apart, and stop.
- Name the file as in the table without the number, for example `fx_pen_tap.m4a`. Claude cuts the
  takes and keeps the cleanest three of the pen tap.

## Sounds made in code

Claude makes these in V-08 with a script kept in the repository; the script is their prompt. The
sounds are:

- **mallet:** a soft felt mallet on a small metal bar: a pure tone with faint upper overtones,
  a 5 ms start and a smooth fade;
- **pluck:** a soft nylon string, warmer and shorter than the mallet;
- **wood:** a short, hollow knock with a little pitch;
- **thump:** a low, soft tone that falls quickly in pitch, felt more than heard.

Notes are named with their octave: D5 is the D above middle C (C4).

| Sound | Recipe | Length |
| --- | --- | --- |
| `sfx_ring_close` | Mallet D5, then A5 60 ms later | 400 ms |
| `sfx_captured` | Mallet A4, then E4 80 ms later, softer | 500 ms |
| `sfx_capture_big` | Thump, with a mallet and pluck chord D4, A4, E5 on it | 700 ms |
| `sfx_threat` | Wood G3 twice, 120 ms apart | 300 ms |
| `sfx_dots_low` | Wood D4, soft | 200 ms |
| `sfx_boss_turn` | Pluck D3 and G3 together, then a slow wobble in pitch | 800 ms |
| `sfx_double_dot` | Mallet D5, then D6 80 ms later | 300 ms |
| `sfx_area_light` | Mallet D5; the app plays it faster for each later area, one note of D, E, G, A higher | 300 ms |
| `sfx_win` | Mallet D5, G5, A5, D6, 120 ms apart, the last held, over a pluck chord D4, A4 | 1.5 s |
| `sfx_lose` | Mallet A4, G4, E4, 180 ms apart, the last soft | 1.2 s |
| `sfx_draw` | Mallet D5, then A4 | 800 ms |
| `sfx_star` | A short, soft paper click, then mallet D6 | 400 ms |
| `sfx_world_open` | Mallet D5, E5, G5, A5, D6, E6, 80 ms apart, then a pluck and mallet chord D4, A4, E5, held | 2 s |
| `sfx_reward` | Mallet A6, D7, E7, A7 within 150 ms, over a pluck chord D4, A4 | 600 ms |

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

### Before the first track or sound

1. **Subscribe to Suno Pro first.** Songs and sounds made on the free plan carry no commercial
   rights, and upgrading later does not change that.
2. On the same day, before generating, save Suno's terms and its "Does Suno own the music I make?"
   help page as PDF, named `suno-terms-YYYY-MM-DD.pdf` and `suno-ownership-YYYY-MM-DD.pdf` with
   that day's date. They go in the licence copies folder (V-14). Check that they cover Sounds as
   well as songs; if they do not, record the three source sounds with the phone instead.
3. Keep the receipt or a screenshot of the plan and its dates privately. It shows personal details,
   so it never enters the repository, which is public.

### Settings for every track

Every track is instrumental: music only, with no voice of any kind. Three things make sure of it:
the **Instrumental** switch, the first words of each prompt, and the Exclude list.

- **Custom** mode, the newest model Suno offers that day.
- **Instrumental: on.** Check it before every generation; it is the main guard against a voice.
  With it on, the lyrics box disappears or is ignored. If the switch is missing, type only
  `[Instrumental]` in the lyrics box. Never leave the lyrics box empty with the switch off: Suno
  may then add a voice of its own.
- **Style:** the track's prompt below, pasted exactly. Each starts with "Instrumental only, no
  vocals, no lyrics."
- **Exclude** (the same for every track):

  ```
  vocals, lyrics, singing, rap, choir, humming, vocal chops, vocal samples, spoken word, EDM drop, heavy drums, distorted guitar, fade out
  ```

- **Weirdness** about 35% and **Style influence** about 65%, if the sliders show. These are a
  starting point, not a rule.
- **Title:** as given below. The title does not change the music.
- Never use Upload, Cover or Persona with anything that is not ours.

### The prompts

**`music_map.wav`**, title `Pinfold Map`

```
Instrumental only, no vocals, no lyrics. Warm, playful instrumental for a puzzle game's home screen. Felt piano melody, pizzicato strings, soft glockenspiel accents, upright bass, light brushed snare. Curious and inviting, like doodling in a notebook margin on a sunny afternoon. 90 BPM, C major, steady groove throughout, simple repeating phrases, gentle and uncluttered, no build-up, no ending.
```

**`music_notebook.wav`**, title `Pinfold Notebook`

```
Instrumental only, no vocals, no lyrics. Calm, focused instrumental for thinking. Muted felt piano, warm electric piano chords, soft clarinet countermelody, quiet shaker and soft wood block like a pencil tapping. Cozy study room, quiet concentration, a little playful. 80 BPM, F major, steady and even, sparse arrangement with lots of space, same energy from start to end, no ending.
```

**`music_garden.wav`**, title `Pinfold Garden`

```
Instrumental only, no vocals, no lyrics. Gentle, sunny instrumental. Nylon-string acoustic guitar picking, soft marimba, airy flute melody, light hand percussion and shaker, warm upright bass. Morning in a garden, birdsong mood without birds, relaxed and bright but calm enough to think. 84 BPM, G major, steady groove throughout, simple repeating melody, no build-up, no ending.
```

**`music_night_sky.wav`**, title `Pinfold Night Sky`

```
Instrumental only, no vocals, no lyrics. Dreamy, calm instrumental. Celesta and music box melody, soft warm synth pad, slow low strings, felt piano, very light brushed percussion. Starry night, quiet wonder, peaceful but awake, not sleepy. 70 BPM, D major with a lifted, floating feel, slow and even, spacious, same energy from start to end, no ending.
```

**`music_sea.wav`**, title `Pinfold Sea`

```
Instrumental only, no vocals, no lyrics. Breezy, flowing instrumental in a gentle 6/8 sway. Harp arpeggios, soft vibraphone, warm string pads swelling like waves, upright bass, light brushed drums. Calm seaside, open horizon, soft sunlight on water, relaxed and steady. 76 BPM, A dorian, rolling rhythm throughout, simple repeating phrases, no build-up, no ending.
```

**`music_boss.wav`**, title `Pinfold Boss`

```
Instrumental only, no vocals, no lyrics. Playful, mischievous tension for a friendly boss battle in a puzzle game. Low pizzicato strings, staccato bassoon, ticking wood block, soft timpani hits, repeating piano pattern. Cheeky villain sneaking around, focused and a bit urgent but never scary or loud. 100 BPM, D minor, tight steady groove throughout, short repeating phrases, no ending.
```

### Choosing and saving

1. Generate 2 to 4 versions of each track. Keep the one with the steadiest middle: V-09 cuts the
   loop from the middle, so the start and the ending do not matter.
2. Reject any version with a voice anywhere in it, even a short "ooh", a hum or a whisper, and any
   version with a melody that sounds familiar. Listen to the whole track: a voice sometimes comes in
   only near the end.
3. Run the kept version through a song-recognition app (for example Shazam). A match means: reject
   it and generate again.
4. Download the kept version as WAV and name it as in the table above.
5. Add its entry to `audio_log.txt`, at the top of the folder. The three source sounds get entries
   too. If a prompt was changed, the log holds the prompt actually used, and that is the one that
   goes in the asset register.

```
music_map.wav
  made with: Suno song
  date: YYYY-MM-DD
  link: https://suno.com/song/...
  model: (the model name Suno showed)
  prompt changes: none
  Shazam: no match

fx_pen_tap_1.wav
  made with: Suno Sounds (or: phone recording)
  date: YYYY-MM-DD
  link: https://suno.com/song/...
  prompt changes: none
```

## Handing the files over

The files are made outside the repository, in one folder that the owner backs up and keeps:

```
Pinfold-audio/
  audio_log.txt
  sounds/     fx_*.wav (Suno Sounds) or fx_*.m4a (phone)
  music/      music_*.wav, full length
  licences/   suno-terms-YYYY-MM-DD.pdf, suno-ownership-YYYY-MM-DD.pdf
```

This folder is the master copy. Only the finished, compressed files enter the repository: the
full-length WAVs are large, and git would keep every version for ever. All 17 effects together
come to about 0.1 MB in the app; the six music loops to about 7 to 11 MB, so V-10 looks at the
music if the app's size matters.

To bring them in, the owner starts a session with, for example, "Work on V-09, files in
~/Pinfold-audio". In that session Claude cuts and converts the files, puts them in the app, adds
their rows to the [asset register](../assets.md) and the licence copies, and opens the pull
request. The owner listens and merges. V-08 does the same for the effects, and V-10 makes the app
play them.
