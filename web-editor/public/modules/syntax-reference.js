import { renderMarkdown } from "./markdown-renderer.js";
import { translatedString, UI_LANGUAGES } from "./language.js";

const DOCUMENTS = [
  { id: "Guide-Page-Format", label: { en: "Markdown & page format", zh: "Markdown 与页面格式" } },
  { id: "Syntax-Completion", label: { en: "Syntax completion", zh: "语法补全" } },
  { id: "Tags-Reference", label: { en: "Tags reference", zh: "标签参考" } },
  { id: "Images-And-Assets", label: { en: "Images & assets", zh: "图片与资源" } },
  { id: "Navigation", label: { en: "Navigation", zh: "页面导航" } },
  { id: "Annotations", label: { en: "Annotations", zh: "标注" } },
  { id: "Recipes", label: { en: "Recipes", zh: "配方" } },
  { id: "GameScene", label: { en: "Game scenes", zh: "游戏场景" } },
  { id: "Ponder", label: { en: "Ponder", zh: "Ponder 演示" } },
  { id: "Localization", label: { en: "Localization", zh: "本地化" } },
  { id: "Mod-Compatibility", label: { en: "Mod compatibility", zh: "模组兼容" } },
  { id: "Structure-Export", label: { en: "Structure export", zh: "结构导出" } },
  { id: "Examples", label: { en: "Examples", zh: "示例" } },
];

function splitSections(source, documentId) {
  const sections = [];
  let current = null;
  for (const line of source.split(/\r?\n/)) {
    const heading = line.match(/^(#{1,3})\s+(.+)$/);
    if (heading) {
      if (current) sections.push(current);
      current = { id: `${documentId}-${sections.length}`, title: heading[2].replaceAll("`", ""), markdown: line, depth: heading[1].length };
    } else if (current) {
      current.markdown += `\n${line}`;
    }
  }
  if (current) sections.push(current);
  return sections;
}

export function createSyntaxReference(dialog, initialLocale) {
  const search = dialog.querySelector("#reference-search");
  const results = dialog.querySelector("#reference-results");
  const content = dialog.querySelector("#reference-content");
  const count = dialog.querySelector("#reference-count");
  const languageSelect = dialog.querySelector("#reference-language-select");
  const cache = new Map();
  languageSelect.replaceChildren(...Object.entries(UI_LANGUAGES).map(([locale, details]) => {
    const option = document.createElement("option");
    option.value = locale;
    option.textContent = details.label;
    return option;
  }));
  let language = initialLocale;
  let sections = [];
  let selectedId = "";
  let loadRequest = 0;

  function updateLabels() {
    dialog.querySelector("#reference-title").textContent = translatedString(language, "referenceTitle");
    dialog.querySelector("#reference-subtitle").textContent = translatedString(language, "referenceSubtitle");
    search.placeholder = translatedString(language, "referenceSearch");
    search.setAttribute("aria-label", search.placeholder);
    languageSelect.value = language;
    languageSelect.setAttribute("aria-label", translatedString(language, "referenceLanguage"));
    dialog.querySelector("[data-reference-close]").setAttribute("aria-label", translatedString(language, "closeReference"));
  }

  function showSection(section) {
    selectedId = section.id;
    content.innerHTML = renderMarkdown(section.markdown, { locale: language });
    content.scrollTop = 0;
    results.querySelectorAll("[data-reference-section]").forEach((button) => {
      button.classList.toggle("active", button.dataset.referenceSection === selectedId);
    });
  }

  function renderResults() {
    const query = search.value.trim().toLocaleLowerCase();
    const matches = sections.filter((section) => section.markdown.toLocaleLowerCase().includes(query));
    results.replaceChildren();
    count.textContent = `${matches.length} ${translatedString(language, "referenceTopics")}`;
    if (!matches.length) {
      const empty = document.createElement("p");
      empty.className = "reference-empty";
      empty.textContent = translatedString(language, "referenceEmpty");
      results.append(empty);
      content.replaceChildren();
      return;
    }
    let lastDocument = "";
    for (const section of matches) {
      if (section.documentId !== lastDocument) {
        lastDocument = section.documentId;
        const group = document.createElement("h3");
        group.textContent = sections.find((item) => item.documentId === lastDocument && item.depth === 1)?.title || DOCUMENTS.find((item) => item.id === lastDocument).label.en;
        results.append(group);
      }
      const button = document.createElement("button");
      button.type = "button";
      button.dataset.referenceSection = section.id;
      button.className = `reference-result depth-${section.depth}${section.id === selectedId ? " active" : ""}`;
      button.textContent = section.title;
      button.addEventListener("click", () => showSection(section));
      results.append(button);
    }
    const selected = matches.find((section) => section.id === selectedId) || matches[0];
    showSection(selected);
  }

  async function loadLanguage(nextLanguage) {
    const request = ++loadRequest;
    language = nextLanguage;
    updateLabels();
    if (!cache.has(nextLanguage)) {
      content.textContent = translatedString(nextLanguage, "referenceLoading");
      const sources = await Promise.all(DOCUMENTS.map(async (item) => {
        const details = UI_LANGUAGES[nextLanguage] || UI_LANGUAGES.en_us;
        const referencePath = details.referenceDocuments?.includes(item.id) && details.referencePath
          ? details.referencePath
          : UI_LANGUAGES.en_us.referencePath || "en/_us";
        const response = await fetch(`./reference/${referencePath}/${item.id}.md`);
        if (!response.ok) throw new Error(`Unable to load ${item.id}`);
        return { documentId: item.id, source: await response.text() };
      }));
      cache.set(nextLanguage, sources.flatMap(({ documentId, source }) => splitSections(source, documentId).map((section) => ({ ...section, documentId }))));
    }
    if (request !== loadRequest) return;
    sections = cache.get(nextLanguage);
    selectedId = sections.find((section) => section.id === selectedId)?.id || "";
    renderResults();
  }

  dialog.querySelector("[data-reference-close]").addEventListener("click", () => dialog.close());
  dialog.addEventListener("click", (event) => { if (event.target === dialog) dialog.close(); });
  languageSelect.addEventListener("change", () => loadLanguage(languageSelect.value).catch(() => { content.textContent = translatedString(language, "referenceFailed"); }));
  search.addEventListener("input", renderResults);
  content.addEventListener("click", (event) => {
    const link = event.target.closest("a[href]");
    if (!link) return;
    const href = link.getAttribute("href") || "";
    if (/^https?:/i.test(href)) { link.target = "_blank"; link.rel = "noopener noreferrer"; return; }
    const documentId = href.split("#")[0].replace(/^\.\//, "").replace(/^.*\//, "").replace(/\.md$/i, "");
    const section = sections.find((item) => item.documentId === documentId);
    if (!section) return;
    event.preventDefault();
    search.value = "";
    selectedId = section.id;
    renderResults();
    results.querySelector(".reference-result.active")?.scrollIntoView({ block: "nearest" });
  });

  return {
    open() {
      dialog.showModal();
      loadLanguage(language).catch(() => { content.textContent = translatedString(language, "referenceFailed"); });
      search.focus();
    },
    setLocale(locale) {
      if (dialog.open && locale !== language) loadLanguage(locale).catch(() => { content.textContent = translatedString(language, "referenceFailed"); });
      else if (!dialog.open) language = locale;
    },
  };
}
