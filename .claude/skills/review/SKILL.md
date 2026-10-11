---
name: review
description: Independent review of a diff or the whole repository against the assignment, the hard constraints and the rules. Produces a findings table and changes no code.
---

# Review

Scope: the diff named by the developer (default `git diff main...HEAD`), or the whole repository when asked.
Read `docs/ASSIGNMENT.md`, `AGENTS.md`, `docs/ASSUMPTIONS.md` and `.claude/rules/*.md` first.

## Checklist

1. **Requirement traceability** — map every requirement in `docs/ASSIGNMENT.md` to the code that implements it and the test that proves it. A requirement without code or without a test is a finding.
2. **Hard constraints** — `TimeDeposit.java` unchanged; `TimeDepositCalculator` keeps its package, no-arg constructor, `updateBalance(List<TimeDeposit>)` and behaviour; exactly two endpoints; assignment text unchanged.
3. **Dead code** — unused classes, members, parameters, dependencies and configuration; speculative abstractions with a single trivial implementation and no requirement behind them.
4. **Comments and names** — audit against `.claude/rules/java-style.md`: narrating comments, Javadoc on obvious members, abbreviations, forbidden suffixes, names that do not use the assignment's domain words.
5. **Layering** — audit imports and dependencies against `.claude/rules/architecture.md`.
6. **Assumptions** — every `// ASSUMPTION A<n>` has a row in `docs/ASSUMPTIONS.md` and every row is referenced where it applies.
7. **Tests** — tests assert behaviour through public APIs, not implementation details; expected values are literals; one behaviour per test.

## Output

A single table, most severe first, and nothing else:

| severity | file:line | finding | suggested fix |
|---|---|---|---|

Severity is `blocker` (breaks a hard constraint or a requirement), `major` (violates a rule) or `minor` (readability).
Make no code changes.
