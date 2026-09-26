import { readFile, readdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const directory = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../public/locales");
const referenceDirectory = path.resolve(directory, "../reference");

export async function syncLocaleIndex() {
  const files = (await readdir(directory)).filter((name) => /^[a-z]{2,3}(?:_[a-z0-9]{2,8}){1,2}\.json$/.test(name));
  const referenceEntries = await readdir(referenceDirectory, { withFileTypes: true });
  const referencePaths = new Map();
  for (const languageEntry of referenceEntries.filter((entry) => entry.isDirectory())) {
    const regions = await readdir(path.join(referenceDirectory, languageEntry.name), { withFileTypes: true });
    for (const regionEntry of regions.filter((entry) => entry.isDirectory() && /^_[a-z0-9]+$/.test(entry.name))) {
      referencePaths.set(`${languageEntry.name}_${regionEntry.name.slice(1)}`, `${languageEntry.name}/${regionEntry.name}`);
    }
  }
  const englishPath = referencePaths.get("en_us");
  if (!englishPath) throw new Error("Missing en/_us reference directory");
  const englishDirectory = path.join(referenceDirectory, englishPath);
  const englishFiles = new Set((await readdir(englishDirectory)).filter((name) => name.endsWith(".md")));
  const documentIds = [...englishFiles].map((name) => name.slice(0, -3));
  const documents = await Promise.all(documentIds.map(async (id) => ({ id, label: (await readFile(path.join(englishDirectory, `${id}.md`), "utf8")).match(/^#\s+(.+)$/m)?.[1] || id })));
  const languages = await Promise.all(files.map(async (name) => {
    const details = JSON.parse(await readFile(path.join(directory, name), "utf8"));
    if (!details.label || !details.strings || typeof details.strings !== "object") throw new Error(`Invalid locale file: ${name}`);
    const code = name.slice(0, -5);
    const referencePath = referencePaths.get(code) || null;
    const available = referencePath ? new Set(await readdir(path.join(referenceDirectory, referencePath))) : new Set();
    const referenceDocuments = documentIds.filter((id) => available.has(`${id}.md`));
    return {
      code,
      label: details.label,
      referencePath,
      referenceDocuments,
      fallbackDocuments: documentIds.filter((id) => !referenceDocuments.includes(id)),
    };
  }));
  if (!languages.some((language) => language.code === "en_us")) throw new Error("Missing en_us locale");
  languages.sort((left, right) => left.code.localeCompare(right.code));
  await writeFile(path.join(directory, "index.json"), `${JSON.stringify({ languages }, null, 2)}\n`, "utf8");
  await writeFile(path.join(referenceDirectory, "index.json"), `${JSON.stringify({ documents }, null, 2)}\n`, "utf8");
  return languages;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const languages = await syncLocaleIndex();
  console.log(`Indexed ${languages.length} UI locales`);
}
