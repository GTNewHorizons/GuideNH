import { attr, booleanAttr, escapeHtml } from "./html-utils.js";
import { translatedString } from "./language.js";

export const BINDING_TAGS = new Set(["BindingLink", "BindingCard", "BindingIcon", "ResearchLink", "ResearchCard", "ResearchIcon", "QuestLink", "QuestCard"]);

export function normalizeBindingId(type, rawId) {
  let id = String(rawId || "").trim();
  if (type === "betterquesting") {
    if (/^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i.test(id)) return id.toLowerCase();
    try {
      const bytes = atob(id.replaceAll("-", "+").replaceAll("_", "/"));
      if (bytes.length === 16) {
        const hex = Array.from(bytes, c => c.charCodeAt(0).toString(16).padStart(2, "0")).join("");
        id = [hex.slice(0, 8), hex.slice(8, 12), hex.slice(12, 16), hex.slice(16, 20), hex.slice(20)].join("-");
      }
    } catch (_) {}
  }
  return id;
}

export function bindingSpec(name, attributes) {
  const type = name.startsWith("Research") ? "thaumcraft" : name.startsWith("Quest") ? "betterquesting" : String(attr(attributes, "type") || "").trim().toLowerCase();
  const id = normalizeBindingId(type, attr(attributes, "id"));
  if (!type || !id || /[\x00-\x1f\x7f]/.test(id)) throw new Error("Binding type and id are required");
  const scale = Number(attr(attributes, "scale") ?? 1);
  if (!Number.isFinite(scale) || scale <= 0) throw new Error("Binding scale must be finite and positive");
  const target = name.startsWith("Quest") ? "native" : String(attr(attributes, "target") || (name.endsWith("Card") ? "native" : "auto"));
  if (!["auto", "native", "guide"].includes(target)) throw new Error("Binding target must be auto, native, or guide");
  return { type, id, scale, target, showTooltip: booleanAttr(attributes, "showTooltip") ?? true };
}

export function readBindings(frontmatter) {
  const result = [];
  const seen = new Set();
  const add = (type, id, anchor = "") => {
    if (typeof id !== "string" || !id.trim() || /[\x00-\x1f\x7f]/.test(id)) return;
    type = String(type).trim().toLowerCase();
    const binding = { type, id: normalizeBindingId(type, id), anchor: String(anchor || "").replace(/^#/, "") };
    const key = JSON.stringify([binding.type, binding.id, binding.anchor]);
    if (!seen.has(key)) { seen.add(key); result.push(binding); }
  };
  if (typeof frontmatter.research_id === "string") add("thaumcraft", frontmatter.research_id);
  for (const id of Array.isArray(frontmatter.research_ids) ? frontmatter.research_ids : []) add("thaumcraft", id);
  for (const entry of Array.isArray(frontmatter.bindings) ? frontmatter.bindings : []) if (entry && typeof entry === "object" && typeof entry.type === "string") add(entry.type, entry.id, entry.anchor);
  return result;
}

export function renderBinding(name, attributes, context = {}) {
  try {
    const spec = bindingSpec(name, attributes);
    const preview = context.resolveBindingPreview?.(spec) || context.bindingPreviews?.[JSON.stringify([spec.type, spec.id])];
    const restricted = ["hidden", "missing", "unavailable"].includes(preview?.state);
    const label = restricted ? preview.displayName || preview.state : String(attr(attributes, "text", "title") || preview?.displayName || spec.id);
    const tooltip = spec.showTooltip ? ` title="${escapeHtml((preview?.tooltipLines || []).join("\n"))}"` : "";
    const targets = restricted ? [] : context.resolveBindingTargets?.(spec) || [];
    const anchor = (target, text = target.title || label) => `<a data-guide-link="${escapeHtml(target.path + (target.anchor ? "#" + target.anchor : ""))}" href="${escapeHtml(target.path + (target.anchor ? "#" + target.anchor : ""))}"${tooltip}>${escapeHtml(text)}</a>`;
    let link = restricted ? `<span class="guide-binding-link">${escapeHtml(label)}</span>` : targets.length === 1 ? anchor(targets[0], label) : targets.length > 1
      ? `<span class="guide-binding-targets" tabindex="0"><span${tooltip}>${escapeHtml(label)}</span><span class="guide-binding-target-list">${targets.map(target => anchor(target)).join("")}</span></span>`
      : `<span class="guide-binding-link"${tooltip}>${escapeHtml(label)} <small>${escapeHtml(translatedString(context.locale || "en_us", "binding.nativeSite"))}</small></span>`;
    const src = preview?.iconPngBase64 ? `data:image/png;base64,${preview.iconPngBase64}` : preview?.iconSrc || "";
    const safe = /^(?:data:image\/(?:png|gif|jpeg|webp);base64,|blob:|https?:\/\/|\.\.?\/|\/)/i.test(src);
    const icon = !restricted && safe ? `<img class="guide-binding-icon" src="${escapeHtml(src)}" alt="${escapeHtml(label)}" width="${Math.max(1, Math.round(16 * spec.scale))}">` : "";
    if (name.endsWith("Link")) return link;
    if (name.endsWith("Icon")) return icon || link;
    const description = booleanAttr(attributes, "showDesc") !== false && !restricted && preview?.detail ? `<p>${escapeHtml(preview.detail)}</p>` : "";
    const parents = booleanAttr(attributes, "showParents") && !restricted ? (preview?.prerequisites || []).map(parent => renderBinding("BindingLink", parent, context)).join(" ") : "";
    return `<blockquote class="guide-binding-card"><p>${booleanAttr(attributes, "showIcon") !== false ? icon : ""} ${link}</p>${description}${parents}</blockquote>`;
  } catch (error) { return `<span class="guide-error">${escapeHtml(error.message)}</span>`; }
}
