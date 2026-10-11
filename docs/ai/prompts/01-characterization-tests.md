# Step 1 — Characterization tests for `TimeDepositCalculator.updateBalance`

You are a senior Java engineer working in this repository at its root. Read `AGENTS.md`
(it imports the rules in `.claude/rules/`) and `docs/ASSIGNMENT.md` before anything else.
The harness, skills (`/verify`, `/commit`), hooks and guard rails from step 0 are in place;
use them.

## Goal

Pin the **exact current behaviour** of `org.ikigaidigital.TimeDepositCalculator.updateBalance`
with tests, so that the refactoring in the next step can be proven behaviour-preserving.
The assignment says the method "is functioning correctly" and its "behavior remains
unchanged after refactoring" — these tests are the definition of "unchanged".

This step changes **no production code**. `java/src/main/**` has zero diff at the end.

## What the original does (read the code; this is a summary, not a substitute)

```java
public void updateBalance(List<TimeDeposit> xs) {
  for each deposit:
    double interest = 0;
    if (days > 30) {
      if planType.equals("student") && days < 366 -> interest += balance * 0.03 / 12
      else if planType.equals("premium") && days > 45 -> interest += balance * 0.05 / 12
      else if planType.equals("basic") -> interest += balance * 0.01 / 12
    }
    balance = balance + new BigDecimal(interest).setScale(2, HALF_UP).doubleValue();
}
```

