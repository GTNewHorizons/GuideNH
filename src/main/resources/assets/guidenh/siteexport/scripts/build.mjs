import { cp, mkdir, readdir, rm, writeFile } from "node:fs/promises";
import { dirname, join, relative, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";

const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const distRoot = join(packageRoot, "dist");
const excludedNames = new Set(["dist", "node_modules", "package.json", "package-lock.json", "scripts"]);

async function listEntries(directory) {
  return readdir(directory, { withFileTypes: true });
}

async function copySourceTree(source, target) {
  await mkdir(target, { recursive: true });
  for (const entry of await listEntries(source)) {
    if (excludedNames.has(entry.name)) {
      continue;
    }
    const sourcePath = join(source, entry.name);
    const targetPath = join(target, entry.name);
    if (entry.isDirectory()) {
      await copySourceTree(sourcePath, targetPath);
    } else if (entry.isFile()) {
      await cp(sourcePath, targetPath, { force: true });
    }
  }
}

await rm(distRoot, { recursive: true, force: true });
await copySourceTree(packageRoot, distRoot);
await writeFile(join(distRoot, ".nojekyll"), "", "utf8");
console.log(`ExportSite built at ${relative(process.cwd(), distRoot) || "."}${sep}`);
