import { installSearchUi } from "./search.js";
import { disposeHydratedScenes, hydrateVisibleScenes } from "./viewer.js";
import { loadSharedText } from "./sharedAssets.js";
import { rememberSiteLanguage } from "./languagePreference.js";
import { installGuideItemNavigation } from "./itemNavigation.js";
import { ensureSiteLanguage, siteText } from "./locale.js";

async function loadCustomSiteLink() {
  const link = document.querySelector("[data-guide-custom-link]");
  if (!(link instanceof HTMLAnchorElement)) return;
  try {
    const response = await fetch(new URL("../site-config.json", import.meta.url), { credentials: "same-origin" });
    if (!response.ok) return;
    const config = await response.json();
    if (typeof config?.headerLink !== "string" || !config.headerLink.trim()) return;
    const url = new URL(config.headerLink.trim());
    if (url.protocol !== "https:" && url.protocol !== "http:") return;
    link.href = url.href;
    const label = typeof config.headerLinkLabel === "string" && config.headerLinkLabel.trim()
      ? config.headerLinkLabel.trim() : url.hostname;
    link.setAttribute("aria-label", label);
    link.title = label;
    link.hidden = false;
  } catch (error) {
    console.warn("GuideNH custom site link could not be loaded.", error);
  }
}

async function loadSidebar(sidebar) {
  const source = sidebar.querySelector("[data-guide-sidebar-src]")?.dataset.guideSidebarSrc;
  if (!source) return;
  const html = await loadSharedText(source);
  const template = document.createElement("template");
  template.innerHTML = html;
  sidebar.replaceChildren(template.content.cloneNode(true));
  sidebar.dataset.guideSidebarSource = new URL(source, document.baseURI).href;
}

async function loadPageTemplates(content) {
  await Promise.all(Array.from(content.querySelectorAll("[data-guide-templates-src]"), async (placeholder) => {
    const html = await loadSharedText(placeholder.dataset.guideTemplatesSrc);
    const template = document.createElement("template");
    template.innerHTML = html;
    placeholder.replaceWith(template.content.cloneNode(true));
  }));
}

async function loadLanguageMenu(header) {
  const placeholder = header?.querySelector("[data-guide-language-menu-src]");
  if (!placeholder) return;
  const html = await loadSharedText(placeholder.dataset.guideLanguageMenuSrc);
  const template = document.createElement("template");
  template.innerHTML = html;
  placeholder.replaceWith(template.content.cloneNode(true));
}

function installMediaWikiSpecialFilters(root) {
  const pages = root.querySelectorAll("[data-guide-special-page]");
  for (const page of pages) {
    const input = page.querySelector("[data-guide-special-filter]");
    const showMoreButton = page.querySelector("[data-guide-special-show-more]");
    if (!(input instanceof HTMLInputElement)) {
      continue;
    }
    const entries = Array.from(page.querySelectorAll("[data-guide-special-entry]"));
    const groups = Array.from(page.querySelectorAll("[data-guide-special-group]"));
    const mode = page.getAttribute("data-guide-special-mode") || "flat";
    const defaultVisibleRaw = Number(page.getAttribute("data-guide-special-default-visible") || "0");
    const defaultVisible = Number.isFinite(defaultVisibleRaw) && defaultVisibleRaw > 0 ? defaultVisibleRaw : Number.MAX_SAFE_INTEGER;
    let visibleCount = defaultVisible;
    const apply = () => {
      const query = input.value.trim().toLowerCase();
      if (!query) {
        if (mode === "grouped") {
          for (const entry of entries) {
            entry.hidden = false;
          }
          for (let index = 0; index < groups.length; index++) {
            groups[index].hidden = index >= visibleCount;
          }
        } else {
          for (let index = 0; index < entries.length; index++) {
            entries[index].hidden = index >= visibleCount;
          }
        }
      } else {
        for (const entry of entries) {
          const searchBlob = (entry.getAttribute("data-guide-special-search") || entry.textContent || "").toLowerCase();
          entry.hidden = !searchBlob.includes(query);
        }
      }
      for (const group of groups) {
        const visibleChildren = group.querySelector("[data-guide-special-entry]:not([hidden])");
        group.hidden = !visibleChildren;
      }
      if (showMoreButton instanceof HTMLElement) {
        const hasMore = !query && visibleCount < (mode === "grouped" ? groups.length : entries.length);
        showMoreButton.hidden = !hasMore;
      }
    };
    input.addEventListener("input", apply);
    input.addEventListener("keydown", (event) => {
      if (event.key === "Escape") {
        input.value = "";
        visibleCount = defaultVisible;
        apply();
      }
    });
    if (showMoreButton instanceof HTMLElement) {
      showMoreButton.addEventListener("click", () => {
        const total = mode === "grouped" ? groups.length : entries.length;
        visibleCount = Math.min(total, visibleCount + 60);
        apply();
      });
    }
    apply();
  }
}

function cycleChildren(container) {
  const current = container.querySelector(".current");
  if (current) {
    current.classList.remove("current");
  }
  const next = current && current.nextElementSibling ? current.nextElementSibling : container.firstElementChild;
  if (next) {
    next.classList.add("current");
  }
}

function stopIngredientCycling(root) {
  if (root?.__guideIngredientCyclingTimer) {
    window.clearInterval(root.__guideIngredientCyclingTimer);
    delete root.__guideIngredientCyclingTimer;
  }
}

function stopGuideSounds(root) {
  const sounds = window.GuideNHSounds;
  if (root && sounds && typeof sounds.stopWithin === "function") {
    sounds.stopWithin(root);
    return;
  }
  if (!root && sounds && typeof sounds.stopAll === "function") {
    sounds.stopAll();
  }
}

function installIngredientCycling(root) {
  stopIngredientCycling(root);
  const cyclingBoxes = root.querySelectorAll("[data-ingredient-cycling]");
  if (!cyclingBoxes.length) {
    return;
  }

  cyclingBoxes.forEach((box) => {
    const first = box.firstElementChild;
    if (first && !box.querySelector(".current")) {
      first.classList.add("current");
    }
  });

  root.__guideIngredientCyclingTimer = window.setInterval(() => {
    cyclingBoxes.forEach((box) => {
      cycleChildren(box);
    });
  }, 1000);
}

function layoutImageAnnotations(root) {
  const annotations = root.querySelectorAll(".guide-image-annotation[data-source-x]");
  for (const annotation of annotations) {
    const wrapper = annotation.closest(".guide-floating-image-wrap");
    const image = wrapper?.querySelector("img.guide-floating-image");
    if (!(image instanceof HTMLImageElement) || !image.naturalWidth || !image.naturalHeight) {
      continue;
    }
    const x = Number(annotation.dataset.sourceX || 0);
    const y = Number(annotation.dataset.sourceY || 0);
    const width = Number(annotation.dataset.sourceWidth || 1);
    const height = Number(annotation.dataset.sourceHeight || 1);
    const cropWidth = Number(image.dataset.cropWidth || image.naturalWidth || 1);
    const cropHeight = Number(image.dataset.cropHeight || image.naturalHeight || 1);
    annotation.style.left = `${(x / cropWidth) * 100}%`;
    annotation.style.top = `${(y / cropHeight) * 100}%`;
    annotation.style.width = `${(width / cropWidth) * 100}%`;
    annotation.style.height = `${(height / cropHeight) * 100}%`;
  }
}

function layoutCroppedFloatingImage(image) {
  const stage = image.closest(".guide-floating-image-crop");
  if (!(stage instanceof HTMLElement) || !image.naturalWidth || !stage.clientWidth) return;
  const cropX = Number(image.dataset.cropX || 0);
  const cropY = Number(image.dataset.cropY || 0);
  const cropWidth = Number(image.dataset.cropWidth || image.naturalWidth || 1);
  const cropHeight = Number(image.dataset.cropHeight || image.naturalHeight || 1);
  const stageStyle = window.getComputedStyle(stage);
  const scaleX = Number.parseFloat(stageStyle.width) / cropWidth;
  const scaleY = Number.parseFloat(stageStyle.height) / cropHeight;
  image.style.position = "absolute";
  image.style.left = `${-cropX * scaleX}px`;
  image.style.top = `${-cropY * scaleY}px`;
  image.style.width = `${image.naturalWidth * scaleX}px`;
  image.style.height = `${image.naturalHeight * scaleY}px`;
}

