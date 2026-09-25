import { readFile, readdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const destination = path.join(root, "web-editor", "public", "reference");

for (const name of await readdir(destination)) {
  if (!name.endsWith(".md")) continue;
  const source = await readFile(path.join(root, "wiki", name), "utf8");
  await writeFile(path.join(destination, name), source, "utf8");
}
