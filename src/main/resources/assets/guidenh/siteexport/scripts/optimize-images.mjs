import { mkdir, readFile, readdir, rm, writeFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import { join, relative, sep } from "node:path";
import { availableParallelism } from "node:os";
import { promisify } from "node:util";
import { gunzip, gzip } from "node:zlib";
import { parse, parseFragment } from "parse5";
import postcss from "postcss";
import parseValue from "postcss-value-parser";
import sharp from "sharp";

const imageConcurrency = Math.max(
  2,
  Math.min(8, Number.parseInt(process.env.GUIDENH_IMAGE_CONCURRENCY || "", 10) || availableParallelism()),
);
const verifyLossless = process.env.GUIDENH_VERIFY_WEBP === "1";
// Each conversion is CPU-heavy. One libvips worker per task avoids oversubscription while
// allowing independent images to be processed in parallel.
sharp.concurrency(1);

const decompress = promisify(gunzip);
const compress = promisify(gzip);

async function collectFiles(directory) {
  const files = [];
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const file = join(directory, entry.name);
    if (entry.isDirectory()) files.push(...await collectFiles(file));
    else if (entry.isFile()) files.push(file);
  }
  return files;
}

function isAnimatedPng(bytes) {
  if (bytes.length < 8 || !bytes.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))) {
    return true;
  }
  for (let offset = 8; offset + 12 <= bytes.length;) {
    const size = bytes.readUInt32BE(offset);
    const type = bytes.toString("ascii", offset + 4, offset + 8);
    if (type === "acTL") return true;
    if (type === "IDAT" || type === "IEND") return false;
    offset += size + 12;
  }
  return true;
}

