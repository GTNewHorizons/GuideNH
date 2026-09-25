import { addPage, createSampleProject, filesToProject, findFile, normalizeProject, serializeProject, stripCommonRoot, updateFile } from "./modules/exportsite-adapter.js";
import { renderMarkdown } from "./modules/markdown-renderer.js";
import { loadProject, saveProject, clearProject } from "./modules/project-store.js";
import { zipToFiles } from "./modules/zip-reader.js";
import { collectProjectLanguages, detectLanguage, languageLabel, localizedPagePath, normalizeLanguage, translatedString, UI_LANGUAGES } from "./modules/language.js";
import { createSyntaxReference } from "./modules/syntax-reference.js";
import { createScrollSync } from "./modules/scroll-sync.js";

const state = { project: null, filter: "", dirty: false, saveTimer: null, collapsedTreePaths: new Set(), theme: localStorage.getItem("guidenh-theme") || "dark", syncScroll: localStorage.getItem("guidenh-sync-scroll") === "true", locale: normalizeLanguage(localStorage.getItem("guidenh-locale") || navigator.language), siteLanguage: normalizeLanguage(localStorage.getItem("guidenh-site-language") || navigator.language) };
const elements = {
  name: document.querySelector("#project-name"), tree: document.querySelector("#file-tree"), filter: document.querySelector("#file-filter"), count: document.querySelector("#file-count"), editor: document.querySelector("#editor"), editorLabel: document.querySelector("#editor-label"), editorMeta: document.querySelector("#editor-meta"), preview: document.querySelector("#preview-site"), frame: document.querySelector("#preview-frame"), scrollToggle: document.querySelector("#scroll-sync-toggle"), saveStatus: document.querySelector("#save-status"), dirty: document.querySelector("#dirty-indicator"), cursor: document.querySelector("#cursor-position"), toast: document.querySelector("#toast"), folderInput: document.querySelector("#folder-input"), bundleInput: document.querySelector("#bundle-input"), language: document.querySelector("#language-select"), uiLanguage: document.querySelector("#ui-language-select"), languageLabel: document.querySelector("#site-language-label"), uiLanguageLabel: document.querySelector("#ui-language-label")
};
const syntaxReference = createSyntaxReference(document.querySelector("#syntax-reference"), state.locale);
const scrollSync = createScrollSync(elements.editor, elements.frame);
elements.scrollToggle.checked = state.syncScroll;
scrollSync.setEnabled(state.syncScroll);

function currentFile() { return findFile(state.project, state.project.selectedPath); }

function pathFromUrl() { return new URL(location.href).searchParams.get("page"); }

function syncPageUrl(mode = "push") {
  const url = new URL(location.href);
  if (state.project.selectedPath) url.searchParams.set("page", state.project.selectedPath);
  else url.searchParams.delete("page");
  if (url.href === location.href) return;
  history[mode === "replace" ? "replaceState" : "pushState"](null, "", url);
}

function selectFile(path, historyMode = "push") {
  if (!findFile(state.project, path)) return false;
  const changed = state.project.selectedPath !== path;
  state.project.selectedPath = path;
  state.siteLanguage = normalizeLanguage(detectLanguage(path) || state.siteLanguage);
  renderLanguageOptions();
  if (changed) {
    renderTree();
    renderEditor();
    renderPreview();
    elements.editor.scrollTop = 0;
    elements.frame.scrollTop = 0;
  }
  if (historyMode) syncPageUrl(historyMode);
  return true;
}

function showToast(message) {
  elements.toast.textContent = message;
  elements.toast.classList.add("visible");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => elements.toast.classList.remove("visible"), 2400);
}

function setProject(project, { restoreFromUrl = false, historyMode = "replace" } = {}) {
  state.project = normalizeProject(project);
  const requestedPath = restoreFromUrl ? pathFromUrl() : null;
  if (requestedPath && findFile(state.project, requestedPath)) state.project.selectedPath = requestedPath;
  state.collapsedTreePaths.clear();
  state.siteLanguage = normalizeLanguage(detectLanguage(state.project.selectedPath) || state.siteLanguage);
  state.dirty = false;
  elements.name.value = state.project.name;
  renderLanguageOptions();
  applyUiLocale();
  renderTree();
  renderEditor();
  renderPreview();
  elements.editor.scrollTop = 0;
  elements.frame.scrollTop = 0;
  syncPageUrl(historyMode);
}

