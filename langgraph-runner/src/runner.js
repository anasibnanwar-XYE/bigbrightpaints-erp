import "dotenv/config";
import fs from "node:fs/promises";
import path from "node:path";

import { getConfig } from "./config.js";
import { createTools } from "./tools.js";
import { createAgentStepGraph } from "./agent_step_graph.js";
import { fileExists, readJson, writeJsonAtomic } from "./state.js";

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

function extractJsonObject(text) {
  const trimmed = String(text ?? "").trim();
  const fenceMatch = trimmed.match(/```(?:json)?\\s*([\\s\\S]*?)\\s*```/i);
  const candidate = fenceMatch ? fenceMatch[1].trim() : trimmed;
  const firstBrace = candidate.indexOf("{");
  const lastBrace = candidate.lastIndexOf("}");
  if (firstBrace === -1 || lastBrace === -1 || lastBrace <= firstBrace) return null;
  return candidate.slice(firstBrace, lastBrace + 1);
}

function getLastAction(state) {
  for (let i = (state?.messages?.length || 0) - 1; i >= 0; i -= 1) {
    const msg = state.messages[i];
    if (msg?.role !== "assistant") continue;
    if (typeof msg.content !== "string") continue;
    const json = extractJsonObject(msg.content);
    if (!json) continue;
    try {
      return JSON.parse(json);
    } catch {
      continue;
    }
  }
  return null;
}

function parseFlags(argv) {
  return {
    once: argv.includes("--once"),
  };
}

async function listInbox(inboxDir) {
  const entries = await fs.readdir(inboxDir, { withFileTypes: true });
  return entries
    .filter((e) => e.isFile() && (e.name.endsWith(".json") || e.name.endsWith(".processing")))
    .map((e) => e.name)
    .sort();
}

async function processTask({ app, cfg, inboxDir, outboxDir, stateDir }, filename) {
  const inboxPath = path.join(inboxDir, filename);
  const taskId = filename.replace(/\.processing$/, "").replace(/\.json$/, "");
  const processingPath = path.join(inboxDir, `${taskId}.processing`);
  const statePath = path.join(stateDir, `${taskId}.json`);

  if (filename.endsWith(".json")) {
    await fs.rename(inboxPath, processingPath);
  }

  console.log(`[${taskId}] running (maxSteps=${cfg.maxSteps})`);

  const task = await readJson(processingPath);
  const goal = task.goal || task.prompt || task.input;
  if (!goal) {
    await writeJsonAtomic(path.join(outboxDir, `${taskId}.error.json`), {
      error: "Task JSON must include { goal } (or prompt/input)",
      task,
    });
    await fs.rename(processingPath, path.join(outboxDir, `${taskId}.badtask.json`));
    console.log(`[${taskId}] invalid task (missing goal)`);
    return;
  }

  const initialState = { goal, messages: [{ role: "user", content: goal }], action: null, done: false, final: "" };
  let state = (await fileExists(statePath)) ? await readJson(statePath) : initialState;

  for (let i = 0; i < cfg.maxSteps && !state.done; i += 1) {
    state = await app.invoke(state);
    await writeJsonAtomic(statePath, state);
    const lastAction = getLastAction(state);
    if (lastAction?.type === "tool") {
      console.log(`[${taskId}] step ${i + 1}: tool ${lastAction.name}`);
    } else if (lastAction?.type === "final") {
      console.log(`[${taskId}] step ${i + 1}: final`);
    } else {
      console.log(`[${taskId}] step ${i + 1}: progressed`);
    }
  }

  if (!state.done) {
    console.log(`[${taskId}] paused (not done yet)`);
    return;
  }

  await fs.mkdir(outboxDir, { recursive: true });
  await fs.writeFile(path.join(outboxDir, `${taskId}.md`), `${state.final}\n`, "utf8");
  await fs.rename(processingPath, path.join(outboxDir, `${taskId}.json`));
  console.log(`[${taskId}] done -> tasks/outbox/${taskId}.md`);
}

async function main() {
  const flags = parseFlags(process.argv.slice(2));
  const cfg = getConfig();

  const baseDir = process.cwd();
  const inboxDir = path.join(baseDir, "tasks/inbox");
  const outboxDir = path.join(baseDir, "tasks/outbox");
  const stateDir = path.join(baseDir, "tasks/state");

  await fs.mkdir(inboxDir, { recursive: true });
  await fs.mkdir(outboxDir, { recursive: true });
  await fs.mkdir(stateDir, { recursive: true });

  const tools = createTools(cfg);
  const app = createAgentStepGraph({ ...cfg, tools });

  console.log(
    `Runner started (model=${cfg.model}, baseURL=${cfg.baseURL}, allowShell=${cfg.allowShell ? "1" : "0"})`,
  );

  do {
    const tasks = await listInbox(inboxDir);
    for (const taskFile of tasks) {
      try {
        await processTask({ app, cfg, inboxDir, outboxDir, stateDir }, taskFile);
      } catch (err) {
        const taskId = taskFile.replace(/\.processing$/, "").replace(/\.json$/, "");
        await writeJsonAtomic(path.join(outboxDir, `${taskId}.runner-error.json`), {
          error: String(err),
        });
        console.log(`[${taskId}] runner error: ${String(err)}`);
      }
    }
    if (flags.once) break;
    await sleep(cfg.pollIntervalMs);
  } while (true);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
