---
name: add-plan
description: Add a plan type whose rule is a fixed annual rate over an earning period - the recipe of ARCHITECTURE.md › Adding a plan, executable. Anything else goes through /extend.
---

# Add a plan

Follow `.claude/rules/*.md`. The characterization tests and `TimeDeposit.java` are frozen; never touch them.

1. **Inputs** — collect from the developer:
   - the code, lowercase, as stored in `plan_type` and sent in `planType`;
   - the annual rate, as a decimal (`0.02` for 2 %);
   - the earning period: `EarningPeriod.after(d)` or `EarningPeriod.between(a, b)`;
   - whether anything other than the interest changes.

   **If anything else changes** — a rule that is not a fixed-rate interest, an end-of-life rule, a rule that reads withdrawals — **stop and run `/extend`'s design gate instead.**

2. **`PlanType`** ([`PlanType.java`](../../../java/src/main/java/org/ikigaidigital/domain/plan/PlanType.java)): a new constant with its code, after the existing ones.

3. **`PlanCatalog.standard()`** ([`PlanCatalog.java`](../../../java/src/main/java/org/ikigaidigital/domain/plan/PlanCatalog.java)): the entry, `new Plan(PlanType.<CODE>, new FixedRateInterest(new AnnualRate(<rate>), <period>))`.

4. **Migration** `java/src/main/resources/db/migration/V<next>__allow_<code>_plan_type.sql`:

   ```sql
   ALTER TABLE time_deposits DROP CONSTRAINT time_deposits_plan_type_check;
   ALTER TABLE time_deposits ADD CONSTRAINT time_deposits_plan_type_check
       CHECK (plan_type IN ('basic', 'student', 'premium', '<code>'));
   ```

   The constraint name is the one PostgreSQL generated for the inline CHECK in V1; list every code, the new one last (ASSUMPTION A12).

5. **Contract** (`java/src/main/resources/openapi/openapi.yaml`): the code added to the `planType` enum, then `cd java && ./mvnw -q generate-sources` regenerates the model.

6. **Tests**, in this order, each with literal expectations:
   1. `PlanTypeTest`: a `code, CONSTANT` row in `findsThePlanTypeOfAnExactCode` and the code in `exposesTheStoredCodeOfEachPlanType`.
   2. `PlanCatalogTest`: `standard<Code>PlanEarns<Rate>...` asserting the entry's rate and period.
   3. A new class `<Code>PlanInterestTest` in `org.ikigaidigital`, next to `TimeDepositCalculatorWithCatalogTest`: a scratch file outside the repository (for example `jshell --class-path java/target/classes`) runs `new TimeDepositCalculator().updateBalance(...)` once for a few balances and days around the period's boundaries and prints `getBalance()`; paste the printed values as literals. Never derive them from the formula, and never edit the characterization tests.
   4. `TimeDepositPersistenceAdapterIT.savesAndLoadsADepositOfEachPlanType`: the code in its `@ValueSource`, so the constraint is proven to accept it.
   5. `PlanTypeContractTest` passes again once the contract and the enum agree.

7. **Seed** (`R__sample_deposits.sql`): one row with the new code, only if the developer wants it visible in the local run; then the README's sample table too.

8. **Docs**: in `ARCHITECTURE.md › The plan catalogue`, the list of codes under `PlanType` and the sentence counting the plans `standard()` holds. A row in `docs/ASSUMPTIONS.md` only if the requirement's wording needed a reading (a boundary day, "from day N" inclusive or exclusive); cite it with `// ASSUMPTION A<n>` in `PlanCatalog.standard()`.

9. **Verify and commit**: `/verify`, then the mutation run (`cd java && ./mvnw -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage`) must still meet its thresholds. Then `/commit` twice, in this order:
   - `feat(db): allow the <code> plan type` — step 4 alone: the constraint admits a code nothing uses yet, and every test still passes;
   - `feat(domain): add the <code> plan` — everything else. It cannot be split further: `PlanTypeContractTest` ties the enum to the contract, and the converter ties the persistence test to the enum, so a domain-only or api-only commit would be red.

   If the developer prefers one commit, make one and say so in the report.
