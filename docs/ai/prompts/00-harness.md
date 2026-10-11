# Step 0 — AI harness, build tooling and guard rails

You are a senior Java engineer setting up the development harness for a take-home
refactoring kata. You work alone in this repository, at its root
(`time-deposit-take-home-kata/`). The developer reviews every diff and every commit
message you produce. The result must contain no dead code, redundant AI-style comments,
unclear names, poor API design or breaking changes.

This step adds **no production code and no tests**. It produces the rules, tooling,
hooks and scripts that every later step (and every later change) will run under.

Read `README.md` fully before doing anything. It is the assignment.

---

## 0. Hard constraints (apply to this step and to every later one)

1. `java/src/main/java/org/ikigaidigital/TimeDeposit.java` is **frozen**. Never edit,
   move, rename, reformat or annotate it. Zero diff, ever.
2. `org.ikigaidigital.TimeDepositCalculator` keeps its package, its public no-arg
   constructor and `public void updateBalance(List<TimeDeposit>)`, and its observable
   behaviour. (This step does not touch it at all.)
3. The final solution has **exactly two API endpoints** (update all balances, list all
   deposits). Swagger UI and the served contract are documentation, not endpoints.
4. The assignment text (`README.md` today, `docs/ASSIGNMENT.md` after this step) is
   preserved verbatim and frozen.
5. Ambiguity is resolved by a stated assumption: a row in `docs/ASSUMPTIONS.md` and a
   `// ASSUMPTION A<n>: <one line>` comment at the point in code where it applies.
6. Agents may `git push` the current branch only after `/verify` is green; they never
   `--amend`, rebase, reset or force-push published history.
7. If any instruction here conflicts with 1–6, stop and report instead of working around it.

## 1. Environment

- JDK 17 or newer (the developer machine has JDK 21; the project targets Java 17 via
  `<release>17</release>` and CI builds on 17 — never use APIs newer than 17), Git,
  `python3` (used by hooks and scripts), network access to Maven Central.
- Do not install anything globally. Everything must be reproducible from a clean clone.
- Run every Maven command through the wrapper you add: `./java/mvnw -f java/pom.xml …`
  (or `cd java && ./mvnw …`). `mvn` may not be installed.
- Look up **current stable** versions of every plugin and library you add
  (Maven Central / official docs). Do not guess versions. Record the versions you
  chose in the report.
