# Step 5 — OpenAPI contract, REST adapter, endpoint tests

Read `AGENTS.md`, `.claude/rules/*.md`, `docs/ASSIGNMENT.md`, `docs/ASSUMPTIONS.md` and the
reports of steps 1–4. Docker must be running (Testcontainers). Node ≥ 18 is needed for the
contract linter (`npx`).

## The two endpoints — and only these two (A1)

| operationId | method + path | response |
|---|---|---|
| `listTimeDeposits` | `GET /api/v1/time-deposits` | `200` — array of `TimeDeposit` |
| `updateBalances` | `POST /api/v1/time-deposits/update-balances` | `200` — array of `TimeDeposit`, the state after the update |

Schemas: `TimeDeposit { id: integer int32, planType: string (enum basic|student|premium),
balance: number, days: integer int32, withdrawals: Withdrawal[] }` — exactly the five fields
the assignment lists, all `required`; `Withdrawal { id: integer int32, amount: number,
date: string format date }` — `timeDepositId` is omitted inside the nesting (A15). Money is
`type: number` with **no `format`** so the generator produces `BigDecimal` (A16).
`additionalProperties: false` on both schemas. Include `description`s, `example`s, one `tag`
(`time-deposits`), `info` with title/version/description, a `servers` entry for
`http://localhost:8080`. OpenAPI **3.0.3** (generator and springdoc support is most mature).

RESTful checklist to apply and record in the report (the assignment asks for REST
explicitly):
- resources are plural nouns in kebab-case; the collection is `/time-deposits`;
- `GET` is safe and idempotent, returns a representation of the collection;
- the balance update is **not idempotent** (each call credits one month), so it cannot be
  `PUT`; it is modelled as a *controller resource* — `POST /time-deposits/update-balances` —
  the one case where REST API design guidance (Massé, *REST API Design Rulebook*, rule on
  controller resources) allows a verb in the path; the alternative noun form
  `/balance-updates` was considered and rejected because no "balance update" resource is
  ever retrievable (A17);
- `POST` answers `200` with the resulting collection rather than `204`, so a client (and a
  reader in Swagger UI) sees the effect without a second request (A8);
- JSON everywhere (`application/json`), camelCase property names exactly as the assignment;
- `/api/v1` prefix for versioning (A18);
- error responses are not described: invalid input handling is out of scope (assignment);
- no authentication: out of scope for the kata, recorded as A19 with what would be added
  (bearer token at the gateway or Spring Security) if it were in scope.

## Contract-first build

- `java/src/main/resources/openapi/openapi.yaml` is the single source. It is also served
  unchanged at `GET /openapi.yaml` (add `classpath:/openapi/` to
  `spring.web.resources.static-locations` **in addition to** the defaults) and Swagger UI
  renders it: `springdoc-openapi-starter-webmvc-ui` (current 2.8.x) with
  `springdoc.api-docs.enabled=false` and `springdoc.swagger-ui.url=/openapi.yaml` — runtime
  introspection off, so the UI shows the contract, not the implementation (A20). Swagger UI
  and `/openapi.yaml` are documentation of the two endpoints, not endpoints (A1).
- `openapi-generator-maven-plugin` (current 7.x), generator `spring`, `interfaceOnly=true`,
  `useSpringBoot3=true`, `useTags=true`, `skipDefaultInterface=true`, `openApiNullable=false`,
  `documentationProvider=none`, `useBeanValidation=false` (no validation framework),
  `apiPackage=org.ikigaidigital.adapter.in.web.api`, `modelPackage=org.ikigaidigital.adapter.in.web.model`,
  output under `target/generated-sources/openapi` (never committed). Exclude the two
  generated packages from JaCoCo and Pitest. Spotless formats `src/` only, so generated code
  is untouched.
- `.spectral.yaml` at the repository root: `extends: ["spectral:oas"]` plus rules
  `operation-operationId`, `operation-description`, `oas3-schema`, and a custom rule that
  `paths` keys are kebab-case. Lint: `npx -y @stoplight/spectral-cli@6 lint java/src/main/resources/openapi/openapi.yaml`
  must report zero errors **and zero warnings**.

## REST adapter (`org.ikigaidigital.adapter.in.web`)

- `TimeDepositsController implements TimeDepositsApi` (`@RestController`; the mapping
  annotations come from the generated interface — none hand-written). Two methods, each:
  call one use case → map → `ResponseEntity.ok(...)`. Constructor injection of the two
  use-case interfaces and the mapper.
