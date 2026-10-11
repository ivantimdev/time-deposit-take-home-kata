# Workflow log

One section per step: the commits, the gate outcome, and the fix rounds.
The prompts are in [`prompts/`](prompts/README.md).

From step 1 on, every step went through the same gate, run by the orchestrator and not by the step agent:
clean tree; expected commit prefixes; `TimeDeposit.java` unchanged; `./mvnw spotless:check verify`; sanitation dry run; `scripts/guardrails.sh`; mutation run at the thresholds; frozen tests unchanged; Failsafe reports present (from step 4); the step's own verification list; the step report.
A fresh review agent then ran `/review` on the step's diff (every first round and some fix rounds), and the developer decided on each finding.
From step 3 on, the developer allowed one fix round per step for minor findings; minor findings not selected are tracked for the final review (step 9).

## Before step 0

Developer commits: `3d0751d chore(repo): remove unused language starters`, `b2b33f5 chore(repo): stop tracking IntelliJ module file, private prompts and local settings`, `93d65c7 chore(repo): remove tracked IntelliJ module file`.

## Step 0 — harness, build tooling, guard rails

Commits:

- `6b65fdc docs(repo): move assignment to docs and add solution readme`
- `9466a03 docs(repo): add assumptions register`
- `979745b build(config): add maven wrapper`
- `81243f1 build(config): add formatting, lint, coverage and test phases`
- `dc8409e build(config): add mutation testing profile`
- `a287313 build(config): pin google-java-format to the last release for java 17`
- `7a5b3c3 build(config): add code sanitation profile`
- `e655db0 chore(ai): add agent instructions and rules`
- `629200f chore(ai): add commit, verify, review and extend skills`
- `971605e chore(ai): add hook denying edits to protected files`
- `7929ce5 chore(repo): add guardrails script and git hooks`
- `da446f8 build(config): replace composite sanitation recipe with explicit list`
- `bc97104 chore(repo): document the commit-message base in the guardrails script`
- `9ce0e1a chore(ai): state the transaction exception in the architecture map`
- `a067a05 build(config): fail the sanitation run on an unknown recipe`

Gate: run before the orchestrator existed; the developer approved each commit and the report.
Fix rounds: one fix-forward and two follow-up rounds.

- `a287313`: google-java-format 1.37.0 needs Java 21; Spotless on JDK 17 allows at most 1.28.0. The build had looked green only because every existing file was excluded from formatting.
- Follow-up 1 (`da446f8`, `bc97104`, `9ce0e1a`): the composite sanitation recipe contained a recipe that would have changed the calculator's rounding, so it was replaced by an explicit list of 14 recipes; the guard rails script documents its commit-message base; the architecture map states the `@Transactional` exception.
- Follow-up 2 (`a067a05`): a misspelled recipe name was skipped silently; the sanitation run now fails on it.

## Step 1 — characterization tests

Commits:

- `59dd77d test(domain): add characterization tests for interest calculation`
- `b9568d1 build(config): apply formatting and sanitation to the calculator tests`
- `13bcc12 build(config): require the mutation score reached by the tests`
- `96f7a68 chore(ai): freeze the characterization tests`

Gate: all checks PASS; 140 behaviour tests and 16 public-API tests; mutation 26/26 killed; the hook blocks edits to both frozen test files.
Review: 0 blocker, 0 major, 6 minor, all deferred.
Developer decisions: mutation and line coverage thresholds stay at 100 %; integration tests do not count towards the mutation score.
Fix rounds: 0.

## Step 2 — domain refactor

Commits, round 1:

- `d9c11be refactor(domain): extract interest policies per plan type`
- `3f61ba3 refactor(domain): accept a plan catalogue in the calculator constructor`
- `b7d578a test(domain): add unit tests for plan types, periods and the catalogue`
- `973f338 build(config): apply formatting and sanitation to the calculator`

Gate: all checks PASS; frozen tests unchanged; no plan rate or day count in the calculator; mutation 29/29.
Review: 1 major (a package cycle between the root package and `domain.plan`, which the prompt itself required), 7 minor.
Fix rounds: 4.

- Round 2 (`b6b54f4`, `be4d46f`, `67e7f43`, `9e6a100`): the root package's two domain classes and `domain` declared one domain zone in the rules; duplicate plan types rejected; overflowing earning periods rejected; null plan type added to A6; catalogue tests split.
- Round 3 (`b0400ed`): one rule line still contradicted the domain zone (major); reworded.
- Round 4 (`284effc`, `2852d94`, `bc70d99`): inverted earning periods rejected; a test pins that a null plan type earns nothing; the rules name exactly `TimeDeposit` and `TimeDepositCalculator`.
- Round 5 (`1c8be6c`, `43a7c6d`, `3874fa3`): one exception type for invalid periods; the domain zone rule merged into one bullet; the mutation threshold comment corrected. Gate only, no review, by the developer's decision.

