export function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function parseAttributes(source) {
  const attributes = {};
  const text = String(source || "");
  let index = 0;
  while (index < text.length) {
    while (index < text.length && /\s/.test(text[index])) index += 1;
    if (index >= text.length || text[index] === ">") break;
    const nameStart = index;
    while (index < text.length && /[:@\w-]/.test(text[index])) index += 1;
    if (nameStart === index) { index += 1; continue; }
    const name = text.slice(nameStart, index);
    while (index < text.length && /\s/.test(text[index])) index += 1;
    if (text[index] !== "=") { attributes[name] = true; continue; }
    index += 1;
    while (index < text.length && /\s/.test(text[index])) index += 1;
    const value = readAttributeValue(text, index);
    attributes[name] = normalizeAttributeValue(value.value);
    index = value.end;
  }
  return attributes;
}

function readAttributeValue(source, startIndex) {
  let index = startIndex;
  if (index >= source.length) return { value: "", end: index };
  const opener = source[index];
  if (opener === "\"" || opener === "'") {
    const quote = opener;
    const start = ++index;
    while (index < source.length) {
      if (source[index] === "\\") { index += 2; continue; }
      if (source[index] === quote) return { value: source.slice(start, index), end: index + 1 };
      index += 1;
    }
    return { value: source.slice(start), end: index };
  }
  if (opener === "{") {
    const start = ++index;
    let depth = 1;
    let quote = "";
    while (index < source.length && depth > 0) {
      const character = source[index];
      if (quote) {
        if (character === "\\") { index += 2; continue; }
        if (character === quote) quote = "";
      } else if (character === "\"" || character === "'") quote = character;
      else if (character === "{") depth += 1;
      else if (character === "}") depth -= 1;
      index += 1;
    }
    return { value: source.slice(start, Math.max(start, index - 1)), end: index };
  }
  const start = index;
  while (index < source.length && !/[\s>]/.test(source[index])) index += 1;
  return { value: source.slice(start, index), end: index };
}

function normalizeAttributeValue(value) {
  const text = String(value).trim();
  if (text === "true") return true;
  if (text === "false") return false;
  if (/^-?(?:\d+\.?\d*|\.\d+)$/.test(text)) return Number(text);
  return text;
}

export function attr(attributes, ...names) {
  for (const name of names) {
    if (Object.prototype.hasOwnProperty.call(attributes, name)) return attributes[name];
  }
  return undefined;
}

export function booleanAttr(attributes, ...names) {
  const value = attr(attributes, ...names);
  if (value === undefined) return undefined;
  if (value === true || value === false) return value;
  return !["false", "0", "no", "off"].includes(String(value).toLowerCase());
}

export function numberAttr(attributes, fallback, ...names) {
  const value = Number(attr(attributes, ...names));
  return Number.isFinite(value) ? value : fallback;
}

export function colorValue(value, fallback = "#6ec6d7") {
  const text = String(value ?? "").trim();
  if (/^(?:#[0-9a-f]{3,8}|rgba?\(|hsla?\(|transparent$)/i.test(text)) return text;
  if (/^0x[0-9a-f]{6,8}$/i.test(text)) return `#${text.slice(2)}`;
  return fallback;
}

export function cssLength(value, fallback = "auto") {
  if (value === undefined || value === null || value === "") return fallback;
  const text = String(value).trim();
  return /^-?(?:\d+(?:\.\d+)?|\.\d+)(?:px|%|em|rem|vh|vw)?$/.test(text) ? (/^[+-]?\d/.test(text) && /^-?(?:\d+(?:\.\d+)?)$/.test(text) ? `${text}px` : text) : fallback;
}

export function splitComma(value) {
  return String(value ?? "").split(",").map((item) => item.trim()).filter(Boolean);
}

export function parseCsv(source) {
  const rows = [];
  let row = [];
  let cell = "";
  let quoted = false;
  const text = String(source ?? "").replace(/^\uFEFF/, "");
  for (let index = 0; index < text.length; index += 1) {
    const character = text[index];
    if (character === '"') {
      if (quoted && text[index + 1] === '"') { cell += '"'; index += 1; }
      else quoted = !quoted;
    } else if (character === "," && !quoted) { row.push(cell); cell = ""; }
    else if ((character === "\n" || character === "\r") && !quoted) {
      if (character === "\r" && text[index + 1] === "\n") index += 1;
      row.push(cell); rows.push(row); row = []; cell = "";
    } else cell += character;
  }
  if (cell.length || row.length) { row.push(cell); rows.push(row); }
  return rows.filter((items) => items.some((item) => item.trim()));
}

export function splitRange(value, fallbackMin, fallbackMax) {
  const match = String(value ?? "").match(/^\s*(-?(?:\d+\.?\d*|\.\d+))\s*\.\.\s*(-?(?:\d+\.?\d*|\.\d+))\s*$/);
  if (!match) return [fallbackMin, fallbackMax];
  const left = Number(match[1]);
  const right = Number(match[2]);
  return Number.isFinite(left) && Number.isFinite(right) && left !== right ? [Math.min(left, right), Math.max(left, right)] : [fallbackMin, fallbackMax];
}
