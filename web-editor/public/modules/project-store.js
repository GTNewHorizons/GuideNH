const DB_NAME = "guidenh-web-editor";
const STORE_NAME = "projects";
const ACTIVE_KEY = "active";
const FALLBACK_KEY = "guidenh-web-editor-project";

function openDatabase() {
  return new Promise((resolve, reject) => {
    if (!globalThis.indexedDB) {
      reject(new Error("IndexedDB unavailable"));
      return;
    }
    const request = indexedDB.open(DB_NAME, 1);
    request.onupgradeneeded = () => request.result.createObjectStore(STORE_NAME);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error || new Error("IndexedDB open failed"));
  });
}

export async function loadProject() {
  try {
    const db = await openDatabase();
    return await new Promise((resolve, reject) => {
      const request = db.transaction(STORE_NAME, "readonly").objectStore(STORE_NAME).get(ACTIVE_KEY);
      request.onsuccess = () => resolve(request.result || null);
      request.onerror = () => reject(request.error);
    });
  } catch (_) {
    try {
      return JSON.parse(localStorage.getItem(FALLBACK_KEY) || "null");
    } catch (_) {
      return null;
    }
  }
}

export async function saveProject(project) {
  const value = { ...project, updatedAt: new Date().toISOString() };
  try {
    const db = await openDatabase();
    await new Promise((resolve, reject) => {
      const transaction = db.transaction(STORE_NAME, "readwrite");
      transaction.objectStore(STORE_NAME).put(value, ACTIVE_KEY);
      transaction.oncomplete = resolve;
      transaction.onerror = () => reject(transaction.error);
    });
  } catch (_) {
    localStorage.setItem(FALLBACK_KEY, JSON.stringify(value));
  }
  return value;
}

export async function clearProject() {
  try {
    const db = await openDatabase();
    await new Promise((resolve, reject) => {
      const transaction = db.transaction(STORE_NAME, "readwrite");
      transaction.objectStore(STORE_NAME).delete(ACTIVE_KEY);
      transaction.oncomplete = resolve;
      transaction.onerror = () => reject(transaction.error);
    });
  } catch (_) {
    localStorage.removeItem(FALLBACK_KEY);
  }
}