End state: 209 unit tests, mutation 33/33, line coverage 64/64.

## Step 3 — application layer

Commits, round 1:

- `a0c11b8 build(config): add spring bom, transaction api and archunit`
- `7d8b809 feat(domain): add withdrawal and time deposit details read model`
- `8c9f352 feat(app): add use cases to list deposits and update balances`
- `1fbe0b8 test(app): add architecture tests for layer dependencies`

Gate: all checks PASS; ArchUnit 10/10; application imports only the JDK, the domain zone and `@Transactional`; mutation 38/38.
Review: 0 blocker, 0 major, 6 minor.
Fix rounds: 1.

- Round 2 (`01494c7`, `ee3eb96`): A9 cited where withdrawals are sorted; one shared test fixture; the in-memory repository rejects saving a deposit it did not load; two reflection tests replaced by an ArchUnit rule that every use-case method is `@Transactional`.

End state: 235 unit tests, ArchUnit 11/11, mutation 38/38, line coverage 82/82.

## Step 4 — Spring Boot, schema, persistence

Commits, round 1:

- `aa35bbb build(config): add spring boot, jpa, flyway, postgres and testcontainers`
- `4a13734 feat(config): add spring boot application and use case wiring`
- `5109c24 feat(db): add schema for time deposits and withdrawals`
- `5a819e7 feat(db): add sample deposits for the local profile`
- `67c2075 feat(persistence): add jpa entities and repository adapter`

Gate: all checks PASS; 38 integration tests against PostgreSQL, 0 failures; ArchUnit 11/11 with empty rules no longer allowed; no containers left.
Review: 0 blocker, 1 major (an unused Lombok constructor the sanitation run cannot see), 9 minor.
Fix rounds: 1.

- Round 2 (`0772718`, `a26516a`, `bad526a`, `fdba9ea`): unused constructor removed; schema tests no longer break on a later migration; the guard rails ban Mockito in domain and application tests; one Spring Boot version property for the BOM and the plugin.

End state: 235 unit tests, 39 integration tests.

## Step 5 — API

Commits, round 1:

- `a8ef4a5 feat(api): add openapi contract for the two endpoints`
- `11b3ee8 build(api): generate server interfaces from the contract`
- `101185a build(api): serve swagger ui as static files instead of springdoc`
- `d0a5a97 feat(api): implement rest adapter from the contract`
- `4003dd8 chore(ai): lint the contract in verify`
- `746b4b4 fix(config): serve the contract as application/yaml`

Gate: all checks PASS; the contract has exactly two operations; Spectral 0 results; 60 integration tests, 0 failures; no unreferenced assumption left.
Two commits beyond the plan: springdoc's UI turned out to need code introspection, and the manual run served the contract with the wrong content type.
Review: 0 blocker, 2 major, 6 minor.
Fix rounds: 1.

- Round 2 (`83b73ff`, `01faf13`, `6121b27`, `7d43758`): only the three Swagger UI assets the page needs are bundled, so the webjar's demo pages answer `404` (major); the README, `AGENTS.md` and `/verify` agree on when Node is needed (major); A1 and A20 rows completed; literal balances for three and five updates; the test executor is closed in `finally`.

End state: 235 unit tests, 66 integration tests.

## Step 6 — Docker

Commits, round 1:

- `9f5d5cd build(config): add dockerfile for the service`
- `260119f build(config): add docker compose with postgresql`
- `0a25665 docs(repo): document how to run the service and call the endpoints`

Gate: all checks PASS; the orchestrator ran `docker compose up --build -d`, both endpoints, Swagger UI and `down -v`; the service runs as a non-root user.
Review: 0 blocker, 0 major, 7 minor.
Fix rounds: 1.

- Round 2 (`116e1e8`, `0ade5c8`, `5e8f6a2`): LF line endings forced for scripts and the Maven wrapper; a fixed jar name; run instructions for a fresh setup (background PostgreSQL, readiness line, port note). The compose gate passed again.

## Step 7 — CI

Commits, round 1:

- `2cf5f25 ci(repo): run build, tests, lint, guard rails and mutation on push`
- `365505b ci(repo): publish the container image to github packages`
- `1127cb1 docs(repo): add build status badge and container image link`

Tag `v0.1.0` on `365505b`.
Gate: all GitHub Actions runs green; images present in GitHub Packages; local checks PASS.
Review: 0 blocker, 2 major, 6 minor.
Fix rounds: 1.

