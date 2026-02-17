# Ticket Template

## Metadata
- ticket_id: `<TKT-ID>`
- title: `<title>`
- goal: `<goal>`
- priority: `<high|medium|low>`
- status: `planned`
- base_branch: `<base branch; defaults to current git branch when bootstrapped>`

## Slices
For each slice capture:
- primary_agent
- reviewers
- lane
- branch
- worktree_path
- allowed_scope_paths
- required_checks
- status

## Definition Of Done
- required checks pass
- reviewer evidence present and approved
- no scope violations
- merge completed (if merge mode enabled)
