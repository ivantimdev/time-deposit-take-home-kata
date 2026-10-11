# Workflow log

One section per step: what it built, the gate outcome, the review and the fix rounds.
The prompts are in [`prompts/`](prompts/README.md).

From step 1 on, every step went through the same gate, run by the orchestrator and not by the step agent:
clean tree; expected commit prefixes; `TimeDeposit.java` unchanged; `./mvnw spotless:check verify`; sanitation dry run; `scripts/guardrails.sh`; mutation run at the thresholds; frozen tests unchanged; Failsafe reports present (from step 4); the step's own verification list; the step report.
A fresh review agent then ran `/review` on the step's diff (every first round and some fix rounds), and the developer decided on each finding.
From step 3 on, the developer allowed one fix round per step for minor findings; minor findings not selected are tracked for the final review (step 9).

## Step 0 — harness, build tooling, guard rails

Built: solution README and assumptions register; Maven wrapper; formatting, lint, coverage, mutation and sanitation profiles; agent instructions, rules, skills and the protected-files hook; guard rails script and git hooks.

Gate: run before the orchestrator existed; the developer approved each commit and the report.
Fix rounds: one fix-forward and two follow-up rounds.

- google-java-format 1.37.0 needs Java 21; Spotless on JDK 17 allows at most 1.28.0. The build had looked green only because every existing file was excluded from formatting.
- The composite sanitation recipe contained a recipe that would have changed the calculator's rounding, so it was replaced by an explicit list of 14 recipes; the guard rails script documents its commit-message base; the architecture map states the `@Transactional` exception.
- A misspelled recipe name was skipped silently; the sanitation run now fails on it.

## Step 1 — characterization tests

Built: characterization tests for the calculator, frozen by the hook.

Gate: all checks PASS; 140 behaviour tests and 16 public-API tests; mutation 26/26 killed; the hook blocks edits to both frozen test files.
Review: 0 blocker, 0 major, 6 minor, all deferred.
Developer decisions: mutation and line coverage thresholds stay at 100 %; integration tests do not count towards the mutation score.
Fix rounds: 0.

## Step 2 — domain refactor

Built: interest policies per plan type, a plan catalogue accepted by the calculator, unit tests for plan types, periods and the catalogue.

Gate: all checks PASS; frozen tests unchanged; no plan rate or day count in the calculator; mutation 29/29.
Review: 1 major (a package cycle between the root package and `domain.plan`, which the prompt itself required), 7 minor.
Fix rounds: 4.

- Round 2: the root package's two domain classes and `domain` declared one domain zone in the rules; duplicate plan types rejected; overflowing earning periods rejected; null plan type added to A6; catalogue tests split.
- Round 3: one rule line still contradicted the domain zone (major); reworded.
- Round 4: inverted earning periods rejected; a test pins that a null plan type earns nothing; the rules name exactly `TimeDeposit` and `TimeDepositCalculator`.
- Round 5: one exception type for invalid periods; the domain zone rule merged into one bullet; the mutation threshold comment corrected. Gate only, no review, by the developer's decision.

End state: 209 unit tests, mutation 33/33, line coverage 64/64.

## Step 3 — application layer

Built: withdrawal and time deposit details read model, use cases to list deposits and update balances, ArchUnit tests for layer dependencies.

Gate: all checks PASS; ArchUnit 10/10; application imports only the JDK, the domain zone and `@Transactional`; mutation 38/38.
Review: 0 blocker, 0 major, 6 minor.
Fix rounds: 1.

- Round 2: A9 cited where withdrawals are sorted; one shared test fixture; the in-memory repository rejects saving a deposit it did not load; two reflection tests replaced by an ArchUnit rule that every use-case method is `@Transactional`.

End state: 235 unit tests, ArchUnit 11/11, mutation 38/38, line coverage 82/82.

## Step 4 — Spring Boot, schema, persistence

Built: Spring Boot application and use-case wiring, Flyway schema, sample data for the local profile, JPA entities and the repository adapter.

Gate: all checks PASS; 38 integration tests against PostgreSQL, 0 failures; ArchUnit 11/11 with empty rules no longer allowed; no containers left.
Review: 0 blocker, 1 major (an unused Lombok constructor the sanitation run cannot see), 9 minor.
Fix rounds: 1.

- Round 2: unused constructor removed; schema tests no longer break on a later migration; the guard rails ban Mockito in domain and application tests; one Spring Boot version property for the BOM and the plugin.

End state: 235 unit tests, 39 integration tests.

## Step 5 — API

Built: the OpenAPI contract, server interfaces generated from it, the REST adapter, Swagger UI served as static files, the contract lint in `/verify`.

Gate: all checks PASS; the contract has exactly two operations; Spectral 0 results; 60 integration tests, 0 failures; no unreferenced assumption left.
Two changes beyond the plan: springdoc's UI turned out to need code introspection, and the manual run served the contract with the wrong content type.
Review: 0 blocker, 2 major, 6 minor.
Fix rounds: 1.

- Round 2: only the three Swagger UI assets the page needs are bundled, so the webjar's demo pages answer `404` (major); the README, `AGENTS.md` and `/verify` agree on when Node is needed (major); A1 and A20 rows completed; literal balances for three and five updates; the test executor is closed in `finally`.

End state: 235 unit tests, 66 integration tests.

## Step 6 — Docker

Built: Dockerfile, Compose with PostgreSQL, run instructions.

