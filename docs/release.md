# Releasing Pinfold on Google Play

A store release starts with a tag such as `v0.1.2` on `main`. The tag starts the workflow
`store release` (`.github/workflows/release.yml`), which tests the game, builds it, signs it with
the upload key and puts it on Google Play's **internal testing** track. Moving a release on to the
closed test or to production is done by hand in Play Console. The rules for tags, version numbers
and keys are in decision 0031.

This page has:

- [Make a release](#make-a-release): the three commands, when to release, which number to use.
- [Crash reports](#crash-reports): where crashes show up, what a report holds, a test crash.
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

Anything else in Play Console (store listing, app content declarations, closed testing,
production, the payments profile, in-app products, Play Games) is set up by the task that needs it,
which then adds a row here.

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
| Project | `pinfold`, Spark plan (free). Google Analytics off. Firebase made a Google Cloud project of the same name for it, with an API key named "Android key (auto created by Firebase)". |
| Android app | `io.github.pavelya.pinfold`, nickname Pinfold. Its settings file is `app/google-services.json` in the repository: public settings, not secrets. |
| Crashlytics | On. Store builds report crashes; the release workflow sends the mapping file for tagged builds. |
| Google Play link | Project settings > Integrations > Google Play: linked to Pinfold, so Crashlytics can filter crashes by Play track (internal, closed). |

Analytics (Q-06) and online play (O-01) use this project too, and add rows here.

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

1. In the left menu, find **Crashlytics**. It is inside a group (named something like **Run** or
   **DevOps & Engagement**); click the group to open it. Or type `Crashlytics` in the search box at
   the top of the page.
2. If a list of apps appears at the top, choose **Pinfold** (Android).
3. If there is a button **Enable Crashlytics**, click it. If the page instead says it is waiting
   for the app or for the first crash, there is nothing to click.

*Worked when:* Crashlytics shows a page that waits for data. It stays like that until a store
build with Crashlytics reaches a phone.

**22. Tell Claude.** Say "Firebase done", with the file name from step 19, and anything that
looked different from these steps. Claude puts the file in the repository, and CI builds the game
with Crashlytics.

**23. Later, after the next release: a test crash.** Once a release with Crashlytics is on your
phone from Google Play, crash it on purpose, as [A test crash](#crash-reports) explains. You need
the phone, a USB cable and `adb` on the Mac (one Homebrew command, in that section).

*Worked when:* Crashlytics lists the crash for that version.

The setup is finished; from now on, [Make a release](#make-a-release).
