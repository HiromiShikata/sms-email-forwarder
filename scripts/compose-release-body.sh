#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 5 ]; then
  echo "usage: compose-release-body.sh <version> <build-number> <short-sha> <previous-tag-or-empty> <output-file>" >&2
  exit 2
fi

version="$1"
build_number="$2"
short_sha="$3"
previous_tag="$4"
output_file="$5"

changes="$(bash "$(dirname "$0")/generate-release-notes.sh" "$previous_tag" HEAD)"
if [ -z "$changes" ]; then
  changes="No changes since the previous release."
fi

if [ -n "$previous_tag" ]; then
  changes_heading="$previous_tag"
else
  changes_heading="the start of the history"
fi

{
  printf '%s\n' "Automated build of main at ${short_sha} (version ${version}, build ${build_number}). Install sms-email-forwarder_arm64-v8a.apk for most modern Android devices, or sms-email-forwarder_universal.apk for any ABI."
  printf '\n'
  printf '## Changes since %s\n' "$changes_heading"
  printf '\n'
  printf '%s\n' "$changes"
} > "$output_file"
