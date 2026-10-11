---
name: commit
description: Create one conventional, single-line commit for one logical change after a green /verify and the developer's approval.
---

# Commit

Follow `.claude/rules/git.md`.

1. Run `git status --porcelain`. If the working tree has changes unrelated to the change being committed, refuse and list them.
2. Run `/verify`. Any failure: stop, fix, rerun; never commit on red.
3. Stage only the files of this change, then show `git diff --cached --stat`.
4. Confirm the staged diff is one logical change in one scope. If not, split it and commit the parts separately.
5. Propose a message that matches
   `^(feat|fix|refactor|test|docs|build|ci|chore)\([a-z]+\): [a-z].{0,70}[^.]$` and is at most 72 characters.
6. **Stop and wait for the developer's approval of the message.**
7. Commit with exactly one `-m` and nothing else:

   ```bash
   git commit -m "<type>(<scope>): <summary>"
   ```

## Never

- Never `--amend`, rebase, reset or force-push.
- Never add a body, trailers (`Co-Authored-By`, `Signed-off-by`) or issue references.
- Never bypass the git hooks (`--no-verify`).
- Never push as part of a commit. Pushing is a separate, explicit action taken only after `/verify` is green.
