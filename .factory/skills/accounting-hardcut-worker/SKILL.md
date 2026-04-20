---
name: accounting-hardcut-worker
description: Mission implementation worker for the accounting truth hard-cut, including decomposition, precision safeguards, RLS-aware cleanup, and proof-driven validation.
---

# Accounting Hard-Cut Worker

NOTE: Startup and cleanup are handled by `worker-base`. This skill defines the WORK PROCEDURE.

## When to Use This Skill

Use for implementation features in this accounting-centralization mission that:
- hard-cut accounting into the singular financial truth boundary
- decompose large accounting services, helpers, listeners, or report seams into smaller reviewable owners
- clean dependency leaks across accounting, inventory, invoice, purchasing, sales, and reports
- tighten settlement, journal, period-close, reconciliation, COA, default-account, supplier-ledger, dealer-ledger, or inventory-accounting behavior
- enforce or preserve tenant scoping, RLS-sensitive data access, approval gates, and accounting audit linkage
- require targeted Maven proof, curl runtime proof, OpenAPI refresh, or canonical frontend-contract cleanup as part of the same packet

This worker owns accounting implementation packets plus the code/test/OpenAPI/doc cleanup needed to leave one canonical accounting truth. It does not own docs-only packets or unrelated HR/payroll feature expansion.

## Required Skills

None.

## Work Procedure

### Step 1: Lock scope, financial invariants, and boundary ownership before editing
1. Read `mission.md`, `validation-contract.md`, mission `AGENTS.md`, `.factory/services.yaml`, `.factory/library/architecture.md`, and `.factory/library/user-testing.md`.
2. Write down the exact business corridor being changed and the canonical accounting owner after the hard cut.
3. Enumerate every touched controller, service, repository, entity, listener, DTO, test, OpenAPI/doc artifact, and report/query seam.
4. Explicitly capture the money and workflow invariants that cannot drift, including as applicable:
   - balanced debits and credits
   - rounding and scale behavior
   - allocation totals and settlement balance
   - ledger/journal/reference linkage
   - period lock/close/reopen rules
   - idempotent replay behavior
   - tenant/company/RLS visibility constraints
5. Trace all dependent consumers before editing. If accounting truth changes, inspect the downstream sales, inventory, invoice, purchasing, and reports callers that depend on it.
6. If the feature would widen into HR/payroll feature work, require two competing accounting owners to survive, or force a public-contract redesign outside the assigned packet, return to orchestrator.

### Step 2: Freeze legacy behavior first with the right safety pattern
1. For messy legacy behavior, internal hard-cuts, decompositions, or unclear accounting flows, use characterization-first.
2. Characterization-first means:
   - identify the current executable behavior and invariants
   - add or extend narrow characterization tests before structural edits
   - run the smallest proving baseline before changing ownership
3. For genuinely new behavior with a clear target contract, use TDD red/green with the narrowest focused test.
4. Prefer the highest-signal proof packs already defined in `.factory/services.yaml`, especially:
   - `commands.targeted-accounting-proof`
   - `commands.targeted-dependent-proof`
   - `commands.targeted-security-proof`
   - `commands.gate-reconciliation` when period-close or reconciliation truth changes
5. If the flow starts outside accounting but ends in accounting truth, add or update at least one proof that demonstrates the originating business action still lands in the correct journal/settlement/report outcome.
6. Record in the handoff whether the packet was characterization-first or TDD-first, and why that choice matched the feature.

### Step 3: Implement the hard cut, not a facade shuffle
1. Make the smallest set of changes that leaves accounting as the singular financial truth owner for the touched corridor.
2. Decompose large services by business responsibility. Do not rename a monolith or hide ownership behind shallow delegate chains.
3. Remove duplicate helper paths, stale facades, obsolete listeners, dead DTOs, retired report shortcuts, and compatibility branches in the touched area.
4. Clean dependency direction deliberately:
   - reports should consume accounting-owned truth instead of bypassing boundaries
   - inventory, invoice, purchasing, and sales may trigger workflows, but accounting owns journal, settlement, period, and reconciliation truth
   - fail closed on company scoping and tenant isolation
