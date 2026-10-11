# Step 10 — Final fixes: assumptions, scale note, add-plan skill, two tests

You work in this repository at its root. Read `AGENTS.md` (imports the rules), `docs/ASSIGNMENT.md`,
`docs/ASSUMPTIONS.md`, `docs/ARCHITECTURE.md` and `.claude/skills/extend/SKILL.md` first. The
solution is complete and CI is green; this step closes documentation gaps found in a final read
and adds one skill and two tests. No behaviour changes. Frozen files stay frozen. Commit
granularity: one commit per scope, listed below; `/verify` before each commit; push at the end
and watch CI.

Writing rule for every assumption row and comment: fact first, hypothetical second; short
sentences; no "would be added" phrasing that can be read as "is added".

## 1. Register and comments (`docs/ASSUMPTIONS.md` + cited code)

### A7 — strengthen (row + comment on `AnnualRate.monthlyInterestOn`)
Decision text: "One call applies one month of interest, `annualRate / 12`, whatever the value of
`days`: `days` gates which plans earn, it does not scale the amount. The method does not advance
`days` and does not know the calendar. The service assumes it is invoked once per period by the
scheduler that owns the calendar (the month-end run); a skipped period is not caught up, because
`days` is an age, not a date." Comment on `AnnualRate.monthlyInterestOn`: one line, "`days` gates,
it does not scale; one call is one month".

### A8 — strengthen (row + comment on `UpdateBalancesService.updateBalances()`)
Add to the decision: "Calling it twice posts two months. Controlling the cadence, and
idempotency across retries, belongs to the caller (the scheduler) or to a period record — not
to a value in the client request, and not to this service in the current scope." Comment on the
service method: `// ASSUMPTION A8: one call posts one month; the month-end scheduler owns the cadence.`

### A19 — reword (row + the comment in `openapi.yaml`)
Comment: "ASSUMPTION A19: the endpoints are open; the assignment names no users, roles or
security. If authentication were required, a bearer token would be validated at the gateway or
by Spring Security, and the scheme declared in the contract." Row decision in the same words.

### A21 — new: numeric precision (row + comment on `TimeDepositMapper.toDomain`)
Requirement quoted: "`balance`: Decimal (required)". Ambiguity: `NUMERIC(19,2)` holds 17
integer digits, a `double` holds about 15–16 significant digits. Decision: "Balances up to 15
significant digits (about 9 999 999 999 999.99) round-trip exactly between the column and the
frozen `Double`; the column keeps the assignment's `Decimal` width, larger balances are outside
this exercise's scope." Comment on `toDomain` where `doubleValue()` is called.

### A22 — new: single transaction and scale (row + comment on `UpdateBalancesService.updateBalances()`)
Requirement quoted: "update the balances of all time deposits in the database". Decision:
"The update runs as one transaction: all deposits are locked (`SELECT … ORDER BY id FOR NO KEY
UPDATE`), credited and saved atomically, so a failure leaves no partial month and concurrent
calls serialise. This bounds the dataset to what one transaction and the JVM heap hold, which is
adequate here." Comment on the method: `// ASSUMPTION A22: one transaction for all deposits; see ARCHITECTURE.md › Scaling the update.`

### A3/A4 consequence — one sentence
In the A7 row's ambiguity column or as a note under the table (whichever reads better): "Through
this API alone `days` never changes, so no deposit crosses day 46 or day 366 unless the stored
`days` changes; the age is maintained by the data owner."

Keep ids stable; never renumber. Every new row must be cited in `java/src` (guard rail).

## 2. `docs/ARCHITECTURE.md`

- Under **Concurrency and idempotency**: add the cadence paragraph (A7/A8 wording above) and the
  `days`-never-changes sentence.
- New section **Scaling the update** (after Concurrency): today's bound (A22); at scale the
  operation becomes an asynchronous job — `POST` answers `202 Accepted` with a job id; a worker
  processes deposits in keyset batches (`WHERE id > :lastId ORDER BY id LIMIT n`), one transaction
  per batch; a `balance_postings(time_deposit_id, period)` table with a unique constraint makes
  every batch idempotent so a failed or retried batch never double-credits — and is where
  run-once-per-period idempotency would live; progress exposed through a job resource. Batching by
  page number is rejected because the rows being updated change the result set between pages.
  State plainly that this would add endpoints and a table, both outside today's scope (A1, A22).
  Adding tables is permitted by the assignment; changing `TimeDeposit` is not.
- Under **Money**: one line on A21.
- Under **Adding a plan**: reference the new `/add-plan` skill as the executable form of the list.

## 3. README.md

- API table, `POST` row: "Posts one month of interest to every eligible deposit and returns the
  updated deposits. Two calls post two months: the caller is the month-end scheduler
  (Architecture › Concurrency and idempotency)."
- Development section: list `/add-plan` next to `/extend` with one line.
- **Remove the pre-built image option.** Delete the paragraph that offers
  `docker pull ghcr.io/…` / `docker login ghcr.io` as an alternative, and any `image:` hint or
  comment in `compose.yaml` that points at the registry. The only documented way to run is
  the local build: `docker compose up --build` (and `docker compose up -d postgres` +
  `./mvnw spring-boot:run` for development). The registry image remains a CI artefact: keep
  one sentence in the CI/Development part saying the pipeline publishes an image to GitHub
  Packages on every push to `main` and on `v*` tags, with no instructions to pull it. A
  reader must never be told to log in anywhere to run the solution.

