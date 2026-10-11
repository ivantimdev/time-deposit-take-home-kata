# Architecture rules

- Hexagonal: `domain` ← `application` ← `adapter.*`, wired in `config`. Dependencies point inward only; adapters never depend on each other.
- Domain zone = `org.ikigaidigital.domain..` + `TimeDeposit` + `TimeDepositCalculator`. Classes in the zone import only `java..` and the zone itself; the two root classes get no annotations. So no Spring, JPA, Jackson, Lombok, validation or logging frameworks; ArchUnit enforces this with no exceptions. Inside the zone, `TimeDepositCalculator` may import `org.ikigaidigital.domain..`, and `org.ikigaidigital.domain..` may import only `TimeDeposit` from the root package. Any other root-package class, such as the Spring Boot application class, is outside the zone and never imported by zone classes.
- `application` imports the JDK, `domain`, `TimeDeposit`, `TimeDepositCalculator` and `org.springframework.transaction..` only. ArchUnit forbids `jakarta.persistence..`, `org.springframework.data..`, `org.springframework.web..`, Jackson, `adapter..` and `config..` there — a JPA entity or a web DTO must never reach a use case.
- Inbound adapters call use-case interfaces (`application.port.in`); outbound adapters implement ports (`application.port.out`). Ports are named by intent, not technology.
- `TimeDeposit` stays a plain domain class in `org.ikigaidigital`; persistence uses a separate `TimeDepositEntity` mapped at the adapter boundary. Same for withdrawals.
- Transactions are declared with `@Transactional` on use-case services (ASSUMPTION A2); nowhere else except adapters when a local transaction is unavoidable.
- Plans are a composition of rules in a catalogue, not branches in the calculator. Adding a plan is a `PlanType` constant, a catalogue entry and tests.
- No speculative rule families: build the seam, not the feature.
- No cycles between the slices `org.ikigaidigital.(*)..` (ArchUnit); the two root-package domain classes are not a slice.