Properties worth pinning (verify each against the real code, don't trust this list blindly):
- interest only when `days > 30`; student only while `days < 366`; premium only when `days > 45`
- plan type comparison is **case-sensitive**: `"Basic"` and `"BASIC"` earn nothing
- an unknown plan type earns nothing; the balance is still rewritten (same value)
- arithmetic is `double`: `balance * rate / 12`, in that order
- rounding is `new BigDecimal(double)` (exact binary expansion, **not** `BigDecimal.valueOf`),
  scale 2, `HALF_UP`, then added back as `double`
- `days`, `id`, `planType` are never modified; the same `TimeDeposit` instances are mutated in
  place; list order is unchanged; the list itself is not replaced
- an empty list is a no-op; calling twice compounds (the second call works on the new balance)

## Golden values — the one rule that matters most

Every expected value in these tests is a **literal** obtained by **running the original
implementation** and printing `Double.toString(deposit.getBalance())`. Never compute an
expected value with the formula in the test, never with your own arithmetic, never by hand.

Procedure:
1. Write a throwaway generator **outside the repository** (e.g. a single-file Java program or
   `jshell` script in a temp directory, classpath `java/target/classes` after
   `./mvnw -q compile`). It constructs each case, calls `new TimeDepositCalculator().updateBalance(...)`
   and prints `planType, days, balanceBefore, Double.toString(balanceAfter)`.
2. Paste the generator's full output table into the report (`prompts/reports/01-characterization-tests.md`).
3. Copy the printed strings into the tests as `double` literals exactly as printed
   (`Double.toString` output round-trips exactly; `1235595.81` and `1000.9300000000001` are
   both legitimate expectations if that is what the original prints).
4. Assertions are exact: `assertThat(deposit.getBalance()).isEqualTo(expected)`. No
   `isCloseTo`, no `Offset`, no `Percentage`.
5. Do not commit the generator.

## Test classes (both in `java/src/test/java/org/ikigaidigital/`, package `org.ikigaidigital`)

Pure JUnit 5 + AssertJ. No Spring, no Mockito, no reflection outside the API test.
Replace the placeholder `TimeDepositCalculatorTest` (it asserts `1 == 1`).

### `TimeDepositCalculatorTest` — behaviour

Use `@ParameterizedTest` with `@CsvSource` (or `@MethodSource` when a row needs more than
primitives) so each matrix reads as a table. Group related rows in `@Nested` classes named
after the behaviour (`InterestByPlanAndAge`, `PlanTypeMatching`, `Rounding`, `ListHandling`).
Required coverage — every row is a golden value from the generator:

1. **Plan × age matrix** for `basic`, `student`, `premium`, balance `1234567.00`, days
   `-1, 0, 1, 2, 15, 29, 30, 31, 32, 44, 45, 46, 47, 60, 90, 180, 300, 364, 365, 366, 367,
   1000, 10000, 2147483647` (24 ages × 3 plans = 72 rows). Negative days and
   `Integer.MAX_VALUE` are not requirements; they pin that ages outside every window earn
   nothing and that nothing overflows. Say so in a one-line comment on the source.
2. **Balance matrix** at days `100` for the three plans, balances
   `0.00, 0.01, 0.10, 1.00, 99.99, 100.00, 999.99, 1000.00, 123456.78, 999999999.99,
   1000000000000.00` (11 × 3 = 33 rows). This pins sub-cent interest rounding to zero,
   cents handling, and precision loss on very large balances as the original behaves.
3. **Plan type matching** at days 100, balance `1000.00`: `BASIC`, `Basic`, `bAsIc`,
   `STUDENT`, `Premium`, `basic ` (trailing space), ` basic` (leading space), `gold`, `""`,
   `basic\n` — balance unchanged in value for all of them.
4. **Rounding**:
   - the exact half-cent that `HALF_UP` rounds **up** and `HALF_EVEN` would round down:
     `basic`, `150.00`, days 45 → interest `150 * 0.01 / 12 = 0.125` exactly in binary;
   - find, by brute force in the generator, for **each plan** at least one balance where
     `new BigDecimal(interest)` and `BigDecimal.valueOf(interest)` round to **different**
     cents (iterate balances in cents from 0.01 upwards, compute `interest` in `double` exactly
     like the original, compare both roundings, keep the first two hits per rate). Put the
     hits in the tests with their golden values and the search range in the report. If a
     rate has no hit below 1,000,000.00, say so;
   - at least two cases where the `double` addition leaves a long tail in `Double.toString`
     (the generator output will show them; pick two from different plans);
   - at least one case where the rounded interest is exactly `0.00` although the raw interest
     is positive, and one where it is exactly `0.01`.
5. **Mutation in place**: a list of three deposits (one per plan, different ages) — after the
   call: same size, same instances in the same order (`isSameAs`), `id`/`planType`/`days`
   unchanged, each balance equal to its golden value; the list object itself is the same
   reference the caller passed.
6. **Aliasing**: the same `TimeDeposit` instance appearing twice in the list receives interest
   twice (golden values after the call).
7. **Independence**: two deposits with the same `id` but different balances are updated
   independently (the method does not key by id).
8. **Empty list**: no exception, list still empty.
9. **Repeated calls compound**: `basic`, `1000.00`, days 45, called three times → golden
   values after each call; `student`, `1000.00`, days 365 then (a new deposit) days 366 →
   compounds then stops.
10. **Unmodifiable input list** (`List.of(...)`): works, because the method mutates elements,
    not the list (pins that the method never calls `set`/`add`/`remove` on the list).
11. **Singleton and large lists**: `Collections.singletonList(...)` works; a list of 10,000
    deposits built from the generator's seed (one golden value spot-checked for the first,
    the 5,000th and the last element) completes without exception.
12. **Negative balance** (`basic`, `-1000.00`, days 45): pins the current arithmetic sign
    behaviour with a golden value, and a one-line comment that this is not a requirement.

Not pinned, on purpose (say so in the report): `null` list, `null` elements, `null`
`planType`, `null` `balance` (all raise `NullPointerException` today), non-finite balances
(`new BigDecimal(Infinity/NaN)` raises `NumberFormatException`). The assignment excludes
invalid input, and pinning exception types would force the refactor to reproduce accidental
failure modes.

### `TimeDepositCalculatorPublicApiTest` — the frozen signature

Reflection-based assertions that the refactoring step must keep green:
- class `org.ikigaidigital.TimeDepositCalculator` exists, is `public`, not abstract;
- it has a `public` no-arg constructor;
- it has exactly one `public` method named `updateBalance`, non-static, returning `void`,
  with the single parameter type `java.util.List`;
