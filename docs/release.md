# Releasing Pinfold on Google Play

A store release is a tag on `main`. Pushing a tag `v0.1.0` starts the workflow `store release`
(`.github/workflows/release.yml`). It runs the tests, builds the release bundle with code shrinking,
signs it with the upload key and puts it on Google Play's **internal testing** track. Moving a
release on to the closed test or to production is done by hand in Play Console. The rules for tags,
version numbers and keys are in decision 0031.

## Make a release

1. Pick the next version. It must be higher than every version uploaded before, and each number is
   used once: a broken build is fixed with the next patch number (`v0.1.1`), never by moving a tag.
2. Tag the newest commit of `main` and push the tag:

   ```sh
   git fetch origin
   git tag v0.1.0 origin/main
   git push origin v0.1.0
   gh run watch "$(gh run list --workflow release.yml --limit 1 --json databaseId -q '.[0].databaseId')"
   ```

3. When the run is green, the release is on the internal track. Internal testers get it from Google
   Play within a few minutes; on a phone that has the app, Google Play shows an update.

The run keeps the signed bundle and R8's mapping file as an artifact, `pinfold-<version>-bundle`.
Google Play gets the mapping file inside the bundle, so its crash reports show the real code names.

**Dry run.** Actions > "store release" > Run workflow, on any branch, with any version. It builds
and signs the bundle and uploads nothing to Google Play. Use it to check the upload key.

**If Play keeps the release as a draft.** While the app has never been published, Google Play
refuses releases sent through its API and takes only drafts. The workflow then uploads a draft and
says so in a warning. Roll the draft out in Play Console: Test and release > Testing > Internal
testing.

## One-time setup

The owner does this once. Steps 1 and 3 need a Mac with a JDK and the GitHub CLI signed in.

### 1. The upload key

```sh
keytool -genkeypair -v -keystore pinfold-upload.jks -storetype PKCS12 -alias upload \
  -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Pinfold upload"
```

It asks for a password (at least 6 characters; the same one protects the file and the key). The
name in `-dname` is visible to anyone who unpacks the app, so it holds no personal details.

Then put the key in the Actions secrets. The second command asks for the password.

```sh
base64 -i pinfold-upload.jks | gh secret set UPLOAD_KEYSTORE_BASE64
gh secret set UPLOAD_KEYSTORE_PASSWORD
```

Keep two copies outside GitHub: the file and its password in the password manager (Bitwarden, as an
attachment to one entry), and the file on an offline USB stick. Then delete it from the Mac. Never
put it in the repository; `.gitignore` blocks `*.jks` files.

### 2. The app in Play Console

1. Play Console > Create app: name **Pinfold**, default language English, **Game**, **Free**.
2. Test and release > Testing > Internal testing > Testers: make an email list with the owner's
   Google account and any internal testers.

### 3. The first release, by hand

The Play Developer API knows an app only after its first bundle, so the first one is uploaded by
hand.

1. Push the first tag (`v0.1.0`) as in [Make a release](#make-a-release). Without the secret
   `PLAY_SERVICE_ACCOUNT_JSON`, the run builds and signs the bundle and ends with a warning that
   nothing was uploaded.
2. Download the bundle:

   ```sh
   gh run download <run-id> -n pinfold-0.1.0-bundle -D ~/Downloads/pinfold-0.1.0
   ```

3. Play Console > Test and release > Testing > Internal testing > Create new release. When Play
   asks about app signing, keep **Google-generated key** (Play App Signing). Upload
   `pinfold-0.1.0.aab`, keep the release name `0.1.0`, then Next > Save and publish.
4. Check the key: Test and release > App integrity > Play app signing. The **upload key
   certificate** SHA-256 must match the one printed in the run's "Signing certificate" step.

### 4. The service account, for automatic uploads

1. [Google Cloud console](https://console.cloud.google.com/), signed in with the game's Google
   account: create a project `pinfold-release`.
2. APIs & Services > Library: enable the **Google Play Android Developer API**.
3. IAM & Admin > Service accounts > Create service account: name `play-release`, no roles. Open
   it > Keys > Add key > Create new key > JSON. A key file downloads.
4. Play Console > Users and permissions > Invite new users: the service account's email
   (`play-release@pinfold-release.iam.gserviceaccount.com`). Under App permissions add **Pinfold**
   with **Release apps to testing tracks** and nothing else. Invite.
5. Put the key in the Actions secrets and delete the file. A new key can be made at any time.

   ```sh
   gh secret set PLAY_SERVICE_ACCOUNT_JSON < ~/Downloads/pinfold-release-*.json
   rm ~/Downloads/pinfold-release-*.json
   ```

From the next tag on, releases reach the internal track without a step in Play Console.

## Install from the internal track

1. Play Console > Test and release > Testing > Internal testing > Testers > **Copy link**.
2. On the phone, signed in with an account on the testers list, open the link and accept.
3. Install Pinfold from Google Play. A phone with a test build from GitHub (signed with the POC test
   key) must uninstall it first: Android installs an app over another only when both have the same
   signature.

## Keys and secrets

| What | Where | If it is lost or leaks |
| --- | --- | --- |
| App signing key | Google (Play App Signing) | Google keeps it; it never leaves Google. |
| Upload key `pinfold-upload.jks` and its password | Actions secrets `UPLOAD_KEYSTORE_BASE64`, `UPLOAD_KEYSTORE_PASSWORD`; password manager; offline USB stick | Make a new key (step 1), then Play Console > App integrity > Request upload key reset. |
| Service account key | Actions secret `PLAY_SERVICE_ACCOUNT_JSON` | Delete the key in Google Cloud and make a new one (step 4.3 and 4.5). |
| POC test key | Actions secrets `POC_KEYSTORE_BASE64`, `POC_KEYSTORE_PASSWORD` | Signs only test builds from GitHub, never the store app. |
