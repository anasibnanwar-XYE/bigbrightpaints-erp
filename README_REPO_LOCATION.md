# Repo Location Note

This folder (`/home/realnigga/Desktop/CLI_BACKEND_epic04`) contains the active backend worktree for **CLI_BACKEND_epic04**.

If you're running commands (tests/build/deploy scripts), run them from this repo root (the folder that contains
`scripts/verify_local.sh` and `erp-domain/`).

Notes:
- `.github/` is a hidden folder on Linux (dot-folder). In your file manager press `Ctrl+H` to show hidden files.
- If `git rev-parse --show-toplevel` returns `/home/realnigga`, you are inside an accidental git repo at `$HOME` and this
  folder is not a proper git checkout/worktree. Fix by re-cloning into `CLI_BACKEND_epic04/` (do not delete anything
  under pressure).
