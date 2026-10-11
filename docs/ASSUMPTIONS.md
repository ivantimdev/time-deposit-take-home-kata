# Assumptions register

Every reading of an ambiguous requirement gets an id `A<n>` and a row in the table below.
The code where the decision applies carries a `// ASSUMPTION A<n>: <one line>` comment so a reader can trace it back here.

Ids are never reused or renumbered: a decision that changes keeps its id and its row is updated.
`scripts/guardrails.sh` fails when code cites an id that has no row here.

| Id | Requirement text (quoted) | Ambiguity | Decision | Where applied |
|---|---|---|---|---|
| A1 | "The final solution must include **exactly two API endpoints**." | Whether the OpenAPI contract served for Swagger UI, Swagger UI itself or operational endpoints count as endpoints. | The served contract and Swagger UI are documentation of the two endpoints, not additional endpoints; no actuator or other HTTP surface is added. | OpenAPI contract, web configuration |
| A2 | "Embrace Hexagonal Architecture." | Whether transaction demarcation, a framework concern, may appear in the application layer. | Transactions are declared with `@Transactional` on use-case services; the domain stays framework-free and the application layer is otherwise free of persistence and web dependencies (enforced by ArchUnit). | The application services, once they exist |
| A3 | "**Student Plan**: 3% interest (no interest after 1 year)" | Whether "after 1 year" ends on day 365 or day 366, and whether day 365 still earns. | Interest on days 31 to 365 inclusive; none from day 366 (the original's `days < 366`). | `PlanCatalog.standard()` |
| A4 | "**Premium Plan**: 5% interest (interest starts after 45 days)" | Whether day 45 itself earns. | Interest from day 46 (the original's `days > 45`). | `PlanCatalog.standard()` |
| A5 | "No interest is applied for the first 30 days for any existing plans." | Whether the grace period is one global rule or part of each plan, and whether day 30 earns. | Interest from day 31; the grace period is modelled as each plan's earning period rather than a global guard, so a future plan can start earning on another day. | `PlanCatalog.standard()` |
| A6 | "Implement logic to calculate monthly interest based on the plan type" | How the stored `planType` string is matched to a plan, and what an unknown code does. | The code is matched exactly and case-sensitively; an unknown or differently-cased code earns no interest and raises no error (original behaviour). | `PlanType.fromCode` |
| A7 | "Implement logic to calculate monthly interest based on the plan type" | What one call of `updateBalance` represents in time. | One call applies one month of interest, `annualRate / 12`; the method does not advance `days` and does not know the calendar. | `AnnualRate.monthlyInterestOn` |