function stopImageLayout(root) {
  root?.__guideImageResizeObserver?.disconnect();
  if (root) delete root.__guideImageResizeObserver;
}

function installImageAnnotations(root) {
  stopImageLayout(root);
  const images = root.querySelectorAll(".guide-floating-image-wrap img.guide-floating-image");
  if (!images.length) return;
  const observer = new ResizeObserver(entries => {
    for (const { target } of entries) {
      const image = target.querySelector("img.guide-floating-image[data-crop-width]");
      if (image instanceof HTMLImageElement) layoutCroppedFloatingImage(image);
    }
  });
  root.__guideImageResizeObserver = observer;
  for (const image of images) {
    if (!(image instanceof HTMLImageElement)) {
      continue;
    }
    const layout = () => {
      if (image.dataset.cropWidth) {
        layoutCroppedFloatingImage(image);
      } else if (image.dataset.displayHeight && image.naturalWidth && image.naturalHeight) {
        const wrapper = image.closest(".guide-floating-image-wrap");
        wrapper.style.width = `${image.naturalWidth * Number(image.dataset.displayHeight) / image.naturalHeight}px`;
      }
      layoutImageAnnotations(root);
    };
    if (image.dataset.cropWidth) {
      observer.observe(image.closest(".guide-floating-image-crop"));
    }
    if (image.complete) {
      layout();
    } else {
      image.addEventListener("load", layout, { once: true });
    }
  }
}

function installGuideSounds(root) {
  const lastPlayedAt = new Map();
  const activeAudio = new Map();

  function stopAudio(audio) {
    try {
      audio.pause();
      audio.removeAttribute("src");
      audio.load();
    } catch (_) {}
  }

  function stopAll() {
    for (const audio of activeAudio.keys()) {
      stopAudio(audio);
    }
    activeAudio.clear();
  }

  function stopWithin(container) {
    for (const [audio, owner] of Array.from(activeAudio.entries())) {
      if (!(owner instanceof Node) || !owner.isConnected || owner === container || container.contains(owner)) {
        stopAudio(audio);
        activeAudio.delete(audio);
      }
    }
  }

  window.GuideNHSounds = {
    stopAll,
    stopWithin,
  };

  function keyFor(element, sound) {
    return `${sound || ""}:${element.dataset.guideSoundSrc || ""}`;
  }

  function effectiveVolume(element, event, spatialElement = element) {
    const baseVolume = Number(element.dataset.guideSoundVolume || 1);
    const radius = Number(element.dataset.guideSoundRadius || -1);
    if (!Number.isFinite(baseVolume) || baseVolume <= 0 || !event || !Number.isFinite(radius) || radius <= 0) {
      return Math.max(0, baseVolume || 0);
    }
    const rect = spatialElement.getBoundingClientRect();
    const position = {
      x: rect.left + rect.width / 2,
      y: rect.top + rect.height / 2,
    };
    const eventX = Number.isFinite(event.clientX) ? event.clientX : rect.left + rect.width / 2;
    const eventY = Number.isFinite(event.clientY) ? event.clientY : rect.top + rect.height / 2;
    const dx = eventX - position.x;
    const dy = eventY - position.y;
    const minVolume = Math.max(0, Math.min(1, Number(element.dataset.guideSoundMinVolume || 0.15)));
    const factor = Math.max(minVolume, Math.min(1, 1 - Math.sqrt(dx * dx + dy * dy) / radius));
    return Math.max(0, baseVolume * factor);
  }

  function playElementSound(element, event, spatialElement = element) {
    const src = element.dataset.guideSoundSrc;
    if (!src) {
      return false;
    }
    const cooldown = Math.max(0, Number(element.dataset.guideSoundCooldown || 250));
    const key = keyFor(element, element.dataset.guideSound);
    const now = Date.now();
    const last = lastPlayedAt.get(key) || 0;
    if (cooldown > 0 && now - last < cooldown) {
      return true;
    }
    const audio = new Audio(src);
    activeAudio.set(audio, spatialElement);
    audio.addEventListener("ended", () => activeAudio.delete(audio), { once: true });
    audio.addEventListener("error", () => activeAudio.delete(audio), { once: true });
    audio.volume = Math.max(0, Math.min(1, effectiveVolume(element, event, spatialElement)));
    audio.playbackRate = Math.max(0.01, Number(element.dataset.guideSoundPitch || 1) || 1);
    const played = audio.play();
    if (played?.catch) {
      played.catch(() => {
        stopAudio(audio);
        activeAudio.delete(audio);
      });
    }
    lastPlayedAt.set(key, now);
    return true;
  }

  function soundTrigger(element) {
    return (element.dataset.guideSoundTrigger || "click").toLowerCase();
  }

  root.addEventListener("click", (event) => {
    const element = event.target instanceof Element ? event.target.closest("[data-guide-sound]") : null;
    if (element instanceof HTMLElement && soundTrigger(element) === "click" && playElementSound(element, event)) {
      event.preventDefault();
    }
  });

  root.addEventListener("keydown", (event) => {
    if (event.key !== "Enter" && event.key !== " ") {
      return;
    }
    const element = event.target instanceof Element ? event.target.closest("[data-guide-sound]") : null;
    if (element instanceof HTMLElement && soundTrigger(element) === "click" && playElementSound(element, event)) {
      event.preventDefault();
    }
  });

  root.addEventListener("mouseover", (event) => {
    const element = event.target instanceof Element ? event.target.closest("[data-guide-sound]") : null;
    if (element instanceof HTMLElement && soundTrigger(element) === "hover" && !element.dataset.guideSoundHovered) {
      element.dataset.guideSoundHovered = "true";
      playElementSound(element, event);
    }
  });

  root.addEventListener("mouseout", (event) => {
    const element = event.target instanceof Element ? event.target.closest("[data-guide-sound]") : null;
    const related = event.relatedTarget instanceof Node ? event.relatedTarget : null;
    if (element instanceof HTMLElement && (!related || !element.contains(related))) {
      delete element.dataset.guideSoundHovered;
    }
  });

  installSceneSounds(root, playElementSound);
  if (!window.__guideSoundLifecycleInstalled) {
    window.__guideSoundLifecycleInstalled = true;
    window.addEventListener("pagehide", () => window.GuideNHSounds?.stopAll?.());
    document.addEventListener("visibilitychange", () => {
      if (document.hidden) {
        window.GuideNHSounds?.stopAll?.();
      }
    });
  }
  root.addEventListener("click", (event) => {
    const link = event.target instanceof Element ? event.target.closest("a[href]") : null;
    if (link instanceof HTMLAnchorElement && !link.href.startsWith("javascript:")) {
      stopAll();
    }
  }, true);
}

