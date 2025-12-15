import path from "node:path";

export function getConfig() {
  const baseURL = process.env.OPENAI_BASE_URL || "http://127.0.0.1:8317/v1";
  const apiKey = process.env.OPENAI_API_KEY || "dummy";
  const model = process.env.OPENAI_MODEL || "gpt-5.2(xhigh)";

  const workspaceRoot = path.resolve(
    process.cwd(),
    process.env.AGENT_WORKSPACE_ROOT || "..",
  );

  const allowShell = process.env.ALLOW_SHELL === "1";
  const pollIntervalMs = Number(process.env.POLL_INTERVAL_MS || "2000");
  const maxSteps = Number(process.env.MAX_STEPS || "50");

  if (!Number.isFinite(pollIntervalMs) || pollIntervalMs < 250) {
    throw new Error("POLL_INTERVAL_MS must be >= 250");
  }
  if (!Number.isFinite(maxSteps) || maxSteps < 1) {
    throw new Error("MAX_STEPS must be >= 1");
  }

  return {
    baseURL,
    apiKey,
    model,
    workspaceRoot,
    allowShell,
    pollIntervalMs,
    maxSteps,
  };
}

