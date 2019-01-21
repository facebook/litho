#!/bin/bash

# This script is used to codemod all default state update calls to the sync variant
# via a list of all specs. This is useful to keep current behavior, and then adopt
# async behavior with new callsites.
#
# NOTE: This script is not perfect, and will 1) convert calls that match state
# update calls in the same file 2) will miss state updates called from outside the
# particular ComponentrSpec.

all_specs=$1

if [[ -z $all_specs ]]; then
  # to generate this, you can run something like
  # egrep -lir --include=*Spec.{java,kt} --exclude=./buck* "@OnUpdateState\b" [directory_to_search] > all_specs_with_update_state.txt
  echo "Usage: $0 <all_specs_with_update_state.txt>"
  exit 1
fi

while read -r file; do
  echo "$file:"

  if ! matches=$(grep -A 1 "@OnUpdateState\b" "$file" | grep void) || [[ -z $matches ]]; then
    echo "! No matches for $file."
    continue
  fi

  while read -r m; do
    if [[ $m =~ static\ (<[a-zA-Z0-9\ ]+>\ )?void\ ([a-zA-Z0-9]+)\( ]]; then
      method=${BASH_REMATCH[2]}
      echo "  ${BASH_REMATCH[2]}"
    else
      echo "! Failed match for $m"
      continue
    fi

    # Replace callsites with sync variant
    sed -i "" -e 's/\(\.\s*\)'"$method"'(/\1'"$method"'Sync(/g' "$file"

  done <<< "$matches"
done < "$all_specs"
