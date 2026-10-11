# Step 6 — Dockerfile, Docker Compose, run instructions

Read `AGENTS.md`, `docs/ASSIGNMENT.md` ("Provide clear instructions on how to trigger the
endpoints using the Swagger contract") and the reports of steps 4–5. Docker must be running.

## Dockerfile (`java/Dockerfile`) and `java/.dockerignore`

Multi-stage:
1. `eclipse-temurin:17-jdk` (pin a concrete tag, e.g. `17-jdk-jammy`): copy `mvnw`, `.mvn/`,
   `pom.xml` first and run `./mvnw -q -B dependency:go-offline` (cached layer), then copy `src/`
   and `./mvnw -q -B -DskipTests package`. Tests run in CI, not in the image build (one-line
   comment).
2. `eclipse-temurin:17-jre` (same pin family): non-root user, `WORKDIR /app`, copy the jar as
   `app.jar`, `EXPOSE 8080`, `ENTRYPOINT ["java","-jar","/app/app.jar"]`. JVM flags: none
   beyond defaults unless you can justify one in one sentence.
`.dockerignore`: `target/`, `.idea/`, `*.iml`, `.git/`.

## `compose.yaml` (repository root)

- `postgres`: `postgres:17-alpine`, env `POSTGRES_DB/USER/PASSWORD = timedeposit`, port
  `5432:5432`, named volume `timedeposit-data`, healthcheck `pg_isready -U timedeposit -d timedeposit`.
- `app`: `build: ./java`, port `8080:8080`, env `DB_URL=jdbc:postgresql://postgres:5432/timedeposit`,
  `DB_USER`, `DB_PASSWORD`, `SPRING_PROFILES_ACTIVE=local` (sample data), `depends_on: postgres: condition: service_healthy`.
- Comments: what the volume is for and that `docker compose down -v` resets the sample data.

## README — "How to run" and "How to test" (replace the placeholder sentences)

Write for someone who has Docker and nothing else:
1. `docker compose up --build` (first build takes a few minutes) → service on `http://localhost:8080`.
2. Open Swagger UI `http://localhost:8080/swagger-ui/index.html`; the contract itself is at
   `http://localhost:8080/openapi.yaml`.
3. Walkthrough in Swagger UI: expand `GET /api/v1/time-deposits` → *Try it out* → *Execute*
   → read the sample deposits; expand `POST /api/v1/time-deposits/update-balances` → *Execute*
   → balances of the eligible plans have grown by one month of interest (give the expected
   numbers for the seed: `10000.00` basic/45d → `10008.33`, student/100d → `10025.00`,
   premium/60d → `10041.67`, premium/20d unchanged); run `GET` again to confirm persistence.
4. `curl` equivalents of both calls.
5. Reset: `docker compose down -v`.
6. Running without Docker Compose for development: start only PostgreSQL
   (`docker compose up postgres`), then `cd java && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`.
7. Tests: `cd java && ./mvnw verify` (Docker needed for the integration tests); mutation
   testing and sanitation commands; `scripts/guardrails.sh`.
Keep sentences short; no marketing.

## Commits

1. `build(config): add dockerfile for the service`
2. `build(config): add docker compose with postgresql`
3. `docs(repo): document how to run the service and call the endpoints`

## Verification before you finish

1. `docker compose up --build -d`; wait for health; `curl -s localhost:8080/api/v1/time-deposits`
   shows the four seeded deposits; `curl -s -X POST …/update-balances` shows the expected
   balances above; `curl -s -o /dev/null -w '%{http_code}' localhost:8080/swagger-ui/index.html`
   → `200`; `docker compose down -v`. Paste the outputs in the report.
2. `docker compose up -d` a second time: Flyway finds the schema applied and the repeatable
   seed unchanged (log lines in the report); `down -v`.
3. Image size (`docker image ls`) and the user the container runs as (`docker compose exec app id`)
   in the report.
4. `/verify` green (no Java changes expected; the build must still pass).
5. Report `prompts/reports/06-docker.md`.
