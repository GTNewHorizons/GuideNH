import { siteText } from "./locale.js";

const HOLD_DURATION_MS = 500;
const targetCache = new WeakMap();
let chooser;

export function readGuideNavigationTarget(source, key = source) {
  if (!(source instanceof Element)) return null;
  const raw = source.getAttribute("data-guide-fluid-targets") || "";
  let cached = targetCache.get(source);
  if (!cached || cached.raw !== raw) {
    let choices = [];
    try {
      const value = raw ? JSON.parse(raw) : [];
      if (Array.isArray(value)) choices = value.filter(choice => typeof choice?.href === "string"
        && typeof choice?.title === "string").map(choice => ({ href: choice.href, title: choice.title }));
    } catch {}
    cached = { raw, choices };
    targetCache.set(source, cached);
  }
  const href = source.getAttribute("data-guide-fluid-href") || source.getAttribute("data-guide-item-href");
  return href || cached.choices.length ? { key, source, href, choices: cached.choices, revision: raw } : null;
}

export function guideNavigationTarget(element) {
  const source = element instanceof Element
    ? element.closest("[data-guide-item-href], [data-guide-fluid-href], [data-guide-fluid-targets]") : null;
  return readGuideNavigationTarget(source);
}

function destination(href) {
  if (typeof href !== "string" || !href.trim()) return null;
  try {
    const url = new URL(href, document.baseURI);
    return ["http:", "https:", "file:"].includes(url.protocol) ? url.href : null;
  } catch {
    return null;
  }
}

export function activateGuideTarget(target, navigate) {
  const choices = (target?.choices || []).filter(choice => destination(choice.href));
  if (choices.length <= 1) {
    const href = destination(choices[0]?.href || target?.href);
    if (href) navigate(href);
    return;
  }
  chooser?.close();
  const previousFocus = document.activeElement;
  const dialog = document.createElement("dialog");
  chooser = dialog;
  dialog.className = "guide-resource-chooser";
  dialog.setAttribute("aria-label", siteText("navigation", "chooseGuide"));
  const title = document.createElement("h2");
  title.textContent = siteText("navigation", "chooseGuide");
  const close = document.createElement("button");
  close.type = "button";
  close.className = "guide-resource-chooser-close";
  close.textContent = "×";
  close.setAttribute("aria-label", siteText("navigation", "closeChooser"));
  close.addEventListener("click", () => dialog.close());
  dialog.append(title, close);
  for (const choice of choices) {
    const link = document.createElement("a");
    link.href = destination(choice.href);
    link.textContent = choice.title;
    link.addEventListener("click", event => {
      if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
      event.preventDefault();
      dialog.close();
      navigate(link.href);
    });
    dialog.append(link);
  }
  dialog.addEventListener("click", event => { if (event.target === dialog) dialog.close(); });
  dialog.addEventListener("close", () => {
    dialog.remove();
    if (chooser === dialog) chooser = null;
    if (previousFocus instanceof HTMLElement && previousFocus.isConnected) previousFocus.focus();
  }, { once: true });
  document.body.append(dialog);
  dialog.showModal();
  dialog.querySelector("a")?.focus();
}

function domTarget(element) {
  const target = guideNavigationTarget(element);
  return target?.source.isConnected ? target : null;
}

function isEditing(target) {
  return target instanceof Element && (target.isContentEditable
    || !!target.closest("input, textarea, select, [role='textbox']"));
}

export function installGuideItemNavigation(tooltipRoot, resolveTooltipTarget, ensureTooltip, navigate) {
  let hovered = null;
  let focused = null;
  let current = null;
  let holding = null;
  let keyDown = false;
  let frame = 0;
  const footer = document.createElement("div");
  footer.className = "guide-item-navigation";
  const label = document.createElement("div");
  const progress = document.createElement("div");
  progress.className = "guide-item-navigation-progress";
  progress.setAttribute("role", "progressbar");
  progress.setAttribute("aria-valuemin", "0");
  progress.setAttribute("aria-valuemax", "100");
  const fill = document.createElement("div");
  progress.append(fill);
  footer.append(label, progress);

  function setProgress(value) {
    fill.style.transform = `scaleX(${value})`;
    progress.setAttribute("aria-valuenow", String(Math.round(value * 100)));
  }

  function cancel() {
    if (frame) cancelAnimationFrame(frame);
    frame = 0;
    holding = null;
    setProgress(0);
  }

  function refresh() {
    const tooltipTarget = resolveTooltipTarget();
    const target = tooltipRoot.hidden
      ? domTarget(hovered) || domTarget(focused)
      : tooltipTarget;
    if (target?.key !== current?.key || target?.href !== current?.href || target?.revision !== current?.revision) cancel();
    current = target;
    if (!current || tooltipRoot.hidden) {
      footer.remove();
      return;
    }
    const text = siteText("navigation", "holdToOpenGuide");
    label.textContent = text;
    progress.setAttribute("aria-label", text);
    if (footer.parentNode !== tooltipRoot) tooltipRoot.append(footer);
  }

  function tick(time) {
    frame = 0;
    refresh();
    if (!holding || document.hidden) {
      cancel();
      return;
    }
    const value = Math.min(1, (time - holding.started) / HOLD_DURATION_MS);
    setProgress(value);
    if (value === 1) {
      const target = holding.target;
      cancel();
      activateGuideTarget(target, navigate);
    } else {
      frame = requestAnimationFrame(tick);
    }
  }

  document.addEventListener("mouseover", event => {
    hovered = domTarget(event.target)?.source || null;
    refresh();
  });
  document.addEventListener("mouseout", event => {
    hovered = domTarget(event.relatedTarget)?.source || null;
    refresh();
  });
  document.addEventListener("focusin", event => {
    focused = domTarget(event.target)?.source || null;
    refresh();
  });
  document.addEventListener("focusout", event => {
    focused = domTarget(event.relatedTarget)?.source || null;
    refresh();
  });
  window.addEventListener("keydown", event => {
    if (event.key?.toLowerCase() !== "g" || event.repeat || keyDown || event.defaultPrevented
      || event.ctrlKey || event.altKey || event.metaKey || event.isComposing || isEditing(event.target)) return;
    refresh();
    if (!current) return;
    keyDown = true;
    event.preventDefault();
    ensureTooltip(current.source);
    refresh();
    if (!current) return;
    holding = { key: current.key, target: current, started: performance.now() };
    frame = requestAnimationFrame(tick);
  });
  window.addEventListener("keyup", event => {
    if (event.key?.toLowerCase() !== "g") return;
    keyDown = false;
    cancel();
  });
  window.addEventListener("keydown", event => {
    if (event.key === "Escape" || event.ctrlKey || event.altKey || event.metaKey) cancel();
  });
  window.addEventListener("blur", reset);
  document.addEventListener("visibilitychange", () => { if (document.hidden) reset(); });
  document.addEventListener("scroll", cancel, { capture: true, passive: true });
  document.addEventListener("click", event => {
    const source = event.target instanceof Element ? event.target.closest(".guide-fluid-link[data-guide-fluid-targets]") : null;
    if (!source || event.defaultPrevented || event.button !== 0) return;
    event.preventDefault();
    activateGuideTarget(readGuideNavigationTarget(source), navigate);
  });

  function reset() {
    cancel();
    keyDown = false;
    hovered = null;
    focused = null;
    current = null;
    footer.remove();
  }

  return { refresh, reset };
}