5. Stay RLS/security aware:
   - do not widen data visibility through joins, caches, helper lookups, or report readers
   - preserve or strengthen company-scoped access and approval-gated sensitive reporting
   - if you touch accounting tables or tenant filters, re-check both application-level scoping and database/RLS assumptions
6. Preserve money precision and no-drift guarantees:
   - reuse canonical amount/rounding behavior already present in the codebase
   - keep journal totals balanced and replay-safe
   - keep allocation and settlement math reconcilable to the cent/scale used by the system
   - reject or fail closed on ambiguous amount handling instead of silently normalizing incorrect inputs
7. Preserve frontend-usable public contract shapes unless the packet explicitly owns a contract hard-cut. If the public contract changes, update OpenAPI and canonical frontend docs in the same packet.
8. Delete dead or duplicate accounting truth in the touched corridor. A second owner left behind is an incomplete hard cut.

### Step 4: Verify with targeted Maven proof and executable runtime proof
1. Run the narrowest targeted Maven proof packs for the touched seam from `.factory/services.yaml`.
2. After Java changes, run `commands.compile`.
3. Re-run dependent proofs whenever the touched seam feeds another module:
   - `commands.targeted-dependent-proof` for shared business-flow consumers
   - `commands.targeted-security-proof` for tenant binding, RLS, approval gates, or visibility changes
   - `commands.gate-reconciliation` for period-close/reconciliation semantics
4. If runtime/API behavior changed or the packet claims live corridor safety, run the approved compose-backed proof and document the exact `curl` probes and observations.
5. Use `commands.strict-runtime-smoke-check` when the packet needs live boundary evidence.
6. If public endpoint shapes changed, run `commands.openapi-refresh` and the relevant contract proof such as `commands.accounting-frontend-doc-contract-proof`, then inspect the touched paths in `openapi.json` directly.
7. For final PR-readiness, run `commands.gate-fast` unless the orchestrator explicitly scoped the packet to a narrower temporary validation loop.
8. Re-read the diff before handoff and confirm there is no stale duplicate owner, no stale test preserving retired behavior, and no unproven money math assumption.

### Step 5: Produce a stronger accounting handoff
Your handoff must explicitly include:
1. the surviving canonical accounting owner(s) after the packet
2. the duplicate owners/helpers/routes/tests removed or retired
3. the exact money invariants proved and where they were proved
4. the exact workflow linkage proved across journals, settlements, ledgers, audit trails, periods, or reports
5. the downstream modules re-validated because of the change
6. whether RLS/company-scope/approval-gate behavior changed or was re-validated
7. the exact `curl` probes and targeted Maven commands run
8. whether public contract artifacts changed (`yes` or `no`) and which files were refreshed

## Example Handoff

