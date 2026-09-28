import { access, cp, mkdir, readdir, rm, writeFile } from "node:fs/promises";
import { dirname, join, relative, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";

const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const distRoot = join(packageRoot, "dist");
const excludedNames = new Set(["dist", "node_modules", "package.json", "package-lock.json", "scripts"]);

async function ensureBuildDependencies() {
  const packages = ["parse5", "postcss", "postcss-value-parser", "sharp"];
  const installed = await Promise.all(packages.map(async name => {
    try {
      await access(join(packageRoot, "node_modules", name, "package.json"));
      return true;
    } catch {
      return false;
    }
  }));
  if (installed.every(Boolean)) return;

  const npmCli = process.env.npm_execpath;
  const command = npmCli ? process.execPath : process.platform === "win32" ? "npm.cmd" : "npm";
  const args = npmCli ? [npmCli, "ci", "--no-audit", "--no-fund"] : ["ci", "--no-audit", "--no-fund"];
  console.log("Installing ExportSite build dependencies...");
  const result = spawnSync(command, args, { cwd: packageRoot, stdio: "inherit" });
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error(`npm ci failed with exit code ${result.status}`);
}

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

await ensureBuildDependencies();
const { optimizeImages } = await import("./optimize-images.mjs");
await rm(distRoot, { recursive: true, force: true });
await copySourceTree(packageRoot, distRoot);
await optimizeImages(distRoot);
await writeFile(join(distRoot, ".nojekyll"), "", "utf8");
console.log(`ExportSite built at ${relative(process.cwd(), distRoot) || "."}${sep}`);
