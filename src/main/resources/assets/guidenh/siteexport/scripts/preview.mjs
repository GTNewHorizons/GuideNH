import { createReadStream } from "node:fs";
import { realpath, lstat } from "node:fs/promises";
import { createServer } from "node:http";
import { extname, join, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";

const packageRoot = resolve(fileURLToPath(new URL("..", import.meta.url)));
const distRoot = resolve(packageRoot, "dist");
const port = Number(process.env.PORT || 4173);
const contentTypes = {
  ".css": "text/css; charset=utf-8",
  ".gif": "image/gif",
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".mjs": "text/javascript; charset=utf-8",
  ".png": "image/png",
  ".svg": "image/svg+xml",
  ".txt": "text/plain; charset=utf-8",
  ".webp": "image/webp",
  ".woff": "font/woff",
  ".woff2": "font/woff2"
};

function safePath(urlPath) {
  const decoded = decodeURIComponent(urlPath.split("?")[0] || "/");
  const candidate = resolve(distRoot, `.${decoded}`);
  const rootWithSeparator = `${distRoot}${sep}`;
  if (candidate !== distRoot && !candidate.startsWith(rootWithSeparator)) {
    return null;
  }
  return candidate;
}

async function resolveFile(requestPath) {
  const candidate = safePath(requestPath);
  if (!candidate) {
    return null;
  }
  try {
    const realRoot = await realpath(distRoot);
    const realCandidate = await realpath(candidate);
    if (realCandidate !== realRoot && !realCandidate.startsWith(`${realRoot}${sep}`)) return null;
    const details = await lstat(candidate);
    if (details.isSymbolicLink()) return null;
    if (details.isFile()) {
      return realCandidate;
    }
    if (details.isDirectory()) {
      const indexPath = join(realCandidate, "index.html");
      const realIndex = await realpath(indexPath);
      if (!realIndex.startsWith(`${realRoot}${sep}`)) return null;
      const indexDetails = await lstat(indexPath);
      return indexDetails.isFile() && !indexDetails.isSymbolicLink() ? realIndex : null;
    }
  } catch {
    return null;
  }
  return null;
}

const server = createServer(async (request, response) => {
  try {
    const filePath = await resolveFile(request.url || "/");
    if (!filePath) {
      response.writeHead(404, { "content-type": "text/plain; charset=utf-8" });
      response.end("Not found");
      return;
    }
    response.writeHead(200, {
      "cache-control": "no-cache",
      "content-type": contentTypes[extname(filePath).toLowerCase()] || "application/octet-stream"
    });
    const stream = createReadStream(filePath);
    stream.on("error", () => response.destroy());
    response.on("close", () => stream.destroy());
    stream.pipe(response);
  } catch {
    response.writeHead(500, { "content-type": "text/plain; charset=utf-8" });
    response.end("Unable to read file");
  }
});

server.listen(port, "127.0.0.1", () => {
  console.log(`ExportSite preview: http://127.0.0.1:${port}/`);
});
