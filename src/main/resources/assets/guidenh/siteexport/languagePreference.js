import { matchLanguage } from "./languageMatching.js";

const storageKey = `guidenh.siteexport.language:${new URL("../", import.meta.url).pathname}`;

function normalizeLanguage(value) {
  return String(value || "").trim().toLowerCase().replaceAll("-", "_");
}

export function rememberSiteLanguage(language) {
  try {
    localStorage.setItem(storageKey, normalizeLanguage(language));
  } catch {
    // The browser may disable local storage; navigation still works.
  }
}

export function chooseSiteLanguage(availableLanguages) {
  const available = [...new Set(availableLanguages.map(normalizeLanguage))].sort();
  if (!available.length) return null;
  try {
    const saved = normalizeLanguage(localStorage.getItem(storageKey));
    if (available.includes(saved)) return saved;
  } catch {
    // Browser language detection remains available without local storage.
  }

  const preferences = navigator.languages?.length ? navigator.languages : [navigator.language];
  for (const preference of preferences) {
    const matched = matchLanguage(preference, available);
    if (matched) return matched;
  }
  return available.includes("en_us") ? "en_us" : available[0];
}
