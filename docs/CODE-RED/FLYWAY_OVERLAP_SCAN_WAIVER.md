# Flyway Overlap Scan Waiver (Convergence Migrations)

Reviewed: 2026-02-05

Purpose:
- Allow known, intentional overlaps introduced by convergence migrations
  to pass the Flyway overlap scan under `FAIL_ON_FINDINGS`.
- Keep the overlap scan strict for any **new** unintended duplicates.

Scope:
- Applies only to names listed in `scripts/flyway_overlap_allowlist.txt`.
- No historical migrations are edited; convergence is forward-only.

Rules:
1) Any new overlap must be fixed or explicitly added to the allowlist
   with rationale and review.
2) Allowlist changes require a decision-log entry.

Rationale:
- Convergence migrations intentionally re-declare constraints/indexes
  to bring drifted environments into alignment.
