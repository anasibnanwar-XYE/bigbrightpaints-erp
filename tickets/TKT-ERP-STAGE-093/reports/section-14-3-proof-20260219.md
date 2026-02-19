# Section 14.3 Proof Pack (TKT-ERP-STAGE-093)

- release_head_sha: `9624998ea254759d210150dd4200c249ed5c954f`
- canonical_base_ref: `harness-engineering-orchestrator`
- canonical_base_sha: `9624998ea254759d210150dd4200c249ed5c954f`

## Commands

1. `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home && export PATH="$JAVA_HOME/bin:$PATH" && bash scripts/gate_release.sh`
2. `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home && export PATH="$JAVA_HOME/bin:$PATH" && bash scripts/gate_reconciliation.sh`

## Results

- `gate_release`: PASS (`release-gate-traceability.json`, `artifact_count=17`, canonical base verified).
- `gate_reconciliation`: PASS (`gate-reconciliation-traceability.json`, `artifact_count=4`, canonical base verified).

## Immutable Evidence Artifacts

- `artifacts/gate-release/release-gate-traceability.json`
- `artifacts/gate-reconciliation/gate-reconciliation-traceability.json`
- `/tmp/tkt093_base_gate_release.log`
- `/tmp/tkt093_base_gate_reconciliation.log`
