# Time Deposit Service — XA Bank kata solution

A Spring Boot / PostgreSQL service exposing two endpoints — update all balances and list all
deposits — built as a hexagonal refactoring of the original time deposit calculator.
The calculator keeps its public API and its results, pinned by characterization tests; the
interest rules live in a plan catalogue, and the API is generated from an OpenAPI contract.
The original README is kept verbatim as [docs/ASSIGNMENT.md](docs/ASSIGNMENT.md), so that this one can describe the solution.

[![CI](https://github.com/ivantimdev/time-deposit-take-home-kata/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/ivantimdev/time-deposit-take-home-kata/actions/workflows/ci.yml?query=branch%3Amain)

## How to run

You need Docker with Docker Compose. Nothing else.

1. Start PostgreSQL and the service from the repository root:

   ```bash
   docker compose up --build
   ```

   The first build downloads the Maven dependencies and takes a few minutes.
   To skip the build, use the image CI publishes for every release instead:
   `docker pull --platform linux/amd64 ghcr.io/ivantimdev/time-deposit-kata:latest`,
   then replace `build: ./java` with the commented `image:` and `platform:` lines in
   `compose.yaml` and run `docker compose up`.
   The image is built for `linux/amd64` only; on Apple Silicon Docker runs it under emulation.
   While the package on GitHub is private, the pull needs `docker login ghcr.io` first,
   as a GitHub user with access to the package.
   The service is ready when the log shows `Started TimeDepositApplication`.
   It listens on `http://localhost:8080`.
   It runs with the `local` profile, which loads four sample deposits.
   If host port 5432 is taken, change the published PostgreSQL port in `compose.yaml`.

2. Open Swagger UI at `http://localhost:8080/swagger-ui/index.html`.
   The contract itself is served at `http://localhost:8080/openapi.yaml`.

3. Call both endpoints from Swagger UI:
   1. Expand `GET /api/v1/time-deposits`, click *Try it out*, then *Execute*.
      The response lists the four sample deposits, each with a balance of `10000.00` on a fresh volume.
   2. Expand `POST /api/v1/time-deposits/update-balances`, click *Try it out*, then *Execute*.
      Each eligible deposit has earned one month of interest:

      | id | planType | days | balance after one update |
      |---|---|---|---|
      | 1 | basic | 45 | `10008.33` |
      | 2 | student | 100 | `10025.00` |
      | 3 | premium | 60 | `10041.67` |
      | 4 | premium | 20 | `10000.00` (unchanged) |

   3. Run the `GET` again: it returns the updated balances, read back from the database.

   Each further `POST` credits one more month.

4. The same calls with `curl`:

   ```bash
   curl -s http://localhost:8080/api/v1/time-deposits
   curl -s -X POST http://localhost:8080/api/v1/time-deposits/update-balances
   ```

5. Stop the service with `Ctrl+C`, or `docker compose down`. The balances are kept in a Docker volume.
   To reset to the sample data, delete the volume:

   ```bash
   docker compose down -v
   ```

### Without Docker Compose for the service

For development, run only PostgreSQL in Docker, in the background, and the service with Maven (JDK 17 needed):

```bash
docker compose up -d postgres
cd java && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Stop the service with `Ctrl+C`, then PostgreSQL with `docker compose down`.

## How to test

Run every test from `java/` (JDK 17 needed):

```bash
cd java && ./mvnw verify
```

- Unit tests (`*Test`) need nothing else.
- Integration tests (`*IT`) start PostgreSQL with Testcontainers, so Docker must be running.
- The build also checks formatting (Spotless) and writes a coverage report to `java/target/site/jacoco/`.

Mutation testing of the domain and application code (thresholds: 100 % mutation and line coverage):

```bash
cd java && ./mvnw -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage
```

Static analysis (sanitation) without changing files; any finding fails it:

```bash
cd java && ./mvnw -Psanitize rewrite:dryRun
```

Repository guard rails (protected files, commit messages, the two API operations, banned patterns, assumptions):

```bash
scripts/guardrails.sh
```

Node.js (22 or later, which Spectral 6.17 needs; CI runs 22) is optional to build, test and run the service. It is used only for the contract lint,
Spectral run through `npx`, which is part of `/verify` and CI:
`npx -y @stoplight/spectral-cli@6 lint --fail-severity=warn java/src/main/resources/openapi/openapi.yaml`.
The Maven build validates the contract itself: the OpenAPI generator plugin rejects an invalid
contract and fails the build.

## API

| method | path | purpose |
|---|---|---|
| `GET` | `/api/v1/time-deposits` | List all time deposits, each with its withdrawals. |
| `POST` | `/api/v1/time-deposits/update-balances` | Credit one month of interest to every deposit and return the updated deposits. |

The contract is [java/src/main/resources/openapi/openapi.yaml](java/src/main/resources/openapi/openapi.yaml).
With the service running, Swagger UI is at `http://localhost:8080/swagger-ui/index.html`
and the contract at `http://localhost:8080/openapi.yaml`.

## Architecture

- Hexagonal: a REST adapter generated from the contract, use cases behind ports, and a JPA adapter for PostgreSQL.
- The domain (`TimeDeposit`, `TimeDepositCalculator`, the plan catalogue) imports only the JDK; ArchUnit enforces the layering.
- `TimeDeposit` is unchanged, and `TimeDepositCalculator` keeps its package, no-arg constructor and `updateBalance` signature.
- A plan is a catalogue entry of a type, a rate and an earning period; the calculator holds no plan rules.
- A balance update runs in one transaction that locks the deposit rows, so concurrent updates lose no interest.

Request flows, money handling and what is out of scope: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Assumptions

Every reading of an ambiguous requirement is recorded in [docs/ASSUMPTIONS.md](docs/ASSUMPTIONS.md)
and cited in the code as `// ASSUMPTION A<n>`.

## Requirements

[docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) maps every requirement of the assignment to the code that implements it and the test that proves it.

## AI-assisted development

The solution was built with Claude Code: one fresh agent per step, each running a written prompt, and no step started before the previous one was verified and approved by the developer.
The agents work under [AGENTS.md](AGENTS.md), the rules in [.claude/rules/](.claude/rules/), four skills, a hook protecting frozen files, git hooks and [scripts/guardrails.sh](scripts/guardrails.sh); CI runs the same checks.
Setup and what the harness caught: [docs/AI-DEVELOPMENT.md](docs/AI-DEVELOPMENT.md); prompts: [docs/ai/prompts/](docs/ai/prompts/README.md); step log: [docs/ai/WORKFLOW-LOG.md](docs/ai/WORKFLOW-LOG.md).

## Repository layout

```text
.
├── .claude/                 agent rules, skills, protected-files hook, settings
├── .githooks/               commit-msg and pre-commit hooks
├── .github/workflows/       CI and image publishing
├── docs/                    assignment, architecture, assumptions, AI-assisted development
│   └── ai/                  published agent prompts and the workflow log
├── java/                    Maven project and Dockerfile
│   └── src/
│       ├── main/java/org/ikigaidigital/
│       │   ├── (root)                    TimeDeposit, TimeDepositCalculator, TimeDepositApplication
│       │   ├── domain/                   Withdrawal, TimeDepositDetails
│       │   │   └── plan/                 PlanType, Plan, InterestPolicy, FixedRateInterest, AnnualRate, EarningPeriod, PlanCatalog
│       │   ├── application/port/in/      use-case interfaces
│       │   ├── application/port/out/     TimeDepositRepository
│       │   ├── application/service/      use-case implementations
│       │   ├── adapter/in/web/           controller and response mapper
│       │   ├── adapter/out/persistence/  JPA entities, repositories, persistence adapter
│       │   └── config/                   bean wiring, web configuration
│       ├── main/resources/  application.yml, openapi/, db/migration/, db/seed/, static/swagger-ui/
│       └── test/            unit tests (*Test) and integration tests (*IT)
├── scripts/                 guard rails, banned patterns, protected-file hashes, hook setup
├── AGENTS.md, CLAUDE.md     agent instructions
├── compose.yaml             PostgreSQL and the service
└── .spectral.yaml           contract lint rules
```

## Development

| task | how |
|---|---|
| Enable the git hooks, once after cloning | `scripts/setup-hooks.sh` |
| Every check before a commit | `/verify` in Claude Code, or the commands in [its skill](.claude/skills/verify/SKILL.md) |
| Build, unit and integration tests | `cd java && ./mvnw verify` |
| Mutation testing | `cd java && ./mvnw -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage` |
| Sanitation dry run | `cd java && ./mvnw -Psanitize rewrite:dryRun` |
| Guard rails | `scripts/guardrails.sh` |
| Add a new requirement | `/extend` in Claude Code: restate it, pass a design gate, implement, verify, commit |