- `TimeDepositResponseMapper` (package-private, no Lombok needed): `TimeDepositDetails` →
  generated `TimeDeposit` DTO; `balance` as `BigDecimal.valueOf(double).setScale(2, HALF_UP)`
  (A13 applies at this boundary too: cite it), `planType` as the DTO enum from the code,
  withdrawals mapped in order.

## Tests

`*IT`, `@SpringBootTest(webEnvironment = RANDOM_PORT)`, Testcontainers via the shared base,
real HTTP with `TestRestTemplate` or `RestClient`; each test inserts its own rows through a
small JDBC helper and cleans up (`@Sql` or `JdbcTemplate` in `@BeforeEach`), never via the
`local` seed.

`TimeDepositsApiIT`:
- `GET` on an empty table → `200`, `[]`, `Content-Type: application/json`.
- `GET` with three deposits (one per plan) and withdrawals on two of them → every field of
  every element asserted (`id`, `planType`, `balance` with two decimals, `days`,
  `withdrawals[].id/amount/date`), order by id, withdrawals ordered, empty array for the
  deposit without withdrawals, **no additional properties** (compare the JSON key set).
- `POST update-balances` → `200`, the returned balances equal what
  `new TimeDepositCalculator().updateBalance(copies)` yields for the same inputs (the
  calculator is the pinned oracle), `days` unchanged, withdrawals unchanged, and a subsequent
  `GET` returns exactly the `POST` body.
- Three `POST`s in a row compound (compare against three oracle applications).
- A premium deposit at 30 days and a student at 400 days are returned unchanged by `POST`.
- `POST` on an empty table → `200`, `[]`.
- **Concurrency**: 5 threads fire `POST` simultaneously (`CountDownLatch`, `ExecutorService`);
  all return `200`; afterwards the balance of each deposit equals exactly 5 sequential oracle
  applications — no lost update, no over-application. Document in the report the observed
  wall time and that the pessimistic lock serialised the calls.
- `GET /openapi.yaml` → `200` and byte-equal to the classpath resource; `GET /swagger-ui/index.html`
  → `200`. Any other path (`/api/v1/time-deposits/1`, `/actuator/health`) → `404` (A1).

## Assumptions

A15 (nested withdrawal omits `timeDepositId`), A16 (money as `number` without format →
`BigDecimal`), A17 (controller-resource path), A18 (`/api/v1` prefix), A19 (no
authentication), A20 (Swagger UI serves the static contract). A1 and A8 are now referenced
in code (contract comments count only in `java/src`: cite them in the controller). Keep
the ASSUMPTIONS table rows precise and short.

## Commits (each after a green `./mvnw -q verify`; via `/commit`)

1. `feat(api): add openapi contract for the two endpoints` — `openapi.yaml`, `.spectral.yaml`,
   assumptions rows; lint green.
2. `build(api): generate server interfaces from the contract` — generator plugin, springdoc
   dependency, coverage/mutation exclusions. Build green with the generated interface unused.
3. `feat(api): implement rest adapter from the contract` — controller, mapper, static
   location and springdoc settings in `application.yml`.
4. `test(api): add endpoint and concurrency tests with testcontainers`
5. `chore(ai): lint the contract in verify` — add the Spectral command to
   `.claude/skills/verify/SKILL.md` (step 2, before the sanitation dry run) and one line to
   `.claude/rules/spring.md`: a new operation requires a contract change first and the
   contract has exactly two operations (`scripts/guardrails.sh` enforces it).

## Verification before you finish

1. Frozen tests and `TimeDeposit.java` unchanged; `scripts/guardrails.sh` → `PASS contract-operations`,
   `PASS mapping-annotations`, no WARN for A1, A8, A15–A20.
2. `/verify` green including Spectral; Failsafe report present with zero failures.
3. Manual run: `docker run … postgres:17-alpine` as in step 4, `./mvnw -q spring-boot:run -Dspring-boot.run.profiles=local`,
   then `curl -s localhost:8080/api/v1/time-deposits | head -c 400`, `curl -s -X POST localhost:8080/api/v1/time-deposits/update-balances | head -c 400`,
   open `http://localhost:8080/swagger-ui/index.html` (check `200` with curl). Paste outputs
   in the report; the seeded `basic` deposit of `10000.00` at 45 days must show `10008.33`
   after one `POST`.
4. Report `prompts/reports/05-api.md`: commits, the RESTful checklist with the decision per
   item, Spectral output, concurrency test timing, manual run outputs.
