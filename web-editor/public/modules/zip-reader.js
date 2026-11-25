import { readBoundedStream } from "./bounded-stream.js";
const textDecoder = new TextDecoder("utf-8", { fatal: true });
const maxArchiveBytes = 128 * 1024 * 1024;
const maxFileBytes = 64 * 1024 * 1024;
const maxTotalBytes = 256 * 1024 * 1024;
const crcTable = Uint32Array.from({ length: 256 }, (_, index) => {
  let value = index;
  for (let bit = 0; bit < 8; bit++) value = (value >>> 1) ^ (value & 1 ? 0xedb88320 : 0);
  return value >>> 0;
});

function crc32(bytes) {
  let value = 0xffffffff;
  for (const byte of bytes) value = crcTable[(value ^ byte) & 255] ^ (value >>> 8);
  return (value ^ 0xffffffff) >>> 0;
}

function requireRange(offset, length, limit) {
  if (offset < 0 || length < 0 || offset > limit - length) throw new Error("Invalid ZIP entry range");
}

function uint32(view, offset) {
  return view.getUint32(offset, true);
}

function uint16(view, offset) {
  return view.getUint16(offset, true);
}

function findEndOfCentralDirectory(bytes) {
  for (let index = bytes.length - 22; index >= Math.max(0, bytes.length - 65557); index -= 1) {
    if (bytes[index] === 0x50 && bytes[index + 1] === 0x4b && bytes[index + 2] === 0x05 && bytes[index + 3] === 0x06
        && index + 22 + bytes[index + 20] + bytes[index + 21] * 256 === bytes.length) return index;
  }
  return -1;
}

async function inflateRaw(bytes, maxBytes) {
  if (typeof DecompressionStream === "undefined") throw new Error("This browser cannot inflate ZIP files");
  const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream("deflate-raw"));
  return readBoundedStream(stream, maxBytes);
}

export async function zipToFiles(file) {
  if (file.size > maxArchiveBytes) throw new Error("ZIP archive exceeds its input limit");
  const bytes = await readBoundedStream(file.stream(), maxArchiveBytes);
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  const end = findEndOfCentralDirectory(bytes);
  if (end < 0) throw new Error("ZIP central directory is missing");
  if (uint16(view, end + 4) !== 0 || uint16(view, end + 6) !== 0) throw new Error("Multi-volume ZIP archives are unsupported");
  const count = uint16(view, end + 10);
  if (count > 4096 || count !== uint16(view, end + 8)) throw new Error("ZIP entry count exceeds its limit");
  const directoryOffset = uint32(view, end + 16);
  const directorySize = uint32(view, end + 12);
  requireRange(directoryOffset, directorySize, end);
  const directoryEnd = directoryOffset + directorySize;
  let totalDecoded = 0;
  const names = new Set();
  let offset = directoryOffset;
  const files = [];
  for (let index = 0; index < count; index += 1) {
    requireRange(offset, 46, directoryEnd);
    if (uint32(view, offset) !== 0x02014b50) throw new Error("Invalid ZIP central directory entry");
    const flags = uint16(view, offset + 8);
    if (flags & 1) throw new Error("Encrypted ZIP entries are unsupported");
    const method = uint16(view, offset + 10);
    const checksum = uint32(view, offset + 16);
    const decodedSize = uint32(view, offset + 24);
    if (decodedSize > Math.min(maxFileBytes, maxTotalBytes - totalDecoded)) throw new Error("ZIP decoded size exceeds its limit");
    const compressedSize = uint32(view, offset + 20);
    const nameLength = uint16(view, offset + 28);
    const extraLength = uint16(view, offset + 30);
    const commentLength = uint16(view, offset + 32);
    const localOffset = uint32(view, offset + 42);
    requireRange(offset + 46, nameLength + extraLength + commentLength, directoryEnd);
    const rawName = bytes.subarray(offset + 46, offset + 46 + nameLength);
    const name = textDecoder.decode(rawName).replaceAll("\\", "/").replace(/^(?:\.\/)+/, "");
    if (name.startsWith("/") || name.includes(":") || name.includes("\0") || name.split("/").includes("..")) throw new Error("Unsafe ZIP entry path");
    if (names.has(name)) throw new Error("Duplicate ZIP entry path");
    names.add(name);
    offset += 46 + nameLength + extraLength + commentLength;
    if (!name || name.endsWith("/") || name.startsWith("__MACOSX/")) continue;
    requireRange(localOffset, 30, directoryOffset);
    if (uint32(view, localOffset) !== 0x04034b50 || uint16(view, localOffset + 8) !== method
        || uint16(view, localOffset + 6) !== flags) throw new Error("Invalid ZIP local header");
    const localNameLength = uint16(view, localOffset + 26);
    const localExtraLength = uint16(view, localOffset + 28);
    requireRange(localOffset + 30, localNameLength + localExtraLength, directoryOffset);
    const localName = bytes.subarray(localOffset + 30, localOffset + 30 + localNameLength);
    if (localName.length !== rawName.length || !localName.every((byte, index) => byte === rawName[index])) throw new Error("ZIP entry names disagree");
    const dataOffset = localOffset + 30 + localNameLength + localExtraLength;
    requireRange(dataOffset, compressedSize, directoryOffset);
    const compressed = bytes.subarray(dataOffset, dataOffset + compressedSize);
    if (method !== 0 && method !== 8) continue;
    const content = method === 0 ? compressed : await inflateRaw(compressed, decodedSize);
    if (content.length !== decodedSize || crc32(content) !== checksum) throw new Error("ZIP entry size or checksum mismatch");
    totalDecoded += content.length;
    const ext = name.split(".").pop()?.toLowerCase() || "";
    const isText = ["md", "mdx", "html", "htm", "json", "css", "js", "mjs", "svg", "txt", "snbt", "csv", "mmd", "yml", "yaml", "properties", "toml", "xml", "gradle", "kts", "java", "kt", "ts", "tsx", "jsx", "sql", "sh", "bat", "ps1"].includes(ext);
    files.push({ path: name, content: isText ? textDecoder.decode(content) : `data:${mimeType(ext)};base64,${arrayBufferToBase64(content)}`, encoding: isText ? "text" : "data-url" });
  }
  if (offset !== directoryEnd) throw new Error("ZIP central directory size mismatch");
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
