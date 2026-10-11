---
name: verify
description: Run every build, test, sanitation and guard-rail check and report each result. Use before every commit and before declaring any task done.
---

# Verify

Run the checks in order from the repository root. Each must exit 0.

1. Format, build, unit and integration tests, coverage report:

   ```bash
   (cd java && ./mvnw -q spotless:check verify)
   ```

2. Contract lint (Spectral via `npx` on Node 22, the release CI uses; any error or warning fails it):

   ```bash
   npx -y @stoplight/spectral-cli@6 lint --fail-severity=warn java/src/main/resources/openapi/openapi.yaml
   ```

3. Sanitation dry run (static analysis findings fail it):

   ```bash
   (cd java && ./mvnw -q -Psanitize rewrite:dryRun)
   ```

4. Guard rails (protected files, commit messages, contract, banned patterns, assumptions):

   ```bash
   scripts/guardrails.sh
   ```

## Report

Print one line per check: `PASS <check>` or `FAIL <check>: <first relevant error line>`.
Relay every `WARN` line from `scripts/guardrails.sh` verbatim.

## On failure

- Stop at the first failing check.
- Fix the cause, never the check: do not skip, exclude, weaken or delete a test or a guard rail.
- For formatting, run `(cd java && ./mvnw spotless:apply)`.
- For sanitation findings, run `(cd java && ./mvnw -Psanitize rewrite:run && ./mvnw spotless:apply)` and review the diff.
- Rerun `/verify` from step 1 until every check passes.
- If the fix is outside the task's scope or conflicts with a hard constraint in `AGENTS.md`, stop and report.
