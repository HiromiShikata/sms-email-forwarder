#!/usr/bin/env bash
set -euo pipefail

previous_tag="${1:-}"
head_ref="${2:-HEAD}"
log_format='- %s (%h)'

if [ -n "$previous_tag" ] && git rev-parse --verify --quiet "${previous_tag}^{commit}" >/dev/null; then
  git log --no-merges --format="$log_format" "${previous_tag}..${head_ref}"
else
  git log --no-merges --format="$log_format" -n 30 "$head_ref"
fi
