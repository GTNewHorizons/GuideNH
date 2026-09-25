const textDecoder = new TextDecoder("utf-8");

function uint32(view, offset) {
  return view.getUint32(offset, true);
}

function uint16(view, offset) {
  return view.getUint16(offset, true);
}

function findEndOfCentralDirectory(bytes) {
  for (let index = bytes.length - 22; index >= Math.max(0, bytes.length - 65557); index -= 1) {
    if (bytes[index] === 0x50 && bytes[index + 1] === 0x4b && bytes[index + 2] === 0x05 && bytes[index + 3] === 0x06) return index;
  }
  return -1;
}

async function inflateRaw(bytes) {
  if (typeof DecompressionStream === "undefined") throw new Error("This browser cannot inflate ZIP files");
  const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream("deflate-raw"));
  return new Uint8Array(await new Response(stream).arrayBuffer());
}

export async function zipToFiles(file) {
  const bytes = new Uint8Array(await file.arrayBuffer());
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  const end = findEndOfCentralDirectory(bytes);
  if (end < 0) throw new Error("ZIP central directory is missing");
  const count = uint16(view, end + 10);
  const directoryOffset = uint32(view, end + 16);
  let offset = directoryOffset;
  const files = [];
  for (let index = 0; index < count; index += 1) {
    if (uint32(view, offset) !== 0x02014b50) throw new Error("Invalid ZIP central directory entry");
    const method = uint16(view, offset + 10);
    const compressedSize = uint32(view, offset + 20);
    const nameLength = uint16(view, offset + 28);
    const extraLength = uint16(view, offset + 30);
    const commentLength = uint16(view, offset + 32);
    const localOffset = uint32(view, offset + 42);
    const name = textDecoder.decode(bytes.slice(offset + 46, offset + 46 + nameLength)).replaceAll("\\", "/");
    offset += 46 + nameLength + extraLength + commentLength;
    if (!name || name.endsWith("/") || name.startsWith("__MACOSX/")) continue;
    if (uint32(view, localOffset) !== 0x04034b50) continue;
    const localNameLength = uint16(view, localOffset + 26);
    const localExtraLength = uint16(view, localOffset + 28);
    const compressed = bytes.slice(localOffset + 30 + localNameLength + localExtraLength, localOffset + 30 + localNameLength + localExtraLength + compressedSize);
    const content = method === 0 ? compressed : method === 8 ? await inflateRaw(compressed) : null;
    if (!content) continue;
    const ext = name.split(".").pop()?.toLowerCase() || "";
    const isText = ["md", "mdx", "html", "htm", "json", "css", "js", "mjs", "svg", "txt", "snbt", "csv", "mmd", "yml", "yaml", "properties", "toml", "xml", "gradle", "kts", "java", "kt", "ts", "tsx", "jsx", "sql", "sh", "bat", "ps1"].includes(ext);
    files.push({ path: name, content: isText ? textDecoder.decode(content) : `data:${mimeType(ext)};base64,${arrayBufferToBase64(content)}`, encoding: isText ? "text" : "data-url" });
  }
  return files;
}

function mimeType(extension) {
  return { avif: "image/avif", bmp: "image/bmp", ico: "image/x-icon", png: "image/png", jpg: "image/jpeg", jpeg: "image/jpeg", gif: "image/gif", webp: "image/webp", svg: "image/svg+xml", mp4: "video/mp4", webm: "video/webm", ogg: "audio/ogg", mp3: "audio/mpeg", wav: "audio/wav", flac: "audio/flac", m4a: "audio/mp4", mov: "video/quicktime", woff: "font/woff", woff2: "font/woff2", ttf: "font/ttf", otf: "font/otf" }[extension] || "application/octet-stream";
}

function arrayBufferToBase64(bytes) {
  let binary = "";
  for (let index = 0; index < bytes.length; index += 0x8000) binary += String.fromCharCode(...bytes.subarray(index, index + 0x8000));
  return btoa(binary);
}