function installSceneSounds(root, playElementSound) {
  const playedEnterSounds = new WeakMap();
  const buildElement = (sound) => {
    const element = document.createElement("span");
    element.dataset.guideSound = sound.sound || "";
    element.dataset.guideSoundSrc = sound.src || "";
    element.dataset.guideSoundTrigger = sound.trigger || "click";
    element.dataset.guideSoundVolume = String(sound.volume ?? 1);
    element.dataset.guideSoundPitch = String(sound.pitch ?? 1);
    element.dataset.guideSoundCooldown = String(sound.cooldown ?? 250);
    element.dataset.guideSoundRadius = String(sound.radius ?? -1);
    element.dataset.guideSoundMinVolume = String(sound.minVolume ?? 0.15);
    if (sound.x != null) {
      element.dataset.guideSoundX = String(sound.x);
    }
    if (sound.y != null) {
      element.dataset.guideSoundY = String(sound.y);
    }
    if (sound.z != null) {
      element.dataset.guideSoundZ = String(sound.z);
    }
    return element;
  };
  const parseSounds = (element) => {
    try {
      const parsed = JSON.parse(element.dataset.guideSceneSounds || "[]");
      return Array.isArray(parsed) ? parsed : [];
    } catch (_) {
      return [];
    }
  };
  const playMatching = (element, trigger, event) => {
    const elementSounds = parseSounds(element);
    for (const sound of elementSounds) {
      if ((sound.trigger || "click") !== trigger) {
        continue;
      }
      playElementSound(buildElement(sound), event, element);
    }
  };
  const playEnter = (element, event) => {
    const elementSounds = parseSounds(element);
    let played = playedEnterSounds.get(element);
    if (!played) {
      played = new Set();
      playedEnterSounds.set(element, played);
    }
    for (let i = 0; i < elementSounds.length; i++) {
      const sound = elementSounds[i];
      if ((sound.trigger || "click") === "enter" && !played.has(i)) {
        played.add(i);
        playElementSound(buildElement(sound), event, element);
      }
    }
  };
  root.addEventListener("click", (event) => {
    const element = event.target instanceof Element ? event.target.closest("[data-guide-scene-sounds]") : null;
    if (element instanceof HTMLElement) {
      playMatching(element, "click", event);
    }
  });
  root.addEventListener("mouseover", (event) => {
    const element = event.target instanceof Element ? event.target.closest("[data-guide-scene-sounds]") : null;
    if (!(element instanceof HTMLElement) || element.dataset.guideSceneSoundHovered) {
      return;
    }
    element.dataset.guideSceneSoundHovered = "true";
    playMatching(element, "hover", event);
    playEnter(element, event);
  });
  root.addEventListener("mouseout", (event) => {
    const element = event.target instanceof Element ? event.target.closest("[data-guide-scene-sounds]") : null;
    const related = event.relatedTarget instanceof Node ? event.relatedTarget : null;
    if (element instanceof HTMLElement && (!related || !element.contains(related))) {
      delete element.dataset.guideSceneSoundHovered;
    }
  });
}

