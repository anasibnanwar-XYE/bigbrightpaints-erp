import fs from "node:fs/promises";
import path from "node:path";
import { execFile } from "node:child_process";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);

function resolveWithinRoot(root, userPath) {
  const resolvedRoot = path.resolve(root);
  const resolvedTarget = path.resolve(resolvedRoot, userPath || ".");
  if (
    resolvedTarget !== resolvedRoot &&
    !resolvedTarget.startsWith(`${resolvedRoot}${path.sep}`)
  ) {
    throw new Error(`Path escapes workspace root: ${userPath}`);
  }
  return resolvedTarget;
}

function truncate(text, maxChars = 16_000) {
  if (typeof text !== "string") return String(text);
  if (text.length <= maxChars) return text;
  return `${text.slice(0, maxChars)}\n\n...[truncated ${text.length - maxChars} chars]`;
}

export function createTools({ workspaceRoot, allowShell }) {
  return {
    async list_dir({ dir = "." } = {}) {
      const target = resolveWithinRoot(workspaceRoot, dir);
      const entries = await fs.readdir(target, { withFileTypes: true });
      const lines = entries
        .sort((a, b) => a.name.localeCompare(b.name))
        .map((entry) => (entry.isDirectory() ? `${entry.name}/` : entry.name));
      return lines.join("\n");
    },

    async read_file({ file, max_bytes = 50_000 } = {}) {
      if (!file) throw new Error("read_file requires { file }");
      const target = resolveWithinRoot(workspaceRoot, file);
      const buf = await fs.readFile(target);
      const sliced = buf.length > max_bytes ? buf.subarray(0, max_bytes) : buf;
      const text = sliced.toString("utf8");
      if (buf.length > max_bytes) {
        return `${text}\n\n...[truncated ${buf.length - max_bytes} bytes]`;
      }
      return text;
    },

    async write_file({ file, content, mode = "overwrite" } = {}) {
      if (!file) throw new Error("write_file requires { file }");
      if (typeof content !== "string") {
        throw new Error("write_file requires { content: string }");
      }
      const target = resolveWithinRoot(workspaceRoot, file);
      await fs.mkdir(path.dirname(target), { recursive: true });
      if (mode === "append") {
        await fs.appendFile(target, content, "utf8");
        return `Appended ${content.length} chars to ${file}`;
      }
      await fs.writeFile(target, content, "utf8");
      return `Wrote ${content.length} chars to ${file}`;
    },

    async run_shell({ command, cwd = "." } = {}) {
      if (!command) throw new Error("run_shell requires { command }");
      if (!allowShell) {
        return "Shell execution is disabled (set ALLOW_SHELL=1 to enable).";
      }
      const resolvedCwd = resolveWithinRoot(workspaceRoot, cwd);
      const { stdout, stderr } = await execFileAsync("/bin/bash", ["-lc", command], {
        cwd: resolvedCwd,
        timeout: 60_000,
        maxBuffer: 4 * 1024 * 1024,
      });
      const out = [stdout?.trim(), stderr?.trim()].filter(Boolean).join("\n");
      return truncate(out || "(no output)");
    },
  };
}

