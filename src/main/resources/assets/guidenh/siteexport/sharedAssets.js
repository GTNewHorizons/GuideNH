const MAX_CACHE_BYTES = 8 * 1024 * 1024;
const MAX_CACHE_ENTRIES = 32;
const cachedText = new Map();
const pendingText = new Map();
let cacheBytes = 0;

async function readText(url) {
  const response = await fetch(url, { credentials: "same-origin" });
  if (!response.ok) {
    throw new Error(`GuideNH asset request failed: ${response.status} ${url}`);
  }
  const bytes = new Uint8Array(await response.arrayBuffer());
  // Detect the payload so hosts that already decode Content-Encoding work too.
  if (bytes[0] !== 0x1f || bytes[1] !== 0x8b) {
    return new TextDecoder().decode(bytes);
  }
  if (typeof DecompressionStream === "function") {
    const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream("gzip"));
    return new Response(stream).text();
  }
  const { default: decompress } = await import("./model-viewer/vendor/decompressFallback-VGYIC7XH.js");
  return new TextDecoder().decode(await decompress(new Response(bytes)));
}

export function loadSharedText(source) {
  const url = new URL(source, document.baseURI).href;
  if (cachedText.has(url)) {
    const text = cachedText.get(url);
    cachedText.delete(url);
    cachedText.set(url, text);
    return Promise.resolve(text);
  }
  if (pendingText.has(url)) return pendingText.get(url);
  const pending = readText(url).then((text) => {
    const size = text.length * 2;
    if (size <= MAX_CACHE_BYTES) {
      while (cachedText.size && (cacheBytes + size > MAX_CACHE_BYTES || cachedText.size >= MAX_CACHE_ENTRIES)) {
        const oldest = cachedText.keys().next().value;
        cacheBytes -= cachedText.get(oldest).length * 2;
        cachedText.delete(oldest);
      }
      cachedText.set(url, text);
      cacheBytes += size;
    }
    return text;
  }).finally(() => pendingText.delete(url));
  pendingText.set(url, pending);
  return pending;
}