function installTooltips(root) {
  const tooltipRoot = document.querySelector("[data-guide-tooltip-root]");
  if (!tooltipRoot) {
    return;
  }

  let activeState = null;
  let lastPointer = null;
  let restoreStack = [];
  let eventShiftKey = false;
  let itemNavigation;

  function resolveTemplateHtml(templateId) {
    if (!templateId) {
      return "";
    }
    const template = document.getElementById(templateId);
    return template ? template.innerHTML : "";
  }

  function closestGuideTooltip(target) {
    return target instanceof Element ? target.closest("[data-template]") : null;
  }

  function isInsideTooltipRoot(target) {
    return target instanceof Node && tooltipRoot.contains(target);
  }

  function position(pointer) {
    const point = pointer || lastPointer;
    if (!point || tooltipRoot.hidden) {
      return;
    }
    if (isInsideTooltipRoot(point.target)) return;
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    const rect = tooltipRoot.getBoundingClientRect();
    const margin = 14;
    let left = point.clientX + 16;
    let top = point.clientY + 18;
    if (left + rect.width > viewportWidth - margin) {
      left = viewportWidth - rect.width - margin;
    }
    if (top + rect.height > viewportHeight - margin) {
      top = point.clientY - rect.height - 18;
    }
    if (left < margin) {
      left = margin;
    }
    if (top < margin) {
      top = margin;
    }
    tooltipRoot.style.left = `${left}px`;
    tooltipRoot.style.top = `${top}px`;
  }

  function pointerInPopup(pointer) {
    if (!pointer || tooltipRoot.hidden) return false;
    const rect = tooltipRoot.getBoundingClientRect();
    return pointer.clientX >= rect.left && pointer.clientX <= rect.right
      && pointer.clientY >= rect.top && pointer.clientY <= rect.bottom;
  }

  function hideAll() {
    activeState = null;
    restoreStack = [];
    stopGuideSounds(tooltipRoot);
    disposeHydratedScenes(tooltipRoot);
    stopImageLayout(tooltipRoot);
    tooltipRoot.hidden = true;
    tooltipRoot.innerHTML = "";
    stopIngredientCycling(tooltipRoot);
    delete tooltipRoot.dataset.externalTooltipOwner;
    delete tooltipRoot.dataset.externalTooltipTemplate;
    delete tooltipRoot.dataset.externalTooltipShiftTemplate;
    itemNavigation?.reset();
  }

  function applyState(nextState, pointer, resetStack) {
    if (!nextState || !nextState.html) {
      hideAll();
      return;
    }
    if (resetStack) {
      restoreStack = [];
    }
    activeState = nextState;
    stopGuideSounds(tooltipRoot);
    disposeHydratedScenes(tooltipRoot);
    stopImageLayout(tooltipRoot);
    stopIngredientCycling(tooltipRoot);
    tooltipRoot.innerHTML = nextState.html;
    tooltipRoot.hidden = false;
    installIngredientCycling(tooltipRoot);
    installImageAnnotations(tooltipRoot);
    hydrateVisibleScenes(tooltipRoot);
    if (nextState.sourceType === "external") {
      tooltipRoot.dataset.externalTooltipOwner = String(nextState.sourceRef ?? "");
      tooltipRoot.dataset.externalTooltipTemplate = nextState.templateId ?? "";
      tooltipRoot.dataset.externalTooltipShiftTemplate = nextState.shiftTemplateId ?? "";
    } else {
      delete tooltipRoot.dataset.externalTooltipOwner;
      delete tooltipRoot.dataset.externalTooltipTemplate;
    }
    itemNavigation?.refresh();
    position(pointer);
    window.requestAnimationFrame(() => position(pointer));
  }

  function captureState() {
    if (!activeState) {
      return null;
    }
    return {
      sourceType: activeState.sourceType,
      sourceRef: activeState.sourceRef,
      templateId: activeState.templateId,
      shiftTemplateId: activeState.shiftTemplateId,
      baseTemplateId: activeState.baseTemplateId,
      navigationHref: activeState.navigationHref,
      html: activeState.html,
    };
  }

  function restorePrevious(pointer) {
    const previous = restoreStack.pop();
    if (!previous) {
      hideAll();
      return;
    }
    applyState(previous, pointer, false);
  }

  function showTemplate(templateId, sourceType, sourceRef, pointer, preserveCurrent, shiftTemplateId = null,
    baseTemplateId = null) {
    if (activeState?.sourceType === sourceType && activeState.sourceRef === sourceRef
      && activeState.templateId === templateId && activeState.shiftTemplateId === shiftTemplateId) {
      position(pointer);
      return;
    }
    const html = resolveTemplateHtml(templateId);
    if (!html) {
      if (preserveCurrent && restoreStack.length) {
        restorePrevious(pointer);
      } else {
        hideAll();
      }
      return;
    }

    if (preserveCurrent) {
      const snapshot = captureState();
      if (snapshot) {
        restoreStack.push(snapshot);
      }
    }

    applyState(
      {
        sourceType,
        sourceRef,
        templateId,
        shiftTemplateId,
        baseTemplateId: baseTemplateId || templateId,
        navigationHref: preserveCurrent && sourceRef instanceof Element
          ? sourceNavigationTarget(sourceRef)?.href || null : null,
        html,
      },
      pointer,
      !preserveCurrent,
    );
  }

  function showTrigger(trigger, pointer) {
    if (!(trigger instanceof HTMLElement)) {
      return;
    }
    const templateId = trigger.dataset.template;
    const preserveCurrent = isInsideTooltipRoot(trigger) && activeState != null;
    showTemplate(templateId, "trigger", trigger, pointer, preserveCurrent);
  }

  function syntheticPointerFor(element) {
    const rect = element.getBoundingClientRect();
    return {
      clientX: rect.left + rect.width / 2,
      clientY: rect.bottom,
    };
  }

  root.addEventListener("mouseover", (event) => {
    const trigger = closestGuideTooltip(event.target);
    if (trigger) {
      if (activeState?.sourceType === "external" && isInsideTooltipRoot(trigger)) {
        return;
      }
      showTrigger(trigger, event);
    }
  });

  root.addEventListener("mousemove", (event) => {
    if (activeState?.sourceType === "external" && pointerInPopup(event)) {
      lastPointer = event;
      itemNavigation?.refresh();
      return;
    }
    lastPointer = event;
    if (!tooltipRoot.hidden) {
      position(event);
    }
  });

  root.addEventListener("mouseout", (event) => {
    if (activeState?.sourceType === "navigation"
      && !activeState.sourceRef.contains(event.relatedTarget)
      && !isInsideTooltipRoot(event.relatedTarget)) {
      hideAll();
      return;
    }
    const fromTrigger = closestGuideTooltip(event.target);
    const toTrigger = closestGuideTooltip(event.relatedTarget);

    if (fromTrigger && activeState?.sourceType === "trigger" && activeState.sourceRef === fromTrigger) {
      if (toTrigger && toTrigger !== fromTrigger) {
        return;
      }
      if (isInsideTooltipRoot(event.relatedTarget)) {
        return;
      }
      if (restoreStack.length && isInsideTooltipRoot(fromTrigger)) {
        restorePrevious(event);
        return;
      }
      hideAll();
      return;
    }

    if (isInsideTooltipRoot(event.target)) {
      if (activeState?.sourceType === "external") return;
      if (isInsideTooltipRoot(event.relatedTarget)) {
        return;
      }
      if (activeState?.sourceType === "trigger" && restoreStack.length) {
        restorePrevious(event);
        return;
      }
      if (toTrigger) {
        return;
      }
      if (activeState?.sourceType === "trigger"
        && activeState.sourceRef instanceof Element
        && activeState.sourceRef.contains(event.relatedTarget)) {
        return;
      }
      if (restoreStack.length) {
        restorePrevious(event);
        return;
      }
      if (activeState?.sourceType !== "external") {
        hideAll();
      }
    }
  });

  root.addEventListener("focusin", (event) => {
    const trigger = closestGuideTooltip(event.target);
    if (trigger) {
      showTrigger(trigger, syntheticPointerFor(trigger));
    }
  });

  root.addEventListener("focusout", (event) => {
    const fromTrigger = closestGuideTooltip(event.target);
    const toTrigger = closestGuideTooltip(event.relatedTarget);
    if (!fromTrigger || activeState?.sourceType !== "trigger" || activeState.sourceRef !== fromTrigger) {
      return;
    }
    if (toTrigger) {
      return;
    }
    if (restoreStack.length && isInsideTooltipRoot(fromTrigger)) {
      restorePrevious(syntheticPointerFor(fromTrigger));
      return;
    }
    hideAll();
  });

  root.addEventListener("pointerup", (event) => {
    if (event.pointerType !== "touch") return;
    const trigger = closestGuideTooltip(event.target);
    if (!trigger || trigger.closest("input, select, textarea")) return;
    if (activeState?.sourceType === "trigger" && activeState.sourceRef === trigger) {
      hideAll();
    } else {
      showTrigger(trigger, event);
    }
  });

  root.addEventListener("pointerdown", (event) => {
    if (event.pointerType === "touch" && !closestGuideTooltip(event.target) && !isInsideTooltipRoot(event.target)) {
      hideAll();
    }
  });

  window.GuideNHTooltips = {
    hide: hideAll,
    containsTooltip(target) {
      return isInsideTooltipRoot(target);
    },
    updatePointer(pointer) {
      if (pointerInPopup(pointer)) return;
      lastPointer = pointer || lastPointer;
      if (!tooltipRoot.hidden) {
        position(pointer);
      }
    },
    showExternalTemplate(templateSpec, owner, pointer) {
      if (activeState?.sourceType === "external" && activeState.sourceRef === owner
        && pointerInPopup(pointer)) return;
      if (activeState?.sourceType === "trigger" && restoreStack.some(state =>
        state.sourceType === "external" && state.sourceRef === owner)) {
        return;
      }
      const templateId = typeof templateSpec === "string" ? templateSpec : templateSpec?.templateId;
      const shiftTemplateId = typeof templateSpec === "string" ? null : templateSpec?.shiftTemplateId || null;
      const selectedTemplateId = eventShiftKey && shiftTemplateId ? shiftTemplateId : templateId;
      showTemplate(
        selectedTemplateId,
        "external",
        owner,
        pointer || lastPointer,
        false,
        shiftTemplateId,
        templateId,
      );
    },
    hideExternal(owner) {
      if (!activeState || activeState.sourceType !== "external" || activeState.sourceRef !== owner) {
        return;
      }
      hideAll();
    },
  };

  document.addEventListener("scroll", hideAll, { capture: true, passive: true });
  const refreshExternalTooltip = () => {
    if (!activeState || activeState.sourceType !== "external") {
      return;
    }
    const baseTemplateId = activeState.baseTemplateId || activeState.templateId;
    const selectedTemplateId = eventShiftKey && activeState.shiftTemplateId
      ? activeState.shiftTemplateId
      : baseTemplateId;
    const html = resolveTemplateHtml(selectedTemplateId);
    if (!html || selectedTemplateId === activeState.templateId) {
      return;
    }
    applyState({ ...activeState, templateId: selectedTemplateId, html }, lastPointer, false);
  };
  window.addEventListener("keydown", (event) => {
    if (event.key === "Shift" && !event.repeat) {
      eventShiftKey = true;
      refreshExternalTooltip();
    }
    if (event.key === "Escape") {
      hideAll();
    }
  });
  window.addEventListener("keyup", (event) => {
    if (event.key === "Shift") {
      eventShiftKey = false;
      refreshExternalTooltip();
    }
  });
  const templateNavigationTarget = (templateId, key, source) => {
    const template = document.getElementById(templateId);
    if (template?.content?.childElementCount !== 1) return null;
    let item = template?.content?.firstElementChild;
    // Unwrap layout containers, but do not borrow links from items inside rich content.
    while (item && !item.hasAttribute("data-guide-item-href")) {
      if (item.childElementCount !== 1 || item.hasAttribute("data-template")
        || Array.from(item.childNodes).some(node => node.nodeType === Node.TEXT_NODE && node.textContent.trim())) {
        return null;
      }
      item = item.firstElementChild;
    }
    const href = item?.getAttribute("data-guide-item-href");
    return href ? { key, source, href } : null;
  };
  const sourceNavigationTarget = source => {
    const item = source.closest("[data-guide-item-href]");
    if (source.hasAttribute("data-template") && isInsideTooltipRoot(source)
      && item === tooltipRoot.firstElementChild && item !== source) return null;
    const href = item?.getAttribute("data-guide-item-href");
    return href ? { key: item, source: item, href } : null;
  };
  itemNavigation = installGuideItemNavigation(tooltipRoot, () => {
    if (!activeState || tooltipRoot.hidden) return null;
    if (activeState.sourceType === "external") {
      const hovered = document.elementFromPoint(lastPointer?.clientX ?? -1, lastPointer?.clientY ?? -1);
      if (hovered instanceof Element && tooltipRoot.contains(hovered)) {
        const trigger = closestGuideTooltip(hovered);
        const target = sourceNavigationTarget(trigger || hovered);
        return target || (trigger ? templateNavigationTarget(trigger.dataset.template, trigger, trigger) : null);
      }
      const baseId = activeState.baseTemplateId || activeState.templateId;
      return templateNavigationTarget(baseId, `${activeState.sourceRef}:${baseId}`, null);
    }
    const source = activeState.sourceRef;
    if (!(source instanceof Element)) return null;
    if (!source.isConnected) {
      return activeState.navigationHref ? { key: source, source: null, href: activeState.navigationHref }
        : templateNavigationTarget(activeState.templateId, source, null);
    }
    return sourceNavigationTarget(source)
      || templateNavigationTarget(activeState.templateId, source, source);
  }, source => {
    if (tooltipRoot.hidden && source instanceof Element) {
      applyState({ sourceType: "navigation", sourceRef: source, html: "<span></span>" },
        lastPointer || syntheticPointerFor(source), true);
    }
  }, href => window.dispatchEvent(new CustomEvent("guide-item-navigate", { detail: { href } })));
}