- Claude Code specifics you rely on (verify against https://docs.claude.com if unsure):
  - Project instructions: `CLAUDE.md` at the repo root; `@path` imports another file.
  - Rules: `.claude/rules/*.md` are loaded automatically.
  - Skills: `.claude/skills/<name>/SKILL.md` with YAML frontmatter `name`, `description`;
    invoked as `/<name>`.
  - Hooks: `.claude/settings.json` → `hooks.PreToolUse[]` with `matcher` (regex over tool
    names) and `hooks[]` of `{"type":"command","command":"..."}`. The command receives
    JSON on stdin (`tool_name`, `tool_input` with `file_path` for Edit/Write/MultiEdit and
    `command` for Bash). **Exit code 2 blocks the tool call** and stderr is shown to the
    agent. Hooks are snapshotted at session start, so you test your hook by piping sample
    JSON into the script, not by editing a protected file from this session.
  - Permissions: `.claude/settings.json` → `permissions.allow[]` / `permissions.deny[]`
    with rules like `"Bash(git push:*)"`. Deny wins over allow.

## 2. Deliverables — ordered commits

Each item is one commit. Build and tests must pass at every commit. Commit messages are
given below and must be used as written (they follow the convention in §4).

### Commit 1 — `docs(repo): move assignment to docs and add solution readme`
- `git mv README.md docs/ASSIGNMENT.md` (verbatim; no edits).
- New `README.md`, short and true today (it is completed in a later step):
  - title "Time Deposit Service — XA Bank kata solution"
  - one paragraph: Spring Boot / PostgreSQL service exposing two endpoints
    (update all balances, list all deposits) built as a hexagonal refactoring of the
    original calculator; links to `docs/ASSIGNMENT.md`.
  - "Status" line: in progress; sections `How to run`, `How to test`, `Architecture`,
    `AI-assisted development` listed as headings with one line each pointing to where the
    content will live (`docs/ARCHITECTURE.md`, `docs/AI-DEVELOPMENT.md`, `docs/ASSUMPTIONS.md`).
    No placeholder text like "TBD" or "coming soon"; write the one true sentence each.
  - "Developer setup": `scripts/setup-hooks.sh` once after clone; `./mvnw verify`.

### Commit 2 — `docs(repo): add assumptions register`
`docs/ASSUMPTIONS.md`:
- Two paragraphs explaining the convention: every reading of an ambiguous requirement
  gets an id `A<n>`, a row here, and a `// ASSUMPTION A<n>: …` comment where it applies;
  ids are never reused or renumbered.
- Table with columns `Id | Requirement text (quoted) | Ambiguity | Decision | Where applied`.
- First row, A1: "The final solution must include exactly two API endpoints" — the
  OpenAPI contract file served for Swagger UI and the Swagger UI itself are documentation
  of those endpoints, not additional endpoints; no actuator or other HTTP surface is added.
- Second row, A2: "Embrace Hexagonal Architecture" — transactions are declared with
  `@Transactional` on use-case services; the domain stays framework-free and the
  application layer is otherwise free of persistence and web dependencies (enforced by
  ArchUnit). `Where applied`: the application services, once they exist.

### Commit 3 — `build(config): add maven wrapper`
- `cd java && mvn wrapper:wrapper` equivalent (use the Maven Wrapper plugin; if `mvn` is
  absent, download the wrapper distribution per official instructions). Commit `mvnw`,
  `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`. Do **not** commit a wrapper jar.
- Pin a current Maven 3.9.x.

### Commit 4 — `build(config): add formatting, lint, coverage and test phases`
In `java/pom.xml` (keep Java 17; keep groupId/artifactId/version):
- **Spotless** with `google-java-format` (default Google style, not AOSP), bound to
  `check` in the `verify` phase. Also configure `removeUnusedImports` and
  `formatAnnotations`. Exclude, with an XML comment "original kata sources; each is
  formatted when it is first changed; TimeDeposit.java is never changed":
  - `src/main/java/org/ikigaidigital/TimeDeposit.java`
  - `src/main/java/org/ikigaidigital/TimeDepositCalculator.java`
  - `src/test/java/org/ikigaidigital/TimeDepositCalculatorTest.java`
- **maven-compiler-plugin** (current version): `<release>17</release>`,
  `-Xlint:all`, `showWarnings=true`. No `-Werror` (generated code arrives later).
- **Surefire** (current version) runs `**/*Test.java`; **Failsafe** (current version) runs
  `**/*IT.java` in `integration-test`/`verify`. Both with JUnit 5. Upgrade
  `junit-jupiter` to a current 5.x BOM-managed version and AssertJ to current 3.x.
  Replace `junit-jupiter-engine` with `junit-jupiter` (api + engine + params).
- **JaCoCo** (current version): `prepare-agent` + `report` at `verify`. No threshold yet
  (added when there is code to gate). Exclusion placeholder is not needed; later steps add
  generated-code exclusions.
- Run `./mvnw -q verify` — green, with the placeholder test executing.

### Commit 5 — `build(config): add mutation testing profile`
- `pitest-maven` + `pitest-junit5-plugin` (current versions) in a profile `mutation`,
  not active by default.
- `targetClasses`: `org.ikigaidigital.*` **excluding** `org.ikigaidigital.adapter.*`,
  `org.ikigaidigital.config.*`, `org.ikigaidigital.*Application`.
  `targetTests`: `org.ikigaidigital.*Test` (unit tests only, never `*IT`).
- Output formats `HTML,XML`. No `mutationThreshold` yet — step 1 sets it once the
  characterization tests exist. Verify `./mvnw -q -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage`
  runs to completion (score will be poor against the placeholder test; that is expected).
- Add a one-line comment in the pom above the profile: what it is for and how to run it.

### Commit 6 — `build(config): add code sanitation profile`
- OpenRewrite `rewrite-maven-plugin` (current version) with `rewrite-static-analysis` as
  a plugin dependency, in a profile `sanitize`, not active by default.
- `java/rewrite.yml` declares one composite recipe `org.ikigaidigital.Sanitize` with
  **static-analysis recipes only** (no formatting/style recipes — Spotless owns
  formatting). Include at least: remove unused imports, unused local variables, unused
  private methods and fields, unnecessary parentheses/semicolons/casts, and
  `CommonStaticAnalysis` if it does not fight google-java-format on a dry run.
  Verify recipe names exist in the version you pinned.
- `./mvnw -q -Psanitize rewrite:dryRun -Drewrite.failOnDryRunResults=true` is the check;
  `./mvnw -q -Psanitize rewrite:run` then `spotless:apply` is the fix. Both must work.
- **Stop rule:** if this is not working after three focused attempts (≈30 min), drop the
  commit entirely (no half-configured plugin in the pom), say so in the report, and continue.

### Commit 7 — `chore(ai): add agent instructions and rules`
- `AGENTS.md` is the single source of truth (tool-agnostic). `CLAUDE.md` contains exactly
  one line: `@AGENTS.md`.
- `AGENTS.md` (≤ 140 lines, imperative, no prose paragraphs longer than two sentences):
  1. What this repository is (one paragraph) and where the assignment lives.
  2. **Hard constraints** — §0 items 1–6 verbatim in spirit.
  3. **Architecture map** — the package table from §3 below and the dependency rule.
  4. **Workflow for any change**: read the relevant `.claude/rules/*.md`; make the change;
     `./mvnw spotless:apply`; `/verify`; `/commit`; push only after `/verify` is green;
     never amend or force; write/append a report if asked; on any guard-rail conflict
     stop and ask.
  5. **Where things live**: contract `java/src/main/resources/openapi/openapi.yaml`;
     migrations `java/src/main/resources/db/migration/`; seed data
     `java/src/main/resources/db/seed/` (profile `local` only); docs in `docs/`;
     assumptions register and comment convention.
  6. **Definition of done** (applies to every task, step and later change):
     a task is finished only when **all** of the following hold at `HEAD`:
     - every planned commit exists and each one was committed only after a green
       `./mvnw -q verify` (so every commit in history builds and passes);
     - `/verify` is fully green: Spotless check, build, unit and integration tests,
       sanitation dry run, `scripts/guardrails.sh`;
     - no test was deleted, weakened, `@Disabled`, or had its expected values changed to
       make it pass; no coverage/mutation threshold was lowered;
     - once a CI workflow exists (`.github/workflows/`): the agent pushes the branch and
       the latest run for that commit is green (`gh run watch` / `gh run list`); it fixes
       forward with new commits until it is green;
     - the report for the step is written.
     A red check is never "noted for later": it is fixed before the task is declared done.
     If it cannot be fixed within the task's scope, the agent stops and reports — it does
     not declare done.
  7. Index of the rules files and the skills.
- `.claude/rules/` — five files, each a bullet list, each bullet one enforceable statement:

  **architecture.md**
  - Hexagonal: `domain` ← `application` ← `adapter.*`, wired in `config`. Dependencies
    point inward only; adapters never depend on each other.
  - `domain` imports only the JDK: no Spring, JPA, Jackson, Lombok, validation or
    logging frameworks. ArchUnit enforces this with no exceptions.
  - `application` imports the JDK, `domain` and `org.springframework.transaction..`
    only. ArchUnit forbids `jakarta.persistence..`, `org.springframework.data..`,
    `org.springframework.web..`, Jackson, `adapter..` and `config..` there — a JPA
    entity or a web DTO must never reach a use case.
  - Inbound adapters call use-case interfaces (`application.port.in`); outbound adapters
    implement ports (`application.port.out`). Ports are named by intent, not technology.
  - `TimeDeposit` stays a plain domain class in `org.ikigaidigital`; persistence uses a
    separate `TimeDepositEntity` mapped at the adapter boundary. Same for withdrawals.
  - Transactions are declared with `@Transactional` on use-case services (ASSUMPTION A2);
    nowhere else except adapters when a local transaction is unavoidable.
  - Plans are a composition of rules in a catalogue, not branches in the calculator. Adding
    a plan is a catalogue entry plus tests. No speculative rule families: build the seam,
    not the feature.
  - No cycles between packages (ArchUnit).

  **java-style.md**
  - Formatting is google-java-format via Spotless; run `spotless:apply` before every commit.
  - Names reveal intent and reuse the README's domain words (`planType`, `days`, `balance`,
    `withdrawals`). No abbreviations (`xs`, `a2d`, `tmp`, `res`), no `Impl`, `Helper`,
    `Util(s)`, `Manager` suffixes. Adapters are named by role or technology
    (`TimeDepositPersistenceAdapter`, `TimeDepositsController`).
  - Comments say *why* or cite an assumption id; never narrate *what* the code does. No
    Javadoc on self-explanatory members. No commented-out code, no `TODO`/`FIXME`, no
    `@author`, no "generated by" banners, no `System.out`.
  - Prefer records for values, immutable fields, `final` locals where it aids reading,
    small classes with one reason to change (SOLID), composition over inheritance,
    `Optional` only as a return type.
  - Lombok only in `adapter.*` and `config`, for boilerplate a reader would skip anyway
    (`@RequiredArgsConstructor`, `@Getter`, JPA's `@NoArgsConstructor`). Never `@Data`,
    never Lombok-generated equals/hashCode on JPA entities, never in `domain`/`application`
    (records and plain classes there). No MapStruct: mapping is explicit and tested.
  - Money crosses boundaries as `BigDecimal` (scale 2); the frozen `Double` in
    `TimeDeposit` is converted with `BigDecimal.valueOf` at the adapter boundary only.
  - Delete code the moment it is unused. A `rewrite:dryRun` with findings fails `/verify`.

  **spring.md**
  - Constructor injection only; no `@Autowired` anywhere; no field or setter injection.
  - No stereotype annotations in `domain`/`application`; beans for use cases, the
    calculator and ports are declared in `config` `@Configuration` classes.
  - Controllers are thin: map request → call one use case → map response. No logic.
  - Controllers implement the interface generated from the OpenAPI contract; hand-written
    `@RequestMapping`/`@GetMapping`/… annotations are not allowed in `src/main`.
  - Configuration is externalised in `application.yml`; profile `local` adds seed data and
    local DB settings; tests use Testcontainers, never H2 or an embedded database.
  - Flyway owns the schema; `spring.jpa.hibernate.ddl-auto=validate`.
  - Repositories/JPA entities live only in `adapter.out.persistence`; entities map foreign
    keys as plain columns (`timeDepositId`), **no `@OneToMany`/`@ManyToOne` navigation** —
    referential integrity is the database's job (FK constraints and indexes in migrations).
  - Exceptions: handling invalid input is out of scope (README); no global exception
    handler is added.

  **testing.md**
  - Pyramid: characterization tests of the original calculator (pure JUnit, no Spring) →
    domain unit tests → application tests with in-memory fakes (no Mockito in domain or
    application tests) → `@DataJpaTest` + Testcontainers for the persistence adapter →
    `@SpringBootTest` + Testcontainers for the endpoints → ArchUnit → mutation testing
    for `domain`+`application`.
  - Anything that touches PostgreSQL or HTTP runs against a real PostgreSQL container
    (Testcontainers, singleton container reused across classes). Never H2.
  - Unit tests are `*Test` (Surefire), integration tests are `*IT` (Failsafe).
  - Test names state behaviour in domain words, e.g.
    `creditsNoInterestWithinFirstThirtyDays`; parameterized tests for boundaries.
  - Expected values are literals derived from the original implementation, never
    recomputed with the formula under test.
  - Characterization tests are frozen once committed: a later step may add tests, never
    edit or delete them. Protected by hash in `scripts/guardrails.sh`.
  - AssertJ for assertions; one behaviour per test; no logic in tests.

  **git.md** — the commit convention from §4 **verbatim** (drop only the first line
  "Applies to every commit…" if you prefer a heading).

### Commit 8 — `chore(ai): add commit, verify, review and extend skills`
`.claude/skills/<name>/SKILL.md`, each ≤ 60 lines:
- **verify**: `cd java && ./mvnw -q spotless:check verify`, then
  `./mvnw -q -Psanitize rewrite:dryRun -Drewrite.failOnDryRunResults=true` (if commit 6
  exists), then `scripts/guardrails.sh`. Report each result; any failure = stop, fix, rerun.
- **commit**: refuse if the working tree has unrelated changes; run `/verify`; show
  `git diff --cached --stat`; confirm the staged diff is one logical change in one scope;
  propose a message matching the convention; **stop and wait for the developer's
  approval**; then `git commit -m "<message>"` with a single `-m` and nothing else. Never
  amend. Pushing is a separate, explicit action taken only after `/verify` is green.
- **review**: an independent review checklist to run on a diff or the whole repo:
  (1) requirement traceability — every README requirement → code → test; (2) hard
  constraints intact; (3) dead code, unused members, speculative abstractions;
  (4) comment and naming audit against `java-style.md`; (5) layering against
  `architecture.md`; (6) every `// ASSUMPTION` has a register row and vice-versa;
  (7) tests assert behaviour, not implementation. Output: findings table
  `severity | file:line | finding | suggested fix`, no code changes.
- **extend** (used for every new requirement): (1) restate the requirement
  as one business rule in the README's vocabulary and name the rule family (interest,
  lifecycle, persistence, API); (2) list which hard constraints it touches; (3) **design
  gate** — if it adds a rule family, changes a documented assumption or has more than one
  reasonable placement, present at least two designs with trade-offs and **stop for the
  developer's choice**; (4) list files to change in order: production → tests → migration /
  seed / contract → docs/assumptions; (5) implement; (6) `/verify`; never weaken a frozen
  test; (7) `/commit`. Written generically: do not name any hypothetical future plan.

