# Architecture rules

- Hexagonal: `domain` ← `application` ← `adapter.*`, wired in `config`. Dependencies point inward only; adapters never depend on each other.
- `domain` imports only the JDK: no Spring, JPA, Jackson, Lombok, validation or logging frameworks. ArchUnit enforces this with no exceptions.
- `application` imports the JDK, `domain` and `org.springframework.transaction..` only. ArchUnit forbids `jakarta.persistence..`, `org.springframework.data..`, `org.springframework.web..`, Jackson, `adapter..` and `config..` there — a JPA entity or a web DTO must never reach a use case.
- Inbound adapters call use-case interfaces (`application.port.in`); outbound adapters implement ports (`application.port.out`). Ports are named by intent, not technology.
- `TimeDeposit` stays a plain domain class in `org.ikigaidigital`; persistence uses a separate `TimeDepositEntity` mapped at the adapter boundary. Same for withdrawals.
- Transactions are declared with `@Transactional` on use-case services (ASSUMPTION A2); nowhere else except adapters when a local transaction is unavoidable.
- Plans are a composition of rules in a catalogue, not branches in the calculator. Adding a plan is a catalogue entry plus tests.
- No speculative rule families: build the seam, not the feature.
- No cycles between the slices `org.ikigaidigital.(*)..` (ArchUnit); the domain zone below is the one exception at the root.
- The root package `org.ikigaidigital` holds the frozen `TimeDeposit` and the `TimeDepositCalculator` facade; together with `org.ikigaidigital.domain..` it forms the domain zone. Dependencies between the root package and `domain` are allowed, and the root package is not a slice of the cycle rule.
