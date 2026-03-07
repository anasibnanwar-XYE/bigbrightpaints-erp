# Validation-First Evidence Bundle

Use this packet whenever a finding is marked `validation-first`, `prove first`, or `contract check before build`.

No backend implementation slice should open until this bundle is complete.

## 1. Header
- finding IDs
- lane
- implementer
- reviewer
- branch
- target environment

## 2. Claim Under Test
- what route, payload, behavior, or contract is being questioned
- why it might be backend debt, frontend drift, unpublished-route confusion, or an environment-only issue

## 3. Source-Of-Truth Review
- controller or service code inspected
- tests inspected
- `openapi.json` paths inspected
- runtime probe surfaces inspected

## 4. Exact Commands And Artifacts
- exact command list
- exact saved artifact paths
- exact payload snapshots or diff paths

## 5. Verdict
- `confirmed backend defect`
- `contract cleanup only`
- `frontend drift only`
- `unpublished route / not a backend backlog item`
- `environment-only constraint`

## 6. Required Notes
- what evidence supports the verdict
- what work is now allowed
- what work is explicitly not allowed
- whether frontend or operator consumers need a handoff instead of backend changes

## 7. Approval
- reviewer sign-off
- lane owner acknowledgement

## Decision Rule
- if this bundle does not exist, the finding stays in proof mode and does not become a backend implementation packet
