# Commit message convention

A commit message is **exactly one line**:

    <type>(<scope>): <summary>

- No body, no blank line, no trailers (no `Co-Authored-By`, no `Signed-off-by`), no issue references.
- Create it with a single `-m`: `git commit -m "<type>(<scope>): <summary>"`.

## Rules

1. **type** is required and must be one of:

   | type | use for |
   |---|---|
   | `feat` | new behaviour visible to a user of the API or the domain |
   | `fix` | correcting wrong behaviour |
   | `refactor` | structure changes with identical behaviour |
   | `test` | adding or changing tests only |
   | `docs` | documentation only (README, design notes, AI write-up) |
   | `build` | build system and dependencies (`pom.xml`, Maven wrapper, Dockerfile, compose) |
   | `ci` | CI pipeline (`.github/workflows`) |
   | `chore` | housekeeping that touches no production code or behaviour (repo cleanup, ignore files, AI harness config) |

2. **scope** is required, lowercase, and must be one of:

   | scope | covers |
   |---|---|
   | `domain` | `TimeDeposit`, `TimeDepositCalculator`, plans and interest rules |
   | `app` | use cases / application services and ports |
   | `api` | OpenAPI contract and REST adapter |
   | `persistence` | JPA entities, repositories, persistence adapter |
   | `db` | schema migrations and seed data |
   | `config` | Spring configuration and wiring; build configuration |
   | `ai` | AI harness: `CLAUDE.md`, `AGENTS.md`, `.claude/**` |
   | `repo` | repository-level files: `.gitignore`, `scripts/`, `.githooks/`, `docs/`, root layout |

   If a change genuinely needs a new scope, use one short lowercase noun and reuse it consistently afterwards.

3. **summary**
   - imperative mood: "add", "remove", "extract", not "added" or "adds"
   - starts with a lowercase letter
   - no trailing period
   - the whole line is **≤ 72 characters**
   - says *what* changes in domain terms; it does not describe how the work was done or which tool did it

4. **Atomicity**
   - one logical change per commit
   - the project builds and **all tests pass at every commit**
   - don't mix a refactor with a behaviour change
   - don't mix production code from different scopes unless the change can't be split
   - tests that protect existing behaviour (characterization tests) are committed **before** the refactor they protect

5. Agents may push the current branch after `/verify` is green. Nobody rewrites published history (`--amend`, rebase, reset, force-push).

## Examples

Valid:

    chore(repo): remove unused language starters
    test(domain): add characterization tests for interest calculation
    refactor(domain): extract interest rule per plan type
    feat(app): add use cases to list deposits and update balances
    build(config): add Spring Boot and PostgreSQL dependencies
    feat(db): add schema for time deposits and withdrawals
    feat(persistence): add JPA adapter with Testcontainers tests
    feat(api): add OpenAPI contract for the two endpoints
    ci(repo): run build and tests on push
    docs(repo): document how to run and call the endpoints

Invalid:

    Remove unused language starters          # missing type and scope
    chore: remove unused language starters   # missing scope
    chore(Repo): Remove unused starters.     # uppercase scope, capitalised summary, trailing period
    feat(api): added endpoints               # not imperative
    fix(domain): fix bug                     # summary says nothing
    feat(domain): add new rule and refactor calculator and update README   # not atomic

## Checklist before committing

- [ ] `./mvnw -q verify` passes
- [ ] the staged diff is one logical change (`git diff --cached --stat`)
- [ ] the message matches `^(feat|fix|refactor|test|docs|build|ci|chore)\([a-z]+\): [a-z].{0,70}[^.]$` and is ≤ 72 characters
- [ ] show the proposed message to the developer and wait for approval before `git commit`
