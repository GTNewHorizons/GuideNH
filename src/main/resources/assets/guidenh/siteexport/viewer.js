import { siteSection } from "./locale.js";

let modelViewerModulePromise;
let loadedModelViewerModule;

async function getModelViewerModule() {
  if (loadedModelViewerModule) {
    return loadedModelViewerModule;
  }

  const moduleUrl = new URL("./model-viewer/modelViewer.js", import.meta.url).href;
  modelViewerModulePromise ||= import(moduleUrl).then((module) => {
    loadedModelViewerModule = module;
    return module;
  });
  return modelViewerModulePromise;
}

const activeScenes = [];
// Keep hydrated scenes alive until the current page is replaced. The cap protects
// browsers that limit the number of simultaneous WebGL contexts.
const MAX_ACTIVE_SCENES = 16;
// Tracks one observer per hydration root so repeated tooltip/document hydration
// does not stack duplicate IntersectionObservers over the same scene nodes.
const observerEntries = new Map();
const sceneObservers = new WeakMap();
const observerSources = new WeakMap();
let sceneOperation = Promise.resolve();

function queueSceneOperation(work) {
  const operation = sceneOperation.then(work);
  sceneOperation = operation.catch((error) => console.error("GuideNH scene operation failed.", error));
  return operation;
}

function markActive(node) {
  const idx = activeScenes.indexOf(node);
  if (idx >= 0) {
    activeScenes.splice(idx, 1);
  }
  activeScenes.push(node);
}

async function disposeNode(node) {
  const idx = activeScenes.indexOf(node);
  if (idx >= 0) {
    activeScenes.splice(idx, 1);
  }
  const runtimeNode = node.__guidenhHydratedRoot;
  if (loadedModelViewerModule?.disposeHydratedScenes) {
    loadedModelViewerModule.disposeHydratedScenes(runtimeNode || node);
  }
  const observer = sceneObservers.get(node);
  if (observer && runtimeNode instanceof HTMLElement && runtimeNode !== node) {
    observer.unobserve(runtimeNode);
    observerSources.delete(runtimeNode);
    if (node.isConnected) {
      observer.observe(node);
    }
  }
  delete node.__guidenhHydratedRoot;
  delete node.dataset.sceneHydrated;
}

async function hydrateNode(node, module, isCurrent = () => true) {
  if (node.dataset.sceneHydrated === "true" || node.dataset.sceneHydrated === "loading") {
    return;
  }
  if (!node.isConnected || !isCurrent()) {
    return;
  }
  if (activeScenes.length >= MAX_ACTIVE_SCENES) {
    return;
  }
  clearHydrationFailure(node);
  try {
    node.dataset.sceneHydrated = "loading";
    const runtime = await module.setupGameScene(node);
    if (!isCurrent()) {
      module.disposeHydratedScenes?.(runtime?.wrapper);
      delete node.dataset.sceneHydrated;
      return;
    }
    if (!runtime?.wrapper?.isConnected) {
      if (runtime?.wrapper) {
        module.disposeHydratedScenes?.(runtime.wrapper);
      }
      markHydrationFailure(node, new Error("The scene runtime did not create a 3D viewer."));
      return;
    }
    node.__guidenhHydratedRoot = runtime.wrapper || node;
    node.dataset.sceneHydrated = "true";
    clearHydrationFailure(node);
    markActive(node);
    const observer = sceneObservers.get(node);
    if (observer && runtime.wrapper instanceof HTMLElement && runtime.wrapper !== node) {
      observer.unobserve(node);
      observerSources.set(runtime.wrapper, node);
      observer.observe(runtime.wrapper);
    }
  } catch (error) {
    if (isCurrent()) {
      markHydrationFailure(node, error);
    }
  }
}

function clearHydrationFailure(node) {
  const sibling = node.nextElementSibling;
  if (sibling instanceof HTMLElement && sibling.classList.contains("scene-hydration-error")) {
    sibling.remove();
  } else {
    node.parentElement?.querySelector(":scope > .scene-hydration-error")?.remove();
  }
  node.classList.remove("scene-hydration-fallback");
}

function sceneUiText() {
  return siteSection("sceneLoading");
}

function markHydrationFailure(node, error) {
  node.dataset.sceneHydrated = "fallback";
  node.classList.add("scene-hydration-fallback");
  console.error("GuideNH game scene hydration failed.", error);

  const parent = node.parentElement;
  if (!(parent instanceof HTMLElement)) {
    return;
  }
  let message = parent.querySelector(":scope > .scene-hydration-error");
  if (!(message instanceof HTMLElement)) {
    const labels = sceneUiText();
    message = node.ownerDocument.createElement("div");
    message.className = "scene-hydration-error";
    const text = node.ownerDocument.createElement("span");
    text.textContent = labels.error;
    const retry = node.ownerDocument.createElement("button");
    retry.type = "button";
    retry.textContent = labels.retry;
    retry.addEventListener("click", async () => {
      node.dataset.sceneHydrated = "pending";
      retry.disabled = true;
      text.textContent = sceneUiText().loading;
      try {
        await queueSceneOperation(async () => hydrateNode(node, await getModelViewerModule()));
      } catch (retryError) {
        markHydrationFailure(node, retryError);
      } finally {
        retry.disabled = false;
      }
    });
    message.append(text, retry);
    node.insertAdjacentElement("afterend", message);
  }
  const detail = error instanceof Error ? error.message : String(error);
  message.title = detail;
  message.setAttribute("role", "status");
}

