# Timeline

## 2026-02-28

- Created ticket and aligned scope with product direction:
  - sales role restricted to sales work only
  - dispatch finalization factory-owned
  - approved credit increase request updates dealer limit
  - sales gets dedicated dashboard contract
- Implemented backend slices:
  - sales dashboard DTO + endpoint + repository counters
  - dispatch confirm endpoint role-gated to factory/admin + `dispatch.confirm`
  - credit approval mutates dealer credit limit with lock/fail-closed checks
  - promotion image URL support + Flyway `V22__promotions_image_url.sql`
- Added/updated regression tests:
  - service: dashboard aggregation, credit approval mutation/fail-closed, promotion image mapping
  - integration: dashboard metrics, dispatch role gate, credit-approval limit increment
- Started DB/QA independent review step before merge confirmation.
