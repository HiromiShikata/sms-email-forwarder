#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "usage: $0 <releases.json> [draft-tag]" >&2
  exit 2
fi

input="$1"
draft_tag="${2:-}"
if [ ! -f "$input" ]; then
  echo "releases file not found: $input" >&2
  exit 2
fi

generated_at="${MANIFEST_GENERATED_AT:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"

jq -s --arg draftTag "$draft_tag" --arg generatedAt "$generated_at" '
  add
  | if type == "array" then . else error("releases input must be a JSON array") end
  | if $draftTag != "" and (map(select(.tag_name == $draftTag)) | length) == 0
      then error("release \($draftTag) is missing from the releases list") else . end
  | map(select((.draft | not) or (.tag_name == $draftTag)))
  | map({
      version: (.tag_name | ltrimstr("v")),
      tag: .tag_name,
      publishedAt: (.published_at // $generatedAt),
      notes: (.body // "")
    })
  | sort_by(.publishedAt)
  | reverse
' "$input"
