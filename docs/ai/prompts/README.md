# Agent prompts

These are the prompts the agents ran under, one per step, plus the orchestrator's.
Each step was executed by a fresh Claude Code agent at the repository root; the orchestrator verified the result and stopped for the developer's approval.
What happened in each step is in [`../WORKFLOW-LOG.md`](../WORKFLOW-LOG.md).

| prompt | purpose |
|---|---|
| [`orchestrator.md`](orchestrator.md) | Runs the steps under gates: start a step agent, verify independently, start a review agent, wait for the developer. |
| [`00-harness.md`](00-harness.md) | Agent instructions, rules, skills, hooks, guard rails, and the formatting, coverage, mutation and sanitation build. |
| [`01-characterization-tests.md`](01-characterization-tests.md) | Characterization tests of the original `TimeDepositCalculator`, then frozen. |
| [`02-domain-refactor.md`](02-domain-refactor.md) | Calculator refactored into a plan catalogue with the same public API and results. |
| [`03-application-layer.md`](03-application-layer.md) | Use cases, ports, an in-memory repository for tests, and ArchUnit rules. |
| [`04-spring-boot-persistence.md`](04-spring-boot-persistence.md) | Spring Boot, Flyway schema, seed data, and the JPA adapter with Testcontainers tests. |
| [`05-api.md`](05-api.md) | OpenAPI contract, generated interfaces, REST adapter, Swagger UI and endpoint tests. |
| [`06-docker.md`](06-docker.md) | Dockerfile, Docker Compose with PostgreSQL, and the run instructions. |
| [`07-ci.md`](07-ci.md) | GitHub Actions running every check, and the container image published to GitHub Packages. |
| [`08-docs.md`](08-docs.md) | Architecture, assumptions, AI-assisted development, these prompts and the final README. |
| [`09-review.md`](09-review.md) | Independent final review against the assignment, then fixes. |
| [`10-final-fixes.md`](10-final-fixes.md) | Assumptions strengthened and added, the scaling note, the `/add-plan` skill proven on a throwaway branch, and two tests. |

Some sentences were reworded or shortened for publication.
Paths under `prompts/` point to the private, git-ignored directory where the originals and the step reports live; they are not part of the repository.
