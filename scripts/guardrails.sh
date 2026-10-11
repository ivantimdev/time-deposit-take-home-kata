#!/usr/bin/env bash
# Repository guard rails: protected files, commit messages, API surface, banned patterns, assumptions.
# Usage: scripts/guardrails.sh   (from any directory; exits non-zero on the first failing check)
#   GUARDRAILS_BASE=<commit>              first commit excluded from the message check (default 93d65c7)
#     the default starts after 93d65c7 because the commit before it, b2b33f5, predates the convention and is kept as is
#   GUARDRAILS_EXTRA_PATTERNS=<file>      extra extended regexes, one per line, matched against all tracked files
# Prints PASS, FAIL <check>: <detail>, SKIP or WARN per check. Needs bash, git, python3, shasum, grep, awk.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

readonly COMMIT_SUBJECT='^(feat|fix|refactor|test|docs|build|ci|chore)\([a-z]+\): [a-z].{0,70}[^.]$'
readonly CONTRACT=java/src/main/resources/openapi/openapi.yaml

pass() { echo "PASS $1"; }
skip() { echo "SKIP $1: $2"; }
fail() {
  echo "FAIL $1: $2"
  exit 1
}

# Lists the non-comment, non-blank lines of a config file.
entries() { grep -vE '^[[:space:]]*(#|$)' "$1" || true; }

check_protected_files() {
  local path expected actual
  while IFS= read -r path; do
    [ -f "$path" ] || fail protected-files "$path is missing"
    expected=$(awk -v path="$path" '$2 == path { print $1 }' scripts/protected-files.sha256)
    [ -n "$expected" ] || fail protected-files "$path has no hash in scripts/protected-files.sha256"
    actual=$(shasum -a 256 "$path" | awk '{ print $1 }')
    [ "$actual" = "$expected" ] || fail protected-files "$path changed (sha256 $actual, expected $expected)"
  done < <(entries .claude/protected-files.txt)
  pass protected-files
}

check_commit_messages() {
  local base="${GUARDRAILS_BASE:-93d65c7}" commit subject
  git rev-parse --verify --quiet "$base^{commit}" >/dev/null || fail commit-messages "base $base not found"
  for commit in $(git rev-list "$base..HEAD"); do
    subject=$(git log -1 --format=%s "$commit")
    echo "$subject" | grep -qE "$COMMIT_SUBJECT" ||
      fail commit-messages "${commit:0:7} '$subject' does not match $COMMIT_SUBJECT"
    [ "${#subject}" -le 72 ] || fail commit-messages "${commit:0:7} subject is ${#subject} characters (max 72)"
    [ -z "$(git log -1 --format=%b "$commit")" ] || fail commit-messages "${commit:0:7} has a body or trailers"
  done
  pass commit-messages
}

check_contract_operations() {
  if [ ! -f "$CONTRACT" ]; then
    skip contract-operations "$CONTRACT does not exist yet"
    return
  fi
  local count
  count=$(python3 - "$CONTRACT" <<'PY'
import re
import sys

path = sys.argv[1]
methods = {"get", "post", "put", "patch", "delete", "head", "options", "trace"}
try:
    import yaml

    with open(path) as contract:
        paths = yaml.safe_load(contract).get("paths") or {}
    print(sum(1 for item in paths.values() for key in item if key in methods))
except ImportError:
    with open(path) as contract:
        print(sum(1 for line in contract if re.match(r"^\s{4}(get|post|put|patch|delete|head|options):", line)))
PY
)
  [ "$count" -eq 2 ] || fail contract-operations "$CONTRACT declares $count operations (expected exactly 2)"
  pass contract-operations
}

check_mapping_annotations() {
  local hits
  hits=$(grep -rnE '@(Get|Post|Put|Patch|Delete|Request)Mapping' java/src/main/java || true)
  [ -z "$hits" ] || fail mapping-annotations "hand-written mapping annotation: $(echo "$hits" | head -1)"
  pass mapping-annotations
}

# A pattern prefixed with "icase:" is matched case-insensitively.
check_banned_patterns() {
  local excluded pattern flags file hit
  excluded=$(entries scripts/guardrails-exclude.txt)
  while IFS= read -r pattern; do
    flags=-nE
    case "$pattern" in
      icase:*) pattern=${pattern#icase:} flags=-niE ;;
    esac
    while IFS= read -r file; do
      echo "$excluded" | grep -qxF "$file" && continue
      hit=$(grep $flags -e "$pattern" "$file" | head -1 || true)
      [ -z "$hit" ] || fail banned-patterns "$file:$hit (pattern: $pattern)"
    done < <(find java/src -name '*.java' -type f | sort)
  done < <(entries scripts/banned-patterns.txt)
  pass banned-patterns
}

check_embedded_database() {
  if grep -qE '<artifactId>[[:space:]]*(h2|hsqldb)[[:space:]]*</artifactId>' java/pom.xml; then
    fail embedded-database "java/pom.xml declares an embedded database"
  fi
  pass embedded-database
}

check_assumptions() {
  local referenced id
  referenced=$(grep -rhoE 'ASSUMPTION A[0-9]+' java/src | awk '{ print $2 }' | sort -u || true)
  for id in $referenced; do
    grep -qE "^\| $id \|" docs/ASSUMPTIONS.md || fail assumptions "$id is cited in java/src but has no row in docs/ASSUMPTIONS.md"
  done
  for id in $(grep -oE '^\| A[0-9]+ \|' docs/ASSUMPTIONS.md | awk '{ print $2 }'); do
    echo "$referenced" | grep -qxF "$id" || echo "WARN unreferenced $id"
  done
  pass assumptions
}

check_extra_patterns() {
  local patterns="${GUARDRAILS_EXTRA_PATTERNS:-}" regexes hit
  if [ -z "$patterns" ] || [ ! -r "$patterns" ]; then
    skip extra-patterns "GUARDRAILS_EXTRA_PATTERNS not set to a readable file"
    return
  fi
  regexes=$(mktemp)
  entries "$patterns" >"$regexes"
  # Only file:line is printed so that private patterns never reach logs.
  hit=$(git ls-files -z | xargs -0 grep -nE -f "$regexes" -- 2>/dev/null | head -1 | cut -d: -f1,2 || true)
  rm -f "$regexes"
  [ -z "$hit" ] || fail extra-patterns "match at $hit"
  pass extra-patterns
}

check_protected_files
check_commit_messages
check_contract_operations
check_mapping_annotations
check_banned_patterns
check_embedded_database
check_assumptions
check_extra_patterns