function rewriteUrl(value, mapping) {
  if (/^(?:[a-z][a-z\d+.-]*:|\/\/|#)/i.test(value)) return value;
  const start = value.indexOf("_res/");
  if (start < 0) return value;
  if (!/^(?:(?:\.\.?\/)*|\{\{root\}\}\/|\/)$/.test(value.slice(0, start))) return value;
  const end = value.slice(start).search(/[?#]/);
  const pathEnd = end < 0 ? value.length : start + end;
  const replacement = mapping[value.slice(start, pathEnd)];
  return replacement ? value.slice(0, start) + replacement + value.slice(pathEnd) : value;
}

function rewriteCss(value, mapping, inline = false) {
  const root = postcss.parse(inline ? `x{${value}}` : value);
  root.walkDecls(declaration => {
    const parsed = parseValue(declaration.value);
    parsed.walk(node => {
      if (node.type !== "function" || node.value.toLowerCase() !== "url") return;
      const url = node.nodes.find(child => child.type === "string" || child.type === "word");
      if (url) url.value = rewriteUrl(url.value, mapping);
      return false;
    });
    declaration.value = parsed.toString();
  });
  return inline ? root.first.nodes.map(node => node.toString()).join(";") : root.toString();
}

function rewriteJson(value, mapping) {
  if (typeof value === "string") {
    return value.includes("<") && value.includes("_res/")
      ? rewriteHtml(value, mapping, true) : rewriteUrl(value, mapping);
  }
  if (Array.isArray(value)) return value.map(entry => rewriteJson(entry, mapping));
  if (value && typeof value === "object") {
    for (const key of Object.keys(value)) value[key] = rewriteJson(value[key], mapping);
  }
  return value;
}

const resourceUrlPattern = /_res\/[A-Za-z0-9._~/-]+\.png(?=$|[?#"'()<>;&\s\\])/gi;

function rewriteResourceUrlsFast(value, mapping) {
  return value.replace(resourceUrlPattern, match => mapping[match] || match);
}

function rewriteHtml(value, mapping, fragment = false) {
  const options = { sourceCodeLocationInfo: true };
  const root = fragment ? parseFragment(value, options) : parse(value, options);
  const replacements = [];
  function visit(node) {
    for (const attribute of node.attrs || []) {
      if (!attribute.value.includes("_res/")) continue;
      const original = attribute.value;
      if (attribute.name === "style") {
        attribute.value = rewriteCss(attribute.value, mapping, true);
      } else if (/^[\[{]/.test(attribute.value)) {
        let data;
        try { data = JSON.parse(attribute.value); } catch { continue; }
        attribute.value = JSON.stringify(rewriteJson(data, mapping));
      } else {
        attribute.value = rewriteUrl(attribute.value, mapping);
      }
      const location = node.sourceCodeLocation?.attrs?.[attribute.name];
      if (location && original !== attribute.value) {
        const escaped = attribute.value.replaceAll("&", "&amp;").replaceAll('"', "&quot;");
        replacements.push({ ...location, value: `${attribute.name}="${escaped}"` });
      }
    }
    if (node.tagName === "style") {
      for (const child of node.childNodes || []) {
        if (child.nodeName === "#text" && child.value.includes("_res/")) {
          const rewritten = rewriteCss(child.value, mapping);
          if (rewritten !== child.value && child.sourceCodeLocation) {
            replacements.push({ ...child.sourceCodeLocation, value: rewritten });
          }
        }
      }
    }
    if (node.content) visit(node.content);
    for (const child of node.childNodes || []) visit(child);
  }
  visit(root);
  // Edit parsed attribute ranges so whitespace and visible document text stay intact.
  for (const replacement of replacements.sort((left, right) => right.startOffset - left.startOffset)) {
    value = value.slice(0, replacement.startOffset) + replacement.value + value.slice(replacement.endOffset);
  }
  return value;
}

async function convertImage(original) {
  if (isAnimatedPng(original)) return { reason: "animated or invalid PNG" };
  const input = sharp(original, { failOn: "warning" });
  const metadata = await input.metadata();
  if (metadata.width > 16383 || metadata.height > 16383 || metadata.depth !== "uchar") {
    return { reason: "unsupported size or bit depth" };
  }
  const webp = await input.clone().keepIccProfile().webp({ lossless: true, effort: 4 }).toBuffer();
  if (webp.length >= original.length) return { reason: "PNG is smaller" };
  if (verifyLossless) {
    // Optional audit mode compares every RGBA byte, including transparent texels.
    const pixels = await input.clone().ensureAlpha().raw().toBuffer();
    const convertedPixels = await sharp(webp).ensureAlpha().raw().toBuffer();
    if (!pixels.equals(convertedPixels)) return { reason: "RGBA pixels differ" };
  }
  return { webp, saved: original.length - webp.length };
}

export async function optimizeImages(distRoot, cacheRoot) {
  await mkdir(cacheRoot, { recursive: true });
  const cacheIndexPath = join(cacheRoot, "index.json");
  let cachedEntries = {};
  try {
    cachedEntries = JSON.parse(await readFile(cacheIndexPath, "utf8"));
  } catch {
    // A missing or incomplete cache is rebuilt from the source PNG files.
  }
  const cacheEntries = { ...cachedEntries };
  const inFlight = new Map();
  let cacheHits = 0;
  const files = await collectFiles(distRoot);
  const images = files.filter(file => relative(distRoot, file).startsWith(`_res${sep}`) && /\.png$/i.test(file));
  const mapping = Object.create(null);
  const convertedFiles = [];
  const skipped = new Map();
  let savedBytes = 0;
  let nextImage = 0;
  const conversionStartedAt = process.hrtime.bigint();
  await Promise.all(Array.from({ length: Math.min(imageConcurrency, images.length) }, async () => {
    while (nextImage < images.length) {
      const file = images[nextImage++];
      let result;
      try {
        const original = await readFile(file);
        const hash = createHash("sha256").update(original).digest("hex");
        let conversion = inFlight.get(hash);
        if (!conversion) {
          conversion = (async () => {
            const cached = cacheEntries[hash];
            if (cached?.reason) {
              cacheHits++;
              return { reason: cached.reason };
            }
            if (cached?.webp) {
              try {
                const webp = await readFile(join(cacheRoot, `${hash}.webp`));
                cacheHits++;
                return { webp, saved: original.length - webp.length };
              } catch {
                // A missing cached image is converted again.
              }
            }
            const converted = await convertImage(original);
            if (converted.webp) {
              await writeFile(join(cacheRoot, `${hash}.webp`), converted.webp);
              cacheEntries[hash] = { webp: true };
            } else {
              cacheEntries[hash] = { reason: converted.reason };
            }
            return converted;
          })();
          inFlight.set(hash, conversion);
        }
        result = await conversion;
      } catch (error) {
        console.warn(`WebP conversion skipped for ${relative(distRoot, file)}: ${error.message}`);
        result = { reason: "conversion failed" };
      }
      if (!result.webp) {
        skipped.set(result.reason, (skipped.get(result.reason) || 0) + 1);
        continue;
      }
      const target = file.replace(/\.png$/i, ".webp");
      const sourceUrl = relative(distRoot, file).split(sep).join("/");
      const targetUrl = relative(distRoot, target).split(sep).join("/");
      if (result.saved <= Buffer.byteLength(JSON.stringify({ [sourceUrl]: targetUrl }))) {
        skipped.set("path mapping exceeds savings", (skipped.get("path mapping exceeds savings") || 0) + 1);
        continue;
      }
      // Existing assets with this name must remain untouched.
      try {
        await writeFile(target, result.webp, { flag: "wx" });
      } catch (error) {
        if (error.code !== "EEXIST") throw error;
        skipped.set("WebP path already exists", (skipped.get("WebP path already exists") || 0) + 1);
        continue;
      }
      mapping[sourceUrl] = targetUrl;
      convertedFiles.push(file);
      savedBytes += result.saved;
    }
  }));
  console.log(`Image conversion: ${(Number(process.hrtime.bigint() - conversionStartedAt) / 1e9).toFixed(2)}s; ${cacheHits}/${images.length} cache hits.`);
  const rewriteStartedAt = process.hrtime.bigint();
  const textFiles = files.filter(file => /\.(?:html|json|css)(?:\.gz)?$/.test(file));
  let nextTextFile = 0;
  await Promise.all(Array.from({ length: Math.min(imageConcurrency, textFiles.length) }, async () => {
    while (nextTextFile < textFiles.length) {
      const file = textFiles[nextTextFile++];
      const zipped = file.endsWith(".gz");
      const textPath = zipped ? file.slice(0, -3) : file;
      const content = await readFile(file);
      const text = (zipped ? await decompress(content) : content).toString("utf8");
      if (!text.includes("_res/")) continue;
      let rewritten;
      if (textPath.endsWith(".json")) rewritten = rewriteResourceUrlsFast(text, mapping);
      else if (textPath.endsWith(".css")) rewritten = rewriteCss(text, mapping);
      else rewritten = rewriteResourceUrlsFast(text, mapping);
      if (rewritten !== text) {
        await writeFile(file, zipped ? await compress(Buffer.from(rewritten, "utf8")) : rewritten, "utf8");
      }
    }
  }));
  // Binary scene payloads retain their original URLs; the image loader resolves this map.
  const manifest = Object.fromEntries(Object.entries(mapping).sort(([left], [right]) => left.localeCompare(right)));
  const manifestJson = JSON.stringify(manifest);
  await writeFile(join(distRoot, "_site", "model-viewer", "image-formats.json"), manifestJson, "utf8");
  for (const file of convertedFiles) await rm(file);
  await writeFile(cacheIndexPath, JSON.stringify(cacheEntries), "utf8");
  console.log(`Image reference rewrite: ${(Number(process.hrtime.bigint() - rewriteStartedAt) / 1e9).toFixed(2)}s.`);
  savedBytes -= Buffer.byteLength(manifestJson) - 2;
  console.log(`WebP: ${convertedFiles.length}/${images.length} images converted; ${(savedBytes / 1048576).toFixed(2)} MiB saved.`);
  for (const [reason, count] of skipped) console.log(`PNG retained: ${count} (${reason}).`);
}
