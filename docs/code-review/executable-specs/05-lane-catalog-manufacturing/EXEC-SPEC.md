# Lane 05 Exec Spec

## Covers
- Backlog row 5
- `MFG-02`, `MFG-03`, `MFG-04`, `MFG-05`, `MFG-06`, `MFG-07`, `MFG-08`, `MFG-09`

## Why This Lane Is Narrowed
Manufacturing has too many hidden authority paths. This lane must reduce them without collapsing SKU, costing, or dispatch assumptions that other modules rely on.

## Primary Review Sources
- `flows/manufacturing-inventory.md`
- `flows/finance-reporting-audit.md`

## Primary Code Hotspots
- `ProductionCatalogService`
- `CatalogService`
- `RawMaterialService`
- `OpeningStockImportService`
- `ProductionLogService`
- `FinishedGoodsWorkflowEngineService`

## Related Execution Packages
- this lane is the remediation wrapper for the broader catalog/material authority-migration package at `/home/realnigga/Desktop/mission-control-refactor-specs/catalog-materials-refactor/README.md`
- use the external package for the detailed authority-migration sequence
- use this lane to decide when that refactor is allowed to move relative to the rest of the production remediation program

## Entry Criteria
- Lane 03 boundary decisions are stable enough that product, material, and FG authority work will not redefine accounting truth by accident
- Lane 02 auth and reset work is not sharing the same slice
- `MFG-09` and the related stock-bearing create blockers are reproducible on the current branch
- the external catalog/material package remains the source of truth for the detailed Track 0-6 sequence

## Produces For Other Lanes
- safe start conditions for the catalog and manufacturing refactor
- one authority map for product, raw material, finished good, import, and repair paths
- frontend and operator cutover rules that do not surprise downstream teams

## Packet Sequence

### Packet 0 - baseline blockers and authority inventory
- close `MFG-09` by making stock-bearing create flows fail only on true setup defects, not on accidental seed readiness gaps
- classify each current path as canonical, wrapper, admin-only, or feature-flagged
- output: authority map aligned with Track 0 of the external package

### Packet 1 - defaults, bootstrap, and product-authority preparation
- keep `CompanyDefaultAccountsService.requireDefaults()` strict
- make normal create flows surface actionable setup defects instead of bypassing real prerequisites
- align legacy catalog writes so they can delegate toward one canonical product path
- output: bootstrap-hardening proof and product-authority prep packet

### Packet 2 - material-kind contract and raw-material authority convergence
- consume the external package Tracks 2-3 to separate production versus packaging material rules
- remove bidirectional sync from normal write behavior and make one write path dominant
- output: material-authority packet and regression proof against opening stock and purchasing callers

### Packet 3 - import, receipt, and direct finished-good guardrails
- ensure opening stock, admin repair, raw-material intake, and direct FG create cannot recreate removed authorities
- consume the external package Tracks 4-5 for receipt, import, and direct-write alignment
- output: guardrail packet for imports, receipts, and FG surfaces

### Packet 4 - packaging workbench and frontend cutover are gated follow-on work
- packaging workflow convergence and frontend cutover wait until Packets 0-3 are exit-clean
- consume the external package Tracks 6-7 only after the authority map is stable
- output: explicit go or no-go note for packaging and frontend cutover

## Frontend And Operator Handoff
- frontend cutover follows the external Track 6 gate, not ad hoc endpoint assumptions
- operators get one authority map showing which paths remain canonical, wrapped, admin-only, or disabled
- if a path stays transitional, the handoff must say who still calls it and when it can be removed

## Stop-The-Line Triggers
- packaging workflow redesign starts before product and material authority are stable
- direct FG create, opening stock import, or manual intake still act as independent authority paths while old writers are removed
- `CompanyDefaultAccountsService.requireDefaults()` is weakened just to make create flows pass
- external package sequencing is skipped and the lane starts mixing several authority migrations in one PR

## Must Not Mix With
- packaging workbench convergence
- purchasing boundary redesign
- sales dispatch redesign

## Must-Pass Evidence
- production catalog invariant tests
- opening stock idempotency tests
- manufacturing WIP costing tests
- finished-good batch gating tests

## Rollback
- revert the narrow authority or bootstrap fix while preserving the surviving canonical path and existing stock data

## Exit Gate
- stock-bearing product and FG creation do not depend on accidental bootstrap state
- opening stock and production replay do not recreate deprecated authority paths
- packaging-cost behavior is explicit rather than optional mystery behavior
