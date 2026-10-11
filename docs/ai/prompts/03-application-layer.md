# Step 3 — Application layer: use cases, ports, architecture tests

Read `AGENTS.md`, `.claude/rules/*.md`, `docs/ASSIGNMENT.md`, `docs/ASSUMPTIONS.md` and the
reports of steps 1–2. No Spring Boot, no database, no web yet — this step is the hexagon's
inside: application services and the ports the adapters will implement in steps 4–5.

## Domain additions (`org.ikigaidigital.domain`)

- `Withdrawal` — record `(int id, int timeDepositId, BigDecimal amount, LocalDate date)`.
- `TimeDepositDetails` — record `(TimeDeposit deposit, List<Withdrawal> withdrawals)`; the
  read model both endpoints return (assignment §1: `id, planType, balance, days, withdrawals`).
  The list is unmodifiable (`List.copyOf`) and ordered by withdrawal id.

## Ports (`org.ikigaidigital.application.port`)

- `in.UpdateBalancesUseCase` — `List<TimeDepositDetails> updateBalances()`.
- `in.ListTimeDepositsUseCase` — `List<TimeDepositDetails> listTimeDeposits()`.
- `out.TimeDepositRepository`:
  - `List<TimeDeposit> loadAllForUpdate()` — all deposits ordered by id, **locked for the
    current transaction** (the adapter decides how; the contract says "no concurrent update
    of the same rows may interleave until the transaction ends");
  - `void saveAll(List<TimeDeposit> deposits)`;
  - `List<TimeDepositDetails> loadAllWithWithdrawals()` — all deposits ordered by id, each with
    its withdrawals ordered by id.
  Javadoc on the port methods only where the contract is not obvious from the name
  (the lock and the ordering qualify).

## Services (`org.ikigaidigital.application.service`)

- `UpdateBalancesService implements UpdateBalancesUseCase` — constructor
  `(TimeDepositRepository, TimeDepositCalculator)`; `@Transactional`:
  `loadAllForUpdate()` → `calculator.updateBalance(deposits)` → `saveAll(deposits)` →
  `return loadAllWithWithdrawals()`. One transaction so concurrent calls serialise on the lock
  and none loses an interest application. Comment `// ASSUMPTION A2` on the annotation and
  `// ASSUMPTION A8` on the return (see below).
- `ListTimeDepositsService implements ListTimeDepositsUseCase` — `@Transactional(readOnly = true)`,
  returns `loadAllWithWithdrawals()`.
- No stereotype annotations; beans are declared in `config` in step 4. The only framework
  import in `application` is `org.springframework.transaction.annotation.Transactional`.

## Assumptions

- A8 — the update endpoint returns the updated deposits in the same shape as the list
  endpoint (so the caller sees the effect without a second call); it is **not idempotent**:
  each call applies one month of interest, which is why it is a `POST` (recorded now,
  applied in the service and in the contract in step 5).
- A9 — both endpoints return deposits ordered by `id` ascending and withdrawals ordered by
  `id` ascending; the assignment gives no order. Applied on the port.
- A10 — concurrency: concurrent balance updates are serialised by locking the rows for the
  duration of the transaction; no idempotency key or period tracking is introduced because
  the assignment's schema has no field for it and `updateBalance` has no notion of time.
  Applied in `UpdateBalancesService`.

## Tests

- `test/.../application/InMemoryTimeDepositRepository` — a fake implementing the port on
  maps; records whether `loadAllForUpdate` was called before `saveAll` (ordering), stores
  copies so the test can assert what was saved. No Mockito.
- `UpdateBalancesServiceTest`: applies interest to every deposit (compare against values
  produced by `new TimeDepositCalculator()` on copies — the calculator is pinned elsewhere,
  so using it as the oracle here is legitimate); saves exactly the loaded instances; returns
  details including withdrawals untouched; empty repository → empty list; the result order
  is by id.
- `ListTimeDepositsServiceTest`: returns what the repository provides, unchanged, ordered.
- `ArchitectureTest` (ArchUnit, `com.tngtech.archunit:archunit-junit5`), one rule per method:
  - the domain zone is `org.ikigaidigital.domain..` plus exactly two root-package classes,
    `TimeDeposit` and `TimeDepositCalculator` (not the whole root package: the Spring Boot
    application class will live there); every class in the zone depends only on `java..`
    and the zone itself;
  - `org.ikigaidigital.application..` depends only on `java..`, the domain packages above,
    itself, and `org.springframework.transaction..`;
  - `..application..` does not depend on `jakarta.persistence..`, `org.springframework.data..`,
    `org.springframework.web..`, `com.fasterxml..`, `..adapter..`, `..config..`;
  - `..adapter.in..` and `..adapter.out..` do not depend on each other;
  - nothing outside `..config..` depends on `..config..`; nothing outside the adapters depends
    on `..adapter..`;
  - no cycles between slices `org.ikigaidigital.(*)..`;
  - `TimeDeposit` carries no annotations;
  - no class outside the domain zone and `..config..` is named in an import of a domain-zone
    class (i.e. the zone never depends on `..application..`, `..adapter..` or the
    application class).
  Packages that do not exist yet (adapters, config) make some rules check zero classes:
  put `archRule.failOnEmptyShould=false` in `src/test/resources/archunit.properties` with a
  one-line comment, rather than weakening the rules.

## Build

`java/pom.xml`: import the Spring Boot BOM (`spring-boot-dependencies`, current 3.5.x) in
`<dependencyManagement>`; add `spring-tx` (compile) and `archunit-junit5` (test). Nothing else.
Pitest: `application` is a target package — the threshold must still pass; raise if higher.

## Commits (each after a green `./mvnw -q verify`; via `/commit`)

1. `build(config): add spring bom, transaction api and archunit`
2. `feat(domain): add withdrawal and time deposit details read model`
3. `feat(app): add use cases to list deposits and update balances` — ports, services,
   A8–A10 rows and comments.
4. `test(app): add use case tests with an in-memory repository`
5. `test(app): add architecture tests for layer dependencies`

## Verification before you finish

1. Frozen tests unchanged (zero diff for the step); `TimeDeposit.java` unchanged.
2. `/verify` green; mutation threshold met; `scripts/guardrails.sh` shows no WARN for A2,
   A8, A9, A10.
3. `grep -rn "^import" java/src/main/java/org/ikigaidigital/application` shows only
   `java.*`, `org.ikigaidigital.*` and `org.springframework.transaction.annotation.Transactional`.
4. Report `prompts/reports/03-application-layer.md`: commits, the port contract as written,
   ArchUnit rules list, mutation score, open questions.
