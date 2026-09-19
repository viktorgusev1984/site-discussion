import { readdir, readFile, writeFile } from "node:fs/promises";
import { join } from "node:path";

const packageRoot = process.argv[2];

if (!packageRoot) {
  throw new Error("Usage: node patch-acp-memory.mjs <qwen-code package root>");
}

const chunksDirectory = join(packageRoot, "chunks");
const original =
  "const targetMB = Math.min(Math.floor(memoryMb * 0.5), 16384);";
const replacement = `const configuredTargetMB = Number.parseInt(
    process.env["QWEN_ACP_HEAP_MB"] ?? "",
    10
  );
  const targetMB = Math.min(
    Number.isSafeInteger(configuredTargetMB) && configuredTargetMB > 0
      ? configuredTargetMB
      : Math.floor(memoryMb * 0.5),
    16384
  );`;

let patchedFiles = 0;

for (const entry of await readdir(chunksDirectory, { withFileTypes: true })) {
  if (!entry.isFile() || !entry.name.endsWith(".js")) continue;

  const path = join(chunksDirectory, entry.name);
  const source = await readFile(path, "utf8");
  if (!source.includes(original)) continue;

  const occurrences = source.split(original).length - 1;
  if (occurrences !== 1) {
    throw new Error(`Expected one ACP heap expression in ${path}, found ${occurrences}`);
  }

  await writeFile(path, source.replace(original, replacement));
  patchedFiles += 1;
}

if (patchedFiles !== 1) {
  throw new Error(`Expected to patch one Qwen chunk, patched ${patchedFiles}`);
}

console.log("Added QWEN_ACP_HEAP_MB support to Qwen Code");
