# Step 2 — Refactor `TimeDepositCalculator` into an extensible plan model

Read `AGENTS.md`, `.claude/rules/*.md`, `docs/ASSIGNMENT.md`, `docs/ASSUMPTIONS.md` and
`prompts/reports/01-characterization-tests.md`. The characterization tests are frozen: the
hook denies edits to them and the guard rails fail if they change. They are your oracle.

## Goal

Make the interest calculation "extensible to accommodate future complexities" (assignment §4)
**without changing behaviour**: every frozen test stays green, bit for bit. Keep the public
API: package `org.ikigaidigital`, public no-arg constructor, `updateBalance(List<TimeDeposit>)`.

## Target design (package `org.ikigaidigital.domain.plan`)

- `PlanType` — enum `BASIC("basic")`, `STUDENT("student")`, `PREMIUM("premium")`;
  `String code()`; `static Optional<PlanType> fromCode(String code)` with an **exact,
  case-sensitive** match (`"Basic"`, `"BASIC"`, `"basic "` → empty; `null` → empty). The
  original compares with `equals`; the frozen tests pin that mis-cased and unknown codes earn
  nothing (ASSUMPTION — see below).
- `AnnualRate` — record over a `double` (e.g. `0.03`), `double monthlyInterestOn(double balance)`
  returning `balance * rate / 12` in exactly that order. Do not use `BigDecimal` here; the
  arithmetic must stay `double` to keep the frozen results.
- `EarningPeriod` — record `(int firstDay, int lastDay)` inclusive; factories `from(int day)`
  (`day..MAX_VALUE`), `after(int day)` (`day + 1..MAX_VALUE`), `between(int first, int last)`;
  `boolean includes(int days)`.
- `InterestPolicy` — interface, `double monthlyInterest(TimeDeposit deposit)`.
- `FixedRateInterest` — record `(AnnualRate rate, EarningPeriod period)` implementing
  `InterestPolicy`: rate applied when the period includes `deposit.getDays()`, else `0`.
- `Plan` — record `(PlanType type, InterestPolicy interest)`. One field per rule family; a
  plan is a composition of rules, not a branch.
- `PlanCatalog` — holds the plans by `PlanType` (an `EnumMap`); `static PlanCatalog standard()`
  with exactly:
  ```
  BASIC    FixedRateInterest(AnnualRate(0.01), EarningPeriod.after(30))
  STUDENT  FixedRateInterest(AnnualRate(0.03), EarningPeriod.between(31, 365))
  PREMIUM  FixedRateInterest(AnnualRate(0.05), EarningPeriod.after(45))
  ```
  and `Optional<Plan> planFor(String planTypeCode)` (via `PlanType.fromCode`). The global
  "no interest in the first 30 days" guard of the original becomes each plan's earning
  period — same behaviour, but a future plan can start earning on another day.

`TimeDepositCalculator` (same package, same public API):
- field `PlanCatalog plans`; `public TimeDepositCalculator()` → `this(PlanCatalog.standard())`;
  `public TimeDepositCalculator(PlanCatalog plans)`.
- `updateBalance(List<TimeDeposit> deposits)`: for each deposit, interest =
  `plans.planFor(code).map(plan -> plan.interest().monthlyInterest(deposit)).orElse(0.0)`,
  rounded to cents with `new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP)` and
  added to the balance as `double`, exactly as today. Rounding lives in one private method.
  Keep the `new BigDecimal(double)` constructor and write the one comment that earns its
  place: *why* it is not `BigDecimal.valueOf` (different cents on some balances; pinned by the
  characterization tests).
- Rename the parameter `xs` → `deposits` (not part of the signature). No other public members.

Do not add: rule types the assignment does not ask for, clocks, configuration
loading, a `Plan` field that nothing reads. The design must make those *possible*; building
them now is speculative code.

## Assumptions to register (rows in `docs/ASSUMPTIONS.md`, comments where applied)

