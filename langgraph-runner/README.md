# LangGraph Runner (via CLIProxyAPI)

This is a small **LangGraph** agent runner that talks to your local `CLIProxyAPI` (OpenAI-compatible) so you can run autonomous jobs continuously (hours/days) without relying on a single long-lived request.

## Prereqs

- `CLIProxyAPI` running on `http://127.0.0.1:8317`
- Node.js (works on Node 18 here, but Node 20+ is recommended by LangChain packages)

## Setup

```bash
cd langgraph-runner
npm install
cp .env.example .env
```

## Start / restart the proxy

If you want a one-liner from **PowerShell (Windows)**:

```powershell
cd "C:\Users\ASUS\Downloads\CLI BACKEND\langgraph-runner"
.\restart-proxy.ps1
```

If PowerShell blocks scripts on your machine, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\restart-proxy.ps1
```

Or from **WSL/Linux**:

```bash
cd langgraph-runner
./restart-proxy.sh
```

If it still doesn’t connect, run:

```powershell
.\proxy-status.ps1
```

## One-off run

If you run `curl` from PowerShell, use `curl.exe` (PowerShell’s `curl` can be an alias).

```bash
cd langgraph-runner
npm run run -- --id demo --goal "Say exactly: OK"
```

State is saved to `langgraph-runner/tasks/state/demo.json` so you can resume with the same `--id`.

## Long-running daemon (task queue)

1) Drop a task JSON into `langgraph-runner/tasks/inbox/`:

`langgraph-runner/examples/sample-task.json` shows the format.

2) Start the runner:

```bash
cd langgraph-runner
npm start
```

Outputs are written to:

- `langgraph-runner/tasks/outbox/<taskId>.md`
- `langgraph-runner/tasks/state/<taskId>.json` (checkpoint / resume)

## Safety switches

- Shell execution is **off** by default. Enable only if you want the agent to run commands:
  - Set `ALLOW_SHELL=1` in `langgraph-runner/.env`
- File reads/writes are restricted to `AGENT_WORKSPACE_ROOT` (defaults to repo root).
