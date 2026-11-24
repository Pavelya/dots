# Releasing Pinfold on Google Play

A store release starts with a tag such as `v0.1.2` on `main`. The tag starts the workflow
`store release` (`.github/workflows/release.yml`), which tests the game, builds it, signs it with
the upload key and puts it on Google Play's **internal testing** track. Moving a release on to the
closed test or to production is done by hand in Play Console. The rules for tags, version numbers
and keys are in decision 0031.

This page has:

- [Make a release](#make-a-release): the three commands, when to release, which number to use.
- [Crash reports](#crash-reports): where crashes show up, what a report holds, a test crash.
- [Analytics](#analytics): what the game reports, the funnel to level 10, checking events.
- [Ads](#ads): the rewarded ads, Google's consent form, and checking them on a phone.
- [What is set up](#what-is-set-up): what exists in Play Console, Google Cloud and GitHub, and
  which rights each account has. Tasks that work in those consoles build on it.
- [If something goes wrong](#if-something-goes-wrong) and [Keys and secrets](#keys-and-secrets).
- [Setting it up from nothing](#setting-it-up-from-nothing): the full walkthrough, step by step,
  for redoing any part of the setup, such as a new upload key.

**Reading the commands.** Open this page in a Markdown preview (in VS Code: Cmd+Shift+V) or on
GitHub, so each command shows in its own box. Copy a whole box, from its first character to its
last, and paste it in the terminal. Run every command from the repository folder (`cd ~/dots`, or
wherever the repository is).

## Make a release

1. Pick the next version (see [Version numbers](#version-numbers)).
2. Tag the newest `main` and push the tag (here `v0.1.2`):

   ```sh
   git fetch origin
   ```

   ```sh
   git tag v0.1.2 origin/main
   ```

   ```sh
   git push origin v0.1.2
   ```

3. Open
   [github.com/Pavelya/dots/actions/workflows/release.yml](https://github.com/Pavelya/dots/actions/workflows/release.yml)
   and click the newest run, the one with your tag beside it. It takes about six minutes. When it
   is green and its last step says "is out on the internal track", internal testers get the update
   from Google Play, usually within minutes.

Nothing else is needed: no step in Play Console, no upload by hand.

The run keeps the signed build and R8's mapping file as an artifact, `pinfold-<version>-bundle`.
Google Play also gets the mapping file inside the build, and the run sends it to Firebase
Crashlytics, so both show crash reports with the real code names.

**Dry run.** `gh workflow run release.yml -f version=0.0.1`, or Actions > "store release" > Run
workflow on any branch. It builds and signs the game and uploads nothing. Use it after changing the
upload key or the workflow.

### When to release

Not after every merge. Every merge to `main` already builds a test APK (`dots-debug-apk` in the
`test` run) for a quick look. Release to the internal track when the game should be tried on a
phone the way players will get it:

- after a group of tasks that change what the player sees or hears (a new world, the shop);
- before showing the game to someone;
- before the closed test (R-06), which starts from an internal release.

### Version numbers

Each tag is `v` plus three numbers; the version code follows from them (decision 0031).

- **Patch** (`v0.1.2`, `v0.1.3`, ...) for ordinary builds.
- **Minor** (`v0.2.0`) when something big lands, such as the end of a phase.
- **`v1.0.0`** for the launch (R-07).

A number is used once and must be higher than every one before. A broken build is fixed on `main`
and released with the next patch number, never by moving a tag. The one exception is a run that
failed for a reason outside the code (see the first row of [If something goes
wrong](#if-something-goes-wrong)): re-run it, same tag.

## Crash reports

Crashes of store builds show up in two places (decision 0032). Debug builds from GitHub send
nothing.

| Where | What it shows |
| --- | --- |
| **Firebase Crashlytics**: [console.firebase.google.com](https://console.firebase.google.com/) > `pinfold` > Crashlytics | Every crash of every player within minutes, grouped by cause, with the stack trace and the phone. The top of the page shows **crash-free sessions** and crash-free users, per version. The closed test (R-06) needs crash-free sessions at least 99.5%. |
| **Android vitals**: Play Console > Pinfold > Monitor and improve > Android vitals | Crashes and ANRs (the app froze for 5 seconds), but only from players who share usage and diagnostics with Google, about a day late. Google Play ranks apps by these numbers, so check them before each move to closed testing or production. |

**After each release,** look at Crashlytics the next day: any new issue in the new version gets a
task card (`Q-` letter) with its stack trace.

**What a report holds.** Only what Crashlytics collects by itself: the stack trace, the app's
state and the phone's model and Android version at the moment of the crash, and a random ID for the
installation. The app adds no names, levels or IDs of its own. For the Data safety form (R-03):
crash logs, diagnostics, and device or other IDs; collected, not shared, encrypted in transit,
used for crash reporting.

**A test crash.** To check that reports arrive, crash a store build on purpose. This needs the
phone connected to the Mac by USB with USB debugging on (Settings > About phone > tap Build number
seven times, then Settings > System > Developer options > USB debugging), and `adb`, which Homebrew
installs:

```sh
brew install --cask android-platform-tools
```

With Pinfold from Google Play open on the phone:

```sh
adb shell am crash io.github.pavelya.pinfold
```

The game closes. Open it again: Crashlytics sends the report when the game starts. Within about
five minutes Crashlytics lists a new issue, `CrashedByAdbException` (on Android 11 and older,
`RemoteServiceException`), for that version. Close the issue in Crashlytics afterwards, so it
doesn't count against the next releases' numbers.

## Analytics

Store builds report a few game events to Google Analytics for Firebase (decision 0033), to see
where players get stuck or leave. Debug builds from GitHub send nothing; they write each event to
the phone's log instead (`adb logcat -s Analytics`).

| Event | When | With |
| --- | --- | --- |
| `first_open` | The first time the game opens after an install. Firebase sends it by itself. | |
| `level_start` | A new game of a campaign level: from the map, Play again or Retry. | `level_name` (`1-4`), `level_number` (counted from the first level: 1-1 is 1), `world` |
| `level_end` | The level's game ended. | the same, plus `success` (1 passed, 0 failed) and `stars` |
| `booster_used` | A booster was used in a level. | `booster` (`wall`, `hint`, `bomb`, `double-dot`, `undo`), `level_name`, `level_number`, `world` |
| `daily_puzzle_done` | An attempt at a daily puzzle ended. | `puzzle_number`, `success` |

Practice games (a replay of the tutorial), free play and two players send none of them. Firebase
also counts sessions and time in the game by itself. Nothing sent names a player: no advertising
ID, no Android ID, no account; only a random ID per installation, which lets a funnel follow one
installation from level to level.

**Where to look.** [analytics.google.com](https://analytics.google.com/), account `Pinfold`.
Firebase also shows a summary under Analytics > Dashboard. New events appear in reports after
about a day; DebugView (below) shows them within seconds.

**The funnel to level 10.** In Google Analytics: **Explore** > **Funnel exploration**. Set the
date range, then edit **Steps** (a funnel has at most 10):

| Step | Event | Condition |
| --- | --- | --- |
| Install | `first_open` | |
| Level 1 started | `level_start` | `level_number` = 1 |
| Level 1 passed | `level_end` | `level_number` = 1 and `success` = 1 |
| Level 2 | `level_end` | `level_number` = 2 and `success` = 1 |
| Level 3 | `level_end` | `level_number` = 3 and `success` = 1 |
| Level 4 | `level_end` | `level_number` = 4 and `success` = 1 |
| Level 5 | `level_end` | `level_number` = 5 and `success` = 1 |
| Level 6 | `level_end` | `level_number` = 6 and `success` = 1 |
| Level 8 | `level_end` | `level_number` = 8 and `success` = 1 |
| Level 10 | `level_end` | `level_number` = 10 and `success` = 1 |

Leave "Make open funnel" off, so every player starts at the install. The chart shows how many
installations reached each step and where the rest stopped. The exploration is saved under Explore
for next time. Explorations look back at most 2 months, the data kept (Part 7).

To see where players fail, a **Free form** exploration with the dimension `level_name`, the metric
**Event count** and a filter on the event `level_end` and `success` = 0 lists failures per level.

**Checking events on a phone (DebugView).** This needs a store build on the phone, from Google
Play, the phone connected by USB with USB debugging on, and `adb` (as in [A test
crash](#crash-reports)). Turn on debug mode for Pinfold:

```sh
adb shell setprop debug.firebase.analytics.app io.github.pavelya.pinfold
```

Open the game and play a level. In Firebase: **Analytics** > **DebugView** (or in Google
Analytics: **Admin** > **DebugView**). The phone appears at the top within a minute, and each
event shows as it happens; click one to see its parameters. Check that no parameter holds anything
but what the table above says. Then turn debug mode off again:

```sh
adb shell setprop debug.firebase.analytics.app .none.
```

**Nothing before the consent form's answer.** A store build collects nothing until Google's
consent form (C-03) has the player's answer, or Google has said no answer is needed where the
player is: the manifest keeps collection off, and the game turns it on then. In the EU, the EEA
and the UK the answer also decides whether Analytics may keep its ID on the phone (consent mode).
To check it, see [Ads](#ads).

**For the Data safety form (R-03),** besides what crash reports need: app interactions (under
App activity) and device or other IDs, collected, not shared, encrypted in transit, used for
analytics. The privacy policy names Google Analytics for Firebase.

## Ads

Rewarded ads come from AdMob (C-03, decision 0041), in the two places of
`docs/design/money.md`: the Result of a failed level from World 2, and a chest of two boosters.
At most 3 a day, none for 24 hours after a purchase, and none with Show ad offers off (Settings >
Privacy). Players in the EU, the EEA and the UK first answer Google's consent form, which opens
over the loading screen the first time and again from Settings > Privacy > Ad choices.

| Build | Ads | Consent form |
| --- | --- | --- |
| Debug (GitHub, POC tests) | Google's test ads, marked "Test Ad": they earn and cost nothing. | Shown where the law needs it; anywhere with the developer menu's **Ads as in the EU**. |
| Store | Real ads from the ad unit in `app/build.gradle.kts`. | Shown where the law needs it. |

The game sends no advertising ID: the app has no permission to read it, and every ad is limited
to content rated PG. The IDs of the AdMob app and the ad unit are in `app/build.gradle.kts`; they
are not secrets.

**Checking ads and the consent form on a debug build.** Install the newest `dots-debug-apk` from a
CI run.

1. Settings > tap the version line 7 times > developer menu > **Ads as in the EU** on > **Forget
   the consent answer**. Close the game (swipe it away) and open it. *Worked when:* Google's
   consent form opens over the loading screen.
2. Answer it. Settings > Privacy. *Worked when:* **Ad choices** is listed, opens the same form, and
   **Show ad offers** is on.
3. Developer menu > Campaign progress > **Halfway through World 2**. Lose a level of World 2. *Worked when:*
   Result offers "Watch an ad for one … on your retry" below Retry. Watch the test ad to its end:
   the offer turns into "One … ready for your retry", and Retry starts with one more of that booster.
4. Lose again and close the ad early. *Worked when:* the offer stays, and nothing is given.
5. Watch two more ads (3 in all today). Lose again. *Worked when:* no offer.
6. Show ad offers off, or a level of World 1: *Worked when:* no offer.

**Checking that nothing leaves the phone before the answer, on a store build.** Uninstall the
game. Turn on DebugView's debug mode ([Analytics](#analytics)), install the newest internal
release from Google Play and open it in the EU. *Worked when:* DebugView shows nothing from the
phone while the consent form is open, and events (`first_open`, then `level_start`) once it is
answered.

**For the Data safety form (R-03):** AdMob collects device or other IDs (an app set ID, no
advertising ID), app interactions and diagnostics for advertising and fraud prevention, shared
with Google, encrypted in transit. The privacy policy names Google AdMob and Google's consent
form. Play Console > App content > Ads: **Yes, my app contains ads**.

## What is set up

All of it belongs to the game's Google account (B-05), the same one in Play Console
and Google Cloud. **A task that changes anything here** (an account's rights, a project, an API, a
tester list, a secret) **updates this section in the same pull request**, so the next task starts
from what really exists.

### Play Console

[play.google.com/console](https://play.google.com/console), personal developer account (R-01).

| What | How it is set |
| --- | --- |
| App | **Pinfold**, package `io.github.pavelya.pinfold`, Game, Free. The package name can never change; Free can never become paid. |
| App signing | Play App Signing: Google holds the app signing key. |
| Upload key certificate | SHA-256 `D1:DF:DE:33:F4:45:1F:F8:0F:F6:B3:82:B5:75:89:11:71:1B:38:D4:36:3E:40:A7:61:6A:B3:91:39:B8:C0:41`. Every release must be signed with this key; the workflow prints the certificate in its "Signing certificate" step. |
| Internal testing track | Every tag lands here, released at once to the testers. |
| Internal testers | Email list `Pinfold internal`: the owner's Google account. Testers join through the link on the Testers tab. |
| Users and permissions | The owner (account owner). The service account below, with one app permission on Pinfold: **Release apps to testing tracks**. Nothing else. |
| Payments profile | The owner's, made in [Part 8](#part-8-the-shops-products) (C-02), so Pinfold can sell. |
| One-time products | Six, one per cosmetic of `docs/design/money.md`: `star_chart`, `neon_arcade`, `hearts_and_clubs`, `flame_and_frost`, `lasso`, `rubber_stamp`. Each has one purchase option, `buy`, at the base price ($2.99 for a theme, $0.99 for the others) with the prices of money.md's two lower-price groups set by hand, and is not sold in Russia or Belarus. The game finds them by these IDs (`Cosmetic.productId`). |
| License testing | The owner's Google account, so its purchases use Google's test cards and cost nothing. |

Anything else in Play Console (store listing, app content declarations, closed testing,
production, Play Games) is set up by the task that needs it, which then adds a row here.

### Google Cloud

[console.cloud.google.com](https://console.cloud.google.com/), the same Google account.

| What | How it is set |
| --- | --- |
| Project | `pinfold-release`, no organization. Its exact ID is in the project picker (Google adds digits when a name is taken). |
| Enabled APIs | Google Play Android Developer API. |
| Service account | `play-release` (email on its Details tab). No Google Cloud roles: its only rights are the Play Console permission above. One JSON key, stored in the Actions secret `PLAY_SERVICE_ACCOUNT_JSON`. |

This project exists for release uploads; Firebase has its own (below). A task that needs the
service account to do more adds only the Play Console permission it needs, and notes it in the
table above.

### Firebase

[console.firebase.google.com](https://console.firebase.google.com/), the same Google account.
Decision 0032.

| What | How it is set |
| --- | --- |
| Project | `pinfold`, ID `pinfold-1bc9f`, Spark plan (free). Gemini in Firebase off. Firebase made a Google Cloud project of the same name for it, with an API key named "Android key (auto created by Firebase)". |
| Android app | `io.github.pavelya.pinfold`, nickname Pinfold. Its settings file is `app/google-services.json` in the repository: public settings, not secrets. |
| Crashlytics | On. Store builds report crashes; the release workflow sends the mapping file for tagged builds. |
| Google Play link | Project settings > Integration > Google Play: linked to Pinfold. Switches: Crashlytics on (crashes can be filtered by Play track, internal or closed); App Distribution and Google Analytics off. |
| Google Analytics | Project settings > Integrations > Google Analytics: linked to the Google Analytics property below. Store builds send the events of [Analytics](#analytics). |

Online play (O-01) uses this project too, and adds rows here.

### Google Analytics

[analytics.google.com](https://analytics.google.com/), the same Google account. Decision 0033.

| What | How it is set |
| --- | --- |
| Account and property | Account `Pinfold`, with one property that Firebase made for the project `pinfold` when Analytics was turned on (its name is shown at the top left). One Android data stream, `io.github.pavelya.pinfold`. |
| Data retention | 2 months, the shortest. |
| Google signals | Off. |
| Granular location and device data | Off. |
| Data sharing settings | All off: no sharing with Google products, technical support, or business insights. |
| Product links | Firebase only. No Google Ads, AdMob or Play link. |
| Custom definitions | Event-scoped custom dimensions `level_name`, `level_number`, `world`, `success`, `stars`, `booster` and `puzzle_number`, so reports and explorations can use them. |

### AdMob

[admob.google.com](https://admob.google.com/), the same Google account. Decision 0041,
[Part 9](#part-9-ads).

| What | How it is set |
| --- | --- |
| App | **Pinfold**, Android, added as not on an app store yet (no store details), user metrics off. App ID `ca-app-pub-3639439691153326~3889232280`, in `app/build.gradle.kts` for every build. Once Pinfold is public on Google Play, its app settings > **Add app store details** links the listing; until AdMob has reviewed it, few real ads are served. |
| Ad unit | `Booster`, Rewarded, reward 1 `booster`, no server-side verification. ID `ca-app-pub-3639439691153326/6766356268`, used by store builds; debug builds use Google's test unit. |
| Blocking controls | Maximum ad content rating PG. |
| Privacy & messaging | A published European regulations (GDPR) message for Pinfold, in English, with the privacy policy URL `https://pavelya.github.io/dots/privacy/` (R-03 publishes the page). |
| Links | None: no Firebase or Google Analytics link (decision 0033). |

### GitHub

| What | Where |
| --- | --- |
| Workflow | `store release`, `.github/workflows/release.yml`, started by a `v*` tag on `main`. The upload itself: `.github/scripts/play-upload.sh`. |
| CI check | The `test` workflow's `release-build` job builds the shrunk release on every pull request. |
| Secrets | `UPLOAD_KEYSTORE_BASE64`, `UPLOAD_KEYSTORE_PASSWORD`, `PLAY_SERVICE_ACCOUNT_JSON`, and the POC test key's two. See [Keys and secrets](#keys-and-secrets). |

## If something goes wrong

| The run says | What to do |
| --- | --- |
| Many app tests fail with `MavenArtifactFetcher` and `FileNotFoundException` | Maven Central, where the tests download Android from, had an outage. Open the run and click **Re-run jobs** > **Re-run failed jobs**. Same tag, same version. |
| `uploadCrashlyticsMappingFileRelease` failed | Firebase didn't answer; nothing reached Google Play yet. **Re-run failed jobs**, same tag. |
| `UPLOAD_KEYSTORE_BASE64 is missing` | Step 3. |
| `Keystore was tampered with, or password was incorrect` | The two secrets don't match. Redo step 3 with the key file from a backup. |
| `Commit ... is not on main` | Delete the tag (`git push origin :v0.1.2`, `git tag -d v0.1.2`) and tag a commit of `main`. |
| `The version must look like 1.2.3` | Tags are `v` plus three numbers, for example `v0.1.2`. Delete the tag as above. |
| `version code ... has already been used` | That version was uploaded before. Tag the next patch number. |
| `The caller does not have permission` | Step 14: the service account needs Pinfold and "Release apps to testing tracks". A new invite can take up to a day to start working. |
| A warning that the release is a draft | Roll it out in Play Console: Internal testing, open the draft. |

## Keys and secrets

| What | Where | If it is lost or leaks |
| --- | --- | --- |
| App signing key | Google (Play App Signing) | Google keeps it; it never leaves Google. |
| Upload key `pinfold-upload.jks` | Actions secret `UPLOAD_KEYSTORE_BASE64`; two copies from step 4 | Make a new key (steps 1 to 3), then Play Console > App integrity > **Request upload key reset**. Google takes a few days. |
| Upload key password | Actions secret `UPLOAD_KEYSTORE_PASSWORD`; the password manager | As above. |
| Service account key | Actions secret `PLAY_SERVICE_ACCOUNT_JSON` | Delete the key in Google Cloud and make a new one (step 13.4, then step 15). |
| POC test key | Actions secrets `POC_KEYSTORE_BASE64`, `POC_KEYSTORE_PASSWORD` | Signs only test builds from GitHub, never the store app. |

## Setting it up from nothing

The whole setup, in order, from no key and no app to a release that reaches the phone by
itself. [What is set up](#what-is-set-up) shows the result. To redo one part (a new upload key, a
new service account key), follow just those steps. Each step ends with how to tell it worked.

**You need:** the Mac with the repository and the GitHub CLI signed in (`gh auth status` says
"Logged in"), the Play Console developer account, a password manager, two USB sticks (or one stick
and one other safe place) and the Android phone.

### Part 1: the upload key

The upload key proves to Google Play that a build comes from you. Google keeps the real app
signing key; the upload key only signs what you send.

**1. Make a password.** This puts a random password on the clipboard without showing it:

```sh
openssl rand -base64 24 | tr -d '\n' | pbcopy
```

Paste it (Cmd+V) into a new entry in your password manager, named "Pinfold upload key", and
save it. The clipboard still holds it for the next step.

*Worked when:* the entry is saved.

**2. Make the key.**

```sh
keytool -genkeypair -v -keystore pinfold-upload.jks -storetype PKCS12 -alias upload -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Pinfold upload"
```

It asks `Enter keystore password:`. Paste the password (Cmd+V) and press Enter. **Nothing appears
while you paste**; terminals hide passwords. It asks `Re-enter new password:`: paste and press
Enter again.

The password goes only into these hidden prompts and the password manager, never into a chat, a
document or a visible command line. If it ever shows anywhere else, delete `pinfold-upload.jks`
and start again from step 1.

*Worked when:* it ends with `[Storing pinfold-upload.jks]`, and `ls pinfold-upload.jks` prints the
name. (`.gitignore` keeps the file out of commits.)

**3. Put the key in GitHub.** The workflow reads it from two Actions secrets.

```sh
base64 -i pinfold-upload.jks | gh secret set UPLOAD_KEYSTORE_BASE64
```

That one asks nothing. The next one asks `Paste your secret:`. Paste the same password (copy it
from the password manager if the clipboard has changed) and press Enter.

```sh
gh secret set UPLOAD_KEYSTORE_PASSWORD
```

*Worked when:* `gh secret list` shows `UPLOAD_KEYSTORE_BASE64` and `UPLOAD_KEYSTORE_PASSWORD`.

**4. Back the key up, then delete it from the Mac.** GitHub never shows a secret again, so GitHub
is not a backup. Copy `pinfold-upload.jks` to two places you control: two USB sticks kept apart,
or one stick and the password manager entry, if your password manager can hold files (Bitwarden's
free plan can't). Note in the password manager entry where the copies are. Then:

```sh
rm pinfold-upload.jks
```

*Worked when:* both copies open (the file is about 4 KB), and the file is gone from the
repository folder.

**5. Check the key with a dry run.** This builds and signs the game on GitHub but sends nothing to
Google Play.

```sh
gh workflow run release.yml -f version=0.0.1
```

It prints a link to the run (`https://github.com/Pavelya/dots/actions/runs/...`). Open it: hold
Cmd and click it. The page follows the run by itself; it takes about six minutes.

*Worked when:* the run shows a green tick. Click the job **release**, then the step **Signing
certificate**: it shows `Owner: CN=Pinfold upload` and a `SHA256:` line. Copy that line's
fingerprint into the password manager entry; step 11 compares it.

### Part 2: the app in Play Console

Play Console is at [play.google.com/console](https://play.google.com/console), signed in with the
game's Google account. Its menus move around from time to time; the search box at the top finds
any page by name.

**6. Create the app.** Home > **Create app**:

- App name: `Pinfold`
- Package name: `io.github.pavelya.pinfold`, exactly, all lower case, then **Check availability**.
  It is the app's permanent ID (decision 0016) and must match the app's `applicationId`; it can
  never change, and Play refuses a bundle with any other one.
- Default language: English (United States) – en-US. (Play may suggest another English; any
  works, and it can change later.)
- App or game: **Game**
- Free or paid: **Free** (this can never change to paid; Pinfold is free, decision 0004)
- Tick both declarations, then **Create app**.

*Worked when:* Pinfold's dashboard opens.

**7. Add yourself as an internal tester.** Left menu: Test and release > Testing > **Internal
testing** > tab **Testers** > **Create email list**. Name it `Pinfold internal`, add the Google
account you use on the phone (and any other internal testers), Save. Tick the list, then **Save**.

*Worked when:* the list is ticked on the Testers tab.

### Part 3: the first release, uploaded by hand

Google Play's upload API knows an app only after its first build, so the first one goes in by
hand. The workflow builds it.

**8. Tag version 0.1.0.**

```sh
git fetch origin
```

```sh
git tag v0.1.0 origin/main
```

```sh
git push origin v0.1.0
```

A tag prints no link, so open the workflow's page instead:
[github.com/Pavelya/dots/actions/workflows/release.yml](https://github.com/Pavelya/dots/actions/workflows/release.yml).
The newest run is at the top, with the tag `v0.1.0` beside it. Click it; the page follows the run by itself.

*Worked when:* the run is green and ends with a yellow warning, "No PLAY_SERVICE_ACCOUNT_JSON
secret: nothing was uploaded". That is expected: part 5 sets it up.

**9. Download the build.**

```sh
gh run download -n pinfold-0.1.0-bundle -D ~/Downloads/pinfold-0.1.0
```

*Worked when:* `~/Downloads/pinfold-0.1.0/` holds `pinfold-0.1.0.aab`.

**10. Upload it.** Play Console > Test and release > Testing > Internal testing > **Create new
release**.

- If Play asks how to sign the app, keep **Google-generated key** (Play App Signing).
- Drag `pinfold-0.1.0.aab` into the App bundles box. Wait until it shows version 1000 (0.1.0).
- Wait until the bundle shows version code 1000 (0.1.0); until then the release name stays empty.
  Release name: `0.1.0` (the workflow names later releases the same way). Release notes:
  `First internal build.`
- **Next**. The check page shows one warning, "This App Bundle contains native code, and you've
  not uploaded debug symbols". It is expected on every release: the native code is two Google
  libraries (Compose and DataStore), shipped without symbols. Pinfold's own crash reports are
  readable through R8's mapping file.
- **Save and publish** (the button may say **Start rollout to Internal testing**).

*Worked when:* the Internal testing page shows release 0.1.0 as available to internal testers.
If Play instead lists tasks that must be finished first, note which, and stop here: those
questions belong to the release cards R-03 and R-04.

**11. Check the key Google Play saw.** Left menu: Test and release > **App integrity** > App
signing. The **Upload key certificate** SHA-256 must be the same as the fingerprint you saved in step 5.

*Worked when:* the two match.

### Part 4: install it on the phone

**12. Install from Google Play.**

1. Internal testing > tab **Testers** > **Copy link**. Send the link to the phone (email to
   yourself, for example).
2. If the phone has a Pinfold test build from GitHub, uninstall it first. It is signed with the
   test key, and Android installs an app over another only when both have the same signature.
   Its saved games are lost.
3. On the phone, signed in with the tester account, open the link, tap **Accept invite**, then
   **Download it on Google Play** and install.

The first release of a new app can take an hour or more to reach testers; until then the phone
says "Item not found". If it still does after that, check that the Play Store on the phone uses the
same Google account that accepted the invite and is on the testers list (profile picture, top
right).

*Worked when:* Pinfold opens on the phone and a game plays to the end.

### Part 5: automatic uploads

A service account is a robot Google account. The workflow signs in with it to upload builds,
so from now on a tag is enough.

**13. Create the service account.** Go to
[console.cloud.google.com](https://console.cloud.google.com/), signed in with the game's Google
account.

1. Project picker at the top > **New project**. Name: `pinfold-release`. **Create**, then select it.
2. Search box: "Google Play Android Developer API" > open it > **Enable**.
3. Left menu: IAM & Admin > **Service accounts** > **Create service account**. Name:
   `play-release`. **Create and continue**, skip the roles, **Done**.
4. Click the new account > tab **Keys** > **Add key** > **Create new key** > **JSON** >
   **Create**. A file named like `pinfold-release-1a2b3c4d5e6f.json` downloads.
5. Copy the account's email from the Details tab:
   `play-release@pinfold-release.iam.gserviceaccount.com`.

*Worked when:* the JSON file is in `~/Downloads`.

**14. Let it release Pinfold.** Play Console, the top level (not inside the app): **Users and
permissions** > **Invite new users**.

- Email: the service account's email from step 13.
- Tab **App permissions** > **Add app** > Pinfold > **Apply**. Tick **Release apps to testing
  tracks** and nothing else. **Apply**.
- **Invite user**.

*Worked when:* the service account is listed under Users and permissions.

**15. Put its key in GitHub, then delete the file.** A new key can be made at any time, so this
file needs no backup. Check the file's name first:

```sh
ls ~/Downloads/pinfold-release-*.json
```

Then, with that name:

```sh
gh secret set PLAY_SERVICE_ACCOUNT_JSON < ~/Downloads/pinfold-release-1a2b3c4d5e6f.json
```

```sh
rm ~/Downloads/pinfold-release-1a2b3c4d5e6f.json
```

*Worked when:* `gh secret list` shows `PLAY_SERVICE_ACCOUNT_JSON`.

**16. Release 0.1.1 the normal way.** Follow [Make a release](#make-a-release) with `v0.1.1`.

*Worked when:* the run is green with no warning, and Play Console's Internal testing page shows
0.1.1 without any step there. If the run's warning says the release is a **draft**, open Internal
testing, open the draft and roll it out; Google Play does this until the app is published, and the
workflow handles it each time.

### Part 6: crash reports

Firebase is Google's set of services for apps. Pinfold uses one of them, **Crashlytics**: when
the game crashes on a player's phone, it sends a report to Firebase, and you see it in the
Firebase console within minutes (decision 0032). This part creates the Firebase project and
registers the game in it. It takes about 15 minutes and costs nothing.

**You need:** the Mac's browser and the game's Google account (the one that owns Play Console).

**You don't need:** a credit card, a paid plan, a password or a GitHub secret. If Firebase offers
to upgrade to the **Blaze** plan, say no: the free **Spark** plan covers Crashlytics.

Firebase's pages change their look from time to time. If a button has a slightly different name,
pick the one that means the same thing. If a page asks something these steps don't mention, stop
and note what it says.

**17. Sign in to Firebase.**

1. Open [console.firebase.google.com](https://console.firebase.google.com/).
2. Look at the round picture in the top right corner. It must be the game's Google account. If it
   isn't, click it and choose (or add) the game's account.
3. If Firebase shows a welcome page or asks you to accept its terms, accept them.

*Worked when:* you see a page with "Welcome to Firebase" or a list of projects (empty is fine).

**18. Create the project.**

1. Click **Create a new Firebase project** (it may say **Create a project**, **Add project** or
   **Get started with a Firebase project**).
2. **Project name:** type `pinfold`. Below the name, Firebase shows a project ID such as
   `pinfold-1a2b3`; leave it as it is.
3. Tick the box that accepts the Firebase terms. If there is a second box about using Firebase for
   your trade or business, tick it too: Pinfold is a business. Click **Continue**.
4. A page about **AI assistance** (Gemini in Firebase) may appear. Switch it **off** and click
   **Continue**.
5. A page about **Google Analytics** appears. Switch **Enable Google Analytics for this project**
   **off**. The button below it changes to **Create project**. Click it.
6. Wait about half a minute. When it says the project is ready, click **Continue**.

*Worked when:* the page shows `pinfold` in the top left corner, with **Spark plan** (or
"no-cost") next to it.

**19. Register the game and download its settings file.**

1. On the project's start page, find the row of round icons under "Get started by adding Firebase
   to your app". Click the **Android** icon (the green robot). If you don't see the icons: gear
   icon next to **Project Overview** (top of the left menu) > **Project settings** > tab
   **General** > section **Your apps** > **Add app** > Android.
2. **Android package name:** type `io.github.pavelya.pinfold`, exactly: all lower case, with the
   dots. This must match the game, or nothing works.
3. **App nickname:** `Pinfold`.
4. **Debug signing certificate SHA-1:** leave it empty.
5. Click **Register app**.
6. Click **Download google-services.json**. The browser saves the file in Downloads. Don't open
   or change it.
7. The page goes on with "Add Firebase SDK" and other steps. Those are code changes, already in
   the repository: click **Next**, **Next**, then **Continue to console**.

*Worked when:* this command, in the terminal, prints the file's name:

```sh
ls ~/Downloads/google-services*.json
```

If it prints a name like `google-services (1).json`, that is fine; tell Claude the exact name.

The file holds the project's public settings, not secrets, and goes into the public repository
as `app/google-services.json`. The game can't be built without it.

**20. Link Firebase to Google Play.** This lets Crashlytics tell crashes from internal testers
apart from crashes in the closed test.

1. Click the gear icon next to **Project Overview** > **Project settings**.
2. Click the tab **Integration** (or **Integrations**).
3. On the **Google Play** card, click **Link**. A page "Google Play" opens, with the steps "Link
   Firebase to Google Play" and "Configure integration".
4. Under **Configure integration**, set the switches:
   - **Crashlytics: Share app and crash data**: **on**. This is the one we need.
   - **App Distribution**: off. It is another way to hand out test builds; Pinfold uses Google
     Play's testing tracks.
   - **Google Analytics: share in-app purchase and subscription revenue data**: off. The project
     has no Analytics.
5. Scroll down. If Firebase lists the apps of your Play Console account, choose **Pinfold**
   (`io.github.pavelya.pinfold`). Click the button at the bottom (**Link** or **Finish**). If
   Firebase asks to allow access to Play Console, allow it.

*Worked when:* the Google Play card says **Linked** (or shows **Manage** instead of **Link**).

If the card says you lack permission, check the picture in the top right again: only the account
that owns Play Console can link.

**21. Open Crashlytics once.**

1. In the left menu, open **DevOps and engagement** and click **Crashlytics**. Or type
   `Crashlytics` in **Search for products** at the top of the menu.
2. If a list of apps appears at the top, choose **Pinfold** (Android).
3. If there is a button **Enable Crashlytics**, click it. If the page instead shows a big dark
   card with an **Add SDK** button, don't click it: adding the SDK is the code change, already in
   the repository. There is nothing to click.

*Worked when:* Crashlytics shows that card (or a page that waits for data). It stays like that until a store
build with Crashlytics reaches a phone.

**22. Tell Claude.** Say "Firebase done", with the file name from step 19, and anything that
looked different from these steps. Claude puts the file in the repository, and CI builds the game
with Crashlytics.

**23. Later, after the next release: a test crash.** Once a release with Crashlytics is on your
phone from Google Play, crash it on purpose, as [A test crash](#crash-reports) explains. You need
the phone, a USB cable and `adb` on the Mac (one Homebrew command, in that section).

*Worked when:* Crashlytics lists the crash for that version.

### Part 7: analytics

This part turns on Google Analytics in the Firebase project, so store builds can report the
game's events (decision 0033), and switches off everything about ads and people. It takes about
20 minutes and costs nothing. Like Part 6, it needs only the Mac's browser and the game's Google
account; no credit card, no password, no GitHub secret.

Google Analytics is a separate site from Firebase, at analytics.google.com, with the same Google
account. Firebase creates an Analytics **account** (the top level) and in it a **property** (the
game's reports).

**24. Turn on Google Analytics in Firebase.**

1. Open [console.firebase.google.com](https://console.firebase.google.com/) and the project
   `pinfold`. Check the round picture in the top right: the game's Google account.
2. Click the gear icon next to **Project Overview** > **Project settings** > tab **Integrations**.
3. On the **Google Analytics** card, click **Enable** (it may say **Link**).
4. Firebase asks for an Analytics account. Choose **Create a new account** and name it `Pinfold`.
   (If it offers an account that already exists, don't pick it: Pinfold gets its own.)
5. **Analytics location:** the country you live in. **Data sharing settings and Google Analytics
   terms:** if there are boxes about sharing data with Google, **untick** them; tick only the box
   that accepts the terms. Click **Enable Google Analytics** (or **Save**).
6. Wait until the card says it is linked.

*Worked when:* the Google Analytics card on the Integrations tab shows **Manage** (or a property
named `Pinfold` or `pinfold`) instead of **Enable**.

Firebase may offer a new `google-services.json`. The game doesn't need it; you can skip it.

**25. Leave the Google Play link's Analytics switch off.** Same tab, the **Google Play** card >
**Manage**. The switch **Google Analytics: share in-app purchase and subscription revenue data**
stays **off**. Crashlytics stays on.

*Worked when:* the switches are as [What is set up](#firebase) says.

**26. Open Google Analytics.** Go to [analytics.google.com](https://analytics.google.com/) with
the same account. If it asks to accept terms, accept them (untick any data sharing boxes). At the
top left it shows the account and property, `Pinfold`. If it shows another, click the name and
choose `Pinfold`.

Every setting below is under **Admin**: the gear icon at the bottom left. The Admin page has two
columns or sections: **Account settings** and **Property settings**.

*Worked when:* the top left shows `Pinfold`.

**27. Keep data for 2 months.** Admin > Property settings > **Data collection and modification**
> **Data retention**. **Event data retention:** **2 months**. **Reset user data on new activity:**
off. **Save**.

*Worked when:* it shows 2 months after the page reloads.

**28. Google signals and granular data off.** Admin > Property settings > **Data collection and
modification** > **Data collection**.

- **Google signals data collection:** off (if it shows **Get started** or **Turn on**, it is off
  already; don't click it).
- **Granular location and device data collection:** off. If it asks which regions, choose **all
  regions**.
- **Ads personalization:** if it shows "Allow ads personalization" with a list of countries, turn
  it off for all.

*Worked when:* the page shows Google signals and granular location and device data as off.

**29. Data sharing off.** Admin > Account settings > **Account details** (it may say **Account
settings**). Scroll to **Data sharing settings**. Untick every box (Google products and services,
Modeling contributions and business insights, Technical support, Recommendations for your
business, Google sales experts). **Save**.

*Worked when:* every box is unticked after the page reloads.

**30. Check the links.** Admin > Property settings > **Product links**. Only **Firebase** should
show a link. If **Google Ads**, **AdMob** or **Google Play** shows one, open it and remove it.

*Worked when:* only Firebase is linked.

**31. Name the event parameters.** Google Analytics shows a parameter in reports and explorations
only once it is named here. Admin > Property settings > **Data display** > **Custom
definitions** > tab **Custom dimensions** > **Create custom dimension**. Make seven, one at a time,
each with **Scope: Event**:

| Dimension name | Event parameter |
| --- | --- |
| Level | `level_name` |
| Level number | `level_number` |
| World | `world` |
| Success | `success` |
| Stars | `stars` |
| Booster | `booster` |
| Daily puzzle number | `puzzle_number` |

For each: type the dimension name, choose **Event** as scope, type the parameter exactly as in the
right column (Google may also offer it in a list once events have arrived), **Save**.

*Worked when:* the Custom dimensions tab lists all seven.

**32. Tell Claude.** Say "Analytics done", with anything that looked different from these steps
or any setting you couldn't find.

**33. Later, after the next release: check the events.** Once a release with analytics is on your
phone from Google Play, follow [Checking events on a phone](#analytics): turn on debug mode, play a
level, and watch the events arrive in DebugView.

*Worked when:* DebugView shows `level_start` and `level_end` with the parameters of the table in
[Analytics](#analytics), and nothing else of yours.


### Part 8: the Shop's products

The Shop (C-02) sells six cosmetics through Google Play Billing. Google Play needs to know each
one: its ID, its name, its price in every country. This part makes them, and then checks a test
purchase, a refund and a restore on the phone. It takes about an hour, most of it typing prices.
It needs the Mac's browser, the game's Google account and the phone.

**Before you start.** A release with the Shop must already be on the internal testing track: tag
a release after the Shop's pull request is merged ([Make a release](#make-a-release)). Play
Console lets you make products only once it has seen a build that can sell.

**34. Make a payments profile.** It tells Google who gets the money.

1. Open [play.google.com/console](https://play.google.com/console). In the left menu of the list
   of apps (not inside Pinfold), click **Settings** > **Payments profile**.
2. If it shows a profile already, skip to step 35. Otherwise click **Create payments profile** and
   fill in your legal name and address. Google asks for a bank account for payouts later, when
   there is money to pay out.

*Worked when:* Pinfold > **Monetize with Play** > **Products** > **One-time products** shows a
**Create one-time product** button instead of a request for a payments profile.

**35. Make your account a license tester.** Purchases by a license tester use Google's test cards
and cost nothing.

1. Same left menu of the list of apps: **Settings** > **License testing**.
2. Tick the email list **Pinfold internal** (or add the game's Google account by hand).
3. **License response:** `RESPOND_NORMALLY`. **Save changes**.

*Worked when:* the list is ticked after the page reloads.

**36. Make the six products.** Pinfold > **Monetize with Play** > **Products** > **One-time
products** > **Create one-time product**. Make one for each row, one after the other. The product
ID can never change or be used again, so copy it exactly.

| Product ID | Name | Description | Base price |
| --- | --- | --- | --- |
| `star_chart` | Star chart | A theme: gold stars and ice-blue comets on a chart of the sky. | 2.99 USD |
| `neon_arcade` | Neon arcade | A theme: magenta and cyan pieces, with rings of neon light. | 2.99 USD |
| `hearts_and_clubs` | Hearts and clubs | Pieces: hearts for one side, clubs for the other. | 0.99 USD |
| `flame_and_frost` | Flame and frost | Pieces: flames for one side, snowflakes for the other. | 0.99 USD |
| `lasso` | Lasso | Rings: a rope thrown round the area and pulled tight. | 0.99 USD |
| `rubber_stamp` | Rubber stamp | Rings: a double line, and the area stamped down at once. | 0.99 USD |

For each product:

1. **Product ID**, **Name** and **Description** from the table. Leave any tax or category
   settings as Play Console suggests (a digital game item).
2. Under **Purchase options**, add one: ID `buy`, type **Buy** (a one-time purchase, not a rent).
   If it offers **Backwards compatible**, tick it.
3. **Set prices** (it may say **Price** or **Set price**): the default price in US dollars from the
   table. Play Console fills in every country from it.
4. Change the countries of money.md's two lower-price groups by hand: open
   `docs/design/money.md`, section "Prices by country", and type each country's price from the
   "set" columns (the theme columns for the two themes, the others for the four others). That is
   18 countries per product. If Play Console shows a currency other than the table's for a
   country, or a price of its own far from the table's "Google Play's price", type the group's
   share of what Play Console shows (70% for central Europe, 40% for the lower prices) and write
   down the country; Claude fixes the table.
5. **Availability** (or **Countries / regions**): Russia and Belarus not available. They may be
   unavailable already; leave them so.
6. **Save**, then **Activate**.

*Worked when:* the One-time products page lists six products, each **Active**.

**37. Buy, use and restore on the phone.** Install the newest internal release from Google Play
(the testers' link on the Testers tab) and open the game with the game's Google account.

1. Open the Shop. *Worked when:* Star chart shows a price in your currency and "Or earn".
2. Open Star chart, **Buy for …**. Google Play's purchase screen says it is a test; choose **Test
   card, always approves**. *Worked when:* the preview says **Owned** and offers **Use it**. Tap it:
   the board is drawn in Star chart.
3. Open Neon arcade, **Buy for …**, choose **Test card, always declines**. *Worked when:* the
   preview says the purchase didn't go through and offers **Retry**.
4. Buy Lasso with **Slow test card, approves after a few minutes**. *Worked when:* the preview
   says **Payment pending**, and a few minutes later (reopen the Shop) Lasso is **Owned**.
5. Uninstall the game and install it again from Google Play. Open the Shop. *Worked when:* Star
   chart and Lasso are still **Owned** (the saved progress came back with Android's backup; the
   purchases came back from Google Play).

**38. Refund one.** Play Console > **Order management**. Find the test order for Lasso, open it,
**Refund**, and tick **Remove entitlement** (it may say "revoke access"). Then close the game on
the phone and open it again, and open the Shop.

*Worked when:* Lasso shows its price again, and if Lasso was in use, the rings are Standard again.
(With 70 stars or more from the campaign, Lasso stays: it is earned as well.)

**39. Tell Claude.** Say "Shop products done", with anything that looked different from these
steps, any country whose price you had to work out yourself (step 36.4), and what each check in
steps 37 and 38 showed.

### Part 9: ads

The rewarded ads (C-03) need an AdMob account with the game's app, one ad unit, and Google's
consent form for the EU and the UK. This part makes them. It takes about 30 minutes and costs
nothing. It needs the Mac's browser and the game's Google account; no credit card, no password,
no GitHub secret. AdMob's pages change their look from time to time: if a button has a slightly
different name, pick the one that means the same thing. If a page asks something these steps
don't mention, stop and tell Claude what it asks.

**40. Make the AdMob account.**

1. Open [admob.google.com](https://admob.google.com/) and sign in with the game's Google account.
   Check the round picture in the top right.
2. Click **Get started** (or **Sign up**). **Country:** where you live. **Time zone:** yours.
   **Billing currency:** your currency. Accept the terms. If it asks about emails with tips,
   answer as you like.
3. AdMob may ask for payment details now; you can skip them until there is money to pay out.

*Worked when:* AdMob shows its home page with **Apps** in the left menu.

**41. Add the game.**

1. **Apps** > **Add app** (it may say **Add your first app**).
2. **Platform:** Android. **Is the app listed on a supported app store?** **No**: Pinfold is on
   internal testing only. AdMob links the store listing later, once the game is public (its app
   page then offers **Add app store details**).
3. **App name:** `Pinfold`. **User metrics:** off (Pinfold has its own analytics, decision 0033).
   **Add app**.
4. The next page shows the **App ID**, which looks like `ca-app-pub-1234567890123456~1234567890`.
   Copy it into a note.

*Worked when:* **Apps** lists Pinfold, and you have its App ID.

**42. Make the ad unit.**

1. Pinfold > **Ad units** > **Add ad unit** > **Rewarded** > **Select**.
2. **Ad unit name:** `Booster`. **Reward settings:** amount `1`, item `booster`. Leave
   server-side verification off, and leave every advanced setting as AdMob suggests.
3. **Create ad unit**. The page shows the **Ad unit ID**, which looks like
   `ca-app-pub-1234567890123456/1234567890` (a slash, not a tilde). Copy it into the note.
4. **Done**. Don't follow AdMob's steps to add code: Claude does that.

*Worked when:* Pinfold's **Ad units** lists `Booster`, Rewarded.

**43. Only ads for everyone up to PG.** **Blocking controls** in the left menu > **All apps** (or
**Content**) > **Maximum ad content rating:** **PG**. **Save**. The game asks for the same with
every ad.

*Worked when:* the page shows PG after it reloads.

**44. The consent form for the EU, the EEA and the UK.**

1. **Privacy & messaging** in the left menu > **European regulations** (it may say **GDPR**) >
   **Create message**.
2. **Apps:** Pinfold. **Languages:** English. **Consent options:** **Consent** and **Manage
   options** (the default). Turn on **Close (do not consent)** if it is offered, so a player can
   say no as easily as yes.
3. **Privacy policy URL:** `https://pavelya.github.io/dots/privacy/`. The page goes up with R-03;
   tell Claude if AdMob refuses the address.
4. **Ad partners:** leave Google's commonly used ad technology providers as suggested.
5. **Publish**.
6. On the European regulations page, open **Settings** (a gear or a tab). If there is a switch
   about **consent mode** or **Google Analytics for Firebase**, turn it **on**: it lets the
   player's answer reach Analytics. Save.

*Worked when:* the European regulations page lists a published message for Pinfold.

**45. Don't link AdMob to Firebase.** If AdMob offers to link the app to Firebase or Google
Analytics, skip it (decision 0033: no AdMob link).

**46. Tell Claude.** Say "AdMob done", with the App ID and the Ad unit ID from your note (they
are not secrets: every copy of the game holds them), and anything that looked different from these
steps. Claude puts them in `app/build.gradle.kts` and adds AdMob to [What is set up](#what-is-set-up).

**47. Later, on the phone: the checks.** Once Claude's change is merged, follow [Ads](#ads): the
checks on a debug build, and after the next release the check that nothing leaves the phone
before the answer.

The setup is finished; from now on, [Make a release](#make-a-release).
