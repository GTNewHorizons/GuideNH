import { siteText } from "./locale.js";

const HOLD_DURATION_MS = 500;

function domTarget(element) {
  const source = element instanceof Element ? element.closest("[data-guide-item-href]") : null;
  const href = source?.getAttribute("data-guide-item-href");
  return href && source.isConnected ? { key: source, source, href } : null;
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
    if (target?.key !== current?.key || target?.href !== current?.href) cancel();
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
      const href = holding.href;
      cancel();
      navigate(new URL(href, document.baseURI).href);
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
    holding = { key: current.key, href: current.href, started: performance.now() };
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
