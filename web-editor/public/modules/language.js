export const UI_LANGUAGES = {
  en_us: { label: "English", short: "EN", strings: { project: "Project", preview: "Preview", new: "New", importFolder: "Import folder", importBundle: "Import bundle", exportBundle: "Export bundle", language: "Language", siteLanguage: "Site", uiLanguage: "UI", syncScroll: "Sync scroll", pages: "pages", saved: "Saved in this browser", saving: "Saving…", unsaved: "Unsaved changes", noPages: "No matching pages", emptyPreview: "Import or create a page to start previewing." } },
  zh_cn: { label: "简体中文", short: "中", strings: { project: "项目", preview: "预览", new: "新建", importFolder: "导入文件夹", importBundle: "导入项目包", exportBundle: "导出项目包", language: "语言", siteLanguage: "站点", uiLanguage: "界面", syncScroll: "同步滚动", pages: "页", saved: "已保存到此浏览器", saving: "保存中…", unsaved: "未保存修改", noPages: "没有匹配页面", emptyPreview: "请导入或创建页面开始预览。" } },
  zh_tw: { label: "繁體中文", short: "繁", strings: { project: "專案", preview: "預覽", new: "新增", importFolder: "匯入資料夾", importBundle: "匯入專案包", exportBundle: "匯出專案包", language: "語言", siteLanguage: "網站", uiLanguage: "介面", syncScroll: "同步捲動", pages: "頁", saved: "已儲存到此瀏覽器", saving: "儲存中…", unsaved: "未儲存變更", noPages: "沒有符合頁面", emptyPreview: "請匯入或建立頁面開始預覽。" } },
  ja_jp: { label: "日本語", short: "日", strings: { project: "プロジェクト", preview: "プレビュー", new: "新規", importFolder: "フォルダーを読み込む", importBundle: "バンドルを読み込む", exportBundle: "バンドルを書き出す", language: "言語", siteLanguage: "サイト", uiLanguage: "UI", syncScroll: "スクロール同期", pages: "ページ", saved: "このブラウザーに保存済み", saving: "保存中…", unsaved: "未保存の変更", noPages: "一致するページがありません", emptyPreview: "フォルダーを読み込むかページを作成してください。" } },
};

export function normalizeLanguage(value) {
  const normalized = String(value || "").replace("-", "_").toLowerCase();
  const aliases = { en: "en_us", zh: "zh_cn", ja: "ja_jp" };
  if (aliases[normalized]) return aliases[normalized];
  return /^[a-z]{2}(?:_[a-z]{2})?$/.test(normalized) ? normalized : "en_us";
}

export function languageLabel(value) {
  const key = normalizeLanguage(value);
  if (UI_LANGUAGES[key]?.label) return UI_LANGUAGES[key].label;
  const [language, region] = key.split("_");
  return region ? `${language.toUpperCase()} (${region.toUpperCase()})` : language.toUpperCase();
}

export function detectLanguage(path) {
  const match = String(path || "").match(/(?:^|\/)(?:_|)?([a-z]{2}(?:[_-][a-z]{2})?)(?:\/|$)/i);
  if (!match) return null;
  const key = normalizeLanguage(match[1]);
  return key;
}

export function collectProjectLanguages(project) {
  const values = new Set();
  for (const file of project?.files || []) {
    const language = detectLanguage(file.path);
    if (language) values.add(language);
  }
  return values.size ? [...values].sort() : ["en_us"];
}

export function localizedPagePath(project, path, language) {
  const wanted = normalizeLanguage(language);
  const files = project?.files || [];
  const direct = files.find((file) => file.path === path && detectLanguage(file.path) === wanted);
  if (direct) return direct.path;
  const sourceLanguage = detectLanguage(path);
  if (sourceLanguage) {
    const segment = path.match(new RegExp(`(^|/)(_?${sourceLanguage})(?=/|$)`, "i"));
    const replacement = segment?.[2]?.startsWith("_") ? `_${wanted}` : wanted;
    const candidate = segment ? `${path.slice(0, segment.index + segment[1].length)}${replacement}${path.slice(segment.index + segment[0].length)}` : path;
    const found = files.find((file) => file.path === candidate);
    if (found) return found.path;
  }
  const filename = path.split("/").pop();
  const found = files.find((file) => (file.kind === "markdown" || file.kind === "html") && file.path.endsWith(`/${filename}`) && detectLanguage(file.path) === wanted);
  return found?.path || path;
}

export function translatedString(language, key) {
  const normalized = normalizeLanguage(language);
  return UI_LANGUAGES[normalized]?.strings[key] || UI_LANGUAGES.en_us.strings[key] || key;
}
