---
name: extend
description: Add a new business requirement safely - restate it, check constraints, pass a design gate, then implement, verify and commit.
---

# Extend

Use for every new requirement, including a later requirement or a future change.

1. **Restate** the requirement as one business rule in the vocabulary of `docs/ASSIGNMENT.md` (`planType`, `days`, `balance`, `withdrawals`). Name its rule family: interest, lifecycle, persistence or API.
2. **Constraints** — list which hard constraints in `AGENTS.md` it touches and how they stay intact.
3. **Design gate** — if the change adds a rule family, changes a documented assumption, or has more than one reasonable placement:
   - present at least two designs, each with its placement, the files it changes and its trade-offs;
   - **stop and wait for the developer's choice.**
   Otherwise state the single placement and why it is the only reasonable one.
4. **Plan** the files to change, in this order:
   1. production code;
   2. tests;
   3. migration, seed data or contract;
   4. docs and `docs/ASSUMPTIONS.md` (new row plus `// ASSUMPTION A<n>` comment for every new reading of an ambiguity).
5. **Implement** the plan, following `.claude/rules/*.md`. Build the seam the requirement needs, nothing speculative.
6. Run `/verify`. Never edit, weaken or delete a frozen test to make it pass; add new tests instead.
7. Run `/commit` for each logical change, tests that protect existing behaviour first.
