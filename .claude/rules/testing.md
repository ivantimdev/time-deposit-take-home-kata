# Testing rules

- Pyramid: characterization tests of the original calculator (pure JUnit, no Spring) → domain unit tests → application tests with in-memory fakes → `@DataJpaTest` + Testcontainers for the persistence adapter → `@SpringBootTest` + Testcontainers for the endpoints → ArchUnit → mutation testing for `domain` + `application`.
- No Mockito anywhere: domain and application tests use in-memory fakes, adapter tests run against a real container.
- Anything that touches PostgreSQL or HTTP runs against a real PostgreSQL container (Testcontainers, singleton container reused across classes). Never H2.
- Unit tests are `*Test` (Surefire); integration tests are `*IT` (Failsafe).
- Test names state behaviour in domain words, e.g. `creditsNoInterestWithinFirstThirtyDays`.
- Use parameterized tests for boundaries.
- Expected values are literals derived from the original implementation, never recomputed with the formula under test.
- The characterization tests, `TimeDepositCalculatorTest` and `TimeDepositCalculatorPublicApiTest` in `java/src/test/java/org/ikigaidigital/`, are frozen: a later step may add tests, never edit or delete them; `scripts/guardrails.sh` protects them by hash.
- AssertJ for assertions; one behaviour per test; no logic in tests.