function installPageBehaviors(root, hydrateScenes = true) {
  installMediaWikiSpecialFilters(root);
  installIngredientCycling(root);
  installImageAnnotations(root);
  installGuideSounds(root);
  installMermaidLayout(root);
  installMermaidPanZoom(root);
  installChartHoverTooltips(root);
  if (hydrateScenes) hydrateVisibleScenes(root);
}

function navigationState(sidebar) {
  const expanded = new Map();
  for (const node of sidebar?.querySelectorAll?.("[data-guide-nav-node]") || []) {
    expanded.set(node.dataset.guideNavNode, node.dataset.guideNavExpanded !== "false");
  }
  return {
    expanded,
    scrollTop: sidebar?.querySelector?.("[data-guide-navigation]")?.scrollTop || 0,
  };
}

function navigationStorageKey() {
  const siteRoot = document.querySelector("base")?.href || `${window.location.origin}/`;
  return `guidenh-site-navigation:${siteRoot}`;
}

function saveNavigationState(sidebar) {
  try {
    const state = navigationState(sidebar);
    window.localStorage.setItem(navigationStorageKey(), JSON.stringify(Object.fromEntries(state.expanded)));
  } catch (error) {
    console.warn("GuideNH could not save the navigation state.", error);
  }
}

function readSavedNavigationState() {
  try {
    const serialized = window.localStorage.getItem(navigationStorageKey());
    const parsed = serialized ? JSON.parse(serialized) : {};
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
  } catch (error) {
    console.warn("GuideNH could not restore the navigation state.", error);
    return {};
  }
}

function setNavigationNodeExpanded(node, expanded) {
  if (!(node instanceof HTMLElement) || !node.hasAttribute("data-guide-nav-expanded")) {
    return;
  }
  node.dataset.guideNavExpanded = expanded ? "true" : "false";
  const toggle = node.querySelector(":scope > [data-guide-nav-toggle]");
  if (toggle instanceof HTMLButtonElement) {
    toggle.setAttribute("aria-expanded", expanded ? "true" : "false");
  }
}

function restoreNavigationState(sidebar, state, currentUrl) {
  for (const link of sidebar.querySelectorAll('[aria-current="page"]')) {
    link.removeAttribute("aria-current");
  }
  const nodes = sidebar.querySelectorAll("[data-guide-nav-node]");
  const savedState = readSavedNavigationState();
  for (const node of nodes) {
    const nodeId = node.dataset.guideNavNode;
    const saved = savedState[nodeId] ?? state.expanded.get(nodeId);
    if (saved !== undefined) {
      setNavigationNodeExpanded(node, saved);
    }
  }
  const current = findCurrentNavigationLink(sidebar, currentUrl);
  if (current) {
    current.setAttribute("aria-current", "page");
    for (let node = current.closest("[data-guide-nav-node]"); node; node = node.parentElement?.closest("[data-guide-nav-node]")) {
      setNavigationNodeExpanded(node, true);
    }
  }
  const navigation = sidebar.querySelector("[data-guide-navigation]");
  if (navigation instanceof HTMLElement) {
    navigation.scrollTop = state.scrollTop;
    window.requestAnimationFrame(() => current?.scrollIntoView({ block: "nearest" }));
  }
}

function findCurrentNavigationLink(root, url) {
  const target = new URL(url, window.location.href);
  for (const link of root.querySelectorAll("[data-guide-navigation] a[href]")) {
    const candidate = new URL(link.getAttribute("href"), document.baseURI);
    if (candidate.origin === target.origin && candidate.pathname === target.pathname && candidate.search === target.search) {
      return link;
    }
  }
  return null;
}

function installNavigationUi(root) {
  if (!(root instanceof HTMLElement) || root.dataset.guideNavigationUiInstalled === "true") {
    return;
  }
  root.dataset.guideNavigationUiInstalled = "true";
  root.addEventListener("click", (event) => {
    const target = event.target instanceof Element ? event.target : null;
    const toggle = target?.closest("[data-guide-nav-toggle]");
    const label = target?.closest("[data-guide-nav-label]");
    const link = target?.closest("a[href]");
    if (
      !(toggle instanceof HTMLButtonElement)
      && !(label instanceof HTMLElement)
      && !(link instanceof HTMLAnchorElement)
    ) {
      return;
    }
    const node = (toggle || label || link)?.closest("[data-guide-nav-node]");
    if (!(node instanceof HTMLElement)) {
      return;
    }
    if (link instanceof HTMLAnchorElement) {
      if (node.dataset.guideNavExpanded !== "false") {
        return;
      }
      setNavigationNodeExpanded(node, true);
      saveNavigationState(root);
      return;
    }
    event.preventDefault();
    setNavigationNodeExpanded(node, node.dataset.guideNavExpanded === "false");
    saveNavigationState(root);
  });
}

function updateMobileNavigationLabels() {
  const toggle = document.querySelector("[data-guide-mobile-nav-toggle]");
  const backdrop = document.querySelector("[data-guide-mobile-nav-backdrop]");
  toggle?.setAttribute("aria-label", siteText("navigation", toggle.getAttribute("aria-expanded") === "true" ? "close" : "open"));
  backdrop?.setAttribute("aria-label", siteText("navigation", "close"));
}

function setMobileNavigationOpen(open) {
  const toggle = document.querySelector("[data-guide-mobile-nav-toggle]");
  const backdrop = document.querySelector("[data-guide-mobile-nav-backdrop]");
  const sidebar = document.querySelector(".guide-sidebar");
  const content = document.querySelector(".guide-content");
  const narrow = window.matchMedia("(max-width: 900px)").matches;
  const expanded = open && narrow;
  document.body.dataset.guideMobileNavOpen = String(expanded);
  toggle?.setAttribute("aria-expanded", String(expanded));
  if (backdrop) backdrop.hidden = !expanded;
  if (sidebar) sidebar.inert = narrow && !expanded;
  if (content) content.inert = expanded;
  updateMobileNavigationLabels();
}

function installMobileNavigation() {
  const toggle = document.querySelector("[data-guide-mobile-nav-toggle]");
  const backdrop = document.querySelector("[data-guide-mobile-nav-backdrop]");
  toggle?.addEventListener("click", () => {
    const open = toggle.getAttribute("aria-expanded") !== "true";
    setMobileNavigationOpen(open);
    if (open) document.querySelector("[data-guide-navigation] a[aria-current='page']")?.focus({ preventScroll: true });
  });
  backdrop?.addEventListener("click", () => setMobileNavigationOpen(false));
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && document.body.dataset.guideMobileNavOpen === "true") {
      setMobileNavigationOpen(false);
      toggle?.focus();
    }
  });
  window.matchMedia("(max-width: 900px)").addEventListener("change", () => setMobileNavigationOpen(false));
  setMobileNavigationOpen(false);
}

function installLanguageMenus(root) {
  for (const menu of root.querySelectorAll("[data-guide-language-menu]")) {
    const trigger = menu.querySelector(":scope > [data-guide-language-menu-trigger]");
    const options = menu.querySelector(":scope > [data-guide-language-menu-options]");
    if (!(trigger instanceof HTMLButtonElement) || !(options instanceof HTMLElement)) {
      continue;
    }
    for (const link of options.querySelectorAll("[data-guide-language-code]")) {
      link.addEventListener("click", () => rememberSiteLanguage(link.dataset.guideLanguageCode));
    }
    const close = () => {
      trigger.setAttribute("aria-expanded", "false");
      options.hidden = true;
    };
    trigger.addEventListener("click", (event) => {
      event.preventDefault();
      const open = options.hidden;
      for (const otherMenu of document.querySelectorAll("[data-guide-language-menu]")) {
        const otherTrigger = otherMenu.querySelector(":scope > [data-guide-language-menu-trigger]");
        const otherOptions = otherMenu.querySelector(":scope > [data-guide-language-menu-options]");
        if (otherTrigger instanceof HTMLButtonElement && otherOptions instanceof HTMLElement) {
          otherTrigger.setAttribute("aria-expanded", "false");
          otherOptions.hidden = true;
        }
      }
      trigger.setAttribute("aria-expanded", open ? "true" : "false");
      options.hidden = !open;
    });
    menu.__guideLanguageMenuClose = close;
  }
  if (!window.__guideLanguageMenuDismissInstalled) {
    window.__guideLanguageMenuDismissInstalled = true;
    document.addEventListener("click", (event) => {
      for (const menu of document.querySelectorAll("[data-guide-language-menu]")) {
        if (event.target instanceof Node && menu.contains(event.target)) {
          continue;
        }
        menu.__guideLanguageMenuClose?.();
      }
    });
  }
}

