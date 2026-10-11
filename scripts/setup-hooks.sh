#!/usr/bin/env bash
# Points git at the versioned hooks in .githooks; safe to run more than once.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."
git config core.hooksPath .githooks
chmod +x .githooks/*
echo "git hooks enabled from .githooks"
