---
name: accounting-docs-worker
description: Mission documentation worker for client-shareable accounting workflow architecture and backend contract docs used by frontend consumers.
---

# Accounting Docs Worker

NOTE: Startup and cleanup are handled by `worker-base`. This skill defines the WORK PROCEDURE.

## When to Use This Skill

Use for docs-only features in this accounting-centralization mission that:
- create or update client-shareable accounting workflow architecture packets
- refresh backend docs for frontend consumers of accounting, reporting, invoice, purchasing, or related portal surfaces
- document accountant guidance, workflow shortcuts, draft behavior, approval-gated sensitive reporting, or next-step/review guidance once implementation truth exists
- update canonical frontend-facing docs under `docs/frontend-api/**`, `docs/frontend-portals/**`, `docs/flows/**`, `docs/modules/**`, `docs/adrs/**`, or approved governance/library surfaces when the accounting mission changes implemented truth

This worker owns documentation packets only. It does not change application code, tests, or runtime behavior.

## Required Skills

None.

## Work Procedure

### Step 1: Lock source truth and intended audience before editing
1. Read `mission.md`, `validation-contract.md`, mission `AGENTS.md`, `.factory/services.yaml`, `.factory/library/architecture.md`, and any existing target docs before drafting.
2. Identify which audience the packet serves:
   - client-shareable workflow audience
   - frontend engineers consuming backend contracts
   - internal reviewers/governance readers when the packet includes ADR or canonical module/flow truth
3. Treat implemented code, controller annotations, DTOs, tests, and `openapi.json` as source truth. Do not present planned behavior as implemented.
4. If the packet would require code changes to become accurate, return to orchestrator instead of inventing behavior.

### Step 2: Gather evidence from the accounting truth boundary
1. Read the relevant controllers, request/response DTOs, services, tests, mission artifacts, and current canonical docs for the assigned area.
2. For client-shareable workflow docs, capture:
   - actors and permissions
   - entrypoints and prerequisites
   - canonical workflow steps and decision points
   - draft/save/resume behavior if implemented
   - review cues, next-step guidance, and approval gates
   - sensitive-data boundaries and anomaly/review behavior if implemented
3. For frontend-consumer backend docs, capture:
   - exact routes, methods, auth/RBAC assumptions, filters, and pagination
   - request and response shapes from controllers/DTOs/OpenAPI
   - state transitions, blocking conditions, and error envelopes
   - cross-links to module and workflow truth so frontend readers can follow the full corridor
4. If implementation and docs disagree, resolve the disagreement from code/OpenAPI/test evidence and mark older docs as deprecated or stale instead of copying them forward.

### Step 3: Write the packet in the correct canonical lane
1. Put client-shareable workflow architecture in the canonical workflow/module/docs lane requested by the feature, using simple accurate language suitable for external sharing.
2. Put frontend-consumer backend docs in canonical frontend-facing surfaces such as `docs/frontend-api/**` and `docs/frontend-portals/**`.
3. When documenting accounting flows, make the workflow understandable end-to-end:
   - what triggers the flow
   - what the backend needs before it can proceed
   - what the backend records or posts
   - what the user should do next
   - what is blocked, warned, approval-gated, or still pending
4. When documenting backend contracts for frontend consumers, include the exact route strings, relevant roles, request/response semantics, major errors, and important filtering or linkage fields.
5. Keep wording simple and precise. Do not oversell partial implementation or hide sensitive-data restrictions.
6. If the packet changes canonical public-contract documentation, refresh all overlapping canonical surfaces in the same packet instead of leaving contradictory docs behind.

### Step 4: Verify accuracy with docs-focused proof
1. Re-read every edited document against the code, `openapi.json`, mission artifacts, and validation contract.
2. For docs-only accounting packets, run `commands.docs-lint` at minimum.
3. If the packet refreshes frontend-facing contract docs tied to public routes or generated API truth, run `commands.docs-contract-guards` and the matching contract proof such as `commands.accounting-frontend-doc-contract-proof` when the feature requires route/DTO/error-envelope validation.
4. Verify route strings, role names, field names, enum values, example payloads, and approval-gate language directly against source.
5. Do not start services or run runtime mutation flows unless the feature explicitly requires non-doc evidence.
6. If a validator fails on an unrelated pre-existing issue, record it precisely and keep the packet scoped.