## 4. New skill `.claude/skills/add-plan/SKILL.md` (≤ 70 lines)

Purpose: add a plan type whose rule is a fixed annual rate over an earning period — the recipe
from `ARCHITECTURE.md › Adding a plan`, executable. Steps:

1. Inputs to collect from the developer: code (lowercase), annual rate, earning period
   (`from(d)`, `after(d)` or `between(a, b)`), and whether anything else than interest changes.
   **If anything else changes (a rule that is not a fixed-rate interest, an end-of-life rule, a
   rule that reads withdrawals), stop and run `/extend`'s design gate instead.**
2. `PlanType`: new constant with its code.
3. `PlanCatalog.standard()`: the entry.
4. Migration `V<next>__allow_<code>_plan_type.sql`: `ALTER TABLE time_deposits DROP CONSTRAINT time_deposits_plan_type_check; ALTER TABLE time_deposits ADD CONSTRAINT time_deposits_plan_type_check CHECK (plan_type IN (...all codes...));`
   (constraint name as PostgreSQL generated it for the inline CHECK in V1).
5. `openapi.yaml`: the code in the `planType` enum; regenerate (`./mvnw -q generate-sources`).
6. Tests, in this order: `PlanTypeTest` (code maps), `PlanCatalogTest` (entry present with rate
   and period), a new test class `<Code>PlanInterestTest` in `org.ikigaidigital` with literal
   expected balances obtained by running the calculator once and reading the printed results
   (never the formula; the characterization tests are frozen and must not be touched),
   `TimeDepositPersistenceAdapterIT` row for the constraint accepting the code,
   `PlanTypeContractTest` passing again.
7. Seed (`R__sample_deposits.sql`): optional one row, only if the developer wants it visible.
8. `docs/ASSUMPTIONS.md`: a row only if the requirement's wording needed a reading (boundary
   day, "from day N" inclusive/exclusive); cite it in `PlanCatalog.standard()`.
9. `/verify`; the mutation threshold must still pass; `/commit` as
   `feat(domain): add the <code> plan` + `feat(db): allow the <code> plan type` + `feat(api): add the <code> plan to the contract`
   (or one commit if the developer prefers; say so).

**Prove the skill** before committing it: on a throwaway branch (`git switch -c scratch/add-plan`),
follow the skill literally for an invented plan (choose a neutral code such as `flex`, 2 %,
`after(60)`), run `/verify`, confirm every step was sufficient and nothing was missing; record
the proof (files touched, test counts, verify output) in the report; then `git switch main` and
delete the scratch branch. Nothing from the scratch branch is committed to `main`. If a step
was missing, fix the skill text, not the proof.

## 5. Two tests

- `TimeDepositsApiIT`: `rendersALargeBalanceInPlainDecimalNotation` — insert a deposit with
  `12000000.00` (choose a plan and age that keeps it unchanged, e.g. premium at 20 days), `GET`,
  assert the raw response body contains the literal `12000000.00` and not `E`-notation (string
  assertion on the body, in addition to the `decimalValue()` comparison).
- New `SampleDataIT` (Failsafe, `@SpringBootTest`, `@ActiveProfiles("local")`, shared container):
  the four sample deposits and three withdrawals are present with their ids and balances; after
  that, an `INSERT` into `withdrawals` **without** an id succeeds (the sequence was moved past the
  explicit ids). Make sure it cleans up and does not disturb the other ITs (own context/profile).

## Commits (order; each after a green `/verify`)

1. `docs(repo): state cadence, precision and transaction scope assumptions` — ASSUMPTIONS rows
   A7, A8, A19, A21, A22, the days note; ARCHITECTURE sections; README lines.
   If the registry removal touches `compose.yaml`, that part is its own commit:
   `build(config): run only from the local image build`; the README part is
   `docs(repo): document the local build as the only way to run`.
2. `docs(domain): cite the cadence and precision assumptions in code` — comments on
   `AnnualRate`, `TimeDepositMapper`.
3. `docs(app): cite the cadence and transaction assumptions in the use case` — comments on
   `UpdateBalancesService`.
4. `docs(api): state that the endpoints are open` — the A19 comment in `openapi.yaml`.
   (If commits 2–4 feel like splitting hairs, fold 2–4 into one `docs(repo)` commit with the
   comments and say so in the report — the convention allows docs-only changes across files.)
5. `chore(ai): add the add-plan skill` — the skill, proven on the scratch branch.
6. `test(api): render large balances in plain notation and load the sample data` — both tests.

## Verification before you finish

1. `/verify` green; `scripts/guardrails.sh` shows PASS for assumptions with no WARN; also green
   with `GUARDRAILS_EXTRA_PATTERNS=<pattern file>`.
2. Failsafe report lists the two new ITs with zero failures.
   `grep -rn 'docker login\|docker pull' README.md compose.yaml docs/` → nothing; the compose
   gate (`up --build -d` → both curls → `down -v`) still passes.
3. Frozen files and `TimeDeposit.java` unchanged; mutation threshold still met.
4. `git push origin main`; `gh run watch --exit-status` green.
5. Report `prompts/reports/10-final-fixes.md`: commits, the add-plan proof, the two new tests'
   output, CI run URL. Append a short "Final fixes" entry to `docs/ai/WORKFLOW-LOG.md` (facts only)
   in commit 1 or as a last `docs(repo)` commit.
