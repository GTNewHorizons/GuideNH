import { attr, booleanAttr, colorValue, cssLength, escapeHtml, numberAttr, parseAttributes, parseCsv, splitComma } from "./html-utils.js";
import { renderChart, renderFunctionGraph, renderMermaidDiagram } from "./visual-renderers.js";
import { translatedString } from "./language.js";

function copy(context, key) { return escapeHtml(translatedString(context?.locale || "en_us", key)); }

const INLINE_TAGS = new Set(["a", "abbr", "b", "br", "code", "del", "em", "i", "img", "kbd", "mark", "small", "span", "strong", "sub", "sup", "u", "Color", "Spoiler", "Tooltip", "SoundLink", "PlayerName", "KeyBind", "ItemImage", "ItemIcon", "ItemLink", "CommandLink", "Latex", "QuestLink", "FloatingImage"]);
const BLOCK_TAGS = new Set(["address", "article", "aside", "blockquote", "details", "div", "dl", "dt", "dd", "fieldset", "figcaption", "figure", "footer", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hr", "main", "nav", "ol", "p", "pre", "section", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "ul", "video", "audio", "Latex", "Tooltip", "Spoiler", "ContentTabs", "Tab", "FileTree", "Row", "Column", "FootnoteList", "ItemGrid", "ItemImage", "ItemIcon", "ItemLink", "Block", "BlockImage", "FloatingImage", "GameScene", "Scene", "Structure", "Mermaid", "CsvTable", "ColumnChart", "BarChart", "LineChart", "PieChart", "ScatterChart", "FunctionGraph", "Function", "Recipe", "RecipeFor", "RecipesFor", "ImportPonder", "Ponder", "QuestCard", "SubPages", "Category", "Special", "ImageAnnotation", "BlockStats", "BlockStat", "ImportStructure", "ImportStructureLib", "IsometricCamera", "PlaySound", "RemoveBlocks", "RemoveEntity", "ReplaceBlock", "PlaceBlock", "BlockAnnotationTemplate", "Entity", "BoxAnnotation", "LineAnnotation", "DiamondAnnotation", "TextAnnotation", "InputAnnotation", "SoundArea", "NodeContent", "Series", "LineSeries", "Slice", "PieInset", "Plot", "Point", "Tier", "Channel", "Facing", "Rotation", "Flip", "Orientation", "GregTechActiveController", "GregTechPlaceHatches", "Comment"]);
const SAFE_HTML_ATTRIBUTES = new Set(["class", "className", "id", "role", "aria-label", "aria-description", "colspan", "rowspan", "align", "width", "height", "style", "target", "rel", "href", "src", "alt", "poster", "controls", "autoplay", "loop", "muted", "playsinline"]);
const HTML_CONTAINER_TAGS = new Set(["address", "article", "aside", "blockquote", "details", "div", "dl", "fieldset", "figure", "footer", "header", "main", "nav", "ol", "section", "table", "tbody", "tfoot", "thead", "tr", "ul"]);
const RUNTIME_TAGS = new Set(["Block", "BlockImage", "BlockId", "ItemId", "GameScene", "Scene", "Structure", "Recipe", "RecipeFor", "RecipesFor", "ImportPonder", "Ponder", "QuestCard", "QuestLink", "BlockStats", "BlockStat", "ImportStructure", "ImportStructureLib", "IsometricCamera", "PlaySound", "RemoveBlocks", "RemoveEntity", "ReplaceBlock", "PlaceBlock", "BlockAnnotationTemplate", "Entity", "BoxAnnotation", "LineAnnotation", "DiamondAnnotation", "TextAnnotation", "InputAnnotation", "SoundArea", "Series", "LineSeries", "Slice", "PieInset", "Plot", "Point", "Tier", "Channel", "Facing", "Rotation", "Flip", "Orientation", "GregTechActiveController", "GregTechPlaceHatches"]);
const ITEM_PLACEHOLDER_TAGS = new Set(["ItemLink", "ItemImage", "ItemIcon", "Block", "BlockId", "ItemId"]);
const VOID_TAGS = new Set(["area", "br", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr"]);

export function parseFrontmatter(source) {
  const normalized = String(source ?? "").replaceAll("\r\n", "\n");
  if (!normalized.startsWith("---\n")) return { attributes: {}, ...extractReferences(normalized) };
  const end = normalized.indexOf("\n---", 4);
  if (end < 0) return { attributes: {}, body: normalized, definitions: new Map(), footnotes: new Map() };
  const raw = normalized.slice(4, end);
  const attributes = parseSimpleYaml(raw);
  const parsed = extractReferences(normalized.slice(end + 4));
  return { attributes, ...parsed };
}

function parseSimpleYaml(source) {
  const attributes = {};
  const stack = [{ indent: -1, value: attributes }];
  let pending = null;
  for (const line of source.split("\n")) {
    const indent = line.match(/^\s*/)?.[0].length || 0;
    const list = line.match(/^\s*-\s+(.+)$/);
    if (list) {
      if (pending && pending.indent < indent) {
        pending.parent[pending.key] = [];
        stack.at(-1).value = pending.parent[pending.key];
        pending = null;
      }
      if (Array.isArray(stack.at(-1).value)) stack.at(-1).value.push(stripYamlValue(list[1]));
      continue;
    }
    const entry = line.match(/^\s*([\w-]+):\s*(.*)$/);
    if (!entry) continue;
    const key = entry[1];
    const value = entry[2].trim();
    while (stack.length > 1 && stack.at(-1).indent >= indent) stack.pop();
    const target = stack.at(-1).value;
    pending = null;
    if (!value) {
      target[key] = {};
      pending = { indent, parent: target, key };
      stack.push({ indent, value: target[key] });
    } else target[key] = stripYamlValue(value);
  }
  return attributes;
}

function stripYamlValue(value) {
  const text = String(value).trim();
  if (text === "true") return true;
  if (text === "false") return false;
  if (/^-?\d+(?:\.\d+)?$/.test(text)) return Number(text);
  return text.replace(/^['"]|['"]$/g, "");
}

function extractReferences(source) {
  const definitions = new Map();
  const footnotes = new Map();
  const kept = [];
  const lines = String(source ?? "").split("\n");
  for (let index = 0; index < lines.length; index += 1) {
    const footnote = lines[index].match(/^\[\^([^\]]+)\]:\s*(.*)$/);
    if (footnote) {
      const body = [footnote[2]];
      while (index + 1 < lines.length && /^\s{2,}/.test(lines[index + 1])) body.push(lines[++index].trim());
      footnotes.set(footnote[1].toLowerCase(), body.join(" ")); continue;
    }
    const reference = lines[index].match(/^\[([^\]^][^\]]*)\]:\s*(\S+)(?:\s+["']([^"']*)["'])?\s*$/);
    if (reference) { definitions.set(reference[1].trim().toLowerCase(), { href: reference[2], title: reference[3] || "" }); continue; }
    kept.push(lines[index]);
  }
  return { body: kept.join("\n"), definitions, footnotes };
}

function normalizeNestedMarkdown(source) {
  const normalized = String(source ?? "").replaceAll("\r\n", "\n").replaceAll("\r", "\n");
  const lines = normalized.split("\n");
  while (lines.length && !lines[0].trim()) lines.shift();
  while (lines.length && !lines.at(-1).trim()) lines.pop();
  const indents = lines.filter((line) => line.trim()).map((line) => (line.match(/^\s*/) || [""])[0].length);
  const commonIndent = indents.length ? Math.min(...indents) : 0;
  return lines.map((line) => line.slice(Math.min(commonIndent, (line.match(/^\s*/) || [""])[0].length))).join("\n");
}

export function renderMarkdown(source, context = {}) {
  const withoutComments = String(source ?? "").replace(/\{\/\*[\s\S]*?\*\/\}/g, "");
  const parsed = parseFrontmatter(withoutComments);
  const ctx = { ...context, ...parsed, footnoteOrder: [], depth: context.depth || 0 };
  let html = renderBlocks(parsed.body, ctx);
  if (ctx.footnoteOrder.length && !context.suppressFootnotes) html += renderFootnotes(ctx);
  return html;
}

function renderBlocks(source, context) {
  const lines = String(source ?? "").replaceAll("\r\n", "\n").split("\n");
  const output = [];
  let index = 0;
  while (index < lines.length) {
    const line = lines[index];
    if (!line.trim()) { index += 1; continue; }
    const fence = line.match(/^\s{0,8}(`{3,}|~{3,})(.*)$/);
    if (fence) {
      const marker = fence[1][0];
      const close = fence[1];
      const meta = fence[2].trim();
      const languageMatch = meta.match(/^([\w+-]+)?\s*(.*)$/);
      const language = (languageMatch?.[1] || "text").toLowerCase();
      const metaAttributes = parseAttributes(languageMatch?.[2] || "");
      const body = [];
      index += 1;
      while (index < lines.length && !new RegExp(`^\\s*${marker}{${close.length},}\\s*$`).test(lines[index])) body.push(lines[index++]);
      index += 1;
      output.push(renderFence(language, metaAttributes, body.join("\n"), context));
      continue;
    }
    if (/^\s*\$\$/.test(line)) {
      const firstFormula = line.replace(/^\s*\$\$/, "");
      if (firstFormula.includes("$$")) {
        const end = firstFormula.indexOf("$$");
        output.push(renderLatex(firstFormula.slice(0, end), {}, false));
        const remainder = firstFormula.slice(end + 2).trim();
        if (remainder) output.push(`<p>${renderInline(remainder, context)}</p>`);
        index += 1;
        continue;
      }
      const formulaLines = [firstFormula];
      index += 1;
      while (index < lines.length && !lines[index].includes("$$")) formulaLines.push(lines[index++]);
      if (index < lines.length) formulaLines[formulaLines.length - 1] = formulaLines.at(-1).replace(/\$\$.*/, "");
      index += 1;
      output.push(renderLatex(formulaLines.join("\n"), {}, false));
      continue;
    }
    const opening = readBlockOpening(lines, index);
    if (opening && BLOCK_TAGS.has(opening.name)) {
      const extracted = extractBlock(lines, index, opening);
      output.push(renderBlockTag(opening.name, opening.attributes, extracted.body, context));
      index = extracted.nextIndex;
      continue;
    }
    if (/^\s{0,3}#{1,6}\s+/.test(line)) {
      const heading = line.match(/^\s*(#{1,6})\s+(.+?)\s*#*\s*$/);
      const text = heading?.[2] || "";
      const level = heading?.[1].length || 1;
      const id = slugify(stripMarkdown(text));
      output.push(`<h${level} id="${escapeHtml(id)}">${renderInline(text, context)}</h${level}>`);
      index += 1;
      continue;
    }
    if (/^\s{0,3}([-*_])(?:\s*\1){2,}\s*$/.test(line)) { output.push("<hr>"); index += 1; continue; }
    if (/^\s*>/.test(line)) {
      const quote = [];
      while (index < lines.length && /^\s*>/.test(lines[index])) quote.push(lines[index++].replace(/^\s*>\s?/, ""));
      output.push(renderQuote(quote.join("\n"), context)); continue;
    }
    const table = readMarkdownTable(lines, index);
    if (table) { output.push(renderMarkdownTable(table.rows, table.widths, context)); index = table.nextIndex; continue; }
    const list = readList(lines, index);
    if (list) { output.push(renderList(list.items, list.ordered, context)); index = list.nextIndex; continue; }
    const paragraph = [line];
    index += 1;
    while (index < lines.length && lines[index].trim() && !isBlockBoundary(lines, index)) paragraph.push(lines[index++]);
    output.push(renderParagraph(paragraph.join("\n"), context));
  }
  return output.join("\n");
}

function renderParagraph(source, context) {
  const trimmed = String(source || "").trim();
  const standaloneTag = readTagAt(trimmed, 0);
  if (standaloneTag && standaloneTag.nextIndex === trimmed.length && (standaloneTag.name === "Latex" || BLOCK_TAGS.has(standaloneTag.name) || BLOCK_TAGS.has(standaloneTag.name.toLowerCase()))) {
    return renderTag(standaloneTag.name, standaloneTag.attributes, standaloneTag.body, standaloneTag.selfClosing, context, false);
  }
  const blockNames = [...BLOCK_TAGS].filter((name) => !["ItemImage", "ItemIcon", "ItemLink", "Block"].includes(name));
  if (new RegExp(`<\\/?(?:${blockNames.join("|")})\\b`, "i").test(source)) return renderInline(source, context);
  return `<p>${renderInline(source, context)}</p>`;
}

function isBlockBoundary(lines, index) {
  const line = lines[index];
  return /^\s{0,3}(?:#{1,6}\s|```|~~~|>|[-*_](?:\s*[-*_]){2,}\s*$|[-*+]\s+|\d+\.\s+|\|)/.test(line) || Boolean(readBlockOpening(lines, index));
}

function readBlockOpening(lines, index) {
  if (!/^\s*</.test(lines[index])) return null;
  const inlineTag = readTagAt(lines[index].trim(), 0);
  const trailing = inlineTag ? lines[index].trim().slice(inlineTag.nextIndex).trim() : "";
  if (inlineTag && !trailing && BLOCK_TAGS.has(inlineTag.name)) {
    return { name: inlineTag.name, attributes: inlineTag.attributes, selfClosing: inlineTag.selfClosing, inlineBody: inlineTag.body, openingEnd: index };
  }
  if (inlineTag && trailing) return null;
  let text = lines[index].trim();
  let endIndex = index;
  while (!text.includes(">") && endIndex + 1 < lines.length && endIndex - index < 12) text += ` ${lines[++endIndex].trim()}`;
  const match = text.match(/^<([A-Za-z][\w-]*)([\s\S]*?)>$/);
  if (!match || /^<\//.test(text) || match[1].toLowerCase() === "http") return null;
  const selfClosing = /\/\s*>$/.test(text);
  return { name: match[1], attributes: parseAttributes(match[2].replace(/\/\s*$/, "")), selfClosing, openingEnd: endIndex };
}

function extractBlock(lines, index, opening) {
  if (opening.inlineBody !== undefined) return { body: opening.inlineBody, nextIndex: index + 1 };
  if (opening.selfClosing) return { body: "", nextIndex: opening.openingEnd + 1 };
  const pattern = new RegExp(`<\\/?${opening.name}\\b[^>]*>`, "gi");
  let depth = 1;
  const body = [];
  let cursor = opening.openingEnd + 1;
  for (; cursor < lines.length; cursor += 1) {
    const line = lines[cursor];
    const tokens = line.match(pattern) || [];
    let consumed = false;
    for (const token of tokens) {
      consumed = true;
      if (/^<\//.test(token)) depth -= 1;
      else if (!/\/\s*>$/.test(token)) depth += 1;
    }
    if (depth <= 0) return { body: body.join("\n"), nextIndex: cursor + 1 };
    body.push(line);
    if (consumed && depth < 1) break;
  }
  return { body: body.join("\n"), nextIndex: cursor };
}

function renderFence(language, attributes, body, context) {
  if (language === "mermaid") return renderMermaidDiagram(body);
  if (language === "csv") return renderCsvTable(body, attributes, context);
  if (language === "tree" || language === "filetree") return renderFileTree(body, attributes, context);
  if (language === "funcgraph" || language === "functiongraph") return renderFunctionGraph(attributesToText(attributes), body);
  const height = cssLength(attr(attributes, "height"), "");
  const style = height ? ` style="max-height:${escapeHtml(height)};overflow:auto"` : "";
  return `<pre class="guide-code"${style}><code class="language-${escapeHtml(language)}">${escapeHtml(body)}</code></pre>`;
}

function attributesToText(attributes) {
  return Object.entries(attributes).map(([key, value]) => `${key}="${String(value).replaceAll('"', "&quot;")}"`).join(" ");
}

function readMarkdownTable(lines, index) {
  if (!/^\s*\|?.*\|.*$/.test(lines[index]) || !/^\s*\|?\s*:?-{1,}:?\s*(?:\|\s*:?-{1,}:?\s*)+\|?\s*$/.test(lines[index + 1] || "")) return null;
  const rows = [splitTableRow(lines[index]), splitTableRow(lines[index + 1])];
  let cursor = index + 2;
  while (cursor < lines.length && /^\s*\|?.*\|.*$/.test(lines[cursor]) && lines[cursor].trim()) rows.push(splitTableRow(lines[cursor++]));
  let widths = [];
  const hint = lines[cursor]?.match(/^\s*\{:\s*widths\s*=\s*(?:["']([^"']+)["']|([^}\s]+))\s*\}\s*$/);
  if (hint) { widths = splitComma(hint[1] || hint[2]).map(Number).filter(Number.isFinite); cursor += 1; }
  return { rows, widths, nextIndex: cursor };
}

function splitTableRow(line) {
  let text = line.trim();
  if (text.startsWith("|")) text = text.slice(1);
  if (text.endsWith("|")) text = text.slice(0, -1);
  return text.split(/(?<!\\)\|/).map((cell) => cell.replaceAll("\\|", "|").trim());
}

function renderMarkdownTable(rows, widths, context) {
  const columns = Math.max(...rows.map((row) => row.length), 1);
  const colgroup = widths.length ? `<colgroup>${Array.from({ length: columns }, (_item, index) => `<col style="width:${Number(widths[index] || 0) || "auto"}px">`).join("")}</colgroup>` : "";
  const head = rows[0] || [];
  const body = rows.slice(2);
  return `<div class="guide-table-wrap"><table>${colgroup}<thead><tr>${Array.from({ length: columns }, (_item, index) => `<th>${renderInline(head[index] || "", context)}</th>`).join("")}</tr></thead><tbody>${body.map((row) => `<tr>${Array.from({ length: columns }, (_item, index) => `<td>${renderInline(row[index] || "", context)}</td>`).join("")}</tr>`).join("")}</tbody></table></div>`;
}

function readList(lines, index) {
  const first = lines[index].match(/^\s*(-|\*|\+|\d+\.)\s+(.*)$/);
  if (!first) return null;
  const ordered = /\d+\./.test(first[1]);
  const pattern = ordered ? /^\s*\d+\.\s+(.*)$/ : /^\s*[-*+]\s+(.*)$/;
  const items = [];
  let cursor = index;
  while (cursor < lines.length) {
    const match = lines[cursor].match(pattern);
    if (!match) break;
    items.push(match[1]); cursor += 1;
  }
  return { ordered, items, nextIndex: cursor };
}

function renderList(items, ordered, context) {
  const tag = ordered ? "ol" : "ul";
  return `<${tag}>${items.map((item) => {
    const task = item.match(/^\[([ xX])\]\s+(.*)$/);
    return `<li>${task ? `<input type="checkbox" disabled ${task[1].toLowerCase() === "x" ? "checked" : ""}> ` : ""}${renderInline(task ? task[2] : item, context)}</li>`;
  }).join("")}</${tag}>`;
}

function renderQuote(source, context) {
  const lines = String(source || "").split("\n");
  const alert = lines[0]?.match(/^\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]$/i);
  const directive = lines[0]?.match(/^\{:\s*([\s\S]*?)\s*\}$/);
  const body = alert || directive ? lines.slice(1).join("\n") : source;
  if (directive) {
    const attributes = parseAttributes(directive[1]);
    const accent = colorValue(attr(attributes, "color", "accent"), "var(--preview-quote-default)");
    const title = String(attr(attributes, "title", "label") || "").trim();
    const iconValue = attr(attributes, "icon");
    const iconItem = attr(attributes, "iconItem", "icon_item");
    const iconPng = attr(attributes, "iconPng", "icon_png");
    const icon = iconItem ? renderItemPlaceholder("ItemIcon", { id: iconItem, showId: false }, context, true) : iconPng ? `<img class="guide-quote-icon-image" src="${escapeHtml(resolveAsset(iconPng, context) || iconPng)}" alt="">` : iconValue ? `<span class="guide-quote-icon">${escapeHtml(iconValue)}</span>` : "";
    return `<blockquote class="guide-quote" style="--guide-quote-accent:${accent}">${title || icon ? `<div class="guide-quote-title">${icon}${title ? `<span>${escapeHtml(title)}</span>` : ""}</div>` : ""}<div class="guide-quote-body">${renderBlocks(normalizeNestedMarkdown(body), { ...context, suppressFootnotes: true })}</div></blockquote>`;
  }
  if (alert) {
    const type = alert[1].toUpperCase();
    const alertStyles = {
      NOTE: { color: "var(--preview-alert-note)", icon: "ⓘ" },
      TIP: { color: "var(--preview-alert-tip)", icon: "✦" },
      IMPORTANT: { color: "var(--preview-alert-important)", icon: "➤" },
      WARNING: { color: "var(--preview-alert-warning)", icon: "⚠" },
      CAUTION: { color: "var(--preview-alert-caution)", icon: "☢" },
    };
    const style = alertStyles[type] || alertStyles.NOTE;
    return `<blockquote class="guide-quote guide-alert guide-alert-${type.toLowerCase()}" style="--guide-quote-accent:${style.color}"><div class="guide-quote-title"><span class="guide-quote-icon">${style.icon}</span><span>${type}</span></div><div class="guide-quote-body">${renderBlocks(normalizeNestedMarkdown(body), { ...context, suppressFootnotes: true })}</div></blockquote>`;
  }
  return `<blockquote class="guide-quote"><div class="guide-quote-body">${renderBlocks(normalizeNestedMarkdown(source), { ...context, suppressFootnotes: true })}</div></blockquote>`;
}

function renderInline(source, context) {
  const text = String(source ?? "");
  const tokens = [];
  const codeProtected = text.replace(/`([^`\n]+)`/g, (_match, code) => {
    const marker = `\u0000${tokens.length}\u0000`;
    tokens.push(`<code>${escapeHtml(code)}</code>`);
    return marker;
  });
  let cursor = 0;
  let protectedText = "";
  while (cursor < codeProtected.length) {
    const next = findInlineToken(codeProtected, cursor);
    if (!next) { protectedText += codeProtected.slice(cursor); break; }
    protectedText += codeProtected.slice(cursor, next.index);
    const token = readInlineToken(codeProtected, next.index, context);
    if (!token) { protectedText += codeProtected[next.index]; cursor = next.index + 1; continue; }
    const marker = `\u0000${tokens.length}\u0000`;
    tokens.push(token.html);
    protectedText += marker;
    cursor = token.nextIndex;
  }
  let html = escapeHtml(protectedText);
  html = html.replace(/  \n/g, "<br>");
  html = html.replace(/\*\*([^*\n]+)\*\*/g, "<strong>$1</strong>").replace(/__([^_\n]+)__/g, "<strong>$1</strong>");
  html = html.replace(/~~([^~\n]+)~~/g, "<del>$1</del>").replace(/==([^=\n]+)==/g, "<mark>$1</mark>");
  html = html.replace(/\*([^*\n]+)\*/g, "<em>$1</em>").replace(/_([^_\n]+)_/g, "<em>$1</em>");
  return html.replace(/\u0000(\d+)\u0000/g, (_match, index) => tokens[Number(index)] || "");
}

function findInlineToken(text, start) {
  const candidates = ["<", "![", "[", "$$", "[^"].map((needle) => ({ needle, index: text.indexOf(needle, start) })).filter((item) => item.index >= 0).sort((left, right) => left.index - right.index);
  return candidates[0] || null;
}

function readInlineToken(text, index, context) {
  if (text.startsWith("$$", index)) {
    const end = text.indexOf("$$", index + 2);
    if (end >= 0) return { html: renderLatex(text.slice(index + 2, end), {}, true), nextIndex: end + 2 };
  }
  if (text.startsWith("![", index)) {
    const match = text.slice(index).match(/^!\[([^\]]*)\]\(([^\s)]+)(?:\s+["']([^"']*)["'])?\)/);
    if (match) return { html: renderImage(match[1], match[2], match[3], context), nextIndex: index + match[0].length };
    const reference = text.slice(index).match(/^!\[([^\]]*)\]\[([^\]]*)\]/);
    if (reference) { const target = context.definitions?.get((reference[2] || reference[1]).toLowerCase()); if (target) return { html: renderImage(reference[1], target.href, target.title, context), nextIndex: index + reference[0].length }; }
  }
  if (text[index] === "[") {
    const footnote = text.slice(index).match(/^\[\^([^\]]+)\]/);
    if (footnote && context.footnotes?.has(footnote[1].toLowerCase())) {
      const key = footnote[1].toLowerCase();
      let number = context.footnoteOrder.indexOf(key) + 1;
      if (!number) { context.footnoteOrder.push(key); number = context.footnoteOrder.length; }
      return { html: `<sup id="footnote-ref-${escapeHtml(key)}-${number}" class="guide-footnote-ref"><a href="#footnote-${escapeHtml(key)}">[${number}]</a></sup>`, nextIndex: index + footnote[0].length };
    }
    const link = text.slice(index).match(/^\[([^\]]+)\]\(([^\s)]+)(?:\s+["']([^"']*)["'])?\)/);
    if (link) return { html: `<a href="${escapeHtml(link[2])}"${link[3] ? ` aria-label="${escapeHtml(link[3])}"` : ""} data-guide-link>${renderInline(link[1], context)}</a>`, nextIndex: index + link[0].length };
    const reference = text.slice(index).match(/^\[([^\]]+)\]\[([^\]]*)\]/);
    if (reference) { const target = context.definitions?.get((reference[2] || reference[1]).toLowerCase()); if (target) return { html: `<a href="${escapeHtml(target.href)}"${target.title ? ` aria-label="${escapeHtml(target.title)}"` : ""} data-guide-link>${renderInline(reference[1], context)}</a>`, nextIndex: index + reference[0].length }; }
  }
  if (text[index] === "<") {
    const tag = readTagAt(text, index);
    if (tag) return { html: renderTag(tag.name, tag.attributes, tag.body, tag.selfClosing, context, true), nextIndex: tag.nextIndex };
  }
  return null;
}

function readTagAt(text, index) {
  const first = text.slice(index).match(/^<([A-Za-z][\w-]*)\b/);
  if (!first) return null;
  const name = first[1];
  let end = index + first[0].length;
  let quote = "";
  let braceDepth = 0;
  for (; end < text.length; end += 1) {
    const character = text[end];
    if (quote) { if (character === quote) quote = ""; continue; }
    if (character === '"' || character === "'") { quote = character; continue; }
    if (character === "{") { braceDepth += 1; continue; }
    if (character === "}") { braceDepth = Math.max(0, braceDepth - 1); continue; }
    if (character === ">" && !braceDepth) break;
  }
  if (end >= text.length) return null;
  const opening = text.slice(index, end + 1);
  const selfClosing = /\/\s*>$/.test(opening);
  if (selfClosing || VOID_TAGS.has(name.toLowerCase())) return { name, attributes: parseAttributes(opening.slice(first[0].length, selfClosing ? -2 : -1)), body: "", selfClosing: true, nextIndex: end + 1 };
  const closingPattern = new RegExp(`<\\/?${name}\\b[^>]*>`, "gi");
  closingPattern.lastIndex = end + 1;
  let depth = 1;
  let token;
  while ((token = closingPattern.exec(text))) {
    if (/^<\//.test(token[0])) depth -= 1;
    else if (!/\/\s*>$/.test(token[0])) depth += 1;
    if (depth === 0) return { name, attributes: parseAttributes(opening.slice(first[0].length, -1)), body: text.slice(end + 1, token.index), selfClosing: false, nextIndex: closingPattern.lastIndex };
  }
  return null;
}

function renderRichTooltip(attributes, body, context) {
  const label = renderInline(String(attr(attributes, "label", "text") || "tooltip"), context);
  const content = renderMarkdown(normalizeNestedMarkdown(body), { ...context, suppressFootnotes: true, depth: (context.depth || 0) + 1 });
  return `<span class="guide-tooltip-inline" data-guide-tooltip-rich="true"><span class="guide-tooltip-trigger" tabindex="0">${label}</span><template data-guide-tooltip-content>${content}</template></span>`;
}

function renderTag(name, attributes, body, selfClosing, context, inline) {
  if (name === "Comment") return "";
  const lowerName = name.toLowerCase();
  if (lowerName === "img") {
    const source = attr(attributes, "src") || "";
    const resolved = resolveAsset(source, context) || source;
    return `<img class="guide-inline-image" src="${escapeHtml(resolved)}" alt="${escapeHtml(attr(attributes, "alt", "title") || "")}">`;
  }
  if (lowerName === "video" || lowerName === "audio") return renderMedia(lowerName, attributes, body, context);
  if (ITEM_PLACEHOLDER_TAGS.has(name)) {
    const wrap = String(attr(attributes, "wrap") || "inline").toLowerCase();
    return wrap !== "inline" ? applyWrap(renderItemPlaceholder(name, attributes, context, false, body), attributes) : renderItemPlaceholder(name, attributes, context, inline, body);
  }
  if (name === "BlockImage" || name === "Structure" || name === "Recipe" || name === "RecipeFor" || name === "RecipesFor" || name === "QuestCard" || name === "ImportPonder" || name === "Ponder") return renderRuntimePlaceholder(name, attributes, inline, context, body);
  if (name === "GameScene" || name === "Scene") return renderScenePlaceholder(name, attributes, context);
  if (name === "FloatingImage") return renderFloatingImage(attributes, body, context, inline);
  if (name === "Latex") return renderLatex(String(attr(attributes, "formula") || body || ""), attributes, inline, body && body.trim() ? renderMarkdown(normalizeNestedMarkdown(body), { ...context, suppressFootnotes: true }) : "", true, context);
  if (name === "Color") return `<span class="guide-color" style="color:${colorValue(attr(attributes, "color", "id"), "var(--accent)")}">${renderInline(body, context)}</span>`;
  if (name === "Spoiler") return `<span class="guide-spoiler" tabindex="0"><span class="guide-spoiler-content">${renderInline(body, context)}</span></span>`;
  if (name === "Tooltip") return renderRichTooltip(attributes, body, context);
  if (name === "PlayerName") return `<span class="guide-runtime-inline guide-player-name">[player]</span>`;
  if (name === "KeyBind") return `<kbd class="guide-keybind">${escapeHtml(attr(attributes, "id", "action") || "key")}</kbd>`;
  if (name === "SoundLink") return `<span class="guide-sound-link" data-sound="${escapeHtml(attr(attributes, "sound", "id") || "sound")}">${renderInline(body || attr(attributes, "label", "text") || "Sound", context)}</span>`;
  if (name === "CommandLink" || name === "QuestLink") return `<span class="guide-runtime-inline guide-link-placeholder">${renderInline(body || attr(attributes, "text", "id") || name, context)}</span>`;
  if (lowerName === "br") return `<br${attr(attributes, "clear") ? ` data-clear="${escapeHtml(attr(attributes, "clear"))}"` : ""}>`;
  if (["a", "abbr", "b", "code", "del", "em", "i", "kbd", "mark", "small", "span", "strong", "sub", "sup", "u"].includes(lowerName)) {
    if (lowerName === "a") return renderSafeHtmlTag("a", attributes, body, context);
    return `<${lowerName}${renderSafeHtmlAttributes(attributes)}>${renderInline(body, context)}</${lowerName}>`;
  }
  if (BLOCK_TAGS.has(name)) return renderBlockTag(name, attributes, body, context);
  if (BLOCK_TAGS.has(lowerName)) return renderBlockTag(lowerName, attributes, body, context);
  if (INLINE_TAGS.has(name) || INLINE_TAGS.has(lowerName)) return `<span>${renderInline(body, context)}</span>`;
  if (name[0] === name[0]?.toLowerCase()) return renderSafeHtmlTag(lowerName, attributes, body, context);
  return `<span class="guide-tag-card unknown guide-inline-unknown"><span class="guide-tag-name">&lt;${escapeHtml(name)}&gt;</span><em>${copy(context, "previewPending")}</em></span>`;
}

function renderBlockTag(name, attributes, body, context) {
  if (name === "Comment") return "";
  if (name === "GameScene" || name === "Scene") return applyWrap(renderScenePlaceholder(name, attributes, context), attributes);
  if (name === "Tooltip") return renderRichTooltip(attributes, body, context);
  if (name === "Spoiler") return `<span class="guide-spoiler" tabindex="0"><span class="guide-spoiler-content">${renderInline(body, context)}</span></span>`;
  if (name === "Latex") return renderLatex(String(attr(attributes, "formula") || body || ""), attributes, false, body && body.trim() ? renderMarkdown(normalizeNestedMarkdown(body), { ...context, suppressFootnotes: true }) : "", true, context);
  if (ITEM_PLACEHOLDER_TAGS.has(name)) return applyWrap(renderItemPlaceholder(name, attributes, context, false, body), attributes);
  if (RUNTIME_TAGS.has(name)) return applyWrap(renderRuntimePlaceholder(name, attributes, false, context, body), attributes);
  if (name === "Row" || name === "Column") {
    const direction = name === "Row" ? "row" : "column";
    const gap = numberAttr(attributes, 5, "gap");
    const align = attr(attributes, "alignItems") || "stretch";
    const width = cssLength(attr(attributes, "width"), attr(attributes, "fullWidth") === true ? "100%" : "auto");
    return `<div class="guide-layout guide-${direction}" style="--guide-gap:${gap}px;align-items:${escapeHtml(align)};width:${escapeHtml(width)}">${renderBlocks(body, { ...context, depth: (context.depth || 0) + 1 })}</div>`;
  }
  if (name === "div") {
    const className = attr(attributes, "class", "className");
    const style = attr(attributes, "style");
    return `<div${className ? ` class="${escapeHtml(className)}"` : ""}${style ? ` style="${escapeHtml(style)}"` : ""}>${renderBlocks(body, context)}</div>`;
  }
  if (name === "section" || name === "article" || name === "figure") return `<${name}>${renderBlocks(body, context)}</${name}>`;
  if (name === "video" || name === "audio") return renderMedia(name, attributes, body, context);
  if (name === "details") {
    const summaryMatch = body.match(/<summary\b[^>]*>([\s\S]*?)<\/summary>/i);
    const content = summaryMatch ? body.replace(summaryMatch[0], "") : body;
    const height = cssLength(attr(attributes, "height"), "");
    const width = cssLength(attr(attributes, "width"), "");
    const details = `<details class="guide-details"${booleanAttr(attributes, "open") === false ? "" : " open"} style="${width ? `width:${escapeHtml(width)};` : ""}${height ? `--guide-details-height:${escapeHtml(height)};` : ""}"><summary>${summaryMatch ? renderInline(summaryMatch[1], context) : copy(context, "details")}</summary><div class="guide-details-body">${renderBlocks(content, { ...context, suppressFootnotes: true })}</div></details>`;
    return applyWrap(details, attributes);
  }
  if (name === "ContentTabs") return renderContentTabs(attributes, body, context);
  if (name === "Tab") return renderBlocks(body, context);
  if (name === "FloatingImage") return renderFloatingImage(attributes, body, context, false);
  if (name === "FileTree") return renderFileTree(body, attributes, context);
  if (name === "FootnoteList") return `<section class="guide-footnotes" style="width:${escapeHtml(cssLength(attr(attributes, "width"), "100%"))}">${renderBlocks(body, { ...context, suppressFootnotes: true })}</section>`;
  if (name === "CsvTable") {
    const source = resolveTextAsset(attr(attributes, "src"), context) || body;
    return renderCsvTable(source || "", attributes, context);
  }
  if (name === "Mermaid") {
    const source = attr(attributes, "src") ? resolveTextAsset(attr(attributes, "src"), context) : body;
    const nodeContents = [...String(body || "").matchAll(/<NodeContent\b([^>]*)>([\s\S]*?)<\/NodeContent>/gi)].map((match) => {
      const nodeAttributes = parseAttributes(match[1]);
      return { id: String(attr(nodeAttributes, "id") || ""), html: renderBlocks(normalizeNestedMarkdown(match[2]), { ...context, suppressFootnotes: true }) };
    }).filter((entry) => entry.id);
    return renderMermaidDiagram(source || "", { width: attr(attributes, "width"), height: attr(attributes, "height"), sourceLabel: attr(attributes, "src"), nodeContents });
  }
  if (["ColumnChart", "BarChart", "LineChart", "PieChart", "ScatterChart"].includes(name)) return renderChart(name, attributesToText(attributes), body);
  if (name === "FunctionGraph") return renderFunctionGraph(attributesToText(attributes), body);
  if (name === "Function") return renderFunctionGraph(attributesToText(attributes), body, attr(attributes, "expr"));
  if (name === "ItemGrid") return applyWrap(`<div class="guide-item-grid">${renderInline(body, context)}</div>`, attributes);
  if (name === "SubPages" || name === "Category" || name === "Special") return `<div class="guide-runtime-panel"><strong>${escapeHtml(name)}</strong><span>${copy(context, "navigationRuntime")}</span></div>`;
  if (name === "ImageAnnotation") return renderStandaloneImageAnnotation(attributes, body, context);
  if (name[0] === name[0]?.toLowerCase()) return renderSafeHtmlTag(name.toLowerCase(), attributes, body, context);
  return `<div class="guide-tag-card unknown"><span class="guide-tag-name">&lt;${escapeHtml(name)}&gt;</span>${body ? renderBlocks(body, context) : `<em>${copy(context, "previewPending")}</em>`}</div>`;
}

function renderMedia(name, attributes, body, context) {
  const source = attr(attributes, "src");
  const resolved = source ? resolveAsset(source, context) || source : "";
  const mediaAttributes = [`class="guide-media guide-${name}"`];
  if (resolved) mediaAttributes.push(`src="${escapeHtml(resolved)}"`);
  if (name === "video" && attr(attributes, "poster")) mediaAttributes.push(`poster="${escapeHtml(resolveAsset(attr(attributes, "poster"), context) || attr(attributes, "poster"))}"`);
  if (booleanAttr(attributes, "controls") !== false) mediaAttributes.push("controls");
  for (const key of ["autoplay", "loop", "muted", "playsinline"]) if (booleanAttr(attributes, key)) mediaAttributes.push(key);
  const sources = [...String(body || "").matchAll(/<source\b([^>]*)\/?>/gi)].map((match) => {
    const sourceAttributes = parseAttributes(match[1]);
    const sourceUrl = attr(sourceAttributes, "src");
    if (!sourceUrl) return "";
    return `<source src="${escapeHtml(resolveAsset(sourceUrl, context) || sourceUrl)}"${attr(sourceAttributes, "type") ? ` type="${escapeHtml(attr(sourceAttributes, "type"))}"` : ""}>`;
  }).join("");
  return `<${name} ${mediaAttributes.join(" ")}>${sources || (body && !/<source\b/i.test(body) ? renderInline(body, context) : "")}</${name}>`;
}

function renderSafeHtmlTag(name, attributes, body, context) {
  const tag = String(name || "span").toLowerCase();
  if (!/^[a-z][a-z0-9-]*$/.test(tag)) return renderInline(body, context);
  const content = ["img", "br", "hr", "input", "source", "meta", "link"].includes(tag)
    ? ""
    : tag === "pre" ? escapeHtml(body) : (HTML_CONTAINER_TAGS.has(tag) ? renderBlocks(body, context) : renderInline(body, context));
  return `<${tag}${renderSafeHtmlAttributes(attributes)}>${content}</${tag}>`;
}

function renderSafeHtmlAttributes(attributes) {
  const result = [];
  for (const [key, rawValue] of Object.entries(attributes || {})) {
    const normalizedKey = key === "className" ? "class" : key.toLowerCase();
    if (!SAFE_HTML_ATTRIBUTES.has(key) && !SAFE_HTML_ATTRIBUTES.has(normalizedKey)) continue;
    if (rawValue === false || rawValue === undefined || rawValue === null) continue;
    if (["href", "src", "poster"].includes(normalizedKey)) {
      const value = String(rawValue);
      if (/^\s*javascript:/i.test(value)) continue;
    }
    if (rawValue === true) result.push(normalizedKey);
    else result.push(`${normalizedKey}="${escapeHtml(rawValue)}"`);
  }
  return result.length ? ` ${result.join(" ")}` : "";
}

function applyWrap(html, attributes) {
  const wrap = String(attr(attributes, "wrap") || "inline").toLowerCase();
  const align = String(attr(attributes, "align") || "left").toLowerCase();
  if (wrap === "inline") return html;
  return `<div class="guide-embedded guide-embedded-${escapeHtml(wrap)} guide-embedded-${escapeHtml(align)}">${html}</div>`;
}

function renderTooltipParts(name, attributes, body, context, fallbackDetails = "") {
  if (booleanAttr(attributes, "showTooltip") === false || booleanAttr(attributes, "noTooltip") === true) return { attributes: "", template: "" };
  const explicit = attr(attributes, "tooltip", "description", "title");
  const source = String(body || explicit || "").trim();
  const content = source
    ? renderMarkdown(normalizeNestedMarkdown(source), { ...context, suppressFootnotes: true, depth: (context.depth || 0) + 1 })
    : `<div class="guide-runtime-tooltip"><strong>${escapeHtml(name)}</strong>${fallbackDetails ? `<code>${escapeHtml(fallbackDetails)}</code>` : ""}<p>${copy(context, "tooltipRuntime")}</p></div>`;
  return {
    attributes: ` data-guide-tooltip-rich="true" data-guide-tooltip-kind="${escapeHtml(name.toLowerCase())}" tabindex="0"`,
    template: `<template data-guide-tooltip-content>${content}</template>`,
  };
}

function renderItemPlaceholder(name, attributes, context, inline, body = "") {
  const id = String(attr(attributes, "id", "ore") || "minecraft:unknown");
  const showIcon = booleanAttr(attributes, "showIcon", "show_icon") !== false;
  const showId = booleanAttr(attributes, "showId", "show_id") !== false;
  const labelOption = String(attr(attributes, "label") || "").trim();
  const labelSide = ["left", "right"].includes(labelOption.toLowerCase()) ? labelOption.toLowerCase() : "";
  const label = String(body || (labelSide ? id : labelOption) || id).trim() || id;
  const scale = Math.max(.5, numberAttr(attributes, 1, "scale"));
  const hiddenClass = showId ? "" : " guide-hidden-runtime-id";
  const icon = showIcon && !["BlockId", "ItemId"].includes(name) ? `<span class="guide-item-placeholder-icon" style="font-size:${Math.round(22 * scale)}px">◈</span>` : "";
  const customLabel = Boolean(String(body || "").trim() || (labelOption && !labelSide));
  const idText = `<span class="guide-item-placeholder-id${hiddenClass}">${escapeHtml(id)}</span>`;
  const text = customLabel ? `<span class="guide-item-placeholder-label">${escapeHtml(label)}</span>${idText}` : idText;
  const tooltip = renderTooltipParts(name, attributes, body || attr(attributes, "tooltip"), context, id);
  const element = `<span class="guide-item-placeholder ${inline ? "inline" : "block"}${labelSide ? ` label-${labelSide}` : ""}"${tooltip.attributes}>${labelSide === "left" ? text + icon : icon + text}${tooltip.template}</span>`;
  return inline ? element : `<div class="guide-item-placeholder-block">${element}</div>`;
}

function renderRuntimePlaceholder(name, attributes, inline, context = {}, body = "") {
  const id = attr(attributes, "id", "controller", "name");
  const details = id ? `<span class="runtime-id">${escapeHtml(id)}</span>` : "";
  const icon = name === "BlockImage" ? "▣" : "◇";
  const tooltip = renderTooltipParts(name, attributes, body || attr(attributes, "tooltip"), context, id ? String(id) : "");
  const element = `<span class="guide-runtime-placeholder"${tooltip.attributes}><span class="runtime-icon">${icon}</span><strong>${escapeHtml(name)}</strong>${details}<em>${copy(context, "runtimeData")}</em>${tooltip.template}</span>`;
  return inline ? element : `<div class="guide-runtime-frame"><div class="guide-runtime-frame-title">${escapeHtml(name)} · ${copy(context, "runtimePreview")}</div>${element}</div>`;
}

function renderScenePlaceholder(name, attributes, context) {
  const width = cssLength(attr(attributes, "width"), "256px");
  const height = cssLength(attr(attributes, "height"), "192px");
  const perspective = attr(attributes, "perspective") || "isometric-north-east";
  const zoom = attr(attributes, "zoom");
  const details = [perspective, zoom !== undefined ? `${translatedString(context?.locale, "zoom")} ${zoom}` : "", booleanAttr(attributes, "interactive") === false ? translatedString(context?.locale, "static") : translatedString(context?.locale, "interactive")].filter(Boolean).join(" · ");
  return `<div class="guide-scene-placeholder" style="width:${escapeHtml(width)};height:${escapeHtml(height)}"><div class="scene-grid"></div><div class="scene-label">${escapeHtml(name)}<small>${copy(context, "sceneRuntime")}</small><small>${escapeHtml(details)}</small></div></div>`;
}

function renderContentTabs(attributes, body, context) {
  const tabs = [];
  const pattern = /<Tab\b([^>]*)>([\s\S]*?)<\/Tab>/gi;
  let match;
  while ((match = pattern.exec(body || ""))) {
    const tabAttributes = parseAttributes(match[1]);
    tabs.push({ title: String(attr(tabAttributes, "title") || `${translatedString(context?.locale, "tab")} ${tabs.length + 1}`), body: match[2] });
  }
  if (!tabs.length) return renderBlocks(body, context);
  const requestedIndex = attr(attributes, "defaultIndex");
  const fallbackIndex = tabs.findIndex((tab) => tab.title === attr(attributes, "default"));
  const defaultIndex = Math.max(0, Math.min(tabs.length - 1, requestedIndex !== undefined && Number.isFinite(Number(requestedIndex)) ? Number(requestedIndex) : (fallbackIndex >= 0 ? fallbackIndex : 0)));
  const accent = colorValue(attr(attributes, "color"), "var(--accent)");
  const icon = attr(attributes, "icon", "iconPng", "icon_png", "iconItem", "icon_item");
  const heading = attr(attributes, "title") ? `<strong>${icon ? `<span class="guide-tabs-icon">${escapeHtml(icon)}</span>` : ""}${escapeHtml(attr(attributes, "title"))}</strong>` : "";
  return `<section class="guide-tabs" data-guide-tabs style="--guide-tabs-accent:${accent}"><div class="guide-tabs-heading">${heading}<div class="guide-tabs-buttons">${tabs.map((tab, index) => `<button type="button" data-guide-tab="${index}" class="${index === defaultIndex ? "active" : ""}">${escapeHtml(tab.title)}</button>`).join("")}</div></div>${tabs.map((tab, index) => `<div class="guide-tab-panel" data-guide-panel="${index}"${index === defaultIndex ? "" : " hidden"}>${renderBlocks(normalizeNestedMarkdown(tab.body), { ...context, suppressFootnotes: true })}</div>`).join("")}</section>`;
}

function renderFloatingImage(attributes, body, context, inline) {
  const src = String(attr(attributes, "src") || "");
  const image = resolveAsset(src, context);
  if (!image) return `<span class="guide-image-placeholder">${escapeHtml(src || "FloatingImage")}</span>`;
  const x = numberAttr(attributes, 0, "x");
  const y = numberAttr(attributes, 0, "y");
  const cropWidthValue = attr(attributes, "width", "w");
  const cropHeightValue = attr(attributes, "height", "h");
  const cropWidth = numberAttr(attributes, 0, "width", "w");
  const cropHeight = numberAttr(attributes, 0, "height", "h");
  const cropValuesPresent = ["x", "y", "width", "w", "height", "h"].some((key) => attr(attributes, key) !== undefined);
  const cropValuesComplete = attr(attributes, "x") !== undefined && attr(attributes, "y") !== undefined && cropWidthValue !== undefined && cropHeightValue !== undefined;
  if (cropValuesPresent && !cropValuesComplete) return `<span class="guide-image-placeholder">FloatingImage crop requires x, y, width and height</span>`;
  const cropped = cropWidth > 0 && cropHeight > 0 && cropWidthValue !== undefined && cropHeightValue !== undefined;
  const scaleX = numberAttr(attributes, 1, "scaleX");
  const scaleY = numberAttr(attributes, 1, "scaleY");
  const displayWidthValue = attr(attributes, "displayWidth");
  const displayHeightValue = attr(attributes, "displayHeight");
  const displayWidth = displayWidthValue !== undefined ? numberAttr(attributes, 0, "displayWidth") : cropWidth * scaleX;
  const displayHeight = displayHeightValue !== undefined ? numberAttr(attributes, 0, "displayHeight") : cropHeight * scaleY;
  const wrap = String(attr(attributes, "wrap") || (inline ? "inline" : "inline")).toLowerCase();
  const align = String(attr(attributes, "align") || "left").toLowerCase();
  const className = `guide-floating-image ${wrap === "inline" ? "inline" : `wrap-${wrap}`} align-${align}`;
  const title = attr(attributes, "title") || "";
  if (!cropped) {
    const imageStyle = displayWidthValue !== undefined || displayHeightValue !== undefined
      ? `${displayWidthValue !== undefined ? `width:${Math.max(1, displayWidth)}px;` : ""}${displayHeightValue !== undefined ? `height:${Math.max(1, displayHeight)}px;` : ""}`
      : "";
    const annotations = renderImageAnnotations(body, context, 1, 1, displayWidth || 1, displayHeight || 1);
    return `<figure class="${className} guide-floating-image-full"${title ? ` aria-label="${escapeHtml(title)}"` : ""}><img src="${escapeHtml(image)}" alt="${escapeHtml(src)}"${imageStyle ? ` style="${imageStyle}"` : ""}><div class="guide-image-annotations">${annotations}</div></figure>`;
  }
  const hasOnlyDisplayWidth = displayWidthValue !== undefined && displayHeightValue === undefined;
  const hasOnlyDisplayHeight = displayHeightValue !== undefined && displayWidthValue === undefined;
  const aspect = cropHeight / cropWidth;
  const finalWidth = hasOnlyDisplayHeight ? displayHeight * (1 / aspect) : displayWidth;
  const finalHeight = hasOnlyDisplayWidth ? displayWidth * aspect : displayHeight;
  const style = `width:${Math.max(1, finalWidth)}px;height:${Math.max(1, finalHeight)}px;--crop-x:${x * scaleX}px;--crop-y:${y * scaleY}px;--crop-scale-x:${scaleX};--crop-scale-y:${scaleY}`;
  const annotations = renderImageAnnotations(body, context, cropWidth, cropHeight, finalWidth, finalHeight);
  return `<figure class="${className}" style="${style}"${title ? ` aria-label="${escapeHtml(title)}"` : ""}><img src="${escapeHtml(image)}" alt="${escapeHtml(src)}"><div class="guide-image-annotations">${annotations}</div></figure>`;
}

function renderImageAnnotations(body, context, cropWidth, cropHeight, displayWidth, displayHeight) {
  const annotations = [];
  const pattern = /<ImageAnnotation\b([^>]*)>([\s\S]*?)<\/ImageAnnotation>/gi;
  let match;
  while ((match = pattern.exec(body || ""))) {
    const attributes = parseAttributes(match[1]);
    const hasCoordinates = ["x", "y", "w", "width", "h", "height"].some((key) => attr(attributes, key) !== undefined);
    const x = numberAttr(attributes, 0, "x");
    const y = numberAttr(attributes, 0, "y");
    const width = numberAttr(attributes, hasCoordinates ? 1 : cropWidth, "w", "width");
    const height = numberAttr(attributes, hasCoordinates ? 1 : cropHeight, "h", "height");
    const border = booleanAttr(attributes, "border") === true;
    const thickness = Math.max(1, numberAttr(attributes, 1, "borderThickness"));
    const borderStyle = border ? `border:${thickness}px solid ${colorValue(attr(attributes, "borderColor"), "var(--preview-gold)")}` : "border:1px solid transparent";
    const left = hasCoordinates ? x / Math.max(1, cropWidth) * 100 : 0;
    const top = hasCoordinates ? y / Math.max(1, cropHeight) * 100 : 0;
    const widthPercent = hasCoordinates ? width / Math.max(1, cropWidth) * 100 : 100;
    const heightPercent = hasCoordinates ? height / Math.max(1, cropHeight) * 100 : 100;
    annotations.push(renderImageAnnotationElement(attributes, match[2], context, `left:${left}%;top:${top}%;width:${widthPercent}%;height:${heightPercent}%;${borderStyle}`));
  }
  const selfClosingPattern = /<ImageAnnotation\b([^>]*)\/\s*>/gi;
  while ((match = selfClosingPattern.exec(body || ""))) {
    const attributes = parseAttributes(match[1]);
    const hasCoordinates = ["x", "y", "w", "width", "h", "height"].some((key) => attr(attributes, key) !== undefined);
    const x = numberAttr(attributes, 0, "x");
    const y = numberAttr(attributes, 0, "y");
    const width = numberAttr(attributes, hasCoordinates ? 1 : cropWidth, "w", "width");
    const height = numberAttr(attributes, hasCoordinates ? 1 : cropHeight, "h", "height");
    const border = booleanAttr(attributes, "border") === true;
    const thickness = Math.max(1, numberAttr(attributes, 1, "borderThickness"));
    const borderStyle = border ? `border:${thickness}px solid ${colorValue(attr(attributes, "borderColor"), "var(--preview-gold)")}` : "border:1px solid transparent";
    const left = hasCoordinates ? x / Math.max(1, cropWidth) * 100 : 0;
    const top = hasCoordinates ? y / Math.max(1, cropHeight) * 100 : 0;
    const widthPercent = hasCoordinates ? width / Math.max(1, cropWidth) * 100 : 100;
    const heightPercent = hasCoordinates ? height / Math.max(1, cropHeight) * 100 : 100;
    annotations.push(renderImageAnnotationElement(attributes, "", context, `left:${left}%;top:${top}%;width:${widthPercent}%;height:${heightPercent}%;${borderStyle}`));
  }
  return annotations.join("");
}

function renderImageAnnotationElement(attributes, body, context, positionStyle) {
  const tooltip = renderTooltipParts("ImageAnnotation", attributes, body, context, "image region");
  const label = stripMarkdown(body || attr(attributes, "tooltip", "title") || "Image annotation");
  return `<span class="guide-image-annotation" style="${positionStyle}" aria-label="${escapeHtml(label)}"${tooltip.attributes}>${tooltip.template}</span>`;
}

function renderStandaloneImageAnnotation(attributes, body, context) {
  const tooltip = renderTooltipParts("ImageAnnotation", attributes, body, context, "image region");
  const label = stripMarkdown(body || attr(attributes, "tooltip", "title") || "Image annotation");
  return `<span class="guide-image-annotation guide-image-annotation-standalone" aria-label="${escapeHtml(label)}"${tooltip.attributes}>${escapeHtml(label)}${tooltip.template}</span>`;
}

function renderFileTree(body, attributes, context = {}) {
  const indent = numberAttr(attributes, 14, "indent");
  const gap = numberAttr(attributes, 0, "gap");
  const sourceLines = String(body || "").replaceAll("\r\n", "\n").split("\n").filter((line) => line.trim());
  const rows = sourceLines.map((line) => {
    const iconImage = line.match(/\{:\s*iconPng\s*=\s*["']?([^}\"']+)["']?\s*\}/i);
    const iconText = line.match(/\{:\s*icon\s*=\s*["']?([^}\"']+)["']?\s*\}/i);
    const iconItem = line.match(/\{:\s*iconItem\s*=\s*["']?([^}\"']+)["']?\s*\}/i);
    const withoutDirectives = line.replace(/\{:\s*(?:iconPng|iconItem|icon)\s*=\s*[^}]+\}/gi, "");
    const prefixMatch = withoutDirectives.match(/^[│| ├└─+\\\-]*/);
    const prefix = prefixMatch?.[0] || "";
    const label = withoutDirectives.slice(prefix.length).trim();
    const depth = Math.max(0, Math.floor(prefix.replace(/[│|]/g, " ").length / 4));
    const connector = [...prefix].map((character) => {
      if (character === "│" || character === "|") return `<span class="guide-filetree-vertical">│</span>`;
      if (["├", "+"].includes(character)) return `<span class="guide-filetree-branch">├</span>`;
      if (["└", "\\"].includes(character)) return `<span class="guide-filetree-branch">└</span>`;
      if (["─", "-"].includes(character)) return `<span class="guide-filetree-horizontal">─</span>`;
      return " ";
    }).join("");
    const iconSource = iconImage ? iconImage[1].trim() : "";
    const iconAsset = iconSource ? resolveAsset(iconSource, context) : "";
    const icon = iconItem ? renderItemPlaceholder("ItemIcon", { id: iconItem[1].trim(), showId: false }, context, true) : iconAsset ? `<img class="guide-filetree-image" src="${escapeHtml(iconAsset)}" alt="">` : iconImage ? "▧" : iconText ? escapeHtml(iconText[1].trim()) : (/[\/]$/.test(label) ? "▰" : "▱");
    const type = /[\/]$/.test(label) ? "directory" : "file";
    return `<div class="guide-filetree-row guide-filetree-${type}" data-tree-depth="${depth}"><span class="guide-filetree-prefix">${connector}</span><span class="guide-filetree-icon">${icon}</span><span class="guide-filetree-label">${renderInline(label, context)}</span></div>`;
  }).join("");
  return `<div class="guide-filetree" style="--tree-indent:${indent}px;--tree-gap:${gap}px">${rows}</div>`;
}

function renderCsvTable(source, attributes, context) {
  const rows = parseCsv(source);
  if (!rows.length) return `<div class="guide-csv-empty">CSV table is empty.</div>`;
  const header = booleanAttr(attributes, "header") !== false;
  const widths = splitComma(attr(attributes, "widths")).map(Number).filter(Number.isFinite);
  const columns = Math.max(...rows.map((row) => row.length), 1);
  const colgroup = widths.length ? `<colgroup>${Array.from({ length: columns }, (_item, index) => `<col style="width:${Number(widths[index] || 0) || "auto"}px">`).join("")}</colgroup>` : "";
  const renderRow = (row, rowIndex) => `<tr>${Array.from({ length: columns }, (_item, index) => {
    const tag = header && rowIndex === 0 ? "th" : "td";
    return `<${tag}>${renderInline(row[index] || "", context)}</${tag}>`;
  }).join("")}</tr>`;
  const head = header ? renderRow(rows[0], 0) : "";
  const body = rows.slice(header ? 1 : 0).map((row, index) => renderRow(row, header ? index + 1 : index)).join("");
  return `<div class="guide-table-wrap"><table>${colgroup}${header ? `<thead>${head}</thead>` : ""}<tbody>${body}</tbody></table></div>`;
}

function renderFootnotes(context) {
  return `<section class="guide-footnotes"><h4>Footnotes</h4><ol>${context.footnoteOrder.map((key, index) => `<li id="footnote-${escapeHtml(key)}"><span>${renderInline(context.footnotes.get(key) || "", { ...context, suppressFootnotes: true })}</span> <a href="#footnote-ref-${escapeHtml(key)}-${index + 1}" data-footnote-back="${index + 1}">↩</a></li>`).join("")}</ol></section>`;
}

function renderLatex(formula, attributes, inline, tooltip = "", defaultTooltip = false, context = {}) {
  const color = colorValue(attr(attributes, "color"), "var(--preview-latex)");
  const scale = numberAttr(attributes, 1, "scale", "sourceScale");
  const explicitTooltip = attr(attributes, "tooltip");
  const tooltipDisabled = booleanAttr(attributes, "showTooltip") === false || booleanAttr(attributes, "noTooltip") === true;
  const showFormulaTooltip = booleanAttr(attributes, "showTooltip") === true;
  const defaultFormulaTooltip = defaultTooltip && booleanAttr(attributes, "showTooltip") === undefined;
  const className = inline ? "guide-latex inline" : "guide-latex display";
  const valign = inline ? String(attr(attributes, "valign") || "baseline").toLowerCase() : "baseline";
  const offsetX = numberAttr(attributes, 0, "offsetX");
  const offsetY = numberAttr(attributes, 0, "offsetY");
  const positionStyle = inline && (offsetX || offsetY) ? `position:relative;left:${offsetX}px;top:${offsetY}px;` : "";
  const verticalStyle = inline ? `vertical-align:${["baseline", "top", "center", "bottom"].includes(valign) ? valign : "baseline"};` : "";
  let tooltipContent = "";
  if (!tooltipDisabled) {
    if (tooltip) tooltipContent = tooltip;
    else if (explicitTooltip !== undefined) {
      const raw = String(explicitTooltip).trim().replace(/^<>\s*|\s*<\/>$/g, "").replace(/^(["'])([\s\S]*)\1$/, "$2");
      tooltipContent = renderMarkdown(normalizeNestedMarkdown(raw), { ...context, suppressFootnotes: true, depth: (context.depth || 0) + 1 });
    } else if (showFormulaTooltip || defaultFormulaTooltip) {
      tooltipContent = `<code class="guide-latex-tooltip-source">${escapeHtml(formula)}</code>`;
    }
  }
  const tooltipAttributes = tooltipContent ? ` data-guide-tooltip-rich="true" data-guide-tooltip-kind="latex"` : "";
  const template = tooltipContent ? `<template data-guide-tooltip-content>${tooltipContent}</template>` : "";
  return `<span class="${className}" style="color:${color};--latex-scale:${scale};${verticalStyle}${positionStyle}"${tooltipAttributes}>${formatLatex(formula)}${template}</span>`;
}

function readLatexGroup(source, index) {
  if (source[index] !== "{") return null;
  let depth = 1;
  for (let cursor = index + 1; cursor < source.length; cursor += 1) {
    if (source[cursor] === "\\") { cursor += 1; continue; }
    if (source[cursor] === "{") depth += 1;
    else if (source[cursor] === "}" && --depth === 0) return { value: source.slice(index + 1, cursor), end: cursor + 1 };
  }
  return null;
}

function readLatexBracket(source, index) {
  if (source[index] !== "[") return null;
  const end = source.indexOf("]", index + 1);
  return end >= 0 ? { value: source.slice(index + 1, end), end: end + 1 } : null;
}

function formatLatex(formula) {
  const symbols = { alpha: "α", beta: "β", gamma: "γ", delta: "δ", epsilon: "ε", varepsilon: "ϵ", zeta: "ζ", eta: "η", theta: "θ", vartheta: "ϑ", iota: "ι", kappa: "κ", lambda: "λ", mu: "μ", nu: "ν", xi: "ξ", omicron: "ο", pi: "π", varpi: "ϖ", rho: "ρ", sigma: "σ", varsigma: "ς", tau: "τ", upsilon: "υ", phi: "φ", varphi: "ϕ", chi: "χ", psi: "ψ", omega: "ω", Gamma: "Γ", Delta: "Δ", Theta: "Θ", Lambda: "Λ", Xi: "Ξ", Pi: "Π", Sigma: "Σ", Upsilon: "Υ", Phi: "Φ", Psi: "Ψ", Omega: "Ω", infty: "∞", infinity: "∞", times: "×", cdot: "·", pm: "±", mp: "∓", le: "≤", leq: "≤", ge: "≥", geq: "≥", neq: "≠", approx: "≈", equiv: "≡", sim: "∼", simeq: "≃", to: "→", rightarrow: "→", leftarrow: "←", mapsto: "↦", partial: "∂", nabla: "∇", sum: "∑", int: "∫", intop: "∫", varint: "∫", oint: "∮", oiint: "∯", oiiint: "∰", iint: "∬", iints: "∬", iiint: "∭", iiiint: "⨌", idotsint: "∫⋯∫", prod: "∏", bigcup: "⋃", bigcap: "⋂", cup: "∪", cap: "∩", subset: "⊂", subseteq: "⊆", supset: "⊃", supseteq: "⊇", in: "∈", notin: "∉", forall: "∀", exists: "∃", emptyset: "∅", neg: "¬", land: "∧", lor: "∨", cdots: "⋯", ldots: "…", vdots: "⋮", ddots: "⋱", degree: "°", prime: "′", ell: "ℓ", hbar: "ℏ" };
  const delimiters = { lbrace: "{", rbrace: "}", lvert: "|", rvert: "|", langle: "⟨", rangle: "⟩", lceil: "⌈", rceil: "⌉", lfloor: "⌊", rfloor: "⌋", colon: ":", ';': ";", ',': ",", '!': "" };
  const operatorCommands = new Set(["sin", "cos", "tan", "cot", "sec", "csc", "arcsin", "arccos", "arctan", "sinh", "cosh", "tanh", "log", "ln", "lg", "exp", "lim", "min", "max", "det", "gcd"]);
  function renderEnvironment(name, content) {
    const rows = String(content || "").split(/\\\\(?:\s*\[.*?\])?/).map((row) => row.split("&").map((cell) => renderExpression(cell)));
    const tag = ["cases", "dcases"].includes(name) ? "cases" : "matrix";
    return `<span class="latex-${tag}">${rows.map((row) => `<span class="latex-row">${row.map((cell) => `<span class="latex-cell">${cell}</span>`).join("")}</span>`).join("")}</span>`;
  }
  function renderAtom(source, index) {
    if (source[index] === "{") { const group = readLatexGroup(source, index); if (group) return { html: renderExpression(group.value), end: group.end }; }
    if (source[index] === "\\") {
      const command = source.slice(index + 1).match(/^([A-Za-z]+|[^A-Za-z\s])/);
      if (command) return { html: renderExpression(source.slice(index, index + 1 + command[1].length)), end: index + 1 + command[1].length };
    }
    return { html: escapeHtml(source[index] || ""), end: index + 1 };
  }
  function renderExpression(source) {
    let html = "";
    for (let index = 0; index < source.length;) {
      const character = source[index];
      if (/\s/.test(character)) { html += " "; index += 1; continue; }
      if (character === "&") { index += 1; continue; }
      if (character === "^" || character === "_") {
        const atom = source[index + 1] === "{" ? renderAtom(source, index + 1) : renderAtom(source, index + 1);
        const tag = character === "^" ? "sup" : "sub";
        html += `<${tag}>${atom.html}</${tag}>`; index = atom.end; continue;
      }
      if (character !== "\\") { html += escapeHtml(character); index += 1; continue; }
      if (source[index + 1] === "\\") { html += "<br>"; index += 2; continue; }
      const commandMatch = source.slice(index + 1).match(/^([A-Za-z]+|[^A-Za-z\s])/);
      if (!commandMatch) { index += 1; continue; }
      const command = commandMatch[1]; index += 1 + command.length;
      if (command === "begin") {
        const env = readLatexGroup(source, index); if (env) { const endToken = `\\end{${env.value}}`; const end = source.indexOf(endToken, env.end); if (end >= 0) { html += renderEnvironment(env.value, source.slice(env.end, end)); index = end + endToken.length; continue; } }
      }
      if (["frac", "dfrac", "tfrac", "binom"].includes(command)) {
        const numerator = readLatexGroup(source, index); const denominator = numerator ? readLatexGroup(source, numerator.end) : null;
        if (numerator && denominator) { const fractionClass = command === "binom" ? "latex-binomial" : "latex-frac"; html += `<span class="${fractionClass}"><span>${renderExpression(numerator.value)}</span><span>${renderExpression(denominator.value)}</span></span>`; index = denominator.end; continue; }
      }
      if (command === "sqrt") {
        const order = source[index] === "[" ? readLatexBracket(source, index) : null; if (order) index = order.end;
        const group = readLatexGroup(source, index); if (group) { html += `<span class="latex-sqrt">${order ? `<sup>${renderExpression(order.value)}</sup>` : ""}√<span>${renderExpression(group.value)}</span></span>`; index = group.end; continue; }
      }
      if (["color", "textcolor"].includes(command)) {
        const colorGroup = readLatexGroup(source, index);
        const valueGroup = colorGroup ? readLatexGroup(source, colorGroup.end) : null;
        if (colorGroup && valueGroup) { html += `<span style="color:${colorValue(colorGroup.value, "currentColor")}">${renderExpression(valueGroup.value)}</span>`; index = valueGroup.end; continue; }
      }
      if (["text", "mathrm", "mathbf", "mathit", "mathbb", "mathcal", "mathsf", "mathtt", "operatorname", "textrm", "textbf", "textit"].includes(command)) { const group = readLatexGroup(source, index); if (group) { html += `<span class="latex-${command}">${command.startsWith("text") ? escapeHtml(group.value) : renderExpression(group.value)}</span>`; index = group.end; continue; } }
      if (["overline", "underline", "vec", "hat", "bar", "dot", "ddot", "tilde", "overbrace", "underbrace", "cancel"].includes(command)) { const group = readLatexGroup(source, index); if (group) { html += `<span class="latex-accent latex-${command}">${renderExpression(group.value)}</span>`; index = group.end; continue; } }
      if (operatorCommands.has(command)) { html += `<span class="latex-operator">${escapeHtml(command)}</span>`; continue; }
      if (["left", "right", "displaystyle", "scriptstyle", "scriptscriptstyle", "limits", "nolimits"].includes(command)) { if (source[index] && "([{⟨|)]}⟩".includes(source[index])) { html += escapeHtml(source[index++]); } continue; }
      if ([",", ";", ":", "!", ",,", "quad", "qquad", "enspace", "hspace"].includes(command)) { html += command === "!" ? "" : command === "quad" ? "\u00a0\u00a0" : command === "qquad" ? "\u00a0\u00a0\u00a0\u00a0" : "\u2009"; continue; }
      if (symbols[command] || delimiters[command] !== undefined) { html += escapeHtml(symbols[command] ?? delimiters[command]); continue; }
      html += `<span class="latex-command">\\${escapeHtml(command)}</span>`;
    }
    return html;
  }
  return renderExpression(String(formula ?? "").trim());
}

function resolveAsset(source, context) {
  if (!source) return null;
  return context.resolveAsset ? context.resolveAsset(String(source), context.pagePath) : null;
}

function resolveTextAsset(source, context) {
  if (!source) return "";
  return context.resolveText ? context.resolveText(String(source), context.pagePath) || "" : "";
}

function renderImage(alt, source, title, context) {
  const resolved = resolveAsset(source, context) || source;
  return `<img class="guide-inline-image" src="${escapeHtml(resolved)}" alt="${escapeHtml(alt || title || "")}">`;
}

function stripMarkdown(value) {
  return String(value ?? "").replace(/<[^>]+>/g, "").replace(/[\*_`~]/g, "");
}

function slugify(value) {
  return String(value).trim().toLowerCase().replace(/[^\p{L}\p{N}]+/gu, "-").replace(/^-|-$/g, "") || "section";
}