### Step 5: Produce a docs handoff that frontend and client readers can trust
Your handoff must include:
1. the exact docs edited
2. the audiences served by each edited doc
3. the code/OpenAPI/test evidence used as source truth
4. the validator commands run and what they proved
5. any implementation gaps that had to remain clearly labeled as pending or unsupported
6. any contradictory or deprecated docs/artifacts discovered while updating the packet

## Example Handoff

```json
{
  "salientSummary": "Updated the accounting workflow packet for client sharing and refreshed the frontend-facing accounting contract docs so tenant-admin consumers now have one accurate source for period close, settlements, and sensitive-report access behavior.",
  "whatWasImplemented": "Refreshed `docs/flows/accounting-period-close.md` as a client-shareable workflow narrative covering prerequisites, close request, approval, reopen constraints, blockers, and next-step guidance. Updated `docs/frontend-api/accounting.md` and `docs/frontend-portals/tenant-admin/api-contracts.md` so frontend consumers now have the exact accounting routes, role assumptions, request/response expectations, and sensitive-report gating notes that match the current backend truth.",
  "whatWasLeftUndone": "Did not document draft anomaly-review UX beyond the implemented backend warning surfaces because the paid-toggle workflow is not yet fully delivered in code.",
  "audiences": [
    "client-shareable workflow readers",
    "frontend engineers consuming accounting backend contracts"
  ],
  "verification": {
    "commandsRun": [
      {
        "command": "ROOT=$(git rev-parse --show-toplevel) && cd \"$ROOT\" && bash ci/lint-knowledgebase.sh",
        "exitCode": 0,
        "observation": "Canonical docs formatting, links, and knowledgebase lint remained valid."
      },
      {
        "command": "ROOT=$(git rev-parse --show-toplevel) && cd \"$ROOT\" && bash ci/check-enterprise-policy.sh && bash ci/check-architecture.sh && bash ci/check-orchestrator-layer.sh && bash scripts/guard_openapi_contract_drift.sh",
        "exitCode": 0,
        "observation": "Docs/governance checks and OpenAPI drift guard stayed aligned with the refreshed accounting contract docs."
      },
      {
        "command": "ROOT=$(git rev-parse --show-toplevel) && cd \"$ROOT/erp-domain\" && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='SettlementControllerIdempotencyHeaderParityTest,AccountingEndpointContractTest,AccountingApplicationExceptionAdviceTest,AccountingApplicationExceptionResponsesTest,OpenApiSnapshotIT' test",
        "exitCode": 0,
        "observation": "The documented accounting routes, validation envelopes, and OpenAPI snapshot matched the executable backend contract."
      }
    ],
    "interactiveChecks": [
      {
        "action": "Compared every documented route, role name, and example field against controller annotations, DTOs, and `openapi.json`.",
        "observed": "The refreshed docs now match the implemented accounting surfaces and avoid overstating unfinished anomaly-toggle behavior."
      }
    ]
  },
  "sourceEvidence": {
    "controllers": [
      "JournalController",
      "SettlementController",
      "PeriodController",
      "StatementReportController"
    ],
    "tests": [
      "AccountingEndpointContractTest",
      "SettlementControllerIdempotencyHeaderParityTest",
      "OpenApiSnapshotIT"
    ],
    "artifacts": [
      "openapi.json",
      "mission.md",
      "validation-contract.md"
    ]
  },
  "docsEdited": [
    {
      "path": "docs/flows/accounting-period-close.md",
      "audience": "client-shareable workflow readers"
    },
    {
      "path": "docs/frontend-api/accounting.md",
      "audience": "frontend engineers"
    },
    {
      "path": "docs/frontend-portals/tenant-admin/api-contracts.md",
      "audience": "frontend engineers"
    }
  ],
  "discoveredIssues": [
    {
      "severity": "medium",
      "description": "An older reference doc outside the canonical frontend lane still described a retired report-export permission assumption.",
      "suggestedFix": "Retire or refresh that duplicate doc in the next docs packet instead of letting it compete with the canonical accounting contract lane."
    }
  ]
}
```

## When to Return to Orchestrator

- The requested documentation would require inventing accounting behavior that the code, tests, or OpenAPI snapshot do not support.
- The packet actually needs application-code, test, or runtime changes to become accurate.
- The canonical doc lane is ambiguous and updating one surface would leave an unresolvable contradiction with another required surface.
- Source-of-truth evidence is contradictory enough that the implemented route, role, workflow step, or approval behavior is not clear.
- The assigned packet broadens from docs-only work into the accounting implementation hard-cut itself.
