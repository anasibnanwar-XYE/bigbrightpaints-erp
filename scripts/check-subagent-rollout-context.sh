#!/usr/bin/env bash
set -euo pipefail

# Prints runtime role/model/effort/profile evidence from Codex rollout JSONL logs.
# Usage:
#   scripts/check-subagent-rollout-context.sh <agent-id> [<agent-id> ...]

if [[ $# -lt 1 ]]; then
  echo "usage: $0 <agent-id> [<agent-id> ...]" >&2
  exit 2
fi

CODEX_HOME_DIR="${CODEX_HOME:-$HOME/.codex}"
SESSIONS_DIR="$CODEX_HOME_DIR/sessions"

if [[ ! -d "$SESSIONS_DIR" ]]; then
  echo "missing sessions directory: $SESSIONS_DIR" >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required but not installed" >&2
  exit 1
fi

echo "| agent_id | requested_role | observed_model | observed_effort | observed_instruction_head | log_file |"
echo "| --- | --- | --- | --- | --- | --- |"

for agent_id in "$@"; do
  rollout_file="$(find "$SESSIONS_DIR" -type f -name "rollout-*${agent_id}.jsonl" | sort | tail -n 1)"
  if [[ -z "$rollout_file" ]]; then
    echo "| \`$agent_id\` | not_found | - | - | - | - |"
    continue
  fi

  requested_role="$(jq -r '
    if .type == "session_meta" then
      .payload.source.subagent.thread_spawn.agent_role // empty
    elif .event_type == "session_meta" then
      .session_meta.source.subagent.thread_spawn.agent_role // empty
    else empty end
  ' "$rollout_file" | head -n 1)"

  observed_model="$(jq -r '
    if .type == "turn_context" then
      .payload.model // empty
    elif .event_type == "turn_context" then
      .turn_context.model // empty
    else empty end
  ' "$rollout_file" | head -n 1)"

  observed_effort="$(jq -r '
    if .type == "turn_context" then
      .payload.effort // empty
    elif .event_type == "turn_context" then
      .turn_context.effort // empty
    else empty end
  ' "$rollout_file" | head -n 1)"

  instruction_head="$(jq -r '
    if .type == "turn_context" then
      .payload.developer_instructions // empty
    elif .event_type == "turn_context" then
      .turn_context.developer_instructions // empty
    else empty end
  ' "$rollout_file" | head -n 1 | awk 'NR==1{print; exit}' | sed 's/|/\\|/g')"

  echo "| \`$agent_id\` | \`${requested_role:-unknown}\` | \`${observed_model:-unknown}\` | \`${observed_effort:-unknown}\` | ${instruction_head:-unknown} | \`${rollout_file}\` |"
done
