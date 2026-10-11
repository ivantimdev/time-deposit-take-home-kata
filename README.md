# Time Deposit Service — XA Bank kata solution

A Spring Boot / PostgreSQL service exposing two endpoints — update all balances and list all
deposits — built as a hexagonal refactoring of the original time deposit calculator.
The assignment is in [docs/ASSIGNMENT.md](docs/ASSIGNMENT.md).

**Status:** in progress.

## How to run

You need Docker with Docker Compose. Nothing else.

1. Start PostgreSQL and the service from the repository root:

   ```bash
   docker compose up --build
   ```

   The first build downloads the Maven dependencies and takes a few minutes.
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

Node.js is optional to build, test and run the service. It is used only for the contract lint,
Spectral run through `npx`, which is part of `/verify` and CI:
`npx -y @stoplight/spectral-cli@6 lint --fail-severity=warn java/src/main/resources/openapi/openapi.yaml`.
The Maven build validates the contract itself: the OpenAPI generator plugin rejects an invalid
contract and fails the build.

## Architecture

The package layout and dependency rule are described in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## AI-assisted development

The AI harness, its rules and which parts were AI-assisted are described in [docs/AI-DEVELOPMENT.md](docs/AI-DEVELOPMENT.md).

## Assumptions

Every reading of an ambiguous requirement is recorded in [docs/ASSUMPTIONS.md](docs/ASSUMPTIONS.md).

## Developer setup

1. Run `scripts/setup-hooks.sh` once after cloning to enable the git hooks.
2. Build and run all checks with `cd java && ./mvnw verify`.