function renderTree() {
  const query = elements.filter.value.trim().toLowerCase();
  const visibleFiles = state.project.files.filter((file) => !query || file.path.toLowerCase().includes(query) || state.project.pages.find((page) => page.path === file.path)?.title.toLowerCase().includes(query));
  elements.count.textContent = `${visibleFiles.length} ${translatedString(state.locale, "pages")}`;
  elements.tree.replaceChildren();
  const root = { directories: new Map(), files: [] };
  for (const file of visibleFiles) {
    const parts = file.path.split("/");
    let node = root;
    for (const part of parts.slice(0, -1)) {
      if (!node.directories.has(part)) node.directories.set(part, { directories: new Map(), files: [] });
      node = node.directories.get(part);
    }
    node.files.push(file);
  }
  renderDirectoryTree(root, elements.tree, "", 0);
  if (!visibleFiles.length) elements.tree.innerHTML = `<div class="empty-state">${translatedString(state.locale, "noPages")}</div>`;
}

function renderDirectoryTree(node, container, prefix, depth) {
  for (const [name, child] of [...node.directories.entries()].sort(([left], [right]) => left.localeCompare(right))) {
    const wrapper = document.createElement("div"); wrapper.className = "tree-directory";
    const path = `${prefix}${name}/`;
    const collapsed = state.collapsedTreePaths.has(path);
    const toggle = document.createElement("button"); toggle.className = "tree-folder"; toggle.innerHTML = `<span class="tree-folder-chevron">${collapsed ? "▸" : "▾"}</span><span class="file-icon">▰</span><span></span>`; toggle.lastElementChild.textContent = name; toggle.title = path;
    const children = document.createElement("div"); children.className = "tree-children"; children.hidden = collapsed;
    toggle.setAttribute("aria-expanded", collapsed ? "false" : "true");
    toggle.addEventListener("click", () => {
      const nextCollapsed = !children.hidden;
      children.hidden = nextCollapsed;
      toggle.querySelector(".tree-folder-chevron").textContent = nextCollapsed ? "▸" : "▾";
      toggle.setAttribute("aria-expanded", nextCollapsed ? "false" : "true");
      if (nextCollapsed) state.collapsedTreePaths.add(path); else state.collapsedTreePaths.delete(path);
    });
    wrapper.append(toggle, children); container.append(wrapper);
    renderDirectoryTree(child, children, `${prefix}${name}/`, depth + 1);
  }
  for (const file of node.files.sort((left, right) => left.path.localeCompare(right.path))) {
    const page = state.project.pages.find((candidate) => candidate.path === file.path);
    const button = document.createElement("button"); button.className = `tree-item${file.path === state.project.selectedPath ? " selected" : ""}`; button.title = file.path;
    const icon = file.kind === "markdown" ? "#" : file.kind === "html" ? "◇" : file.encoding === "data-url" ? "▧" : "·";
    button.innerHTML = `<span class="file-icon">${icon}</span><span></span>`; button.lastElementChild.textContent = page?.title || file.path.split("/").at(-1);
    button.addEventListener("click", () => selectFile(file.path));
    container.append(button);
  }
}

function renderEditor() {
  const file = currentFile();
  if (!file) { elements.editor.value = ""; elements.editor.disabled = true; elements.editorLabel.textContent = "No file selected"; return; }
  elements.editor.disabled = file.encoding === "data-url";
  elements.editor.value = file.content;
  elements.editorLabel.textContent = file.path.split("/").pop();
  elements.editorMeta.textContent = `${file.kind === "markdown" ? "Markdown source" : file.kind === "html" ? "ExportSite HTML" : "Asset"} · ${file.path}`;
  updateCursor();
}

function renderPreview() {
  hideRichTooltip();
  const file = currentFile();
  if (!file) { elements.preview.innerHTML = `<div class="empty-state">${translatedString(state.locale, "emptyPreview")}</div>`; scrollSync.refresh("", false); return; }
  if (file.kind === "html") {
    elements.preview.innerHTML = `<iframe class="html-preview" sandbox="allow-scripts" title="ExportSite HTML preview"></iframe>`;
    const iframe = elements.preview.querySelector("iframe");
    scrollSync.refresh(file.content, false, iframe);
    iframe.srcdoc = buildHtmlPreview(file.content, file.path);
    return;
  }
  if (file.encoding === "data-url") {
    const media = document.createElement(file.content.startsWith("data:video/") ? "video" : file.content.startsWith("data:audio/") ? "audio" : "img");
    media.className = "asset-preview-media";
    media.src = file.content;
    if (media instanceof HTMLMediaElement) media.controls = true;
    const article = document.createElement("article");
    article.className = "guide-page asset-preview-page";
    article.append(media);
    elements.preview.replaceChildren(article);
    scrollSync.refresh(file.content, false);
    return;
  }
  if (file.kind === "asset" && file.encoding !== "data-url") {
    const article = document.createElement("article");
    article.className = "guide-page";
    const code = document.createElement("pre");
    code.className = "guide-code asset-preview-code";
    code.textContent = file.content;
    article.append(code);
    elements.preview.replaceChildren(article);
    scrollSync.refresh(file.content, false);
    return;
  }
  elements.preview.innerHTML = `<article class="guide-page">${renderMarkdown(file.content, { pagePath: file.path, resolveAsset: resolvePreviewAsset, resolveText: resolvePreviewText })}</article>`;
  installPreviewInteractions();
  scrollSync.refresh(file.content, true);
}

