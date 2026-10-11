# Testing rules

- Pyramid: characterization tests of the original calculator (pure JUnit, no Spring) → domain unit tests → application tests with in-memory fakes → `@DataJpaTest` + Testcontainers for the persistence adapter → `@SpringBootTest` + Testcontainers for the endpoints → ArchUnit → mutation testing for `domain` + `application`.
- No Mockito in domain or application tests.
- Anything that touches PostgreSQL or HTTP runs against a real PostgreSQL container (Testcontainers, singleton container reused across classes). Never H2.
- Unit tests are `*Test` (Surefire); integration tests are `*IT` (Failsafe).
- Test names state behaviour in domain words, e.g. `creditsNoInterestWithinFirstThirtyDays`.
- Use parameterized tests for boundaries.
- Expected values are literals derived from the original implementation, never recomputed with the formula under test.
- Characterization tests are frozen once committed: a later step may add tests, never edit or delete them. They are protected by hash in `scripts/guardrails.sh`.
- AssertJ for assertions; one behaviour per test; no logic in tests.
