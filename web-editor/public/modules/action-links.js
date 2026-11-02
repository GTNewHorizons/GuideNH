const resolvers = new Map([
  ["nei", resolveNeiAction],
  ["sound", resolveSoundAction],
  ["sound-src", resolveSoundAction],
]);
const NEI_ATTRIBUTES = new Set(["mode", "id", "handlerId", "title"]);
const NEI_MODES = new Set(["handler", "recipes", "usage"]);

export function createActionLinkError(key, message, values = {}) {
  return Object.assign(new Error(message), { translationKey: key, translationValues: values });
}

export function formatActionLinkError(error, translate) {
  if (!error.translationKey) return error.message;
  return translate(error.translationKey).replace(/\{(\w+)\}/g, (match, name) => error.translationValues[name] ?? match);
}

export function readActionLink(text, start) {
  const labelEnd = findClosing(text, start + 2, "]");
  if (labelEnd < 0 || text[labelEnd + 1] !== "(") return null;
  const end = findClosing(text, labelEnd + 2, ")");
  if (end < 0) return null;
  const label = unescapeDelimiters(text.slice(start + 2, labelEnd));
  const destination = unescapeDelimiters(text.slice(labelEnd + 2, end));
  if (!label || !destination) return null;
  return { label, destination, source: text.slice(start, end + 1), nextIndex: end + 1 };
}

export function expandActionLink(link, defaultNamespace) {
  const separator = link.destination.indexOf(":");
  const scheme = link.destination.slice(0, separator).toLowerCase();
  const resolver = separator > 0 ? resolvers.get(scheme) : null;
  return resolver ? resolver(link.destination.slice(separator + 1), defaultNamespace, scheme) : null;
}

export function resolveNeiAttributes(attributes, defaultNamespace = "minecraft") {
  for (const [name, value] of Object.entries(attributes)) {
    if (!NEI_ATTRIBUTES.has(name) || typeof value !== "string") throw createActionLinkError("neiAttributeError", `Invalid NeiRecipeLink attribute: ${name}`, { name });
  }
  let id = attributes.id?.trim() || null;
  const handlerId = attributes.handlerId?.trim() || null;
  const mode = attributes.mode === undefined ? (id ? "recipes" : "handler") : attributes.mode.trim().toLowerCase();
  if (!NEI_MODES.has(mode)) throw createActionLinkError("neiModeError", `Unknown NEI action: ${mode}`, { mode });
  if (mode === "handler" && (!handlerId || id)) throw createActionLinkError("neiHandlerError", "NEI handler mode requires handlerId and does not accept id.");
  if (mode !== "handler" && !id) throw createActionLinkError("neiItemError", `NEI ${mode} mode requires id.`, { mode });
  if (/[\u0000-\u001f\u007f-\u009f]/.test((attributes.id || "") + (attributes.handlerId || ""))) throw createActionLinkError("neiControlError", "NEI targets must not contain control characters.");
  if (id) {
    let head = id.split("{", 1)[0];
    if (id.includes("{") && head.endsWith(":")) head = head.slice(0, -1);
    const parts = head.split(":");
    if (!head || !(parts.length > 1 ? parts[1] : parts[0])) throw createActionLinkError("neiEmptyItemError", "NEI item id must not be empty.");
    if (!head.includes(":")) id = `${defaultNamespace}:${id}`;
  }
  return { mode, id, handlerId, title: attributes.title || "" };
}

function resolveNeiAction(payload, defaultNamespace) {
  if (payload.includes("#")) throw createActionLinkError("neiFragmentError", "NEI actions do not accept fragments.");
  const queryStart = payload.indexOf("?");
  const attributes = { mode: queryStart < 0 ? payload : payload.slice(0, queryStart) };
  if (queryStart >= 0) {
    for (const pair of payload.slice(queryStart + 1).split("&")) {
      const separator = pair.indexOf("=");
      if (separator <= 0) throw createActionLinkError("neiQueryError", "NEI parameters must have a name and value.");
      let name, value;
      try {
        name = decodeURIComponent(pair.slice(0, separator));
        value = decodeURIComponent(pair.slice(separator + 1));
      } catch {
        throw createActionLinkError("neiEncodingError", "NEI parameters must use valid UTF-8 percent escapes.");
      }
      if (name !== "id" && name !== "handlerId") throw createActionLinkError("neiParameterError", `Unknown NEI parameter: ${name}`, { name });
      if (!value.trim() || Object.hasOwn(attributes, name)) throw createActionLinkError("neiDuplicateParameterError", `Blank or duplicate NEI parameter: ${name}`, { name });
      attributes[name] = value;
    }
  }
  resolveNeiAttributes(attributes, defaultNamespace);
  return { name: "NeiRecipeLink", attributes };
}

function resolveSoundAction(payload, _defaultNamespace, scheme) {
  const queryStart = payload.indexOf("?");
  const decode = value => decodeURIComponent(value.replaceAll("+", " "));
  const attributes = { [scheme === "sound-src" ? "src" : "sound"]: decode(queryStart < 0 ? payload : payload.slice(0, queryStart)) };
  if (queryStart >= 0) {
    for (const pair of payload.slice(queryStart + 1).split("&")) {
      const separator = pair.indexOf("=");
      if (separator <= 0) continue;
      const name = decode(pair.slice(0, separator));
      const value = decode(pair.slice(separator + 1));
      if (["volume", "pitch", "radius", "minVolume", "cooldown"].includes(name) && value.trim() && Number.isFinite(Number(value))) attributes[name] = value;
    }
  }
  return { name: "SoundLink", attributes };
}

function findClosing(text, start, delimiter) {
  let escaped = false;
  for (let index = start; index < text.length; index += 1) {
    if (escaped) escaped = false;
    else if (text[index] === "\\") escaped = true;
    else if (text[index] === delimiter) return index;
  }
  return -1;
}

function unescapeDelimiters(value) {
  return value.replace(/\\([\[\]()\\])/g, (match, delimiter) => delimiter === "\\" ? match : delimiter);
}