function resolvePreviewAsset(source, pagePath) {
  return findPreviewAsset(source, pagePath);
}

function resolvePreviewText(source, pagePath) {
  const file = findPreviewFile(source, pagePath);
  return file?.encoding === "text" ? file.content : "";
}

let activeRichTooltip = null;
let richTooltipHost = null;
let richTooltipGlobalListenersInstalled = false;
let richTooltipHideTimer = null;

function ensureRichTooltip() {
  if (richTooltipHost?.isConnected) return richTooltipHost;
  richTooltipHost = document.createElement("div");
  richTooltipHost.className = "guide-rich-tooltip";
  richTooltipHost.setAttribute("role", "tooltip");
  richTooltipHost.hidden = true;
  richTooltipHost.addEventListener("pointerenter", () => { if (richTooltipHideTimer) { clearTimeout(richTooltipHideTimer); richTooltipHideTimer = null; } });
  richTooltipHost.addEventListener("pointerleave", () => scheduleRichTooltipHide());
  richTooltipHost.addEventListener("pointerover", (event) => {
    const nestedHost = event.target.closest?.("[data-guide-tooltip-rich]");
    if (!nestedHost || nestedHost === activeRichTooltip) return;
    showRichTooltip(nestedHost);
  });
  richTooltipHost.addEventListener("focusin", (event) => {
    const nestedHost = event.target.closest?.("[data-guide-tooltip-rich]");
    if (nestedHost) showRichTooltip(nestedHost);
  });
  document.body.append(richTooltipHost);
  return richTooltipHost;
}

function positionRichTooltip(host) {
  if (!richTooltipHost || richTooltipHost.hidden) return;
  const anchor = host.querySelector(".guide-tooltip-trigger") || host;
  const anchorRect = anchor.getBoundingClientRect();
  const tooltipRect = richTooltipHost.getBoundingClientRect();
  const gap = 9;
  let left = anchorRect.right + gap;
  let top = anchorRect.top;
  if (left + tooltipRect.width > window.innerWidth - 8) left = anchorRect.left - tooltipRect.width - gap;
  if (left < 8) {
    left = Math.min(window.innerWidth - tooltipRect.width - 8, Math.max(8, anchorRect.left));
    top = anchorRect.bottom + gap;
    if (top + tooltipRect.height > window.innerHeight - 8) top = anchorRect.top - tooltipRect.height - gap;
  }
  top = Math.min(Math.max(8, top), Math.max(8, window.innerHeight - tooltipRect.height - 8));
  richTooltipHost.style.left = `${Math.round(Math.max(8, left))}px`;
  richTooltipHost.style.top = `${Math.round(top)}px`;
}

function showRichTooltip(host) {
  if (richTooltipHideTimer) { clearTimeout(richTooltipHideTimer); richTooltipHideTimer = null; }
  const template = host.querySelector("template[data-guide-tooltip-content]");
  if (!template) return;
  const tooltip = ensureRichTooltip();
  tooltip.innerHTML = template.innerHTML;
  tooltip.hidden = false;
  tooltip.dataset.visible = "true";
  activeRichTooltip = host;
  positionRichTooltip(host);
}

function scheduleRichTooltipHide(host = null) {
  if (richTooltipHideTimer) clearTimeout(richTooltipHideTimer);
  richTooltipHideTimer = setTimeout(() => {
    richTooltipHideTimer = null;
    if (richTooltipHost?.matches(":hover") || host?.matches(":hover")) return;
    hideRichTooltip(host);
  }, 160);
}

function hideRichTooltip(host = null) {
  if (richTooltipHideTimer) { clearTimeout(richTooltipHideTimer); richTooltipHideTimer = null; }
  if (host && activeRichTooltip && host !== activeRichTooltip) return;
  if (!richTooltipHost) return;
  richTooltipHost.hidden = true;
  richTooltipHost.removeAttribute("data-visible");
  richTooltipHost.replaceChildren();
  activeRichTooltip = null;
}

function installRichTooltipInteractions() {
  const hosts = elements.preview.querySelectorAll("[data-guide-tooltip-rich]");
  for (const host of hosts) {
    host.addEventListener("pointerenter", () => showRichTooltip(host));
    host.addEventListener("focusin", () => showRichTooltip(host));
    host.addEventListener("pointerleave", (event) => {
      if (!event.relatedTarget || !host.contains(event.relatedTarget)) scheduleRichTooltipHide(host);
    });
    host.addEventListener("focusout", (event) => {
      if (!event.relatedTarget || !host.contains(event.relatedTarget)) scheduleRichTooltipHide(host);
    });
  }
  if (!richTooltipGlobalListenersInstalled) {
    window.addEventListener("resize", () => { if (activeRichTooltip) positionRichTooltip(activeRichTooltip); }, { passive: true });
    elements.preview.addEventListener("scroll", () => { if (activeRichTooltip) positionRichTooltip(activeRichTooltip); }, { passive: true });
    richTooltipGlobalListenersInstalled = true;
  }
}

