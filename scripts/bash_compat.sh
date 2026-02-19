#!/usr/bin/env bash
# Compatibility shims for Bash 3 environments (sourced via BASH_ENV).

if ! builtin help mapfile >/dev/null 2>&1; then
  mapfile() {
    local trim_newline=false
    local OPTIND=1
    local opt
    while getopts ":t" opt; do
      case "$opt" in
        t) trim_newline=true ;;
        *) return 2 ;;
      esac
    done
    shift $((OPTIND - 1))

    local array_name="${1:-MAPFILE}"
    local line
    local escaped_value
    eval "$array_name=()"
    while IFS= read -r line || [[ -n "$line" ]]; do
      if [[ "$trim_newline" == "true" ]]; then
        line="${line%$'\n'}"
      fi
      printf -v escaped_value '%q' "$line"
      eval "$array_name+=( $escaped_value )"
    done
  }
fi
