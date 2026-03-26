---
name: milestone-shareout-worker
description: Checkpoint worker that summarizes a sealed ERP-38 milestone and returns control so the orchestrator can push for remote bot review.
---

# Milestone Shareout Worker

NOTE: Startup and cleanup are handled by `worker-base`. This skill defines the WORK PROCEDURE.

## When to Use This Skill

Use only for the deliberate ERP-38 checkpoint features that pause the mission after a milestone seals so the orchestrator can:

- push the feature branch to origin
- wait for remote bot review
- decide whether new fix features are needed before the next milestone begins

This worker does not implement source changes.

## Required Skills

None.

## Work Procedure

### Step 1: Confirm checkpoint scope
1. Read `mission.md`, mission `AGENTS.md`, the feature description, and `.factory/services.yaml`.
2. Confirm the feature is a milestone checkpoint/shareout only.
3. Do not edit application code, tests, docs, OpenAPI, or config.

### Step 2: Gather milestone evidence
1. Run the shareout/review commands approved in `.factory/services.yaml`.
2. Collect the current branch name, `git status`, commits since `origin/main`, and the relevant validation artifact paths for the just-sealed milestone.
3. If the prior milestone is not actually sealed yet, return that fact clearly.

### Step 3: Return control to orchestrator
1. Summarize what milestone was just completed and where the validation evidence lives.
2. State whether the branch appears ready for orchestrator push/review.
3. Return to orchestrator rather than attempting any implementation work.

## Example Handoff

```json
{
  "salientSummary": "Prepared the setup-truth milestone shareout. The branch is clean, the milestone validation artifacts are present, and control should return to the orchestrator so the branch can be pushed for remote bot review before batch-pack work starts.",
  "whatWasImplemented": "No source files were changed. I gathered the checkpoint evidence for the sealed setup-truth milestone: current branch state, commit summary since origin/main, and the validation artifact directories for the milestone scrutiny and user-testing runs.",
  "whatWasLeftUndone": "",
  "verification": {
    "commandsRun": [
      {
        "command": "ROOT=$(git rev-parse --show-toplevel) && git -C \"$ROOT\" status --short --branch",
        "exitCode": 0,
        "observation": "Branch state captured for orchestrator review."
      },
      {
        "command": "ROOT=$(git rev-parse --show-toplevel) && git -C \"$ROOT\" log --oneline --decorate origin/main..HEAD",
        "exitCode": 0,
        "observation": "Milestone commit range captured for push/shareout."
      }
    ],
    "interactiveChecks": []
  },
  "tests": {
    "added": []
  },
  "discoveredIssues": []
}
```

## When to Return to Orchestrator

- Always, once the milestone shareout is ready
- The previous milestone is not actually sealed/validated yet
- The branch is dirty in ways the orchestrator should inspect before pushing
