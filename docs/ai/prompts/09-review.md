# Step 9 — Independent final review and fixes

Two roles, two agents. The **review agent** has never seen the work and makes no code changes.
The **fixer** is started only after the developer triages the findings.

## Review agent

Read `docs/ASSIGNMENT.md` first and write down, before looking at any code, the list of every
requirement sentence it contains (numbered R1…Rn, including "Important Guidelines",
"Preferred Stack" and "Submission Instructions"). Then read `AGENTS.md`, the rules,
`docs/ASSUMPTIONS.md`, `docs/ARCHITECTURE.md`, `README.md`, and only then the code.

Produce `prompts/reports/09-review.md` with:

1. **Requirements traceability matrix** — one row per R: requirement (quoted) → where
   implemented (file:symbol) → where proven (test class:method, or "manual: README section")
   → status `met` / `partially met` / `not met` / `out of scope by assumption A<n>`.
2. **Hard-constraint audit** — `git diff c4ea358 -- java/src/main/java/org/ikigaidigital/TimeDeposit.java`
   empty; `TimeDepositCalculatorPublicApiTest` and `TimeDepositCalculatorTest` unchanged since
   their first commit (`git log --follow --oneline` shows one commit each); exactly two
   operations in the contract; no hand-written mapping annotations; the original README
   verbatim in `docs/ASSIGNMENT.md`.
3. **Every commit builds** — in a scratch clone (`git clone --no-local . /tmp/review-clone`),
   `git rebase --exec 'cd java && ./mvnw -q -o test 2>&1 | tail -3' c4ea358` (unit tests only
   at each commit; `-o` offline after one online run to warm the cache; if offline mode fails,
   drop `-o`). Report the first failing commit, if any. Also `git log --format='%s' c4ea358..HEAD`
   checked against the convention regex (expect `b2b33f5` as the one documented exception).
4. **`/review` skill output** on the whole repository (findings table with severities).
5. **Quality audit** beyond the skill: dead code (unused dependencies via
   `./mvnw dependency:analyze`, unused classes, config keys nobody reads), comments that
   narrate, names that leak implementation, tests that test the mock, duplicated mapping
   logic, anything a reader would ask "why is this here" about.
6. **Docs audit**: every claim in `README.md`, `ARCHITECTURE.md`, `AI-DEVELOPMENT.md` checked
   against the repository; run instructions executed once more.
7. **Score sheet**: for each of assignment §5 "SOLID principles, design patterns, clean code"
   and the preferred-stack items, one line: evidence or gap.

Findings table columns: `id | severity (blocker/major/minor) | file:line | finding | suggested fix`.
No code changes, no commits.

## Developer triage

The orchestrator shows the report; the developer marks each finding `fix` / `defer` / `reject`
(with a one-line reason recorded in the report).

## Fixer

For each finding marked `fix`, in severity order: smallest change, `/verify`, `/commit` with a
conventional message in the scope of the file touched (`fix(...)`, `refactor(...)`,
`test(...)`, `docs(...)`). Add the traceability matrix to the repository as
`docs/REQUIREMENTS.md` (`docs(repo): add requirements traceability matrix`), linked from the
README, after removing the review-internal columns (keep requirement → implementation →
test → status). Never touch frozen files; never lower a threshold. Push after the
orchestrator's gate.

## Re-review

The review agent (same agent, context intact) re-checks only the `fix` items and confirms the
traceability matrix in `docs/` matches its own. Done when no `blocker` or `major` remains and
the developer approves the remaining `minor`/`defer` list, which goes into
`prompts/reports/09-review.md` as the final state.
