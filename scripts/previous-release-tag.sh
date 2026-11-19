#!/usr/bin/env bash
set -euo pipefail

current_tag="${1:?usage: previous-release-tag.sh <current-tag>}"

while IFS= read -r tag || [ -n "$tag" ]; do
  if [ -n "$tag" ] && [ "$tag" != "$current_tag" ]; then
    printf '%s\n' "$tag"
    exit 0
  fi
done
