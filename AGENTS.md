# Agent instructions

## 1. Repository

This is a solution to the XA Bank time deposit refactoring kata: a Spring Boot / PostgreSQL service with two endpoints, built as a hexagonal refactoring of the original `TimeDepositCalculator`.
The assignment is in `docs/ASSIGNMENT.md`; read it before any change.
The Maven project lives in `java/`; run every Maven command through the wrapper (`cd java && ./mvnw …`).

## 2. Hard constraints

1. `java/src/main/java/org/ikigaidigital/TimeDeposit.java` is frozen: never edit, move, rename, reformat or annotate it.
2. `org.ikigaidigital.TimeDepositCalculator` keeps its package, its public no-arg constructor, `public void updateBalance(List<TimeDeposit>)` and its observable behaviour.
3. The solution has exactly two API endpoints: update all balances and list all deposits. Swagger UI and the served contract are documentation (ASSUMPTION A1).
4. `docs/ASSIGNMENT.md` is the assignment text, verbatim and frozen.
5. Resolve ambiguity with a stated assumption: a row in `docs/ASSUMPTIONS.md` and a `// ASSUMPTION A<n>: <one line>` comment where it applies.
6. Push the current branch only after `/verify` is green. Never `--amend`, rebase, reset or force-push.

If an instruction conflicts with these constraints, stop and report instead of working around it.

## 3. Architecture map

| package | content |
|---|---|
| `org.ikigaidigital` | `TimeDeposit` (frozen), `TimeDepositCalculator` (same public API), Spring Boot application class |
| `org.ikigaidigital.domain` | `Withdrawal`, read models, `plan/` (interest policy, rate, earning period, plan catalogue) |
| `org.ikigaidigital.application.port.in` | use-case interfaces |
| `org.ikigaidigital.application.port.out` | `TimeDepositRepository` |
| `org.ikigaidigital.application.service` | use-case implementations; framework-free except `@Transactional` (ASSUMPTION A2) |
| `org.ikigaidigital.adapter.in.web` | controller implementing the generated API interface, DTO mappers |
| `org.ikigaidigital.adapter.out.persistence` | JPA entities, Spring Data repositories, persistence adapter |
| `org.ikigaidigital.config` | `@Configuration` wiring: use cases, calculator |

Dependency rule: `adapter.*` → `application` → `domain`; `config` → everything; nothing → `config` or `adapter.*`.
`TimeDeposit` and `TimeDepositCalculator` in the root package belong to the domain zone together with `domain` (see `.claude/rules/architecture.md`).

## 4. Workflow for any change

1. Read the `.claude/rules/*.md` files relevant to the change.
2. Make the change.
3. Run `cd java && ./mvnw spotless:apply`.
4. Run `/verify`; fix every failure and rerun until green.
5. Run `/commit`; one logical change per commit.
6. Push only after `/verify` is green; never amend or force.
7. Write or append the step report when asked to.
8. On any conflict with a guard rail, stop and ask the developer.

## 5. Where things live

- OpenAPI contract: `java/src/main/resources/openapi/openapi.yaml`.
- Schema migrations: `java/src/main/resources/db/migration/`.
- Seed data: `java/src/main/resources/db/seed/`, loaded only with profile `local`.
- Documentation: `docs/`.
- Assumptions: `docs/ASSUMPTIONS.md`; cite them in code as `// ASSUMPTION A<n>: <one line>`. Ids are never reused or renumbered.
- Guard rails: `scripts/guardrails.sh`, protected files in `.claude/protected-files.txt` with hashes in `scripts/protected-files.sha256`.

## 6. Definition of done

A task, step or later change is finished only when all of the following hold at `HEAD`:

- every planned commit exists and each was committed only after a green `./mvnw -q verify`, so every commit in history builds and passes;
- `/verify` is fully green: Spotless check, build, unit and integration tests, sanitation dry run, `scripts/guardrails.sh`;
- no test was deleted, weakened, `@Disabled` or had its expected values changed to make it pass; no coverage or mutation threshold was lowered;
- once a CI workflow exists in `.github/workflows/`, the branch is pushed and the latest run for that commit is green (`gh run watch`, `gh run list`); fix forward with new commits until it is;
- the report for the step is written.

A red check is never noted for later: fix it before declaring done.
If it cannot be fixed within the task's scope, stop and report; do not declare done.

## 7. Rules and skills

Rules, loaded automatically from `.claude/rules/`:

- `architecture.md` — layering, ports, transactions, plan catalogue.
- `java-style.md` — formatting, naming, comments, Lombok, money.
- `spring.md` — injection, wiring, controllers, configuration, persistence.
- `testing.md` — test pyramid, naming, Testcontainers, frozen characterization tests.
- `git.md` — commit message convention and atomicity.

Skills, in `.claude/skills/`:

- `/verify` — run every check and report each result.
- `/commit` — verify, propose a conventional message, wait for approval, commit.
- `/review` — independent review checklist producing a findings table.
- `/extend` — add a new requirement through a design gate.