### Commit 9 — `chore(ai): add hook denying edits to protected files`
- `.claude/protected-files.txt` — one repo-relative path per line, `#` comments allowed:
  ```
  # Frozen by the assignment: no breaking changes to the shared class
  java/src/main/java/org/ikigaidigital/TimeDeposit.java
  # The assignment text itself
  docs/ASSIGNMENT.md
  ```
- `.claude/hooks/protect-files.sh` (bash + python3 for JSON; no jq): reads the hook JSON
  from stdin; for `Edit|Write|MultiEdit|NotebookEdit` compares `tool_input.file_path`
  (normalised to repo-relative) against the list; for `Bash` denies when the command
  mentions a protected path or basename **and** contains a mutating token
  (`>`, `>>`, `sed -i`, `tee`, `rm `, `mv `, `cp `, `truncate`, `git mv`, `git rm`,
  `perl -i`). On a match: print `BLOCKED: <path> is protected (see .claude/protected-files.txt)`
  to stderr and `exit 2`. Otherwise `exit 0`. Read-only Bash use (`cat`, `git diff`,
  `shasum`) stays allowed. Document the heuristic in a 3-line header comment.
- `.claude/settings.json`:
  - `hooks.PreToolUse`: matcher `Edit|Write|MultiEdit|NotebookEdit|Bash` → the script,
    invoked as `"$CLAUDE_PROJECT_DIR"/.claude/hooks/protect-files.sh`.
  - `permissions.deny`: `Bash(git push --force:*)`, `Bash(git push -f:*)`,
    `Bash(git commit --amend:*)`, `Bash(git rebase:*)`, `Bash(git reset --hard:*)`,
    `Bash(git filter-branch:*)`.
  - `permissions.allow`: `Bash(./mvnw:*)`, `Bash(cd java && ./mvnw:*)`, `Bash(git status:*)`,
    `Bash(git diff:*)`, `Bash(git log:*)`, `Bash(git add:*)`, `Bash(git commit -m:*)`,
    `Bash(scripts/guardrails.sh:*)`, `Bash(shasum:*)`.
