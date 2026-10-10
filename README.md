# Time Deposit Service — XA Bank kata solution

A Spring Boot / PostgreSQL service exposing two endpoints — update all balances and list all
deposits — built as a hexagonal refactoring of the original time deposit calculator.
The assignment is in [docs/ASSIGNMENT.md](docs/ASSIGNMENT.md).

**Status:** in progress.

## How to run

Run instructions, including how to call both endpoints from Swagger UI, are added to this section with the service.

## How to test

Test instructions are added to this section with the test suite; the build is checked with `./mvnw verify`.

## Architecture

The package layout and dependency rule are described in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## AI-assisted development

The AI harness, its rules and which parts were AI-assisted are described in [docs/AI-DEVELOPMENT.md](docs/AI-DEVELOPMENT.md).

## Assumptions

Every reading of an ambiguous requirement is recorded in [docs/ASSUMPTIONS.md](docs/ASSUMPTIONS.md).

## Developer setup

1. Run `scripts/setup-hooks.sh` once after cloning to enable the git hooks.
2. Build and run all checks with `cd java && ./mvnw verify`.
