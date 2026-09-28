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

// Maximum number of game scenes that may be live (hydrated) at the same time.
// Each scene allocates its own WebGL context; browsers cap that at ~16 globally,
// and stacking too many causes severe lag and the early scenes to crash.
const MAX_ACTIVE_SCENES = 2;
// Tracks the order in which scenes were activated so we can evict the oldest
// when the live set exceeds the cap.
const activeScenes = [];
// Nodes whose viewport status entry is "intersecting" but that have been
// throttled because the live set is full. They will be promoted as slots free.
const pendingScenes = new Set();
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
  clearHydrationFailure(node);
  // Evict the oldest live scene while the cap is reached so newcomers can run.
  while (activeScenes.length >= MAX_ACTIVE_SCENES) {
    const oldest = activeScenes.shift();
    if (oldest && oldest !== node) {
      await disposeNode(oldest);
    }
  }
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
  const language = (document.documentElement.lang || navigator.language || "en").toLowerCase();
  if (language.startsWith("zh-tw") || language.startsWith("zh-hk")) {
    return { error: "3D 場景載入失敗。", retry: "重試", loading: "正在載入 3D 場景…" };
  }
  if (language.startsWith("zh")) {
    return { error: "3D 场景加载失败。", retry: "重试", loading: "正在加载 3D 场景…" };
  }
  if (language.startsWith("ja")) {
    return { error: "3D シーンを読み込めません。", retry: "再試行", loading: "3D シーンを読み込み中…" };
  }
  if (language.startsWith("de")) {
    return { error: "3D-Szene konnte nicht geladen werden.", retry: "Erneut versuchen", loading: "3D-Szene wird geladen…" };
  }
  if (language.startsWith("fr")) {
    return { error: "La scène 3D n’a pas pu être chargée.", retry: "Réessayer", loading: "Chargement de la scène 3D…" };
  }
  if (language.startsWith("es")) {
    return { error: "No se pudo cargar la escena 3D.", retry: "Reintentar", loading: "Cargando la escena 3D…" };
  }
  if (language.startsWith("pt")) {
    return { error: "Não foi possível carregar a cena 3D.", retry: "Tentar novamente", loading: "Carregando a cena 3D…" };
  }
  if (language.startsWith("ru")) {
    return { error: "Не удалось загрузить 3D-сцену.", retry: "Повторить", loading: "Загрузка 3D-сцены…" };
  }
  if (language.startsWith("uk")) {
    return { error: "Не вдалося завантажити 3D-сцену.", retry: "Повторити", loading: "Завантаження 3D-сцени…" };
  }
  if (language.startsWith("pl")) {
    return { error: "Nie można załadować sceny 3D.", retry: "Spróbuj ponownie", loading: "Ładowanie sceny 3D…" };
  }
  if (language.startsWith("nl")) {
    return { error: "De 3D-scène kon niet worden geladen.", retry: "Opnieuw proberen", loading: "3D-scène laden…" };
  }
  return { error: "The 3D scene could not be loaded.", retry: "Retry", loading: "Loading 3D scene…" };
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
  for (const node of entry.nodes) {
    pendingScenes.delete(node);
  }
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
        pendingScenes.delete(node);
        await disposeNode(node);
        continue;
      }
      if (entry.isIntersecting) {
        const runtimeConnected = node.__guidenhHydratedRoot instanceof HTMLElement
          && node.__guidenhHydratedRoot.isConnected;
        if (!node.isConnected && !runtimeConnected) {
          pendingScenes.delete(node);
          continue;
        }
        if (node.dataset.sceneHydrated === "true") {
          markActive(node);
          continue;
        }
        if (activeScenes.length >= MAX_ACTIVE_SCENES) {
          // Defer hydration until a scene leaves the viewport. The observer keeps
          // watching so the next scroll update triggers another evaluation.
          pendingScenes.add(node);
          continue;
        }
        pendingScenes.delete(node);
        await hydrateNode(node, module, () => !observerEntry.invalid);
      } else {
        // Scene scrolled out of view: dispose it so its WebGL context is freed
        // (this is what previously made repeated scrolls crash early scenes).
        pendingScenes.delete(node);
        if (node.dataset.sceneHydrated === "true") {
          await disposeNode(node);
        }
        // Promote one queued scene now that we may have a free slot.
        if (pendingScenes.size && activeScenes.length < MAX_ACTIVE_SCENES) {
          const next = pendingScenes.values()
            .next().value;
          pendingScenes.delete(next);
          await hydrateNode(next, module, () => !observerEntry.invalid);
        }
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
      pendingScenes.delete(node);
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
      pendingScenes.delete(sourceNode);
      sceneObservers.get(sourceNode)?.unobserve(wrapper);
      observerSources.delete(wrapper);
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