function installPreviewInteractions() {
  installRichTooltipInteractions();
  for (const tabs of elements.preview.querySelectorAll("[data-guide-tabs]")) {
    for (const button of tabs.querySelectorAll("[data-guide-tab]")) button.addEventListener("click", () => {
      const selected = button.dataset.guideTab;
      tabs.querySelectorAll("[data-guide-tab]").forEach((item) => item.classList.toggle("active", item === button));
      tabs.querySelectorAll("[data-guide-panel]").forEach((panel) => { panel.hidden = panel.dataset.guidePanel !== selected; });
    });
  }
  for (const diagram of elements.preview.querySelectorAll(".guide-mermaid-diagram")) {
    const svg = diagram.querySelector("svg");
    const baseWidth = Number(diagram.dataset.guideDiagramWidth) || svg?.viewBox?.baseVal?.width || 420;
    const baseHeight = Number(diagram.dataset.guideDiagramHeight) || svg?.viewBox?.baseVal?.height || 220;
    let zoom = Number(diagram.dataset.guideDiagramZoom) || 1;
    if (svg) {
      svg.style.width = `${Math.round(baseWidth * zoom)}px`;
      svg.style.height = `${Math.round(baseHeight * zoom)}px`;
    }
    const applyZoom = (nextZoom, event = null) => {
      const next = Math.max(0.5, Math.min(3, nextZoom));
      if (!svg || next === zoom) return;
      const rect = diagram.getBoundingClientRect();
      const anchorX = event ? event.clientX - rect.left + diagram.scrollLeft : diagram.scrollLeft + diagram.clientWidth / 2;
      const anchorY = event ? event.clientY - rect.top + diagram.scrollTop : diagram.scrollTop + diagram.clientHeight / 2;
      const ratio = next / zoom;
      zoom = next;
      diagram.dataset.guideDiagramZoom = String(zoom);
      svg.style.width = `${Math.round(baseWidth * zoom)}px`;
      svg.style.height = `${Math.round(baseHeight * zoom)}px`;
      if (event) {
        diagram.scrollLeft = Math.max(0, anchorX * ratio - (event.clientX - rect.left));
        diagram.scrollTop = Math.max(0, anchorY * ratio - (event.clientY - rect.top));
      }
    };
    diagram.addEventListener("wheel", (event) => {
      const content = event.target.closest?.(".guide-flowchart-label.has-rich-content, .guide-mermaid-node-content");
      if (content && content.scrollHeight > content.clientHeight && !event.ctrlKey) return;
      event.preventDefault();
      applyZoom(zoom * (event.deltaY < 0 ? 1.1 : 0.9), event);
    }, { passive: false });
    for (const node of diagram.querySelectorAll("[data-node-id]")) node.addEventListener("click", () => {
      const id = node.dataset.nodeId;
      const content = [...diagram.querySelectorAll("[data-node-content-id]")].find((item) => item.dataset.nodeContentId === id);
      if (content) content.open = !content.open;
    });
    let drag = null;
    diagram.addEventListener("pointerdown", (event) => {
      drag = { x: event.clientX, y: event.clientY, left: diagram.scrollLeft, top: diagram.scrollTop };
      diagram.setPointerCapture?.(event.pointerId);
      diagram.classList.add("is-panning");
    });
    diagram.addEventListener("pointermove", (event) => {
      if (!drag) return;
      diagram.scrollLeft = drag.left - (event.clientX - drag.x);
      diagram.scrollTop = drag.top - (event.clientY - drag.y);
    });
    const stopDrag = () => { drag = null; diagram.classList.remove("is-panning"); };
    diagram.addEventListener("pointerup", stopDrag);
    diagram.addEventListener("pointercancel", stopDrag);
    diagram.addEventListener("pointerleave", stopDrag);
  }
  for (const link of elements.preview.querySelectorAll("a[data-footnote-back]")) link.addEventListener("click", (event) => {
    event.preventDefault();
    const target = link.getAttribute("href");
    if (target?.startsWith("#")) document.getElementById(target.slice(1))?.scrollIntoView({ behavior: "smooth", block: "center" });
  });
  for (const link of elements.preview.querySelectorAll("a[data-guide-link]")) link.addEventListener("click", (event) => {
    const href = link.getAttribute("href") || "";
    if (!href || href.startsWith("#") || /^(?:https?:|mailto:|tel:|javascript:)/i.test(href)) return;
    const file = findPreviewFile(href, state.project.selectedPath);
    if (!file || (file.kind !== "markdown" && file.kind !== "html")) return;
    event.preventDefault();
    selectFile(file.path);
  });
}

