# Assumptions register

Every reading of an ambiguous requirement gets an id `A<n>` and a row in the table below.
The code where the decision applies carries a `// ASSUMPTION A<n>: <one line>` comment so a reader can trace it back here.

Ids are never reused or renumbered: a decision that changes keeps its id and its row is updated.
`scripts/guardrails.sh` fails when code cites an id that has no row here.

| Id | Requirement text (quoted) | Ambiguity | Decision | Where applied |
|---|---|---|---|---|
| A1 | "The final solution must include **exactly two API endpoints**." | Whether the OpenAPI contract served for Swagger UI, Swagger UI itself or operational endpoints count as endpoints. | The served contract and Swagger UI are documentation of the two endpoints, not additional endpoints; no actuator or other HTTP surface is added. | OpenAPI contract, web configuration |
| A2 | "Embrace Hexagonal Architecture." | Whether transaction demarcation, a framework concern, may appear in the application layer. | Transactions are declared with `@Transactional` on use-case services; the domain stays framework-free and the application layer is otherwise free of persistence and web dependencies (enforced by ArchUnit). | The application services, once they exist |
