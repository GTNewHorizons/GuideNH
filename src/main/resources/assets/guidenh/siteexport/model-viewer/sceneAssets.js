const sceneCache = { entries: new Map(), pending: new Map(), bytes: 0, limit: 32 * 1024 * 1024, count: 16 };
const imageCache = { entries: new Map(), pending: new Map(), bytes: 0, limit: 288 * 1024 * 1024, count: 48 };
const siteRoot = new URL("../../", import.meta.url);
let imageFormats;

/**
 * Keep scene texture uploads in the same orientation on ImageBitmap and canvas
 * fallbacks. three.js applies UNPACK_FLIP_Y_WEBGL to canvas uploads, while
 * browsers commonly ignore it for ImageBitmap uploads.
 */
export function prepareSceneTexture(texture) {
  if (texture && typeof texture === "object") {
    texture.flipY = false;
  }
  return texture;
}

/** Return the scale three.js uses when an image exceeds maxTextureSize. */
export function getSceneTextureScale(width, height, maxTextureSize) {
  const imageWidth = Number(width);
  const imageHeight = Number(height);
  const limit = Number(maxTextureSize);
  if (![imageWidth, imageHeight, limit].every(Number.isFinite)
      || imageWidth <= 0 || imageHeight <= 0 || limit <= 0) {
    return 1;
  }
  return Math.min(1, limit / Math.max(imageWidth, imageHeight));
}

/** Match three.js' integer dimensions for resized texture regions. */
export function scaleSceneTextureCoordinate(value, scale) {
  return Math.floor(Number(value) * Number(scale));
}

function loadImageFormats() {
  if (!imageFormats) {
    imageFormats = request(new URL("image-formats.json", import.meta.url).href).then(response => response.json());
  }
  return imageFormats;
}

function loadCached(cache, key, load, sizeOf) {
  const entry = cache.entries.get(key);
  if (entry) {
    cache.entries.delete(key);
    cache.entries.set(key, entry);
    return Promise.resolve(entry.value);
  }
  if (cache.pending.has(key)) return cache.pending.get(key);
  const pending = load().then(value => {
    const size = sizeOf(value);
    if (size <= cache.limit) {
      while (cache.entries.size && (cache.bytes + size > cache.limit || cache.entries.size >= cache.count)) {
        const oldest = cache.entries.keys().next().value;
        cache.bytes -= cache.entries.get(oldest).size;
        // Active renderers may still use the bitmap; dropping the cache reference is sufficient.
        cache.entries.delete(oldest);
      }
      cache.entries.set(key, { value, size });
      cache.bytes += size;
    }
    return value;
  }).finally(() => cache.pending.delete(key));
  cache.pending.set(key, pending);
  return pending;
}

async function request(source) {
  const response = await fetch(source, { credentials: "same-origin" });
  if (!response.ok) throw new Error(`Scene asset request failed: ${response.status} ${source}`);
  return response;
}

export async function loadSceneBytes(source, signal) {
  signal?.throwIfAborted();
  const url = new URL(source, document.baseURI).href;
  const bytes = await loadCached(sceneCache, url, async () => {
    const data = new Uint8Array(await (await request(url)).arrayBuffer());
    if (data[0] !== 0x1f || data[1] !== 0x8b) return data;
    if (typeof DecompressionStream === "function") {
      const stream = new Blob([data]).stream().pipeThrough(new DecompressionStream("gzip"));
      return new Uint8Array(await new Response(stream).arrayBuffer());
    }
    const { default: decompress } = await import("./vendor/decompressFallback-VGYIC7XH.js");
    return new Uint8Array(await decompress(new Response(data)));
  }, data => data.byteLength);
  signal?.throwIfAborted();
  return bytes;
}

export async function loadSceneImage(source, flipY = false) {
  let url = new URL(source, document.baseURI).href;
  if (url.startsWith(siteRoot.href)) {
    const replacement = (await loadImageFormats())[url.slice(siteRoot.href.length).split(/[?#]/, 1)[0]];
    if (replacement) url = new URL(replacement, siteRoot).href;
  }
  return loadCached(imageCache, `${flipY}:${url}`, async () => {
    const blob = await (await request(url)).blob();
    return createImageBitmap(blob, {
      imageOrientation: flipY ? "flipY" : "none",
      premultiplyAlpha: "none",
      colorSpaceConversion: "none",
    });
  }, image => image.width * image.height * 4);
}
