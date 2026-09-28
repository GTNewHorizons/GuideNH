import { access, cp, mkdir, readdir, rm, writeFile } from "node:fs/promises";
import { dirname, join, relative, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";

const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const distRoot = join(packageRoot, "dist");
const imageCacheRoot = join(packageRoot, "node_modules", ".cache", "guidenh-webp-v2");
const excludedNames = new Set(["dist", "node_modules", "package.json", "package-lock.json", "scripts"]);
const buildStartedAt = process.hrtime.bigint();

function formatBuildDuration() {
  const elapsedSeconds = Number(process.hrtime.bigint() - buildStartedAt) / 1e9;
  const minutes = Math.floor(elapsedSeconds / 60);
  const seconds = (elapsedSeconds % 60).toFixed(2).padStart(5, "0");
  return `${minutes}m ${seconds}s`;
}

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
  const entries = (await listEntries(source)).filter(entry => !excludedNames.has(entry.name));
  await Promise.all(entries.map(entry => cp(
    join(source, entry.name),
    join(target, entry.name),
    { force: true, recursive: entry.isDirectory() },
  )));
}

try {
  let phaseStartedAt = process.hrtime.bigint();
  await ensureBuildDependencies();
  console.log(`Dependency check: ${(Number(process.hrtime.bigint() - phaseStartedAt) / 1e9).toFixed(2)}s.`);
  const { optimizeImages } = await import("./optimize-images.mjs");
  phaseStartedAt = process.hrtime.bigint();
  await rm(distRoot, { recursive: true, force: true });
  await copySourceTree(packageRoot, distRoot);
  console.log(`Source copy: ${(Number(process.hrtime.bigint() - phaseStartedAt) / 1e9).toFixed(2)}s.`);
  await optimizeImages(distRoot, imageCacheRoot);
  await writeFile(join(distRoot, ".nojekyll"), "", "utf8");
  console.log(`ExportSite built at ${relative(process.cwd(), distRoot) || "."}${sep}`);
  console.log(`ExportSite build completed in ${formatBuildDuration()}.`);
} catch (error) {
  console.error(`ExportSite build failed after ${formatBuildDuration()}.`);
  throw error;
}
