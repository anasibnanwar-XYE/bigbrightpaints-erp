---
name: packet-governance-worker
description: Workflow, preflight-review, packet-governance, and release-gate worker for Factory-droid remediation packets.
---

# Packet Governance Worker

NOTE: Startup and cleanup are handled by `worker-base`. This skill defines the WORK PROCEDURE.

## When to Use This Skill

Use for features that:
- preserve docs-only or packet-only branch state before remediation work starts
- review merged changes on `Factory-droid` and classify remaining packet scope
- complete validation-first bundles and packet templates
- verify release-gate, rollback, branch-lineage, and handoff readiness
- must return to the orchestrator when base-branch review, PR preparation, or merge action is required

This worker does not perform broad product-code implementation unless the feature explicitly says so.

## Work Procedure

### Step 1: Read the packet controls first
1. Read the feature description, preconditions, expectedBehavior, and verificationSteps carefully.
2. Read mission `AGENTS.md`, `.factory/services.yaml`, and `.factory/library/packet-governance.md` before touching git state.
3. Read the relevant execution controls in `docs/code-review/executable-specs/`:
   - `PACKET-TEMPLATE.md`
   - `VALIDATION-FIRST-BUNDLE.md` when the feature is `prove first`
   - `RELEASE-GATE.md`
   - the active lane `EXEC-SPEC.md`
4. Confirm the active base is `Factory-droid` and identify whether the feature is allowed to modify code, docs, packet artifacts, or only review notes.

### Step 2: Make git state explicit
1. Run `git status --short --branch` before any branch or packet action.
2. If the feature owns docs-preservation work, isolate the docs-only changes exactly as the feature requires before any packet code work begins.
3. Never push, merge, or rewrite history. If the feature reaches that boundary, stop and return to the orchestrator.
4. Keep packet scope attributable: if unrelated dirty files appear, leave them alone and record them.

### Step 3: Review or prepare the packet
1. For preflight-review features, inventory the merged change set on `Factory-droid`, compare it to the executable-spec package, and classify what remains open.
2. For validation-first features, complete the exact bundle with commands, evidence paths, verdict, reviewer sign-off, and lane-owner acknowledgement.
3. For release-gate features, fill in the packet/release-gate evidence using the real diff and verification outputs from the current packet, including `docs/frontend-update-v2/**` updates or explicit no-op entries when frontend-relevant surfaces were touched.
4. For lineage or cross-packet audits, verify that each packet still traces back to `Factory-droid` and that downstream packets are not consuming unreviewed earlier work.

### Step 4: Verify with high-signal evidence
1. Run the smallest useful git and validation commands needed to prove the packet state.
2. When runtime evidence is required, use the approved compose-backed `v2` runtime path from `.factory/services.yaml`.
3. Treat degraded actuator health as a note, not a waiver; keep route-level/API evidence and targeted tests as the real proof.
4. Record exact commands, outputs, and file references in the handoff.

### Step 5: Return control at the right boundary
1. If the packet is ready for base-branch review or merge handling, return to the orchestrator instead of improvising.
2. If the packet is blocked by missing proof, missing rollback notes, or scope creep, return to the orchestrator with the exact blocker.
3. If the packet is fully complete and no orchestration action is required, say so explicitly and provide the final evidence bundle.

## Example Handoff

```json
{
  "salientSummary": "Preserved the docs-only working set before sync, reviewed the merged auth/company/admin hardening on Factory-droid, and prepared a narrow merge-gate packet boundary note. The packet is ready for orchestrator review because merge handling and base-branch judgment are required next.",
  "whatWasImplemented": "Created the packet-governance baseline by isolating docs/code-review and .factory/droids work onto a docs-only branch, synced local Factory-droid, and reviewed the merged auth/company/admin change set against the executable-spec package. The review confirmed the remaining merge-gate regressions and attached the packet template / release-gate notes required before opening the fix packet.",
  "whatWasLeftUndone": "Did not merge or push the packet, because base-branch review and merge handling belong to the orchestrator.",
  "verification": {
    "commandsRun": [
      {"command": "git status --short --branch", "exitCode": 0, "observation": "Captured the dirty-tree state before preservation and before review handoff."},
      {"command": "git fetch origin Factory-droid", "exitCode": 0, "observation": "Synced the integration base for packet review."},
      {"command": "git diff --stat origin/Factory-droid~15..origin/Factory-droid -- erp-domain/src/main/java erp-domain/src/test/java openapi.json .factory docs/code-review", "exitCode": 0, "observation": "Reviewed the merged auth/company/admin surface and confirmed packet boundaries."}
    ],
    "interactiveChecks": [
      {"action": "Compared the merged change set to PACKET-TEMPLATE.md, RELEASE-GATE.md, and the lane EXEC-SPEC.", "observed": "The remaining work fits a narrow merge-gate packet; merge action itself must return to the orchestrator."}
    ]
  },
  "tests": {
    "added": []
  },
  "discoveredIssues": [
    {
      "severity": "high",
      "description": "Token revocation still truncates timestamps before comparison, which leaves same-millisecond pre-revocation access tokens usable.",
      "suggestedFix": "Keep it in the merge-gate packet and close it with a targeted TokenBlacklistService regression test before merge review."
    }
  ]
}
```

## When to Return to Orchestrator

- The feature reaches base-branch review, PR preparation, or merge handling.
- A packet is wider than one lane / one remediation slice.
- Required packet controls (validation-first bundle, release gate, rollback notes, named roles) are incomplete and need orchestration judgment.
- Git state is too dirty or ambiguous to preserve packet attribution safely.
- Runtime/tooling issues prevent collection of the evidence the packet promised.