function disconnectObserverEntry(root) {
  const entry = observerEntries.get(root);
  if (!entry) {
    return;
  }
  entry.observer.disconnect();
  entry.invalid = true;
  observerEntries.delete(root);
}

export function hydrateVisibleScenes(root) {
  if (!root?.querySelectorAll) {
    return;
  }
  disconnectObserverEntry(root);
  const sceneNodes = Array.from(root.querySelectorAll("[data-scene-src]"));
  if (!sceneNodes.length) {
    return;
  }

  if (typeof IntersectionObserver !== "function") {
    for (const node of sceneNodes) {
      queueSceneOperation(async () => hydrateNode(node, await getModelViewerModule()))
        .catch((error) => markHydrationFailure(node, error));
    }
    return;
  }

  const observer = new IntersectionObserver((entries) => queueSceneOperation(async () => {
    if (observerEntry.invalid) {
      return;
    }
    let module;
    try {
      module = await getModelViewerModule();
    } catch (error) {
      for (const entry of entries) {
        if (entry.isIntersecting && entry.target instanceof HTMLImageElement) {
          markHydrationFailure(entry.target, error);
        }
      }
      return;
    }
    for (const entry of entries) {
      const observedNode = entry.target;
      const node = observerSources.get(observedNode) || observedNode;
      if (!observedNode.isConnected) {
        observer.unobserve(observedNode);
        await disposeNode(node);
        continue;
      }
      if (entry.isIntersecting) {
        const runtimeConnected = node.__guidenhHydratedRoot instanceof HTMLElement
          && node.__guidenhHydratedRoot.isConnected;
        if (!node.isConnected && !runtimeConnected) {
          continue;
        }
        if (node.dataset.sceneHydrated === "true") {
          markActive(node);
          continue;
        }
        if (activeScenes.length >= MAX_ACTIVE_SCENES) {
          continue;
        }
        await hydrateNode(node, module, () => !observerEntry.invalid);
      }
    }
  }), { rootMargin: "128px 0px" });

  const observerEntry = { observer, nodes: sceneNodes, invalid: false };

  for (const node of sceneNodes) {
    sceneObservers.set(node, observer);
    observer.observe(node);
  }
  observerEntries.set(root, observerEntry);
}

export function disposeHydratedScenes(root) {
  if (!root) {
    return;
  }
  disconnectObserverEntry(root);
  // Drop any nodes inside `root` from our bookkeeping so they don't leak across navigations.
  if (root.querySelectorAll) {
    for (const node of root.querySelectorAll("[data-scene-src]")) {
      const idx = activeScenes.indexOf(node);
      if (idx >= 0) {
        activeScenes.splice(idx, 1);
      }
    }
  }
  const wrappers = [];
  if (root instanceof HTMLElement && root.classList.contains("game-scene-wrapper")) {
    wrappers.push(root);
  }
  wrappers.push(...(root.querySelectorAll?.(".game-scene-wrapper") || []));
  for (const wrapper of wrappers) {
    const sourceNode = observerSources.get(wrapper);
    if (sourceNode) {
      const index = activeScenes.indexOf(sourceNode);
      if (index >= 0) {
        activeScenes.splice(index, 1);
      }
      sceneObservers.get(sourceNode)?.unobserve(wrapper);
      observerSources.delete(wrapper);
      delete sourceNode.__guidenhHydratedRoot;
      delete sourceNode.dataset.sceneHydrated;
    }
  }
  const detachedRuntimes = [];
  for (const node of [...activeScenes]) {
    const runtimeNode = node.__guidenhHydratedRoot;
    if (!(runtimeNode instanceof HTMLElement)
      || (runtimeNode.isConnected && !root.contains(runtimeNode))) {
      continue;
    }
    if (!root.contains(runtimeNode)) {
      detachedRuntimes.push(runtimeNode);
    }
    const index = activeScenes.indexOf(node);
    if (index >= 0) {
      activeScenes.splice(index, 1);
    }
    delete node.__guidenhHydratedRoot;
  }
  loadedModelViewerModule?.disposeHydratedScenes?.(root);
  for (const runtime of detachedRuntimes) {
    loadedModelViewerModule?.disposeHydratedScenes?.(runtime);
  }
}
