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

Not started when this log was written. The minor findings deferred from steps 1 to 7 are its input.
