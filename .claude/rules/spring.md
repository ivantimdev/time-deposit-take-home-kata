# Spring rules

- Constructor injection only; no `@Autowired` anywhere; no field or setter injection.
- No stereotype annotations in `domain`/`application`; beans for use cases, the calculator and ports are declared in `config` `@Configuration` classes.
- Controllers are thin: map request → call one use case → map response. No logic.
- Controllers implement the interface generated from the OpenAPI contract; hand-written `@RequestMapping`/`@GetMapping`/… annotations are not allowed in `src/main`.
- Configuration is externalised in `application.yml`; profile `local` adds seed data and local DB settings.
- Tests use Testcontainers, never H2 or an embedded database.
- Flyway owns the schema; `spring.jpa.hibernate.ddl-auto=validate`.
- Repositories and JPA entities live only in `adapter.out.persistence`.
- Entities map foreign keys as plain columns (`timeDepositId`), with no `@OneToMany`/`@ManyToOne` navigation; referential integrity is the database's job (FK constraints and indexes in migrations).
- Handling invalid input is out of scope (README); no global exception handler is added.
