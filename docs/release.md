# Releasing Pinfold on Google Play

A store release starts with a tag such as `v0.1.0` on `main`. The tag starts the workflow
`store release` (`.github/workflows/release.yml`), which tests the game, builds it, signs it with
the upload key and puts it on Google Play's **internal testing** track. Moving a release on to the
closed test or to production is done by hand in Play Console. The rules for tags, version numbers
and keys are in decision 0031.

The first release takes some one-time setup, so it has its own walkthrough below. Every later
release is three commands: [Every release after the first](#every-release-after-the-first).

**Reading the commands.** Open this page in a Markdown preview (in VS Code: Cmd+Shift+V) or on
GitHub, so each command shows in its own box. Copy a whole box, from its first character to its
last, and paste it in the terminal. Run every command from the repository folder (`cd ~/dots`, or
wherever the repository is).

## The first release, step by step

Do the steps in order. Each one ends with how to tell it worked.

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

Wait ten seconds, then follow the run. `gh` asks which run to watch; pick "store release" with the
arrow keys and press Enter. It takes about six minutes.

```sh
gh run watch
```

*Worked when:* it ends with a green tick. Then open the run in the browser (`gh run view --web`,
pick the same run), open the job and the step **Signing certificate**: it shows
`Owner: CN=Pinfold upload` and a `SHA256:` line. Keep that page open; step 11 compares it.

### Part 2: the app in Play Console

Play Console is at [play.google.com/console](https://play.google.com/console), signed in with the
game's Google account. Its menus move around from time to time; the search box at the top finds
any page by name.

**6. Create the app.** Home > **Create app**:

- App name: `Pinfold`
- Default language: English (United States)
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

Wait ten seconds, then `gh run watch` and pick the "store release" run, as in step 5.

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
- Keep the suggested release name. Release notes: `First internal build.`
- **Next**, then **Save and publish** (the button may say **Start rollout to Internal testing**).

*Worked when:* the Internal testing page shows release 0.1.0 as available to internal testers.
If Play instead lists tasks that must be finished first, note which, and stop here: those
questions belong to the release cards R-03 and R-04.

**11. Check the key Google Play saw.** Left menu: Test and release > **App integrity** > App
signing. The **Upload key certificate** SHA-256 must be the same as the `SHA256:` line from step 5.

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

A new release can take up to an hour to show up for testers.

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

**16. Release 0.1.1 the normal way.** Follow [Every release after the
first](#every-release-after-the-first) with `v0.1.1`.

*Worked when:* the run is green with no warning, and Play Console's Internal testing page shows
0.1.1 without any step there. If the run's warning says the release is a **draft**, open Internal
testing, open the draft and roll it out; Google Play does this until the app is published, and the
workflow handles it each time.

The setup is finished.

## Every release after the first

1. Pick the next version, higher than every one before. A number is used once: a broken build is
   fixed with the next patch number (`v0.1.2`), never by moving a tag.
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

3. Wait ten seconds, then `gh run watch` and pick the "store release" run. When it is green,
   testers get the update from Google Play.

The run keeps the signed build and R8's mapping file as an artifact, `pinfold-<version>-bundle`.
Google Play also gets the mapping file inside the build, so its crash reports show the real code
names.

**Dry run.** `gh workflow run release.yml -f version=0.0.1`, or Actions > "store release" > Run
workflow on any branch. It builds and signs the game and uploads nothing.

## If something goes wrong

| The run says | What to do |
| --- | --- |
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
