import { attr, booleanAttr, escapeHtml } from "./html-utils.js";

export function fluidDescription(attributes) {
  const id = String(attr(attributes, "id") ?? "").trim();
  if (!id) throw new Error("Fluid id is required");
  if (attr(attributes, "ore") !== undefined) throw new Error("Fluid tags do not accept ore selectors");
  const raw = attr(attributes, "amount");
  let amount;
  if (raw !== undefined) {
    if (!/^\d+$/.test(String(raw).trim())) throw new Error("Fluid amount must be an integer");
    amount = Number(raw);
    if (!Number.isSafeInteger(amount) || amount > 2147483647) throw new Error("Fluid amount must be an integer");
  }
  return { id, amount, nbt: attr(attributes, "nbt") };
}

export function fluidScale(attributes) {
  const rawScale = attr(attributes, "scale");
  const scale = rawScale === undefined ? 1 : Number(rawScale);
  if (typeof rawScale === "boolean") throw new Error("Fluid scale must be finite and positive");
  if (!Number.isFinite(scale) || scale <= 0) throw new Error("Fluid scale must be finite and positive");
  return Math.max(.125, scale);
}

export function renderFluidIcon(attributes, context = {}, extraClass = "") {
  const spec = fluidDescription(attributes);
  const preview = context.resolveFluidPreview?.(spec) || context.fluidPreviews?.[spec.id];
  const name = String(preview?.displayName || spec.id);
  const scale = fluidScale(attributes);
  const size = Math.max(2, Math.round(22 * Math.max(.125, scale)));
  const src = preview?.iconSrc || preview?.iconUrl || (preview?.iconPngBase64 ? `data:image/png;base64,${preview.iconPngBase64}` : "");
  const safeImage = /^(?:data:image\/(?:png|gif|jpeg|webp);base64,|blob:|https?:\/\/|\.\.?\/|\/)/i.test(src);
  const icon = safeImage
    ? `<img class="guide-fluid-icon ${escapeHtml(extraClass)}" src="${escapeHtml(src)}" alt="${escapeHtml(name)}" width="${size}" height="${size}" decoding="async">`
    : `<span class="guide-fluid-icon guide-fluid-icon-fallback ${escapeHtml(extraClass)}" style="width:${size}px;height:${size}px" aria-label="${escapeHtml(name)}">◒</span>`;
  const quantity = spec.amount === undefined ? "" : `<span class="guide-fluid-quantity">${escapeHtml(spec.amount)}</span>`;
  return `<span class="guide-fluid-icon-stack" data-fluid-id="${escapeHtml(spec.id)}">${icon}${quantity}</span>`;
}

export function fluidTooltipVisible(attributes) {
  const explicit = booleanAttr(attributes, "showTooltip");
  return explicit === undefined ? !booleanAttr(attributes, "noTooltip") : explicit;
}

export function fluidIconSourceConflict(attributes) {
  return attr(attributes, "iconFluid", "icon_fluid") !== undefined
    && attr(attributes, "iconItem", "icon_item", "iconPng", "icon_png", "icon") !== undefined;
}

export function renderFluidLabel(name, format) {
  if (format === undefined) return `<em>${escapeHtml(name)}</em>`;
  const markers = [["~~", "s"], ["**", "strong"], ["__", "u"], ["++", "u"],
    ["^^", 'span style="text-decoration:underline wavy"'], ["::", 'span style="text-decoration:underline dotted"'],
    ["*", "em"], ["_", "em"]];
  let template = String(format), open = "", close = "";
  while (true) {
    const marker = markers.find(([token]) => template.length > token.length * 2 && template.startsWith(token) && template.endsWith(token));
    if (!marker) break;
    const [token, tag] = marker;
    open += `<${tag}>`;
    close = `</${tag.split(" ")[0]}>` + close;
    template = template.slice(token.length, -token.length);
  }
  return open + escapeHtml(template.replaceAll("%s", () => name)) + close;
}