- class `org.ikigaidigital.TimeDeposit` exists, is `public`, has the `public` constructor
  `(int, String, Double, int)`, public `getId()`, `getPlanType()`, `getBalance()`, `getDays()`,
  `setBalance(Double)`, and **no other public methods** apart from those inherited from
  `Object`.
Each assertion in its own test method with a behaviour-stating name
(`keepsPublicNoArgConstructor`, …). A short class-level comment: these tests exist because
the assignment forbids breaking changes to `TimeDeposit` and to the `updateBalance` signature.

## Style

`.claude/rules/java-style.md` and `testing.md` apply. In particular: no narrating comments,
no Javadoc on tests, no `xs`/`tmp`/`res`, no logic in tests (no loops computing expectations,
no `if`), AssertJ only, one behaviour per test method. The generator output goes in the
report, not in comments.

## Commits (in this order; each after a green `./mvnw -q verify`; use `/commit`)

1. `test(domain): add characterization tests for interest calculation`
   — both test classes; the placeholder is replaced. The test file is still excluded from
   Spotless/sanitize/banned patterns at this point, so format it by hand to google-java-format
   (run `./mvnw spotless:apply` after commit 2 to confirm nothing changes).
2. `build(config): apply formatting and sanitation to the calculator tests`
   — remove `src/test/java/org/ikigaidigital/TimeDepositCalculatorTest.java` from the Spotless
   `<excludes>`, the rewrite `<exclusions>` in `java/pom.xml`, and from
   `scripts/guardrails-exclude.txt`. `spotless:check`, the sanitation dry run and
   `scripts/guardrails.sh` must stay green with **no change to the test file** (if they
   don't, fix the test file in commit 1 before committing commit 2 — do not reorder).
3. `build(config): require the mutation score reached by the tests`
   — run `./mvnw -q -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage`, read the
   score from `target/pit-reports/**/mutations.xml` (or the console summary), and set
   `<mutationThreshold>` in the `mutation` profile to the **achieved** percentage (integer,
   rounded down). It must be ≥ 90; if it is lower, the matrix has a gap — find the surviving
   mutants in the report, add rows that kill them (still golden values, still in commit 1's
   file via an additional `test(domain)` commit before this one), and re-measure. Also set
   `<coverageThreshold>` to the achieved line coverage of the target classes, rounded down.
   Record both numbers and the surviving mutants (if any, with why they are equivalent) in the
   report.
4. `chore(ai): freeze the characterization tests`
   — append both test files to `.claude/protected-files.txt` (with a one-line comment: they
   define "behaviour unchanged" and may be extended only by adding new test classes) and
   their SHA-256 lines to `scripts/protected-files.sha256`. From now on the hook denies edits
   to them and `scripts/guardrails.sh` fails if they change. Also add one line to
   `.claude/rules/testing.md` naming the two frozen files.

## Verification before you finish

1. `git diff c4ea358 -- java/src/main` is empty.
2. `./mvnw -q verify` green; `/verify` green (`scripts/guardrails.sh` all PASS/SKIP; the
   assumptions WARNs are expected).
3. Mutation run passes its new threshold.
4. Pipe a sample hook event for `Edit` on each frozen test file into
   `.claude/hooks/protect-files.sh` → exit 2.
5. Sanity check against yourself: pick three golden rows at random and re-run the original
   on them in `jshell` — identical strings.
6. Count: at least 72 + 33 + 10 + (1 + 3 + 2 + 2) + 1 + 1 + 1 + 1 + 2 + 1 + 2 + 1 ≈ 133
   behaviour rows, plus the API tests. More is fine if each row pins a distinct behaviour;
   padding is not. A `@Nested` structure keeps the report of a failure readable.

You are done only when all of the above are green and the report is written
(`prompts/reports/01-characterization-tests.md`: commits, generator output, rounding-search
results, mutation/coverage numbers, surviving mutants, what was deliberately not pinned,
open questions). Do not push unless the developer asks.
