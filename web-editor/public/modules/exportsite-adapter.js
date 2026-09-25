const TEXT_EXTENSIONS = new Set(["md", "mdx", "html", "htm", "json", "css", "js", "mjs", "svg", "txt", "snbt", "csv", "mmd", "yml", "yaml", "properties", "toml", "xml", "gradle", "kts", "java", "kt", "ts", "tsx", "jsx", "sql", "sh", "bat", "ps1"]);
const BINARY_EXTENSIONS = new Set(["avif", "bmp", "ico", "png", "jpg", "jpeg", "gif", "webp", "mp4", "webm", "ogg", "mp3", "wav", "flac", "m4a", "mov", "woff", "woff2", "ttf", "otf", "gz", "gzip", "bin", "dat", "ktx2", "glb"]);

function extension(path) {
  return path.split(".").pop()?.toLowerCase() || "";
}

function normalizePath(path) {
  const safe = [];
  for (const segment of String(path || "").replaceAll("\\", "/").split("/")) {
    if (!segment || segment === ".") continue;
    if (segment === "..") { safe.pop(); continue; }
    safe.push(segment);
  }
  return safe.join("/");
}

function pageTitle(path, content) {
  const heading = content.match(/^#{1,2}\s+(.+)$/m)?.[1]?.trim();
  if (heading) return heading.replaceAll("`", "");
  return path.split("/").pop()?.replace(/\.(md|mdx|html?)$/i, "") || "Untitled";
}

function sourceKind(path) {
  const ext = extension(path);
  if (ext === "md" || ext === "mdx") return "markdown";
  if (ext === "html" || ext === "htm") return "html";
  return "asset";
}

export function createSampleProject() {
  const content = `---
navigation:
  title: GuideNH Web Editor
  position: 0
---

# GuideNH Web Editor

Edit this page in the center pane and see a GuideNH styled preview on the right.

## ExportSite compatibility

- Import an exported site folder from **ExportSite**.
- Keep Markdown, frontmatter, navigation, and assets together.
- Use the export button to move the project to another browser.

> [!TIP]
> Your work is saved in this browser automatically.

\`\`\`mermaid
mindmap
  root((GuideNH))
    Markdown
    ExportSite
    Preview
\`\`\`
`;
  return normalizeProject({ name: "GuideNH project", files: [{ path: "guides/guidenh/guidenh/en_us/index.md", content, kind: "markdown" }] });
}

export async function filesToProject(fileList) {
  const files = [];
  for (const file of Array.from(fileList || [])) {
    const path = normalizePath(file.webkitRelativePath || file.name);
    const segments = path.split("/");
    if (!path || path.endsWith("/") || segments.some((segment) => [".git", "node_modules", ".gradle", "build", "logs"].includes(segment))) continue;
    const ext = extension(path);
    let content;
    let encoding = "text";
    if (TEXT_EXTENSIONS.has(ext) || file.type.startsWith("text/")) {
      content = await file.text();
    } else if (file.type.startsWith("image/") || file.type.startsWith("audio/") || file.type.startsWith("video/") || BINARY_EXTENSIONS.has(ext)) {
      content = `data:${file.type || assetMimeType(ext)};base64,${arrayBufferToBase64(await file.arrayBuffer())}`;
      encoding = "data-url";
    } else {
      continue;
    }
    files.push({ path, content, encoding, kind: sourceKind(path) });
  }
  return normalizeProject({ name: "Imported ExportSite", files: stripCommonRoot(files) });
}

function assetMimeType(extension) {
  return { avif: "image/avif", bmp: "image/bmp", ico: "image/x-icon", png: "image/png", jpg: "image/jpeg", jpeg: "image/jpeg", gif: "image/gif", webp: "image/webp", svg: "image/svg+xml", mp4: "video/mp4", webm: "video/webm", ogg: "audio/ogg", mp3: "audio/mpeg", wav: "audio/wav", flac: "audio/flac", m4a: "audio/mp4", mov: "video/quicktime", woff: "font/woff", woff2: "font/woff2", ttf: "font/ttf", otf: "font/otf" }[extension] || "application/octet-stream";
}

export function stripCommonRoot(files) {
  const roots = files.map((file) => file.path.split("/")[0]).filter(Boolean);
  if (!roots.length || !roots.every((root) => root === roots[0])) return files;
  return files.map((file) => ({ ...file, path: file.path.slice(roots[0].length + 1) || file.path }));
}

function arrayBufferToBase64(buffer) {
  let binary = "";
  const bytes = new Uint8Array(buffer);
  const step = 0x8000;
  for (let index = 0; index < bytes.length; index += step) binary += String.fromCharCode(...bytes.subarray(index, index + step));
  return btoa(binary);
}

export function normalizeProject(input) {
  const files = Array.from(input?.files || []).filter((file) => file?.path).map((file) => ({
    path: normalizePath(file.path),
    content: String(file.content ?? ""),
    encoding: file.encoding || "text",
    kind: file.kind || sourceKind(file.path),
  })).filter((file) => file.path);
  const pages = files.filter((file) => file.kind === "markdown" || file.kind === "html").map((file) => ({
    path: file.path,
    title: pageTitle(file.path, file.content),
    kind: file.kind,
  })).sort((left, right) => pageRank(left.path) - pageRank(right.path) || left.path.localeCompare(right.path));
  return {
    id: input?.id || globalThis.crypto?.randomUUID?.() || `project-${Date.now()}`,
    name: input?.name || "GuideNH project",
    files,
    pages,
    selectedPath: input?.selectedPath && files.some((file) => file.path === input.selectedPath) ? input.selectedPath : pages[0]?.path || files[0]?.path || "",
    updatedAt: input?.updatedAt || new Date().toISOString(),
  };
}

function pageRank(path) {
  if (path.startsWith("guides/")) return 0;
  if (path.includes("wiki/resourcepack/assets/") && path.endsWith(".md")) return 1;
  if (path.includes("src/main/resources/assets/guidenh/welcome/")) return 2;
  if (path.startsWith("wiki/")) return 3;
  return 4;
}

export function findFile(project, path) {
  return project.files.find((file) => file.path === path) || null;
}

export function updateFile(project, path, content) {
  const files = project.files.map((file) => file.path === path ? { ...file, content, encoding: "text" } : file);
  return normalizeProject({ ...project, files, selectedPath: path });
}

export function addPage(project) {
  const base = "guides/guidenh/guidenh/en_us/new-page.md";
  let path = base;
  let index = 2;
  while (project.files.some((file) => file.path === path)) path = base.replace("new-page", `new-page-${index++}`);
  const file = { path, content: "---\nnavigation:\n  title: New page\n---\n\n# New page\n\nStart writing here.\n", kind: "markdown", encoding: "text" };
  return normalizeProject({ ...project, files: [...project.files, file], selectedPath: path });
}

export function serializeProject(project) {
  return JSON.stringify({ format: "guidenh-web-editor", version: 1, exportedAt: new Date().toISOString(), project }, null, 2);
}
