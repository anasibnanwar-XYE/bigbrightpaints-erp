import { ChatOpenAI } from "@langchain/openai";
import { Annotation, END, START, StateGraph } from "@langchain/langgraph";
import { AIMessage, HumanMessage, SystemMessage } from "@langchain/core/messages";
import { z } from "zod";

function extractJsonObject(text) {
  const trimmed = String(text ?? "").trim();
  const fenceMatch = trimmed.match(/```(?:json)?\s*([\s\S]*?)\s*```/i);
  const candidate = fenceMatch ? fenceMatch[1].trim() : trimmed;

  const firstBrace = candidate.indexOf("{");
  const lastBrace = candidate.lastIndexOf("}");
  if (firstBrace === -1 || lastBrace === -1 || lastBrace <= firstBrace) {
    throw new Error("No JSON object found in model output");
  }
  return candidate.slice(firstBrace, lastBrace + 1);
}

const ToolName = z.enum(["list_dir", "read_file", "write_file", "run_shell"]);
const ActionSchema = z.union([
  z.object({
    type: z.literal("tool"),
    name: ToolName,
    args: z.record(z.any()).default({}),
  }),
  z.object({
    type: z.literal("final"),
    final: z.string().min(1),
  }),
]);

function coerceAction(parsed) {
  if (!parsed || typeof parsed !== "object") return null;

  // Happy path: matches our strict schema.
  const strict = ActionSchema.safeParse(parsed);
  if (strict.success) return strict.data;

  // Common "almost there" shapes:
  // - {"final":"..."}
  if (typeof parsed.final === "string" && parsed.final.trim()) {
    return { type: "final", final: parsed.final.trim() };
  }
  // - {"type":"final","content":"..."}
  if (parsed.type === "final" && typeof parsed.content === "string" && parsed.content.trim()) {
    return { type: "final", final: parsed.content.trim() };
  }
  // - {"tool":"read_file","args":{...}}
  if (typeof parsed.tool === "string") {
    const maybe = ActionSchema.safeParse({ type: "tool", name: parsed.tool, args: parsed.args ?? {} });
    if (maybe.success) return maybe.data;
  }
  // - {"name":"read_file","args":{...}} (missing type)
  if (typeof parsed.name === "string") {
    const maybe = ActionSchema.safeParse({ type: "tool", name: parsed.name, args: parsed.args ?? {} });
    if (maybe.success) return maybe.data;
  }

  return null;
}

function parseActionFromText(text) {
  const trimmed = String(text ?? "").trim();
  if (!trimmed) return null;
  try {
    return coerceAction(JSON.parse(extractJsonObject(trimmed)));
  } catch {
    return null;
  }
}

function buildSystemPrompt({ goal, workspaceRoot, allowShell }) {
  return [
    "You are an autonomous agent running in a local workspace.",
    "",
    `GOAL: ${goal}`,
    `WORKSPACE_ROOT: ${workspaceRoot}`,
    "",
    "You may use these tools (one per step):",
    "- list_dir { dir }",
    "- read_file { file, max_bytes? }",
    "- write_file { file, content, mode? }  (mode: overwrite|append)",
    `- run_shell { command, cwd? }  (enabled: ${allowShell ? "yes" : "no"})`,
    "",
    "Rules:",
    "- Always respond with a SINGLE JSON object, no markdown, no extra text.",
    "- Use one of these exact shapes:",
    '  {"type":"final","final":"..."}',
    '  {"type":"tool","name":"list_dir","args":{"dir":"."}}',
    "- Either choose a tool action, or finish with type=final.",
    "- Keep file paths inside WORKSPACE_ROOT.",
    "- Prefer small, safe steps; read before you write.",
  ].join("\n");
}

function toLangChainMessages(storedMessages) {
  return (storedMessages || []).map((m) => {
    if (m?.role === "assistant") return new AIMessage(m.content ?? "");
    if (m?.role === "system") return new SystemMessage(m.content ?? "");
    return new HumanMessage(m?.content ?? "");
  });
}

export function createAgentStepGraph({ baseURL, apiKey, model, tools, workspaceRoot, allowShell }) {
  const llm = new ChatOpenAI({
    model,
    apiKey,
    temperature: 0,
    configuration: { baseURL },
  });
  const jsonLlm = llm.withConfig({ response_format: { type: "json_object" } });

  const GraphState = Annotation.Root({
    goal: Annotation({ default: () => "" }),
    messages: Annotation({
      default: () => [],
      reducer: (left, right) => left.concat(right),
    }),
    action: Annotation({ default: () => null }),
    done: Annotation({ default: () => false }),
    final: Annotation({ default: () => "" }),
  });

  async function decide(state) {
    const system = new SystemMessage(
      buildSystemPrompt({
        goal: state.goal,
        workspaceRoot,
        allowShell,
      }),
    );

    const history = toLangChainMessages(state.messages);
    const response = await jsonLlm.invoke([system, ...history]);
    const content = typeof response.content === "string" ? response.content : JSON.stringify(response.content);

    let action = parseActionFromText(content);

    if (!action) {
      const repair = await jsonLlm.invoke([
        system,
        ...history,
        new HumanMessage(
          [
            "Your previous output was invalid or did not match the required schema.",
            "Return ONLY a valid JSON object with one of these shapes:",
            '{"type":"final","final":"..."}',
            '{"type":"tool","name":"list_dir","args":{"dir":"."}}',
          ].join("\n"),
        ),
      ]);
      const repairedContent =
        typeof repair.content === "string" ? repair.content : JSON.stringify(repair.content);
      action = parseActionFromText(repairedContent);
      if (action) {
        return {
          messages: [
            { role: "assistant", content },
            { role: "assistant", content: repairedContent },
          ],
          action,
          done: action.type === "final",
          final: action.type === "final" ? action.final : "",
        };
      }
    }

    if (!action) {
      // Last resort: don't crash; treat the model output as the final answer.
      action = { type: "final", final: String(content ?? "").trim() || "OK" };
    }

    return {
      messages: [{ role: "assistant", content }],
      action,
      done: action.type === "final",
      final: action.type === "final" ? action.final : "",
    };
  }

  function afterDecide(state) {
    if (state.done || state?.action?.type === "final") return END;
    return "act";
  }

  async function act(state) {
    if (!state.action || state.action.type !== "tool") return {};
    const { name, args } = state.action;
    const toolFn = tools[name];
    if (!toolFn) {
      return {
        messages: [{ role: "user", content: `Tool error: unknown tool "${name}"` }],
        action: null,
      };
    }
    try {
      const result = await toolFn(args);
      return {
        messages: [{ role: "user", content: `Tool ${name} result:\n${result}` }],
        action: null,
      };
    } catch (err) {
      return {
        messages: [{ role: "user", content: `Tool ${name} error:\n${String(err)}` }],
        action: null,
      };
    }
  }

  const graph = new StateGraph(GraphState)
    .addNode("decide", decide)
    .addNode("act", act)
    .addEdge(START, "decide")
    .addConditionalEdges("decide", afterDecide, {
      act: "act",
      [END]: END,
    })
    .addEdge("act", END);

  return graph.compile();
}
