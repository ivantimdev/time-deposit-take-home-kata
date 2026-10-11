# Step 9 — Independent final review, triage, fixes, re-check

You are a senior engineer with no prior knowledge of this repository. You run in Claude Code
at the repository root. The work is a take-home refactoring solution. It must contain no dead
code, no redundant or narrating comments, no unclear names, no poor API design, no breaking
change to the shared class or method signature, and no code the author cannot explain. Your
job is to find every such thing, then fix what the developer approves.

Three phases, with a hard stop after phase 1. Do not fix anything during phase 1.

---

## Phase 1 — Review (read-only; no edits, no commits)

### 1.0 Order of reading (follow it; it keeps you independent)

1. `docs/ASSIGNMENT.md` — the assignment. Before anything else, write down every requirement
   sentence as `R1…Rn`, including "Important Guidelines", "Preferred Stack" and "Submission
   Instructions". Do this from the assignment alone.
2. `README.md`, `docs/ARCHITECTURE.md`, `docs/ASSUMPTIONS.md`, `docs/AI-DEVELOPMENT.md`,
   `docs/ai/WORKFLOW-LOG.md` — what the solution claims.
3. `AGENTS.md` and `.claude/rules/*.md` — the rules the code says it follows.
4. The code: `java/src/main` package by package (root, `domain`, `application`, `adapter.out`,
   `adapter.in`, `config`), then `java/src/test`, then `java/pom.xml`, `java/rewrite.yml`,
   migrations, `openapi.yaml`, `compose.yaml`, `java/Dockerfile`, `.github/workflows/ci.yml`,
   `scripts/`, `.githooks/`, `.claude/`.
5. Last: `prompts/reports/orchestrator.md` — its "step 9 list" holds minors deferred by
   earlier gates. Every item there becomes a finding in your table (keep its wording, add your
   own severity). The reports are private, git-ignored files; never cite them in anything
   committed.

### 1.1 Requirements traceability matrix

One row per `R`: requirement (quoted) → implementation (`file:symbol`) → proof (`TestClass.method`,
or `manual: README §…`, or `CI job …`) → status `met` / `partially met` / `not met` /
`out of scope by A<n>`. A requirement without code **or** without proof is a finding.

### 1.2 Hard constraints (each is a command you run, result pasted)

- `git diff c4ea358 -- java/src/main/java/org/ikigaidigital/TimeDeposit.java` → empty.
- `git log --oneline --follow -- java/src/test/java/org/ikigaidigital/TimeDepositCalculatorTest.java`
  and the same for `TimeDepositCalculatorPublicApiTest.java` → exactly one commit each.
- `git diff c4ea358:README.md HEAD:docs/ASSIGNMENT.md` → empty.
- `scripts/guardrails.sh` → all PASS/SKIP, no WARN; then run it again with
  `GUARDRAILS_EXTRA_PATTERNS=<pattern file>` → still green.
- `openapi.yaml` declares exactly two operations; `grep -rn '@\(Get\|Post\|Put\|Patch\|Delete\|Request\)Mapping' java/src/main` → nothing.
- Plain-Java consumer simulation: in a temp directory outside the repo, compile and run a
  single-file Java program against `java/target/classes` that does
  `new org.ikigaidigital.TimeDepositCalculator().updateBalance(List.of(new TimeDeposit(1,"basic",1234567.00,45)))`
  and prints the balance → `1235595.81`, with **no Spring on the classpath**.

### 1.3 Every commit builds

`git clone --no-local . /tmp/review-clone && cd /tmp/review-clone && git rebase --exec 'cd java && ./mvnw -q -o test 2>&1 | tail -3' c4ea358`
(unit tests at every commit; run one online `./mvnw -q test` first to warm the cache; drop `-o`
if offline mode fails). Report the first failing commit if any. Also check every subject in
`git log --format=%s c4ea358..HEAD` against
`^(feat|fix|refactor|test|docs|build|ci|chore)\([a-z]+\): [a-z].{0,70}[^.]$` — `b2b33f5` is the
one documented exception. Count commits per step from the workflow log; note any step with
more than 12.

### 1.4 Full pipeline, locally

- `(cd java && ./mvnw -q spotless:check verify)` — paste the Surefire/Failsafe counts.
- `(cd java && ./mvnw -q -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage)` — score.
- `(cd java && ./mvnw -q -Psanitize rewrite:dryRun)`.
- `npx -y @stoplight/spectral-cli@6 lint java/src/main/resources/openapi/openapi.yaml`.
- `(cd java && ./mvnw -q dependency:analyze)` — unused declared / used undeclared dependencies
  are findings unless justified in a pom comment.
- `docker compose up --build -d`, wait for readiness, `curl` both endpoints exactly as the
  README says, compare with the README's stated numbers, open `/swagger-ui/index.html`
  (status 200), `docker compose down -v`.
- `gh run list --limit 3` → latest `main` run green; `gh api` or `docker pull` → image tags
  exist in GHCR.

### 1.5 Code review — read every file as the strictest colleague you know

Run the `/review` skill on the whole repository and merge its table with your own findings
from the checks below. For each file ask: *would the author be able to explain why this line
exists?* Specifically hunt for:

- **Dead code**: unused classes, methods, parameters, fields, imports, dependencies,
  configuration keys nobody reads, test helpers used once, abstractions with a single trivial
  implementation and no requirement behind them (the `Plan` record and `InterestPolicy`
  interface are justified by assignment §4 — check that the justification is written where a
  reader looks).