function shouldHandleSiteNavigation(event, link) {
  if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
    return false;
  }
  if (link.target || link.hasAttribute("download")) {
    return false;
  }
  const target = new URL(link.href, window.location.href);
  return target.origin === window.location.origin;
}

function installSiteRouter() {
  let requestId = 0;
  history.scrollRestoration = "manual";

  const navigate = async (url, pushState) => {
    window.GuideNHTooltips?.hide();
    const target = new URL(url, window.location.href);
    const currentContent = document.getElementById("page-content");
    const currentSidebar = document.querySelector(".guide-sidebar");
    const currentLanguage = document.documentElement.lang;
    if (!(currentContent instanceof HTMLElement) || !(currentSidebar instanceof HTMLElement)) {
      window.location.assign(target);
      return;
    }
    const activeRequest = ++requestId;
    try {
      const response = await fetch(target.href, { headers: { "X-Requested-With": "GuideNH" } });
      if (!response.ok) {
        throw new Error(`Page request failed: ${response.status}`);
      }
      const parsed = new DOMParser().parseFromString(await response.text(), "text/html");
      const nextContent = parsed.getElementById("page-content");
      const nextSidebar = parsed.querySelector(".guide-sidebar");
      const nextLanguage = parsed.documentElement.lang;
      const nextLanguageSwitcher = parsed.querySelector(".guide-header-lang");
      if (activeRequest !== requestId) {
        return;
      }
      if (!nextContent || !nextSidebar) {
        window.location.assign(target);
        return;
      }

      const nextSidebarSource = nextSidebar.querySelector("[data-guide-sidebar-src]")?.dataset.guideSidebarSrc;
      const reuseSidebar = nextLanguage === currentLanguage && nextSidebarSource
        && currentSidebar.dataset.guideSidebarSource === new URL(nextSidebarSource, document.baseURI).href;
      await Promise.all([
        reuseSidebar ? Promise.resolve() : loadSidebar(nextSidebar),
        loadPageTemplates(nextContent),
        loadLanguageMenu(nextLanguageSwitcher),
        ensureSiteLanguage(nextLanguage || currentLanguage),
      ]);
      if (activeRequest !== requestId) return;

      const state = navigationState(currentSidebar);
      stopGuideSounds(currentContent);
      stopIngredientCycling(currentContent);
      stopImageLayout(currentContent);
      disposeHydratedScenes(currentContent);
      currentContent.replaceChildren(...Array.from(nextContent.childNodes, (node) => document.importNode(node, true)));
      currentContent.className = nextContent.className;
      document.title = parsed.title;
      document.documentElement.lang = nextLanguage || currentLanguage;
      setMobileNavigationOpen(false);

      if (!reuseSidebar) {
        currentSidebar.replaceChildren(...Array.from(nextSidebar.childNodes, (node) => document.importNode(node, true)));
        if (nextSidebar.dataset.guideSidebarSource) {
          currentSidebar.dataset.guideSidebarSource = nextSidebar.dataset.guideSidebarSource;
        } else {
          delete currentSidebar.dataset.guideSidebarSource;
        }
        installSearchUi(currentSidebar);
        installNavigationUi(currentSidebar);
      }
      const headerLanguage = document.querySelector(".guide-header-lang");
      if (headerLanguage && nextLanguageSwitcher) {
        headerLanguage.replaceChildren(...Array.from(nextLanguageSwitcher.childNodes, (node) => document.importNode(node, true)));
      }
      installLanguageMenus(document);
      restoreNavigationState(currentSidebar, state, target);
      installPageBehaviors(currentContent);
      const contentScroll = document.querySelector(".guide-content");
      if (contentScroll instanceof HTMLElement) {
        contentScroll.scrollTop = 0;
      }
      if (pushState) {
        history.pushState({}, "", target);
      }
    } catch (error) {
      if (activeRequest !== requestId) return;
      console.warn("GuideNH site navigation failed; using a full-page load instead.", error);
      window.location.assign(target);
    }
  };

  document.addEventListener("click", (event) => {
    const link = event.target instanceof Element ? event.target.closest("a[href]") : null;
    if (!(link instanceof HTMLAnchorElement) || !shouldHandleSiteNavigation(event, link)) {
      return;
    }
    event.preventDefault();
    const target = new URL(link.href, window.location.href);
    if (target.pathname === window.location.pathname && target.search === window.location.search) {
      setMobileNavigationOpen(false);
      return;
    }
    navigate(link.href, true);
  });
  window.addEventListener("popstate", () => navigate(window.location.href, false));
  window.addEventListener("guide-item-navigate", event => navigate(event.detail.href, true));
}

document.addEventListener("DOMContentLoaded", async () => {
  const base = document.querySelector("base");
  // Keep the site root stable when history navigation changes the page depth.
  if (base) base.href = base.href;
  const sidebar = document.querySelector(".guide-sidebar");
  const content = document.getElementById("page-content");
  loadCustomSiteLink();
  installMobileNavigation();
  installTooltips(document);
  if (content instanceof HTMLElement) hydrateVisibleScenes(content);
  const loaded = await Promise.allSettled([
    content instanceof HTMLElement ? loadPageTemplates(content) : Promise.resolve(),
    sidebar instanceof HTMLElement ? loadSidebar(sidebar) : Promise.resolve(),
    loadLanguageMenu(document.querySelector(".guide-header-lang")),
  ]);
  for (const result of loaded) {
    if (result.status === "rejected") console.error("GuideNH page resources could not be loaded.", result.reason);
  }
  installLanguageMenus(document);
  if (content instanceof HTMLElement) {
    installPageBehaviors(content, false);
  }
  if (sidebar instanceof HTMLElement) {
    installSearchUi(sidebar);
    installNavigationUi(sidebar);
    restoreNavigationState(sidebar, navigationState(sidebar), window.location.href);
  }
  installSiteRouter();
});

function installMermaidLayout(root) {
  const stages = root.querySelectorAll(".guide-mermaid-stage[data-guide-mermaid-stage]");
  if (!stages.length) {
    return;
  }
  const PADDING = 20;
  const GAP_X = 56;
  const GAP_Y = 28;
  let rafId = 0;
  const scheduleLayout = () => {
    if (rafId) {
      return;
    }
    rafId = window.requestAnimationFrame(() => {
      rafId = 0;
      stages.forEach((stage) => layoutMermaidStage(stage, PADDING, GAP_X, GAP_Y));
    });
  };
  if (!window.__guideMermaidLayoutResizeInstalled) {
    window.__guideMermaidLayoutResizeInstalled = true;
    window.addEventListener("resize", scheduleLayout, { passive: true });
  }
  stages.forEach((stage) => {
    if (stage.__guideMermaidLayoutInstalled) {
      return;
    }
    stage.__guideMermaidLayoutInstalled = true;
    if (window.ResizeObserver) {
      const observer = new window.ResizeObserver(() => scheduleLayout());
      observer.observe(stage);
      stage.querySelectorAll(".guide-mermaid-node").forEach((node) => observer.observe(node));
      stage.__guideMermaidResizeObserver = observer;
    }
    stage.querySelectorAll("img").forEach((image) => {
      if (image.complete) {
        return;
      }
      image.addEventListener("load", scheduleLayout, { once: true });
    });
  });
  scheduleLayout();
}

