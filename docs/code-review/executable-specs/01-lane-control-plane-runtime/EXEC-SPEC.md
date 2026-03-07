# Lane 01 Exec Spec

## Covers
- Backlog row 1
- `TEN-01`, `TEN-02`, `TEN-04`, `TEN-05`, `TEN-06`, `TEN-07`, `TEN-09`, `TEN-10`, `ADMIN-01`, `ADMIN-09`, `OPS-06`

## Why This Lane Starts First
It defines the target tenant, lifecycle, and runtime policy model that later auth, governance, and operational fixes depend on. If this lane stays ambiguous, every later lane can land on the wrong control surface.

## Primary Review Sources
- `flows/company-tenant-control-plane.md`
- `flows/admin-governance.md`
- `ops-deployment-runtime.md`

## Primary Code Hotspots
- `CompanyContextFilter`
- `CompanyService`
- `TenantLifecycleService`
- `TenantOnboardingService`
- `TenantRuntimeEnforcementService`
- `TenantRuntimePolicyService`

## Entry Criteria
- the current company, super-admin, and admin-settings route family matrix has been captured from controller code and `openapi.json`
- `ADMIN-01` scope drift is reproduced against the real `system_settings` write path
- `TEN-10` cache-invalidation regression is reproducible on the current branch before the fix starts
- no auth-secret migration or accounting-boundary change is sharing the same slice

## Produces For Other Lanes
- a stable company and tenant control contract for Lanes 02, 06, and 07
- one authoritative runtime-policy write and enforcement path
- a route and payload note that frontend and operators can consume without guessing

## Packet Sequence

### Packet 0 - prove the current control-plane contract
- inventory canonical versus alias routes across `CompanyController`, `SuperAdminController`, and `AdminSettingsController`
- pin current filter rebinding, audit scope, and runtime-policy touchpoints
- output: route matrix, alias disposition note, and the completed `TEN-09` validation-first bundle

### Packet 1 - separate global settings from tenant runtime policy
- stop tenant-admin paths from mutating platform-wide `system_settings` keys
- define which keys are global-only and which are tenant-scoped runtime controls
- output: scope map, controller or service guard changes, and regression proof for `ADMIN-01`

### Packet 2 - lock lifecycle vocabulary and runtime-policy truth
- align enum, DTO, migration, and validation handling for lifecycle state
- unify policy load, write, and cache invalidation so canonical updates invalidate immediately
- output: migration compatibility proof for `TEN-02` and regression proof for `TEN-10`

### Packet 3 - converge operator metrics and support-control posture
- reduce tenant-metrics, usage, and runtime-counter drift to one operator story
- decide whether support warnings remain lightweight signals or become managed workflow objects
- publish payload samples or DTO proof for surviving tenant-runtime and portal surfaces
- output: operator handoff note and final `TEN-09` disposition

## Frontend And Operator Handoff
- frontend consumes only the surviving canonical route family or an explicit wrapper
- operator docs state which quota, usage, and runtime surface is authoritative after the lane lands
- if `TEN-09` is confirmed as frontend drift instead of backend debt, the handoff must say that directly

## Stop-The-Line Triggers
- an alias route is deleted before wrapper, redirect, or deprecation posture exists
- global-versus-tenant scope work expands into auth or accounting redesign
- metrics cleanup turns into dashboard feature work instead of contract convergence
- `TEN-09` is treated as a build ticket without current code and OpenAPI proof

## Must Not Mix With
- token hashing or reset-token schema migration
- export approval redesign
- dispatch or purchase posting changes

## Must-Pass Evidence
- control-plane and runtime truth-suite coverage
- company/super-admin controller tests
- OpenAPI parity for surviving control-plane routes
- explicit proof that cache invalidation occurs on canonical runtime-policy updates

## Rollback
- revert route binding and policy-source changes behind the canonical control family without deleting the old alias surface until parity is proven

## Exit Gate
- one canonical control-plane family is the execution target
- runtime-policy updates invalidate cached policy immediately
- dashboards and policy surfaces stop disagreeing about quotas and counts
- `TEN-09` is either re-proven as a backend gap or downgraded to contract cleanup
