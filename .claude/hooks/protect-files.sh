#!/usr/bin/env bash
# PreToolUse hook: blocks Edit/Write/MultiEdit/NotebookEdit on any path in .claude/protected-files.txt, and
# blocks Bash commands that mention a protected path or basename together with a mutating token (redirection,
# sed -i, tee, rm, mv, cp, truncate, git mv, git rm, perl -i). Heuristic: read-only commands such as cat stay allowed.
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
hook_input="$(cat)"

HOOK_INPUT="$hook_input" exec python3 - "$repo_root" <<'PY'
import json
import os
import re
import sys

repo_root = os.path.realpath(sys.argv[1])
with open(os.path.join(repo_root, ".claude", "protected-files.txt")) as listing:
    protected = [line.strip() for line in listing if line.strip() and not line.lstrip().startswith("#")]

event = json.loads(os.environ["HOOK_INPUT"])
tool_name = event.get("tool_name", "")
tool_input = event.get("tool_input") or {}

MUTATING = re.compile(
    r">|\bsed\s+(-\w*\s+)*-\w*i|\btee\b|\brm\s|\bmv\s|\bcp\s|\btruncate\b|\bgit\s+(mv|rm)\b|\bperl\s+(-\w*\s+)*-\w*i"
)
HARMLESS_REDIRECTS = re.compile(r"\d*>\s*/dev/null|\d*>&\d")


def block(path):
    print(f"BLOCKED: {path} is protected (see .claude/protected-files.txt)", file=sys.stderr)
    sys.exit(2)


def repo_relative(file_path):
    absolute = os.path.realpath(os.path.join(os.getcwd(), file_path))
    return os.path.relpath(absolute, repo_root)


if tool_name in ("Edit", "Write", "MultiEdit", "NotebookEdit"):
    file_path = tool_input.get("file_path") or tool_input.get("notebook_path") or ""
    if file_path and repo_relative(file_path) in protected:
        block(repo_relative(file_path))
elif tool_name == "Bash":
    command = tool_input.get("command", "")
    if MUTATING.search(HARMLESS_REDIRECTS.sub("", command)):
        for path in protected:
            if path in command or re.search(rf"(?<![\w.-]){re.escape(os.path.basename(path))}(?![\w.-])", command):
                block(path)
sys.exit(0)
PY
