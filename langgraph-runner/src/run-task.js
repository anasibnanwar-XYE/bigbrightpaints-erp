import "dotenv/config";
import path from "node:path";
import { getConfig } from "./config.js";
import { createTools } from "./tools.js";
import { createAgentStepGraph } from "./agent_step_graph.js";
import { fileExists, readJson, writeJsonAtomic } from "./state.js";

function parseArgs(argv) {
  const args = argv.slice(2);
  const out = { goal: "", id: "adhoc" };
  for (let i = 0; i < args.length; i += 1) {
    const a = args[i];
    if (a === "--id") out.id = args[++i];
    else if (a === "--goal") out.goal = args[++i];
    else if (!a.startsWith("--")) out.goal = [out.goal, a].filter(Boolean).join(" ");
  }
  return out;
}

async function main() {
  const { id, goal } = parseArgs(process.argv);
  if (!goal) {
    console.error("Usage: npm run run:task -- --goal \"...\" [--id task123]");
    process.exit(2);
  }

  const cfg = getConfig();
  const tools = createTools(cfg);
  const app = createAgentStepGraph({ ...cfg, tools });

  const statePath = path.resolve(process.cwd(), "tasks/state", `${id}.json`);
  const state = (await fileExists(statePath))
    ? await readJson(statePath)
    : { goal, messages: [{ role: "user", content: goal }], action: null, done: false, final: "" };

  let current = state;
  for (let i = 0; i < cfg.maxSteps && !current.done; i += 1) {
    current = await app.invoke(current);
    await writeJsonAtomic(statePath, current);
  }

  if (current.done) {
    console.log(current.final);
  } else {
    console.log(`Paused after MAX_STEPS=${cfg.maxSteps}. Resume by re-running with --id ${id}.`);
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