function layoutMermaidStage(stage, padding, gapX, gapY) {
  const svg = stage.querySelector("svg.guide-mermaid-canvas");
  const layer = stage.querySelector(".guide-mermaid-node-layer");
  if (!svg || !layer) {
    return;
  }
  const nodeElements = Array.from(layer.querySelectorAll(".guide-mermaid-node[data-node-id]"));
  if (!nodeElements.length) {
    svg.setAttribute("width", "100");
    svg.setAttribute("height", "40");
    svg.setAttribute("viewBox", "0 0 100 40");
    svg.innerHTML = "";
    return;
  }

  const nodes = new Map();
  nodeElements.forEach((el) => {
    nodes.set(el.dataset.nodeId || "", {
      id: el.dataset.nodeId || "",
      parentId: el.dataset.parentId || "",
      el,
      children: [],
      width: Math.max(64, Math.ceil(el.offsetWidth)),
      height: Math.max(32, Math.ceil(el.offsetHeight)),
      subtreeWidth: 0,
      subtreeHeight: 0,
      x: 0,
      y: 0,
    });
  });

  let rootNode = null;
  nodes.forEach((node) => {
    const parent = node.parentId ? nodes.get(node.parentId) : null;
    if (parent) {
      parent.children.push(node);
    } else if (!rootNode) {
      rootNode = node;
    }
  });
  if (!rootNode) {
    rootNode = nodes.values().next().value;
  }
  if (!rootNode) {
    return;
  }

  const measure = (node) => {
    if (!node.children.length) {
      node.subtreeWidth = node.width;
      node.subtreeHeight = node.height;
      return;
    }
    let childrenWidth = 0;
    let childrenHeight = 0;
    node.children.forEach((child) => {
      measure(child);
      childrenWidth += child.subtreeWidth;
      childrenHeight = Math.max(childrenHeight, child.subtreeHeight);
    });
    childrenWidth += gapX * (node.children.length - 1);
    node.subtreeWidth = Math.max(node.width, childrenWidth);
    node.subtreeHeight = node.height + gapY + childrenHeight;
  };

  const place = (node, x, y) => {
    node.x = x + (node.subtreeWidth - node.width) / 2;
    node.y = y;
    if (!node.children.length) {
      return;
    }
    let childrenWidth = 0;
    node.children.forEach((child) => {
      childrenWidth += child.subtreeWidth;
    });
    childrenWidth += gapX * (node.children.length - 1);
    let cursorX = x + (node.subtreeWidth - childrenWidth) / 2;
    const childY = y + node.height + gapY;
    node.children.forEach((child) => {
      place(child, cursorX, childY);
      cursorX += child.subtreeWidth + gapX;
    });
  };

  measure(rootNode);
  place(rootNode, 0, 0);

  const totalWidth = Math.ceil(rootNode.subtreeWidth + padding * 2);
  const totalHeight = Math.ceil(rootNode.subtreeHeight + padding * 2);
  stage.style.width = `${totalWidth}px`;
  stage.style.height = `${totalHeight}px`;
  svg.setAttribute("width", String(totalWidth));
  svg.setAttribute("height", String(totalHeight));
  svg.setAttribute("viewBox", `0 0 ${totalWidth} ${totalHeight}`);

  const paths = [];
  const drawConnectors = (node) => {
    const parentCx = padding + node.x + node.width / 2;
    const parentBottom = padding + node.y + node.height;
    node.children.forEach((child) => {
      const childCx = padding + child.x + child.width / 2;
      const childTop = padding + child.y;
      const midY = (parentBottom + childTop) / 2;
      paths.push(`M${parentCx} ${parentBottom} V${midY} H${childCx} V${childTop}`);
      drawConnectors(child);
    });
  };
  drawConnectors(rootNode);
  svg.innerHTML = `
    <rect x="0.5" y="0.5" width="${Math.max(1, totalWidth - 1)}" height="${Math.max(1, totalHeight - 1)}"
      fill="rgba(12,17,23,0.94)" stroke="rgba(67,76,87,0.4)" stroke-width="1"></rect>
    ${paths
      .map(
        (path) =>
          `<path d="${path}" stroke="rgba(93,108,124,1)" stroke-width="1" fill="none" shape-rendering="crispEdges"></path>`,
      )
      .join("")}
  `;

  nodes.forEach((node) => {
    node.el.style.transform = `translate(${padding + node.x}px, ${padding + node.y}px)`;
    node.el.style.setProperty("--guide-mermaid-accent", node.el.dataset.accent || "#7AA2F7");
  });
}

/**
 * Pan + zoom for mindmap canvases. Each `.guide-mermaid-pan` gains drag-to-pan
 * (pointerdown/move/up) plus wheel-to-zoom around the cursor. The transform is
 * applied to the inner stage via CSS `transform: translate(tx,ty) scale(s)`.
 */
function installMermaidPanZoom(root) {
  const containers = root.querySelectorAll(".guide-mermaid-pan[data-guide-pannable]");
  for (const container of containers) {
    const stage = container.querySelector(".guide-mermaid-stage") || container.querySelector("svg");
    if (!stage) continue;
    const state = { tx: 0, ty: 0, scale: 1, dragging: false, startX: 0, startY: 0, startTx: 0, startTy: 0 };
    const pointers = new Map();
    let pinch = null;
    const pinchGeometry = () => {
      const [first, second] = [...pointers.values()];
      const rect = container.getBoundingClientRect();
      return {
        distance: Math.max(1, Math.hypot(second.x - first.x, second.y - first.y)),
        x: (first.x + second.x) / 2 - rect.left,
        y: (first.y + second.y) / 2 - rect.top,
      };
    };
    const apply = () => {
      stage.style.transform = `translate(${state.tx}px, ${state.ty}px) scale(${state.scale})`;
    };
    apply();
    container.addEventListener("pointerdown", (event) => {
      if (event.button !== 0) return;
      if (event.target instanceof Element
        && event.target.closest("a, button, input, textarea, select, summary, [role='button'], [data-guide-sound]")) {
        return;
      }
      state.dragging = true;
      pointers.set(event.pointerId, { x: event.clientX, y: event.clientY });
      state.startX = event.clientX;
      state.startY = event.clientY;
      state.startTx = state.tx;
      state.startTy = state.ty;
      container.classList.add("is-grabbing");
      if (event.isTrusted) container.setPointerCapture?.(event.pointerId);
      if (pointers.size === 2) {
        pinch = { ...pinchGeometry(), scale: state.scale, tx: state.tx, ty: state.ty };
      }
      event.preventDefault();
    });
    container.addEventListener("pointermove", (event) => {
      if (!state.dragging) return;
      if (!pointers.has(event.pointerId)) return;
      pointers.set(event.pointerId, { x: event.clientX, y: event.clientY });
      if (pointers.size === 2 && pinch) {
        const geometry = pinchGeometry();
        state.scale = Math.max(0.2, Math.min(8, pinch.scale * geometry.distance / pinch.distance));
        const ratio = state.scale / pinch.scale;
        state.tx = geometry.x - (pinch.x - pinch.tx) * ratio;
        state.ty = geometry.y - (pinch.y - pinch.ty) * ratio;
        apply();
        return;
      }
      state.tx = state.startTx + (event.clientX - state.startX);
      state.ty = state.startTy + (event.clientY - state.startY);
      apply();
    });
    const stopDrag = (event) => {
      if (!state.dragging) return;
      pointers.delete(event.pointerId);
      pinch = null;
      state.dragging = pointers.size > 0;
      container.classList.toggle("is-grabbing", state.dragging);
      if (state.dragging) {
        const remaining = pointers.values().next().value;
        state.startX = remaining.x;
        state.startY = remaining.y;
        state.startTx = state.tx;
        state.startTy = state.ty;
      }
      try { container.releasePointerCapture?.(event.pointerId); } catch (_) {}
    };
    container.addEventListener("pointerup", stopDrag);
    container.addEventListener("pointercancel", stopDrag);
    container.addEventListener("wheel", (event) => {
      event.preventDefault();
      const rect = container.getBoundingClientRect();
      const cx = event.clientX - rect.left;
      const cy = event.clientY - rect.top;
      const factor = event.deltaY < 0 ? 1.15 : 1 / 1.15;
      const newScale = Math.max(0.2, Math.min(8, state.scale * factor));
      const ratio = newScale / state.scale;
      state.tx = cx - (cx - state.tx) * ratio;
      state.ty = cy - (cy - state.ty) * ratio;
      state.scale = newScale;
      apply();
    }, { passive: false });
    container.addEventListener("dblclick", () => {
      state.tx = 0;
      state.ty = 0;
      state.scale = 1;
      apply();
    });
  }
}

