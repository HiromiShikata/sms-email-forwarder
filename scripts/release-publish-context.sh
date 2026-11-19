#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "usage: $0 <releases.json> <tag>" >&2
  exit 2
fi

releases="$1"
tag="$2"
if [ ! -f "$releases" ]; then
  echo "releases file not found: $releases" >&2
  exit 2
fi

current="${tag#v}"
lower=()
newer_published=0
published_tags=$(jq -s -r --arg tag "$tag" 'add | if type == "array" then . else error("releases input must be a JSON array") end | map(select((.draft | not) and .tag_name != $tag)) | .[].tag_name' "$releases")
while IFS= read -r published; do
  if [ -z "$published" ]; then
    continue
  fi
  candidate="${published#v}"
  if [ "$candidate" = "$current" ]; then
    continue
  fi
  top=$(printf '%s\n%s\n' "$candidate" "$current" | sort -V | tail -n 1)
  if [ "$top" = "$candidate" ]; then
    newer_published=1
  else
    lower+=("$published")
  fi
done <<< "$published_tags"

previous=""
if [ "${#lower[@]}" -gt 0 ]; then
  previous=$(printf '%s\n' "${lower[@]}" | sort -V | tail -n 1)
fi

if [ "$newer_published" -eq 1 ]; then
  is_newest=false
else
  is_newest=true
fi

echo "PREVIOUS_TAG=$previous"
echo "IS_NEWEST=$is_newest"