- **Comments**: anything that narrates *what*; Javadoc on obvious members; any comment that
  reads as generated ("This method…", "Initialize…"); comments that cite an assumption id that
  does not match the register; assumption comments that are placed where the behaviour is
  not actually decided.
- **Names**: abbreviations, `Impl`/`Helper`/`Util`/`Manager`, names that don't use the
  assignment's domain words, test names that don't state behaviour, inconsistent singular/plural.
- **API design**: paths, methods, status codes, response shapes vs the contract; the contract
  vs the README; `additionalProperties`, `required`, examples; the RESTful justification in
  A17 present and accurate; anything that smells RPC without the stated reason.
- **Hexagonal boundaries**: imports across layers that ArchUnit might not cover (check the
  rules' package patterns against the real package names — a mistyped pattern passes); JPA or
  web types reaching the application; domain depending on anything.
- **Money and rounding**: every `Double`↔`BigDecimal` conversion uses `valueOf` + scale 2 at
  the boundaries, `new BigDecimal(double)` only in the calculator with its comment.
- **Concurrency**: the lock query is actually `FOR UPDATE` (check generated SQL in the IT log
  or by `EXPLAIN`-style inspection), the transaction spans load→calculate→save→reload, the
  concurrency IT really runs threads in parallel (latch) and asserts an exact result.
- **Tests**: assertions on behaviour not implementation; expected values literal; no test that
  passes vacuously (run one deliberately broken expectation per test class in a scratch copy
  if in doubt); Testcontainers singleton; ITs clean up their rows; no Mockito where banned.
- **Guard rails and tooling**: each check in `scripts/guardrails.sh` fails when its rule is
  broken (break each one in the scratch clone, one at a time; a check that passes anyway is a
  **major**); the hook blocks a protected edit; `commit-msg` rejects a bad message; thresholds
  are what the pom says they are.
- **Docs**: every claim in `README.md`, `ARCHITECTURE.md`, `AI-DEVELOPMENT.md` is true of the
  repository; every relative link resolves; Mermaid renders (syntax check); README numbers
  match what the seed produces; `docs/ai/prompts/*` are consistent with what the workflow log
  says happened; nothing anywhere mentions other solutions or requirements the assignment
  does not state (the pattern scan is mechanical, your reading is the second line).
- **Licensing and hygiene**: bundled third-party assets carry their notices; `.gitattributes`,
  `.dockerignore`, `.gitignore` are coherent; no secrets, no local paths, no `.idea` leftovers.

### 1.6 Design defensibility

List every non-obvious decision a reader will ask "why?" about (at least: two classes for the
deposit, `PlanType` enum + String matching, `new BigDecimal(double)`, `POST …/update-balances`,
`200` with the list, pessimistic lock and no idempotency, snake_case, FK without JPA
navigation, static Swagger UI, Lombok scope, mutation 100 %, OpenRewrite recipe choice,
Spotless exclusions, why the original README moved). For each: where the rationale is written
in the repository, or `MISSING` (a finding).

### 1.7 Output

Write `prompts/reports/09-review.md` with sections 1.1–1.6 and a single **findings table**,
most severe first:

`id | severity | file:line | finding | suggested fix | source (own / skill / deferred-step-N)`

Severity: `blocker` = breaks a hard constraint or a requirement, or a guard rail that fails
open; `major` = violates a rule, misleads a reader, or is dead code; `minor` = readability.

Then **stop** and print to the developer: the counts per severity, the blockers and majors in
full, and the sentence "Awaiting triage: mark each id fix / defer / reject."

---

## Phase 2 — Triage (the developer)

The developer answers with a decision per id and a one-line reason for every `defer` and
`reject`. Record the decisions in the report. Do not start phase 3 without them.

---

## Phase 3 — Fixes and re-check (same session, after triage)

Rules of the repository apply (`AGENTS.md`, `.claude/rules/`): run `/verify` before every
commit, use `/commit`, one logical change per commit, one commit per scope per round, messages
per the convention, never touch frozen files, never lower a threshold, never `--no-verify`,
never amend.

1. Fix `fix` items in severity order. Message types by content: `fix(...)` for wrong behaviour
   or a guard rail that fails open, `refactor(...)` for structure, `test(...)`, `docs(...)`,
   `build(config)`, `chore(repo|ai)`.
2. Add the traceability matrix as `docs/REQUIREMENTS.md` (columns: requirement → implementation
   → proof → status; drop review-internal columns; link it from `README.md` under
   "Assumptions" or a new "Requirements" line) — `docs(repo): add requirements traceability matrix`.
3. Append to `docs/ai/WORKFLOW-LOG.md` a "Final review" section: counts per severity, what was
   fixed, what was deferred with reasons (no process wording, no private file names).
4. Re-run 1.2, 1.3 (only for the new commits: `git rebase --exec … <pre-review HEAD>`), 1.4, and
   the pattern scan. Re-read every file you changed against 1.5.
5. Push: `git push origin main`; `gh run watch --exit-status` until green; if red, fix forward.
6. Update `prompts/reports/09-review.md` with the final state: remaining `minor`/`defer` list,
   commit list, CI run URL, image tags. Print a 10-line summary to the developer.

Done when: no `blocker` or `major` remains open, the developer has approved the remaining
list, CI is green on the pushed HEAD, and `/verify` is green locally.
