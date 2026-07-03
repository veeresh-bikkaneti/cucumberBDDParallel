# Contributing to cucumberBDDParallel

**No one pushes directly to `main`.** All changes go through a pull request.

## Branch policy

- **`main` is the protected default branch.**
- **Never run `git push origin main`** for feature work or fixes.
- Create a topic branch from `main` using one of these prefixes:
  - `feature/…` — new capabilities, examples, docs
  - `fix/…` — bug fixes
  - `chore/…` — CI, metadata, non-functional polish

Keep branch names short and descriptive (e.g. `feature/web-patterns-mcp-examples`).

## Pull request workflow

1. Sync with `main`: `git checkout main && git pull origin main`
2. Create a branch: `git checkout -b feature/my-change`
3. Commit on the branch — one logical change per commit where possible
4. Push: `git push -u origin feature/my-change`
5. Open a PR targeting `main`
6. Wait for CI — all workflow jobs must pass on the PR
7. Human review — a maintainer approves; agents do not merge
8. **Merge to `main`** — after approval and green CI
9. **Delete the branch after merge** — merged topic branches are removed from the remote. GitHub deletes the head branch automatically when a PR merges. Delete your local copy:

   ```bash
   git checkout main
   git pull origin main
   git branch -d feature/my-change
   ```

   If the remote branch was not auto-deleted, remove it explicitly:

   ```bash
   git push origin --delete feature/my-change
   ```

## Agent contributors

Automated agents follow the same rules:

- Do not push to `main`
- Push the branch, open a PR, and stop — humans merge
- After merge, do not recreate or keep stale branches on `origin`