function installChartHoverTooltips(root) {
  const svgs = root.querySelectorAll("svg.guide-chart, svg.guide-chart-svg, svg.guide-function-graph");
  for (const svg of svgs) {
    const isFunctionGraph = svg.classList.contains("guide-function-graph");
    const owner = `chart-${Math.random().toString(36).slice(2, 9)}`;
    let popupEl = null;
    const ensurePopup = () => {
      if (popupEl) return popupEl;
      popupEl = document.createElement("div");
      popupEl.className = "guide-tooltip-popup guide-chart-tooltip-popup";
      popupEl.hidden = true;
      document.body.appendChild(popupEl);
      return popupEl;
    };
    const showText = (text, ev) => {
      const el = ensurePopup();
      el.textContent = text;
      el.hidden = false;
      positionPopup(el, ev);
    };
    const showHtml = (html, ev) => {
      const el = ensurePopup();
      el.innerHTML = html;
      el.hidden = false;
      positionPopup(el, ev);
    };
    const hide = () => {
      if (popupEl) popupEl.hidden = true;
    };
    const plotData = [];
    if (isFunctionGraph) {
      svg.querySelectorAll("polyline.guide-chart-shape").forEach((poly) => {
        const titleEl = poly.querySelector("title");
        const label = titleEl?.textContent ?? "";
        titleEl?.remove();
        const raw = poly.getAttribute("points") || "";
        const pts = [];
        for (const tok of raw.trim().split(/\s+/)) {
          const [px, py] = tok.split(",");
          const fx = parseFloat(px), fy = parseFloat(py);
          if (Number.isFinite(fx) && Number.isFinite(fy)) pts.push([fx, fy]);
        }
        if (pts.length) {
          plotData.push({
            pts,
            label,
            label: poly.dataset.plotLabel || label,
            expression: poly.dataset.plotExpression || label,
            inverse: poly.dataset.plotInverse === "true",
            showFunction: poly.dataset.plotShowFunction !== "false",
            showValues: poly.dataset.plotShowValues !== "false",
            tooltip: poly.dataset.plotTooltip || "",
            tooltipHtml: poly.dataset.plotTooltipHtml || ""
          });
        }
      });
    }
    svg.querySelectorAll(".guide-chart-shape").forEach((shape) => {
      const titleEl = shape.querySelector("title");
      const text = titleEl?.textContent ?? "";
      if (titleEl && !(isFunctionGraph && shape.matches("polyline.guide-chart-shape"))) titleEl.remove();
      shape.addEventListener("mouseenter", (ev) => showText(text, ev));
      shape.addEventListener("mousemove", (ev) => positionPopup(popupEl, ev));
      shape.addEventListener("mouseleave", hide);
    });
    if (isFunctionGraph) {
      const meta = svg.querySelector("metadata[data-plot-domain]");
      const dom = meta ? {
        xMin: parseFloat(meta.getAttribute("data-x-min")),
        xMax: parseFloat(meta.getAttribute("data-x-max")),
        yMin: parseFloat(meta.getAttribute("data-y-min")),
        yMax: parseFloat(meta.getAttribute("data-y-max")),
        left: parseFloat(meta.getAttribute("data-plot-left")),
        right: parseFloat(meta.getAttribute("data-plot-right")),
        top: parseFloat(meta.getAttribute("data-plot-top")),
        bottom: parseFloat(meta.getAttribute("data-plot-bottom")),
      } : null;
      svg.addEventListener("mousemove", (ev) => {
        if (!dom || !plotData.length) {
          const closest = findClosestShape(svg, ev);
          const text = closest?.querySelector("title")?.textContent;
          if (text) showText(text, ev); else hide();
          return;
        }
        const rect = svg.getBoundingClientRect();
        const sx = (ev.clientX - rect.left) * (svg.viewBox.baseVal.width || rect.width) / rect.width;
        const sy = (ev.clientY - rect.top) * (svg.viewBox.baseVal.height || rect.height) / rect.height;
        if (sx < dom.left || sx > dom.right || sy < dom.top || sy > dom.bottom) {
          hide();
          return;
        }
        const dataX = dom.xMin + (sx - dom.left) / (dom.right - dom.left) * (dom.xMax - dom.xMin);
        let best = null;
        let bestDist = Infinity;
        for (const plot of plotData) {
          const y = interpolateAtX(plot.pts, sx);
          if (y === null) continue;
          const d = Math.abs(y - sy);
          if (d < bestDist) { bestDist = d; best = { plot, svgY: y }; }
        }
        if (!best) { hide(); return; }
        const svgUPerCssPx = (svg.viewBox.baseVal.width || rect.width) / rect.width;
        const THRESHOLD_CSS_PX = 10;
        if (bestDist > THRESHOLD_CSS_PX * svgUPerCssPx) { hide(); return; }
        const dataY = dom.yMin + (dom.bottom - best.svgY) / (dom.bottom - dom.top) * (dom.yMax - dom.yMin);
        showHtml(buildFunctionGraphTooltip(best.plot, dataX, dataY), ev);
      });
      svg.addEventListener("mouseleave", hide);
    }
  }
}

function buildFunctionGraphTooltip(plot, dataX, dataY) {
  const lines = [];
  if (plot.label) lines.push(`<p>${escapeGuideTooltipHtml(plot.label)}</p>`);
  if (plot.showFunction) {
    const expression = plot.expression || "";
    const hasAssignment = /^\s*[xy]\s*=/.test(expression);
    lines.push(`<p>${escapeGuideTooltipHtml(hasAssignment ? expression : `${plot.inverse ? "x" : "y"} = ${expression}`)}</p>`);
  }
  if (plot.showValues) {
    lines.push(`<p>x = ${dataX.toFixed(3)}, y = ${dataY.toFixed(3)}</p>`);
  }
  if (plot.tooltip) {
    lines.push(`<p>${escapeGuideTooltipHtml(plot.tooltip).replace(/\\n/g, "<br>")}</p>`);
  }
  if (plot.tooltipHtml) lines.push(plot.tooltipHtml);
  return lines.join("");
}

function escapeGuideTooltipHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/\"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function interpolateAtX(pts, x) {
  for (let i = 1; i < pts.length; i++) {
    const a = pts[i - 1], b = pts[i];
    const lo = Math.min(a[0], b[0]);
    const hi = Math.max(a[0], b[0]);
    if (x >= lo && x <= hi && hi !== lo) {
      const t = (x - a[0]) / (b[0] - a[0]);
      return a[1] + (b[1] - a[1]) * t;
    }
  }
  return null;
}

function findClosestShape(svg, ev) {
  let best = null;
  let bestDist = Infinity;
  const rect = svg.getBoundingClientRect();
  const cx = ev.clientX - rect.left;
  const cy = ev.clientY - rect.top;
  for (const shape of svg.querySelectorAll(".guide-chart-shape")) {
    const r = shape.getBoundingClientRect();
    const sx = r.left - rect.left + r.width / 2;
    const sy = r.top - rect.top + r.height / 2;
    const d = (sx - cx) * (sx - cx) + (sy - cy) * (sy - cy);
    if (d < bestDist) { bestDist = d; best = shape; }
  }
  return best;
}

function positionPopup(el, ev) {
  if (!el) return;
  const x = ev.clientX + 14;
  const y = ev.clientY + 14;
  el.style.left = `${x}px`;
  el.style.top = `${y}px`;
}
