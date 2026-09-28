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
    const normalized = normalizeLanguage(preference);
    if (available.includes(normalized)) return normalized;

    const parts = normalized.split("_");
    const family = parts[0];
    if (parts.length > 2) {
      const regional = `${family}_${parts.at(-1)}`;
      if (available.includes(regional)) return regional;
    }
    if (family === "zh") {
      const traditional = parts.includes("hant") || parts.includes("tw") || parts.includes("hk")
        || parts.includes("mo");
      const variant = traditional ? "zh_tw" : "zh_cn";
      if (available.includes(variant)) return variant;
    }
    const commonVariant = { en: "en_us", ja: "ja_jp", pt: "pt_br" }[family];
    if (commonVariant && available.includes(commonVariant)) return commonVariant;
    const related = available.find(language => language === family || language.startsWith(`${family}_`));
    if (related) return related;
  }
  return available.includes("en_us") ? "en_us" : available[0];
}
