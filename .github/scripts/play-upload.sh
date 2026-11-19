#!/usr/bin/env bash
# Uploads an Android App Bundle to a Google Play track and releases it there.
#
#   play-upload.sh <application id> <bundle.aab> <track> <release name>
#
# PLAY_SERVICE_ACCOUNT_JSON holds the key file of a Google Cloud service account that has release
# rights for the app in Play Console (docs/release.md). The script signs in with it, then makes
# one Play "edit": upload the bundle, put its version on the track, commit.
#
# Google Play refuses a released ("completed") release while the app has never been published; it
# says "draft app". Then the script makes the edit again with the release as a draft, and the owner
# rolls it out in Play Console.
#
# Only curl, jq and openssl, so no third-party code ever sees the key. PLAY_API_ROOT and
# PLAY_TOKEN_URL point it at a fake server for a local test; unset, it talks to Google.
set -euo pipefail

if [ $# -ne 4 ]; then
  echo "usage: $0 <application id> <bundle.aab> <track> <release name>" >&2
  exit 2
fi
package=$1 bundle=$2 track=$3 name=$4
: "${PLAY_SERVICE_ACCOUNT_JSON:?is not set}"
api_root=${PLAY_API_ROOT:-https://androidpublisher.googleapis.com}
token_url=${PLAY_TOKEN_URL:-https://oauth2.googleapis.com/token}

b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

# Sign in: a JSON Web Token signed with the service account's private key, traded for an access
# token (Google's "service account" OAuth flow). The key goes to openssl through a pipe, never a file.
email=$(jq -r .client_email <<<"$PLAY_SERVICE_ACCOUNT_JSON")
now=$(date +%s)
header=$(printf '{"alg":"RS256","typ":"JWT"}' | b64url)
claims=$(jq -nc --arg iss "$email" --arg aud "$token_url" --argjson iat "$now" \
  '{iss: $iss, scope: "https://www.googleapis.com/auth/androidpublisher", aud: $aud, iat: $iat, exp: ($iat + 3600)}' | b64url)
signature=$(printf '%s.%s' "$header" "$claims" \
  | openssl dgst -sha256 -binary -sign <(jq -r .private_key <<<"$PLAY_SERVICE_ACCOUNT_JSON") | b64url)
token=$(curl -sS --fail-with-body "$token_url" \
  --data-urlencode "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer" \
  --data-urlencode "assertion=$header.$claims.$signature" | jq -r .access_token)
echo "::add-mask::$token"

# One call to the Play Developer API. Prints the response body; on an HTTP error, returns 1.
api() {
  local method=$1 url=$2 body status
  shift 2
  body=$(mktemp)
  status=$(curl -sS -o "$body" -w '%{http_code}' -X "$method" -H "Authorization: Bearer $token" "$@" "$url")
  cat "$body"
  rm -f "$body"
  [ "$status" -lt 300 ]
}

# Prints the failed call's response and stops.
fail() {
  echo "::error::$1"
  echo "$2" >&2
  exit 1
}

app="$api_root/androidpublisher/v3/applications/$package"
json=(-H "Content-Type: application/json")

# One Play edit: upload the bundle, put it on the track with the given release status, commit.
# Edits are all or nothing: if a step fails, nothing of it reaches Play.
release() {
  local status=$1 edit
  response=$(api POST "$app/edits" "${json[@]}" --data '{}') || return 1
  edit=$(jq -r .id <<<"$response")
  echo "Uploading $bundle"
  response=$(api POST "$api_root/upload/androidpublisher/v3/applications/$package/edits/$edit/bundles?uploadType=media" \
    -H "Content-Type: application/octet-stream" --data-binary "@$bundle") || return 1
  version_code=$(jq -r .versionCode <<<"$response")
  echo "Uploaded version code $version_code"
  response=$(api PUT "$app/edits/$edit/tracks/$track" "${json[@]}" --data "$(
    jq -nc --arg track "$track" --arg name "$name" --arg code "$version_code" --arg status "$status" \
      '{track: $track, releases: [{name: $name, versionCodes: [$code], status: $status}]}'
  )") || return 1
  response=$(api POST "$app/edits/$edit:commit" "${json[@]}" --data '{}') || return 1
}

if release completed; then
  echo "Release $name (version code $version_code) is out on the $track track."
elif grep -q "draft app" <<<"$response"; then
  echo "Play keeps releases of a never-published app as drafts; uploading again as a draft."
  release draft || fail "Play refused the draft release." "$response"
  echo "::warning::Release $name (version code $version_code) is a draft on the $track track, because the app has never been published. Roll it out in Play Console: Test and release > Testing > Internal testing."
else
  fail "Play refused the release." "$response"
fi
