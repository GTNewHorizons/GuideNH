import { matchLanguage } from "./languageMatching.js";

const localeRoot = new URL("./lang/", import.meta.url);
const localeRequests = new Map();
const localeData = new Map();

async function readJson(url) {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`Locale request failed: ${response.status} ${url}`);
  return response.json();
}

const indexRequest = readJson(new URL("index.json", localeRoot));
loadLocale("en_us");
let availableLocales;
try {
  availableLocales = await indexRequest;
  if (!Array.isArray(availableLocales) || !availableLocales.includes("en_us")) {
    throw new Error("The locale index must contain en_us.");
  }
} catch (error) {
  console.warn("GuideNH locale index could not be loaded.", error);
  availableLocales = ["en_us"];
}
function resolveLocale(language) {
  return matchLanguage(language, availableLocales) || "en_us";
}

function loadLocale(code) {
  if (!localeRequests.has(code)) {
    localeRequests.set(code, readJson(new URL(`${code}.json`, localeRoot)).then(data => {
      localeData.set(code, data);
      return data;
    }).catch(error => {
      console.warn(`GuideNH locale ${code} could not be loaded.`, error);
      return {};
    }));
  }
  return localeRequests.get(code);
}

export async function ensureSiteLanguage(language = document.documentElement.lang) {
  const code = resolveLocale(language);
  await Promise.all([loadLocale("en_us"), loadLocale(code)]);
}

export function siteSection(section) {
  const english = localeData.get("en_us") || {};
  const localized = localeData.get(resolveLocale(document.documentElement.lang)) || {};
  return { ...english[section], ...localized[section] };
}

export function siteText(section, key) {
  return siteSection(section)[key] || key;
}

await ensureSiteLanguage();
