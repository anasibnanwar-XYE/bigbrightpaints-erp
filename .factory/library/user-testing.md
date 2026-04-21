# User Testing

Validation surfaces, setup rules, and concurrency guidance for the platform-owner-first ERP hard-cut mission.

**What belongs here:** how validators should exercise the compose-backed backend, capture email artifacts, and verify repo-static contract truth.

---

## Validation Surfaces

### 1. API runtime validation
- **Type:** backend HTTP/API validation against the compose-backed runtime
- **Base URL:** `http://localhost:8081`
- **Actuator:** `http://localhost:9090/actuator/health`
- **MailHog UI/API:** `http://localhost:8025`
- **Tools:** `curl`, MailHog HTTP endpoints, targeted DB inspection only when a feature explicitly requires it

Representative journeys:
- platform login -> `GET /api/v1/auth/me` -> `/api/v1/superadmin/**`
- tenant onboarding -> MailHog credential capture -> tenant login -> `GET /api/v1/auth/me`
- shared self-service profile/password/reset/MFA flows
- platform support workspace and admin-recovery exception flows

### 2. Targeted JVM proof packs
- **Type:** Maven unit/integration/truthsuite coverage for risky refactors and contracts
- **Working directory:** `erp-domain/`
- **Tools:** `mvn`, manifest commands from `.factory/services.yaml`
- **Use when:** a feature changes auth/security, company control plane, onboarding, shared self-service, platform support/recovery, or public contract DTO/controller behavior

### 3. Repo-static contract/governance validation
- **Type:** read-only repo inspection
- **Tools:** `Read`, `Grep`, `LS`, contract guard scripts
- **Use when:** a milestone validates OpenAPI/docs parity, retired routes, canonical docs, or docs-only cleanup behavior

## Validation Concurrency

- **api-runtime:** max concurrent validators **1**
- **jvm-proof:** max concurrent validators **1**
- **repo-static:** max concurrent validators **2**

Rationale:
- the dry run showed the compose-backed runtime is workable, but the user explicitly requested low-resource overnight execution on the current machine
- MailHog, onboarding, and shared runtime state are all easier to keep deterministic when API validation is serialized
- Maven proof packs share the same checkout and `target/` tree; serialize them
- repo-static checks are read-only and can safely run in small parallel batches

## Setup Steps

1. Run `.factory/init.sh`.
2. Start the approved compose boundary from `.factory/services.yaml` if it is not already healthy.
   - Local validator note: the checked-out `.env` may override compose ports to `18081/19090/18025/15673/15674`; for mission validation, explicitly export `APP_PORT=8081`, `MANAGEMENT_PORT=9090`, `MAILHOG_SMTP_PORT=1025`, `MAILHOG_UI_PORT=8025`, `RABBIT_PORT=5672`, and `RABBIT_MANAGEMENT_PORT=15672` when invoking `docker compose` so the runtime matches the approved boundary.
   - If `DD_API_KEY` interpolation blocks compose startup, set `DD_API_KEY=local-dev-dd-api-key` for the local validation command rather than hunting for a real secret.
3. Verify:
   - `http://localhost:9090/actuator/health`
   - `http://localhost:9090/actuator/health/readiness`
   - `GET http://localhost:8081/api/v1/auth/me` returning `200`, `401`, or `403`
   - Readiness plus `auth/me` is sufficient to proceed with targeted runtime validation when the aggregate health endpoint is temporarily `503` because an optional side service such as MailHog is not bound yet.
4. For onboarding or reset flows, use MailHog to capture the actual email artifact instead of inventing credentials or reset tokens.
5. Create isolated tenant codes per validator run when mutating shared runtime state.
6. For cleanup/doc assertions, skip runtime startup and inspect repo state directly.

## MailHog Guidance

- Prefer MailHog for:
  - onboarding first-admin temporary credentials
  - public forgot-password emails
  - platform-issued admin recovery reset emails
- Treat the email artifact as validation evidence.
- When proving latest-token-wins behavior, capture both email deliveries and demonstrate only the newest token succeeds.

## High-Signal Proof Packs

### Platform / onboarding / auth
- `platform-control-proof`
- `onboarding-proof`
- `self-service-proof`

### Contract cleanup
- `cleanup-contract-proof`
- `contract-guards`
- `docs-lint`

## Validator Guidance

- Prefer end-to-end API proof over source inspection when an assertion names a runtime route.
- For target-state routes introduced by this mission, do not fall back to retired aliases just because they still exist in pre-hard-cut code before the relevant milestone lands.
- Pair privacy-wall exception proofs with denied platform-owner calls to unrelated tenant business APIs.
- Pair canonical-route success proof with retired-route absence or fail-closed proof whenever a milestone retires an alias.
- Keep overnight validation low-resource: reuse the existing healthy runtime, serialize runtime-heavy checks, and prefer the narrowest proof pack that still satisfies the assertion.

## Flow Validator Guidance: api-runtime

- Use only the approved compose-backed runtime on `localhost:8081`, `localhost:9090`, and `localhost:8025`; do not start alternate app ports or sidecar runtimes.
- If the delegated `user-testing-flow-validator` helper fails to launch or returns no output, fall back to direct in-session validation, write the flow report and synthesis artifacts explicitly, and record the helper failure as environment/tooling friction rather than silently skipping assertions.
- For `platform-truth-rails-and-privacy-wall`, keep the auth/privacy-wall assertions in a single validator lane because they share the same login identities, MailHog inbox, and tenant-scoped authorization surface.
- Prefer read-only discovery of live actors (existing credentials, MailHog artifacts, or targeted read-only DB/container inspection) before attempting any mutation. If password-reset flows are required to regain access to an existing seeded actor, use the supported API + MailHog path and record which identity was changed.
- Reuse one platform-scoped superadmin session for `auth/login -> auth/me -> /api/v1/superadmin/**` continuity checks, and keep denied-route probes to non-destructive reads from tenant-admin, tenant business, portal, dealer-portal, sales, accounting, or factory route families.
- Do not modify tenant lifecycle, limits, modules, billing plans, or onboarding state in this lane; this milestone only needs auth continuity and privacy-wall denial proof.
- Evidence should capture the exact auth scope used for each actor (`PLATFORM`, tenant code, or other scope) so failed/denied results can be tied back to the intended boundary.
- Local privacy-wall validation may find seeded actors in the database without discoverable plaintext passwords in `.env`; before mutating anything else, stabilize only the dedicated local validation actors/fixtures you need for the lane and record which identities were reset or inserted.
- MailHog reset emails in this runtime can mask the reset token inside the HTML link. Treat MailHog as the primary evidence artifact, but if token-level setup is required for local-only validation recovery, document the runtime-state inspection path you used instead of guessing credentials or inventing tokens.

## Known Constraints

- This repository may still contain pre-hard-cut docs/OpenAPI truth until the relevant cleanup milestones land.
- Some target-state routes in the validation contract do not exist yet; validators should only expect them after the features that claim those assertions complete.
- Runtime seed state can drift; prefer onboarding fresh tenants and MailHog-captured credentials over assuming long-lived seed actors are correct.
