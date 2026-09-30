export function matchLanguage(language, availableLanguages) {
  const available = new Set(availableLanguages);
  const exact = String(language || "").trim().toLowerCase().replaceAll("-", "_");
  if (available.has(exact)) return exact;

  let locale;
  try {
    locale = new Intl.Locale(exact.replaceAll("_", "-"));
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
  for (const code of candidates) if (available.has(code)) return code;
  if (family === "zh") {
    const traditional = script === "hant" || ["tw", "hk", "mo"].includes(region);
    const preferred = traditional ? "zh_tw" : "zh_cn";
    if (available.has(preferred)) return preferred;
  }
  return availableLanguages.find(code => code === family || code.startsWith(`${family}_`)) || null;
}