- A3 — "Student Plan: 3% interest (no interest after 1 year)": interest on days 31–365
  inclusive, none from day 366 (the original's `days < 366`). Applied in `PlanCatalog.standard()`.
- A4 — "Premium Plan: 5% interest (interest starts after 45 days)": interest from day 46.
  Same place.
- A5 — "No interest is applied for the first 30 days for any existing plans": interest from
  day 31; modelled per plan as its earning period rather than as a global guard. Same place.
- A6 — plan type codes: the stored code is matched exactly and case-sensitively; an unknown
  or differently-cased code earns no interest and raises no error (original behaviour).
  Applied in `PlanType.fromCode`.
- A7 — "monthly interest" means one application of `annualRate / 12` per call of
  `updateBalance`; the method does not advance `days` and does not know the calendar.
  Applied in `AnnualRate.monthlyInterestOn`.

## Tests (new, `*Test`, pure JUnit + AssertJ; never touch the frozen files)

- `PlanTypeTest`: each code maps; `Basic`, `BASIC`, `basic `, `gold`, `""`, `null` → empty.
- `EarningPeriodTest`: `after(30)` excludes 30, includes 31 and `Integer.MAX_VALUE`;
  `between(31,365)` boundaries; `from(1)` includes 1, excludes 0 and negatives.
- `AnnualRateTest`: `monthlyInterestOn` equals the literal `balance * rate / 12` for two or
  three values (literals from a tiny generator, not the formula in the test).
- `FixedRateInterestTest`: zero outside the period, rate applied inside.
- `PlanCatalogTest`: `standard()` has exactly the three plans with those rates and periods;
  `planFor("basic")` present, `planFor("Basic")` empty.
- `TimeDepositCalculatorWithCatalogTest`: a custom catalogue (e.g. one plan at
  `AnnualRate(0.12)`, `from(1)`) proves the constructor injection works — this is the
  extensibility seam demonstrated by a test, with golden values from running the new code
  once and reading the result (document in the report how they were obtained).

Mutation: run the `mutation` profile; the threshold must still pass. If the achieved score is
higher than the configured threshold, raise the threshold to the new value (never lower).

## Commits (each after a green `./mvnw -q verify`; via `/commit`)

1. `refactor(domain): extract interest policies per plan type`
   — `PlanType`, `AnnualRate`, `EarningPeriod`, `InterestPolicy`, `FixedRateInterest`, `Plan`,
   `PlanCatalog` added; calculator delegates to the catalogue. Behaviour identical; the global
   `days > 30` guard is still in the calculator at this point **if** that keeps the diff
   reviewable — otherwise fold into one commit and skip commit 2 (say which in the report).
2. `refactor(domain): move the thirty-day grace period into each plan`
   — the guard becomes the earning periods above; the calculator no longer knows about 30.
3. `refactor(domain): accept a plan catalogue in the calculator constructor`
   — second constructor; no-arg constructor delegates to `standard()`.
4. `test(domain): add unit tests for plan types, periods and the catalogue`
5. `build(config): apply formatting and sanitation to the calculator`
   — remove `TimeDepositCalculator.java` from the Spotless `<excludes>`, the rewrite
   `<exclusions>` and `scripts/guardrails-exclude.txt`; raise the mutation threshold if higher.
   `spotless:check`, the dry run and `scripts/guardrails.sh` green.
6. (if needed) `docs(repo): register interest calculation assumptions` — only if A3–A7 were
   not added alongside the code in commits 1–2; prefer adding the rows in the commit that
   introduces the comment.

## Verification before you finish

1. `git diff HEAD~N -- java/src/test/java/org/ikigaidigital/TimeDepositCalculatorTest.java java/src/test/java/org/ikigaidigital/TimeDepositCalculatorPublicApiTest.java` empty for the whole step.
2. `./mvnw -q verify` green; `/verify` green; mutation threshold met; `scripts/guardrails.sh`
   shows no `WARN` for A3–A7 (they are referenced in code).
3. `grep -rn "30\b\|45\b\|366\b\|365\b" java/src/main/java/org/ikigaidigital/TimeDepositCalculator.java` finds nothing — all numbers live in the catalogue.
4. No class in `org.ikigaidigital.domain..` imports anything outside `java.*` and
   `org.ikigaidigital..` (`grep -rn "^import" java/src/main/java/org/ikigaidigital/domain`).
5. Report `prompts/reports/02-domain-refactor.md`: commits, design as built (one paragraph),
   how the custom-catalogue test values were obtained, mutation score before/after, anything
   you considered and rejected.