Gate: all checks PASS; the orchestrator ran `docker compose up --build -d`, both endpoints, Swagger UI and `down -v`; the service runs as a non-root user.
Review: 0 blocker, 0 major, 7 minor.
Fix rounds: 1.

- Round 2: LF line endings forced for scripts and the Maven wrapper; a fixed jar name; run instructions for a fresh setup (background PostgreSQL, readiness line, port note). The compose gate passed again.

## Step 7 — CI

Built: the CI workflow (build, tests, contract lint, sanitation, guard rails, mutation) and the build status badge.

Gate: all GitHub Actions runs green; local checks PASS.
Review: 0 blocker, 2 major, 6 minor.
Fix rounds: 1.

- Round 2: the commit message guard rail skips merge commits, which failed every pull request (major); only the publishing job may write packages (major); no cancelled runs on `main` and tags; pinned runner; job timeouts; artifact uploads fail when empty.

## Step 8 — documentation

Built: architecture page, final assumptions register, published prompts and this log, README, AI-assisted development page.
The ignore rule for the private `prompts/` directory also matched `docs/ai/prompts/`, so the published prompts needed a fix-forward.
`docs/AI-DEVELOPMENT.md` was committed last, after the developer confirmed the tool version, the models and the role statement.

Gate: all checks PASS; docs-only diff; every relative link resolves; every assumption row cited in `java/src`; the compose run instructions re-executed.
Review: 4 blocker, 3 major, minors selected by the developer.
Fix rounds: 1.

- Round 2: the rules, the `/extend` skill and the published prompts no longer name rule families or plans the assignment does not define; the note on how the prompts were published made exact; exact wording on when the branch was pushed and which rounds had a review agent; the lock test stated as a repeatable experiment; replay instructions; factual corrections in the architecture, development and README pages.

## Step 9 — final review

A fresh agent reviewed the whole repository against the assignment, the hard constraints and the rules, with the minor findings deferred from steps 1 to 8 as extra input, and then fixed what the developer selected, in rounds: each round of fixes was re-checked and reviewed again by a fresh agent until a round yielded no fix.
Run with Fable 5.1 in Claude Code.

Checks before the review, all green: the frozen files unchanged; every commit since the fork point builds and passes its unit tests; 235 unit and 66 integration tests; mutation 38/38 at 100 % line coverage; sanitation, Spectral and the guard rails; the Compose run reproduces the README's balances; a plain-Java consumer of `TimeDepositCalculator`, with no Spring on the classpath, prints `1235595.81`.

Review, round 1: 0 blocker, 11 major, 31 minor. The developer marked everything fixable as fix; 6 minors stay as they are (fixture arithmetic and class-name strings in the frozen tests, commit types in published history, the amd64-only image, a null plan type pinned through a custom catalogue only, and a reading already closed).

Fixed in round 1:

- Guard rails that passed when broken: a fully qualified mapping annotation, a banned-pattern scope on a missing directory, and the operation count without a YAML library. The Mockito ban now covers every test.
- Dead code: an entity constructor without a caller, an earning-period factory only tests used, a Testcontainers JUnit integration and an ArchUnit JUnit integration nobody used, a JUnit version override, the default server port.
- Misleading wording: the lock comment said `FOR UPDATE` while Hibernate renders `FOR NO KEY UPDATE`; the architecture page said only the balance column is written while Hibernate's `UPDATE` sets every mapped column.
- Licensing: the licence notices the bundled Swagger UI scripts refer to are now in the jar and tested.
- Names and tests: the generated response models no longer share the domain classes' names; two persistence tests that asserted configuration rather than behaviour are gone; a test ties the contract's `planType` enum to `PlanType`; the withdrawals index is the foreign key alone.
- Docs: `docs/REQUIREMENTS.md` traces every requirement to code and proof; the architecture page states why the thresholds are 100 %, where Lombok is allowed and why the original README moved.

Rounds 2 to 7 reviewed only the previous round's changes, with findings falling from 3 major to none.
Most of them hardened one guard rail, the check that the contract has exactly two operations, against YAML spellings that could hide an operation from a line-based count; it is now paired with `GeneratedApiInterfacesTest`, which counts the operations of the generated interfaces with the build's own parser. The remaining fixes were test literals instead of recomputed balances, and wording.

## Step 10 — final fixes

A fresh agent closed the documentation gaps found in a final read, added one skill and two tests; no behaviour changed.
Run with Fable 5.1 in Claude Code.

The register: A7, A8 and A19 reworded, fact first; A21 (which balances survive the round trip through the frozen `Double`) and A22 (one transaction for all deposits) added and cited in code; a note that `days` never changes through this API.
The architecture page gained the cadence paragraph and a "Scaling the update" section; the README runs the service from the local image build only.
The `/add-plan` skill was proven before it was committed: on a throwaway branch an invented `flex` plan (2 %, from day 61) was added by following the skill literally, with every check green and mutation 37/37; the branch was then deleted and nothing of it reached `main`.
The proof changed the skill in two places: the docs step also names the sentence counting the plans, and the commit split is two commits rather than three, because `PlanTypeContractTest` ties the enum to the contract and the converter ties the persistence test to the enum.
Tests: `TimeDepositsApiIT.rendersALargeBalanceInPlainDecimalNotation`, and `SampleDataIT` (four tests, the `local` profile, its own database on the shared container because the seed is a repeatable migration recorded in Flyway's history).
After the step: 241 unit and 67 integration tests, 0 failures; mutation 37/37, line coverage 81/81; the Compose run reproduces the README's balances.
