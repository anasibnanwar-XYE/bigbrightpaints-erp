# Codex Cloud Agent Guide

This repository is used with Codex Cloud and local worktrees. Follow the rules
below to avoid data loss and ensure repeatable CI/debugging runs.

## Scope
- Primary service: `erp-domain`
- CI entrypoint: `mvn -B -ntp verify` (see `.github/workflows/ci.yml`)
- Debugging plan: `docs/codex-cloud-ci-debugging-plan.md`

## Safety rules (do not deviate)
- Do not use `git push --force` unless explicitly approved; if forced, use
  `--force-with-lease` only after a successful rebase.
- Do not delete branches or rewrite history without explicit approval.
- Do not touch other worktrees/branches unless explicitly instructed.
- Do not discard local changes unless instructed; prefer `git stash` with a clear
  message.
- If anything unexpected appears (unknown diffs, missing files), stop and report.

## Long-running tasks (Codex Cloud)
- Run long tasks asynchronously and track them; do not block the session.
- Use the async procedure in `docs/codex-cloud-ci-debugging-plan.md`.

## CI debugging workflow
1. Identify the first failing test and capture the stack trace.
2. Download Surefire artifacts and confirm all failing tests.
3. Classify failure type (logic, nondeterministic, infra/config, data/setup).
4. Reproduce locally when possible.
5. Propose and implement fixes only after root cause is verified.

## Accounting verification
- Follow the accounting verification checklist in
  `docs/codex-cloud-ci-debugging-plan.md`.
- Do not introduce new features while fixing accounting logic.

## Output expectations
- Summarize changes with file paths.
- Provide reproduction steps and test commands.
- If blocked, state the exact blocker and what evidence is needed.
