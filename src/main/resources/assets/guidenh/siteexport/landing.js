import { chooseSiteLanguage } from "./languagePreference.js";

const landingPages = JSON.parse(document.body.dataset.guideLanguagePages || "{}");
const language = chooseSiteLanguage(Object.keys(landingPages));
if (language && landingPages[language]) {
  location.replace(new URL(landingPages[language], new URL("../", import.meta.url)).href);
}
