#!/usr/bin/env bash
# Seed demo moments into social-service (moments API).
#
# Usage:
#   export TRAVELO_MOMENTS_URL=http://localhost:8096   # optional; default below
#   export TRAVELO_USER_ID=<uuid>                      # optional dev header when JWT not used
#   export TRAVELO_USER_NAME="Travel Explorer"
#   ./scripts/seed-moments.sh
#
# With JWT (recommended):
#   export TRAVELO_TOKEN=<access_token>
#   ./scripts/seed-moments.sh

set -euo pipefail

BASE="${TRAVELO_MOMENTS_URL:-http://localhost:8096}"
HDR_USER=()
if [[ -n "${TRAVELO_USER_ID:-}" ]]; then
  HDR_USER=(-H "X-User-Id: ${TRAVELO_USER_ID}")
fi
HDR_NAME=()
if [[ -n "${TRAVELO_USER_NAME:-}" ]]; then
  HDR_NAME=(-H "X-User-Name: ${TRAVELO_USER_NAME}")
fi
AUTH=()
if [[ -n "${TRAVELO_TOKEN:-}" ]]; then
  AUTH=(-H "Authorization: Bearer ${TRAVELO_TOKEN}")
fi

post_moment() {
  local type="$1" caption="$2" location="$3" media_url="$4"
  curl -sS -X POST "${BASE}/api/v1/moments" \
    "${AUTH[@]}" \
    "${HDR_USER[@]}" \
    "${HDR_NAME[@]}" \
    -F "type=${type}" \
    -F "mediaType=image" \
    -F "caption=${caption}" \
    -F "location=${location}" \
    -F "tags=seed,travel,demo" \
    -F "mediaUrls=${media_url}" \
    -F "audience=followers" \
    | head -c 400
  echo
}

echo "Seeding moments to ${BASE} ..."
post_moment "travel" "Sunset over the caldera" "Santorini · Cyclades" "https://picsum.photos/1080/1920?random=41"
post_moment "trip" "Golden hour in Alfama" "Lisbon · Alfama" "https://picsum.photos/1080/1920?random=42"
post_moment "place" "Alpine morning" "Interlaken" "https://picsum.photos/1080/1920?random=43"
post_moment "live" "Sky stream" "LIVE" "https://picsum.photos/1080/1920?random=44"

echo ""
echo "Verify feed:"
curl -sS "${BASE}/api/v1/moments/feed?limit=10" "${AUTH[@]}" "${HDR_USER[@]}" | head -c 800
echo