- Round 2 (`3633a85`, `5fa475d`, `e49368e`): the commit message guard rail skips merge commits, which failed every pull request (major); only the publishing job may write packages (major); no cancelled runs on `main` and tags; pinned runner; job timeouts; artifact uploads fail when empty; the README says the image pull needs a login while the package is private.

## Step 8 — documentation

Commits, round 1:

- `a73159c docs(repo): describe the architecture and design decisions`
- `6b6c1fb docs(repo): finalize the assumptions register`
- `a1a0ce0 docs(repo): publish the agent prompts and the workflow log`
- `c383e2c docs(repo): add the published prompts hidden by the ignore rule`
- `98c5cb3 docs(repo): complete the readme`
- `25a9a00 docs(repo): describe the ai-assisted development setup`

`c383e2c` is a fix-forward: the ignore rule for the private `prompts/` directory also matched `docs/ai/prompts/`, so `a1a0ce0` held only this log.
Two intermediate commits had a link that resolved only later: this log's link to the prompts at `a1a0ce0`, and the README's link to `docs/AI-DEVELOPMENT.md` at `98c5cb3`.
`docs/AI-DEVELOPMENT.md` was committed last, after the developer confirmed the tool version, the models and the role statement.

Gate: all checks PASS; docs-only diff; every relative link resolves; every assumption row cited in `java/src`; the compose run instructions re-executed.
Review: 4 blocker, 3 major, minors selected by the developer.
Fix rounds: 1.

- Round 2 (`84e5c6c chore(ai): describe rule families without naming one`, then `docs(repo): correct the published prompts and the development docs`): the rules, the `/extend` skill and the published prompts no longer name rule families or plans the assignment does not define; the note on how the prompts were published made exact; exact wording on when the branch was pushed and which rounds had a review agent; the lock test stated as a repeatable experiment; replay instructions; factual corrections in the architecture, development and README pages.

## Step 9 — final review

A fresh agent reviewed the whole repository against the assignment, the hard constraints and the rules, with the minor findings deferred from steps 1 to 8 as extra input, and then fixed what the developer selected, in rounds: each round of fixes was re-checked and reviewed again by a fresh agent until a round yielded no fix.
Run with Fable 5.1 in Claude Code.

Checks before the review, all green: the frozen files unchanged; every one of the 83 commits since the fork point builds and passes its unit tests; 235 unit and 66 integration tests; mutation 38/38 at 100 % line coverage; sanitation, Spectral and the guard rails; the Compose run reproduces the README's balances; a plain-Java consumer of `TimeDepositCalculator`, with no Spring on the classpath, prints `1235595.81`.

Review, round 1: 0 blocker, 11 major, 31 minor. The developer marked everything fixable as fix; 6 minors stay as they are (fixture arithmetic and class-name strings in the frozen tests, commit types in published history, the amd64-only image, a null plan type pinned through a custom catalogue only, and a reading already closed).

Fixed, round 1 (`1c397ca` to `383388b`):

- Guard rails that passed when broken: a fully qualified mapping annotation, a banned-pattern scope on a missing directory, and the operation count without a YAML library. The Mockito ban now covers every test.
- Dead code: an entity constructor without a caller, an earning-period factory only tests used, a Testcontainers JUnit integration and an ArchUnit JUnit integration nobody used, a JUnit version override, the default server port.
- Misleading wording: the lock comment said `FOR UPDATE` while Hibernate renders `FOR NO KEY UPDATE`; the architecture page said only the balance column is written while Hibernate's `UPDATE` sets every mapped column; a Javadoc that repeated the assumption comment under it.
- Licensing: the licence notices the bundled Swagger UI scripts refer to are now in the jar and tested.
- Names and tests: the generated response models no longer share the domain classes' names; two persistence tests that asserted configuration rather than behaviour and a timing log in the endpoint test are gone; a test ties the contract's `planType` enum to `PlanType`; the withdrawals index is the foreign key alone.
- Docs: `docs/REQUIREMENTS.md` traces every requirement to code and proof; the architecture page states why the thresholds are 100 %, where Lombok is allowed and why the original README moved; the published step-9 prompt is the one that ran; Node 22 in CI.

Review, round 2, on the round-1 changes: 0 blocker, 3 major, 8 minor. Fixed (`f98d020` to `07c7773`): the operation counter handles quoted path keys and rejects `$ref` path items; the architecture page no longer says the contract enum is untested; this section; the lock comment names Hibernate as the renderer; the endpoint test drops a path that hard-coded the Swagger UI version; the migration-history test joins the two deleted earlier; `EarningPeriod.after(0)` replaces a repeated sentinel; the Node release is named where the lint runs.
Rejected, with reason: the round-1 commits group several small changes per scope, which is the fix-round rule of this project (one commit per scope per round).

