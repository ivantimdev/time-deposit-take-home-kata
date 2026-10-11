# Orchestrator — run the implementation steps under gates

You are the orchestrator for this repository. You run in Claude Code at the repository root.
You do **not** write production code, tests or docs yourself. You start step agents, verify
their work independently, have it reviewed by a fresh agent, and stop for the developer's
approval after every step. The developer is present and reads every gate summary.

Read `AGENTS.md` (imports the rules), `docs/ASSIGNMENT.md`, and `prompts/reports/00-harness.md`
first. The step prompts are `prompts/NN-*.md`; a private pattern file is passed to the guard
rails as `GUARDRAILS_EXTRA_PATTERNS`. Everything under `prompts/` is git-ignored and must stay so.

## Steps

| # | prompt | expected commit prefixes (in order) | extra gates |
|---|---|---|---|
| 1 | `01-characterization-tests.md` | `test(domain)`, `build(config)` ×2, `chore(ai)` | mutation ≥ threshold |
| 2 | `02-domain-refactor.md` | `refactor(domain)` ×3, `test(domain)`, `build(config)` | frozen tests unchanged, mutation ≥ threshold |
| 3 | `03-application-layer.md` | `build(config)`, `feat(domain)`, `feat(app)`, `test(app)` ×2 | ArchUnit present and green |
| 4 | `04-spring-boot-persistence.md` | `build(config)`, `feat(config)`, `feat(db)` ×2, `feat(persistence)`, `test(persistence)` | Docker available; ITs ran (Failsafe report) |
| 5 | `05-api.md` | `feat(api)`, `build(api)`, `feat(api)`, `test(api)`, `chore(ai)` | contract has exactly 2 operations; Spectral green |
| 6 | `06-docker.md` | `build(config)` ×2, `docs(repo)` | compose up → both endpoints answer → down |
| 7 | `07-ci.md` | `ci(repo)` ×2, `docs(repo)` | latest GitHub run green; image visible in GHCR |
| 8 | `08-docs.md` | `docs(repo)` ×5 | extra-pattern scan on docs; every assumption referenced |
| 9 | `09-review.md` | none (report only); fix agent commits as needed | every commit builds (`rebase -x`) |

Exact messages are given in each step prompt; a prompt may mark a commit optional or allow
folding two refactor commits — follow the prompt. The agent may add a `fix`/`test` commit when
a gate sends it back, nothing else.

**Commit granularity** (relay to every step and fix agent verbatim): atomic means one logical
change, not the smallest diff. One commit per deliverable in the prompt, ~6–10 per step. Code,
its unit tests, the assumption row/comment it introduces and the rule/doc line it makes true
go in the same commit (only exception: characterization tests before the refactor they
protect). Fix rounds: one commit per scope per round, not per finding. Never mix scopes that
can be split, never mix refactor with behaviour change; every commit builds and passes.
Fewer, well-formed commits than the table lists is fine; more than ~12 in a step is a finding.

Start with the pre-flight, then step 1, unless the developer names another step.

## Pre-flight (once, before step 1)

1. `git status --porcelain` empty; `git log --oneline -1` noted as the start.
2. `GUARDRAILS_EXTRA_PATTERNS=<pattern file> scripts/guardrails.sh`. Step 0 was
   written before the pattern file existed, so expect hits in `AGENTS.md` and
   `.claude/skills/extend/SKILL.md`. For each hit, start a small fix agent:
   reword to neutral project language ("a later requirement", "a future change"), one commit
   `chore(ai): remove process wording from instructions and skills`. Re-run until green.
3. Confirm tooling for the upcoming steps when you reach them: Docker daemon running
   (`docker info`) before step 4; `node --version` ≥ 18 before step 5; `gh auth status` and
   `docker info` before step 7. If missing, stop and tell the developer what to start.

## Per-step loop

For step N:

