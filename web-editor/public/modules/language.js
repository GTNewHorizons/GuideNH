import { UI_LANGUAGES } from "./ui-copy.js";

export { UI_LANGUAGES };

export function normalizeLanguage(value) {
  const normalized = String(value || "").trim().replaceAll("-", "_").toLowerCase();
  return /^[a-z]{2,3}(?:_[a-z0-9]{2,8}){0,2}$/.test(normalized) ? normalized : "en_us";
}

function matchingLanguage(value, available) {
  const key = normalizeLanguage(value);
  if (available.includes(key)) return key;
  let locale;
  try {
    locale = new Intl.Locale(key.replaceAll("_", "-"));
  } catch {
    return null;
  }
  const family = locale.language.toLowerCase();
  const region = locale.region?.toLowerCase();
  const script = locale.script?.toLowerCase();
  const candidates = [
    [family, script, region].filter(Boolean).join("_"),
    [family, region].filter(Boolean).join("_"),
    [family, script].filter(Boolean).join("_"),
  ];
  for (const candidate of candidates) if (available.includes(candidate)) return candidate;
  if (family === "zh") {
    const traditional = script === "hant" || ["tw", "hk", "mo"].includes(region);
    const preferred = traditional ? "zh_tw" : "zh_cn";
    if (available.includes(preferred)) return preferred;
  }
  return available.find((language) => language === family || language.startsWith(`${family}_`)) || null;
}

export function languageLabel(value) {
  const key = normalizeLanguage(value);
  const specialNames = { en_ud: "Upside Down English", lol_us: "LOLCAT", qya_aa: "Quenya", tlh_aa: "Klingon" };
  let name = UI_LANGUAGES[key]?.label || specialNames[key];
  if (!name) {
    const tag = key.replaceAll("_", "-");
    try { name = new Intl.DisplayNames([tag], { type: "language" }).of(tag); }
    catch (_) { name = key.toUpperCase(); }
  }
  return `${name} (${key})`;
}

export function browserLanguages() {
  const languages = globalThis.navigator?.languages;
  return languages?.length ? [...languages] : [globalThis.navigator?.language || "en-US"];
}

export function uiLanguageFor(value) {
  return matchingLanguage(value, Object.keys(UI_LANGUAGES)) || "en_us";
}

export function preferredUiLanguage(preferences) {
  for (const preference of preferences) {
    const matched = matchingLanguage(preference, Object.keys(UI_LANGUAGES));
    if (matched) return matched;
  }
  return "en_us";
}

export function projectLanguageFor(available, preferences) {
  for (const preference of preferences) {
    const matched = matchingLanguage(preference, available);
    if (matched) return matched;
  }
  return available.includes("en_us") ? "en_us" : available[0] || "en_us";
}

export function detectLanguage(path) {
  const match = String(path || "").match(/(?:^|\/)(?:_([a-z]{2,3}(?:[_-][a-z0-9]{2,8}){0,2})|([a-z]{2,3}(?:[_-][a-z0-9]{2,8}){1,2}))(?:\/|$)/i);
  if (!match) return null;
  const key = normalizeLanguage(match[1] || match[2]);
  return key;
}

export function collectProjectLanguages(project) {
  const values = new Set();
  for (const file of project?.files || []) {
    if (file.kind !== "markdown" && file.kind !== "html") continue;
    const language = detectLanguage(file.path);
    if (language) values.add(language);
  }
  return values.size ? [...values].sort() : ["en_us"];
}

export function localizedPagePath(project, path, language) {
  const wanted = normalizeLanguage(language);
  const files = (project?.files || []).filter((file) => file.kind === "markdown" || file.kind === "html");
  const direct = files.find((file) => file.path === path && detectLanguage(file.path) === wanted);
  if (direct) return direct.path;
  const sourceLanguage = detectLanguage(path);
  if (sourceLanguage) {
    const segments = path.split("/");
    const index = segments.findIndex((segment) => /^[a-z]{2,3}(?:[_-][a-z0-9]{2,8}){0,2}$/i.test(segment.replace(/^_/, "")) && normalizeLanguage(segment.replace(/^_/, "")) === sourceLanguage);
    if (index >= 0) segments[index] = `${segments[index].startsWith("_") ? "_" : ""}${wanted}`;
    const candidate = segments.join("/");
    const found = files.find((file) => file.path === candidate);
    if (found) return found.path;
  }
  const filename = path.split("/").pop();
  const found = files.find((file) => (file.kind === "markdown" || file.kind === "html") && file.path.endsWith(`/${filename}`) && detectLanguage(file.path) === wanted);
  return found?.path || files.find((file) => detectLanguage(file.path) === wanted)?.path || path;
}

export function translatedString(language, key) {
  const normalized = uiLanguageFor(language);
  return UI_LANGUAGES[normalized]?.strings[key] || UI_LANGUAGES.en_us.strings[key] || key;
}
