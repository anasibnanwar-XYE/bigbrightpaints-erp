# AGENT.md — Task 01 Execution Rules (Codex + IntelliJ)

This file is the single source of truth for how the agent operates while executing `/tasks/task-01.md`.
It is designed to prevent stalling, enforce resumability, and keep changes safe and test-backed.

---

## 0) Inputs & Priority Order

The agent must obey files in this priority order:

1. `/tasks/task-01.md` (the task spec; EPICs + milestones + gates)
2. `/SCOPE.md` (big picture, boundaries, invariants, DoD)
3. `/RULES.md` (branch, tests, commit/push, docs)
4. `/HYDRATION.md` (live run state, findings, decisions, resume pointer)
5. `docs/codex-cloud-ci-debugging-plan.md` (async + debugging discipline)

If a conflict exists, the higher priority file wins.

---

## 1) Never Stall Policy (Async‑First)

### The agent must never be idle
If a step would block (e.g., `mvn verify`), the agent must:
1) run it asynchronously (see below),
2) record PID/log in `HYDRATION.md`,
3) continue with the next independent milestone or “safe work” bucket.

### Safe work buckets (allowed while async full-suite runs)
- Add/extend regression tests for deploy blockers (EPIC 01–04).
- Documentation work (task/spec updates, scope/acceptance criteria).
- Code reading + call-chain mapping to confirm root cause before changing logic.
- Small refactors behind tests only (no formatting sweeps).

### If blocked by missing info
Record in `HYDRATION.md`:
- blocker
- evidence needed (file, failing test output, log snippet)
- minimal next action
Then move to the next independent milestone.

---

## 2) Branch + Change Discipline

- Work ONLY on branch: `predeploy-blockers-v1`.
- Never commit unrelated formatting sweeps.
- Prefer minimal diffs that fix the root cause and add regression coverage.
- No new endpoints, no new business features.

Commit style:
- Small, scoped commits per milestone.
- Commit message format:
  - `task01:<epic>.<milestone> <summary>`
- Push after every milestone (no force push).

---

## 3) Required Async Procedure (Codex Cloud Rule)

### Full suite gate must be async (never foreground)
Run:
```bash
nohup bash -lc 'cd erp-domain && mvn -B -ntp verify' > /tmp/task01-verify.log 2>&1 & echo $! > /tmp/task01-verify.pid

tail -n 200 /tmp/task01-verify.log
ps -p "$(cat /tmp/task01-verify.pid)" >/dev/null && echo "VERIFY RUNNING" || echo "VERIFY FINISHED"
```