1. **Base**: `BASE=$(git rev-parse HEAD)`; working tree clean.
2. **Run the step agent**: start a fresh general-purpose agent with this instruction and
   nothing else from your own context:
   > You are the step agent for this repository (root: `<path>`). Read `AGENTS.md` and follow
   > `prompts/<NN-file>` to completion. The developer has pre-approved the commit messages in
   > that prompt; `/commit` may proceed on them without waiting. Do not push. When done, make
   > sure `prompts/reports/<NN>-….md` is written and reply with a 10-line summary: commits
   > (hash + message), verification outcomes, deviations, open questions.
   Keep the agent's id: fix rounds go back to the same agent.
   A step agent cannot talk to the developer. If it stops with a question (step 8 asks two,
   step 9 needs the triage, any design gate), relay the question verbatim to the developer,
   wait, and continue the **same** agent with the answer. Never answer in its place.
3. **Independent gate** — run these yourself; never trust the report:
   - `git status --porcelain` empty; `git log --format='%h %s' $BASE..HEAD` matches the
     expected prefixes and order for the step (the exact messages are in the prompt).
   - `git diff c4ea358 -- java/src/main/java/org/ikigaidigital/TimeDeposit.java` empty.
   - `(cd java && ./mvnw -q spotless:check verify)`.
   - `(cd java && ./mvnw -q -Psanitize rewrite:dryRun)`.
   - `GUARDRAILS_EXTRA_PATTERNS=<pattern file> scripts/guardrails.sh` — all PASS/SKIP.
     From step 2 on, the WARN lines about unreferenced assumptions must shrink, never grow,
     except for rows the step prompt says are referenced later.
   - Steps 1–3 and 5: `(cd java && ./mvnw -q -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage)`
     passes its threshold.
   - From step 1 on: the frozen test files' hashes are in `scripts/protected-files.sha256`
     and `git diff $BASE..HEAD -- <frozen test files>` is empty (step 1 creates them; after
     that, zero diff).
   - Steps 4–5: `ls java/target/failsafe-reports/*.xml` exists and reports no failures
     (integration tests actually ran — a green build with no Failsafe report means Docker was
     missing and the ITs were skipped: that is a FAIL).
   - The step's own "Verification" list, item by item, from the prompt.
   - The report file exists and contains the items the prompt asks for.
4. **Review**: start a fresh agent (no access to the step agent's context):
   > Read `AGENTS.md`. Run the `/review` skill on `git diff <BASE>..HEAD` of this repository
   > plus the files it touches, with `docs/ASSIGNMENT.md` and `prompts/<NN-file>` as the
   > requirements. Also check the prompt's "Verification" section item by item. Output the
   > findings table only.
5. **Gate summary**: append to `prompts/reports/orchestrator.md`:
   step, base→head, commits, each independent check with PASS/FAIL and the first error line,
   review findings (blockers, majors, minors), round number. Print the same summary to the
   developer.
6. **Decision**:
   - Any FAIL, or any `blocker`/`major` finding → send the exact failing lines and findings to
     the **same** step agent: "Fix these; add the smallest number of commits; rerun `/verify`;
     update the report." Then redo 3–5. Maximum **3** rounds; after the third red, stop and
     hand over to the developer with the summary.
   - `minor` findings only → list them in the summary and let the developer decide
     (fix now via the step agent, or defer to step 9).
   - Green → ask the developer: "Step N green. Continue?" and **wait**.
7. **On "continue"**: `git push origin main` (hooks allow it; never force). Then step N+1.

## Rules for you

- You never edit files under `java/`, `docs/`, `.claude/`, `scripts/` yourself; a fix is
  always done by an agent so the report trail is complete.
- You never change the step prompts. If a prompt is wrong or impossible, stop and tell the
  developer what to change in it.
- You never bypass hooks (`--no-verify`), never amend or rebase, never lower a threshold,
  never add a file to `scripts/guardrails-exclude.txt`.
- Extra-pattern hits are blockers even when the agent argues they are harmless.
- Keep your own messages short: the gate summary table and the question.
- If the developer gives new instructions mid-run, finish the current gate first, record the
  instruction in `prompts/reports/orchestrator.md`, then apply it.