- Test the script by piping at least four JSON samples (protected edit → exit 2;
  unprotected edit → 0; `cat` of a protected file → 0; `sed -i` on it → 2). Put the
  samples and results in the report.

### Commit 10 — `chore(repo): add guardrails script and git hooks`
- `scripts/guardrails.sh` — bash, exits non-zero on the first failing check, prints
  `PASS`/`FAIL <check>: <detail>` per check; usable from a clean clone and from CI
  (macOS and Ubuntu: use `shasum -a 256`, `python3`, POSIX grep/awk; no jq).
  Checks, in order:
  1. **Protected files unchanged**: every path in `.claude/protected-files.txt` exists and
     its SHA-256 matches `scripts/protected-files.sha256` (format: `<sha256>  <path>`).
     Generate that file now for the current list. Later steps append lines for newly
     frozen files (e.g. the characterization test).
  2. **Commit messages**: for every commit in `c4ea358..HEAD` (upstream base; make the
     base overridable with `GUARDRAILS_BASE`): subject matches
     `^(feat|fix|refactor|test|docs|build|ci|chore)\([a-z]+\): [a-z].{0,70}[^.]$`,
     length ≤ 72, and the body is empty (single line, no trailers).
  3. **Exactly two operations in the contract**: if
     `java/src/main/resources/openapi/openapi.yaml` exists, count HTTP method keys under
     `paths` (python3; use PyYAML if importable, otherwise a regex over
     `^\s{4}(get|post|put|patch|delete|head|options):`) and require exactly 2. Skip with
     `SKIP` when the file does not exist yet.
  4. **No hand-written mapping annotations** in `java/src/main/java`:
     `@(Get|Post|Put|Patch|Delete|Request)Mapping` must not appear.
  5. **Banned patterns** in `java/src/**/*.java`, excluding files listed in
     `scripts/guardrails-exclude.txt` (initially the two original main sources and the
     placeholder test). Patterns live in `scripts/banned-patterns.txt`, one extended
     regex per line, `#` comments allowed; grep `-E -n` case-insensitive where marked.
     Initial content:
     ```
     # narrating comments
     ^\s*//\s*(import|create|initialize|initialise|return|set|get|loop|check|call|define|declare|instantiate|iterate|increment)\b
     \bThis (method|class|function|constructor|test) 
     \b(TODO|FIXME|XXX)\b
     @author
     [Gg]enerated by
     # debugging and smells
     System\.(out|err)\.print
     \bclass \w+(Impl|Helper|Utils?|Manager)\b
     \b(xs|a2d|tmp|obj|ret|res)\b
     # forbidden frameworks and styles
     @Data\b
     @Autowired
     ```
  6. **No embedded database**: `java/pom.xml` contains no `h2` or `hsqldb` artifact.
  7. **Assumption ids consistent**: every `ASSUMPTION A<n>` referenced in `java/src`
     has a row `| A<n> |` in `docs/ASSUMPTIONS.md` (FAIL otherwise); every row id that
     is referenced nowhere in `java/src` is printed as `WARN unreferenced A<n>` (not a
     failure — the review step decides).
  8. **Extra patterns (private)**: if `GUARDRAILS_EXTRA_PATTERNS` points to a readable
     file, grep all tracked files (`git ls-files`) with its regexes; any hit fails. Not
     set in CI; the developer sets it locally.
  Make the script executable; add a 5-line usage header.
