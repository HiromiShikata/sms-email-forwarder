#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "usage: $0 <releases.json>" >&2
  exit 2
fi

input="$1"
if [ ! -f "$input" ]; then
  echo "releases file not found: $input" >&2
  exit 2
fi

jq -s '
  add
  | if type == "array" then . else error("releases input must be a JSON array") end
  | map(select(.draft | not))
  | sort_by(.published_at)
  | reverse
  | map({
      version: (.tag_name | ltrimstr("v")),
      tag: .tag_name,
      publishedAt: .published_at,
      notes: (.body // "")
    })
' "$input"
