# Code review foundation

This directory is the review-only workspace for the backend architecture audit described in the mission plan. The foundation files in this feature establish the platform shape, the main controller and module surfaces, and the dependency hotspots that later flow and governance reviews build on.

## Foundation artifacts

| File | Purpose |
| --- | --- |
| [architecture-overview.md](./architecture-overview.md) | Maps the package layout, runtime entrypoints, module boundaries, and shared platform infrastructure with code-path references. |
| [dependency-map.md](./dependency-map.md) | Summarizes cross-module dependencies, ownership seams, and the main coupling hotspots that later reviews should revisit in flow-specific detail. |

## Evidence sources used for this foundation

- `erp-domain/src/main/java/com/bigbrightpaints/erp/**`
- `erp-domain/src/main/resources/application.yml`
- `erp-domain/src/main/resources/application-flyway-v2.yml`
- `erp-domain/src/main/resources/db/migration_v2/**`
- `docker-compose.yml`

## How this foundation is intended to be used

- Start with the architecture overview to understand package ownership, request entrypoints, and cross-cutting infrastructure.
- Use the dependency map before changing shared workflows such as sales, accounting, reports, portal dashboards, or orchestrator flows.
- Treat `docs/code-review/flows/` as the next layer: later review features will add narrative walkthroughs for each major business area on top of this platform foundation.