- `.githooks/commit-msg`: validates the message with the same regex/length/single-line
  rules (bash + grep; print the offending rule). `.githooks/pre-commit`: runs
  `cd java && ./mvnw -q spotless:check` on staged `.java` files only (skip when none).
- `scripts/setup-hooks.sh`: `git config core.hooksPath .githooks` and `chmod +x`; idempotent.
  Run it now. Test `commit-msg` with a bad and a good message via `git commit --dry-run`
  or by invoking the hook with a temp file; record in the report.
- `scripts/guardrails.sh` must pass on the repository as left by this step.

## 3. Architecture map (goes into AGENTS.md and architecture.md)

| package | content |
|---|---|
| `org.ikigaidigital` | `TimeDeposit` (frozen), `TimeDepositCalculator` (same public API), Spring Boot application class |
| `org.ikigaidigital.domain` | `Withdrawal`, read models, `plan/` (interest policy, rate, earning period, plan catalogue) |
| `org.ikigaidigital.application.port.in` | use-case interfaces |
| `org.ikigaidigital.application.port.out` | `TimeDepositRepository` |
| `org.ikigaidigital.application.service` | use-case implementations, framework-free |
| `org.ikigaidigital.adapter.in.web` | controller implementing the generated API interface, DTO mappers |
| `org.ikigaidigital.adapter.out.persistence` | JPA entities, Spring Data repositories, persistence adapter |
| `org.ikigaidigital.config` | `@Configuration` wiring: use cases, calculator |