Review, round 3, on the round-2 changes: 1 blocker, 1 major, 6 minor. Fixed (`16697cf` to `e1d4217`): the operation counter rejects every key under `paths` that is not a plain block-mapping key, since a flow mapping, alias, anchor, tag or merge key could hide an operation from the count (blocker); the endpoint tests that recomputed their expectations with the calculator are gone, every balance is a literal from the original (major); the lock comment says why the lock is right; the count of concurrent updates is no longer repeated in a constant; the Node wording says what works and what CI runs; this section names the finding instead of a file outside the repository.
Kept, with reason: `indexesWithdrawalsByDeposit` asserts the index A14 decided on, a schema decision rather than a framework setting.

Review, round 4, on the round-3 changes: 1 blocker, 0 major, 5 minor. Fixed (`f08e283` to `02423bf`): three further YAML spellings of an operation key (a space before the colon, an explicit `?` key, an escape inside a quoted key) slipped past the counter, so it now accepts only plain `/...` path keys, or quoted ones without escapes, and plain lowercase operation keys, and `TimeDepositsApiTest` counts the `time-deposits` operations from the generated interface, with the build's own parser (blocker); the guard rail's comment and message say what it enforces; the current Spectral 6 releases need Node 22, which the three places that name Node now say; the concurrency test no longer asserts its own helper's loop; the lock comment separates the reason for the lock from how Hibernate renders it; this section names the round-3 commits.

Review, round 5, on the round-4 changes: 0 blocker, 2 major, 6 minor. Fixed: a flow sequence or a multi-line quoted value at operation level could make the counter count a line that is not an operation, so such values are rejected, and the comment says what the pair guarantees: the script lets no operation hide, the test gives the exact number (major); the test scans every generated interface, one per contract tag, instead of the one (major); the message says what to write; the requirements matrix, the assumptions register and the architecture page name both enforcement points; the Node notes no longer pin a Spectral patch release the command does not pin; the lock comment says what is not blocked.

Review, round 6, on the round-5 changes: 0 blocker, 0 major, 6 minor, all fixed: the counter strips comments only outside quotes and accepts one-line flow values, so valid path-item shapes such as `parameters: []` are no longer rejected; its comment and message say which spellings it accepts; `TimeDepositsApiTest` is renamed `GeneratedApiInterfacesTest`, after what it scans, with a comment that states only why; the lock comment names the other writer.

Review, round 7, on the round-6 changes: 0 blocker, 0 major, 3 minor, all wording, fixed: the guard rail's comment and message name the shapes it accepts; the comment-stripping helper states its simplification; this entry says which test was renamed.

One commit of round 1, `da69464`, carries the message of the commit before it; it holds the configuration-class alignment and the removal of the default port. History is not rewritten, so the message stays.

## Step 10 — final fixes

A fresh agent closed the documentation gaps found in a final read, added one skill and two tests; no behaviour changed.
Run with Fable 5.1 in Claude Code.

Commits, each after a green `/verify`:

- `d613929 docs(repo): state cadence, precision and transaction scope assumptions`
- `a9e4585 docs(repo): document the local build as the only way to run`
- `d108477 build(config): run only from the local image build`
- `37f87ed docs(repo): cite the strengthened and new assumptions in code`
- `a83b9d9 chore(ai): add the add-plan skill`
- `eedf039 test(api): render large balances plainly and load the sample data`
- this entry and the published step prompt, as the last `docs(repo)` commit

The register: A7, A8 and A19 reworded, fact first; A21 (which balances survive the round trip through the frozen `Double`) and A22 (one transaction for all deposits) added and cited in code; a note that `days` never changes through this API.
The architecture page gained the cadence paragraph and a "Scaling the update" section; the README no longer offers the registry image, which the pipeline still publishes as a build artefact.
The `/add-plan` skill was proven before it was committed: on a throwaway branch an invented `flex` plan (2 %, from day 61) was added by following the skill literally, in two commits, with every check green and mutation 37/37; the branch was then deleted and nothing of it reached `main`.
The proof changed the skill in two places: the docs step also names the sentence counting the plans, and the commit split is two commits rather than three, because `PlanTypeContractTest` ties the enum to the contract and the converter ties the persistence test to the enum.
Tests: `TimeDepositsApiIT.rendersALargeBalanceInPlainDecimalNotation`, and `SampleDataIT` (four tests, the `local` profile, its own database on the shared container because the seed is a repeatable migration recorded in Flyway's history).
After the step: 241 unit and 67 integration tests, 0 failures; mutation 37/37, line coverage 81/81; the Compose run reproduces the README's balances.

Pushed by the step agent after a green `/verify`; the CI run is recorded in the private step report.
Deviation: the four `docker pull` or `docker login` mentions left under `docs/` are in the published prompts of steps 7 and 9, kept as the record of what ran.
