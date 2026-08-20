#!/usr/bin/env bash
set -euo pipefail

: "${SOURCE_SHA:?SOURCE_SHA is required}"
: "${APK:?APK is required}"
: "${AAB:?AAB is required}"
: "${EVIDENCE:?EVIDENCE is required}"
: "${CANDIDATE:?CANDIDATE is required}"
: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"
: "${GITHUB_RUN_ID:?GITHUB_RUN_ID is required}"

[[ "$SOURCE_SHA" =~ ^[0-9a-f]{40}$ ]] || {
  echo "SOURCE_SHA must be a full lowercase Git SHA" >&2
  exit 1
}

for file in "$APK" "$AAB" "$EVIDENCE" "$CANDIDATE"; do
  test -f "$file" || { echo "required candidate file is missing: $file" >&2; exit 1; }
done

command -v gh >/dev/null || { echo "GitHub CLI is required to retain candidate release assets" >&2; exit 1; }
command -v sha256sum >/dev/null || { echo "sha256sum is required" >&2; exit 1; }

short_sha="${SOURCE_SHA:0:12}"
tag="closed-alpha/android/$SOURCE_SHA"
stage_dir="${RUNNER_TEMP:?RUNNER_TEMP is required}/closed-alpha-android-$short_sha"
mkdir -p "$stage_dir"

cp "$APK" "$stage_dir/eyespie-android-$short_sha.apk"
cp "$AAB" "$stage_dir/eyespie-android-$short_sha.aab"
cp "$EVIDENCE" "$stage_dir/release-evidence.json"
cp "$CANDIDATE" "$stage_dir/candidate.json"
(
  cd "$stage_dir"
  sha256sum \
    "eyespie-android-$short_sha.apk" \
    "eyespie-android-$short_sha.aab" \
    release-evidence.json \
    candidate.json \
    > SHA256SUMS
)

notes="$stage_dir/RELEASE-NOTES.md"
cat > "$notes" <<EOF
Closed-alpha Android qualification candidate.

- Source SHA: \`$SOURCE_SHA\`
- Workflow run: \`$GITHUB_SERVER_URL/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID\`
- Distribution state: validated candidate; not a production release
- Storage: draft GitHub Release assets; intentionally outside GitHub Actions artifact storage

The APK/AAB and evidence files in this draft release are the exact files retained from the protected qualification job.
EOF

if gh release view "$tag" --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1; then
  is_draft="$(gh release view "$tag" --repo "$GITHUB_REPOSITORY" --json isDraft --jq '.isDraft')"
  test "$is_draft" = "true" || {
    echo "refusing to overwrite non-draft release for $tag" >&2
    exit 1
  }
  gh release edit "$tag" \
    --repo "$GITHUB_REPOSITORY" \
    --draft \
    --title "Closed alpha Android $short_sha" \
    --notes-file "$notes"
  gh release upload "$tag" \
    "$stage_dir/eyespie-android-$short_sha.apk" \
    "$stage_dir/eyespie-android-$short_sha.aab" \
    "$stage_dir/release-evidence.json" \
    "$stage_dir/candidate.json" \
    "$stage_dir/SHA256SUMS" \
    --repo "$GITHUB_REPOSITORY" \
    --clobber
else
  gh release create "$tag" \
    "$stage_dir/eyespie-android-$short_sha.apk" \
    "$stage_dir/eyespie-android-$short_sha.aab" \
    "$stage_dir/release-evidence.json" \
    "$stage_dir/candidate.json" \
    "$stage_dir/SHA256SUMS" \
    --repo "$GITHUB_REPOSITORY" \
    --target "$SOURCE_SHA" \
    --title "Closed alpha Android $short_sha" \
    --notes-file "$notes" \
    --draft
fi

release_url="$(gh release view "$tag" --repo "$GITHUB_REPOSITORY" --json url --jq '.url')"
{
  echo '### Retained Android candidate'
  printf -- '- draft release: %s\n' "$release_url"
  printf -- '- tag: `%s`\n' "$tag"
  printf -- '- source: `%s`\n' "$SOURCE_SHA"
  echo '- GitHub Actions artifact storage used: `no`'
} >> "$GITHUB_STEP_SUMMARY"

printf 'release_url=%s\n' "$release_url"