function renderLanguageOptions() {
  const languages = collectProjectLanguages(state.project);
  elements.language.replaceChildren(...languages.map((language) => { const option = document.createElement("option"); option.value = language; option.textContent = languageLabel(language); option.selected = language === state.siteLanguage; return option; }));
  if (!languages.includes(state.siteLanguage)) state.siteLanguage = languages[0];
  elements.language.value = state.siteLanguage;
  const uiOptions = Object.entries(UI_LANGUAGES).map(([language, details]) => { const option = document.createElement("option"); option.value = language; option.textContent = details.label; option.selected = language === state.locale; return option; });
  if (!UI_LANGUAGES[state.locale]) { const option = document.createElement("option"); option.value = state.locale; option.textContent = languageLabel(state.locale); option.selected = true; uiOptions.push(option); }
  elements.uiLanguage.replaceChildren(...uiOptions);
  elements.uiLanguage.value = state.locale;
}

function applyUiLocale() {
  const locale = state.locale;
  document.documentElement.lang = locale.replace("_", "-");
  elements.languageLabel.textContent = translatedString(locale, "siteLanguage");
  elements.uiLanguageLabel.textContent = translatedString(locale, "uiLanguage");
  document.querySelector('[data-action="new"]').textContent = translatedString(locale, "new");
  document.querySelector('[data-action="import-folder"]').textContent = translatedString(locale, "importFolder");
  document.querySelector('[data-action="import-bundle"]').textContent = translatedString(locale, "importBundle");
  document.querySelector('[data-action="export-bundle"]').textContent = translatedString(locale, "exportBundle");
  document.querySelector(".sidebar .panel-heading span").textContent = translatedString(locale, "project");
  document.querySelector(".preview-heading > div > span")?.replaceChildren(translatedString(locale, "preview"));
  elements.saveStatus.textContent = translatedString(locale, state.dirty ? "unsaved" : "saved");
  document.querySelector("#scroll-sync-label").textContent = translatedString(locale, "syncScroll");
  elements.scrollToggle.setAttribute("aria-label", translatedString(locale, "syncScroll"));
  document.querySelector("#reference-button-label").textContent = locale === "zh_cn" || locale === "zh_tw" ? "语法" : "Syntax";
  document.querySelector('[data-action="reference"]').title = locale === "zh_cn" || locale === "zh_tw" ? "全部语法与示例" : "Syntax and examples";
  syntaxReference.setLocale(locale);
}

function buildHtmlPreview(source, pagePath) {
  let html = source.replace(/<base\b[^>]*>/gi, "");
  html = inlinePreviewStyles(html, pagePath);
  html = inlinePreviewRuntime(html);
  html = html.replace(/\b(?:src|poster)=["']([^"']+)["']/gi, (match, url) => {
    const asset = findPreviewAsset(url, pagePath);
    return asset ? `${match.slice(0, match.indexOf("=") + 1)}"${asset}"` : match;
  });
  html = html.replace(/\b(data-scene-src|data-scene-state-manifest-src|data-scene-placeholder-src)=["']([^"']+)["']/gi, (match, name, url) => {
    const asset = findPreviewAsset(url, pagePath);
    return asset ? `${name}="${asset}"` : match;
  });
  const resolved = html.replace(/<link\b([^>]*?)\bhref=["']([^"']+)["']([^>]*)>/gi, (match, before, url, after) => {
    const asset = findPreviewAsset(url, pagePath);
    return asset ? `<link${before}href="${asset}"${after}>` : match;
  });
  return injectPreviewScrollBridge(resolved);
}

function injectPreviewScrollBridge(html) {
  const source = `(() => {
    let root;
    let observer;
    const report = () => {
      if (!root) return;
      parent.postMessage({ type: "guidenh-scroll-position", top: root.scrollTop, max: Math.max(0, root.scrollHeight - root.clientHeight) }, "*");
    };
    const bind = () => {
      const next = document.querySelector(".guide-content") || document.scrollingElement;
      if (!next || next === root) { report(); return; }
      root?.removeEventListener("scroll", report);
      observer?.disconnect();
      root = next;
      root.addEventListener("scroll", report, { passive: true });
      if (window.ResizeObserver) {
        observer = new ResizeObserver(report);
        observer.observe(root);
        if (root.firstElementChild) observer.observe(root.firstElementChild);
      }
      report();
    };
    window.addEventListener("load", bind);
    document.addEventListener("DOMContentLoaded", bind);
    window.addEventListener("message", (event) => {
      if (event.data?.type !== "guidenh-set-scroll") return;
      bind();
      root.scrollTop = Math.max(0, Math.min(1, Number(event.data.ratio) || 0)) * Math.max(0, root.scrollHeight - root.clientHeight);
      report();
    });
    setTimeout(bind, 0);
  })();`;
  const script = `<script>${escapeScript(source)}</script>`;
  if (/<\/body>/i.test(html)) return html.replace(/<\/body>/i, `${script}</body>`);
  if (/<\/html>/i.test(html)) return html.replace(/<\/html>/i, `${script}</html>`);
  return `${html}${script}`;
}

