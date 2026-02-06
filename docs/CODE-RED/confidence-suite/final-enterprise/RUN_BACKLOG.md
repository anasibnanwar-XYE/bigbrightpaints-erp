# Run Backlog (Autonomous)

Last refreshed: 2026-02-06T07:03:57Z

## In Progress
- [x] Final evidence collation + rerun commands summary.

## Next
- [ ] Optional: clean non-versioned `artifacts/` before commit.

## Done
- [x] gate-fast local pass with explicit `DIFF_BASE` and PR-correct default merge-base logic retained.
- [x] gate-core pass with runtime class coverage thresholds.
- [x] gate-reconciliation pass with reconciliation summary artifact.
- [x] gate-quality pass with actionable mutation signal + full 20-run flake window.
- [x] gate-release pass with strict verify and fresh+upgrade migration matrix using isolated postgres.
- [x] release migration matrix credential fallback hardened (`PG*` -> `SPRING_DATASOURCE_*` -> defaults).