Dependency rule: `adapter.*` → `application` → `domain`; `config` → everything; nothing → `config` or `adapter.*`.

## 4. Commit convention (verbatim source for `.claude/rules/git.md`)

```
# Commit message convention

A commit message is **exactly one line**:

    <type>(<scope>): <summary>

- No body, no blank line, no trailers (no `Co-Authored-By`, no `Signed-off-by`), no issue references.
- Create it with a single `-m`: `git commit -m "<type>(<scope>): <summary>"`.

## Rules

1. **type** is required and must be one of:

   | type | use for |
   |---|---|
   | `feat` | new behaviour visible to a user of the API or the domain |
   | `fix` | correcting wrong behaviour |
   | `refactor` | structure changes with identical behaviour |
   | `test` | adding or changing tests only |
   | `docs` | documentation only (README, design notes, AI write-up) |
   | `build` | build system and dependencies (`pom.xml`, Maven wrapper, Dockerfile, compose) |
   | `ci` | CI pipeline (`.github/workflows`) |
   | `chore` | housekeeping that touches no production code or behaviour (repo cleanup, ignore files, AI harness config) |

2. **scope** is required, lowercase, and must be one of:

   | scope | covers |
   |---|---|
   | `domain` | `TimeDeposit`, `TimeDepositCalculator`, plans, interest and lifecycle rules |
   | `app` | use cases / application services and ports |
   | `api` | OpenAPI contract and REST adapter |
   | `persistence` | JPA entities, repositories, persistence adapter |
   | `db` | schema migrations and seed data |
   | `config` | Spring configuration and wiring; build configuration |
   | `ai` | AI harness: `CLAUDE.md`, `AGENTS.md`, `.claude/**` |
   | `repo` | repository-level files: `.gitignore`, `scripts/`, `.githooks/`, `docs/`, root layout |

   If a change genuinely needs a new scope, use one short lowercase noun and reuse it consistently afterwards.

3. **summary**
   - imperative mood: "add", "remove", "extract", not "added" or "adds"
   - starts with a lowercase letter
   - no trailing period
   - the whole line is **≤ 72 characters**
   - says *what* changes in domain terms; it does not describe how the work was done or which tool did it

4. **Atomicity**
   - one logical change per commit
   - the project builds and **all tests pass at every commit**
   - don't mix a refactor with a behaviour change
   - don't mix production code from different scopes unless the change can't be split
   - tests that protect existing behaviour (characterization tests) are committed **before** the refactor they protect

5. Agents may push the current branch after `/verify` is green. Nobody rewrites published history (`--amend`, rebase, reset, force-push).

## Examples

Valid:

    chore(repo): remove unused language starters
    test(domain): add characterization tests for interest calculation
    refactor(domain): extract interest rule per plan type
    feat(app): add use cases to list deposits and update balances
    build(config): add Spring Boot and PostgreSQL dependencies
    feat(db): add schema for time deposits and withdrawals
    feat(persistence): add JPA adapter with Testcontainers tests
    feat(api): add OpenAPI contract for the two endpoints
    ci(repo): run build and tests on push
    docs(repo): document how to run and call the endpoints

Invalid:

    Remove unused language starters          # missing type and scope
    chore: remove unused language starters   # missing scope
    chore(Repo): Remove unused starters.     # uppercase scope, capitalised summary, trailing period
    feat(api): added endpoints               # not imperative
    fix(domain): fix bug                     # summary says nothing
    feat(domain): add new rule and refactor calculator and update README   # not atomic

## Checklist before committing

- [ ] `./mvnw -q verify` passes
- [ ] the staged diff is one logical change (`git diff --cached --stat`)
- [ ] the message matches `^(feat|fix|refactor|test|docs|build|ci|chore)\([a-z]+\): [a-z].{0,70}[^.]$` and is ≤ 72 characters
- [ ] show the proposed message to the developer and wait for approval before `git commit`
```

## 5. What you must NOT do in this step

- No Spring, database, Testcontainers, OpenAPI or ArchUnit dependencies yet (they arrive
  with the code that needs them).
- No changes under `java/src/` at all. No new tests. No refactoring.
- No CI workflow, Dockerfile or compose (later steps).
- No files under `prompts/` other than your report (the directory is private and ignored).
- No mention anywhere in committed files of other repositories or of plan types the
  assignment does not define.

## 6. Verification before you finish

Run and paste the outcomes into the report:
1. `git log --oneline c4ea358..HEAD` — the ten (or nine, if commit 6 was dropped) messages
   exactly as specified.
2. `cd java && ./mvnw -q verify` — green.
3. `./mvnw -q -Pmutation …mutationCoverage` — runs to completion.
4. Sanitation dry run — green (or dropped, stated).
5. `scripts/guardrails.sh` — all `PASS`/`SKIP`.
6. Hook script samples (§ commit 9) — expected exit codes.
7. `git status --porcelain` — empty (except `prompts/`, which is untracked by design).
8. `git diff c4ea358 -- java/src/main/java/org/ikigaidigital/TimeDeposit.java` — empty.

You are done only when every item above is green (or explicitly `SKIP`/dropped as this
prompt allows) **and** the report is written. This is the definition of done you are also
writing into `AGENTS.md`; this step is held to it too.

## 7. Report

Write `prompts/reports/00-harness.md`: commits made (hash + message); every version you
pinned and why; the verification outcomes above; anything you dropped or deviated from and
why; open questions for the developer. Do not commit it.

## 8. Working style

- Work commit by commit; do not batch. Before each `git commit`, show the staged stat and
  the message, then commit (the developer has pre-approved the messages in §2).
- Prefer the smallest correct configuration; no plugin options you cannot explain in one
  sentence. Pom comments only where a reader would otherwise ask "why".
- If anything here is impossible or contradictory in your environment, stop, explain and
  wait rather than improvising a workaround.