function inlinePreviewStyles(html, pagePath) {
  const inlined = html.replace(/<link\b([^>]*?)rel=["']stylesheet["']([^>]*?)>/gi, (match, before, after) => {
    const hrefMatch = `${before}${after}`.match(/\bhref=["']([^"']+)["']/i);
    const href = hrefMatch?.[1];
    if (!href || /^(?:data:|https?:|blob:|#)/i.test(href)) return match;
    const cssFile = findPreviewFile(href, pagePath);
    if (!cssFile || cssFile.encoding === "data-url") return match;
    const css = rewriteCssUrls(cssFile.content, cssFile.path);
    return `<style data-guidenh-source="${escapeAttribute(cssFile.path)}">${css}</style>`;
  });
  return inlined.replace(/<style\b([^>]*)>([\s\S]*?)<\/style>/gi, (match, attributes, css) => `<style${attributes}>${rewriteCssUrls(css, pagePath)}</style>`);
}

function inlinePreviewRuntime(html) {
  return html.replace(/<script\b([^>]*?)\bsrc=["']([^"']+)["']([^>]*)>[\s\S]*?<\/script>/gi, (match, before, source, after) => {
    if (!/\btype=["']module["']/i.test(`${before}${after}`) || !/(?:^|\/)app\.js(?:[?#]|$)/i.test(source)) return match;
    const appFile = findPreviewFile(source, "");
    if (!appFile || appFile.encoding === "data-url") return match;
    const moduleUrl = createPreviewModuleUrl(appFile.path);
    if (!moduleUrl) return match;
    const runtime = `${createPreviewVendorResolver()}\nimport(${JSON.stringify(moduleUrl)}).catch((error) => console.error("GuideNH preview runtime failed", error));`;
    return `<script type="module">${escapeScript(runtime)}</script>`;
  });
}

function createPreviewVendorResolver() {
  const assets = {};
  for (const file of state.project?.files || []) {
    if (file.encoding !== "data-url" || !/model-viewer\/vendor\//i.test(file.path)) continue;
    const name = file.path.split("/").at(-1);
    if (name) assets[name] = file.content;
  }
  return `window.__guidenhResolveVendorAsset = (value) => { const key = String(value || "").replace(/^\.\//, ""); return ${JSON.stringify(assets)}[key] || value; };`;
}

function createPreviewModuleUrl(entryPath) {
  const cache = new Map();
  const building = new Set();
  const moduleUrl = (path) => {
    const normalized = normalizeWorkspacePath(path);
    if (!normalized) return null;
    if (cache.has(normalized)) return cache.get(normalized);
    const file = findFile(state.project, normalized);
    if (!file || file.encoding !== "text") return null;
    if (building.has(normalized)) return null;
    building.add(normalized);
    let source = String(file.content || "");
    const replaceSpecifier = (specifier) => {
      if (/^(?:data:|https?:|blob:|#|\/)/i.test(specifier)) return specifier;
      const dependency = findPreviewFile(specifier, normalized);
      if (!dependency || dependency.encoding !== "text") return specifier;
      return moduleUrl(dependency.path) || specifier;
    };
    source = source.replace(/(\bfrom\s*|\bimport\s*)(["'])([^"']+)\2/g, (match, prefix, quote, specifier) => {
      const replacement = replaceSpecifier(specifier);
      return `${prefix}${quote}${replacement}${quote}`;
    });
    source = source.replace(/\bimport\s*\(\s*(["'])([^"']+)\1\s*\)/g, (match, quote, specifier) => {
      const replacement = replaceSpecifier(specifier);
      return `import(${quote}${replacement}${quote})`;
    });
    source = patchPreviewModuleAssetResolution(source);
    const url = encodePreviewModule(source);
    cache.set(normalized, url);
    building.delete(normalized);
    return url;
  };
  return moduleUrl(entryPath);
}

function patchPreviewModuleAssetResolution(source) {
  const resolver = "window.__guidenhResolveVendorAsset ? window.__guidenhResolveVendorAsset : ((value) => value)";
  return String(source || "")
    .replace(/new URL\(`vendor\/\$\{value\.slice\(2\)\}`, import\.meta\.url\)\.toString\(\)/g, `(${resolver})(value)`)
    .replace(/new URL\((["'])\.\/diamond\.png\1\s*,\s*import\.meta\.url\)\.href/g, `(${resolver})("./diamond.png")`);
}

function encodePreviewModule(source) {
  const bytes = new TextEncoder().encode(String(source || ""));
  let binary = "";
  const chunkSize = 0x8000;
  for (let offset = 0; offset < bytes.length; offset += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(offset, Math.min(offset + chunkSize, bytes.length)));
  }
  return `data:text/javascript;base64,${btoa(binary)}`;
}

function escapeScript(source) {
  return String(source || "").replace(/<\/script/gi, "<\\/script");
}

function escapeAttribute(value) {
  return String(value || "").replaceAll("&", "&amp;").replaceAll('"', "&quot;");
}

function rewriteCssUrls(source, cssPath) {
  return String(source || "").replace(/url\(\s*(["']?)([^)"']+)\1\s*\)/gi, (match, quote, url) => {
    const asset = findPreviewAsset(url, cssPath);
    return asset ? `url("${asset}")` : match;
  });
}

function findPreviewAsset(url, pagePath) {
  if (/^(data:|https?:|blob:|#)/i.test(url)) return null;
  const file = findPreviewFile(url, pagePath);
  if (!file) return null;
  if (file.encoding === "data-url") return file.content;
  if (file.path.toLowerCase().endsWith(".svg")) return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(file.content)}`;
  return null;
}

function findPreviewFile(url, pagePath) {
  let clean;
  try { clean = decodeURIComponent(String(url || "").split(/[?#]/)[0]); } catch (_) { clean = String(url || "").split(/[?#]/)[0]; }
  clean = clean.replaceAll("\\", "/").trim().replace(/^\//, "");
  if (!clean) return null;
  const candidates = new Set();
  const add = (value) => {
    const normalized = normalizeWorkspacePath(value);
    if (normalized) candidates.add(normalized);
  };
  add(clean);
  const pageDirectory = String(pagePath || "").split("/").slice(0, -1).join("/");
  if (pageDirectory && !clean.startsWith("_site/") && !clean.startsWith("guides/")) add(`${pageDirectory}/${clean}`);
  add(`_res/${clean}`);
  add(`_site/${clean}`);
  if (pageDirectory) add(`${pageDirectory}/${clean}`);
  if (clean.startsWith("guidenh:")) add(`src/main/resources/assets/${clean.replace(":", "/")}`);
  if (/^[\w-]+:[^/]/.test(clean)) add(`src/main/resources/assets/${clean.replace(":", "/")}`);
  const siteAsset = clean.replace(/^_site\//, "").replace(/^_res\/[^/]+\//, "");
  add(`src/main/resources/assets/guidenh/siteexport/${siteAsset}`);
  add(`src/main/resources/assets/guidenh/${clean}`);
  add(`assets/guidenh/siteexport/${clean}`);
  add(`assets/guidenh/${clean}`);
  const direct = [...candidates].map((candidate) => findFile(state.project, candidate)).find(Boolean);
  if (direct || /\.(?:md|mdx|html?)$/i.test(clean)) return direct || null;
  const localizedPath = normalizeWorkspacePath(`${pageDirectory}/${clean}`);
  const fallbackPath = localizedPath.replace(/(^|\/)(_?)([a-z]{2}[_-][a-z]{2})(?=\/)/i, (_match, separator, prefix, language) =>
    language.toLowerCase().replace("-", "_") === "en_us" ? _match : `${separator}${prefix}en_us`);
  return fallbackPath !== localizedPath ? findFile(state.project, fallbackPath) : null;
}

function normalizeWorkspacePath(value) {
  const segments = [];
  for (const segment of String(value || "").replaceAll("\\", "/").split("/")) {
    if (!segment || segment === ".") continue;
    if (segment === "..") { segments.pop(); continue; }
    segments.push(segment);
  }
  return segments.join("/");
}

function scheduleSave() {
  state.dirty = true; elements.dirty.textContent = "Unsaved changes"; elements.saveStatus.textContent = "Saving…";
  window.clearTimeout(state.saveTimer);
  state.saveTimer = window.setTimeout(async () => {
    state.project = await saveProject(state.project);
    state.dirty = false; elements.dirty.textContent = ""; elements.saveStatus.textContent = "Saved in this browser";
  }, 450);
}

function updateCursor() {
  const before = elements.editor.value.slice(0, elements.editor.selectionStart || 0).split("\n");
  elements.cursor.textContent = `Ln ${before.length}, Col ${before.at(-1).length + 1}`;
}

function download(name, content, type = "application/json") {
  const href = URL.createObjectURL(new Blob([content], { type }));
  const link = document.createElement("a"); link.href = href; link.download = name; link.click(); URL.revokeObjectURL(href);
}

async function importFiles(fileList, name = "Imported ExportSite") {
  const imported = await filesToProject(fileList);
  imported.name = name;
  if (!imported.files.length) throw new Error("No supported files found");
  setProject(imported); await saveProject(state.project); showToast(`Imported ${imported.files.length} files`);
}

async function importZip(file) {
  const files = await zipToFiles(file);
  const imported = normalizeProject({ name: file.name.replace(/\.zip$/i, ""), files: stripCommonRoot(files) });
  if (!imported.files.length) throw new Error("No supported files found in ZIP");
  setProject(imported); await saveProject(state.project); showToast(`Imported ${imported.files.length} files from ZIP`);
}

async function handleAction(action) {
  try {
    if (action === "new") { await clearProject(); setProject(createSampleProject()); await saveProject(state.project); showToast("New project created"); }
    if (action === "import-folder") elements.folderInput.click();
    if (action === "import-bundle") elements.bundleInput.click();
    if (action === "export-bundle") download(`${state.project.name.replace(/\s+/g, "-").toLowerCase()}.guidenh.json`, serializeProject(state.project));
    if (action === "download-file") { const file = currentFile(); if (file) download(file.path.split("/").pop(), file.content, "text/plain;charset=utf-8"); }
    if (action === "add-page") { setProject(addPage(state.project), { historyMode: "push" }); scheduleSave(); }
    if (action === "refresh") renderPreview();
    if (action === "reference") syntaxReference.open();
    if (action === "format") { elements.editor.value = elements.editor.value.replaceAll("\r\n", "\n"); elements.editor.dispatchEvent(new Event("input")); }
    if (action === "theme") { state.theme = state.theme === "dark" ? "light" : "dark"; localStorage.setItem("guidenh-theme", state.theme); document.documentElement.dataset.theme = state.theme; }
  } catch (error) { showToast(error.message || "Action failed"); }
}

document.documentElement.dataset.theme = state.theme;
document.addEventListener("click", (event) => { const action = event.target.closest("[data-action]")?.dataset.action; if (action) handleAction(action); });
elements.filter.addEventListener("input", renderTree);
elements.language.addEventListener("change", () => {
  state.siteLanguage = normalizeLanguage(elements.language.value);
  localStorage.setItem("guidenh-site-language", state.siteLanguage);
  const target = localizedPagePath(state.project, state.project.selectedPath, state.siteLanguage);
  if (target !== state.project.selectedPath) selectFile(target);
});
elements.uiLanguage.addEventListener("change", () => {
  state.locale = normalizeLanguage(elements.uiLanguage.value);
  localStorage.setItem("guidenh-locale", state.locale);
  applyUiLocale();
  renderTree();
});
elements.scrollToggle.addEventListener("change", () => {
  state.syncScroll = elements.scrollToggle.checked;
  localStorage.setItem("guidenh-sync-scroll", String(state.syncScroll));
  scrollSync.setEnabled(state.syncScroll);
});
elements.name.addEventListener("input", () => { state.project.name = elements.name.value || "GuideNH project"; scheduleSave(); });
elements.editor.addEventListener("input", () => { state.project = updateFile(state.project, state.project.selectedPath, elements.editor.value); renderPreview(); scheduleSave(); });
elements.editor.addEventListener("keyup", updateCursor); elements.editor.addEventListener("click", updateCursor); elements.editor.addEventListener("select", updateCursor);
document.querySelectorAll("[data-view]").forEach((button) => button.addEventListener("click", () => { document.querySelectorAll("[data-view]").forEach((item) => item.classList.remove("active")); button.classList.add("active"); elements.frame.classList.toggle("mobile", button.dataset.view === "mobile"); elements.frame.classList.toggle("desktop", button.dataset.view !== "mobile"); scrollSync.refresh(elements.editor.value, currentFile()?.kind === "markdown"); }));
elements.folderInput.addEventListener("change", () => importFiles(elements.folderInput.files).catch((error) => showToast(error.message)));
elements.bundleInput.addEventListener("change", async () => {
  const file = elements.bundleInput.files[0]; if (!file) return;
  try {
    if (file.name.toLowerCase().endsWith(".zip")) await importZip(file);
    else {
      const parsed = JSON.parse(await file.text());
      setProject(parsed.project || parsed);
      await saveProject(state.project);
      showToast("Bundle restored");
    }
  } catch (error) { showToast(error.message || "Invalid bundle"); }
});
window.addEventListener("beforeunload", (event) => { if (state.dirty) { event.preventDefault(); event.returnValue = ""; } });
window.addEventListener("popstate", () => {
  const requestedPath = pathFromUrl();
  if (!requestedPath || !selectFile(requestedPath, null)) syncPageUrl("replace");
});

const restored = await loadProject();
setProject(restored ? normalizeProject(restored) : createSampleProject(), { restoreFromUrl: true });