```json
{
  "salientSummary": "Hard-cut dealer receipt and supplier settlement posting onto focused accounting services, retired the duplicate totals/line-drafting helpers, and re-proved that receipt/payment workflows still land in balanced journals without money drift.",
  "whatWasImplemented": "Extracted dealer receipt posting into DealerReceiptPostingService and supplier settlement allocation into SupplierSettlementEngine, removed the legacy duplicate helper branches inside AccountingFacade, tightened company-scoped lookup boundaries used by the reporting read model, and refreshed the touched accounting contract artifacts so the surviving public flow still matches frontend expectations.",
  "whatWasLeftUndone": "Did not absorb unrelated payroll-posting cleanup because it was outside the assigned accounting linkage corridor.",
  "safetyPattern": {
    "mode": "characterization-first",
    "reason": "The legacy settlement corridor had overlapping helper ownership and unclear replay/linkage behavior, so executable characterization coverage was established before decomposition."
  },
  "verification": {
    "commandsRun": [
      {
        "command": "ROOT=$(git rev-parse --show-toplevel) && cd \"$ROOT/erp-domain\" && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='CriticalAccountingAxesIT,CR_DealerReceiptSettlementAuditTrailTest,CR_PurchasingToApAccountingTest,AccountingEndpointContractTest' test",
        "exitCode": 0,
        "observation": "Balanced posting, receipt/payment linkage, and accounting contract behavior passed on the narrowed proof pack."
      },
      {
        "command": "ROOT=$(git rev-parse --show-toplevel) && cd \"$ROOT/erp-domain\" && MIGRATION_SET=v2 mvn -T8 compile -q",
        "exitCode": 0,
        "observation": "Compilation clean after the service decomposition."
      },
      {
        "command": "ROOT=$(git rev-parse --show-toplevel) && cd \"$ROOT\" && bash scripts/gate_reconciliation.sh",
        "exitCode": 0,
        "observation": "Period-close and reconciliation proofs stayed green after the settlement engine cleanup."
      },
      {
        "command": "ROOT=$(git rev-parse --show-toplevel) && cd \"$ROOT\" && bash scripts/gate_fast.sh",
        "exitCode": 0,
        "observation": "Final gate-fast passed for PR-readiness."
      }
    ],
    "interactiveChecks": [
      {
        "action": "Curled the dealer receipt and supplier payment corridors on the compose-backed runtime using the approved localhost boundary.",
        "observed": "Both canonical routes returned successful accounting responses, and the follow-up audit/transaction reads showed the expected linked journal and partner-ledger references."
      }
    ]
  },
  "moneyMathProof": {
    "invariants": [
      "journal debits equal journal credits for every touched posting path",
      "dealer receipt allocation totals equal the applied receipt amount",
      "supplier settlement allocations do not exceed outstanding balances",
      "replay of the same idempotent request does not create a second posting or diverging amount"
    ],
    "result": "All listed invariants were re-proved on the touched corridors."
  },
  "accountingLinkageProof": {
    "survivingOwners": [
      "DealerReceiptPostingService",
      "SupplierSettlementEngine",
      "JournalPostingService"
    ],
    "retiredOwners": [
      "AccountingFacade duplicate settlement math branch",
      "legacy receipt totals helper",
      "stale report-side direct settlement reader"
    ],
    "downstreamModulesRevalidated": [
      "sales",
      "purchasing",
      "reports"
    ],
    "auditAndReferenceEvidence": "Receipt and payment flows preserved journal reference linkage, partner-ledger linkage, and audit trail visibility on the canonical accounting surfaces."
  },
  "accessControlProof": {
    "rlsOrScopeChanged": false,
    "observation": "Company-scoped access and sensitive-report boundaries were re-validated after the decomposition; no visibility widening was introduced."
  },
  "contractArtifactsChanged": {
    "changed": true,
    "files": [
      "openapi.json",
      "docs/frontend-api/accounting.md",
      "docs/frontend-portals/tenant-admin/api-contracts.md"
    ]
  },
  "tests": {
    "added": [
      {
        "file": "erp-domain/src/test/java/com/bigbrightpaints/erp/regression/accounting/DealerReceiptPostingCharacterizationIT.java",
        "cases": [
          {
            "name": "dealerReceipt_replay_returnsSamePostingWithoutMoneyDrift",
            "verifies": "Idempotent replay preserves posting identity and amount correctness."
          },
          {
            "name": "dealerReceipt_allocationsRemainBalancedAfterServiceExtraction",
            "verifies": "Allocation totals remain balanced after the decomposition."
          }
        ]
      }
    ]
  },
  "discoveredIssues": [
    {
      "severity": "medium",
      "description": "A separate report export path still constructs a duplicate partner-aging summary outside the touched accounting owner, but it was outside this packet's assigned corridor.",
      "suggestedFix": "Track a follow-up hard-cut packet to route that export through the surviving accounting read service."
    }
  ]
}
```

## When to Return to Orchestrator

- The feature cannot be completed without preserving two competing accounting owners or adding a compatibility bridge the mission does not allow.
- The packet would require widening into HR/payroll feature work rather than shared accounting-boundary consistency.
- A required public-contract break is broader than the assigned packet and cannot be cleaned across OpenAPI/docs/tests in the same slice.
- Another worker has already claimed the required migration version or the touched accounting truth seam in a conflicting way.
- Required money-math, linkage, tenant-scope, or approval-gate behavior cannot be proven with targeted tests and approved runtime evidence.
- Source-of-truth evidence is contradictory enough that the canonical accounting owner or expected financial outcome is no longer clear.
