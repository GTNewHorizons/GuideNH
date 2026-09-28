async function loadJson(url) {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`Unable to load locale: ${url}`);
  return response.json();
}

const directory = new URL("../locales/", import.meta.url);
const { languages } = await loadJson(new URL("index.json", directory));

export const UI_LANGUAGES = Object.fromEntries(await Promise.all(languages.map(async ({ code, label, referencePath, referenceDocuments, fallbackDocuments }) => {
  const details = await loadJson(new URL(`${code}.json`, directory));
  return [code, { ...details, label, referencePath, referenceDocuments, fallbackDocuments }];
})));
